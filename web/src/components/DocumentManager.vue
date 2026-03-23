<template>
  <div class="document-manager">
    <!-- 上传区域 -->
    <el-upload
      class="upload-area"
      drag
      action="/api/documents/upload-async"
      :show-file-list="false"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      :before-upload="beforeUpload"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        拖拽文件或 <em>点击上传</em>
      </div>
    </el-upload>

    <!-- 刷新控制 -->
    <div class="list-header">
      <h3>文档库</h3>
      <el-button size="small" circle icon="Refresh" @click="fetchDocuments" :loading="loading" plain type="info" />
    </div>

    <!-- 文档列表 -->
    <div class="doc-list" v-loading="loading" element-loading-background="rgba(26, 26, 46, 0.8)">
      <el-empty v-if="documents.length === 0" description="暂无文档" :image-size="60" />
      
      <div v-for="doc in documents" :key="doc.id" class="doc-item">
        <div class="doc-icon">
          <el-icon><Document /></el-icon>
        </div>
        
        <div class="doc-info">
          <div class="doc-name" :title="doc.fileName">{{ doc.fileName }}</div>
          <div class="doc-meta">
            {{ formatSize(doc.fileSize) }} · {{ formatDate(doc.createdAt) }}
          </div>
        </div>
        
        <!-- 状态指示灯与文字 -->
        <div class="doc-status" :class="'status-' + getStatusColorClass(doc.status)">
          <span class="status-dot"></span>
          <span class="status-text">{{ getStatusText(doc.status) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, Refresh } from '@element-plus/icons-vue'
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
    console.error('获取列表失败', error)
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
  if (!isLt20M) ElMessage.error('文档不超过 20MB!')
  return isLt20M
}

const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    ElMessage.success('上传成功，处理中...')
    fetchDocuments()
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const handleUploadError = () => ElMessage.error('网络错误，上传失败')

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const d = new Date(dateString)
  return `${d.getMonth()+1}-${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

const getStatusColorClass = (status) => {
  switch (status) {
    case 'COMPLETED': return 'green'
    case 'PENDING': return 'gray'
    case 'EXTRACTING': return 'orange'
    case 'PARTIAL_FAILED': return 'red'
    default: return 'gray'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'PENDING': return '队列中'
    case 'EXTRACTING': return '处理中'
    case 'COMPLETED': return '就绪'
    case 'PARTIAL_FAILED': return '异常'
    default: return status
  }
}
</script>

<style scoped>
.document-manager { display: flex; flex-direction: column; height: 100%; }

.upload-area { margin-bottom: 25px; }
:deep(.el-upload-dragger) {
  background-color: #1a1a2e;
  border: 1px dashed #2a2a4a;
  border-radius: 12px;
  padding: 20px;
}
:deep(.el-upload-dragger:hover) {
  border-color: #00adb5;
  background-color: rgba(0, 173, 181, 0.05);
}
.el-upload__text { color: #94a3b8; font-size: 13px; margin-top: 10px; }
.el-upload__text em { color: #00adb5; font-style: normal; }

.list-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 15px; padding: 0 5px;
}
.list-header h3 { margin: 0; font-size: 14px; color: #cbd5e1; font-weight: 500; letter-spacing: 0.5px;}

.doc-list {
  flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 12px;
  padding-right: 6px;
}

.doc-list::-webkit-scrollbar { width: 4px; }
.doc-list::-webkit-scrollbar-thumb { background: #2a2a4a; border-radius: 2px; }

.doc-item {
  background-color: #16213e;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid #1f2937;
  transition: all 0.2s ease;
}
.doc-item:hover {
  border-color: #334155;
  background-color: #1a233a;
  transform: translateY(-1px);
}

.doc-icon {
  width: 32px; height: 32px;
  background-color: rgba(0, 173, 181, 0.1);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #00adb5; font-size: 18px;
  flex-shrink: 0;
}

.doc-info {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column; justify-content: center; gap: 4px;
}

.doc-name {
  font-size: 13px; font-weight: 500; color: #f1f5f9;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.doc-meta { font-size: 11px; color: #64748b; }

.doc-status {
  display: flex; align-items: center; gap: 6px;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 500;
}

.status-dot {
  width: 8px; height: 8px; border-radius: 50%;
  box-shadow: 0 0 4px currentColor;
}

.status-green { color: #22c55e; }
.status-green .status-dot { background-color: #22c55e; }
.status-orange { color: #f59e0b; }
.status-orange .status-dot { background-color: #f59e0b; animation: pulse 2s infinite; }
.status-red { color: #ef4444; }
.status-red .status-dot { background-color: #ef4444; }
.status-gray { color: #94a3b8; }
.status-gray .status-dot { background-color: #94a3b8; }

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.4); }
  70% { box-shadow: 0 0 0 4px rgba(245, 158, 11, 0); }
  100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
}
</style>
