@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ==============================================================================
:: 脚本名称: adb_tools.bat
:: 功能描述: 综合 ADB 工具脚本，集成多种 ADB 调试功能（Windows 版）
:: 使用方式: adb_tools.bat
:: ==============================================================================

:: ==============================================================================
:: 公共模块：颜色定义（ANSI 转义序列，Windows 10+ 支持）
:: ==============================================================================
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
set "RED=!ESC![31m"
set "GREEN=!ESC![32m"
set "YELLOW=!ESC![33m"
set "BLUE=!ESC![34m"
set "NC=!ESC![0m"

:: ==============================================================================
:: 主程序入口
:: ==============================================================================
call :detect_and_select_device
if !ERRORLEVEL! neq 0 exit /b 1
echo.

:main_loop
call :show_menu
set "menu_choice="
set /p "menu_choice=请输入功能编号 [0-25]: "

if "!menu_choice!"=="1" call :func_switch_env
if "!menu_choice!"=="2" call :func_ut_realtime
if "!menu_choice!"=="3" call :func_rotation
if "!menu_choice!"=="4" call :func_resolution
if "!menu_choice!"=="5" call :func_voice_command
if "!menu_choice!"=="6" call :func_close_emulator
if "!menu_choice!"=="7" call :func_top_activity
if "!menu_choice!"=="8" call :func_app_version
if "!menu_choice!"=="9" call :func_device_uuid
if "!menu_choice!"=="10" call :func_process_manage
if "!menu_choice!"=="11" call :func_deeplink
if "!menu_choice!"=="12" call :func_app_manage
if "!menu_choice!"=="13" call :func_system_property
if "!menu_choice!"=="14" call :func_content_read
if "!menu_choice!"=="15" call :func_screen_capture
if "!menu_choice!"=="16" call :func_overdraw
if "!menu_choice!"=="17" call :func_audio_info
if "!menu_choice!"=="18" call :func_exit_factory
if "!menu_choice!"=="19" call :func_anr_export
if "!menu_choice!"=="20" call :func_logcat
if "!menu_choice!"=="21" call :func_monkey_test
if "!menu_choice!"=="22" call :func_runtime
if "!menu_choice!"=="23" call :func_memory
if "!menu_choice!"=="24" call :func_find_package
if "!menu_choice!"=="25" call :func_power_key
if "!menu_choice!"=="0" (
    echo.
    call :print_success "感谢使用 ADB 综合工具箱，再见！"
    exit /b 0
)

:: 验证输入有效性
set "VALID_CHOICE=0"
for %%n in (0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25) do (
    if "!menu_choice!"=="%%n" set "VALID_CHOICE=1"
)
if "!VALID_CHOICE!"=="0" call :print_error "无效的选择，请输入 0 到 25 之间的数字"

call :wait_return_menu
goto :main_loop

:: ==============================================================================
:: 公共模块：打印函数
:: ==============================================================================
:print_success
echo !GREEN![成功]!NC! %~1
goto :eof

:print_error
echo !RED![错误]!NC! %~1
goto :eof

:print_warning
echo !YELLOW![提示]!NC! %~1
goto :eof

:print_info
echo !BLUE![信息]!NC! %~1
goto :eof

:print_divider
echo !BLUE!==============================================================%NC!
goto :eof

:print_center_line
set "content=%~1"
set "box_width=%~2"
:: 简化居中：固定填充
set "pad=                    "
echo !pad!!content!
goto :eof

:wait_return_menu
echo.
pause
goto :eof

:: ==============================================================================
:: 公共模块：设备检测与选择逻辑
:: ==============================================================================
:detect_and_select_device
call :print_info "正在检测已连接的 ADB 设备..."

:: 检查 adb 命令是否可用
where adb >nul 2>&1
if !ERRORLEVEL! neq 0 (
    call :print_error "未找到 adb 命令，请先安装 Android SDK 并配置 PATH 环境变量"
    exit /b 1
)

:: 启动 adb server
adb start-server >nul 2>&1

:: 获取已连接的设备列表
set "DEVICE_COUNT=0"
for /f "tokens=1,2" %%a in ('adb devices') do (
    if "%%b"=="device" (
        set "DEVICES[!DEVICE_COUNT!]=%%a"
        set /a "DEVICE_COUNT+=1"
    )
)

if !DEVICE_COUNT! equ 0 (
    call :print_error "未检测到任何已连接的 ADB 设备，请先连接设备并确认 USB 调试已开启"
    exit /b 1
)

if !DEVICE_COUNT! equ 1 (
    set "SELECTED_DEVICE=!DEVICES[0]!"
    call :print_success "检测到 1 台设备，自动选择：!SELECTED_DEVICE!"
    goto :device_selected
)

:: 多设备选择
call :print_warning "检测到 !DEVICE_COUNT! 台设备，请选择要操作的设备："
for /l %%i in (0,1,!DEVICE_COUNT!) do (
    if %%i lss !DEVICE_COUNT! (
        set "dev=!DEVICES[%%i]!"
        set /a "display_idx=%%i+1"
        for /f "tokens=*" %%m in ('adb -s !dev! shell getprop ro.product.model 2^>nul') do set "model=%%m"
        if not defined model set "model=未知型号"
        echo   !GREEN!!display_idx!!NC!. !dev! ^(!model!^)
        set "model="
    )
)

:device_select_loop
set "device_index="
set /p "device_index=请输入设备序号 [1-!DEVICE_COUNT!]: "
:: 验证输入
set "VALID_IDX=0"
if defined device_index (
    set /a "check_idx=device_index" 2>nul
    if !check_idx! geq 1 if !check_idx! leq !DEVICE_COUNT! set "VALID_IDX=1"
)
if "!VALID_IDX!"=="0" (
    call :print_error "无效的输入，请输入 1 到 !DEVICE_COUNT! 之间的数字"
    goto :device_select_loop
)
set /a "arr_idx=device_index-1"
set "SELECTED_DEVICE=!DEVICES[%arr_idx%]!"
call :print_success "已选择设备：!SELECTED_DEVICE!"

:device_selected
set "ADB=adb -s !SELECTED_DEVICE!"
goto :eof

:: ==============================================================================
:: 公共模块：Root 状态检查与处理
:: ==============================================================================
:ensure_root
call :print_info "正在检查设备 root 状态..."
set "CURRENT_USER="
for /f "tokens=*" %%i in ('!ADB! shell whoami 2^>nul') do set "CURRENT_USER=%%i"

if not "!CURRENT_USER!"=="root" (
    call :print_warning "设备未处于 root 状态（当前用户：!CURRENT_USER!），执行 adb root..."
    !ADB! root
    call :print_info "等待设备重新连接..."
    !ADB! wait-for-device
    timeout /t 1 /nobreak >nul

    set "CURRENT_USER="
    for /f "tokens=*" %%i in ('!ADB! shell whoami 2^>nul') do set "CURRENT_USER=%%i"
    if not "!CURRENT_USER!"=="root" (
        call :print_error "adb root 失败，设备可能不支持 root（用户类型：!CURRENT_USER!）"
        exit /b 1
    )
    call :print_success "设备已切换至 root 状态"
) else (
    call :print_success "设备已处于 root 状态"
)
exit /b 0

