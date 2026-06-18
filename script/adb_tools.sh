#!/bin/bash

# ==============================================================================
# 脚本名称: adb_tools.sh
# 功能描述: 综合 ADB 工具脚本，集成多种 ADB 调试功能
# 使用方式: ./adb_tools.sh
# ==============================================================================

# ==============================================================================
# 公共模块：颜色定义与打印函数
# ==============================================================================
RED='\033[0;31m'      # 红色：错误
GREEN='\033[0;32m'    # 绿色：成功
YELLOW='\033[0;33m'   # 黄色：提示
BLUE='\033[0;34m'     # 蓝色：信息
NC='\033[0m'          # 无颜色（重置）

# 打印成功信息
print_success() {
    echo -e "${GREEN}[成功]${NC} $1"
}

# 打印错误信息
print_error() {
    echo -e "${RED}[错误]${NC} $1"
}

# 打印警告/提示信息
print_warning() {
    echo -e "${YELLOW}[提示]${NC} $1"
}

# 打印普通信息
print_info() {
    echo -e "${BLUE}[信息]${NC} $1"
}

# 分隔线
print_divider() {
    echo -e "${BLUE}==============================================================${NC}"
}

# 计算字符串的终端显示宽度（CJK字符占2列）
str_display_width() {
    local str="$1"
    local byte_count char_count
    byte_count=$(printf '%s' "$str" | wc -c | tr -d ' ')
    char_count=$(printf '%s' "$str" | wc -m | tr -d ' ')
    # CJK字符3字节1字符占2列，每个CJK比ASCII多占1列
    echo $(( char_count + (byte_count - char_count) / 2 ))
}

# 居中打印内容（基于终端宽度居中，无侧边框）
print_center_line() {
    local content="$1"
    local box_width="$2"
    local dw=$(str_display_width "$content")
    local left_pad=$(( (box_width - dw) / 2 ))
    [[ $left_pad -lt 0 ]] && left_pad=0
    local lpad=""
    for ((i=0; i<left_pad; i++)); do lpad+=" "; done
    echo "${lpad}${content}"
}

# ==============================================================================
# 公共模块：设备检测与选择逻辑
# ==============================================================================
detect_and_select_device() {
    print_info "正在检测已连接的 ADB 设备..."

    # 检查 adb 命令是否可用
    if ! command -v adb &> /dev/null; then
        print_error "未找到 adb 命令，请先安装 Android SDK 并配置 PATH 环境变量"
        exit 1
    fi

    # 启动 adb server（避免首次执行干扰输出）
    adb start-server > /dev/null 2>&1

    # 获取已连接的设备列表（排除离线/未授权设备，仅保留 device 状态）
    DEVICES=()
    while IFS= read -r line; do
        if [[ -z "$line" || "$line" == *"List of devices attached"* ]]; then
            continue
        fi
        if [[ "$line" == *$'\t'device* || "$line" == *" device" ]]; then
            serial=$(echo "$line" | awk '{print $1}')
            DEVICES+=("$serial")
        fi
    done < <(adb devices)

    DEVICE_COUNT=${#DEVICES[@]}

    if [[ $DEVICE_COUNT -eq 0 ]]; then
        print_error "未检测到任何已连接的 ADB 设备，请先连接设备并确认 USB 调试已开启"
        exit 1
    elif [[ $DEVICE_COUNT -eq 1 ]]; then
        SELECTED_DEVICE="${DEVICES[0]}"
        print_success "检测到 1 台设备，自动选择：${SELECTED_DEVICE}"
    else
        print_warning "检测到 ${DEVICE_COUNT} 台设备，请选择要操作的设备："
        for i in "${!DEVICES[@]}"; do
            model=$(adb -s "${DEVICES[$i]}" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
            echo -e "  ${GREEN}$((i+1))${NC}. ${DEVICES[$i]} (${model:-未知型号})"
        done

        while true; do
            read -p "请输入设备序号 [1-${DEVICE_COUNT}]: " device_index
            if [[ "$device_index" =~ ^[0-9]+$ ]] && \
               [[ $device_index -ge 1 ]] && \
               [[ $device_index -le $DEVICE_COUNT ]]; then
                SELECTED_DEVICE="${DEVICES[$((device_index-1))]}"
                print_success "已选择设备：${SELECTED_DEVICE}"
                break
            else
                print_error "无效的输入，请输入 1 到 ${DEVICE_COUNT} 之间的数字"
            fi
        done
    fi

    # 统一封装 adb 命令
    ADB="adb -s ${SELECTED_DEVICE}"
}

# ==============================================================================
# 公共模块：Root 状态检查与处理
# ==============================================================================
ensure_root() {
    print_info "正在检查设备 root 状态..."
    CURRENT_USER=$(${ADB} shell whoami 2>/dev/null | tr -d '\r')

    if [[ "$CURRENT_USER" != "root" ]]; then
        print_warning "设备未处于 root 状态（当前用户：${CURRENT_USER:-未知}），执行 adb root..."
        ROOT_RESULT=$(${ADB} root 2>&1)
        echo "$ROOT_RESULT"

        print_info "等待设备重新连接..."
        ${ADB} wait-for-device
        sleep 1

        CURRENT_USER=$(${ADB} shell whoami 2>/dev/null | tr -d '\r')
        if [[ "$CURRENT_USER" != "root" ]]; then
            print_error "adb root 失败，设备可能不支持 root（用户类型：${CURRENT_USER:-未知}）"
            return 1
        fi
        print_success "设备已切换至 root 状态"
    else
        print_success "设备已处于 root 状态"
    fi
    return 0
}

# ==============================================================================
# 公共模块：Remount 检查与处理
# ==============================================================================
ensure_remount() {
    print_info "正在检查 /system 分区写入权限..."

    TEST_FILE="/system/.adb_tools_test_$$"
    WRITE_TEST=$(${ADB} shell "touch ${TEST_FILE} 2>&1 && echo OK || echo FAIL" 2>/dev/null | tr -d '\r')

    if [[ "$WRITE_TEST" != *"OK"* ]]; then
        print_warning "/system 分区不可写入，执行 adb remount..."
        REMOUNT_RESULT=$(${ADB} remount 2>&1)
        echo "$REMOUNT_RESULT"

        sleep 1
        WRITE_TEST=$(${ADB} shell "touch ${TEST_FILE} 2>&1 && echo OK || echo FAIL" 2>/dev/null | tr -d '\r')
        if [[ "$WRITE_TEST" != *"OK"* ]]; then
            print_error "adb remount 后 /system 仍不可写，请检查设备是否已禁用 verity（dm-verity）"
            print_warning "提示：可尝试手动执行 'adb disable-verity' 后重启设备再运行本脚本"
            return 1
        fi
        print_success "/system 分区已成功 remount 为可写"
    else
        print_success "/system 分区已可写入"
    fi

    # 清理测试文件
    ${ADB} shell "rm -f ${TEST_FILE}" > /dev/null 2>&1
    return 0
}

# ==============================================================================
# 公共模块：返回主菜单提示
# ==============================================================================
wait_return_menu() {
    echo ""
    read -p "按 Enter 键返回主菜单..." _
}

# ==============================================================================
# 功能 1: 切换网络环境
# ==============================================================================
func_switch_env() {
    print_divider
    print_info "【切换网络环境】"
    print_divider

    # 执行 root 和 remount
    ensure_root || return
    ensure_remount || return

    echo ""
    print_warning "请选择要切换的网络环境："
    echo -e "  ${GREEN}0${NC} = 线上环境"
    echo -e "  ${GREEN}1${NC} = 预发环境1"
    echo -e "  ${GREEN}2${NC} = 预发环境2"

    while true; do
        read -p "请输入环境编号 [0/1/2]: " ENV_VALUE
        case "$ENV_VALUE" in
            0) ENV_NAME="线上环境"; break ;;
            1) ENV_NAME="预发环境1"; break ;;
            2) ENV_NAME="预发环境2"; break ;;
            *) print_error "无效的输入，请输入 0、1 或 2" ;;
        esac
    done

    PROP_KEY="persist.sys.genie.env"
    print_info "正在执行环境切换：${PROP_KEY}=${ENV_VALUE} ..."
    ${ADB} shell "setprop ${PROP_KEY} ${ENV_VALUE}"

    # 验证
    sleep 1
    ACTUAL_VALUE=$(${ADB} shell "getprop ${PROP_KEY}" 2>/dev/null | tr -d '\r')
    if [[ "$ACTUAL_VALUE" == "$ENV_VALUE" ]]; then
        print_success "环境切换成功！${PROP_KEY} = ${ACTUAL_VALUE} (${ENV_NAME})"
    else
        print_error "环境切换失败！期望值=${ENV_VALUE}，实际值=${ACTUAL_VALUE:-空}"
        return
    fi

    # 提示是否重启
    echo ""
    read -p "是否需要重启设备使环境生效？[y/N]: " reboot_choice
    if [[ "$reboot_choice" == "y" || "$reboot_choice" == "Y" ]]; then
        print_info "正在执行 adb reboot..."
        ${ADB} reboot
        print_success "重启命令已发送，请等待设备重新启动"
    else
        print_warning "已跳过重启，部分功能可能需要重启后才能生效"
    fi
}

