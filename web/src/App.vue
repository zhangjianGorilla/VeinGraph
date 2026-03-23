<template>
  <el-config-provider :zIndex="3000">
    <div class="veingraph-dashboard dark">
      <!-- 顶栏 -->
      <header class="top-navbar">
        <div class="logo">
          <el-icon class="logo-icon"><Connection /></el-icon>
          <span>KNOWLEDGE AI</span>
        </div>
        <div class="nav-actions">
          <el-button circle class="icon-btn"><el-icon><Bell /></el-icon></el-button>
          <el-button circle class="icon-btn"><el-icon><User /></el-icon></el-button>
        </div>
      </header>

      <div class="main-container">
        <!-- 左侧面板：文档管理 -->
        <div class="panel left-panel" :class="{ 'collapsed': !leftExpanded }">
          <!-- 这里去掉了明显的 Header，而是由控制伸缩的小开关和内部组件接管 -->
          <div class="collapse-trigger" @click="leftExpanded = !leftExpanded">
            <el-icon><ArrowLeft v-if="leftExpanded"/><ArrowRight v-else/></el-icon>
          </div>
          <div class="panel-content" v-show="leftExpanded">
            <DocumentManager />
          </div>
        </div>

        <!-- 中间面板：图谱可视化 -->
        <div class="panel center-panel">
          <div class="panel-header">
            <h2>Knowledge Graph Visualization</h2>
            <div class="header-tools">
              <el-button class="tool-btn"><el-icon><Plus /></el-icon></el-button>
              <el-button class="tool-btn"><el-icon><Minus /></el-icon></el-button>
              <el-button class="tool-btn"><el-icon><Search /></el-icon></el-button>
            </div>
          </div>
          <div class="graph-area">
             <!-- 预留图谱组件位置 -->
             <div class="placeholder-comp">
               <div class="mock-graph-circle"></div>
               <p style="color: #64748b; margin-top: 20px;">Graph rendering engine mounting...</p>
             </div>
          </div>
        </div>

        <!-- 右侧面板：GraphRAG 智能对话 -->
        <div class="panel right-panel" :class="{ 'collapsed': !rightExpanded }">
          <div class="collapse-trigger left-side" @click="rightExpanded = !rightExpanded">
            <el-icon><ArrowRight v-if="rightExpanded"/><ArrowLeft v-else/></el-icon>
          </div>
          <div class="panel-content" v-show="rightExpanded">
            <ChatPanel />
          </div>
        </div>
      </div>
    </div>
  </el-config-provider>
</template>

<script setup>
import { ref } from 'vue'
import { Connection, Bell, User, ArrowLeft, ArrowRight, Plus, Minus, Search } from '@element-plus/icons-vue'
import DocumentManager from './components/DocumentManager.vue'
import ChatPanel from './components/ChatPanel.vue'

const leftExpanded = ref(true)
const rightExpanded = ref(true)
</script>

<style>
/* 基础全局颜色变量 (严格还原设计图) */
:root {
  --bg-darkest: #11141a;       /* 最深色底层背景 */
  --bg-panel: #181c25;         /* 面板背景色 */
  --bg-panel-hover: #1f2430;
  --border-color: #242a38;     /* 面板边框 */
  --cyan-primary: #00e5ff;     /* 标志性青色 */
  --cyan-dim: rgba(0, 229, 255, 0.15);
  --text-main: #e2e8f0;        /* 主文字 */
  --text-muted: #64748b;       /* 次要文字 */
}

html, body, #app {
  margin: 0; padding: 0;
  height: 100vh; width: 100vw;
  overflow: hidden;
  background-color: var(--bg-darkest);
  color: var(--text-main);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  letter-spacing: 0.3px;
}

/* 深色模式基础 */
.dark { color-scheme: dark; }

.veingraph-dashboard {
  height: 100%;
  padding: 12px 16px 16px 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 顶部导航分析 */
.top-navbar {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--text-main);
}

.logo-icon {
  color: var(--cyan-primary);
  font-size: 18px;
}

.nav-actions {
  display: flex;
  gap: 12px;
}

.icon-btn.el-button {
  background-color: var(--bg-panel);
  border: 1px solid var(--border-color);
  color: var(--text-main);
}
.icon-btn.el-button:hover {
  background-color: var(--bg-panel-hover);
  color: var(--cyan-primary);
  border-color: var(--cyan-primary);
}

/* 主容器：三列岛屿布局 */
.main-container {
  display: flex;
  flex: 1;
  gap: 16px;
  overflow: hidden;
}

.panel {
  background-color: var(--bg-panel);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  position: relative;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 侧边栏伸缩触控区 */
.collapse-trigger {
  position: absolute;
  top: 50%;
  right: -10px;
  width: 20px;
  height: 40px;
  background-color: var(--bg-panel);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  color: var(--text-muted);
  transform: translateY(-50%);
}
.collapse-trigger.left-side {
  right: auto;
  left: -10px;
}
.collapse-trigger:hover {
  color: var(--cyan-primary);
  border-color: var(--cyan-primary);
}

.left-panel { width: 280px; min-width: 280px; }
.left-panel.collapsed { width: 0; min-width: 0; border: none; margin-right: -16px; }

.right-panel { width: 340px; min-width: 340px; }
.right-panel.collapsed { width: 0; min-width: 0; border: none; margin-left: -16px; }

.panel-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 中间区域 */
.center-panel {
  flex: 1;
  min-width: 400px;
}

.panel-header {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255,255,255,0.03);
}

.panel-header h2 {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-main);
  margin: 0;
}

.header-tools {
  display: flex;
  gap: 8px;
}

.tool-btn.el-button {
  background-color: var(--bg-darkest);
  border: 1px solid var(--border-color);
  color: var(--text-muted);
  padding: 8px;
  height: auto;
  border-radius: 6px;
}
.tool-btn.el-button:hover {
  color: var(--cyan-primary);
  border-color: var(--cyan-primary);
}

.graph-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: radial-gradient(circle at center, #1a2030 0%, var(--bg-panel) 70%);
}

.placeholder-comp {
  width: 100%; height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.mock-graph-circle {
  width: 300px; height: 300px;
  border-radius: 50%;
  border: 1px dashed var(--cyan-dim);
  box-shadow: 0 0 60px rgba(0, 229, 255, 0.05);
  animation: spin 30s linear infinite;
}
@keyframes spin { 100% { transform: rotate(360deg); } }
</style>
