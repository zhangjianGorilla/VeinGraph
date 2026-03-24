<template>
  <div class="oauth-callback">
    <div class="loading-container">
      <el-icon class="spin-icon" :size="40"><Loading /></el-icon>
      <p>正在完成登录认证...</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'

const router = useRouter()

onMounted(() => {
  const params = new URLSearchParams(window.location.search)
  const token = params.get('token')

  if (token) {
    localStorage.setItem('token', token)
    router.replace('/')
  } else {
    router.replace('/login')
  }
})
</script>

<style scoped>
.oauth-callback {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0b1016;
}
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #e2e8f0;
}
.spin-icon {
  color: #00e5ff;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  100% { transform: rotate(360deg); }
}
</style>
