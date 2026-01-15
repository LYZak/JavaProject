#!/bin/bash

echo "=========================================="
echo "BigComp 门禁控制系统 - 完整运行脚本"
echo "=========================================="
echo ""

echo "[1/3] 编译项目..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "编译失败！"
    exit 1
fi
echo "编译成功！"
echo ""

echo "[2/3] 运行测试脚本创建测试数据..."
mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.test.SystemTestRunner" -Dexec.args="" -q
if [ $? -ne 0 ]; then
    echo "测试数据创建失败！"
    exit 1
fi
echo "测试数据创建完成！"
echo ""

echo "[3/3] 启动GUI应用程序..."
echo "正在启动图形界面..."
mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.Main" -q

