# 德州扑克游戏 - Vue.js 前端

这是一个基于Vue 3的德州扑克游戏前端，采用现代化的组件架构和状态管理。

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Vite** - 快速的构建工具
- **Pinia** - Vue的状态管理库
- **WebSocket** - 实时通信
- **Composition API** - Vue 3的响应式API

## 项目结构

```
src/
├── components/          # Vue组件
│   ├── GameHeader.vue   # 游戏头部
│   ├── GameTable.vue    # 游戏桌面
│   ├── CommunityCards.vue # 公共牌
│   ├── PlayerCard.vue   # 玩家卡片
│   ├── PlayerHand.vue   # 玩家手牌
│   ├── ActionButtons.vue # 操作按钮
│   ├── GameControls.vue # 游戏控制面板
│   ├── MessageToast.vue # 消息提示
│   └── GameLog.vue      # 游戏日志
├── stores/              # 状态管理
│   ├── game.js          # 游戏状态
│   └── ui.js            # UI状态
├── assets/              # 静态资源
│   └── styles.css       # 全局样式
├── App.vue              # 根组件
└── main.js              # 入口文件
```

## 开发环境运行

1. 安装依赖：
```bash
npm install
```

2. 启动开发服务器：
```bash
npm run dev
```

3. 在浏览器中访问：`http://localhost:5173`

## 构建生产版本

1. 构建项目：
```bash
npm run build
```

2. 构建文件将输出到 `../static-vue/` 目录

3. 或使用快速构建脚本：
```bash
./build.bat
```

## 主要功能

- **实时游戏** - 基于WebSocket的实时多人游戏
- **6人桌支持** - 支持最多6名玩家同时游戏
- **AI玩家** - 智能AI玩家自动参与游戏
- **自动游戏模式** - 一键创建6人AI桌
- **响应式设计** - 适配桌面和移动设备
- **状态管理** - 使用Pinia进行状态管理
- **组件化架构** - 模块化的Vue组件设计

## 与后端集成

前端通过以下方式与Spring Boot后端集成：

1. **WebSocket连接**：`ws://localhost:8080/ws/game`
2. **REST API**：`/api/game/*`
3. **静态资源**：构建后的文件部署到Spring Boot的static目录

## 开发说明

### 状态管理

使用Pinia进行状态管理，主要包括：

- `useGameStore()` - 游戏相关状态（玩家、牌局、连接状态等）
- `useUIStore()` - UI相关状态（消息提示、模态框等）

### 组件通信

- 父子组件通过props和events通信
- 全局状态通过Pinia stores共享
- WebSocket消息通过store统一处理

### 样式设计

- 使用CSS变量和现代CSS特性
- 支持响应式布局
- 包含动画和过渡效果
- 扑克桌圆形布局设计

## 部署说明

Vue前端可以独立运行，也可以集成到Spring Boot项目中：

1. **独立部署**：将构建文件部署到任何静态文件服务器
2. **集成部署**：将构建文件复制到Spring Boot的static目录

构建时会自动配置代理，确保与后端API的正确通信。