# ==============================================================================
# 功能 2: 设置埋点UT实时上报验证
# ==============================================================================
func_ut_realtime() {
    print_divider
    print_info "【设置埋点UT实时上报验证】"
    print_divider

    # 执行 root 和 remount
    ensure_root || return
    ensure_remount || return

    # 设置 realtime enable
    RT_ENABLE_PROP="persist.ut.realtime.enable"
    RT_ENABLE_VALUE="1"
    print_info "正在设置 ${RT_ENABLE_PROP}=${RT_ENABLE_VALUE} ..."
    ${ADB} shell "setprop ${RT_ENABLE_PROP} ${RT_ENABLE_VALUE}"

    sleep 1
    ACTUAL_VALUE=$(${ADB} shell "getprop ${RT_ENABLE_PROP}" 2>/dev/null | tr -d '\r')
    if [[ "$ACTUAL_VALUE" == "$RT_ENABLE_VALUE" ]]; then
        print_success "${RT_ENABLE_PROP} 已成功设置为 ${RT_ENABLE_VALUE}（验证通过）"
    else
        print_error "${RT_ENABLE_PROP} 设置验证失败，期望值=${RT_ENABLE_VALUE}，实际值=${ACTUAL_VALUE:-空}"
        return
    fi

    # 设置 debugkey
    DEBUGKEY_PROP="ut.realtime.debugkey"
    echo ""
    print_warning "请输入 debugkey 值："
    read -p "debugkey: " DEBUG_KEY_VALUE
    if [[ -z "$DEBUG_KEY_VALUE" ]]; then
        print_error "debugkey 值不能为空"
        return
    fi

    print_info "正在设置 ${DEBUGKEY_PROP}=${DEBUG_KEY_VALUE} ..."
    ${ADB} shell "setprop ${DEBUGKEY_PROP} ${DEBUG_KEY_VALUE}"

    sleep 1
    ACTUAL_DEBUGKEY=$(${ADB} shell "getprop ${DEBUGKEY_PROP}" 2>/dev/null | tr -d '\r')
    if [[ "$ACTUAL_DEBUGKEY" == "$DEBUG_KEY_VALUE" ]]; then
        print_success "${DEBUGKEY_PROP} 已成功设置为 ${DEBUG_KEY_VALUE}（验证通过）"
    else
        print_error "${DEBUGKEY_PROP} 设置验证失败，期望值=${DEBUG_KEY_VALUE}，实际值=${ACTUAL_DEBUGKEY:-空}"
        return
    fi

    # 重启指定进程
    echo ""
    print_warning "请输入需要重启的进程包名："
    read -p "包名: " PACKAGE_NAME
    if [[ -z "$PACKAGE_NAME" ]]; then
        print_error "包名不能为空"
        return
    fi

    print_info "正在查询进程 ${PACKAGE_NAME} ..."
    PROCESS_LIST=$(${ADB} shell "ps | grep ${PACKAGE_NAME}" 2>/dev/null | grep -v "grep" | tr -d '\r')

    if [[ -z "$PROCESS_LIST" ]]; then
        print_error "未找到与 '${PACKAGE_NAME}' 匹配的进程"
        return
    fi

    # 解析进程信息
    PIDS=()
    PROCESS_LINES=()
    while IFS= read -r line; do
        if [[ -n "$line" ]]; then
            pid=$(echo "$line" | awk '{print $2}')
            PIDS+=("$pid")
            PROCESS_LINES+=("$line")
        fi
    done <<< "$PROCESS_LIST"

    PROCESS_COUNT=${#PIDS[@]}
    if [[ $PROCESS_COUNT -eq 0 ]]; then
        print_error "未找到与 '${PACKAGE_NAME}' 匹配的进程"
        return
    fi

    # 选择要 kill 的进程
    TARGET_PIDS=()
    if [[ $PROCESS_COUNT -eq 1 ]]; then
        TARGET_PIDS+=("${PIDS[0]}")
        print_info "找到 1 个匹配进程（PID: ${PIDS[0]}）："
        echo "  ${PROCESS_LINES[0]}"
    else
        print_warning "找到 ${PROCESS_COUNT} 个匹配进程，请选择要 kill 的进程："
        for i in "${!PROCESS_LINES[@]}"; do
            echo -e "  ${GREEN}$((i+1))${NC}. PID=${PIDS[$i]}  ${PROCESS_LINES[$i]}"
        done

        while true; do
            echo ""
            print_info "可输入多个序号（用空格或逗号分隔），输入 all 表示全部 kill"
            read -p "请输入进程序号: " proc_input

            if [[ "$proc_input" == "all" || "$proc_input" == "ALL" ]]; then
                TARGET_PIDS=("${PIDS[@]}")
                print_success "已选择全部 ${PROCESS_COUNT} 个进程"
                break
            fi

            proc_input=$(echo "$proc_input" | tr ',' ' ')
            VALID=true
            SELECTED_PIDS=()
            for idx in $proc_input; do
                if [[ "$idx" =~ ^[0-9]+$ ]] && [[ $idx -ge 1 ]] && [[ $idx -le $PROCESS_COUNT ]]; then
                    SELECTED_PIDS+=("${PIDS[$((idx-1))]}")
                else
                    print_error "无效的序号：${idx}，有效范围为 1 到 ${PROCESS_COUNT}"
                    VALID=false
                    break
                fi
            done

            if [[ "$VALID" == true ]] && [[ ${#SELECTED_PIDS[@]} -gt 0 ]]; then
                declare -A SEEN_PIDS
                for p in "${SELECTED_PIDS[@]}"; do
                    if [[ -z "${SEEN_PIDS[$p]+_}" ]]; then
                        TARGET_PIDS+=("$p")
                        SEEN_PIDS[$p]=1
                    fi
                done
                unset SEEN_PIDS
                print_success "已选择 ${#TARGET_PIDS[@]} 个进程: ${TARGET_PIDS[*]}"
                break
            elif [[ "$VALID" == true ]]; then
                print_error "未输入任何序号，请重新输入"
            fi
        done
    fi

    # 记录旧 PID 并 kill
    OLD_PIDS=("${TARGET_PIDS[@]}")
    KILL_FAILED=false
    for pid in "${TARGET_PIDS[@]}"; do
        print_info "正在 kill 进程 PID=${pid} ..."
        ${ADB} shell "kill ${pid}"
        if [[ $? -ne 0 ]]; then
            print_error "kill 进程 PID=${pid} 失败"
            KILL_FAILED=true
        else
            print_success "已发送 kill 信号给进程 PID=${pid}"
        fi
    done

    if [[ "$KILL_FAILED" == true ]]; then
        print_error "部分进程 kill 失败，请检查后重试"
        return
    fi

    # 验证进程重启
    print_info "等待进程重新启动..."
    sleep 3

    NEW_PROCESS_LIST=$(${ADB} shell "ps | grep ${PACKAGE_NAME}" 2>/dev/null | grep -v "grep" | tr -d '\r')
    if [[ -z "$NEW_PROCESS_LIST" ]]; then
        print_info "进程尚未出现，继续等待..."
        sleep 3
        NEW_PROCESS_LIST=$(${ADB} shell "ps | grep ${PACKAGE_NAME}" 2>/dev/null | grep -v "grep" | tr -d '\r')
    fi

    if [[ -z "$NEW_PROCESS_LIST" ]]; then
        print_error "进程 '${PACKAGE_NAME}' 未能重新启动，请手动检查"
        return
    fi

    NEW_PIDS=()
    while IFS= read -r line; do
        if [[ -n "$line" ]]; then
            new_pid=$(echo "$line" | awk '{print $2}')
            NEW_PIDS+=("$new_pid")
        fi
    done <<< "$NEW_PROCESS_LIST"

    ALL_RESTARTED=true
    for old_pid in "${OLD_PIDS[@]}"; do
        STILL_EXISTS=false
        for new_pid in "${NEW_PIDS[@]}"; do
            if [[ "$new_pid" == "$old_pid" ]]; then
                STILL_EXISTS=true
                break
            fi
        done
        if [[ "$STILL_EXISTS" == true ]]; then
            print_error "进程 PID=${old_pid} 未改变，可能未成功重启"
            ALL_RESTARTED=false
        else
            print_success "进程 PID=${old_pid} 已重启（旧 PID 已不存在）"
        fi
    done

    if [[ "$ALL_RESTARTED" == true ]]; then
        echo ""
        print_success "============================================"
        print_success "  埋点UT实时上报配置完成！"
        print_success "  - persist.ut.realtime.enable = 1"
        print_success "  - ut.realtime.debugkey = ${DEBUG_KEY_VALUE}"
        print_success "  - 进程 ${PACKAGE_NAME} 已重启"
        print_success "    已 kill 的旧 PID: ${OLD_PIDS[*]}"
        print_success "    当前新 PID: ${NEW_PIDS[*]}"
        print_success "============================================"
    fi
}

# ==============================================================================
# 功能 3: 横竖屏切换
# ==============================================================================
func_rotation() {
    print_divider
    print_info "【横竖屏切换】"
    print_divider

    echo -e "  ${GREEN}0${NC} = 竖屏（Portrait）"
    echo -e "  ${GREEN}1${NC} = 横屏（Landscape）"

    while true; do
        read -p "请选择 [0/1]: " rotation_value
        if [[ "$rotation_value" == "0" || "$rotation_value" == "1" ]]; then
            break
        fi
        print_error "无效输入，请输入 0 或 1"
    done

    ${ADB} shell settings put system user_rotation ${rotation_value}
    if [[ $? -eq 0 ]]; then
        [[ "$rotation_value" == "0" ]] && mode="竖屏" || mode="横屏"
        print_success "已切换为${mode}模式"
    else
        print_error "切换失败"
    fi
}

# ==============================================================================
# 功能 4: 屏幕分辨率修改
# ==============================================================================
func_resolution() {
    print_divider
    print_info "【屏幕分辨率修改】"
    print_divider

    echo -e "  ${GREEN}1${NC}. 修改 density（DPI）"
    echo -e "  ${GREEN}2${NC}. 修改 size（分辨率）"
    echo -e "  ${GREEN}3${NC}. 重置为默认值"
    echo -e "  ${GREEN}4${NC}. 预设：1920x1080 / density 160"
    echo -e "  ${GREEN}5${NC}. 预设：2160x3840 / density 480"

    read -p "请选择操作 [1-5]: " res_choice
    case "$res_choice" in
        1)
            read -p "请输入 density 值（如 160、320、480）: " density_val
            if [[ -n "$density_val" ]]; then
                ${ADB} shell wm density ${density_val}
                print_success "density 已设置为 ${density_val}"
            else
                print_error "输入不能为空"
            fi
            ;;
        2)
            read -p "请输入 size 值（如 1920x1080）: " size_val
            if [[ -n "$size_val" ]]; then
                ${ADB} shell wm size ${size_val}
                print_success "size 已设置为 ${size_val}"
            else
                print_error "输入不能为空"
            fi
            ;;
        3)
            ${ADB} shell wm size reset
            ${ADB} shell wm density reset
            print_success "分辨率和 density 已重置为默认值"
            ;;
        4)
            ${ADB} shell wm size 1920x1080
            ${ADB} shell wm density 160
            print_success "已设置为 1920x1080 / density 160"
            ;;
        5)
            ${ADB} shell wm size 2160x3840
            ${ADB} shell wm density 480
            print_success "已设置为 2160x3840 / density 480"
            ;;
        *)
            print_error "无效选择"
            ;;
    esac
}

