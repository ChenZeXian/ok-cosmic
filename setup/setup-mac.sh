#!/bin/bash
# ok-cosmic-knowledge 离线 API 知识图谱构建工具
# 用法: ./setup-mac.sh [参数]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$SCRIPT_DIR/setup.jar"

if ! command -v java &> /dev/null; then
    echo "错误: 未找到 java 命令，请安装 JDK 8+"
    exit 1
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "请确认发布包完整，或重新获取包含 setup.jar 的安装包"
    exit 1
fi

java -jar "$JAR_FILE" "$@"