:: ==============================================================================
:: 公共模块：Remount 检查与处理
:: ==============================================================================
:ensure_remount
call :print_info "正在检查 /system 分区写入权限..."

set "WRITE_TEST="
for /f "tokens=*" %%i in ('!ADB! shell "touch /system/.adb_tools_test 2>&1 && echo OK || echo FAIL" 2^>nul') do set "WRITE_TEST=%%i"

echo !WRITE_TEST! | findstr "OK" >nul 2>&1
if !ERRORLEVEL! neq 0 (
    call :print_warning "/system 分区不可写入，执行 adb remount..."
    !ADB! remount
    timeout /t 1 /nobreak >nul

    set "WRITE_TEST="
    for /f "tokens=*" %%i in ('!ADB! shell "touch /system/.adb_tools_test 2>&1 && echo OK || echo FAIL" 2^>nul') do set "WRITE_TEST=%%i"
    echo !WRITE_TEST! | findstr "OK" >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        call :print_error "adb remount 后 /system 仍不可写，请检查设备是否已禁用 verity"
        call :print_warning "提示：可尝试手动执行 'adb disable-verity' 后重启设备再运行本脚本"
        exit /b 1
    )
    call :print_success "/system 分区已成功 remount 为可写"
) else (
    call :print_success "/system 分区已可写入"
)

:: 清理测试文件
!ADB! shell "rm -f /system/.adb_tools_test" >nul 2>&1
exit /b 0

:: ==============================================================================
:: 功能 1: 切换网络环境
:: ==============================================================================
:func_switch_env
call :print_divider
call :print_info "【切换网络环境】"
call :print_divider

call :ensure_root
if !ERRORLEVEL! neq 0 goto :eof
call :ensure_remount
if !ERRORLEVEL! neq 0 goto :eof

echo.
call :print_warning "请选择要切换的网络环境："
echo   !GREEN!0!NC! = 线上环境
echo   !GREEN!1!NC! = 预发环境1
echo   !GREEN!2!NC! = 预发环境2

:env_select_loop
set "ENV_VALUE="
set /p "ENV_VALUE=请输入环境编号 [0/1/2]: "
if "!ENV_VALUE!"=="0" (set "ENV_NAME=线上环境" & goto :env_selected)
if "!ENV_VALUE!"=="1" (set "ENV_NAME=预发环境1" & goto :env_selected)
if "!ENV_VALUE!"=="2" (set "ENV_NAME=预发环境2" & goto :env_selected)
call :print_error "无效的输入，请输入 0、1 或 2"
goto :env_select_loop

:env_selected
set "PROP_KEY=persist.sys.genie.env"
call :print_info "正在执行环境切换：!PROP_KEY!=!ENV_VALUE! ..."
!ADB! shell "setprop !PROP_KEY! !ENV_VALUE!"

timeout /t 1 /nobreak >nul
set "ACTUAL_VALUE="
for /f "tokens=*" %%i in ('!ADB! shell "getprop !PROP_KEY!" 2^>nul') do set "ACTUAL_VALUE=%%i"
if "!ACTUAL_VALUE!"=="!ENV_VALUE!" (
    call :print_success "环境切换成功！!PROP_KEY! = !ACTUAL_VALUE! (!ENV_NAME!)"
) else (
    call :print_error "环境切换失败！期望值=!ENV_VALUE!，实际值=!ACTUAL_VALUE!"
    goto :eof
)

echo.
set "reboot_choice="
set /p "reboot_choice=是否需要重启设备使环境生效？[y/N]: "
if /i "!reboot_choice!"=="y" (
    call :print_info "正在执行 adb reboot..."
    !ADB! reboot
    call :print_success "重启命令已发送，请等待设备重新启动"
) else (
    call :print_warning "已跳过重启，部分功能可能需要重启后才能生效"
)
goto :eof

:: ==============================================================================
:: 功能 2: 设置埋点UT实时上报验证
:: ==============================================================================
:func_ut_realtime
call :print_divider
call :print_info "【设置埋点UT实时上报验证】"
call :print_divider

call :ensure_root
if !ERRORLEVEL! neq 0 goto :eof
call :ensure_remount
if !ERRORLEVEL! neq 0 goto :eof

:: 设置 realtime enable
set "RT_ENABLE_PROP=persist.ut.realtime.enable"
set "RT_ENABLE_VALUE=1"
call :print_info "正在设置 !RT_ENABLE_PROP!=!RT_ENABLE_VALUE! ..."
!ADB! shell "setprop !RT_ENABLE_PROP! !RT_ENABLE_VALUE!"

timeout /t 1 /nobreak >nul
set "ACTUAL_VALUE="
for /f "tokens=*" %%i in ('!ADB! shell "getprop !RT_ENABLE_PROP!" 2^>nul') do set "ACTUAL_VALUE=%%i"
if "!ACTUAL_VALUE!"=="!RT_ENABLE_VALUE!" (
    call :print_success "!RT_ENABLE_PROP! 已成功设置为 !RT_ENABLE_VALUE!（验证通过）"
) else (
    call :print_error "!RT_ENABLE_PROP! 设置验证失败，期望值=!RT_ENABLE_VALUE!，实际值=!ACTUAL_VALUE!"
    goto :eof
)

:: 设置 debugkey
set "DEBUGKEY_PROP=ut.realtime.debugkey"
echo.
call :print_warning "请输入 debugkey 值："
set "DEBUG_KEY_VALUE="
set /p "DEBUG_KEY_VALUE=debugkey: "
if "!DEBUG_KEY_VALUE!"=="" (
    call :print_error "debugkey 值不能为空"
    goto :eof
)

call :print_info "正在设置 !DEBUGKEY_PROP!=!DEBUG_KEY_VALUE! ..."
!ADB! shell "setprop !DEBUGKEY_PROP! !DEBUG_KEY_VALUE!"

timeout /t 1 /nobreak >nul
set "ACTUAL_DEBUGKEY="
for /f "tokens=*" %%i in ('!ADB! shell "getprop !DEBUGKEY_PROP!" 2^>nul') do set "ACTUAL_DEBUGKEY=%%i"
if "!ACTUAL_DEBUGKEY!"=="!DEBUG_KEY_VALUE!" (
    call :print_success "!DEBUGKEY_PROP! 已成功设置为 !DEBUG_KEY_VALUE!（验证通过）"
) else (
    call :print_error "!DEBUGKEY_PROP! 设置验证失败，期望值=!DEBUG_KEY_VALUE!，实际值=!ACTUAL_DEBUGKEY!"
    goto :eof
)

:: 重启指定进程
echo.
call :print_warning "请输入需要重启的进程包名："
set "PACKAGE_NAME="
set /p "PACKAGE_NAME=包名: "
if "!PACKAGE_NAME!"=="" (
    call :print_error "包名不能为空"
    goto :eof
)

