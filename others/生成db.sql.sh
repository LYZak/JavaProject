#!/bin/bash

echo "========================================"
echo "生成MySQL数据库文件 (db.sql)"
echo "========================================"
echo ""

echo "[1/2] 编译项目..."
mvn clean compile -DskipTests
if [ $? -ne 0 ]; then
    echo "编译失败！"
    exit 1
fi

echo ""
echo "[2/2] 导出数据库为MySQL格式..."
mvn exec:java -Dexec.mainClass="com.bigcomp.accesscontrol.util.DatabaseExporter" -Dexec.classpathScope=compile
if [ $? -ne 0 ]; then
    echo "导出失败！"
    exit 1
fi

echo ""
if [ -f db.sql ]; then
    echo "========================================"
    echo "成功！db.sql 文件已生成在项目根目录"
    echo "========================================"
    echo ""
    echo "文件位置: $(pwd)/db.sql"
    echo ""
    ls -lh db.sql
else
    echo "========================================"
    echo "错误：db.sql 文件未生成"
    echo "========================================"
fi

