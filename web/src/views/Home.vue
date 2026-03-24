<template>
  <div class="veingraph-dashboard dark">
    <!-- 顶栏 -->
    <header class="top-navbar">
      <div class="logo">
        <el-icon class="logo-icon"><Connection /></el-icon>
        <span>VeinGraph 核心中枢</span>
      </div>
      <div class="nav-actions">
        <el-button circle class="icon-btn"><el-icon><Bell /></el-icon></el-button>
        <el-button circle class="icon-btn" @click="handleLogout"><el-icon><SwitchButton /></el-icon></el-button>
      </div>
    </header>

    <div class="main-container">
      <!-- 左侧面板：文档管理 -->
      <div class="panel left-panel" :class="{ 'collapsed': !leftExpanded }">
        <div class="collapse-trigger" @click="leftExpanded = !leftExpanded">
          <el-icon><ArrowLeft v-if="leftExpanded"/><ArrowRight v-else/></el-icon>
        </div>
        <div class="panel-content" v-show="leftExpanded">
          <DocumentManager />
        </div>
      </div>

      <!-- 中间面板：图谱可视化 -->
      <div class="panel center-panel">
        <GraphVisualizer />
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
</template>

<script setup>
import { ref, provide } from 'vue'
import { useRouter } from 'vue-router'
import { Connection, Bell, SwitchButton, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import DocumentManager from '../components/DocumentManager.vue'
import ChatPanel from '../components/ChatPanel.vue'
import GraphVisualizer from '../components/GraphVisualizer.vue'

const router = useRouter()

// 核心跨组件状态共享：当前选中的文档ID
const globalSelectedDocId = ref('')
provide('globalSelectedDocId', globalSelectedDocId)

const leftExpanded = ref(true)
const rightExpanded = ref(true)

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.veingraph-dashboard {
  height: 100vh;
  padding: 12px 16px 16px 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: var(--bg-darkest);
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
</style>
