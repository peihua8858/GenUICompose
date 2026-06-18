@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: monitor_idle_timeout.bat
::
:: 功能：通过 adb 实时监控 Android 设备的 idle 时长与 screen_off_timeout 变化
::
:: 用法：
::   monitor_idle_timeout.bat                  默认 2 秒刷新一次
::   monitor_idle_timeout.bat -i 1             自定义刷新间隔为 1 秒
::   monitor_idle_timeout.bat -d               调试模式
::   monitor_idle_timeout.bat -h               查看帮助
::
:: 退出：Ctrl+C 退出并显示日志文件路径
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:: ---------------------------- ESC 字符生成 ----------------------------------
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"

:: ---------------------------- 颜色定义 --------------------------------------
set "COLOR_RESET=%ESC%[0m"
set "COLOR_RED=%ESC%[1;31m"
set "COLOR_GREEN=%ESC%[1;32m"
set "COLOR_YELLOW=%ESC%[1;33m"
set "COLOR_BLUE=%ESC%[1;34m"
set "COLOR_MAGENTA=%ESC%[1;35m"
set "COLOR_CYAN=%ESC%[1;36m"
set "COLOR_BOLD=%ESC%[1m"

:: ---------------------------- 默认参数 --------------------------------------
set "INTERVAL=2"
set "DEBUG=0"
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

:: 生成时间戳用于日志文件名
call :get_timestamp_for_filename TIMESTAMP_FN
set "LOG_FILE=%SCRIPT_DIR%\idle_monitor_%TIMESTAMP_FN%.log"

:: 临时文件
set "TEMP_DUMP=%TEMP%\idle_monitor_dump_%RANDOM%.tmp"
set "TEMP_DEVICES=%TEMP%\idle_monitor_devs_%RANDOM%.tmp"

:: ---------------------------- 参数解析 --------------------------------------
:parse_args
if "%~1"=="" goto :args_done
if "%~1"=="-h" goto :print_help
if "%~1"=="--help" goto :print_help
if "%~1"=="-d" (
    set "DEBUG=1"
    shift
    goto :parse_args
)
if "%~1"=="-i" (
    if "%~2"=="" (
        echo %COLOR_RED%错误：-i 参数需要指定值%COLOR_RESET%
        exit /b 1
    )
    set "INTERVAL=%~2"
    shift
    shift
    goto :parse_args
)
echo %COLOR_RED%未知参数: %~1%COLOR_RESET%
call :print_help
exit /b 1

:args_done

:: ---------------------------- 启动检查 --------------------------------------
call :check_environment
if errorlevel 1 exit /b 1

:: 清空日志文件
type nul > "%LOG_FILE%"

:: 主循环
goto :main_loop

:: ============================================================================
:: 子程序定义
:: ============================================================================

:: ---------------------------- 帮助信息 --------------------------------------
:print_help
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo   -i ^<interval^>   设置刷新间隔（秒），默认 2 秒
echo   -d              开启调试模式，打印 dumpsys power 关键原始输出
echo   -h              显示帮助信息
echo.
echo 示例:
echo   %~nx0
echo   %~nx0 -i 1
echo   %~nx0 -d
exit /b 0

:: ---------------------------- 环境检查 --------------------------------------
:check_environment
where adb >nul 2>&1
if errorlevel 1 (
    echo %COLOR_RED%[错误] 未检测到 adb 命令，请确认已安装 Android SDK 并配置 PATH。%COLOR_RESET%
    exit /b 1
)

:: 获取设备列表
adb devices > "%TEMP_DEVICES%" 2>nul
set "DEVICE_COUNT=0"
for /f "skip=1 tokens=1,2" %%a in (%TEMP_DEVICES%) do (
    if "%%b"=="device" (
        set /a DEVICE_COUNT+=1
        set "DEV_!DEVICE_COUNT!=%%a"
    )
)

if !DEVICE_COUNT! equ 0 (
    echo %COLOR_RED%[错误] 未检测到已连接的 Android 设备。%COLOR_RESET%
    echo %COLOR_YELLOW%请检查：%COLOR_RESET%
    echo   1^) USB 是否连接正常
    echo   2^) 是否已开启 USB 调试
    echo   3^) 执行 'adb devices' 是否能看到设备
    del /f /q "%TEMP_DEVICES%" >nul 2>&1
    exit /b 1
)

