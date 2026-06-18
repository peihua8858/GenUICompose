@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ---------------------------- 颜色定义 ---------------------------------------
:: Windows 10+ ANSI escape code
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
set "COLOR_RESET=%ESC%[0m"
set "COLOR_RED=%ESC%[1;31m"
set "COLOR_GREEN=%ESC%[1;32m"
set "COLOR_YELLOW=%ESC%[1;33m"
set "COLOR_CYAN=%ESC%[1;36m"

:: 目标值
set "TARGET_VALUE=2147483647"
:: 停止时间（24小时制，例如 21:00 表示晚上9点）
set "STOP_TIME=21:00"
:: 日志文件路径（脚本所在目录）
set "SCRIPT_DIR=%~dp0"
:: 去掉末尾反斜杠
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

:: 生成时间戳
for /f "tokens=1-3 delims=/ " %%a in ("%date%") do (
    set "D_YEAR=%%a"
    set "D_MONTH=%%b"
    set "D_DAY=%%c"
)
for /f "tokens=1-3 delims=:." %%a in ("%time: =0%") do (
    set "T_HOUR=%%a"
    set "T_MIN=%%b"
    set "T_SEC=%%c"
)
set "TIMESTAMP=%D_YEAR%%D_MONTH%%D_DAY%_%T_HOUR%%T_MIN%%T_SEC%"
set "LOG_FILE=%SCRIPT_DIR%\screen_timeout_monitor_%TIMESTAMP%.log"
set "SYSTEM_LOG_FILE=%SCRIPT_DIR%\system_logcat_%TIMESTAMP%.log"
set "PID_FILE=%SCRIPT_DIR%\logcat_pid_%TIMESTAMP%.tmp"

:: ---------------------------- 设备选择 ---------------------------------------
where adb >nul 2>&1
if %errorlevel% neq 0 (
    echo %COLOR_RED%[错误] 未检测到 adb 命令，请确认已安装 Android SDK 并配置 PATH。%COLOR_RESET%
    exit /b 1
)

:: 获取已连接设备列表
set "DEVICE_COUNT=0"
for /f "skip=1 tokens=1,2" %%a in ('adb devices 2^>nul') do (
    if "%%b"=="device" (
        set /a DEVICE_COUNT+=1
        set "DEVICE_!DEVICE_COUNT!=%%a"
    )
)

if %DEVICE_COUNT% equ 0 (
    echo %COLOR_RED%[错误] 未检测到已连接的 Android 设备。%COLOR_RESET%
    echo %COLOR_YELLOW%请检查：%COLOR_RESET%
    echo   1^) USB 是否连接正常
    echo   2^) 是否已开启 USB 调试
    echo   3^) 执行 'adb devices' 是否能看到设备
    exit /b 1
)

if %DEVICE_COUNT% equ 1 (
    set "DEVICE_SERIAL=!DEVICE_1!"
) else (
    echo %COLOR_YELLOW%[提示] 检测到多台设备（共 %DEVICE_COUNT% 台），请选择要监控的设备：%COLOR_RESET%
    echo.
    for /l %%i in (1,1,%DEVICE_COUNT%) do (
        set "DEV=!DEVICE_%%i!"
        :: 获取设备型号
        for /f "delims=" %%m in ('adb -s !DEV! shell getprop ro.product.model 2^>nul') do set "DEV_MODEL=%%m"
        set "DEV_MODEL=!DEV_MODEL: =!"
        if defined DEV_MODEL (
            echo   %COLOR_GREEN%%%i^)%COLOR_RESET% !DEV! ^(!DEV_MODEL!^)
        ) else (
            echo   %COLOR_GREEN%%%i^)%COLOR_RESET% !DEV!
        )
        set "DEV_MODEL="
    )
    echo.
    :select_loop
    set /p "SELECTION=%COLOR_CYAN%请输入设备编号 [1-%DEVICE_COUNT%]: %COLOR_RESET%"
    :: 验证输入
    set "VALID=0"
    for /l %%i in (1,1,%DEVICE_COUNT%) do (
        if "!SELECTION!"=="%%i" set "VALID=1"
    )
    if "!VALID!"=="0" (
        echo %COLOR_RED%无效输入，请输入 1 到 %DEVICE_COUNT% 之间的数字。%COLOR_RESET%
        goto select_loop
    )
    set "DEVICE_SERIAL=!DEVICE_%SELECTION%!"
    echo %COLOR_GREEN%[已选择] 将监控设备: !DEVICE_SERIAL!%COLOR_RESET%
    echo.
)

:: 输出监控信息
call :log "=========================================="
call :log "开始监听 screen_off_timeout 值"
call :log "设备: %DEVICE_SERIAL%"
call :log "目标值: %TARGET_VALUE%"
call :log "停止时间: %STOP_TIME%"
call :log "执行日志: %LOG_FILE%"
call :log "系统日志: %SYSTEM_LOG_FILE%"
call :log "=========================================="
call :log ""

:: 启动后台 logcat 日志捕获
call :log "[%TIMESTAMP%] 开始捕获系统 logcat 日志..."
start /b cmd /c "adb -s %DEVICE_SERIAL% logcat -v time > "%SYSTEM_LOG_FILE%" 2>&1"