call :print_info "正在查询进程 !PACKAGE_NAME! ..."
set "PROC_COUNT=0"
for /f "tokens=1,2,*" %%a in ('!ADB! shell "ps" 2^>nul ^| findstr "!PACKAGE_NAME!" ^| findstr /v "findstr"') do (
    set "PIDS[!PROC_COUNT!]=%%b"
    set "PROC_LINES[!PROC_COUNT!]=%%a %%b %%c"
    set /a "PROC_COUNT+=1"
)

if !PROC_COUNT! equ 0 (
    call :print_error "未找到与 '!PACKAGE_NAME!' 匹配的进程"
    goto :eof
)

:: 显示进程列表
if !PROC_COUNT! equ 1 (
    call :print_info "找到 1 个匹配进程（PID: !PIDS[0]!）："
    echo   !PROC_LINES[0]!
    set "TARGET_COUNT=1"
    set "TARGET_PIDS[0]=!PIDS[0]!"
) else (
    call :print_warning "找到 !PROC_COUNT! 个匹配进程，请选择要 kill 的进程："
    for /l %%i in (0,1,!PROC_COUNT!) do (
        if %%i lss !PROC_COUNT! (
            set /a "display=%%i+1"
            echo   !GREEN!!display!!NC!. PID=!PIDS[%%i]!  !PROC_LINES[%%i]!
        )
    )
    echo.
    call :print_info "可输入多个序号（用空格或逗号分隔），输入 all 表示全部 kill"
    set "proc_input="
    set /p "proc_input=请输入进程序号: "

    set "TARGET_COUNT=0"
    if /i "!proc_input!"=="all" (
        for /l %%i in (0,1,!PROC_COUNT!) do (
            if %%i lss !PROC_COUNT! (
                set "TARGET_PIDS[!TARGET_COUNT!]=!PIDS[%%i]!"
                set /a "TARGET_COUNT+=1"
            )
        )
    ) else (
        set "proc_input=!proc_input:,= !"
        for %%n in (!proc_input!) do (
            set /a "pidx=%%n-1"
            if %%n geq 1 if %%n leq !PROC_COUNT! (
                for %%x in (!pidx!) do (
                    set "TARGET_PIDS[!TARGET_COUNT!]=!PIDS[%%x]!"
                    set /a "TARGET_COUNT+=1"
                )
            )
        )
    )
)

:: Kill 进程
set "KILL_FAILED=0"
for /l %%i in (0,1,!TARGET_COUNT!) do (
    if %%i lss !TARGET_COUNT! (
        call :print_info "正在 kill 进程 PID=!TARGET_PIDS[%%i]! ..."
        !ADB! shell "kill !TARGET_PIDS[%%i]!"
        if !ERRORLEVEL! neq 0 (
            call :print_error "kill 进程 PID=!TARGET_PIDS[%%i]! 失败"
            set "KILL_FAILED=1"
        ) else (
            call :print_success "已发送 kill 信号给进程 PID=!TARGET_PIDS[%%i]!"
        )
    )
)

if "!KILL_FAILED!"=="1" (
    call :print_error "部分进程 kill 失败，请检查后重试"
    goto :eof
)

:: 验证进程重启
call :print_info "等待进程重新启动..."
timeout /t 3 /nobreak >nul

set "NEW_PROC_COUNT=0"
for /f "tokens=2" %%a in ('!ADB! shell "ps" 2^>nul ^| findstr "!PACKAGE_NAME!" ^| findstr /v "findstr"') do (
    set "NEW_PIDS[!NEW_PROC_COUNT!]=%%a"
    set /a "NEW_PROC_COUNT+=1"
)

if !NEW_PROC_COUNT! equ 0 (
    timeout /t 3 /nobreak >nul
    for /f "tokens=2" %%a in ('!ADB! shell "ps" 2^>nul ^| findstr "!PACKAGE_NAME!" ^| findstr /v "findstr"') do (
        set "NEW_PIDS[!NEW_PROC_COUNT!]=%%a"
        set /a "NEW_PROC_COUNT+=1"
    )
)

if !NEW_PROC_COUNT! equ 0 (
    call :print_error "进程 '!PACKAGE_NAME!' 未能重新启动，请手动检查"
    goto :eof
)

echo.
call :print_success "============================================"
call :print_success "  埋点UT实时上报配置完成！"
call :print_success "  - persist.ut.realtime.enable = 1"
call :print_success "  - ut.realtime.debugkey = !DEBUG_KEY_VALUE!"
call :print_success "  - 进程 !PACKAGE_NAME! 已重启"
call :print_success "============================================"
goto :eof

:: ==============================================================================
:: 功能 3: 横竖屏切换
:: ==============================================================================
:func_rotation
call :print_divider
call :print_info "【横竖屏切换】"
call :print_divider

echo   !GREEN!0!NC! = 竖屏（Portrait）
echo   !GREEN!1!NC! = 横屏（Landscape）

:rotation_loop
set "rotation_value="
set /p "rotation_value=请选择 [0/1]: "
if "!rotation_value!"=="0" goto :rotation_do
if "!rotation_value!"=="1" goto :rotation_do
call :print_error "无效输入，请输入 0 或 1"
goto :rotation_loop

:rotation_do
!ADB! shell settings put system user_rotation !rotation_value!
if !ERRORLEVEL! equ 0 (
    if "!rotation_value!"=="0" (set "mode=竖屏") else (set "mode=横屏")
    call :print_success "已切换为!mode!模式"
) else (
    call :print_error "切换失败"
)
goto :eof

:: ==============================================================================
:: 功能 4: 屏幕分辨率修改
:: ==============================================================================
:func_resolution
call :print_divider
call :print_info "【屏幕分辨率修改】"
call :print_divider

echo   !GREEN!1!NC!. 修改 density（DPI）
echo   !GREEN!2!NC!. 修改 size（分辨率）
echo   !GREEN!3!NC!. 重置为默认值
echo   !GREEN!4!NC!. 预设：1920x1080 / density 160
echo   !GREEN!5!NC!. 预设：2160x3840 / density 480

set "res_choice="
set /p "res_choice=请选择操作 [1-5]: "

if "!res_choice!"=="1" (
    set "density_val="
    set /p "density_val=请输入 density 值（如 160、320、480）: "
    if not "!density_val!"=="" (
        !ADB! shell wm density !density_val!
        call :print_success "density 已设置为 !density_val!"
    ) else (
        call :print_error "输入不能为空"
    )
) else if "!res_choice!"=="2" (
    set "size_val="
    set /p "size_val=请输入 size 值（如 1920x1080）: "
    if not "!size_val!"=="" (
        !ADB! shell wm size !size_val!
        call :print_success "size 已设置为 !size_val!"
    ) else (
        call :print_error "输入不能为空"
    )
) else if "!res_choice!"=="3" (
    !ADB! shell wm size reset
    !ADB! shell wm density reset
    call :print_success "分辨率和 density 已重置为默认值"
) else if "!res_choice!"=="4" (
    !ADB! shell wm size 1920x1080
    !ADB! shell wm density 160
    call :print_success "已设置为 1920x1080 / density 160"
) else if "!res_choice!"=="5" (
    !ADB! shell wm size 2160x3840
    !ADB! shell wm density 480
    call :print_success "已设置为 2160x3840 / density 480"
) else (
    call :print_error "无效选择"
)
goto :eof

