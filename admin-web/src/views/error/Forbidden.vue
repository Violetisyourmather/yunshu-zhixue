<template>
  <div class="admin-error-page">
    <p class="muted">403</p>
    <h2>无访问权限</h2>
    <p class="muted">当前账号没有访问该页面的权限，请联系超级管理员。</p>
    <el-button type="primary" @click="$router.replace('/dashboard')">返回数据看板</el-button>
  </div>
</template>
