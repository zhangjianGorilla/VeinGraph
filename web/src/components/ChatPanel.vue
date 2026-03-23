<template>
  <div class="chat-panel">
    <!-- 顶部状态信息 -->
    <div class="chat-header">
      <div class="title">大模型智能体</div>
      <div class="subtitle">图谱多维检索引擎</div>
      <div class="status">
        <span class="status-dot"></span> 在线
      </div>
    </div>

    <!-- 聊天内容区 -->
    <div class="chat-messages" ref="messagesContainer">
      <div 
        v-for="(msg, index) in messages" 
        :key="index"
        class="message-wrapper"
        :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
      >
        <div class="role-name">{{ msg.role === 'user' ? '提问者' : 'GraphRAG 服务' }}</div>
        <div class="message-bubble">
          <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
        </div>
      </div>
      
      <div v-if="isGenerating" class="message-wrapper is-assistant">
        <div class="role-name">GraphRAG 服务正在思考</div>
        <div class="message-bubble">
          <div class="typing-indicator">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入区及状态回显 -->
    <div class="chat-input-container">
      <!-- 已选定的文档上下文胶囊标签 -->
      <transition name="el-fade-in">
        <div class="selected-context-pill" v-if="selectedDocumentId">
          <el-icon class="pill-icon"><Document /></el-icon>
          <span class="pill-text">{{ getSelectedDocumentName() }}</span>
          <el-icon class="pill-close" @click="clearSelectedDocument"><Close /></el-icon>
        </div>
      </transition>

      <div class="chat-input-area">
        <el-input
          v-model="inputQuery"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 5 }"
          placeholder="向图谱提问... (Shift + Enter 换行)"
          resize="none"
          @keydown.enter.prevent="handleEnter"
          :disabled="isGenerating"
          class="custom-textarea"
        />
        <div class="input-bottom-actions">
          <div class="left-tools">
             <!-- 回形针图标触发下拉选择器功能 -->
             <el-popover placement="top" :width="260" trigger="click" effect="dark">
               <template #reference>
                 <el-icon class="tool-icon"><Paperclip /></el-icon>
               </template>
               <div style="font-size: 13px; margin-bottom: 8px; color: #e2e8f0; font-weight: 500;">挂载文件作为上下文语境</div>
               <el-select 
                 v-model="selectedDocumentId" 
                 placeholder="全局知识视角 (未挂载文件)" 
                 clearable 
                 size="small"
                 style="width: 100%"
               >
                 <el-option
                   v-for="doc in documents"
                   :key="doc.id"
                   :label="doc.fileName"
                   :value="doc.id"
                 />
               </el-select>
             </el-popover>
          </div>
          <el-button 
            class="send-btn-icon" 
            type="primary"
            circle
            @click="sendMessage"
            :disabled="!inputQuery.trim() || isGenerating"
          >
            <el-icon><Position /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, inject } from 'vue'
import { Paperclip, Position, Document, Close } from '@element-plus/icons-vue'
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
    return ''
  }
})

const sessionId = ref('session_' + Date.now().toString(36))
const messages = ref([
  { role: 'user', content: '在图谱中，莫里斯和其它角色都有哪些具体交集？' },
  { role: 'assistant', content: '您好！我是被驱动在后台运行的 **VeinGraph RAG 引线级探脉代理**。图谱提取完成后，我不仅洞悉各实体的微观联系，也可以解答跨源文件的宏大叙事。\n\n请从左侧或此处挂载特定文档上下文，试着对我展开提问吧。' }
])
const inputQuery = ref('')
const isGenerating = ref(false)
const messagesContainer = ref(null)
let eventSource = null

// 文档下拉上下文
const documents = ref([])
const selectedDocumentId = inject('globalSelectedDocId')

const getSelectedDocumentName = () => {
  const doc = documents.value.find(d => d.id === selectedDocumentId.value)
  return doc ? doc.fileName : ''
}

const clearSelectedDocument = () => {
  selectedDocumentId.value = ''
}

const fetchDocuments = async () => {
  try {
    const res = await axios.get('/api/documents?page=0&size=100')
    if (res.data.code === 200) {
      documents.value = res.data.data
    }
  } catch (error) {
    console.error('获取挂载文档列表时发生网络异常', error)
  }
}

