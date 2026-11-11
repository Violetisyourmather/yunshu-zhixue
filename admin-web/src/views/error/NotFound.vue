<template>
  <div class="admin-error-page">
    <p class="muted">404</p>
    <h2>页面不存在</h2>
    <p class="muted">你访问的页面可能已被移动或删除。</p>
    <el-button type="primary" @click="$router.replace('/dashboard')">返回数据看板</el-button>
  </div>
</template>
