@echo off
chcp 65001 >nul
echo 正在使用Maven导出数据库...
echo.

REM 尝试使用Maven exec插件运行
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo 找到Maven，使用Maven运行...
    call mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.util.DatabaseExporter" -Dexec.classpathScope=compile
) else (
    echo Maven未找到，请确保Maven已安装并在PATH中
    echo.
    echo 或者您可以：
    echo 1. 安装Maven
    echo 2. 使用IDE运行 DatabaseExporter.main 方法
    echo 3. 运行 prepare_submission.bat 脚本（会自动生成db.sql）
    pause
    exit /b 1
)

if exist db.sql (
    echo.
    echo ========================================
    echo 成功！db.sql 文件已生成
    echo ========================================
    echo.
    dir db.sql
) else (
    echo.
    echo 错误：db.sql 文件未生成
)

pause

