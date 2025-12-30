@echo off
chcp 65001 >nul
echo ==========================================
echo BigComp 门禁控制系统 - 测试脚本
echo ==========================================
echo.

echo [1/2] 编译项目...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)
echo 编译成功！
echo.

echo [2/2] 运行测试脚本...
call mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.test.SystemTestRunner" -Dexec.args="" -q
if %errorlevel% neq 0 (
    echo 测试运行失败！
    pause
    exit /b 1
)
echo.

echo ==========================================
echo 测试完成！
echo ==========================================
echo.
echo 现在可以运行主程序：
echo   mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.Main"
echo.
pause

