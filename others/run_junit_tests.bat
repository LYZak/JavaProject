@echo off
chcp 65001 >nul
echo ==========================================
echo BigComp Access Control System - Unit & Integration Tests
echo ==========================================
echo.

where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到 mvn 命令。
    echo 请先安装 Maven 并将其添加到系统环境变量 PATH 中。
    echo 您可以运行项目根目录下的 install_maven.ps1 脚本来自动安装。
    echo.
    pause
    exit /b 1
)

call mvn test
if %errorlevel% neq 0 (
    echo Tests Failed!
    pause
    exit /b 1
)

echo.
echo Tests Passed Successfully!
pause
