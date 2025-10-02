@echo off
echo 正在安装Vue.js依赖包...
npm install

echo 正在构建Vue应用...
npm run build

echo 正在复制构建文件到Spring Boot静态目录...
xcopy /s /y dist\* ..\vue-app\

echo 构建完成！Vue应用已部署到 /vue 路径