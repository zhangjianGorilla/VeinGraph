<template>
  <el-config-provider :zIndex="3000">
    <div class="veingraph-dashboard dark">
      <!-- 顶栏 (可选，目前保持极简只留主体区域) -->
      
      <div class="main-container">
        <!-- 左侧面板：文档管理 -->
        <div class="sidebar left-sidebar" :class="{ 'collapsed': !leftExpanded }">
          <div class="sidebar-header">
            <span v-if="leftExpanded">文档集</span>
            <el-button 
              circle 
              size="small" 
              @click="leftExpanded = !leftExpanded"
              :icon="leftExpanded ? 'ArrowLeft' : 'ArrowRight'"
            ></el-button>
          </div>
          
          <div class="sidebar-content" v-show="leftExpanded">
            <!-- 上传管理组件 -->
            <DocumentManager />
          </div>
        </div>

        <!-- 中间面板：图谱可视化 -->
        <div class="center-content">
          <div class="header-bar">
            <h2>VeinGraph 关系图谱</h2>
          </div>
          <div class="graph-area">
             <!-- 预留 vis-network 图谱组件位置 -->
             <div class="placeholder-comp">知识图谱加载中...</div>
          </div>
        </div>

        <!-- 右侧面板：GraphRAG 智能对话 -->
        <div class="sidebar right-sidebar" :class="{ 'collapsed': !rightExpanded }">
          <div class="sidebar-header" style="justify-content: space-between;">
            <el-button 
              circle 
              size="small" 
              @click="rightExpanded = !rightExpanded"
              :icon="rightExpanded ? 'ArrowRight' : 'ArrowLeft'"
            ></el-button>
            <span v-if="rightExpanded">GraphRAG 助手</span>
          </div>
          
          <div class="sidebar-content" v-show="rightExpanded">
            <ChatPanel />
          </div>
        </div>
      </div>
    </div>
  </el-config-provider>
</template>

<script setup>
import { ref } from 'vue'
import DocumentManager from './components/DocumentManager.vue'
import ChatPanel from './components/ChatPanel.vue'

const leftExpanded = ref(true)
const rightExpanded = ref(true)
</script>

<style>
/* 基础全屏样式重置 */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: #1a1a2e;
  color: #e2e8f0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
  'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

/* 强制开启深色模式 */
.dark {
  color-scheme: dark;
}

.veingraph-dashboard {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.main-container {
  display: flex;
  flex: 1;
  height: 100%;
  overflow: hidden;
}

/* 侧边栏基础样式 */
.sidebar {
  background-color: #16213e;
  border-right: 1px solid #0f3460;
  transition: width 0.3s ease-in-out;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.left-sidebar {
  width: 20%;
  min-width: 250px;
}
.left-sidebar.collapsed {
  width: 60px;
  min-width: 60px;
}

.right-sidebar {
  width: 25%;
  min-width: 300px;
  border-left: 1px solid #0f3460;
  border-right: none;
}
.right-sidebar.collapsed {
  width: 60px;
  min-width: 60px;
}

.sidebar-header {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  box-sizing: border-box;
  background-color: #1a1a2e;
  border-bottom: 1px solid #0f3460;
  font-weight: bold;
}

.sidebar-content {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

/* 中间主内容区 */
.center-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #1a1a2e;
  position: relative;
}

.header-bar {
  height: 50px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background-color: #16213e;
  border-bottom: 1px solid #0f3460;
}

.header-bar h2 {
  margin: 0;
  font-size: 1.2rem;
  color: #00adb5;
  text-shadow: 0 0 10px rgba(0,173,181,0.5);
}

.graph-area {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.placeholder-comp {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #64748b;
  font-size: 0.9rem;
}
</style>
