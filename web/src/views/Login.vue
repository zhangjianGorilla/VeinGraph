<template>
  <div class="login-screen">
    <!-- 左侧：纯起占位作用，吉祥物露出 -->
    <div class="left-mascot-zone"></div>
    
    <!-- 右侧：悬浮的玻璃质感认证面板 -->
    <div class="right-panel-zone">
      <div class="glass-board">
        
        <div class="header-titles">
          <h1 class="main-title">欢迎回到 <span class="highlight">VEINGRAPH</span></h1>
          <p class="sub-title">登录以访问您的核心控制台。</p>
        </div>

        <el-form label-position="top" class="cyber-form">
          <!-- 账号输入 -->
          <div class="form-group cyan-focus">
            <label>用户名</label>
            <div class="input-wrapper">
              <el-icon class="prefix-icon"><User /></el-icon>
              <input v-model="form.username" type="text" placeholder="用户名或邮箱" />
            </div>
          </div>

          <!-- 密码输入 -->
          <div class="form-group purple-focus">
            <label>密码</label>
            <div class="input-wrapper">
              <el-icon class="prefix-icon"><Lock /></el-icon>
              <input v-model="form.password" :type="pwdVisible ? 'text' : 'password'" placeholder="********" />
              <el-icon class="suffix-icon cursor-pointer" @click="pwdVisible = !pwdVisible">
                <View v-if="pwdVisible"/><Hide v-else/>
              </el-icon>
            </div>
          </div>

          <button class="btn-gradient-login" @click.prevent="handleLogin" :disabled="loading">
            {{ loading ? '处理中...' : (isRegister ? '注 册' : '登 录') }}
          </button>

          <div class="toggle-mode" @click="isRegister = !isRegister">
            {{ isRegister ? '已有账号？点击登录' : '没有账号？点击注册' }}
          </div>

          <div class="divider">
            <span class="line"></span>
            <span class="text">或者继续使用</span>
            <span class="line"></span>
          </div>

          <div class="social-buttons">
            <button class="btn-github" @click.prevent="handleOAuth('github')">
              <svg class="github-icon" viewBox="0 0 24 24" width="20" height="20">
                <path fill="currentColor" d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
              </svg>
              GitHub
            </button>
            <button class="btn-gitee" @click.prevent="handleOAuth('gitee')">
              <svg viewBox="0 0 24 24" width="20" height="20">
                <path fill="currentColor" d="M12 2C6.475 2 2 6.475 2 12s4.475 10 10 10 10-4.475 10-10S17.525 2 12 2zm4.768 14.429H10.18a1.071 1.071 0 01-1.071-1.072v-5.714a1.071 1.071 0 011.071-1.072h6.589A.536.536 0 0117.304 9a.536.536 0 01-.536.536H10.18v5.357h6.589a.536.536 0 01.536.536.536.536 0 01-.536.536h-.001v-.536z"/>
              </svg>
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
import request from '../utils/request'

const router = useRouter()
const form = reactive({ username: '', password: '', nickname: '' })
const pwdVisible = ref(false)
const isRegister = ref(false)
const loading = ref(false)

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const url = isRegister.value ? '/auth/register' : '/auth/login'
    const payload = isRegister.value
      ? { username: form.username, password: form.password, nickname: form.nickname || form.username }
      : { username: form.username, password: form.password }
    const res = await request.post(url, payload)
    if (res.data.code === 200) {
      localStorage.setItem('token', res.data.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.data))
      ElMessage.success(isRegister.value ? '注册成功' : '登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (error) {
    const msg = error.response?.data?.message || '网络异常'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const handleOAuth = (provider) => {
  // 跳转到后端 OAuth2 授权入口
  window.location.href = `/api/oauth2/authorization/${provider}`
}
</script>

<style scoped>
/* ===================== 全局与背景 ===================== */
.login-screen {
  width: 100vw;
  height: 100vh;
  display: flex;
  overflow: hidden;
  background-color: #0b1016; /* 深邃纯正的赛博空间黑 */
  font-family: 'Inter', -apple-system, sans-serif;
}

/* 左侧：吉祥物占位区，巧妙地裁掉原图右侧的英文表单 */
.left-mascot-zone { 
  flex: 1.2;
  position: relative;
  background-image: url('../assets/login_page_ui.png'); 
  background-size: cover;
  background-position: left center; /* 核心：对齐左边，隐藏右边超长的部分 */
  background-repeat: no-repeat;
}

/* 在裁切线的右侧加上平滑的黑色消影过渡，与右半部融为一体 */
.left-mascot-zone::after {
  content: '';
  position: absolute;
  top: 0; right: 0;
  width: 40%; 
  height: 100%;
  background: linear-gradient(to right, rgba(11,16,22,0) 0%, rgba(11,16,22,0.8) 50%, rgba(11,16,22,1) 100%);
  pointer-events: none;
}

/* 右侧：真实的 Vue 登录表单层 */
.right-panel-zone {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center; /* 居中表单 */
  background-color: #0b1016; /* 沉重的深黑底色衬托玻璃悬浮 */
  position: relative;
  z-index: 10;
  padding-right: 5vw;
}

/* ===================== 悬浮毛玻璃面板 ===================== */
.glass-board {
  width: 100%;
  max-width: 440px;
  background: rgba(16, 22, 35, 0.2); /* 更高的透明度，体现原图薄晶质感 */
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-radius: 12px;
  border: 1px solid rgba(0, 240, 255, 0.15);
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.8), inset 0 0 15px rgba(0, 240, 255, 0.05);
  padding: 50px 45px;
  position: relative;
}