# ==============================================================================
# 功能 5: 发送语音命令
# ==============================================================================
func_voice_command() {
    print_divider
    print_info "【发送语音命令】"
    print_divider

    read -p "请输入语音文本: " voice_text
    if [[ -z "$voice_text" ]]; then
        print_error "语音文本不能为空"
        return
    fi

    ${ADB} shell am start -d "waft://com.alibaba.genie.waft.agcs/test?input=${voice_text}"
    if [[ $? -eq 0 ]]; then
        print_success "语音命令已发送：${voice_text}"
    else
        print_error "语音命令发送失败"
    fi
}

# ==============================================================================
# 功能 6: 关闭模拟器
# ==============================================================================
func_close_emulator() {
    print_divider
    print_info "【关闭模拟器】"
    print_divider

    # 检测模拟器设备（通常以 emulator- 开头）
    EMULATORS=()
    while IFS= read -r line; do
        if [[ -z "$line" || "$line" == *"List of devices attached"* ]]; then
            continue
        fi
        if [[ "$line" == emulator-* ]]; then
            serial=$(echo "$line" | awk '{print $1}')
            EMULATORS+=("$serial")
        fi
    done < <(adb devices)

    if [[ ${#EMULATORS[@]} -eq 0 ]]; then
        print_warning "未检测到运行中的模拟器"
        return
    fi

    for emu in "${EMULATORS[@]}"; do
        print_info "正在关闭模拟器：${emu} ..."
        adb -s ${emu} emu kill 2>/dev/null
        if [[ $? -eq 0 ]]; then
            print_success "模拟器 ${emu} 已关闭"
        else
            print_error "关闭模拟器 ${emu} 失败"
        fi
    done
}

# ==============================================================================
# 功能 7: 查看顶层 Activity
# ==============================================================================
func_top_activity() {
    print_divider
    print_info "【查看顶层 Activity】"
    print_divider

    RESULT=$(${ADB} shell dumpsys activity activities 2>/dev/null | grep -E "ResumedActivity|mResumedActivity")
    if [[ -n "$RESULT" ]]; then
        print_success "当前顶层 Activity："
        echo "$RESULT"
    else
        print_warning "未找到 ResumedActivity 信息"
    fi
}

# ==============================================================================
# 功能 8: 查看 app 版本号
# ==============================================================================
func_app_version() {
    print_divider
    print_info "【查看 app 版本号】"
    print_divider

    read -p "请输入包名: " pkg_name
    if [[ -z "$pkg_name" ]]; then
        print_error "包名不能为空"
        return
    fi

    RESULT=$(${ADB} shell dumpsys package ${pkg_name} 2>/dev/null | grep -E "versionCode|versionName")
    if [[ -n "$RESULT" ]]; then
        print_success "包 ${pkg_name} 版本信息："
        echo "$RESULT"
    else
        print_error "未找到包 ${pkg_name} 的版本信息"
    fi
}

# ==============================================================================
# 功能 9: 查询设备 UUID
# ==============================================================================
func_device_uuid() {
    print_divider
    print_info "【查询设备 UUID】"
    print_divider

    RESULT=$(${ADB} shell getprop 2>/dev/null | grep -i uuid)
    if [[ -n "$RESULT" ]]; then
        print_success "设备 UUID 信息："
        echo "$RESULT"
    else
        print_warning "未找到 UUID 相关属性"
    fi
}

# ==============================================================================
# 功能 10: 进程管理
# ==============================================================================
func_process_manage() {
    print_divider
    print_info "【进程管理】"
    print_divider

    echo -e "  ${GREEN}1${NC}. 查看进程（关键字查询）"
    echo -e "  ${GREEN}2${NC}. Kill 进程（输入 PID）"
    echo -e "  ${GREEN}3${NC}. 强制停止 app（输入包名）"
    echo -e "  ${GREEN}4${NC}. 通过包名 kill 进程（支持多进程选择）"

    read -p "请选择操作 [1-4]: " proc_choice
    case "$proc_choice" in
        1)
            read -p "请输入查询关键字: " keyword
            if [[ -z "$keyword" ]]; then
                print_error "关键字不能为空"
                return
            fi
            RESULT=$(${ADB} shell "ps | grep ${keyword}" 2>/dev/null | grep -v "grep")
            if [[ -n "$RESULT" ]]; then
                print_success "匹配进程列表："
                echo "$RESULT"
            else
                print_warning "未找到匹配 '${keyword}' 的进程"
            fi
            ;;
        2)
            read -p "请输入要 kill 的进程 PID: " kill_pid
            if [[ -z "$kill_pid" ]]; then
                print_error "PID 不能为空"
                return
            fi
            ${ADB} shell "kill ${kill_pid}"
            if [[ $? -eq 0 ]]; then
                print_success "已发送 kill 信号给 PID=${kill_pid}"
            else
                print_error "kill 失败"
            fi
            ;;
        3)
            read -p "请输入要强制停止的包名: " stop_pkg
            if [[ -z "$stop_pkg" ]]; then
                print_error "包名不能为空"
                return
            fi
            ${ADB} shell "am force-stop ${stop_pkg}"
            if [[ $? -eq 0 ]]; then
                print_success "已强制停止 ${stop_pkg}"
            else
                print_error "强制停止失败"
            fi
            ;;
        4)
            read -p "请输入包名（支持部分匹配）: " pkg_name
            if [[ -z "$pkg_name" ]]; then
                print_error "包名不能为空"
                return
            fi

            # 查询匹配的进程列表
            PROC_LIST=$(${ADB} shell "ps | grep ${pkg_name}" 2>/dev/null | grep -v "grep")
            if [[ -z "$PROC_LIST" ]]; then
                print_warning "未找到包含 '${pkg_name}' 的进程"
                return
            fi

            # 解析进程信息并编号展示
            PIDS=()
            PROC_NAMES=()
            local idx=0
            while IFS= read -r line; do
                [[ -z "$line" ]] && continue
                local pid=$(echo "$line" | awk '{print $2}')
                local pname=$(echo "$line" | awk '{print $NF}')
                PIDS+=("$pid")
                PROC_NAMES+=("$pname")
                idx=$((idx + 1))
                echo -e "  ${GREEN}${idx}${NC}. PID=${pid}  进程名=${pname}"
            done <<< "$PROC_LIST"

            local proc_count=${#PIDS[@]}
            if [[ $proc_count -eq 0 ]]; then
                print_warning "未解析到有效进程"
                return
            fi

            echo ""
            print_info "共找到 ${proc_count} 个进程"
            print_warning "请输入要 kill 的进程编号（多个用空格或逗号分隔，输入 all 表示全部）:"
            read -p "> " kill_input

            if [[ -z "$kill_input" ]]; then
                print_error "输入不能为空"
                return
            fi

            # 确定要 kill 的 PID 列表
            KILL_PIDS=()
            if [[ "$kill_input" == "all" || "$kill_input" == "ALL" ]]; then
                KILL_PIDS=("${PIDS[@]}")
            else
                # 支持空格、逗号分隔
                kill_input=$(echo "$kill_input" | tr ',' ' ')
                for num in $kill_input; do
                    if [[ "$num" =~ ^[0-9]+$ ]] && [[ $num -ge 1 ]] && [[ $num -le $proc_count ]]; then
                        KILL_PIDS+=("${PIDS[$((num-1))]}")
                    else
                        print_warning "忽略无效编号: ${num}"
                    fi
                done
            fi

            if [[ ${#KILL_PIDS[@]} -eq 0 ]]; then
                print_error "未选择有效的进程"
                return
            fi

            # 执行 kill
            echo ""
            for pid in "${KILL_PIDS[@]}"; do
                ${ADB} shell "kill ${pid}" 2>/dev/null
                if [[ $? -eq 0 ]]; then
                    print_success "已 kill PID=${pid}"
                else
                    print_error "kill PID=${pid} 失败"
                fi
            done

            # 等待片刻后查询当前状态
            sleep 1
            echo ""
            print_info "当前 '${pkg_name}' 进程状态："
            REMAINING=$(${ADB} shell "ps | grep ${pkg_name}" 2>/dev/null | grep -v "grep")
            if [[ -n "$REMAINING" ]]; then
                echo "$REMAINING"
            else
                print_warning "已无匹配进程"
            fi
            ;;
        *)
            print_error "无效选择"
            ;;
    esac
}

# ==============================================================================
# 功能 11: 打开 Deeplink
# ==============================================================================
func_deeplink() {
    print_divider
    print_info "【打开 Deeplink】"
    print_divider

    read -p "请输入 Deeplink URI: " deeplink_uri
    if [[ -z "$deeplink_uri" ]]; then
        print_error "URI 不能为空"
        return
    fi

    ${ADB} shell am start -W -a android.intent.action.VIEW -d "\"${deeplink_uri}\""
    if [[ $? -eq 0 ]]; then
        print_success "Deeplink 已打开：${deeplink_uri}"
    else
        print_error "打开 Deeplink 失败"
    fi
}

# ==============================================================================
# 功能 12: 打开应用/管理应用
# ==============================================================================
func_app_manage() {
    print_divider
    print_info "【打开应用/管理应用】"
    print_divider

    echo -e "  ${GREEN}1${NC}. 启动 app（输入包名）"
    echo -e "  ${GREEN}2${NC}. 打开指定 Activity"
    echo -e "  ${GREEN}3${NC}. 通过 Deeplink 打开"
    echo -e "  ${GREEN}4${NC}. 发送开机广播"
    echo -e "  ${GREEN}5${NC}. 启动 Service（输入 component）"
    echo -e "  ${GREEN}6${NC}. 清除 app 数据（输入包名）"
    echo -e "  ${GREEN}7${NC}. 查看安装路径（输入包名）"

    read -p "请选择操作 [1-7]: " app_choice
    case "$app_choice" in
        1)
            read -p "请输入包名: " launch_pkg
            if [[ -z "$launch_pkg" ]]; then
                print_error "包名不能为空"; return
            fi
            ${ADB} shell monkey -p ${launch_pkg} -c android.intent.category.LAUNCHER 1 2>/dev/null
            print_success "已尝试启动 ${launch_pkg}"
            ;;
        2)
            print_info "格式示例: com.example.app/.MainActivity 或 com.example.app/com.example.app.MainActivity"
            read -p "请输入 Activity component: " activity_component
            if [[ -z "$activity_component" ]]; then
                print_error "Activity component 不能为空"; return
            fi
            ${ADB} shell am start -n "${activity_component}"
            if [[ $? -eq 0 ]]; then
                print_success "Activity 已打开：${activity_component}"
            else
                print_error "打开 Activity 失败"
            fi
            ;;
        3)
            print_info "格式示例: genie://com.android.settings/bt?open=true"
            read -p "请输入 Deeplink URI: " deeplink_uri
            if [[ -z "$deeplink_uri" ]]; then
                print_error "Deeplink URI 不能为空"; return
            fi
            ${ADB} shell am start -W -a android.intent.action.VIEW -d "\"${deeplink_uri}\""
            if [[ $? -eq 0 ]]; then
                print_success "Deeplink 已打开：${deeplink_uri}"
            else
                print_error "打开 Deeplink 失败"
            fi
            ;;
        4)
            print_info "正在发送开机广播..."
            ${ADB} shell am broadcast -a android.intent.action.BOOT_COMPLETED
            print_success "开机广播已发送"
            ;;
        5)
            read -p "请输入 Service component（如 com.example/.MyService）: " component
            if [[ -z "$component" ]]; then
                print_error "component 不能为空"; return
            fi
            ${ADB} shell am startservice -n "${component}"
            if [[ $? -eq 0 ]]; then
                print_success "Service 已启动：${component}"
            else
                print_error "启动 Service 失败"
            fi
            ;;
        6)
            read -p "请输入要清除数据的包名: " clear_pkg
            if [[ -z "$clear_pkg" ]]; then
                print_error "包名不能为空"; return
            fi
            ${ADB} shell pm clear ${clear_pkg}
            print_success "已清除 ${clear_pkg} 的数据"
            ;;
        7)
            read -p "请输入包名: " path_pkg
            if [[ -z "$path_pkg" ]]; then
                print_error "包名不能为空"; return
            fi
            RESULT=$(${ADB} shell pm path ${path_pkg} 2>/dev/null)
            if [[ -n "$RESULT" ]]; then
                print_success "${path_pkg} 安装路径："
                echo "$RESULT"
            else
                print_error "未找到包 ${path_pkg}"
            fi
            ;;
        *)
            print_error "无效选择"
            ;;
    esac
}