:: ==============================================================================
:: 功能 5: 发送语音命令
:: ==============================================================================
:func_voice_command
call :print_divider
call :print_info "【发送语音命令】"
call :print_divider

set "voice_text="
set /p "voice_text=请输入语音文本: "
if "!voice_text!"=="" (
    call :print_error "语音文本不能为空"
    goto :eof
)

!ADB! shell am start -d "waft://com.alibaba.genie.waft.agcs/test?input=!voice_text!"
if !ERRORLEVEL! equ 0 (
    call :print_success "语音命令已发送：!voice_text!"
) else (
    call :print_error "语音命令发送失败"
)
goto :eof


:: ==============================================================================
:: 功能 6: 关闭模拟器
:: ==============================================================================
:func_close_emulator
call :print_divider
call :print_info "【关闭模拟器】"
call :print_divider

set "EMU_COUNT=0"
for /f "tokens=1,2" %%a in ('adb devices') do (
    echo %%a | findstr /b "emulator-" >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        if "%%b"=="device" (
            set "EMUS[!EMU_COUNT!]=%%a"
            set /a "EMU_COUNT+=1"
        )
    )
)

if !EMU_COUNT! equ 0 (
    call :print_warning "未检测到运行中的模拟器"
    goto :eof
)

for /l %%i in (0,1,!EMU_COUNT!) do (
    if %%i lss !EMU_COUNT! (
        call :print_info "正在关闭模拟器：!EMUS[%%i]! ..."
        adb -s !EMUS[%%i]! emu kill 2>nul
        if !ERRORLEVEL! equ 0 (
            call :print_success "模拟器 !EMUS[%%i]! 已关闭"
        ) else (
            call :print_error "关闭模拟器 !EMUS[%%i]! 失败"
        )
    )
)
goto :eof

:: ==============================================================================
:: 功能 7: 查看顶层 Activity
:: ==============================================================================
:func_top_activity
call :print_divider
call :print_info "【查看顶层 Activity】"
call :print_divider

set "FOUND_ACTIVITY=0"
for /f "tokens=*" %%i in ('!ADB! shell dumpsys activity activities 2^>nul ^| findstr "ResumedActivity mResumedActivity"') do (
    if "!FOUND_ACTIVITY!"=="0" call :print_success "当前顶层 Activity："
    echo %%i
    set "FOUND_ACTIVITY=1"
)

if "!FOUND_ACTIVITY!"=="0" (
    call :print_warning "未找到 ResumedActivity 信息"
)
goto :eof

:: ==============================================================================
:: 功能 8: 查看 app 版本号
:: ==============================================================================
:func_app_version
call :print_divider
call :print_info "【查看 app 版本号】"
call :print_divider

set "pkg_name="
set /p "pkg_name=请输入包名: "
if "!pkg_name!"=="" (
    call :print_error "包名不能为空"
    goto :eof
)

set "FOUND_VER=0"
for /f "tokens=*" %%i in ('!ADB! shell dumpsys package !pkg_name! 2^>nul ^| findstr "versionCode versionName"') do (
    if "!FOUND_VER!"=="0" call :print_success "包 !pkg_name! 版本信息："
    echo %%i
    set "FOUND_VER=1"
)

if "!FOUND_VER!"=="0" (
    call :print_error "未找到包 !pkg_name! 的版本信息"
)
goto :eof

:: ==============================================================================
:: 功能 9: 查询设备 UUID
:: ==============================================================================
:func_device_uuid
call :print_divider
call :print_info "【查询设备 UUID】"
call :print_divider

set "FOUND_UUID=0"
for /f "tokens=*" %%i in ('!ADB! shell getprop 2^>nul ^| findstr /i "uuid"') do (
    if "!FOUND_UUID!"=="0" call :print_success "设备 UUID 信息："
    echo %%i
    set "FOUND_UUID=1"
)

if "!FOUND_UUID!"=="0" (
    call :print_warning "未找到 UUID 相关属性"
)
goto :eof

:: ==============================================================================
:: 功能 10: 进程管理
:: ==============================================================================
:func_process_manage
call :print_divider
call :print_info "【进程管理】"
call :print_divider

echo   !GREEN!1!NC!. 查看进程（关键字查询）
echo   !GREEN!2!NC!. Kill 进程（输入 PID）
echo   !GREEN!3!NC!. 强制停止 app（输入包名）
echo   !GREEN!4!NC!. 通过包名 kill 进程（支持多进程选择）

set "proc_choice="
set /p "proc_choice=请选择操作 [1-4]: "