/* 原图左上和右下有高亮异色角标 */
.glass-board::before {
  content: '';
  position: absolute;
  top: -1px; left: -1px;
  width: 45px; height: 45px;
  border-top: 2px solid #00f0ff;
  border-left: 2px solid #00f0ff;
  border-top-left-radius: 12px;
}
.glass-board::after {
  content: '';
  position: absolute;
  bottom: -1px; right: -1px;
  width: 45px; height: 45px;
  border-bottom: 2px solid #b400ff;
  border-right: 2px solid #b400ff;
  border-bottom-right-radius: 12px;
}

.header-titles { margin-bottom: 30px; }
.main-title {
  color: #fff;
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 10px 0;
  letter-spacing: 0.5px;
}
.highlight {
  color: #00f0ff;
  text-shadow: 0 0 12px rgba(0, 240, 255, 0.6);
}
.sub-title {
  color: #94a3b8;
  font-size: 14px;
  margin: 0;
}

.cyber-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.form-group label {
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 500;
}

/* ===================== 输入框极致还原 ===================== */
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.2); /* 原图几乎没有底色，而是靠外边框发光 */
  border-radius: 10px;
  padding: 0 14px;
  height: 52px;
  transition: all 0.3s ease;
}

/* 账号框：青色外线 */
.cyan-focus .input-wrapper {
  border: 1px solid rgba(0, 240, 255, 0.5);
  box-shadow: 0 0 15px rgba(0, 240, 255, 0.15), inset 0 0 10px rgba(0, 240, 255, 0.05);
}
.cyan-focus:focus-within .input-wrapper {
  border-color: #00f0ff;
  box-shadow: 0 0 20px rgba(0, 240, 255, 0.3), inset 0 0 10px rgba(0, 240, 255, 0.1);
}

/* 密码框：紫色外线 */
.purple-focus .input-wrapper {
  border: 1px solid rgba(180, 0, 255, 0.4);
  box-shadow: 0 0 15px rgba(180, 0, 255, 0.1), inset 0 0 10px rgba(180, 0, 255, 0.05);
}
.purple-focus:focus-within .input-wrapper {
  border-color: #b400ff;
  box-shadow: 0 0 20px rgba(180, 0, 255, 0.3), inset 0 0 10px rgba(180, 0, 255, 0.1);
}

.input-wrapper input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #fff;
  font-size: 15px;
  margin-left: 12px;
}
.input-wrapper input::placeholder {
  color: #475569;
}

.prefix-icon { color: #64748b; font-size: 18px; }
.suffix-icon { color: #64748b; font-size: 18px; margin-left: 10px; }
.cursor-pointer { cursor: pointer; }
.suffix-icon:hover { color: #fff; }

/* ===================== 青紫渐变按钮 ===================== */
.btn-gradient-login {
  margin-top: 5px;
  height: 54px;
  border: none;
  border-radius: 10px;
  /* 严格还原原图：左侧青色、中间蓝紫、右侧亮紫，不用拉伸 200% */
  background: linear-gradient(90deg, #00f2fe 0%, #4facfe 40%, #8E2DE2 80%, #b400ff 100%);
  color: white;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 20px rgba(142, 45, 226, 0.4);
}
.btn-gradient-login:hover {
  filter: brightness(1.2);
  transform: translateY(-2px);
  box-shadow: 0 12px 25px rgba(180, 0, 255, 0.5);
}

/* ===================== 底部社交操作 ===================== */
.divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 5px 0;
}
.divider .line {
  flex: 1;
  height: 1px;
  background: rgba(255,255,255,0.08);
}
.divider .text {
  color: #475569;
  font-size: 12px;
  padding: 0 12px;
}

.btn-github {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 50px;
  background: rgba(15, 20, 25, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  color: #cbd5e1;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-github:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.2);
  color: #fff;
  transform: translateY(-1px);
}

.toggle-mode {
  text-align: center;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s;
}
.toggle-mode:hover {
  color: #00f0ff;
}

.social-buttons {
  display: flex;
  gap: 12px;
}
.social-buttons .btn-github,
.social-buttons .btn-gitee {
  flex: 1;
}

.btn-gitee {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 50px;
  background: rgba(15, 20, 25, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  color: #cbd5e1;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-gitee:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: #c71d23;
  color: #fff;
  transform: translateY(-1px);
}
</style>
