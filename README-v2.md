# 德州扑克游戏 - 前后端分离版本

## 项目架构

本项目采用前后端分离架构：

- **后端**: Spring Boot + WebSocket (端口: 8080)
- **前端**: Vue 3 + Vite (端口: 3000/3001)

## 快速启动

### 方式一：使用启动脚本 (推荐)

**Linux/macOS:**
```bash
./start-dev.sh
```

**Windows:**
```bash
start-dev.bat
```

### 方式二：手动启动

1. **启动后端服务器 (端口 8080)**
```bash
# 在项目根目录
java -jar target/pk-0.0.1-SNAPSHOT.jar
```

2. **启动前端开发服务器 (端口 3000)**
```bash
# 进入前端目录
cd src/main/resources/vue-frontend

# 安装依赖(首次运行)
npm install

# 启动开发服务器
npm run dev
```

## 访问地址

- **游戏界面**: http://localhost:3000 (或 http://localhost:3001)
- **后端API**: http://localhost:8080
- **后端健康检查**: http://localhost:8080/actuator/health

## 功能特性

### 🎮 游戏功能
- ✅ 6人桌德州扑克
- ✅ AI玩家自动决策
- ✅ 实时WebSocket通信
- ✅ 完整的游戏流程 (翻牌前、翻牌、转牌、河牌、摊牌)
- ✅ 玩家行动 (弃牌、看牌、跟注、加注、全下)

### 🔧 开发功能
- ✅ 前后端分离架构
- ✅ 热重载开发环境
- ✅ WebSocket代理配置
- ✅ 实时调试面板
- ✅ 详细的游戏状态日志

### 🐛 调试功能
- 点击页面右下角"调试"按钮查看:
  - 玩家ID匹配状态
  - 当前游戏阶段
  - 玩家轮次信息
  - WebSocket连接状态
  - 实时游戏数据

## 游戏流程

1. **创建游戏**: 点击"创建6人桌"
2. **加入游戏**: 输入玩家名称，点击"加入游戏"
3. **开始游戏**: 点击"开始自动游戏"
4. **游戏进行**: AI玩家自动行动，轮到真实玩家时操作按钮会启用
5. **查看调试**: 如有问题，点击"调试"按钮查看详细信息

## 技术栈

### 后端
- Spring Boot 2.7.0
- WebSocket
- H2 数据库
- Maven

### 前端  
- Vue 3
- Vite 4
- Pinia (状态管理)
- WebSocket Client

## 项目结构

```
gg/
├── src/main/java/               # 后端Java代码
│   └── com/gjm/pk/
├── src/main/resources/
│   ├── vue-frontend/            # 前端源码
│   │   ├── src/
│   │   ├── package.json
│   │   └── vite.config.js
│   └── vue-app/                 # 前端构建输出
├── target/                      # 后端构建输出
├── start-dev.sh                 # Linux/macOS启动脚本
├── start-dev.bat                # Windows启动脚本
└── pom.xml                      # Maven配置
```

## 开发说明

### 前端开发
- 前端代码位于 `src/main/resources/vue-frontend/`
- 修改前端代码后会自动热重载
- 通过Vite代理访问后端API和WebSocket

### 后端开发
- 后端修改需要重新编译JAR文件
- WebSocket端点: `/ws/game`
- API端点: `/api/game/*`

### 代理配置
前端开发服务器会自动代理以下请求到后端:
- `/api/*` → `http://localhost:8080/api/*`
- `/ws/*` → `ws://localhost:8080/ws/*`

## 故障排除

1. **按钮无法点击**: 
   - 打开调试面板检查"是否轮到我"状态
   - 确认WebSocket连接正常
   - 查看浏览器控制台错误

2. **玩家不显示**:
   - 检查游戏状态更新是否正常
   - 查看调试面板中的玩家列表
   - 确认后端日志无错误

3. **连接问题**:
   - 确保后端服务在8080端口运行
   - 检查防火墙设置
   - 查看浏览器Network面板

## 更新日志

### v2.0.0 - 前后端分离版本
- ✅ 重构为前后端分离架构
- ✅ 添加Vite开发服务器配置
- ✅ 优化WebSocket代理设置
- ✅ 增强调试功能和日志
- ✅ 修复玩家显示问题
- ✅ 添加启动脚本