if !DEVICE_COUNT! gtr 1 (
    echo %COLOR_YELLOW%[提示] 检测到多台设备（共 !DEVICE_COUNT! 台），请选择要监控的设备：%COLOR_RESET%
    echo.
    for /l %%i in (1,1,!DEVICE_COUNT!) do (
        set "CURR_DEV=!DEV_%%i!"
        for /f "tokens=*" %%m in ('adb -s !CURR_DEV! shell getprop ro.product.model 2^>nul') do set "DEV_MODEL=%%m"
        set "DEV_MODEL=!DEV_MODEL: =!"
        if "!DEV_MODEL!"=="" (
            echo   %COLOR_GREEN%%%i^)%COLOR_RESET% !CURR_DEV!
        ) else (
            echo   %COLOR_GREEN%%%i^)%COLOR_RESET% !CURR_DEV! ^(!DEV_MODEL!^)
        )
    )
    echo.
    :select_device
    set /p "SELECTION=%COLOR_CYAN%请输入设备编号 [1-!DEVICE_COUNT!]: %COLOR_RESET%"
    if "!SELECTION!"=="" goto :select_device
    set /a "SEL_CHECK=!SELECTION!" 2>nul
    if !SEL_CHECK! lss 1 (
        echo %COLOR_RED%无效输入，请输入 1 到 !DEVICE_COUNT! 之间的数字。%COLOR_RESET%
        goto :select_device
    )
    if !SEL_CHECK! gtr !DEVICE_COUNT! (
        echo %COLOR_RED%无效输入，请输入 1 到 !DEVICE_COUNT! 之间的数字。%COLOR_RESET%
        goto :select_device
    )
    set "SELECTED_DEV=!DEV_%SEL_CHECK%!"
    set "ANDROID_SERIAL=!SELECTED_DEV!"
    echo %COLOR_GREEN%[已选择] 将监控设备: !SELECTED_DEV!%COLOR_RESET%
    echo.
)

del /f /q "%TEMP_DEVICES%" >nul 2>&1
exit /b 0

:: ---------------------------- 日志输出 --------------------------------------
:log_line
:: 参数 %~1 = 要输出的文本行
set "LOG_TEXT=%~1"
echo %LOG_TEXT%
:: 写入日志（去除ANSI颜色码 - 简化处理，直接写入原始文本）
set "PLAIN_TEXT=!LOG_TEXT!"
set "PLAIN_TEXT=!PLAIN_TEXT:%ESC%[0m=!"
set "PLAIN_TEXT=!PLAIN_TEXT:%ESC%[1;31m=!"
set "PLAIN_TEXT=!PLAIN_TEXT:%ESC%[1;32m=!"
set "PLAIN_TEXT=!PLAIN_TEXT:%ESC%[1;33m=!"
set "PLAIN_TEXT=!PLAIN_TEXT:%ESC%[1;34m=!"
set "PLAIN_TEXT=!PLAIN_TEXT:%ESC%[1;35m=!"
set "PLAIN_TEXT=!PLAIN_TEXT:%ESC%[1;36m=!"
set "PLAIN_TEXT=!PLAIN_TEXT:%ESC%[1m=!"
>> "%LOG_FILE%" echo !PLAIN_TEXT!
exit /b 0

:: ---------------------------- 时间戳生成 ------------------------------------
:get_timestamp_for_filename
:: 返回 YYYYmmdd_HHMMSS 格式时间戳
set "DT_YEAR=%date:~0,4%"
set "DT_MONTH=%date:~5,2%"
set "DT_DAY=%date:~8,2%"
set "DT_HOUR=%time:~0,2%"
set "DT_MIN=%time:~3,2%"
set "DT_SEC=%time:~6,2%"
:: 去除空格（小时<10时前面有空格）
set "DT_HOUR=!DT_HOUR: =0!"
set "%~1=!DT_YEAR!!DT_MONTH!!DT_DAY!_!DT_HOUR!!DT_MIN!!DT_SEC!"
exit /b 0

