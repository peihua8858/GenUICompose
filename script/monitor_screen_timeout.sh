#!/bin/bash

# ---------------------------- 颜色定义 ---------------------------------------
COLOR_RESET="\033[0m"
COLOR_RED="\033[1;31m"
COLOR_GREEN="\033[1;32m"
COLOR_YELLOW="\033[1;33m"
COLOR_CYAN="\033[1;36m"

# 目标值
TARGET_VALUE="2147483647"
# 停止时间（24小时制，例如 21:00 表示晚上9点）
STOP_TIME="21:00"
# 日志文件路径（脚本所在目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${SCRIPT_DIR}/screen_timeout_monitor_${TIMESTAMP}.log"
SYSTEM_LOG_FILE="${SCRIPT_DIR}/system_logcat_${TIMESTAMP}.log"

# ---------------------------- 设备选择 ---------------------------------------
select_device() {
    if ! command -v adb >/dev/null 2>&1; then
        echo -e "${COLOR_RED}[错误] 未检测到 adb 命令，请确认已安装 Android SDK 并配置 PATH。${COLOR_RESET}" >&2
        exit 1
    fi

    local devices
    devices=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')
    if [[ -z "$devices" ]]; then
        echo -e "${COLOR_RED}[错误] 未检测到已连接的 Android 设备。${COLOR_RESET}" >&2
        echo -e "${COLOR_YELLOW}请检查：${COLOR_RESET}" >&2
        echo -e "  1) USB 是否连接正常" >&2
        echo -e "  2) 是否已开启 USB 调试" >&2
        echo -e "  3) 执行 'adb devices' 是否能看到设备" >&2
        exit 1
    fi

    local device_count
    device_count=$(echo "$devices" | wc -l | tr -d ' ')
    if [[ "$device_count" -eq 1 ]]; then
        DEVICE_SERIAL=$(echo "$devices" | head -n 1)
    else
        echo -e "${COLOR_YELLOW}[提示] 检测到多台设备（共 ${device_count} 台），请选择要监控的设备：${COLOR_RESET}"
        echo ""
        local i=1
        local device_array=()
        while IFS= read -r dev; do
            local dev_model
            dev_model=$(adb -s "$dev" shell getprop ro.product.model </dev/null 2>/dev/null | tr -d '\r')
            if [[ -n "$dev_model" ]]; then
                echo -e "  ${COLOR_GREEN}${i})${COLOR_RESET} ${dev} (${dev_model})"
            else
                echo -e "  ${COLOR_GREEN}${i})${COLOR_RESET} ${dev}"
            fi
            device_array+=("$dev")
            ((i++))
        done <<< "$devices"
        echo ""
        local selection
        while true; do
            printf "${COLOR_CYAN}请输入设备编号 [1-${device_count}]: ${COLOR_RESET}"
            read -r selection
            if [[ "$selection" =~ ^[0-9]+$ ]] && [[ "$selection" -ge 1 ]] && [[ "$selection" -le "$device_count" ]]; then
                break
            else
                echo -e "${COLOR_RED}无效输入，请输入 1 到 ${device_count} 之间的数字。${COLOR_RESET}"
            fi
        done
        DEVICE_SERIAL="${device_array[$((selection-1))]}"
        echo -e "${COLOR_GREEN}[已选择] 将监控设备: ${DEVICE_SERIAL}${COLOR_RESET}"
        echo ""
    fi
}

# 执行设备选择
select_device

echo "==========================================" | tee -a "$LOG_FILE"
echo "开始监听 screen_off_timeout 值" | tee -a "$LOG_FILE"
echo "设备: ${DEVICE_SERIAL}" | tee -a "$LOG_FILE"
echo "目标值: ${TARGET_VALUE}" | tee -a "$LOG_FILE"
echo "停止时间: ${STOP_TIME}" | tee -a "$LOG_FILE"
echo "执行日志: ${LOG_FILE}" | tee -a "$LOG_FILE"
echo "系统日志: ${SYSTEM_LOG_FILE}" | tee -a "$LOG_FILE"
echo "==========================================" | tee -a "$LOG_FILE"
echo "" | tee -a "$LOG_FILE"

# 启动后台 logcat 日志捕获
echo "[${TIMESTAMP}] 开始捕获系统 logcat 日志..." | tee -a "$LOG_FILE"
adb -s ${DEVICE_SERIAL} logcat -v time > "$SYSTEM_LOG_FILE" 2>&1 &
LOGCAT_PID=$!

