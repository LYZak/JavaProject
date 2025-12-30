#!/bin/bash

echo "=========================================="
echo "BigComp 门禁控制系统 - 测试脚本"
echo "=========================================="
echo ""

echo "[1/2] 编译项目..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "编译失败！"
    exit 1
fi
echo "编译成功！"
echo ""

echo "[2/2] 运行测试脚本..."
mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.test.SystemTestRunner" -Dexec.args="" -q
if [ $? -ne 0 ]; then
    echo "测试运行失败！"
    exit 1
fi
echo ""

echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo ""
echo "现在可以运行主程序："
echo "  mvn exec:java -Dexec.mainClass=\"com.bigcomp.accesscontrol.Main\""
echo ""