:get_timestamp
:: 返回 YYYY-MM-DD HH:MM:SS 格式
set "DT_YEAR=%date:~0,4%"
set "DT_MONTH=%date:~5,2%"
set "DT_DAY=%date:~8,2%"
set "DT_HOUR=%time:~0,2%"
set "DT_MIN=%time:~3,2%"
set "DT_SEC=%time:~6,2%"
set "DT_HOUR=!DT_HOUR: =0!"
set "%~1=!DT_YEAR!-!DT_MONTH!-!DT_DAY! !DT_HOUR!:!DT_MIN!:!DT_SEC!"
exit /b 0

:: ---------------------------- Wakefulness 代码转换 ---------------------------
:wakefulness_to_str
:: %1=code %2=output_var
set "WK_CODE=%~1"
if "!WK_CODE!"=="0" (set "%~2=Asleep" & exit /b 0)
if "!WK_CODE!"=="1" (set "%~2=Awake" & exit /b 0)
if "!WK_CODE!"=="2" (set "%~2=Dreaming" & exit /b 0)
if "!WK_CODE!"=="3" (set "%~2=Dozing" & exit /b 0)
set "%~2=!WK_CODE!"
exit /b 0

:: ---------------------------- 获取 dumpsys power ----------------------------
:fetch_dumpsys_power
if defined ANDROID_SERIAL (
    adb -s %ANDROID_SERIAL% shell dumpsys power > "%TEMP_DUMP%" 2>nul
) else (
    adb shell dumpsys power > "%TEMP_DUMP%" 2>nul
)
exit /b 0

:: ---------------------------- 获取 lastUserActivityTime ---------------------
:get_last_user_activity_time
set "%~1="
:: 优先查找 lastUserActivityTime=
for /f "tokens=*" %%l in ('findstr /r /c:"lastUserActivityTime=" "%TEMP_DUMP%" 2^>nul') do (
    set "LUAT_LINE=%%l"
    goto :parse_luat
)
:: 备选查找 mLastUserActivityTime
for /f "tokens=*" %%l in ('findstr /c:"mLastUserActivityTime" "%TEMP_DUMP%" 2^>nul') do (
    set "LUAT_LINE=%%l"
    goto :parse_luat
)
exit /b 0

:parse_luat
:: 从行中提取 = 后面的数字
for /f "tokens=2 delims==" %%v in ("!LUAT_LINE!") do (
    set "LUAT_VAL=%%v"
)
:: 去除空格和非数字前缀
set "LUAT_VAL=!LUAT_VAL: =!"
:: 提取纯数字部分（去除可能的尾部文本）
for /f "delims=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ ,()" %%n in ("!LUAT_VAL!") do (
    set "LUAT_VAL=%%n"
)
set "%~1=!LUAT_VAL!"
exit /b 0

:: ---------------------------- 获取 mWakefulness -----------------------------
:get_wakefulness
set "%~1="
for /f "tokens=*" %%l in ('findstr /c:"mWakefulness=" "%TEMP_DUMP%" 2^>nul') do (
    set "WK_LINE=%%l"
    goto :parse_wk
)
for /f "tokens=*" %%l in ('findstr /c:"getWakefulnessLocked()=" "%TEMP_DUMP%" 2^>nul') do (
    set "WK_LINE=%%l"
    goto :parse_wk
)
exit /b 0

:parse_wk
:: 提取 = 后面的值
for /f "tokens=2 delims==" %%v in ("!WK_LINE!") do (
    set "WK_VAL=%%v"
)
set "WK_VAL=!WK_VAL: =!"
:: 去除回车
set "WK_VAL=!WK_VAL: =!"
:: 检查是否是数字
set "WK_IS_NUM=1"
for /f "delims=0123456789" %%c in ("!WK_VAL!") do set "WK_IS_NUM=0"
if "!WK_IS_NUM!"=="1" (
    if not "!WK_VAL!"=="" (
        call :wakefulness_to_str "!WK_VAL!" WK_RESULT
        set "%~1=!WK_RESULT!"
        exit /b 0
    )
)
set "%~1=!WK_VAL!"
exit /b 0

:: ---------------------------- 获取 mScreenOffTimeoutSetting -----------------
:get_screen_off_timeout_setting
set "%~1="
for %%k in (mScreenOffTimeoutSetting mScreenOffTimeout mUserActivityTimeoutOverrideFromWindowManager) do (
    for /f "tokens=*" %%l in ('findstr /c:"%%k=" "%TEMP_DUMP%" 2^>nul') do (
        set "SOTS_LINE=%%l"
        goto :parse_sots
    )
)
exit /b 0