# ==============================================================================
# 功能 13: 系统属性管理
# ==============================================================================
func_system_property() {
    print_divider
    print_info "【系统属性管理】"
    print_divider

    echo -e "  ${GREEN}1${NC}. 获取 settings 值"
    echo -e "  ${GREEN}2${NC}. 设置 settings 值"
    echo -e "  ${GREEN}3${NC}. getprop 查看属性"
    echo -e "  ${GREEN}4${NC}. setprop 设置属性"

    read -p "请选择操作 [1-4]: " prop_choice
    case "$prop_choice" in
        1)
            echo -e "  命名空间：${GREEN}secure${NC} / ${GREEN}system${NC} / ${GREEN}global${NC}"
            read -p "请输入命名空间: " namespace
            read -p "请输入 key: " settings_key
            if [[ -z "$namespace" || -z "$settings_key" ]]; then
                print_error "命名空间和 key 不能为空"; return
            fi
            RESULT=$(${ADB} shell settings get ${namespace} ${settings_key} 2>/dev/null | tr -d '\r')
            print_success "${namespace}/${settings_key} = ${RESULT}"
            ;;
        2)
            echo -e "  命名空间：${GREEN}secure${NC} / ${GREEN}system${NC} / ${GREEN}global${NC}"
            read -p "请输入命名空间: " namespace
            read -p "请输入 key: " settings_key
            read -p "请输入 value: " settings_value
            if [[ -z "$namespace" || -z "$settings_key" || -z "$settings_value" ]]; then
                print_error "命名空间、key 和 value 不能为空"; return
            fi
            ${ADB} shell settings put ${namespace} ${settings_key} ${settings_value}
            print_success "已设置 ${namespace}/${settings_key} = ${settings_value}"
            ;;
        3)
            read -p "请输入属性名（留空查看所有）: " prop_name
            if [[ -z "$prop_name" ]]; then
                ${ADB} shell getprop
            else
                RESULT=$(${ADB} shell getprop ${prop_name} 2>/dev/null | tr -d '\r')
                print_success "${prop_name} = ${RESULT}"
            fi
            ;;
        4)
            ensure_root || return
            read -p "请输入属性名: " prop_name
            read -p "请输入属性值: " prop_value
            if [[ -z "$prop_name" || -z "$prop_value" ]]; then
                print_error "属性名和值不能为空"; return
            fi
            ${ADB} shell "setprop ${prop_name} ${prop_value}"
            sleep 1
            ACTUAL=$(${ADB} shell "getprop ${prop_name}" 2>/dev/null | tr -d '\r')
            if [[ "$ACTUAL" == "$prop_value" ]]; then
                print_success "${prop_name} = ${prop_value}（验证通过）"
            else
                print_error "设置验证失败，实际值=${ACTUAL:-空}"
            fi
            ;;
        *)
            print_error "无效选择"
            ;;
    esac
}