if "!proc_choice!"=="1" (
    set "keyword="
    set /p "keyword=请输入查询关键字: "
    if "!keyword!"=="" (
        call :print_error "关键字不能为空"
        goto :eof
    )
    set "FOUND_PROC=0"
    for /f "tokens=*" %%i in ('!ADB! shell "ps" 2^>nul ^| findstr "!keyword!" ^| findstr /v "findstr"') do (
        if "!FOUND_PROC!"=="0" call :print_success "匹配进程列表："
        echo %%i
        set "FOUND_PROC=1"
    )
    if "!FOUND_PROC!"=="0" call :print_warning "未找到匹配 '!keyword!' 的进程"
) else if "!proc_choice!"=="2" (
    set "kill_pid="
    set /p "kill_pid=请输入要 kill 的进程 PID: "
    if "!kill_pid!"=="" (
        call :print_error "PID 不能为空"
        goto :eof
    )
    !ADB! shell "kill !kill_pid!"
    if !ERRORLEVEL! equ 0 (
        call :print_success "已发送 kill 信号给 PID=!kill_pid!"
    ) else (
        call :print_error "kill 失败"
    )
) else if "!proc_choice!"=="3" (
    set "stop_pkg="
    set /p "stop_pkg=请输入要强制停止的包名: "
    if "!stop_pkg!"=="" (
        call :print_error "包名不能为空"
        goto :eof
    )
    !ADB! shell "am force-stop !stop_pkg!"
    if !ERRORLEVEL! equ 0 (
        call :print_success "已强制停止 !stop_pkg!"
    ) else (
        call :print_error "强制停止失败"
    )
) else if "!proc_choice!"=="4" (
    set "pkg_name="
    set /p "pkg_name=请输入包名（支持部分匹配）: "
    if "!pkg_name!"=="" (
        call :print_error "包名不能为空"
        goto :eof
    )

    set "P_COUNT=0"
    for /f "tokens=1,2,*" %%a in ('!ADB! shell "ps" 2^>nul ^| findstr "!pkg_name!" ^| findstr /v "findstr"') do (
        set "P_PIDS[!P_COUNT!]=%%b"
        set /a "P_DISP=!P_COUNT!+1"
        echo   !GREEN!!P_DISP!!NC!. PID=%%b  %%a %%b %%c
        set /a "P_COUNT+=1"
    )

    if !P_COUNT! equ 0 (
        call :print_warning "未找到包含 '!pkg_name!' 的进程"
        goto :eof
    )

    echo.
    call :print_info "共找到 !P_COUNT! 个进程"
    call :print_warning "请输入要 kill 的进程编号（多个用空格或逗号分隔，输入 all 表示全部）:"
    set "kill_input="
    set /p "kill_input=> "

    if "!kill_input!"=="" (
        call :print_error "输入不能为空"
        goto :eof
    )

    set "K_COUNT=0"
    if /i "!kill_input!"=="all" (
        for /l %%i in (0,1,!P_COUNT!) do (
            if %%i lss !P_COUNT! (
                set "K_PIDS[!K_COUNT!]=!P_PIDS[%%i]!"
                set /a "K_COUNT+=1"
            )
        )
    ) else (
        set "kill_input=!kill_input:,= !"
        for %%n in (!kill_input!) do (
            set /a "kidx=%%n-1"
            if %%n geq 1 if %%n leq !P_COUNT! (
                for %%x in (!kidx!) do (
                    set "K_PIDS[!K_COUNT!]=!P_PIDS[%%x]!"
                    set /a "K_COUNT+=1"
                )
            ) else (
                call :print_warning "忽略无效编号: %%n"
            )
        )
    )

    if !K_COUNT! equ 0 (
        call :print_error "未选择有效的进程"
        goto :eof
    )

    echo.
    for /l %%i in (0,1,!K_COUNT!) do (
        if %%i lss !K_COUNT! (
            !ADB! shell "kill !K_PIDS[%%i]!" 2>nul
            if !ERRORLEVEL! equ 0 (
                call :print_success "已 kill PID=!K_PIDS[%%i]!"
            ) else (
                call :print_error "kill PID=!K_PIDS[%%i]! 失败"
            )
        )
    )

    timeout /t 1 /nobreak >nul
    echo.
    call :print_info "当前 '!pkg_name!' 进程状态："
    set "REM_FOUND=0"
    for /f "tokens=*" %%i in ('!ADB! shell "ps" 2^>nul ^| findstr "!pkg_name!" ^| findstr /v "findstr"') do (
        echo %%i
        set "REM_FOUND=1"
    )
    if "!REM_FOUND!"=="0" call :print_warning "已无匹配进程"
) else (
    call :print_error "无效选择"
)
goto :eof

:: ==============================================================================
:: 功能 11: 打开 Deeplink
:: ==============================================================================
:func_deeplink
call :print_divider
call :print_info "【打开 Deeplink】"
call :print_divider

set "deeplink_uri="
set /p "deeplink_uri=请输入 Deeplink URI: "
if "!deeplink_uri!"=="" (
    call :print_error "URI 不能为空"
    goto :eof
)

!ADB! shell am start -W -a android.intent.action.VIEW -d "!deeplink_uri!"
if !ERRORLEVEL! equ 0 (
    call :print_success "Deeplink 已打开：!deeplink_uri!"
) else (
    call :print_error "打开 Deeplink 失败"
)
goto :eof

:: ==============================================================================
:: 功能 12: 打开应用/管理应用
:: ==============================================================================
:func_app_manage
call :print_divider
call :print_info "【打开应用/管理应用】"
call :print_divider

echo   !GREEN!1!NC!. 启动 app（输入包名）
echo   !GREEN!2!NC!. 打开指定 Activity
echo   !GREEN!3!NC!. 通过 Deeplink 打开
echo   !GREEN!4!NC!. 发送开机广播
echo   !GREEN!5!NC!. 启动 Service（输入 component）
echo   !GREEN!6!NC!. 清除 app 数据（输入包名）
echo   !GREEN!7!NC!. 查看安装路径（输入包名）

set "app_choice="
set /p "app_choice=请选择操作 [1-7]: "

if "!app_choice!"=="1" (
    set "launch_pkg="
    set /p "launch_pkg=请输入包名: "
    if "!launch_pkg!"=="" (
        call :print_error "包名不能为空"
        goto :eof
    )
    !ADB! shell monkey -p !launch_pkg! -c android.intent.category.LAUNCHER 1 2>nul
    call :print_success "已尝试启动 !launch_pkg!"
) else if "!app_choice!"=="2" (
    call :print_info "格式示例: com.example.app/.MainActivity 或 com.example.app/com.example.app.MainActivity"
    set "activity_component="
    set /p "activity_component=请输入 Activity component: "
    if "!activity_component!"=="" (
        call :print_error "Activity component 不能为空"
        goto :eof
    )
    !ADB! shell am start -n "!activity_component!"
    if !ERRORLEVEL! equ 0 (
        call :print_success "Activity 已打开：!activity_component!"
    ) else (
        call :print_error "打开 Activity 失败"
    )
) else if "!app_choice!"=="3" (
    call :print_info "格式示例: genie://com.android.settings/bt?open=true"
    set "deeplink_uri="
    set /p "deeplink_uri=请输入 Deeplink URI: "
    if "!deeplink_uri!"=="" (
        call :print_error "Deeplink URI 不能为空"
        goto :eof
    )
    !ADB! shell am start -W -a android.intent.action.VIEW -d "!deeplink_uri!"
    if !ERRORLEVEL! equ 0 (
        call :print_success "Deeplink 已打开：!deeplink_uri!"
    ) else (
        call :print_error "打开 Deeplink 失败"
    )
) else if "!app_choice!"=="4" (
    call :print_info "正在发送开机广播..."
    !ADB! shell am broadcast -a android.intent.action.BOOT_COMPLETED
    call :print_success "开机广播已发送"
) else if "!app_choice!"=="5" (
    set "component="
    set /p "component=请输入 Service component（如 com.example/.MyService）: "
    if "!component!"=="" (
        call :print_error "component 不能为空"
        goto :eof
    )
    !ADB! shell am startservice -n "!component!"
    if !ERRORLEVEL! equ 0 (
        call :print_success "Service 已启动：!component!"
    ) else (
        call :print_error "启动 Service 失败"
    )
) else if "!app_choice!"=="6" (
    set "clear_pkg="
    set /p "clear_pkg=请输入要清除数据的包名: "
    if "!clear_pkg!"=="" (
        call :print_error "包名不能为空"
        goto :eof
    )
    !ADB! shell pm clear !clear_pkg!
    call :print_success "已清除 !clear_pkg! 的数据"
) else if "!app_choice!"=="7" (
    set "path_pkg="
    set /p "path_pkg=请输入包名: "
    if "!path_pkg!"=="" (
        call :print_error "包名不能为空"
        goto :eof
    )
    set "FOUND_PATH=0"
    for /f "tokens=*" %%i in ('!ADB! shell pm path !path_pkg! 2^>nul') do (
        if "!FOUND_PATH!"=="0" call :print_success "!path_pkg! 安装路径："
        echo %%i
        set "FOUND_PATH=1"
    )
    if "!FOUND_PATH!"=="0" call :print_error "未找到包 !path_pkg!"
) else (
    call :print_error "无效选择"
)
goto :eof