:parse_sots
for /f "tokens=2 delims==" %%v in ("!SOTS_LINE!") do (
    set "SOTS_VAL=%%v"
)
set "SOTS_VAL=!SOTS_VAL: =!"
for /f "delims=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ ,()" %%n in ("!SOTS_VAL!") do (
    set "SOTS_VAL=%%n"
)
set "%~1=!SOTS_VAL!"
exit /b 0

:: ---------------------------- 获取 screen_off_timeout -----------------------
:get_screen_off_timeout
set "%~1="
if defined ANDROID_SERIAL (
    for /f "tokens=*" %%v in ('adb -s %ANDROID_SERIAL% shell settings get system screen_off_timeout 2^>nul') do set "SOT_VAL=%%v"
) else (
    for /f "tokens=*" %%v in ('adb shell settings get system screen_off_timeout 2^>nul') do set "SOT_VAL=%%v"
)
set "SOT_VAL=!SOT_VAL: =!"
set "SOT_VAL=!SOT_VAL:	=!"
if "!SOT_VAL!"=="null" (
    set "%~1="
    exit /b 0
)
set "%~1=!SOT_VAL!"
exit /b 0

:: ---------------------------- 获取 uptime (毫秒) ----------------------------
:get_uptime_ms
set "%~1="
if defined ANDROID_SERIAL (
    for /f "tokens=1 delims= " %%u in ('adb -s %ANDROID_SERIAL% shell cat /proc/uptime 2^>nul') do set "UT_SEC=%%u"
) else (
    for /f "tokens=1 delims= " %%u in ('adb shell cat /proc/uptime 2^>nul') do set "UT_SEC=%%u"
)
set "UT_SEC=!UT_SEC: =!"
if "!UT_SEC!"=="" exit /b 0
:: 取整数部分（截断小数）
for /f "tokens=1 delims=." %%i in ("!UT_SEC!") do set "UT_INT=%%i"
if "!UT_INT!"=="" exit /b 0
set /a "UT_MS=!UT_INT! * 1000"
set "%~1=!UT_MS!"
exit /b 0

:: ---------------------------- 格式化 timeout --------------------------------
:format_timeout
:: %1=值 %2=输出变量名
set "FT_VAL=%~1"
if "!FT_VAL!"=="" (set "%~2=(未知)" & exit /b 0)
if "!FT_VAL!"=="null" (set "%~2=(未知)" & exit /b 0)
if "!FT_VAL!"=="2147483647" (
    set "%~2=2147483647 (MAX/永不息屏)"
    exit /b 0
)
if "!FT_VAL!"=="180000" (
    set "%~2=180000 (3分钟)"
    exit /b 0
)
:: 检查是否是数字
set "FT_ISNUM=1"
for /f "delims=0123456789-" %%c in ("!FT_VAL!") do set "FT_ISNUM=0"
if "!FT_ISNUM!"=="0" (
    set "%~2=!FT_VAL!"
    exit /b 0
)
:: 大于等于60000 转分钟
if !FT_VAL! geq 60000 (
    set /a "FT_MINS=!FT_VAL! / 60000"
    set /a "FT_REMAIN=!FT_VAL! %% 60000"
    if !FT_REMAIN! equ 0 (
        set "%~2=!FT_VAL! (!FT_MINS!分钟)"
    ) else (
        set /a "FT_TENTHS=!FT_REMAIN! * 10 / 60000"
        set "%~2=!FT_VAL! (!FT_MINS!.!FT_TENTHS!分钟)"
    )
) else (
    set /a "FT_SECS=!FT_VAL! / 1000"
    set /a "FT_SECREM=!FT_VAL! %% 1000"
    if !FT_SECREM! equ 0 (
        set "%~2=!FT_VAL! (!FT_SECS!秒)"
    ) else (
        set /a "FT_TENTHS=!FT_SECREM! * 10 / 1000"
        set "%~2=!FT_VAL! (!FT_SECS!.!FT_TENTHS!秒)"
    )
)
exit /b 0