# ==============================================================================
# 功能 14: Content Provider 读取
# ==============================================================================
func_content_read() {
    print_divider
    print_info "【Content Provider 读取】"
    print_divider

    read -p "请输入 Content URI: " content_uri
    if [[ -z "$content_uri" ]]; then
        print_error "URI 不能为空"
        return
    fi

    RESULT=$(${ADB} shell content read --uri "${content_uri}" 2>&1)
    if [[ -n "$RESULT" ]]; then
        print_success "读取结果："
        echo "$RESULT"
    else
        print_warning "未返回数据"
    fi
}

# ==============================================================================
# 功能 15: 录屏/截屏
# ==============================================================================
func_screen_capture() {
    print_divider
    print_info "【录屏/截屏】"
    print_divider

    echo -e "  ${GREEN}1${NC}. 录屏"
    echo -e "  ${GREEN}2${NC}. 截屏并 pull 到本地"

    read -p "请选择操作 [1-2]: " capture_choice
    case "$capture_choice" in
        1)
            read -p "录屏时长（秒，默认 30）: " duration
            duration=${duration:-30}
            read -p "分辨率（如 1280x720，留空使用默认）: " resolution
            read -p "码率（如 4000000，留空使用默认）: " bitrate

            REMOTE_FILE="/sdcard/screenrecord_$(date +%Y%m%d_%H%M%S).mp4"
            CMD="screenrecord --time-limit ${duration}"
            [[ -n "$resolution" ]] && CMD="${CMD} --size ${resolution}"
            [[ -n "$bitrate" ]] && CMD="${CMD} --bit-rate ${bitrate}"
            CMD="${CMD} ${REMOTE_FILE}"

            print_info "开始录屏（${duration}秒）... 按 Ctrl+C 可提前停止"
            ${ADB} shell ${CMD}
            print_success "录屏完成：${REMOTE_FILE}"

            read -p "是否 pull 到本地？[y/N]: " pull_choice
            if [[ "$pull_choice" == "y" || "$pull_choice" == "Y" ]]; then
                LOCAL_FILE="./$(basename ${REMOTE_FILE})"
                ${ADB} pull ${REMOTE_FILE} ${LOCAL_FILE}
                print_success "已保存到：${LOCAL_FILE}"
            fi
            ;;
        2)
            REMOTE_FILE="/sdcard/screenshot_$(date +%Y%m%d_%H%M%S).png"
            ${ADB} shell screencap -p ${REMOTE_FILE}
            LOCAL_FILE="./$(basename ${REMOTE_FILE})"
            ${ADB} pull ${REMOTE_FILE} ${LOCAL_FILE}
            if [[ $? -eq 0 ]]; then
                print_success "截屏已保存到：${LOCAL_FILE}"
            else
                print_error "截屏失败"
            fi
            ;;
        *)
            print_error "无效选择"
            ;;
    esac
}

