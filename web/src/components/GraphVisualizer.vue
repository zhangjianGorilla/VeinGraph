<template>
  <div class="graph-visualizer">
    <!-- 图谱控制头 -->
    <div class="panel-header">
      <div class="header-left">
        <h2>知识图谱可视化视图</h2>
      </div>

      <!-- 右侧控制区: 图谱操作 -->
      <div class="header-tools">
        <el-button class="tool-btn" @click="zoomIn" title="放大"><el-icon><Plus /></el-icon></el-button>
        <el-button class="tool-btn" @click="zoomOut" title="缩小"><el-icon><Minus /></el-icon></el-button>
        <el-button class="tool-btn" @click="fitGraph" title="适应屏幕"><el-icon><FullScreen /></el-icon></el-button>
      </div>
    </div>

    <!-- 绘制区域 -->
    <div class="graph-area" v-loading="loading" element-loading-background="rgba(26, 32, 48, 0.8)">
      <div ref="networkContainer" class="network-container"></div>
      
      <!-- 无数据占位 -->
      <div v-if="!loading && isEmpty" class="empty-overlay">
        <div class="mock-graph-circle"></div>
        <p>请首先在左侧面板点选指定文档，以汇聚展现其微观图谱拓扑结构。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, shallowRef, nextTick, inject, watch } from 'vue'
import { Plus, Minus, FullScreen } from '@element-plus/icons-vue'
import axios from 'axios'
import request from '../utils/request'
import { Network } from 'vis-network'
import 'vis-network/styles/vis-network.css'

const networkContainer = ref(null)
const networkInstance = shallowRef(null)

const loading = ref(false)
const isEmpty = ref(true)

const documents = ref([])

// 注入来自全局的选中文档状态
const selectedDocumentId = inject('globalSelectedDocId')

// 监听外界状态变化，自动重载图谱
watch(selectedDocumentId, () => {
  fetchGraphData()
})

// Graph configuration aligned with UI design pattern
const graphOptions = {
  nodes: {
    shape: 'dot',
    size: 16,
    font: {
      color: '#e2e8f0',
      size: 12,
      face: 'Inter, sans-serif'
    },
    borderWidth: 2,
    color: {
      background: '#181c25', // 使用纯色以遮挡下方连线
      border: '#00e5ff',
      highlight: { background: '#00e5ff', border: '#fff' }
    },
    shadow: { enabled: true, color: 'rgba(0, 229, 255, 0.4)', size: 15 }
  },
  edges: {
    width: 1.5,
    color: {
      color: 'rgba(148, 163, 184, 0.3)',
      highlight: '#00e5ff',
      hover: '#0f766e'
    },
    font: {
      color: '#94a3b8',
      size: 10,
      background: 'rgba(17, 20, 26, 0.8)',
      strokeWidth: 0
    },
    smooth: {
      type: 'continuous',
      roundness: 0.5
    },
    arrows: { to: { enabled: true, scaleFactor: 0.5 } }
  },
  physics: {
    forceAtlas2Based: {
      gravitationalConstant: -100,
      centralGravity: 0.005,
      springLength: 200,
      springConstant: 0.05
    },
    maxVelocity: 50,
    solver: 'forceAtlas2Based',
    timestep: 0.35,
    stabilization: { iterations: 150 }
  },
  interaction: {
    hover: true,
    tooltipDelay: 200,
    zoomView: true,
    dragView: true
  }
}

const fetchDocuments = async () => {
  try {
    const res = await request.get('/documents?page=0&size=100')
    if (res.data.code === 200) {
      documents.value = res.data.data
    }
  } catch (error) {
    console.error('获取全部文档失败', error)
  }
}

const fetchGraphData = async () => {
  if (!selectedDocumentId.value) {
    // 拦截全局展现的请求，符合“取消选择就不展示图谱”要求
    isEmpty.value = true
    if (networkInstance.value) {
      networkInstance.value.destroy()
      networkInstance.value = null
    }
    return
  }

  loading.value = true
  try {
    const res = await request.get(`/graph?documentId=${selectedDocumentId.value}`)
    if (res.data.code === 200) {
      const data = res.data.data
      renderGraph(data.nodes, data.edges)
    }
  } catch (error) {
    console.error('获取指定图谱数据失败', error)
  } finally {
    loading.value = false
  }
}

const renderGraph = (nodes, edges) => {
  if (!nodes || nodes.length === 0) {
    isEmpty.value = true
    if (networkInstance.value) {
      networkInstance.value.destroy()
      networkInstance.value = null
    }
    return
  }
  isEmpty.value = false
  
  const data = { nodes, edges }
  
  if (networkInstance.value) {
    networkInstance.value.setData(data)
  } else {
    // 确保 DOM 准备好
    nextTick(() => {
      networkInstance.value = new Network(networkContainer.value, data, graphOptions)
      
      // 添加交互事件
      networkInstance.value.on('click', (params) => {
        if (params.nodes.length > 0) {
          console.log('Clicked node:', params.nodes[0])
          // 预留以后点击图谱实体，到右边面板发起问答的联动
        }
      })
    })
  }
}

const zoomIn = () => {
  if (networkInstance.value) {
    const scale = networkInstance.value.getScale()
    networkInstance.value.moveTo({ scale: scale * 1.5, animation: true })
  }
}

const zoomOut = () => {
  if (networkInstance.value) {
    const scale = networkInstance.value.getScale()
    networkInstance.value.moveTo({ scale: scale / 1.5, animation: true })
  }
}

const fitGraph = () => {
  if (networkInstance.value) {
    networkInstance.value.fit({ animation: true })
  }
}

onMounted(() => {
  fetchDocuments()
  fetchGraphData()
})
</script>

<style scoped>
.graph-visualizer {
  display: flex;
  flex-direction: column;
  height: 100%;
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
  align-items: center;
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

.network-container {
  width: 100%;
  height: 100%;
  outline: none;
}

.empty-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: rgba(24, 28, 37, 0.7);
  z-index: 10;
}
.mock-graph-circle {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  border: 1px dashed var(--cyan-dim);
  box-shadow: 0 0 40px rgba(0, 229, 255, 0.05);
  animation: spin 30s linear infinite;
  margin-bottom: 20px;
}
.empty-overlay p {
  color: var(--text-muted);
  font-size: 14px;
}
@keyframes spin { 100% { transform: rotate(360deg); } }
</style>
