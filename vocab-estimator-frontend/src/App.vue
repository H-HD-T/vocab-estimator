<template>
  <div id="app">
    <div class="app-layout">
      <!-- Sidebar Wrapper (collapsible) -->
      <div
        class="sidebar-wrapper"
        :class="{ collapsed: isCollapsed }"
        @mouseenter="expandSidebar"
        @mouseleave="collapseSidebar"
      >
        <!-- Collapsed indicator strip (visible when collapsed) -->
        <div v-if="isCollapsed" class="sidebar-collapsed-indicator">
          <span class="indicator-icon"><el-icon><DArrowRight /></el-icon></span>
        </div>

        <aside class="clay-sidebar">
          <div class="clay-sidebar-header">
            <div class="clay-sidebar-logo">
              <el-icon :size="28" color="#EA580C"><Medal /></el-icon>
            </div>
            <div class="clay-sidebar-title">词汇量估算</div>
            <div class="clay-sidebar-subtitle">Vocabulary Estimator</div>
          </div>

          <nav class="clay-nav">
            <router-link to="/" class="clay-nav-item" :class="{ active: route.path === '/' }">
              <span class="nav-icon"><el-icon><HomeFilled /></el-icon></span>
              <span class="nav-text">首页</span>
              <span class="nav-dot"></span>
            </router-link>
            <router-link to="/online-test" class="clay-nav-item" :class="{ active: route.path === '/online-test' }">
              <span class="nav-icon"><el-icon><Edit /></el-icon></span>
              <span class="nav-text">在线测试</span>
              <span class="nav-dot"></span>
            </router-link>
            <router-link to="/batch-manage" class="clay-nav-item" :class="{ active: route.path === '/batch-manage' }">
              <span class="nav-icon"><el-icon><Upload /></el-icon></span>
              <span class="nav-text">批处理</span>
              <span class="nav-dot"></span>
            </router-link>
            <router-link to="/corpus-analysis" class="clay-nav-item" :class="{ active: route.path === '/corpus-analysis' }">
              <span class="nav-icon"><el-icon><DataAnalysis /></el-icon></span>
              <span class="nav-text">语料分析</span>
              <span class="nav-dot"></span>
            </router-link>
            <router-link to="/validation" class="clay-nav-item" :class="{ active: route.path === '/validation' }">
              <span class="nav-icon"><el-icon><Check /></el-icon></span>
              <span class="nav-text">算法验证</span>
              <span class="nav-dot"></span>
            </router-link>
            <router-link to="/stats" class="clay-nav-item" :class="{ active: route.path === '/stats' }">
              <span class="nav-icon"><el-icon><TrendCharts /></el-icon></span>
              <span class="nav-text">统计报表</span>
              <span class="nav-dot"></span>
            </router-link>
            <router-link to="/word-library" class="clay-nav-item" :class="{ active: route.path === '/word-library' }">
              <span class="nav-icon"><el-icon><Notebook /></el-icon></span>
              <span class="nav-text">词汇库</span>
              <span class="nav-dot"></span>
            </router-link>
          </nav>

          <div class="clay-sidebar-footer">
            <span>v1.0.0 - Fun Edition</span>
          </div>
        </aside>
      </div>

      <!-- Main area -->
      <div style="flex: 1; display: flex; flex-direction: column; overflow: hidden;">
        <header class="clay-header">
          <div class="clay-header-title">
            <span class="header-icon"><el-icon><Coin /></el-icon></span>
            英语词汇量估算工具
          </div>
          <div class="clay-header-badge">
            <span class="clay-badge">
              <el-icon><StarFilled /></el-icon> Fun Edition
            </span>
          </div>
        </header>

        <main class="clay-main">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// Sidebar collapse state
const isCollapsed = ref(true)
let collapseTimer = null

function expandSidebar() {
  if (collapseTimer) {
    clearTimeout(collapseTimer)
    collapseTimer = null
  }
  isCollapsed.value = false
}

function collapseSidebar() {
  collapseTimer = setTimeout(() => {
    isCollapsed.value = true
  }, 400)
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
}
#app {
  height: 100vh;
  width: 100vw;
}
.app-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
}
</style>