# ==============================================================================
# 功能 16: 过度绘制开关
# ==============================================================================
func_overdraw() {
    print_divider
    print_info "【过度绘制开关】"
    print_divider

    echo -e "  ${GREEN}1${NC}. 开启过度绘制检测"
    echo -e "  ${GREEN}2${NC}. 关闭过度绘制检测"

    read -p "请选择 [1/2]: " overdraw_choice
    case "$overdraw_choice" in
        1)
            ${ADB} shell setprop debug.hwui.overdraw show
            print_success "过度绘制检测已开启"
            ;;
        2)
            ${ADB} shell setprop debug.hwui.overdraw false
            print_success "过度绘制检测已关闭"
            ;;
        *)
            print_error "无效选择"
            ;;
    esac
}

# ==============================================================================
# 功能 17: 查看系统音量
# ==============================================================================
func_audio_info() {
    print_divider
    print_info "【查看系统音量】"
    print_divider

    ${ADB} shell dumpsys audio 2>/dev/null | grep -A 5 "STREAM_"
}

# ==============================================================================
# 功能 18: 退出工厂模式
# ==============================================================================
func_exit_factory() {
    print_divider
    print_info "【退出工厂模式】"
    print_divider

    ensure_root || return

    print_info "正在执行退出工厂模式..."
    ${ADB} shell "dumpsys activity service com.alibaba.ailabs.genie.smartapp/.service.SmartAppService exitFactory" 2>/dev/null
    sleep 2

    read -p "是否重启设备？[y/N]: " reboot_choice
    if [[ "$reboot_choice" == "y" || "$reboot_choice" == "Y" ]]; then
        ${ADB} reboot
        print_success "重启命令已发送"
    else
        print_success "退出工厂模式命令已执行"
    fi
}