# 等待一下确保 logcat 文件已创建
sleep 1

# 启动实时监控进程，从当前时刻开始监控新增的息屏日志
tail -n 0 -f "$SYSTEM_LOG_FILE" 2>/dev/null | grep --line-buffered "Going to sleep due to timeout" > "${SCRIPT_DIR}/sleep_detected_flag_${TIMESTAMP}" &
TAIL_PID=$!

# 清理函数：停止 logcat 并输出总结
cleanup() {
    echo "" | tee -a "$LOG_FILE"
    echo "==========================================" | tee -a "$LOG_FILE"
    echo "正在停止实时监控进程 (PID: ${TAIL_PID})..." | tee -a "$LOG_FILE"
    kill $TAIL_PID 2>/dev/null
    wait $TAIL_PID 2>/dev/null
    
    echo "正在停止 logcat 日志捕获 (PID: ${LOGCAT_PID})..." | tee -a "$LOG_FILE"
    kill $LOGCAT_PID 2>/dev/null
    wait $LOGCAT_PID 2>/dev/null
    echo "✓ logcat 日志已保存到: ${SYSTEM_LOG_FILE}" | tee -a "$LOG_FILE"
    
    # 清理临时文件
    rm -f "${SCRIPT_DIR}/sleep_detected_flag_${TIMESTAMP}" 2>/dev/null
    echo "==========================================" | tee -a "$LOG_FILE"
}

# 注册退出时执行清理
trap cleanup EXIT

COUNTER=0
while true; do
    COUNTER=$((COUNTER + 1))
    CURRENT_TIME=$(date +"%Y-%m-%d %H:%M:%S")
    
    # 获取 screen_off_timeout 的值
    CURRENT_VALUE=$(adb -s ${DEVICE_SERIAL} shell settings get system screen_off_timeout 2>/dev/null | tr -d '\r\n ')
    
    if [ -z "$CURRENT_VALUE" ]; then
        echo "[${CURRENT_TIME}] 第 ${COUNTER} 次查询: 获取值失败" | tee -a "$LOG_FILE"
        sleep 1
        continue
    fi
    
    echo "[${CURRENT_TIME}] 第 ${COUNTER} 次查询: screen_off_timeout = ${CURRENT_VALUE}" | tee -a "$LOG_FILE"
    
    # 检查是否等于目标值
    if [ "$CURRENT_VALUE" == "$TARGET_VALUE" ]; then
        echo "[${CURRENT_TIME}] ✓ 检测到目标值 ${TARGET_VALUE}" | tee -a "$LOG_FILE"
    else
        echo "[${CURRENT_TIME}] ✗ 当前值 ${CURRENT_VALUE} 不等于目标值 ${TARGET_VALUE}" | tee -a "$LOG_FILE"
    fi
    
    # 检查是否到达停止时间
    CURRENT_HOUR_MINUTE=$(date +"%H:%M")
    if [ "$CURRENT_HOUR_MINUTE" == "$STOP_TIME" ]; then
        echo "[${CURRENT_TIME}] ⏰ 已到达停止时间 ${STOP_TIME}，停止监听" | tee -a "$LOG_FILE"
        echo "==========================================" | tee -a "$LOG_FILE"
        echo "监听结束" | tee -a "$LOG_FILE"
        echo "执行日志: ${LOG_FILE}" | tee -a "$LOG_FILE"
        echo "系统日志: ${SYSTEM_LOG_FILE}" | tee -a "$LOG_FILE"
        echo "==========================================" | tee -a "$LOG_FILE"
        break
    fi
    
    # 检查实时监控进程是否检测到新的息屏日志
    if [ -s "${SCRIPT_DIR}/sleep_detected_flag_${TIMESTAMP}" ]; then
        echo "[${CURRENT_TIME}] ⚠ 从监听开始检测到新的息屏日志（Going to sleep due to timeout），停止监听" | tee -a "$LOG_FILE"
        echo "==========================================" | tee -a "$LOG_FILE"
        echo "监听结束" | tee -a "$LOG_FILE"
        echo "执行日志: ${LOG_FILE}" | tee -a "$LOG_FILE"
        echo "系统日志: ${SYSTEM_LOG_FILE}" | tee -a "$LOG_FILE"
        echo "==========================================" | tee -a "$LOG_FILE"
        break
    fi
    
    # 等待 2 秒后再次查询
    sleep 2
done
