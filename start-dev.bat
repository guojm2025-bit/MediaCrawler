@echo off
echo === 德州扑克游戏启动脚本 ===

REM 检查是否在项目根目录
if not exist "pom.xml" (
    echo 错误: 请在项目根目录运行此脚本
    pause
    exit /b 1
)

REM 启动后端服务器 (端口 8080)
echo 正在启动后端服务器...
if exist "target\pk-0.0.1-SNAPSHOT.jar" (
    echo 使用已编译的JAR文件启动后端...
    start "后端服务器" java -jar target\pk-0.0.1-SNAPSHOT.jar
    echo 后端服务器启动中...
) else (
    echo 未找到编译好的JAR文件，请先编译项目
    pause
    exit /b 1
)

REM 等待后端启动
echo 等待后端服务启动...
timeout /t 8 /nobreak >nul

REM 启动前端开发服务器 (端口 3000)
echo 正在启动前端开发服务器...
cd src\main\resources\vue-frontend

if not exist "node_modules" (
    echo 正在安装前端依赖...
    call npm install
)

echo 启动前端开发服务器...
start "前端开发服务器" npm run dev

cd ..\..\..\..\

echo.
echo === 启动完成 ===
echo 后端服务: http://localhost:8080
echo 前端服务: http://localhost:3000  
echo 游戏界面: http://localhost:3000
echo.
echo 按任意键退出...
pause >nul