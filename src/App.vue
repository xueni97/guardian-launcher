<template>
  <router-view />
</template>

<script setup>
</script>

<style>
/* 全局大字体：老人友好，根字号放大到 22px */
html {
  font-size: 22px;
}

body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
  background: #f5f7fa;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
  /* 防止老人误触选中文字 */
  -webkit-user-select: none;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

#app {
  min-height: 100vh;
}

/* Vant 组件字号整体放大 */
:root {
  --van-font-size-sm: 16px;
  --van-font-size-md: 20px;
  --van-font-size-lg: 24px;
  --van-nav-bar-height: 56px;
  --van-button-default-height: 56px;
  --van-button-large-height: 64px;
}
</style>