:: 等待一下确保 logcat 文件已创建
timeout /t 2 /nobreak >nul

:: 记录 logcat 进程 PID（通过 wmic 查找）
for /f "tokens=2 delims=," %%p in ('wmic process where "commandline like '%%logcat%%' and commandline like '%%%DEVICE_SERIAL%%%'" get processid /format:csv 2^>nul ^| findstr /r "[0-9]"') do (
    set "LOGCAT_PID=%%p"
)
:: 保存 PID 到临时文件
if defined LOGCAT_PID (
    echo %LOGCAT_PID%> "%PID_FILE%"
)

:: 主监控循环
set "COUNTER=0"
set "SLEEP_DETECTED=0"

:monitor_loop
set /a COUNTER+=1

:: 获取当前时间
for /f "tokens=1-3 delims=/ " %%a in ("%date%") do (
    set "C_YEAR=%%a"
    set "C_MONTH=%%b"
    set "C_DAY=%%c"
)
for /f "tokens=1-3 delims=:." %%a in ("%time: =0%") do (
    set "C_HOUR=%%a"
    set "C_MIN=%%b"
    set "C_SEC=%%c"
)
set "CURRENT_TIME=%C_YEAR%-%C_MONTH%-%C_DAY% %C_HOUR%:%C_MIN%:%C_SEC%"
set "CURRENT_HOUR_MINUTE=%C_HOUR%:%C_MIN%"

:: 获取 screen_off_timeout 的值
set "CURRENT_VALUE="
for /f "delims=" %%v in ('adb -s %DEVICE_SERIAL% shell settings get system screen_off_timeout 2^>nul') do (
    set "CURRENT_VALUE=%%v"
)
:: 去除回车符和空格
if defined CURRENT_VALUE (
    set "CURRENT_VALUE=!CURRENT_VALUE: =!"
    set "CURRENT_VALUE=!CURRENT_VALUE:	=!"
    for /f "delims=" %%x in ("!CURRENT_VALUE!") do set "CURRENT_VALUE=%%x"
)

if not defined CURRENT_VALUE (
    call :log "[!CURRENT_TIME!] 第 !COUNTER! 次查询: 获取值失败"
    timeout /t 1 /nobreak >nul
    goto monitor_loop
)

call :log "[!CURRENT_TIME!] 第 !COUNTER! 次查询: screen_off_timeout = !CURRENT_VALUE!"

:: 检查是否等于目标值
if "!CURRENT_VALUE!"=="%TARGET_VALUE%" (
    call :log "[!CURRENT_TIME!] √ 检测到目标值 %TARGET_VALUE%"
) else (
    call :log "[!CURRENT_TIME!] × 当前值 !CURRENT_VALUE! 不等于目标值 %TARGET_VALUE%"
)

:: 检查是否到达停止时间
if "!CURRENT_HOUR_MINUTE!"=="%STOP_TIME%" (
    call :log "[!CURRENT_TIME!] ⏰ 已到达停止时间 %STOP_TIME%，停止监听"
    call :log "=========================================="
    call :log "监听结束"
    call :log "执行日志: %LOG_FILE%"
    call :log "系统日志: %SYSTEM_LOG_FILE%"
    call :log "=========================================="
    goto cleanup
)

:: 检查是否检测到息屏日志
if exist "%SYSTEM_LOG_FILE%" (
    findstr /c:"Going to sleep due to timeout" "%SYSTEM_LOG_FILE%" >nul 2>&1
    if !errorlevel! equ 0 (
        call :log "[!CURRENT_TIME!] ⚠ 从监听开始检测到新的息屏日志（Going to sleep due to timeout），停止监听"
        call :log "=========================================="
        call :log "监听结束"
        call :log "执行日志: %LOG_FILE%"
        call :log "系统日志: %SYSTEM_LOG_FILE%"
        call :log "=========================================="
        goto cleanup
    )
)

:: 等待 2 秒后再次查询
timeout /t 2 /nobreak >nul
goto monitor_loop

:: ---------------------------- 清理函数 ---------------------------------------
:cleanup
echo.
call :log "=========================================="
call :log "正在停止 logcat 日志捕获..."

:: 终止 logcat 进程
if defined LOGCAT_PID (
    taskkill /f /pid %LOGCAT_PID% >nul 2>&1
) else if exist "%PID_FILE%" (
    set /p LOGCAT_PID=<"%PID_FILE%"
    taskkill /f /pid !LOGCAT_PID! >nul 2>&1
)

:: 也尝试通过命令行匹配终止
wmic process where "commandline like '%%logcat%%' and commandline like '%%%DEVICE_SERIAL%%%'" call terminate >nul 2>&1

call :log "√ logcat 日志已保存到: %SYSTEM_LOG_FILE%"

:: 清理临时文件
if exist "%PID_FILE%" del /f /q "%PID_FILE%" >nul 2>&1

call :log "=========================================="
goto :eof

:: ---------------------------- 日志函数 ---------------------------------------
:log
set "MSG=%~1"
echo !MSG!
echo !MSG!>> "%LOG_FILE%"
goto :eof
