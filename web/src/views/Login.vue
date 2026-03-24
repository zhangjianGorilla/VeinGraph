<template>
  <div class="login-wrapper">
    <div class="login-container">
      
      <!-- 极简半透明玻璃面板 -->
      <div class="glass-panel">
        
        <div class="brand-header">
          <h1 class="brand-title">VEIN<span class="highlight">GRAPH</span></h1>
          <p class="brand-subtitle">连接数据孤岛，探索无限脉络</p>
        </div>

        <el-form class="auth-form" @submit.prevent="handleLogin">
          
          <!-- 注册态专享：昵称 -->
          <div v-show="isRegister" class="input-group">
            <label>称呼 (Nickname)</label>
            <div class="modern-input">
              <el-icon class="icon"><User /></el-icon>
              <input v-model="form.nickname" type="text" placeholder="您希望我们如何称呼您" />
            </div>
          </div>

          <!-- 通用域：用户名 -->
          <div class="input-group">
            <label>身份标识 (Username)</label>
            <div class="modern-input">
              <el-icon class="icon"><User /></el-icon>
              <input v-model="form.username" type="text" placeholder="输入您的用户名或邮箱" />
            </div>
          </div>

          <!-- 通用域：密码 -->
          <div class="input-group">
            <label>密钥验证 (Password)</label>
            <div class="modern-input">
              <el-icon class="icon"><Lock /></el-icon>
              <input v-model="form.password" :type="pwdVisible ? 'text' : 'password'" placeholder="输入准入密码" />
              <el-icon class="icon pointer" @click="pwdVisible = !pwdVisible">
                <View v-if="pwdVisible"/><Hide v-else/>
              </el-icon>
            </div>
          </div>

          <!-- 主操作按钮 -->
          <button class="primary-btn" type="submit" :disabled="loading">
            <span class="btn-text">{{ loading ? '校验中...' : (isRegister ? '确 认 注 册' : '登 录 中 枢') }}</span>
          </button>

          <!-- 模式切换 -->
          <div class="toggle-mode" @click="isRegister = !isRegister">
            {{ isRegister ? '已有通行证？立即访问' : '需要新身份？快速注册' }}
          </div>

          <div class="split-line">
            <span>支持 OAuth2 社交授权</span>
          </div>

          <!-- 社交登录入口 -->
          <div class="social-actions">
            <button class="social-btn github" type="button" @click.prevent="handleOAuth('github')">
              <svg viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
              GitHub
            </button>
            <button class="social-btn gitee" type="button" @click.prevent="handleOAuth('gitee')">
              <svg viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M12 2C6.475 2 2 6.475 2 12s4.475 10 10 10 10-4.475 10-10S17.525 2 12 2zm4.768 14.429H10.18a1.071 1.071 0 01-1.071-1.072v-5.714a1.071 1.071 0 011.071-1.072h6.589A.536.536 0 0117.304 9a.536.536 0 01-.536.536H10.18v5.357h6.589a.536.536 0 01.536.536.536.536 0 01-.536.536h-.001v-.536z"/></svg>
              Gitee
            </button>
          </div>
        </el-form>
      </div>
      
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, View, Hide } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()

// OAuth2 客户端配置
const OAUTH2_CLIENT_ID = 'veingraph-web'
const OAUTH2_CLIENT_SECRET = 'veingraph-web-secret'

// 状态声明
const form = reactive({ username: '', password: '', nickname: '' })
const pwdVisible = ref(false)
const isRegister = ref(false)
const loading = ref(false)

// 鉴权函数 - 使用 OAuth2 Password Grant
const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入完整凭证数据')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      // 注册
      const res = await axios.post('/api/auth/register', {
        username: form.username,
        password: form.password,
        nickname: form.nickname || form.username
      })
      if (res.data.code === 200) {
        ElMessage.success('注册成功，正在登录...')
        // 注册成功后自动登录
        await doOAuth2Login()
      } else {
        ElMessage.error(res.data.message || '注册失败')
      }
    } else {
      // 登录走 OAuth2 Password Grant
      await doOAuth2Login()
    }
  } catch (error) {
    const msg = error.response?.data?.error_description
      || error.response?.data?.message
      || '服务器连接异常'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

// OAuth2 Password Grant 登录
const doOAuth2Login = async () => {
  const params = new URLSearchParams()
  params.append('grant_type', 'password')
  params.append('username', form.username)
  params.append('password', form.password)
  params.append('client_id', OAUTH2_CLIENT_ID)
  params.append('client_secret', OAUTH2_CLIENT_SECRET)

  const res = await axios.post('/api/oauth2/token', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })

  if (res.data.access_token) {
    localStorage.setItem('token', res.data.access_token)
    localStorage.setItem('user', JSON.stringify({
      userId: res.data.userId,
      nickname: res.data.nickname,
      token: res.data.access_token
    }))
    ElMessage.success('接入中枢成功')
    router.push('/')
  } else {
    ElMessage.error(res.data.error_description || '认证失败')
  }
}