:: ==============================================================================
:: 功能 13: 系统属性管理
:: ==============================================================================
:func_system_property
call :print_divider
call :print_info "【系统属性管理】"
call :print_divider

echo   !GREEN!1!NC!. 获取 settings 值
echo   !GREEN!2!NC!. 设置 settings 值
echo   !GREEN!3!NC!. getprop 查看属性
echo   !GREEN!4!NC!. setprop 设置属性

set "prop_choice="
set /p "prop_choice=请选择操作 [1-4]: "

if "!prop_choice!"=="1" (
    echo   命名空间：!GREEN!secure!NC! / !GREEN!system!NC! / !GREEN!global!NC!
    set "namespace="
    set /p "namespace=请输入命名空间: "
    set "settings_key="
    set /p "settings_key=请输入 key: "
    if "!namespace!"=="" if "!settings_key!"=="" (
        call :print_error "命名空间和 key 不能为空"
        goto :eof
    )
    set "RESULT="
    for /f "tokens=*" %%i in ('!ADB! shell settings get !namespace! !settings_key! 2^>nul') do set "RESULT=%%i"
    call :print_success "!namespace!/!settings_key! = !RESULT!"
) else if "!prop_choice!"=="2" (
    echo   命名空间：!GREEN!secure!NC! / !GREEN!system!NC! / !GREEN!global!NC!
    set "namespace="
    set /p "namespace=请输入命名空间: "
    set "settings_key="
    set /p "settings_key=请输入 key: "
    set "settings_value="
    set /p "settings_value=请输入 value: "
    if "!namespace!"=="" (
        call :print_error "命名空间、key 和 value 不能为空"
        goto :eof
    )
    if "!settings_key!"=="" (
        call :print_error "命名空间、key 和 value 不能为空"
        goto :eof
    )
    if "!settings_value!"=="" (
        call :print_error "命名空间、key 和 value 不能为空"
        goto :eof
    )
    !ADB! shell settings put !namespace! !settings_key! !settings_value!
    call :print_success "已设置 !namespace!/!settings_key! = !settings_value!"
) else if "!prop_choice!"=="3" (
    set "prop_name="
    set /p "prop_name=请输入属性名（留空查看所有）: "
    if "!prop_name!"=="" (
        !ADB! shell getprop
    ) else (
        set "RESULT="
        for /f "tokens=*" %%i in ('!ADB! shell getprop !prop_name! 2^>nul') do set "RESULT=%%i"
        call :print_success "!prop_name! = !RESULT!"
    )
) else if "!prop_choice!"=="4" (
    call :ensure_root
    if !ERRORLEVEL! neq 0 goto :eof
    set "prop_name="
    set /p "prop_name=请输入属性名: "
    set "prop_value="
    set /p "prop_value=请输入属性值: "
    if "!prop_name!"=="" (
        call :print_error "属性名和值不能为空"
        goto :eof
    )
    if "!prop_value!"=="" (
        call :print_error "属性名和值不能为空"
        goto :eof
    )
    !ADB! shell "setprop !prop_name! !prop_value!"
    timeout /t 1 /nobreak >nul
    set "ACTUAL="
    for /f "tokens=*" %%i in ('!ADB! shell "getprop !prop_name!" 2^>nul') do set "ACTUAL=%%i"
    if "!ACTUAL!"=="!prop_value!" (
        call :print_success "!prop_name! = !prop_value!（验证通过）"
    ) else (
        call :print_error "设置验证失败，实际值=!ACTUAL!"
    )
) else (
    call :print_error "无效选择"
)
goto :eof

:: ==============================================================================
:: 功能 14: Content Provider 读取
:: ==============================================================================
:func_content_read
call :print_divider
call :print_info "【Content Provider 读取】"
call :print_divider

set "content_uri="
set /p "content_uri=请输入 Content URI: "
if "!content_uri!"=="" (
    call :print_error "URI 不能为空"
    goto :eof
)

set "FOUND_CONTENT=0"
for /f "tokens=*" %%i in ('!ADB! shell content read --uri "!content_uri!" 2^>^&1') do (
    if "!FOUND_CONTENT!"=="0" call :print_success "读取结果："
    echo %%i
    set "FOUND_CONTENT=1"
)
if "!FOUND_CONTENT!"=="0" call :print_warning "未返回数据"
goto :eof

:: ==============================================================================
:: 功能 15: 录屏/截屏
:: ==============================================================================
:func_screen_capture
call :print_divider
call :print_info "【录屏/截屏】"
call :print_divider

echo   !GREEN!1!NC!. 录屏
echo   !GREEN!2!NC!. 截屏并 pull 到本地

set "capture_choice="
set /p "capture_choice=请选择操作 [1-2]: "

if "!capture_choice!"=="1" (
    set "duration="
    set /p "duration=录屏时长（秒，默认 30）: "
    if "!duration!"=="" set "duration=30"
    set "resolution="
    set /p "resolution=分辨率（如 1280x720，留空使用默认）: "
    set "bitrate="
    set /p "bitrate=码率（如 4000000，留空使用默认）: "

    :: 生成时间戳文件名
    for /f "tokens=2 delims==" %%i in ('wmic os get localdatetime /format:list 2^>nul') do set "dt=%%i"
    set "TIMESTAMP=!dt:~0,4!!dt:~4,2!!dt:~6,2!_!dt:~8,2!!dt:~10,2!!dt:~12,2!"
    set "REMOTE_FILE=/sdcard/screenrecord_!TIMESTAMP!.mp4"
    set "CMD=screenrecord --time-limit !duration!"
    if not "!resolution!"=="" set "CMD=!CMD! --size !resolution!"
    if not "!bitrate!"=="" set "CMD=!CMD! --bit-rate !bitrate!"
    set "CMD=!CMD! !REMOTE_FILE!"

    call :print_info "开始录屏（!duration!秒）... 按 Ctrl+C 可提前停止"
    !ADB! shell !CMD!
    call :print_success "录屏完成：!REMOTE_FILE!"

    set "pull_choice="
    set /p "pull_choice=是否 pull 到本地？[y/N]: "
    if /i "!pull_choice!"=="y" (
        for %%f in (!REMOTE_FILE!) do set "LOCAL_FILE=%%~nxf"
        !ADB! pull !REMOTE_FILE! !LOCAL_FILE!
        call :print_success "已保存到：!LOCAL_FILE!"
    )
) else if "!capture_choice!"=="2" (
    for /f "tokens=2 delims==" %%i in ('wmic os get localdatetime /format:list 2^>nul') do set "dt=%%i"
    set "TIMESTAMP=!dt:~0,4!!dt:~4,2!!dt:~6,2!_!dt:~8,2!!dt:~10,2!!dt:~12,2!"
    set "REMOTE_FILE=/sdcard/screenshot_!TIMESTAMP!.png"
    !ADB! shell screencap -p !REMOTE_FILE!
    for %%f in (!REMOTE_FILE!) do set "LOCAL_FILE=%%~nxf"
    !ADB! pull !REMOTE_FILE! !LOCAL_FILE!
    if !ERRORLEVEL! equ 0 (
        call :print_success "截屏已保存到：!LOCAL_FILE!"
    ) else (
        call :print_error "截屏失败"
    )
) else (
    call :print_error "无效选择"
)
goto :eof

