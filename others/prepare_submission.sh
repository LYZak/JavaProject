#!/bin/bash

echo "========================================"
echo "准备项目提交文件"
echo "========================================"
echo ""

# 创建临时目录
rm -rf submission_temp
mkdir -p submission_temp

echo "[1/5] 编译项目..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "编译失败！"
    exit 1
fi

echo ""
echo "[2/5] 复制可执行jar文件..."
cp target/access-control-system-1.0.0.jar submission_temp/access-control-system-1.0.0.jar
if [ $? -ne 0 ]; then
    echo "复制jar文件失败！"
    exit 1
fi

echo ""
echo "[3/5] 创建sources.zip..."
zip -r submission_temp/sources.zip src/
if [ $? -ne 0 ]; then
    echo "创建sources.zip失败！"
    exit 1
fi

echo ""
echo "[4/5] 创建files.zip（包含data和images目录）..."
zip -r submission_temp/files.zip data/ images/
if [ $? -ne 0 ]; then
    echo "创建files.zip失败！"
    exit 1
fi

echo ""
echo "[5/5] 导出数据库为MySQL格式..."
mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.util.DatabaseExporter" -Dexec.classpathScope=compile
if [ $? -ne 0 ]; then
    echo "导出数据库失败！"
    exit 1
fi
if [ -f db.sql ]; then
    cp db.sql submission_temp/db.sql
fi

echo ""
echo "[完成] 复制README.txt..."
cp README.txt submission_temp/README.txt

echo ""
echo "========================================"
echo "提交文件准备完成！"
echo "========================================"
echo ""
echo "文件位置：submission_temp 目录"
echo ""
echo "请检查以下文件："
echo "  - README.txt"
echo "  - sources.zip"
echo "  - access-control-system-1.0.0.jar"
echo "  - files.zip"
echo "  - db.sql"
echo ""
echo "注意：您还需要准备："
echo "  - project-report.pdf（项目报告）"
echo "  - 视频链接（Youku）"
echo ""

