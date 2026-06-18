#!/bin/bash
###############################################################################
# monitor_idle_timeout.sh
#
# 功能：通过 adb 实时监控 Android 设备的 idle 时长与 screen_off_timeout 变化
#
# 用法：
#   ./monitor_idle_timeout.sh                # 默认 2 秒刷新一次
#   ./monitor_idle_timeout.sh -i 1           # 自定义刷新间隔为 1 秒
#   ./monitor_idle_timeout.sh -d             # 调试模式（打印 dumpsys power 关键原始输出）
#   ./monitor_idle_timeout.sh -h             # 查看帮助
#
# 输出：
#   - 终端实时显示监控信息
#   - 同步写入日志文件 idle_monitor_<日期时间>.log
#
# 退出：
#   - Ctrl+C 优雅退出，并显示日志文件路径
###############################################################################

# ---------------------------- 颜色定义 ---------------------------------------
COLOR_RESET="\033[0m"
COLOR_RED="\033[1;31m"
COLOR_GREEN="\033[1;32m"
COLOR_YELLOW="\033[1;33m"
COLOR_BLUE="\033[1;34m"
COLOR_MAGENTA="\033[1;35m"
COLOR_CYAN="\033[1;36m"
COLOR_BOLD="\033[1m"

# ---------------------------- 默认参数 ---------------------------------------
INTERVAL=2
DEBUG=0
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="${SCRIPT_DIR}/idle_monitor_$(date +%Y%m%d_%H%M%S).log"

# ---------------------------- 帮助信息 ---------------------------------------
print_help() {
    cat <<EOF
用法: $(basename "$0") [选项]

选项:
  -i <interval>   设置刷新间隔（秒），默认 2 秒
  -d              开启调试模式，打印 dumpsys power 关键原始输出
  -h              显示帮助信息

示例:
  $(basename "$0")
  $(basename "$0") -i 1
  $(basename "$0") -d
EOF
}

# ---------------------------- 参数解析 ---------------------------------------
while getopts ":i:dh" opt; do
    case "$opt" in
        i)
            if ! [[ "$OPTARG" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
                echo -e "${COLOR_RED}错误：-i 参数必须是数字，收到的值: $OPTARG${COLOR_RESET}" >&2
                exit 1
            fi
            INTERVAL="$OPTARG"
            ;;
        d)
            DEBUG=1
            ;;
        h)
            print_help
            exit 0
            ;;
        \?)
            echo -e "${COLOR_RED}未知参数: -$OPTARG${COLOR_RESET}" >&2
            print_help
            exit 1
            ;;
        :)
            echo -e "${COLOR_RED}参数 -$OPTARG 需要指定值${COLOR_RESET}" >&2
            exit 1
            ;;
    esac
done

# ---------------------------- 启动检查 ---------------------------------------
check_environment() {
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
    if [[ "$device_count" -gt 1 ]]; then
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
        local selected_device="${device_array[$((selection-1))]}"
        export ANDROID_SERIAL="$selected_device"
        echo -e "${COLOR_GREEN}[已选择] 将监控设备: ${selected_device}${COLOR_RESET}"
        echo ""
    fi
}

# ---------------------------- 日志输出 ---------------------------------------
# 同时输出到终端（带颜色）和日志文件（去除颜色）
log_line() {
    local line="$1"
    echo -e "$line"
    # 去除颜色码后写入日志
    echo -e "$line" | sed -E 's/\x1B\[[0-9;]*[mK]//g' >> "$LOG_FILE"
}

# ---------------------------- 工具函数 ---------------------------------------
# 去除字符串首尾空白和回车
trim() {
    local v="$1"
    # 去掉 \r、前后空白
    v="${v//$'\r'/}"
    v="${v#"${v%%[![:space:]]*}"}"
    v="${v%"${v##*[![:space:]]}"}"
    printf '%s' "$v"
}

# 数字校验（支持负数）
is_int() {
    [[ "$1" =~ ^-?[0-9]+$ ]]
}

# 将 wakefulness 数字代码转换为字符串（部分 ROM 输出 getWakefulnessLocked()=1）
wakefulness_code_to_str() {
    case "$1" in
        0) echo "Asleep"   ;;
        1) echo "Awake"    ;;
        2) echo "Dreaming" ;;
        3) echo "Dozing"   ;;
        *) echo "$1"       ;;
    esac
}

# ---------------------------- 数据获取 ---------------------------------------
# 一次性抓取 dumpsys power 的全部输出（缓存到全局变量），减少重复 adb 调用
DUMPSYS_POWER_RAW=""
fetch_dumpsys_power() {
    DUMPSYS_POWER_RAW=$(adb shell dumpsys power 2>/dev/null | tr -d '\r')
}