// 社交授权回调
const handleOAuth = (provider) => {
  window.location.href = `/api/oauth2/authorization/${provider}`
}
</script>

<style scoped>
/* =================== 基础全屏背景 =================== */
.login-wrapper {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
  background-color: #05080f;
  /* 使用用户指定原图作为全屏背景，右侧通过深色羽化渐变遮挡原片中的“假表单” */
  background-image: linear-gradient(to right, rgba(5,8,15,0) 0%, rgba(5,8,15,0) 40%, rgba(5,8,15,0.9) 65%, rgba(5,8,15,1) 100%), url('../assets/login_page_ui.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  align-items: center;
  justify-content: flex-end; /* 面板永远靠右 */
  font-family: 'Inter', -apple-system, sans-serif;
}

/* 保护层，如果背景图太大保证靠右留白 */
.login-container {
  width: 46vw;
  min-width: 400px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-right: 6vw;
}

/* =================== 顶级玻璃拟态框 =================== */
.glass-panel {
  width: 400px;
  background: rgba(10, 15, 25, 0.45);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 45px 40px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255,255,255,0.1);
}

/* =================== 头部文案 =================== */
.brand-header {
  margin-bottom: 35px;
}
.brand-title {
  font-size: 28px;
  color: #fff;
  font-weight: 800;
  letter-spacing: 1px;
  margin: 0 0 6px 0;
}
.brand-title .highlight {
  color: #00e5ff; /* 赛博青色 */
}
.brand-subtitle {
  font-size: 13px;
  color: #8b9cae;
  margin: 0;
}

/* =================== 输入框架构 =================== */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}
.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.input-group label {
  font-size: 12px;
  color: #a1b0c0;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.modern-input {
  display: flex;
  align-items: center;
  height: 48px;
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 0 14px;
  transition: all 0.3s ease;
}
.modern-input:focus-within {
  border-color: rgba(0, 229, 255, 0.6);
  background: rgba(0, 229, 255, 0.02);
  box-shadow: 0 0 0 4px rgba(0, 229, 255, 0.1);
}
.modern-input .icon {
  font-size: 16px;
  color: #5d6f82;
}
.modern-input:focus-within .icon {
  color: #00e5ff;
}
.modern-input input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #fff;
  font-size: 14px;
  margin: 0 12px;
}
.modern-input input::placeholder {
  color: #4a5c6e;
}
.pointer { cursor: pointer; transition: color 0.2s; }
.pointer:hover { color: #fff; }

/* =================== 渐变主按钮 =================== */
.primary-btn {
  height: 50px;
  margin-top: 10px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #00d2ff 0%, #3a7bd5 100%);
  color: white;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 210, 255, 0.25);
  transition: transform 0.2s, box-shadow 0.2s;
}
.primary-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 25px rgba(0, 210, 255, 0.4);
}
.primary-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* =================== 辅助链接 =================== */
.toggle-mode {
  text-align: center;
  font-size: 13px;
  color: #00e5ff;
  cursor: pointer;
  transition: opacity 0.2s;
  margin-top: -6px;
}
.toggle-mode:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.split-line {
  display: flex;
  align-items: center;
  color: #5d6f82;
  font-size: 11px;
  margin: 6px 0;
}
.split-line::before, .split-line::after {
  content: '';
  flex: 1;
  border-top: 1px solid rgba(255,255,255,0.06);
}
.split-line span { padding: 0 12px; text-transform: uppercase; letter-spacing: 0.5px; }

/* =================== 第三方授权按钮 =================== */
.social-actions {
  display: flex;
  gap: 12px;
}
.social-btn {
  flex: 1;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.github {
  background: rgba(255,255,255,0.05);
  color: #fff;
  border: 1px solid rgba(255,255,255,0.1);
}
.github:hover {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.3);
}
.gitee {
  background: rgba(199, 29, 35, 0.05); /* Gitee 的淡红色 */
  color: #ff4d4f;
  border: 1px solid rgba(199, 29, 35, 0.2);
}
.gitee:hover {
  background: rgba(199, 29, 35, 0.15);
  border-color: rgba(199, 29, 35, 0.4);
}
</style>