# ==============================================================================
# 功能 19: ANR 日志导出
# ==============================================================================
func_anr_export() {
    print_divider
    print_info "【ANR 日志导出】"
    print_divider

    LOCAL_DIR="./anr_logs_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "${LOCAL_DIR}"

    print_info "正在导出 /data/anr/ 目录..."
    ${ADB} pull /data/anr/ "${LOCAL_DIR}/"

    if [[ $? -eq 0 ]]; then
        print_success "ANR 日志已导出到：${LOCAL_DIR}"
    else
        print_error "导出失败，可能需要 root 权限"
        ensure_root
        ${ADB} pull /data/anr/ "${LOCAL_DIR}/"
        if [[ $? -eq 0 ]]; then
            print_success "ANR 日志已导出到：${LOCAL_DIR}"
        else
            print_error "导出失败"
        fi
    fi
}

# ==============================================================================
# 功能 20: 日志筛选操作
# ==============================================================================
func_logcat() {
    print_divider
    print_info "【日志筛选操作】"
    print_divider

    echo -e "  ${GREEN}1${NC}. 实时 logcat + 关键字过滤"
    echo -e "  ${GREEN}2${NC}. 从所有缓冲区获取日志"
    echo -e "  ${GREEN}3${NC}. 多关键词筛选（用 | 分隔）"

    read -p "请选择操作 [1-3]: " log_choice
    case "$log_choice" in
        1)
            read -p "请输入过滤关键字（留空显示全部）: " log_keyword
            print_warning "按 Ctrl+C 停止日志输出"
            if [[ -z "$log_keyword" ]]; then
                ${ADB} logcat
            else
                ${ADB} logcat | grep --line-buffered "${log_keyword}"
            fi
            ;;
        2)
            print_info "获取所有缓冲区日志（main/system/crash/events）..."
            print_warning "按 Ctrl+C 停止日志输出"
            ${ADB} logcat -b all
            ;;
        3)
            read -p "请输入多个关键词（用 | 分隔，如 Error|Exception|ANR）: " multi_keywords
            if [[ -z "$multi_keywords" ]]; then
                print_error "关键词不能为空"; return
            fi
            print_warning "按 Ctrl+C 停止日志输出"
            ${ADB} logcat | grep --line-buffered -E "${multi_keywords}"
            ;;
        *)
            print_error "无效选择"
            ;;
    esac
}

# ==============================================================================
# 功能 21: Monkey 测试
# ==============================================================================
func_monkey_test() {
    print_divider
    print_info "【Monkey 测试】"
    print_divider

    read -p "请输入目标包名: " monkey_pkg
    if [[ -z "$monkey_pkg" ]]; then
        print_error "包名不能为空"
        return
    fi

    # 默认参数
    DEFAULT_EVENTS=10000
    DEFAULT_THROTTLE=500
    DEFAULT_SEED=12345

    echo ""
    print_info "默认参数：事件数=${DEFAULT_EVENTS}, 间隔=${DEFAULT_THROTTLE}ms, seed=${DEFAULT_SEED}"
    read -p "事件数（默认 ${DEFAULT_EVENTS}）: " events
    read -p "间隔毫秒（默认 ${DEFAULT_THROTTLE}）: " throttle
    read -p "seed 值（默认 ${DEFAULT_SEED}）: " seed

    events=${events:-$DEFAULT_EVENTS}
    throttle=${throttle:-$DEFAULT_THROTTLE}
    seed=${seed:-$DEFAULT_SEED}

    print_info "开始 Monkey 测试：包=${monkey_pkg}, 事件数=${events}, 间隔=${throttle}ms, seed=${seed}"
    ${ADB} shell monkey -p ${monkey_pkg} -s ${seed} --throttle ${throttle} --ignore-crashes --ignore-timeouts -v ${events}
    print_success "Monkey 测试已完成"
}

# ==============================================================================
# 功能 22: 查看运行时长
# ==============================================================================
func_runtime() {
    print_divider
    print_info "【查看运行时长】"
    print_divider

    read -p "请输入包名关键字: " runtime_keyword
    if [[ -z "$runtime_keyword" ]]; then
        print_error "关键字不能为空"
        return
    fi

    RESULT=$(${ADB} shell "ps -o CMD,ETIME" 2>/dev/null | grep "${runtime_keyword}")
    if [[ -n "$RESULT" ]]; then
        print_success "运行时长信息："
        echo "$RESULT"
    else
        print_warning "未找到匹配 '${runtime_keyword}' 的进程"
    fi
}

