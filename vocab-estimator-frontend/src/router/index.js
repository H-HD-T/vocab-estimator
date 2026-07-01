import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/online-test',
    name: 'OnlineTest',
    component: () => import('@/views/OnlineTest.vue')
  },
  {
    path: '/batch-manage',
    name: 'BatchManage',
    component: () => import('@/views/BatchManage.vue')
  },
  {
    path: '/corpus-analysis',
    name: 'CorpusAnalysis',
    component: () => import('@/views/CorpusAnalysis.vue')
  },
  {
    path: '/validation',
    name: 'ValidationCompare',
    component: () => import('@/views/ValidationCompare.vue')
  },
  {
    path: '/stats',
    name: 'StatsReport',
    component: () => import('@/views/StatsReport.vue')
  },
  {
    path: '/word-library',
    name: 'WordLibrary',
    component: () => import('@/views/WordLibrary.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
