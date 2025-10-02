#!/bin/bash

# 德州扑克游戏 - 前后端分离启动脚本

echo "=== 德州扑克游戏启动脚本 ==="

# 检查是否在项目根目录
if [ ! -f "pom.xml" ]; then
    echo "错误: 请在项目根目录运行此脚本"
    exit 1
fi

# 启动后端服务器 (端口 8080)
echo "正在启动后端服务器..."
if [ -f "target/pk-0.0.1-SNAPSHOT.jar" ]; then
    echo "使用已编译的JAR文件启动后端..."
    java -jar target/pk-0.0.1-SNAPSHOT.jar &
    BACKEND_PID=$!
    echo "后端服务器启动中，PID: $BACKEND_PID"
else
    echo "未找到编译好的JAR文件，请先编译项目"
    exit 1
fi

# 等待后端启动
echo "等待后端服务启动..."
sleep 5

# 检查后端是否启动成功
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✓ 后端服务启动成功 (http://localhost:8080)"
else
    echo "✗ 后端服务启动失败"
    kill $BACKEND_PID 2>/dev/null
    exit 1
fi

# 启动前端开发服务器 (端口 3000)
echo "正在启动前端开发服务器..."
cd src/main/resources/vue-frontend

if [ ! -d "node_modules" ]; then
    echo "正在安装前端依赖..."
    npm install
fi

echo "启动前端开发服务器..."
npm run dev &
FRONTEND_PID=$!

cd ../../../../

echo ""
echo "=== 启动完成 ==="
echo "后端服务: http://localhost:8080"
echo "前端服务: http://localhost:3000"
echo "游戏界面: http://localhost:3000"
echo ""
echo "按 Ctrl+C 停止所有服务"

# 等待用户中断
trap "echo '正在停止服务...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT

# 保持脚本运行
wait