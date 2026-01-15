@echo off
chcp 65001 >nul
echo ========================================
echo 生成MySQL数据库文件 (db.sql)
echo ========================================
echo.

echo [1/2] 编译项目...
call mvn clean compile -DskipTests
if errorlevel 1 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo [2/2] 导出数据库为MySQL格式...
call mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.util.DatabaseExporter" -Dexec.classpathScope=compile
if errorlevel 1 (
    echo 导出失败！
    pause
    exit /b 1
)

echo.
if exist db.sql (
    echo ========================================
    echo 成功！db.sql 文件已生成在项目根目录
    echo ========================================
    echo.
    echo 文件位置: %CD%\db.sql
    echo.
    dir db.sql
) else (
    echo ========================================
    echo 错误：db.sql 文件未生成
    echo ========================================
)

pause