:: ---------------------------- 格式化 idle 时长 ------------------------------
:format_idle_duration
:: %1=毫秒值 %2=输出变量名
set "FID_VAL=%~1"
if "!FID_VAL!"=="" (set "%~2=(未知)" & exit /b 0)
:: 检查是否是数字
set "FID_ISNUM=1"
for /f "delims=0123456789-" %%c in ("!FID_VAL!") do set "FID_ISNUM=0"
if "!FID_ISNUM!"=="0" (set "%~2=(未知)" & exit /b 0)
set /a "FID_SECS=!FID_VAL! / 1000"
set /a "FID_FRAC=(!FID_VAL! %% 1000) / 100"
set "%~2=!FID_VAL! ms (!FID_SECS!.!FID_FRAC!s)"
exit /b 0

:: ---------------------------- 调试输出 --------------------------------------
:print_debug_dump
call :log_line "%COLOR_YELLOW%--- [DEBUG] dumpsys power 关键原始行 ---%COLOR_RESET%"
set "DBG_FOUND=0"
for /f "tokens=*" %%l in ('findstr /i /r /c:"mLastUserActivityTime" /c:"lastUserActivityTime" /c:"mWakefulness" /c:"Wakefulness" /c:"getWakefulnessLocked" /c:"mScreenOffTimeout" /c:"mUserActivityTimeout" /c:"mHoldingDisplaySuspendBlocker" "%TEMP_DUMP%" 2^>nul') do (
    call :log_line "    %%l"
    set "DBG_FOUND=1"
)
if "!DBG_FOUND!"=="0" (
    call :log_line "%COLOR_RED%  (未匹配到任何关键字段)%COLOR_RESET%"
)
call :log_line "%COLOR_YELLOW%-----------------------------------------%COLOR_RESET%"
exit /b 0

:: ============================================================================
:: 主循环
:: ============================================================================
:main_loop
set "PREV_TIMEOUT="
set "PREV_WAKEFULNESS="

call :log_line "%COLOR_CYAN%===================================================%COLOR_RESET%"
call :log_line "%COLOR_BOLD%Android Idle Timeout 监控已启动%COLOR_RESET%"
call :log_line "  刷新间隔 : %INTERVAL% 秒"
if "!DEBUG!"=="1" (
    call :log_line "  调试模式 : 开启"
) else (
    call :log_line "  调试模式 : 关闭"
)
call :log_line "  日志路径 : %LOG_FILE%"
call :log_line "  按 Ctrl+C 退出"
call :log_line "%COLOR_CYAN%===================================================%COLOR_RESET%"

:loop
call :get_timestamp CURR_TS

:: 获取数据
call :fetch_dumpsys_power
call :get_last_user_activity_time LAST_ACTIVITY
call :get_uptime_ms UPTIME_MS
call :get_screen_off_timeout_setting TIMEOUT_SETTING
call :get_wakefulness WAKEFULNESS
call :get_screen_off_timeout TIMEOUT_NOW

:: 计算 idle 时长
set "IDLE_MS="
if defined UPTIME_MS if defined LAST_ACTIVITY (
    set "IDLE_VALID=1"
    if "!UPTIME_MS!"=="" set "IDLE_VALID=0"
    if "!LAST_ACTIVITY!"=="" set "IDLE_VALID=0"
    if "!IDLE_VALID!"=="1" (
        set /a "IDLE_MS=!UPTIME_MS! - !LAST_ACTIVITY!" 2>nul
    )
)

:: 变化检测 - timeout
set "TIMEOUT_TAG="
if defined PREV_TIMEOUT if defined TIMEOUT_NOW (
    if not "!PREV_TIMEOUT!"=="" if not "!TIMEOUT_NOW!"=="" (
        if not "!PREV_TIMEOUT!"=="!TIMEOUT_NOW!" (
            set "TIMEOUT_TAG=%COLOR_RED%[CHANGED: !PREV_TIMEOUT! -^> !TIMEOUT_NOW!]%COLOR_RESET%"
        )
    )
)

:: 变化检测 - wakefulness
set "WAKE_TAG="
if defined PREV_WAKEFULNESS if defined WAKEFULNESS (
    if not "!PREV_WAKEFULNESS!"=="" if not "!WAKEFULNESS!"=="" (
        if not "!PREV_WAKEFULNESS!"=="!WAKEFULNESS!" (
            set "WAKE_TAG=%COLOR_RED%[CHANGED: !PREV_WAKEFULNESS! -^> !WAKEFULNESS!]%COLOR_RESET%"
        )
    )
)

