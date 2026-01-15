@echo off
chcp 65001 >nul
echo ==========================================
echo BigComp 门禁控制系统 - 完整运行脚本
echo ==========================================
echo.

echo [1/3] 编译项目...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)
echo 编译成功！
echo.

echo [2/3] 运行测试脚本创建测试数据...
call mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.test.SystemTestRunner" -Dexec.args="" -q
if %errorlevel% neq 0 (
    echo 测试数据创建失败！
    pause
    exit /b 1
)
echo 测试数据创建完成！
echo.

echo [3/3] 启动GUI应用程序...
echo 正在启动图形界面...
call mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.Main" -q