# ==============================================================================
# 功能 23: 内存相关
# ==============================================================================
func_memory() {
    print_divider
    print_info "【内存相关】"
    print_divider

    echo -e "  ${GREEN}1${NC}. GC 内存回收查看"
    echo -e "  ${GREEN}2${NC}. 低内存模拟"
    echo -e "  ${GREEN}3${NC}. 查看 app 内存信息"
    echo -e "  ${GREEN}4${NC}. 获取 dumpheap"

    read -p "请选择操作 [1-4]: " mem_choice
    case "$mem_choice" in
        1)
            read -p "请输入包名: " gc_pkg
            if [[ -z "$gc_pkg" ]]; then
                print_error "包名不能为空"; return
            fi
            print_info "发送 GC 信号..."
            ${ADB} shell "kill -10 \$(pidof ${gc_pkg})" 2>/dev/null
            sleep 2
            print_info "查看内存信息..."
            ${ADB} shell dumpsys meminfo ${gc_pkg}
            ;;
        2)
            print_info "模拟低内存环境..."
            ensure_root || return
            ${ADB} shell "echo 1 > /proc/sys/vm/drop_caches"
            print_success "已触发内存回收"
            ;;
        3)
            read -p "请输入包名: " meminfo_pkg
            if [[ -z "$meminfo_pkg" ]]; then
                print_error "包名不能为空"; return
            fi
            ${ADB} shell dumpsys meminfo ${meminfo_pkg}
            ;;
        4)
            read -p "请输入包名: " heap_pkg
            if [[ -z "$heap_pkg" ]]; then
                print_error "包名不能为空"; return
            fi
            REMOTE_FILE="/data/local/tmp/${heap_pkg}_heap_$(date +%Y%m%d_%H%M%S).hprof"
            print_info "正在获取 dumpheap..."
            ${ADB} shell am dumpheap ${heap_pkg} ${REMOTE_FILE}
            sleep 3
            LOCAL_FILE="./$(basename ${REMOTE_FILE})"
            ${ADB} pull ${REMOTE_FILE} ${LOCAL_FILE}
            if [[ $? -eq 0 ]]; then
                print_success "Heap dump 已保存到：${LOCAL_FILE}"
            else
                print_error "Pull 文件失败"
            fi
            ;;
        *)
            print_error "无效选择"
            ;;
    esac
}

# ==============================================================================
# 功能 24: 查找包名
# ==============================================================================
func_find_package() {
    print_divider
    print_info "【查找包名】"
    print_divider

    read -p "请输入包名关键字: " pkg_keyword
    if [[ -z "$pkg_keyword" ]]; then
        print_error "关键字不能为空"
        return
    fi

    RESULT=$(${ADB} shell "pm list packages" 2>/dev/null | grep -i "${pkg_keyword}")
    if [[ -n "$RESULT" ]]; then
        print_success "匹配的包名列表："
        echo "$RESULT"
    else
        print_warning "未找到匹配 '${pkg_keyword}' 的包名"
    fi
}

# ==============================================================================
# 功能 25: 模拟电源键
# ==============================================================================
func_power_key() {
    print_divider
    print_info "【模拟电源键】"
    print_divider

    ${ADB} shell input keyevent 26
    if [[ $? -eq 0 ]]; then
        print_success "已模拟电源键按下"
    else
        print_error "模拟电源键失败"
    fi
}

# ==============================================================================
# 主菜单显示
# ==============================================================================
show_menu() {
    clear
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    print_center_line "ADB 综合工具箱 v1.0" 64
    print_center_line "设备: ${SELECTED_DEVICE}" 64
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""
    echo -e "  ${GREEN} 1${NC}. 切换网络环境（线上/预发1/预发2）"
    echo -e "  ${GREEN} 2${NC}. 设置埋点UT实时上报验证"
    echo -e "  ${GREEN} 3${NC}. 横竖屏切换"
    echo -e "  ${GREEN} 4${NC}. 屏幕分辨率修改"
    echo -e "  ${GREEN} 5${NC}. 发送语音命令"
    echo -e "  ${GREEN} 6${NC}. 关闭模拟器"
    echo -e "  ${GREEN} 7${NC}. 查看顶层 Activity"
    echo -e "  ${GREEN} 8${NC}. 查看 app 版本号"
    echo -e "  ${GREEN} 9${NC}. 查询设备 UUID"
    echo -e "  ${GREEN}10${NC}. 进程管理（查看/kill/强制停止）"
    echo -e "  ${GREEN}11${NC}. 打开 Deeplink"
    echo -e "  ${GREEN}12${NC}. 打开应用/管理应用"
    echo -e "  ${GREEN}13${NC}. 系统属性管理（settings/prop）"
    echo -e "  ${GREEN}14${NC}. Content Provider 读取"
    echo -e "  ${GREEN}15${NC}. 录屏/截屏"
    echo -e "  ${GREEN}16${NC}. 过度绘制开关"
    echo -e "  ${GREEN}17${NC}. 查看系统音量"
    echo -e "  ${GREEN}18${NC}. 退出工厂模式"
    echo -e "  ${GREEN}19${NC}. ANR 日志导出"
    echo -e "  ${GREEN}20${NC}. 日志筛选操作"
    echo -e "  ${GREEN}21${NC}. Monkey 测试"
    echo -e "  ${GREEN}22${NC}. 查看运行时长"
    echo -e "  ${GREEN}23${NC}. 内存相关（GC/低内存/meminfo/dumpheap）"
    echo -e "  ${GREEN}24${NC}. 查找包名"
    echo -e "  ${GREEN}25${NC}. 模拟电源键"
    echo ""
    echo -e "  ${RED} 0${NC}. 退出"
    echo ""
    print_divider
}

# ==============================================================================
# 主程序入口
# ==============================================================================

# 启动时执行一次设备检测与选择
detect_and_select_device
echo ""

# 主循环
while true; do
    show_menu
    read -p "请输入功能编号 [0-25]: " menu_choice

    case "$menu_choice" in
        1)  func_switch_env ;;
        2)  func_ut_realtime ;;
        3)  func_rotation ;;
        4)  func_resolution ;;
        5)  func_voice_command ;;
        6)  func_close_emulator ;;
        7)  func_top_activity ;;
        8)  func_app_version ;;
        9)  func_device_uuid ;;
        10) func_process_manage ;;
        11) func_deeplink ;;
        12) func_app_manage ;;
        13) func_system_property ;;
        14) func_content_read ;;
        15) func_screen_capture ;;
        16) func_overdraw ;;
        17) func_audio_info ;;
        18) func_exit_factory ;;
        19) func_anr_export ;;
        20) func_logcat ;;
        21) func_monkey_test ;;
        22) func_runtime ;;
        23) func_memory ;;
        24) func_find_package ;;
        25) func_power_key ;;
        0)
            echo ""
            print_success "感谢使用 ADB 综合工具箱，再见！"
            exit 0
            ;;
        *)
            print_error "无效的选择，请输入 0 到 25 之间的数字"
            ;;
    esac

    # 操作完成后等待用户确认返回主菜单
    wait_return_menu
done