:: idle 超过 timeout 警告
set "WARN_TAG="
if defined IDLE_MS if defined TIMEOUT_NOW (
    if not "!IDLE_MS!"=="" if not "!TIMEOUT_NOW!"=="" (
        set "DO_WARN=0"
        if not "!TIMEOUT_NOW!"=="2147483647" (
            if !TIMEOUT_NOW! gtr 0 (
                if !IDLE_MS! gtr !TIMEOUT_NOW! set "DO_WARN=1"
            )
        )
        if "!DO_WARN!"=="1" (
            set "WARN_TAG=%COLOR_RED%Warning: Idle 已超过 ScreenOffTimeout!%COLOR_RESET%"
        )
    )
)

:: Wakefulness 颜色
set "WAKE_COLOR=%COLOR_GREEN%"
if "!WAKEFULNESS!"=="Awake" set "WAKE_COLOR=%COLOR_GREEN%"
if "!WAKEFULNESS!"=="Dozing" set "WAKE_COLOR=%COLOR_YELLOW%"
if "!WAKEFULNESS!"=="Asleep" set "WAKE_COLOR=%COLOR_MAGENTA%"
if "!WAKEFULNESS!"=="Dreaming" set "WAKE_COLOR=%COLOR_BLUE%"

:: 格式化输出值
call :format_timeout "!TIMEOUT_NOW!" FMT_TIMEOUT_NOW
call :format_timeout "!TIMEOUT_SETTING!" FMT_TIMEOUT_SETTING
call :format_idle_duration "!IDLE_MS!" FMT_IDLE

:: 设置显示值（处理空值）
set "DISP_WAKE=!WAKEFULNESS!"
if "!DISP_WAKE!"=="" set "DISP_WAKE=未知"
set "DISP_ACTIVITY=!LAST_ACTIVITY!"
if "!DISP_ACTIVITY!"=="" set "DISP_ACTIVITY=未知"
set "DISP_UPTIME=!UPTIME_MS!"
if "!DISP_UPTIME!"=="" set "DISP_UPTIME=未知"

:: 输出
call :log_line ""
call :log_line "%COLOR_CYAN%================== !CURR_TS! ==================%COLOR_RESET%"
call :log_line "  Wakefulness     : !WAKE_COLOR!!DISP_WAKE!%COLOR_RESET% !WAKE_TAG!"
call :log_line "  LastUserActivity: !DISP_ACTIVITY! ms"
call :log_line "  Current Uptime  : !DISP_UPTIME! ms"
call :log_line "  Idle Duration   : !FMT_IDLE!"
call :log_line "  ScreenOffTimeout: !FMT_TIMEOUT_NOW! !TIMEOUT_TAG!"
call :log_line "  Timeout Setting : !FMT_TIMEOUT_SETTING!"
if not "!WARN_TAG!"=="" (
    call :log_line "  !WARN_TAG!"
)

:: 调试模式
if "!DEBUG!"=="1" (
    call :print_debug_dump
)

call :log_line "%COLOR_CYAN%----------------------------------------------------------%COLOR_RESET%"

:: 更新上次值
set "PREV_TIMEOUT=!TIMEOUT_NOW!"
set "PREV_WAKEFULNESS=!WAKEFULNESS!"

:: 等待
timeout /t %INTERVAL% /nobreak >nul 2>&1

goto :loop

:: ============================================================================
:: 退出清理（Ctrl+C 时 cmd 会提示，但无法完全 trap）
:: ============================================================================
:cleanup
echo.
call :get_timestamp EXIT_TS
call :log_line ""
call :log_line "%COLOR_CYAN%===================================================%COLOR_RESET%"
call :log_line "%COLOR_GREEN%监控已停止 - !EXIT_TS!%COLOR_RESET%"
call :log_line "%COLOR_GREEN%日志文件已保存: %LOG_FILE%%COLOR_RESET%"
call :log_line "%COLOR_CYAN%===================================================%COLOR_RESET%"
:: 清理临时文件
del /f /q "%TEMP_DUMP%" >nul 2>&1
endlocal
exit /b 0
