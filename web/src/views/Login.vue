<template>
  <div class="login-wrapper">
    <div class="login-container">
      
      <div class="glass-panel">
        
        <div class="brand-header">
          <h1 class="brand-title">WELCOME BACK<br>TO <span class="highlight">CYBERNET</span></h1>
          <p class="brand-subtitle">{{ isRegister ? 'Register to create your account.' : 'Login to access your dashboard.' }}</p>
        </div>

        <el-form class="auth-form" @submit.prevent="handleLogin">
          
          <div v-show="isRegister" class="input-group">
            <label>Nickname</label>
            <div class="modern-input">
              <el-icon class="icon"><User /></el-icon>
              <input v-model="form.nickname" type="text" placeholder="how should we call you" />
            </div>
          </div>

          <div class="input-group">
            <label>Username</label>
            <div class="modern-input">
              <el-icon class="icon"><User /></el-icon>
              <input v-model="form.username" type="text" placeholder="username or email" />
            </div>
          </div>

          <div class="input-group">
            <label>Password</label>
            <div class="modern-input">
              <el-icon class="icon"><Lock /></el-icon>
              <input v-model="form.password" :type="pwdVisible ? 'text' : 'password'" placeholder="********" />
              <el-icon class="icon pointer" @click="pwdVisible = !pwdVisible">
                <View v-if="pwdVisible"/><Hide v-else/>
              </el-icon>
            </div>
          </div>

          <button class="primary-btn" type="submit" :disabled="loading">
            <span class="btn-text">{{ loading ? 'PROCESSING...' : (isRegister ? 'REGISTER' : 'LOGIN') }}</span>
          </button>

          <div class="toggle-mode" @click="isRegister = !isRegister">
            {{ isRegister ? 'Already have an account? Login' : 'Need an account? Register' }}
          </div>

          <div class="split-line">
            <span>or continue with</span>
          </div>

          <div class="social-actions">
            <button class="social-btn github" type="button" @click.prevent="handleOAuth('github')">
              <svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
              <span>Log in with GitHub</span>
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

const OAUTH2_CLIENT_ID = 'veingraph-web'
const OAUTH2_CLIENT_SECRET = 'veingraph-web-secret'

const form = reactive({ username: '', password: '', nickname: '' })
const pwdVisible = ref(false)
const isRegister = ref(false)
const loading = ref(false)

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('Please enter complete credentials')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      const res = await axios.post('/api/auth/register', {
        username: form.username,
        password: form.password,
        nickname: form.nickname || form.username
      })
      if (res.data.code === 200) {
        ElMessage.success('Registration successful. Logging in...')
        await doOAuth2Login()
      } else {
        ElMessage.error(res.data.message || 'Registration failed')
      }
    } else {
      await doOAuth2Login()
    }
  } catch (error) {
    const msg = error.response?.data?.error_description
      || error.response?.data?.message
      || 'Server connection anomaly'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

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
    ElMessage.success('Access granted')
    router.push('/')
  } else {
    ElMessage.error(res.data.error_description || 'Authentication failed')
  }
}

const handleOAuth = (provider) => {
  window.location.href = `/api/oauth2/authorization/${provider}`
}
</script>

<style scoped>
.login-wrapper {
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
  background-color: #05080f;
  background-image: url('../assets/login-background.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  font-family: 'Inter', -apple-system, sans-serif;
}

.login-container {
  width: 46vw;
  min-width: 420px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-right: 6vw;
}

.glass-panel {
  width: 410px;
  background: rgba(14, 21, 35, 0.45);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 229, 255, 0.25);
  border-radius: 12px;
  padding: 42px 38px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.6), inset 0 0 20px rgba(0, 229, 255, 0.05);
}

.brand-header {
  margin-bottom: 30px;
}

.brand-title {
  font-size: 24px;
  color: #ffffff;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin: 0 0 8px 0;
  line-height: 1.25;
}

.brand-title .highlight {
  font-size: 28px;
  color: #00e5ff;
  text-shadow: 0 0 15px rgba(0, 229, 255, 0.4);
}

.brand-subtitle {
  font-size: 13px;
  color: #9cb1c4;
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-group label {
  font-size: 13px;
  color: #cfd8dc;
  font-weight: 500;
  letter-spacing: 0.2px;
}

.modern-input {
  display: flex;
  align-items: center;
  height: 48px;
  background: rgba(6, 9, 15, 0.65);
  border: 1px solid rgba(0, 229, 255, 0.35);
  border-radius: 8px;
  padding: 0 14px;
  transition: all 0.3s ease;
}

.modern-input:focus-within {
  border-color: rgba(0, 229, 255, 0.8);
  background: rgba(6, 9, 15, 0.85);
  box-shadow: 0 0 12px rgba(0, 229, 255, 0.2);
}

.modern-input .icon {
  font-size: 18px;
  color: #00e5ff;
  opacity: 0.7;
}

.modern-input:focus-within .icon {
  opacity: 1;
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
  color: #5b6f83;
}

.pointer {
  cursor: pointer;
  transition: opacity 0.2s;
}

.pointer:hover {
  opacity: 1;
}

.primary-btn {
  height: 52px;
  margin-top: 12px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(90deg, #00d2ff 0%, #b520ff 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(181, 32, 255, 0.3);
  transition: transform 0.2s, box-shadow 0.2s;
}

.primary-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(181, 32, 255, 0.45);
}

.primary-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.toggle-mode {
  text-align: center;
  font-size: 12px;
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
  color: #6c7e90;
  font-size: 12px;
  margin: 4px 0;
}

.split-line::before, .split-line::after {
  content: '';
  flex: 1;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.split-line span {
  padding: 0 14px;
}

.social-actions {
  display: flex;
}

.social-btn {
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.github {
  background: rgba(10, 15, 25, 0.5);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.github:hover {
  background: rgba(20, 25, 35, 0.8);
  border-color: rgba(255, 255, 255, 0.3);
}

.social-btn svg {
  color: #ffffff;
}
</style>