:: ==============================================================================
:: 功能 16: 过度绘制开关
:: ==============================================================================
:func_overdraw
call :print_divider
call :print_info "【过度绘制开关】"
call :print_divider

echo   !GREEN!1!NC!. 开启过度绘制检测
echo   !GREEN!2!NC!. 关闭过度绘制检测

set "overdraw_choice="
set /p "overdraw_choice=请选择 [1/2]: "

if "!overdraw_choice!"=="1" (
    !ADB! shell setprop debug.hwui.overdraw show
    call :print_success "过度绘制检测已开启"
) else if "!overdraw_choice!"=="2" (
    !ADB! shell setprop debug.hwui.overdraw false
    call :print_success "过度绘制检测已关闭"
) else (
    call :print_error "无效选择"
)
goto :eof

:: ==============================================================================
:: 功能 17: 查看系统音量
:: ==============================================================================
:func_audio_info
call :print_divider
call :print_info "【查看系统音量】"
call :print_divider

!ADB! shell dumpsys audio 2>nul | findstr /c:"STREAM_"
goto :eof

:: ==============================================================================
:: 功能 18: 退出工厂模式
:: ==============================================================================
:func_exit_factory
call :print_divider
call :print_info "【退出工厂模式】"
call :print_divider

call :ensure_root
if !ERRORLEVEL! neq 0 goto :eof

call :print_info "正在执行退出工厂模式..."
!ADB! shell "dumpsys activity service com.alibaba.ailabs.genie.smartapp/.service.SmartAppService exitFactory" 2>nul
timeout /t 2 /nobreak >nul

set "reboot_choice="
set /p "reboot_choice=是否重启设备？[y/N]: "
if /i "!reboot_choice!"=="y" (
    !ADB! reboot
    call :print_success "重启命令已发送"
) else (
    call :print_success "退出工厂模式命令已执行"
)
goto :eof

:: ==============================================================================
:: 功能 19: ANR 日志导出
:: ==============================================================================
:func_anr_export
call :print_divider
call :print_info "【ANR 日志导出】"
call :print_divider

for /f "tokens=2 delims==" %%i in ('wmic os get localdatetime /format:list 2^>nul') do set "dt=%%i"
set "TIMESTAMP=!dt:~0,4!!dt:~4,2!!dt:~6,2!_!dt:~8,2!!dt:~10,2!!dt:~12,2!"
set "LOCAL_DIR=anr_logs_!TIMESTAMP!"
mkdir "!LOCAL_DIR!" 2>nul

call :print_info "正在导出 /data/anr/ 目录..."
!ADB! pull /data/anr/ "!LOCAL_DIR!/"

if !ERRORLEVEL! equ 0 (
    call :print_success "ANR 日志已导出到：!LOCAL_DIR!"
) else (
    call :print_error "导出失败，可能需要 root 权限"
    call :ensure_root
    !ADB! pull /data/anr/ "!LOCAL_DIR!/"
    if !ERRORLEVEL! equ 0 (
        call :print_success "ANR 日志已导出到：!LOCAL_DIR!"
    ) else (
        call :print_error "导出失败"
    )
)
goto :eof


:: ==============================================================================
:: 功能 20: 日志筛选操作
:: ==============================================================================
:func_logcat
call :print_divider
call :print_info "【日志筛选操作】"
call :print_divider

echo   !GREEN!1!NC!. 实时 logcat + 关键字过滤
echo   !GREEN!2!NC!. 从所有缓冲区获取日志
echo   !GREEN!3!NC!. 多关键词筛选（用 ^| 分隔）

set "log_choice="
set /p "log_choice=请选择操作 [1-3]: "

if "!log_choice!"=="1" (
    set "log_keyword="
    set /p "log_keyword=请输入过滤关键字（留空显示全部）: "
    call :print_warning "按 Ctrl+C 停止日志输出"
    if "!log_keyword!"=="" (
        !ADB! logcat
    ) else (
        !ADB! logcat | findstr "!log_keyword!"
    )
) else if "!log_choice!"=="2" (
    call :print_info "获取所有缓冲区日志（main/system/crash/events）..."
    call :print_warning "按 Ctrl+C 停止日志输出"
    !ADB! logcat -b all
) else if "!log_choice!"=="3" (
    set "multi_keywords="
    set /p "multi_keywords=请输入多个关键词（用空格分隔，如 Error Exception ANR）: "
    if "!multi_keywords!"=="" (
        call :print_error "关键词不能为空"
        goto :eof
    )
    call :print_warning "按 Ctrl+C 停止日志输出"
    !ADB! logcat | findstr "!multi_keywords!"
) else (
    call :print_error "无效选择"
)
goto :eof

:: ==============================================================================
:: 功能 21: Monkey 测试
:: ==============================================================================
:func_monkey_test
call :print_divider
call :print_info "【Monkey 测试】"
call :print_divider

set "monkey_pkg="
set /p "monkey_pkg=请输入目标包名: "
if "!monkey_pkg!"=="" (
    call :print_error "包名不能为空"
    goto :eof
)

set "DEFAULT_EVENTS=10000"
set "DEFAULT_THROTTLE=500"
set "DEFAULT_SEED=12345"

echo.
call :print_info "默认参数：事件数=!DEFAULT_EVENTS!, 间隔=!DEFAULT_THROTTLE!ms, seed=!DEFAULT_SEED!"
set "events="
set /p "events=事件数（默认 !DEFAULT_EVENTS!）: "
set "throttle="
set /p "throttle=间隔毫秒（默认 !DEFAULT_THROTTLE!）: "
set "seed="
set /p "seed=seed 值（默认 !DEFAULT_SEED!）: "

if "!events!"=="" set "events=!DEFAULT_EVENTS!"
if "!throttle!"=="" set "throttle=!DEFAULT_THROTTLE!"
if "!seed!"=="" set "seed=!DEFAULT_SEED!"

call :print_info "开始 Monkey 测试：包=!monkey_pkg!, 事件数=!events!, 间隔=!throttle!ms, seed=!seed!"
!ADB! shell monkey -p !monkey_pkg! -s !seed! --throttle !throttle! --ignore-crashes --ignore-timeouts -v !events!
call :print_success "Monkey 测试已完成"
goto :eof

:: ==============================================================================
:: 功能 22: 查看运行时长
:: ==============================================================================
:func_runtime
call :print_divider
call :print_info "【查看运行时长】"
call :print_divider

