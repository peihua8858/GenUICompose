#!/bin/bash
# 强制使用 C locale，避免日志中包含非 UTF-8 字符时 AWK 报错
# "multibyte conversion failure"，确保按字节级别处理文本
export LC_ALL=C
export LANG=C

# =============================================================================
# filter_crash_logs.sh
# 用途：扫描指定文件夹下所有日志文件，提取崩溃异常信息并去重输出
# 兼容：macOS / Linux (bash 3.2+)
# 用法：./filter_crash_logs.sh <日志文件夹路径>
# 输出：当前目录下的 crash_report.txt
# =============================================================================

set -euo pipefail

# ----------------------------- 参数校验 --------------------------------------
if [[ $# -lt 1 ]]; then
    echo "用法: $0 <日志文件夹路径>"
    echo "示例: $0 /path/to/logs"
    exit 1
fi

LOG_DIR="$1"

if [[ ! -d "$LOG_DIR" ]]; then
    echo "错误: 文件夹 '$LOG_DIR' 不存在或不是目录"
    exit 1
fi

# ----------------------------- 查找日志文件 -----------------------------------
# 递归查找 .log 和 .txt 文件
LOG_FILES=()
while IFS= read -r -d '' file; do
    LOG_FILES+=("$file")
done < <(find "$LOG_DIR" -type f \( -name "*.log" -o -name "*.txt" \) -print0 2>/dev/null)

if [[ ${#LOG_FILES[@]} -eq 0 ]]; then
    echo "警告: 在 '$LOG_DIR' 下未找到 .log 或 .txt 日志文件"
    exit 0
fi

echo "找到 ${#LOG_FILES[@]} 个日志文件，开始扫描..."

# ----------------------------- 输出文件 ---------------------------------------
OUTPUT_FILE="./crash_report.txt"
TEMP_DIR="./.filter_crash_tmp_$$"
mkdir -p "$TEMP_DIR"
trap "rm -rf '$TEMP_DIR'" EXIT

# ----------------------------- 提取崩溃信息 -----------------------------------
# 使用 awk 提取崩溃堆栈
# 支持的日志格式：
#   1. logcat 标准格式: "06-22 10:30:45.123  1234  5678 E AndroidRuntime: ..."
#   2. 简短 logcat 格式: "E/AndroidRuntime(1234): ..."
#   3. 纯堆栈格式: "java.lang.NullPointerException: ..."
#   4. YYYY-MM-DD 格式: "2026-06-22 10:30:45.123 1234-5678/com.app E/Tag: ..."

awk '
# 函数：提取日志级别（E/D/W/I/V），返回单字母或空串（无法识别时）
function get_log_level(line,    fields, n) {
    # 格式1: 标准 logcat "MM-DD HH:MM:SS.mmm  PID  TID L Tag: content"
    if (line ~ /^[0-9]{2}-[0-9]{2} +[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]+/) {
        n = split(line, fields, /[[:space:]]+/)
        # fields[1]=date, fields[2]=time, fields[3]=PID, fields[4]=TID, fields[5]=level
        if (n >= 5 && fields[5] ~ /^[A-Z]$/) {
            return fields[5]
        }
    }

    # 格式2: YYYY-MM-DD 格式 "2026-06-22 10:30:45.123 PID-TID/pkg L/Tag: content"
    if (line ~ /^[0-9]{4}-[0-9]{2}-[0-9]{2} +[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]+/) {
        n = split(line, fields, /[[:space:]]+/)
        # fields[1]=date, fields[2]=time, fields[3]=PID-TID/pkg, fields[4]=L/Tag:
        if (n >= 4 && fields[4] ~ /^[A-Z]\//) {
            return substr(fields[4], 1, 1)
        }
    }

    # 格式3: "E/Tag(pid): content" 或 "E/Tag: content"
    if (line ~ /^[A-Z]\//) {
        return substr(line, 1, 1)
    }

    # 无法识别级别（纯堆栈行或未知格式）
    return ""
}

# 函数：提取日志 Tag（返回纯 Tag 名称，无法识别时返回空串）
function get_tag(line,    fields, n, tag) {
    # 格式1: 标准 logcat "MM-DD HH:MM:SS.mmm  PID  TID L Tag: content"
    if (line ~ /^[0-9]{2}-[0-9]{2} +[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]+/) {
        n = split(line, fields, /[[:space:]]+/)
        if (n >= 6) {
            tag = fields[6]
            # 去掉末尾的冒号
            sub(/:$/, "", tag)
            return tag
        }
    }

    # 格式2: YYYY-MM-DD 格式 "2026-06-22 10:30:45.123 PID-TID/pkg L/Tag: content"
    if (line ~ /^[0-9]{4}-[0-9]{2}-[0-9]{2} +[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]+/) {
        n = split(line, fields, /[[:space:]]+/)
        if (n >= 4 && fields[4] ~ /^[A-Z]\//) {
            tag = fields[4]
            # 去掉前面的 "L/" 前缀
            sub(/^[A-Z]\//, "", tag)
            # 去掉末尾的冒号
            sub(/:$/, "", tag)
            return tag
        }
    }

    # 格式3: "E/Tag(pid): content"
    if (line ~ /^[A-Z]\/[^(]+\([0-9]+\):/) {
        tag = line
        sub(/^[A-Z]\//, "", tag)
        sub(/\(.*/, "", tag)
        return tag
    }

    # 格式4: "E/Tag: content"
    if (line ~ /^[A-Z]\/[^:]+:/) {
        tag = line
        sub(/^[A-Z]\//, "", tag)
        sub(/:.*/, "", tag)
        return tag
    }

    return ""
}

# 函数：去掉 logcat 格式前缀，返回纯内容
function strip_prefix(line,    result) {
    result = line

    # 格式1: 标准 logcat "MM-DD HH:MM:SS.mmm  PID  TID L Tag: content"
    if (match(result, /^[0-9]{2}-[0-9]{2} +[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]+ +[0-9]+ +[0-9]+ +[A-Z] +[^:]+: */)) {
        return substr(result, RSTART + RLENGTH)
    }

    # 格式2: YYYY-MM-DD 格式 "2026-06-22 10:30:45.123 PID-TID/pkg L/Tag: content"
    if (match(result, /^[0-9]{4}-[0-9]{2}-[0-9]{2} +[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]+ +[0-9]+-[0-9]+\/[^ ]+ +[A-Z]\/[^:]+: */)) {
        return substr(result, RSTART + RLENGTH)
    }

    # 格式3: "E/Tag(pid): content"
    if (match(result, /^[A-Z]\/[^(]+\([0-9]+\): */)) {
        return substr(result, RSTART + RLENGTH)
    }

    # 格式4: "E/Tag: content"
    if (match(result, /^[A-Z]\/[^:]+: */)) {
        return substr(result, RSTART + RLENGTH)
    }

    return result
}

# 函数：判断当前行是否是堆栈的延续行
function is_stack_line(line,    cleaned) {
    cleaned = strip_prefix(line)
    # "at " 开头（带前导空白或tab）
    if (cleaned ~ /^[[:space:]]+(at |\.\.\.)[[:space:]]/) return 1
    # "\tat " 格式
    if (cleaned ~ /^\tat /) return 1
    # "Caused by: " 开头
    if (cleaned ~ /^[[:space:]]*Caused by:/) return 1
    # "... N more" 格式
    if (cleaned ~ /^[[:space:]]*\.\.\. [0-9]+ more/) return 1
    return 0
}

# 函数：判断是否是异常头行（ExceptionClass: msg 或 ExceptionClass）
function is_exception_header(line,    cleaned) {
    cleaned = strip_prefix(line)
    # 去掉前导空白
    gsub(/^[[:space:]]+/, "", cleaned)
    # 匹配 "包名.XxxException: message" 或 "包名.XxxException" 或 "包名.XxxError"
    if (cleaned ~ /^([a-zA-Z_$][a-zA-Z0-9_$]*\.)*[a-zA-Z_$][a-zA-Z0-9_$]*(Exception|Error)(:|[[:space:]]*$)/) return 1
    return 0
}

# 函数：生成去重指纹（去除时间戳、PID等可变部分）
function make_fingerprint(block,    n, lines, fp, i, cleaned) {
    n = split(block, lines, "\n")
    fp = ""
    for (i = 1; i <= n; i++) {
        cleaned = strip_prefix(lines[i])
        # 去掉前后空白
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", cleaned)
        # 跳过 FATAL EXCEPTION 行（线程名可能不同但异常相同）
        if (cleaned ~ /^FATAL EXCEPTION:/) continue
        # 规范化 Process 行（忽略 PID 数字差异）
        if (cleaned ~ /^Process:/) {
            sub(/PID: [0-9]+/, "PID: X", cleaned)
        }
        if (cleaned != "") {
            fp = fp cleaned "\n"
        }
    }
    return fp
}

# 函数：保存当前崩溃块
function save_crash() {
    if (crash_block == "") return
    # 至少有2行（异常头 + 堆栈）才算有效
    if (crash_lines < 2) {
        crash_block = ""
        in_crash = 0
        crash_lines = 0
        return
    }

    crash_count++
    fp = make_fingerprint(crash_block)

    if (!(fp in seen_fps)) {
        seen_fps[fp] = 1
        unique_count++
        unique_crashes[unique_count] = crash_block
    }

    crash_block = ""
    in_crash = 0
    crash_lines = 0
}

BEGIN {
    in_crash = 0
    crash_block = ""
    crash_count = 0
    unique_count = 0
    crash_lines = 0
}

{
    # -------- 日志级别过滤 --------
    # 提取当前行的日志级别
    _level = get_log_level($0)

    # 如果能识别出级别且不是 E (Error)，则跳过该行
    if (_level != "" && _level != "E") {
        # 如果当前正在收集崩溃块，非 E 级别行意味着崩溃上下文结束
        if (in_crash) save_crash()
        next
    }

    # -------- Tag 过滤：排除 LogUtils --------
    _tag = get_tag($0)
    if (_tag == "LogUtils") {
        # 如果当前正在收集崩溃块，先保存已收集的内容
        if (in_crash) save_crash()
        next
    }

    # -------- 状态机处理（仅处理 E 级别或无级别的行） --------

    # 如果检测到 FATAL EXCEPTION，开始新崩溃块
    if ($0 ~ /FATAL EXCEPTION/) {
        if (in_crash) save_crash()
        in_crash = 1
        crash_block = $0
        crash_lines = 1
        next
    }

    # 正在收集崩溃块中
    if (in_crash) {
        if (is_stack_line($0) || is_exception_header($0)) {
            crash_block = crash_block "\n" $0
            crash_lines++
            next
        }
        # 检查是否是 AndroidRuntime 标签的附加信息（Process 行等）
        if ($0 ~ /AndroidRuntime/) {
            cleaned_line = strip_prefix($0)
            if (cleaned_line ~ /^Process:|^PID:/) {
                crash_block = crash_block "\n" $0
                crash_lines++
                next
            }
        }
        # 当前行不属于崩溃块，保存并结束收集
        save_crash()
        # 注意：不要 next，继续检查当前行是否是新异常的开头
    }

    # 未在崩溃块中，检测独立异常头（纯堆栈格式）
    if (!in_crash && is_exception_header($0)) {
        cleaned_line = strip_prefix($0)
        gsub(/^[[:space:]]+/, "", cleaned_line)
        # 排除 Caused by 独立出现
        if (cleaned_line !~ /^Caused by:/) {
            in_crash = 1
            crash_block = $0
            crash_lines = 1
            next
        }
    }
}

END {
    if (in_crash) save_crash()

    # 输出去重后的崩溃信息
    for (i = 1; i <= unique_count; i++) {
        if (i > 1) {
            print ""
            print "================================================================================"
            print ""
        }
        print unique_crashes[i]
    }
    print ""

    # 统计信息输出到 stderr
    printf "\n统计信息:\n" > "/dev/stderr"
    printf "  共发现崩溃: %d 个\n", crash_count > "/dev/stderr"
    printf "  去重后保留: %d 个\n", unique_count > "/dev/stderr"
}
' "${LOG_FILES[@]}" > "$OUTPUT_FILE" 2>"$TEMP_DIR/stats.txt"

# ----------------------------- 输出统计信息 -----------------------------------
if [[ -f "$TEMP_DIR/stats.txt" ]]; then
    cat "$TEMP_DIR/stats.txt"
fi

# 检查是否找到崩溃
if [[ ! -s "$OUTPUT_FILE" ]]; then
    echo "未发现任何崩溃异常信息。"
    rm -f "$OUTPUT_FILE"
else
    echo "结果已输出到: $OUTPUT_FILE"
fi