# 从已抓取的 dumpsys power 输出中提取 lastUserActivityTime（毫秒）
# 兼容格式：
#   1) lastUserActivityTime=14515261 (1597954 ms ago)
#   2) mLastUserActivityTime=123456789
#   3) mLastUserActivityTime(excludingAttention)=14515261
#   4) Last user activity time: 123456789
get_last_user_activity_time() {
    local line val
    # 优先匹配 lastUserActivityTime=xxx (不带 m 前缀，不带 NoChangeLights 后缀)
    line=$(printf '%s\n' "$DUMPSYS_POWER_RAW" | grep -E "^[[:space:]]*lastUserActivityTime=" | head -n 1)
    if [[ -n "$line" ]]; then
        val=$(printf '%s' "$line" | grep -oE '=[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -n 1)
        if [[ -n "$val" ]]; then
            printf '%s' "$val"
            return
        fi
    fi
    # 匹配 mLastUserActivityTime(excludingAttention)=xxx 或 mLastUserActivityTime=xxx
    line=$(printf '%s\n' "$DUMPSYS_POWER_RAW" | grep -E "mLastUserActivityTime" | head -n 1)
    if [[ -n "$line" ]]; then
        val=$(printf '%s' "$line" | grep -oE '=[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -n 1)
        if [[ -n "$val" ]]; then
            printf '%s' "$val"
            return
        fi
    fi
    # 备用：Last user activity time: 123
    line=$(printf '%s\n' "$DUMPSYS_POWER_RAW" | grep -iE "Last user activity( time)?[[:space:]]*[:=]" | head -n 1)
    if [[ -n "$line" ]]; then
        val=$(printf '%s' "$line" | grep -oE '\-?[0-9]+' | head -n 1)
        printf '%s' "$val"
        return
    fi
    printf ''
}

# 获取 mWakefulness 状态
# 兼容格式：
#   1) mWakefulness=Awake
#   2) Wakefulness=Awake
#   3) getWakefulnessLocked()=1
#   4) mWakefulness=1
get_wakefulness() {
    local line val
    # 1) mWakefulness=Xxx
    line=$(printf '%s\n' "$DUMPSYS_POWER_RAW" | grep -E "mWakefulness[[:space:]]*=" | head -n 1)
    if [[ -n "$line" ]]; then
        val=$(printf '%s' "$line" | sed -nE 's/.*mWakefulness[[:space:]]*=[[:space:]]*([A-Za-z0-9_]+).*/\1/p')
        if [[ -n "$val" ]]; then
            if [[ "$val" =~ ^[0-9]+$ ]]; then
                wakefulness_code_to_str "$val"
            else
                printf '%s' "$val"
            fi
            return
        fi
    fi
    # 2) getWakefulnessLocked()=1 / =Awake
    line=$(printf '%s\n' "$DUMPSYS_POWER_RAW" | grep -E "getWakefulnessLocked\(\)[[:space:]]*=" | head -n 1)
    if [[ -n "$line" ]]; then
        val=$(printf '%s' "$line" | sed -nE 's/.*getWakefulnessLocked\(\)[[:space:]]*=[[:space:]]*([A-Za-z0-9_]+).*/\1/p')
        if [[ -n "$val" ]]; then
            if [[ "$val" =~ ^[0-9]+$ ]]; then
                wakefulness_code_to_str "$val"
            else
                printf '%s' "$val"
            fi
            return
        fi
    fi
    # 3) Wakefulness=Xxx（部分 ROM 去除前缀 m）
    line=$(printf '%s\n' "$DUMPSYS_POWER_RAW" | grep -iE "(^|[^A-Za-z])Wakefulness[[:space:]]*[:=]" | head -n 1)
    if [[ -n "$line" ]]; then
        val=$(printf '%s' "$line" | sed -nE 's/.*[Ww]akefulness[[:space:]]*[:=][[:space:]]*([A-Za-z0-9_]+).*/\1/p')
        if [[ -n "$val" ]]; then
            if [[ "$val" =~ ^[0-9]+$ ]]; then
                wakefulness_code_to_str "$val"
            else
                printf '%s' "$val"
            fi
            return
        fi
    fi
    printf ''
}

# 获取 mScreenOffTimeoutSetting（毫秒）
# 兼容字段：mScreenOffTimeoutSetting / mScreenOffTimeout / Screen off timeout setting
get_screen_off_timeout_setting() {
    local line val
    for key in "mScreenOffTimeoutSetting" "mScreenOffTimeout" "mUserActivityTimeoutOverrideFromWindowManager"; do
        line=$(printf '%s\n' "$DUMPSYS_POWER_RAW" | grep -E "${key}[[:space:]]*=" | head -n 1)
        if [[ -n "$line" ]]; then
            val=$(printf '%s' "$line" | grep -oE "${key}[[:space:]]*=[[:space:]]*-?[0-9]+" \
                  | grep -oE '\-?[0-9]+$' | head -n 1)
            if [[ -n "$val" ]]; then
                printf '%s' "$val"
                return
            fi
        fi
    done
    printf ''
}

# 从 settings get system 获取 screen_off_timeout 当前值（毫秒）
get_screen_off_timeout() {
    local v
    v=$(adb shell settings get system screen_off_timeout 2>/dev/null)
    v=$(trim "$v")
    # 部分设备 settings 取不到时返回 "null"
    if [[ "$v" == "null" ]]; then
        printf ''
        return
    fi
    printf '%s' "$v"
}

# 获取设备 uptime（毫秒）
get_uptime_ms() {
    # /proc/uptime 第一列是秒（带小数），转换为毫秒
    local uptime_sec
    uptime_sec=$(adb shell cat /proc/uptime 2>/dev/null | awk '{print $1}')
    uptime_sec=$(trim "$uptime_sec")
    if [[ -z "$uptime_sec" ]]; then
        printf ''
        return
    fi
    # 必须是数字（含小数点）
    if ! [[ "$uptime_sec" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
        printf ''
        return
    fi
    awk -v s="$uptime_sec" 'BEGIN { printf "%.0f", s * 1000 }'
}

# ---------------------------- 调试输出 ---------------------------------------
print_debug_dump() {
    log_line "${COLOR_YELLOW}--- [DEBUG] dumpsys power 关键原始行 ---${COLOR_RESET}"
    local matched
    matched=$(printf '%s\n' "$DUMPSYS_POWER_RAW" \
        | grep -iE "mLastUserActivityTime|mWakefulness|Wakefulness|getWakefulnessLocked|mScreenOffTimeout|mUserActivityTimeout|mHoldingDisplaySuspendBlocker|Last user activity")
    if [[ -z "$matched" ]]; then
        log_line "${COLOR_RED}  (未匹配到任何关键字段，可能 dumpsys 抓取失败或字段命名差异较大)${COLOR_RESET}"
        log_line "${COLOR_YELLOW}  dumpsys power 前 30 行原始内容：${COLOR_RESET}"
        printf '%s\n' "$DUMPSYS_POWER_RAW" | head -n 30 | while IFS= read -r l; do
            log_line "    $l"
        done
    else
        printf '%s\n' "$matched" | while IFS= read -r l; do
            log_line "    $l"
        done
    fi
    log_line "${COLOR_YELLOW}-----------------------------------------${COLOR_RESET}"
}

# ---------------------------- 格式化函数 -------------------------------------
# 将 timeout 毫秒值格式化为可读字符串
format_timeout() {
    local v="$1"
    if [[ -z "$v" || "$v" == "null" ]]; then
        echo "(未知)"
        return
    fi
    case "$v" in
        2147483647)
            echo "${v} (MAX/永不息屏)"
            ;;
        180000)
            echo "${v} (3分钟)"
            ;;
        *)
            if [[ "$v" =~ ^-?[0-9]+$ ]]; then
                if [[ "$v" -ge 60000 ]]; then
                    local minutes
                    minutes=$(awk -v ms="$v" 'BEGIN { printf "%.1f", ms / 60000 }')
                    echo "${v} (${minutes}分钟)"
                else
                    local seconds
                    seconds=$(awk -v ms="$v" 'BEGIN { printf "%.1f", ms / 1000 }')
                    echo "${v} (${seconds}秒)"
                fi
            else
                echo "$v"
            fi
            ;;
    esac
}

# 格式化 idle 时长（毫秒 -> "xxxx ms (x.xs)"）
format_idle_duration() {
    local ms="$1"
    if [[ -z "$ms" || ! "$ms" =~ ^-?[0-9]+$ ]]; then
        echo "(未知)"
        return
    fi
    local seconds
    seconds=$(awk -v m="$ms" 'BEGIN { printf "%.1f", m / 1000 }')
    echo "${ms} ms (${seconds}s)"
}

# ---------------------------- 主循环 -----------------------------------------
main_loop() {
    local prev_timeout=""
    local prev_wakefulness=""
    local first_round=1

    log_line "${COLOR_CYAN}===================================================${COLOR_RESET}"
    log_line "${COLOR_BOLD}Android Idle Timeout 监控已启动${COLOR_RESET}"
    log_line "  刷新间隔 : ${INTERVAL} 秒"
    log_line "  调试模式 : $([[ $DEBUG -eq 1 ]] && echo '开启' || echo '关闭')"
    log_line "  日志路径 : ${LOG_FILE}"
    log_line "  按 Ctrl+C 退出"
    log_line "${COLOR_CYAN}===================================================${COLOR_RESET}"

    while true; do
        local ts
        ts=$(date '+%Y-%m-%d %H:%M:%S')

        # 一次抓取，多次解析，避免不一致与重复开销
        fetch_dumpsys_power

        local last_activity uptime_ms timeout_setting wakefulness timeout_now
        last_activity=$(trim "$(get_last_user_activity_time)")
        uptime_ms=$(trim "$(get_uptime_ms)")
        timeout_setting=$(trim "$(get_screen_off_timeout_setting)")
        wakefulness=$(trim "$(get_wakefulness)")
        timeout_now=$(trim "$(get_screen_off_timeout)")

        # 计算 idle 时长
        local idle_ms=""
        if is_int "$uptime_ms" && is_int "$last_activity"; then
            idle_ms=$(( uptime_ms - last_activity ))
        fi

        # 变化检测
        local timeout_tag=""
        if [[ -n "$prev_timeout" && -n "$timeout_now" && "$prev_timeout" != "$timeout_now" ]]; then
            timeout_tag="${COLOR_RED}[CHANGED: ${prev_timeout} -> ${timeout_now}]${COLOR_RESET}"
        fi

        local wake_tag=""
        if [[ -n "$prev_wakefulness" && -n "$wakefulness" && "$prev_wakefulness" != "$wakefulness" ]]; then
            wake_tag="${COLOR_RED}[CHANGED: ${prev_wakefulness} -> ${wakefulness}]${COLOR_RESET}"
        fi

        # idle 超过 timeout 警告
        local warn_tag=""
        if is_int "$idle_ms" && is_int "$timeout_now" && [[ "$timeout_now" -gt 0 ]]; then
            if [[ "$idle_ms" -gt "$timeout_now" && "$timeout_now" -ne 2147483647 ]]; then
                warn_tag="${COLOR_RED}⚠ Idle 已超过 ScreenOffTimeout!${COLOR_RESET}"
            fi
        fi

        # Wakefulness 颜色
        local wake_color="$COLOR_GREEN"
        case "$wakefulness" in
            Awake)    wake_color="$COLOR_GREEN"   ;;
            Dozing)   wake_color="$COLOR_YELLOW"  ;;
            Asleep)   wake_color="$COLOR_MAGENTA" ;;
            Dreaming) wake_color="$COLOR_BLUE"    ;;
            *)        wake_color="$COLOR_RESET"   ;;
        esac

        log_line ""
        log_line "${COLOR_CYAN}================== ${ts} ==================${COLOR_RESET}"
        log_line "  Wakefulness     : ${wake_color}${wakefulness:-未知}${COLOR_RESET} ${wake_tag}"
        log_line "  LastUserActivity: ${last_activity:-未知} ms"
        log_line "  Current Uptime  : ${uptime_ms:-未知} ms"
        log_line "  Idle Duration   : $(format_idle_duration "$idle_ms")"
        log_line "  ScreenOffTimeout: $(format_timeout "$timeout_now") ${timeout_tag}"
        log_line "  Timeout Setting : $(format_timeout "$timeout_setting")"
        if [[ -n "$warn_tag" ]]; then
            log_line "  ${warn_tag}"
        fi

        # 调试模式：打印 dumpsys power 关键原始输出
        if [[ "$DEBUG" -eq 1 ]]; then
            print_debug_dump
        fi

        log_line "${COLOR_CYAN}----------------------------------------------------------${COLOR_RESET}"

        # 更新前值（首轮不视为变化）
        if [[ "$first_round" -eq 1 ]]; then
            first_round=0
        fi
        prev_timeout="$timeout_now"
        prev_wakefulness="$wakefulness"

        sleep "$INTERVAL"
    done
}

# ---------------------------- 退出处理 ---------------------------------------
cleanup() {
    echo
    log_line ""
    log_line "${COLOR_CYAN}===================================================${COLOR_RESET}"
    log_line "${COLOR_GREEN}监控已停止 - $(date '+%Y-%m-%d %H:%M:%S')${COLOR_RESET}"
    log_line "${COLOR_GREEN}日志文件已保存: ${LOG_FILE}${COLOR_RESET}"
    log_line "${COLOR_CYAN}===================================================${COLOR_RESET}"
    exit 0
}
trap cleanup INT TERM

# ---------------------------- 入口 -------------------------------------------
check_environment
# 创建/初始化日志文件
: > "$LOG_FILE"
main_loop