set "runtime_keyword="
set /p "runtime_keyword=请输入包名关键字: "
if "!runtime_keyword!"=="" (
    call :print_error "关键字不能为空"
    goto :eof
)

set "FOUND_RT=0"
for /f "tokens=*" %%i in ('!ADB! shell "ps -o CMD,ETIME" 2^>nul ^| findstr "!runtime_keyword!"') do (
    if "!FOUND_RT!"=="0" call :print_success "运行时长信息："
    echo %%i
    set "FOUND_RT=1"
)
if "!FOUND_RT!"=="0" call :print_warning "未找到匹配 '!runtime_keyword!' 的进程"
goto :eof

:: ==============================================================================
:: 功能 23: 内存相关
:: ==============================================================================
:func_memory
call :print_divider
call :print_info "【内存相关】"
call :print_divider

echo   !GREEN!1!NC!. GC 内存回收查看
echo   !GREEN!2!NC!. 低内存模拟
echo   !GREEN!3!NC!. 查看 app 内存信息
echo   !GREEN!4!NC!. 获取 dumpheap

set "mem_choice="
set /p "mem_choice=请选择操作 [1-4]: "

if "!mem_choice!"=="1" (
    set "gc_pkg="
    set /p "gc_pkg=请输入包名: "
    if "!gc_pkg!"=="" (
        call :print_error "包名不能为空"
        goto :eof
    )
    call :print_info "发送 GC 信号..."
    :: 获取 PID 后发送 signal 10
    set "GC_PID="
    for /f "tokens=*" %%i in ('!ADB! shell "pidof !gc_pkg!" 2^>nul') do set "GC_PID=%%i"
    if not "!GC_PID!"=="" (
        !ADB! shell "kill -10 !GC_PID!" 2>nul
    )
    timeout /t 2 /nobreak >nul
    call :print_info "查看内存信息..."
    !ADB! shell dumpsys meminfo !gc_pkg!
) else if "!mem_choice!"=="2" (
    call :print_info "模拟低内存环境..."
    call :ensure_root
    if !ERRORLEVEL! neq 0 goto :eof
    !ADB! shell "echo 1 > /proc/sys/vm/drop_caches"
    call :print_success "已触发内存回收"
) else if "!mem_choice!"=="3" (
    set "meminfo_pkg="
    set /p "meminfo_pkg=请输入包名: "
    if "!meminfo_pkg!"=="" (
        call :print_error "包名不能为空"
        goto :eof
    )
    !ADB! shell dumpsys meminfo !meminfo_pkg!
) else if "!mem_choice!"=="4" (
    set "heap_pkg="
    set /p "heap_pkg=请输入包名: "
    if "!heap_pkg!"=="" (
        call :print_error "包名不能为空"
        goto :eof
    )
    for /f "tokens=2 delims==" %%i in ('wmic os get localdatetime /format:list 2^>nul') do set "dt=%%i"
    set "TIMESTAMP=!dt:~0,4!!dt:~4,2!!dt:~6,2!_!dt:~8,2!!dt:~10,2!!dt:~12,2!"
    set "REMOTE_FILE=/data/local/tmp/!heap_pkg!_heap_!TIMESTAMP!.hprof"
    call :print_info "正在获取 dumpheap..."
    !ADB! shell am dumpheap !heap_pkg! !REMOTE_FILE!
    timeout /t 3 /nobreak >nul
    for %%f in (!REMOTE_FILE!) do set "LOCAL_FILE=%%~nxf"
    !ADB! pull !REMOTE_FILE! !LOCAL_FILE!
    if !ERRORLEVEL! equ 0 (
        call :print_success "Heap dump 已保存到：!LOCAL_FILE!"
    ) else (
        call :print_error "Pull 文件失败"
    )
) else (
    call :print_error "无效选择"
)
goto :eof

:: ==============================================================================
:: 功能 24: 查找包名
:: ==============================================================================
:func_find_package
call :print_divider
call :print_info "【查找包名】"
call :print_divider

set "pkg_keyword="
set /p "pkg_keyword=请输入包名关键字: "
if "!pkg_keyword!"=="" (
    call :print_error "关键字不能为空"
    goto :eof
)

set "FOUND_PKG=0"
for /f "tokens=*" %%i in ('!ADB! shell "pm list packages" 2^>nul ^| findstr /i "!pkg_keyword!"') do (
    if "!FOUND_PKG!"=="0" call :print_success "匹配的包名列表："
    echo %%i
    set "FOUND_PKG=1"
)
if "!FOUND_PKG!"=="0" call :print_warning "未找到匹配 '!pkg_keyword!' 的包名"
goto :eof

:: ==============================================================================
:: 功能 25: 模拟电源键
:: ==============================================================================
:func_power_key
call :print_divider
call :print_info "【模拟电源键】"
call :print_divider

!ADB! shell input keyevent 26
if !ERRORLEVEL! equ 0 (
    call :print_success "已模拟电源键按下"
) else (
    call :print_error "模拟电源键失败"
)
goto :eof

:: ==============================================================================
:: 主菜单显示
:: ==============================================================================
:show_menu
cls
echo !BLUE!
echo ================================================================
call :print_center_line "ADB 综合工具箱 v1.0" 64
call :print_center_line "设备: !SELECTED_DEVICE!" 64
echo ================================================================
echo !NC!
echo.
echo   !GREEN! 1!NC!. 切换网络环境（线上/预发1/预发2）
echo   !GREEN! 2!NC!. 设置埋点UT实时上报验证
echo   !GREEN! 3!NC!. 横竖屏切换
echo   !GREEN! 4!NC!. 屏幕分辨率修改
echo   !GREEN! 5!NC!. 发送语音命令
echo   !GREEN! 6!NC!. 关闭模拟器
echo   !GREEN! 7!NC!. 查看顶层 Activity
echo   !GREEN! 8!NC!. 查看 app 版本号
echo   !GREEN! 9!NC!. 查询设备 UUID
echo   !GREEN!10!NC!. 进程管理（查看/kill/强制停止）
echo   !GREEN!11!NC!. 打开 Deeplink
echo   !GREEN!12!NC!. 打开应用/管理应用
echo   !GREEN!13!NC!. 系统属性管理（settings/prop）
echo   !GREEN!14!NC!. Content Provider 读取
echo   !GREEN!15!NC!. 录屏/截屏
echo   !GREEN!16!NC!. 过度绘制开关
echo   !GREEN!17!NC!. 查看系统音量
echo   !GREEN!18!NC!. 退出工厂模式
echo   !GREEN!19!NC!. ANR 日志导出
echo   !GREEN!20!NC!. 日志筛选操作
echo   !GREEN!21!NC!. Monkey 测试
echo   !GREEN!22!NC!. 查看运行时长
echo   !GREEN!23!NC!. 内存相关（GC/低内存/meminfo/dumpheap）
echo   !GREEN!24!NC!. 查找包名
echo   !GREEN!25!NC!. 模拟电源键
echo.
echo   !RED! 0!NC!. 退出
echo.
call :print_divider
goto :eof
