<template>
  <div class="chat-panel">
    <!-- 顶部控制区：选择限定对话的文档 -->
    <div class="chat-header">
      <el-icon class="header-icon"><Filter /></el-icon>
      <el-select 
        v-model="selectedDocumentId" 
        placeholder="全局图谱漫游 (不限文档)" 
        clearable 
        class="doc-select" 
        size="small"
      >
        <el-option
          v-for="doc in documents"
          :key="doc.id"
          :label="doc.fileName"
          :value="doc.id"
        />
      </el-select>
    </div>

    <!-- 聊天内容区 -->
    <div class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-chat">
        <el-icon class="empty-icon"><ChatLineRound /></el-icon>
        <p>开启关于当前知识图谱的新对话</p>
      </div>

      <div 
        v-for="(msg, index) in messages" 
        :key="index"
        class="message-wrapper"
        :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
      >
        <div class="avatar">
          <el-icon v-if="msg.role === 'user'"><User /></el-icon>
          <el-icon v-else><Monitor /></el-icon>
        </div>
        <div class="message-bubble">
          <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
        </div>
      </div>
      
      <div v-if="isGenerating" class="message-wrapper is-assistant">
        <div class="avatar"><el-icon><Monitor /></el-icon></div>
        <div class="message-bubble">
          <div class="typing-indicator">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="chat-input-area">
      <el-input
        v-model="inputQuery"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        placeholder="向图谱提问... (Shift + Enter 换行)"
        resize="none"
        @keydown.enter.prevent="handleEnter"
        :disabled="isGenerating"
      />
      <el-button 
        type="primary" 
        class="send-btn" 
        :icon="Position" 
        circle 
        @click="sendMessage"
        :disabled="!inputQuery.trim() || isGenerating"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ChatLineRound, User, Monitor, Position, Filter } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (__) {}
    }
    return '' // use external default escaping
  }
})

const sessionId = ref('session_' + Date.now().toString(36))
const messages = ref([
  { role: 'assistant', content: '您好！我是集成 Neo4j + Elasticsearch 的 **GraphRAG Agent**。图谱构建完成后，您可以随时向我提问关于图谱的人脉关系、剧情脉络等问题。' }
])
const inputQuery = ref('')
const isGenerating = ref(false)
const messagesContainer = ref(null)
let eventSource = null

// 文档过滤相关
const documents = ref([])
const selectedDocumentId = ref('')

const fetchDocuments = async () => {
  try {
    const res = await axios.get('/api/documents?page=0&size=100')
    if (res.data.code === 200) {
      documents.value = res.data.data.content
    }
  } catch (error) {
    console.error('获取列表失败', error)
  }
}

onMounted(() => {
  fetchDocuments()
})

const renderMarkdown = (text) => {
  if (!text) return ''
  return md.render(text)
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const handleEnter = (e) => {
  if (e.shiftKey) {
    // allow newline
  } else {
    sendMessage()
  }
}

const sendMessage = () => {
  const q = inputQuery.value.trim()
  if (!q || isGenerating.value) return

  // 1. 添加用户消息
  messages.value.push({ role: 'user', content: q })
  inputQuery.value = ''
  scrollToBottom()

  // 2. 准备接收模型回复
  isGenerating.value = true
  const assistantMsgIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  
  // 3. 建立 SSE 连接
  let url = `/api/chat/stream?sessionId=${sessionId.value}&question=${encodeURIComponent(q)}`
  if (selectedDocumentId.value) {
    url += `&documentId=${selectedDocumentId.value}`
  }

  eventSource = new EventSource(url)

  eventSource.onmessage = (event) => {
    if (event.data) {
      const textChunk = event.data.replace(/\\n/g, '\n')
      messages.value[assistantMsgIndex].content += textChunk
      scrollToBottom()
    }
  }

  eventSource.onerror = (error) => {
    console.error("SSE Error:", error)
    eventSource.close()
    isGenerating.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 15px;
  background-color: #1a1a2e;
  border-bottom: 1px solid #2a2a4a;
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
}
.header-icon {
  color: #00adb5;
  font-size: 16px;
}
.doc-select {
  flex: 1;
}
:deep(.el-select__wrapper) {
  background-color: #16213e;
  box-shadow: none !important;
  border: 1px solid #2a2a4a;
}
:deep(.el-select__wrapper.is-hovering:not(.is-focused)) {
  box-shadow: none !important;
  border-color: #00adb5;
}
:deep(.el-select__wrapper.is-focused) {
  box-shadow: none !important;
  border-color: #00adb5;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 15px 10px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
/* 自定义滚动条 */
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: #2a2a4a; border-radius: 2px; }

.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 13px;
  opacity: 0.6;
}
.empty-icon { font-size: 48px; margin-bottom: 12px; }

.message-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.message-wrapper.is-user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: #fff; flex-shrink: 0;
}
.is-user .avatar { background: linear-gradient(135deg, #3b82f6, #60a5fa); }
.is-assistant .avatar { background: linear-gradient(135deg, #0f766e, #00adb5); }

.message-bubble {
  max-width: 85%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #e2e8f0;
  word-wrap: break-word;
}
.is-user .message-bubble {
  background-color: #3b82f6;
  border-top-right-radius: 2px;
}
.is-assistant .message-bubble {
  background-color: #1e293b;
  border: 1px solid #334155;
  border-top-left-radius: 2px;
}

.chat-input-area {
  margin-top: 15px;
  padding: 10px;
  background-color: #1e293b;
  border-radius: 12px;
  border: 1px solid #334155;
  display: flex;
  align-items: flex-end;
  gap: 10px;
}
.chat-input-area:focus-within {
  border-color: #00adb5;
  box-shadow: 0 0 0 1px #00adb5;
}

:deep(.el-textarea__inner) {
  background-color: transparent !important;
  border: none !important;
  box-shadow: none !important;
  color: #e2e8f0;
  padding: 0;
  font-size: 14px;
}
:deep(.el-textarea__inner)::placeholder { color: #64748b; }

.send-btn { margin-bottom: 2px; }

/* Typing animation */
.typing-indicator { display: flex; gap: 4px; padding: 4px; }
.dot {
  width: 6px; height: 6px; background-color: #00adb5; border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}
.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Markdown 样式 */
:deep(.message-content p) { margin: 0 0 10px 0; }
:deep(.message-content p:last-child) { margin-bottom: 0; }
:deep(.message-content pre) { background-color: #0f172a; padding: 12px; border-radius: 8px; overflow-x: auto; }
:deep(.message-content code) { background-color: rgba(255,255,255,0.1); padding: 2px 4px; border-radius: 4px; font-family: monospace; }
:deep(.message-content pre code) { background-color: transparent; padding: 0; }
:deep(.message-content a) { color: #38bdf8; text-decoration: none; }
:deep(.message-content a:hover) { text-decoration: underline; }
:deep(.message-content ul), :deep(.message-content ol) { margin-top: 5px; margin-bottom: 10px; padding-left: 20px; }
</style>
