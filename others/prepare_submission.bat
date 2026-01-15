@echo off
chcp 65001 >nul
echo ========================================
echo 准备项目提交文件
echo ========================================
echo.

REM 创建临时目录
if exist submission_temp rmdir /s /q submission_temp
mkdir submission_temp

echo [1/5] 编译项目...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo [2/5] 复制可执行jar文件...
copy target\access-control-system-1.0.0.jar submission_temp\access-control-system-1.0.0.jar
if errorlevel 1 (
    echo 复制jar文件失败！
    pause
    exit /b 1
)

echo.
echo [3/5] 创建sources.zip...
powershell -Command "Compress-Archive -Path src -DestinationPath submission_temp\sources.zip -Force"
if errorlevel 1 (
    echo 创建sources.zip失败！
    pause
    exit /b 1
)

echo.
echo [4/5] 创建files.zip（包含data和images目录）...
if exist submission_temp\files.zip del submission_temp\files.zip
powershell -Command "$files = @('data', 'images'); Compress-Archive -Path $files -DestinationPath submission_temp\files.zip -Force"
if errorlevel 1 (
    echo 创建files.zip失败！
    pause
    exit /b 1
)

echo.
echo [5/5] 导出数据库为MySQL格式...
call mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.util.DatabaseExporter" -Dexec.classpathScope=compile
if errorlevel 1 (
    echo 导出数据库失败！
    pause
    exit /b 1
)
if exist db.sql copy db.sql submission_temp\db.sql

echo.
echo [完成] 复制README.txt...
copy README.txt submission_temp\README.txt

echo.
echo ========================================
echo 提交文件准备完成！
echo ========================================
echo.
echo 文件位置：submission_temp 目录
echo.
echo 请检查以下文件：
echo   - README.txt
echo   - sources.zip
echo   - access-control-system-1.0.0.jar
echo   - files.zip
echo   - db.sql
echo.
echo 注意：您还需要准备：
echo   - project-report.pdf（项目报告）
echo   - 视频链接（Youku）
echo.
pause

