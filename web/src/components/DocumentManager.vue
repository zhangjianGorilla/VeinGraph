<template>
  <div class="document-manager">
    <!-- 顶部高亮上传按钮 -->
    <el-upload
      class="upload-area"
      action="/api/documents/upload-async"
      :show-file-list="false"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      :before-upload="beforeUpload"
    >
      <el-button class="upload-btn" type="primary">
        <el-icon><Plus /></el-icon> Upload Document
      </el-button>
    </el-upload>

    <!-- 列表标题与篇数统计 -->
    <div class="list-header">
      <span class="title">UPLOADED DOCUMENTS</span>
      <span class="count">{{ documents.length }}</span>
    </div>

    <!-- 文档列表 -->
    <div class="doc-list" v-loading="loading" element-loading-background="rgba(24, 28, 37, 0.8)">
      <el-empty v-if="documents.length === 0" description="No documents uploaded" :image-size="60" />
      
      <div v-for="doc in documents" :key="doc.id" class="doc-card">
        <div class="doc-icon">
          <el-icon><Document /></el-icon>
        </div>
        
        <div class="doc-content">
          <div class="doc-top">
            <span class="doc-name" :title="doc.fileName">{{ doc.fileName }}</span>
          </div>
          
          <div class="doc-status" :class="'status-' + getStatusColorClass(doc.status)">
            <span class="status-dot"></span>
            <span class="status-name">{{ getStatusText(doc.status) }}</span>
          </div>
          
          <div class="doc-bottom">
            <span class="doc-date">{{ formatDateForUI(doc.createdAt) }}</span>
            <span class="doc-size">{{ formatSize(doc.fileSize) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Document } from '@element-plus/icons-vue'
import axios from 'axios'

const documents = ref([])
const loading = ref(false)
let pollTimer = null

const fetchDocuments = async () => {
  try {
    loading.value = true
    const res = await axios.get('/api/documents?page=0&size=50')
    if (res.data.code === 200) {
      documents.value = res.data.data.content
    }
  } catch (error) {
    console.error('Failed to fetch documents', error)
  } finally {
    loading.value = false
  }
}

const startPolling = () => {
  pollTimer = setInterval(async () => {
    const hasProcessing = documents.value.some(doc => 
      ['PENDING', 'EXTRACTING'].includes(doc.status)
    )
    if (hasProcessing) {
      const res = await axios.get('/api/documents?page=0&size=50')
      if (res.data.code === 200) {
        documents.value = res.data.data.content
      }
    }
  }, 5000)
}

onMounted(() => {
  fetchDocuments()
  startPolling()
})
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

const beforeUpload = (file) => {
  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isLt20M) ElMessage.error('File must be smaller than 20MB!')
  return isLt20M
}

const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    ElMessage.success('Upload success, processing...')
    fetchDocuments()
  } else {
    ElMessage.error(res.message || 'Upload failed')
  }
}

const handleUploadError = () => ElMessage.error('Network error during upload')

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDateForUI = (dateString) => {
  if (!dateString) return ''
  const d = new Date(dateString)
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`
}

const getStatusColorClass = (status) => {
  switch (status) {
    case 'COMPLETED': return 'green'
    case 'PENDING': return 'orange'
    case 'EXTRACTING': return 'orange'
    case 'PARTIAL_FAILED': return 'red'
    default: return 'gray'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'PENDING': return 'Analyzing' 
    case 'EXTRACTING': return 'Processing'
    case 'COMPLETED': return 'Completed'
    case 'PARTIAL_FAILED': return 'Error'
    default: return status
  }
}
</script>

<style scoped>
.document-manager {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding-top: 10px;
}

/* 顶部抢眼的上传按钮 */
.upload-area {
  display: block;
  margin-bottom: 24px;
}
.upload-btn {
  width: 100%;
  height: 44px;
  background-color: var(--cyan-primary);
  border: none;
  border-radius: 8px;
  color: #000;
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: opacity 0.2s;
}
.upload-btn:hover {
  opacity: 0.9;
  background-color: var(--cyan-primary);
  color: #000;
}

/* 列表头部 */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 4px;
}
.list-header .title {
  font-size: 11px;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  font-weight: 600;
}
.list-header .count {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
}

/* 卡片列表 */
.doc-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
}
.doc-list::-webkit-scrollbar { width: 4px; }
.doc-list::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 2px; }

.doc-card {
  background-color: var(--bg-darkest);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 14px;
  display: flex;
  gap: 14px;
  transition: all 0.2s ease;
  align-items: flex-start;
}
.doc-card:hover {
  border-color: #334155;
  background-color: #1a1e28;
}

/* 左侧图标 */
.doc-icon {
  width: 36px;
  height: 36px;
  background-color: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 18px;
  border: 1px solid rgba(255,255,255,0.05);
}

/* 内容布局 */
.doc-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.doc-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.doc-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 10px;
}

/* 状态标示 */
.doc-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 500;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  box-shadow: 0 0 3px currentColor;
}
.status-green { color: #10B981; }
.status-green .status-dot { background-color: #10B981; }
.status-orange { color: #F59E0B; }
.status-orange .status-dot { background-color: #F59E0B; }
.status-red { color: #ef4444; }
.status-red .status-dot { background-color: #ef4444; }
.status-gray { color: #94a3b8; }
.status-gray .status-dot { background-color: #94a3b8; }

/* 底部元数据 */
.doc-bottom {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 4px;
}
</style>
