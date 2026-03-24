<template>
  <el-config-provider :zIndex="3000">
    <router-view></router-view>
  </el-config-provider>
</template>

<script setup>
</script>

<style>
/* 基础全局颜色变量 (严格还原设计图) */
:root {
  --bg-darkest: #11141a;       /* 最深色底层背景 */
  --bg-panel: #181c25;         /* 面板背景色 */
  --bg-panel-hover: #1f2430;
  --border-color: #242a38;     /* 面板边框 */
  --cyan-primary: #00e5ff;     /* 标志性青色 */
  --cyan-dim: rgba(0, 229, 255, 0.15);
  --text-main: #e2e8f0;        /* 主文字 */
  --text-muted: #64748b;       /* 次要文字 */
}

html, body, #app {
  margin: 0; padding: 0;
  height: 100vh; width: 100vw;
  overflow: hidden;
  background-color: var(--bg-darkest);
  color: var(--text-main);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  letter-spacing: 0.3px;
}

/* 深色模式基础 */
.dark { color-scheme: dark; }

/* 全局滚动条美化 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.4);
}
</style>
