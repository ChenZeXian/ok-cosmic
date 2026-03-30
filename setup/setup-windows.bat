@echo off
setlocal
REM ok-cosmic-knowledge 离线 API 知识图谱构建工具
REM 用法: setup-windows.bat [参数]

chcp 65001 >nul 2>nul

set "SCRIPT_DIR=%~dp0"
set "JAR_FILE=%SCRIPT_DIR%setup.jar"
set "EXIT_CODE=0"
set "PAUSE_ON_EXIT="

REM Explorer 双击通常通过 cmd.exe /c 启动，执行结束后窗口会立即关闭。
echo %CMDCMDLINE% | findstr /I /C:" /c " >nul && set "PAUSE_ON_EXIT=1"

if not exist "%JAR_FILE%" (
    echo 错误: setup.jar 文件不存在: "%JAR_FILE%"
    echo 请确认发布包完整，或重新获取包含 setup.jar 的安装包
    set "EXIT_CODE=1"
    goto end
)

where java >nul 2>nul
if errorlevel 1 (
    echo 错误: 未找到 java 命令，请先安装 JRE/JDK 并配置 PATH。
    set "EXIT_CODE=1"
    goto end
)

pushd "%SCRIPT_DIR%" >nul 2>nul
if errorlevel 1 (
    echo 错误: 无法进入脚本目录: "%SCRIPT_DIR%"
    set "EXIT_CODE=1"
    goto end
)

java -jar "%JAR_FILE%" %*
set "EXIT_CODE=%ERRORLEVEL%"
popd >nul

:end
if defined PAUSE_ON_EXIT (
    echo.
    if "%EXIT_CODE%"=="0" (
        echo 执行完成，按任意键关闭窗口...
    ) else (
        echo 执行失败，退出码: %EXIT_CODE%
        echo 按任意键关闭窗口...
    )
    pause >nul
)

exit /b %EXIT_CODE%