onMounted(() => {
  fetchDocuments()
  scrollToBottom()
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
  if (e.shiftKey) return // allow new line
  sendMessage()
}

const sendMessage = () => {
  const q = inputQuery.value.trim()
  if (!q || isGenerating.value) return

  messages.value.push({ role: 'user', content: q })
  inputQuery.value = ''
  scrollToBottom()

  isGenerating.value = true
  const assistantMsgIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  
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

/* 顶部 header 区域 */
.chat-header {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-panel);
}
.chat-header .title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  letter-spacing: 0.5px;
}
.chat-header .subtitle {
  font-size: 13px;
  color: var(--text-main);
}
.chat-header .status {
  font-size: 12px;
  color: #10B981;
  display: flex;
  align-items: center;
  gap: 6px;
}
.status-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background-color: #10B981;
}

/* 聊天滚屏区 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 2px; }

/* 消息包装块 */
.message-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.message-wrapper.is-user {
  align-items: flex-end;
}
.message-wrapper.is-assistant {
  align-items: flex-start;
}

.role-name {
  font-size: 13px;
  color: var(--text-main);
  padding: 0 2px;
}

.message-bubble {
  max-width: 85%;
  padding: 14px 18px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-main);
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

/* 用户气泡样式: 青色背景底，深色文字 (像 UI 图) */
.is-user .message-bubble {
  background-color: var(--cyan-primary);
  color: #000;
  border-top-right-radius: 2px;
}

/* 助手气泡样式: 次级深色底，白色文字 */
.is-assistant .message-bubble {
  background-color: var(--bg-darkest);
  border: 1px solid var(--border-color);
  border-top-left-radius: 2px;
}

/* 底部输入框容器 */
.chat-input-container {
  margin: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 优雅的文档选择回收胶囊 */
.selected-context-pill {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: var(--cyan-dim);
  border: 1px solid rgba(0, 229, 255, 0.3);
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: var(--cyan-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.pill-icon { font-size: 14px; }
.pill-text { 
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}
.pill-close {
  cursor: pointer;
  border-radius: 50%;
  padding: 2px;
  transition: all 0.2s;
}
.pill-close:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

/* 底部输入框外框 */
.chat-input-area {
  background-color: var(--bg-darkest);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: border-color 0.2s;
}
.chat-input-area:focus-within {
  border-color: var(--cyan-primary);
}

:deep(.el-textarea__inner) {
  background-color: transparent !important;
  border: none !important;
  box-shadow: none !important;
  color: var(--text-main);
  padding: 12px 16px;
  font-size: 14px;
}
:deep(.el-textarea__inner)::placeholder {
  color: var(--text-muted);
}

/* 输入框底部图标及按钮行 */
.input-bottom-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px 12px 16px;
}

.left-tools {
  display: flex;
  gap: 12px;
  color: var(--text-muted);
}
.tool-icon {
  font-size: 18px;
  cursor: pointer;
}
.tool-icon:hover {
  color: var(--cyan-primary);
}

/* 修改版青蓝色纯图标发送按钮 */
.send-btn-icon {
  background-color: var(--cyan-primary);
  border: none;
  color: #000;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.2s;
}
.send-btn-icon:hover {
  opacity: 0.9;
  background-color: var(--cyan-primary);
  color: #000;
  transform: scale(1.05);
}
.send-btn-icon.is-disabled {
  background-color: var(--border-color);
  color: var(--text-muted);
  transform: none;
}

/* Typing animation */
.typing-indicator { display: flex; gap: 4px; padding: 4px; }
.dot {
  width: 6px; height: 6px; background-color: var(--cyan-primary); border-radius: 50%;
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
:deep(.message-content pre) { background-color: rgba(255,255,255,0.05); padding: 12px; border-radius: 8px; overflow-x: auto; }
:deep(.message-content code) { background-color: rgba(255,255,255,0.1); padding: 2px 4px; border-radius: 4px; font-family: monospace; }
.is-user :deep(.message-content code) { background-color: rgba(0,0,0,0.1); color: #000; }
</style>
