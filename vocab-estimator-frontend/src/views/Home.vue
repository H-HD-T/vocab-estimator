<template>
  <div class="home-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-inner">
        <!-- Left: Hero Content -->
        <div class="hero-content">
          <div class="hero-badge">
            <span class="badge-dot"></span>
            {{ currentUser ? '已登录: ' + currentUser.studentCode : '欢迎使用' }}
          </div>

          <h1 class="hero-title">
            探索你的<br/>
            <span class="hero-highlight">英语词汇量</span>
          </h1>

          <p class="hero-desc">
            基于词频加权与分层校准算法，支持在线测试、批量词表估算、
            语料分析与四六级成绩相关性统计。
          </p>

          <div class="hero-actions">
            <button class="clay-btn clay-btn-primary clay-btn-lg" @click="$router.push('/online-test')">
              <el-icon><Edit /></el-icon> 开始测试词汇
            </button>
            <button class="clay-btn clay-btn-outline clay-btn-lg" @click="$router.push('/corpus-analysis')">
              <el-icon><DataAnalysis /></el-icon> 分析语料
            </button>
          </div>

          <div class="hero-stats">
            <div class="hero-stat-item">
              <div class="hero-stat-value">{{ stats[0].value }}</div>
              <div class="hero-stat-label">{{ stats[0].label }}</div>
            </div>
            <div class="hero-stat-divider"></div>
            <div class="hero-stat-item">
              <div class="hero-stat-value">{{ stats[1].value }}</div>
              <div class="hero-stat-label">{{ stats[1].label }}</div>
            </div>
            <div class="hero-stat-divider"></div>
            <div class="hero-stat-item">
              <div class="hero-stat-value">{{ stats[2].value }}</div>
              <div class="hero-stat-label">{{ stats[2].label }}</div>
            </div>
          </div>
        </div>

        <!-- Right: Progress Card -->
        <div class="hero-visual">
          <div class="hero-card">
            <div class="hero-card-header">
              <div class="hero-card-icon">
                <el-icon :size="24"><Medal /></el-icon>
              </div>
              <div>
                <div class="hero-card-title">我的词汇量</div>
                <div class="hero-card-subtitle">
                  {{ currentUser ? '上次测试: ' + (lastTestDate || '暂无') : '登录后开始测试' }}
                </div>
              </div>
            </div>

            <div v-if="currentUser && lastEstimate" class="hero-card-body">
              <div class="hero-card-score">
                <span class="score-number">{{ lastEstimate }}</span>
                <span class="score-unit">词</span>
              </div>

              <div class="hero-card-progress">
                <div class="progress-row">
                  <span class="progress-label">CET-4 (4500)</span>
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: Math.min(100, (lastEstimate / 4500) * 100) + '%' }"></div>
                  </div>
                </div>
                <div class="progress-row">
                  <span class="progress-label">CET-6 (6000)</span>
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: Math.min(100, (lastEstimate / 6000) * 100) + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="hero-card-empty">
              <el-icon :size="40"><EditPen /></el-icon>
              <p>完成测试获取<br/>你的词汇量评估</p>
            </div>

            <button v-if="currentUser" class="clay-btn clay-btn-secondary" style="width:100%; margin-top: 12px;" @click="$router.push('/online-test')">
              <el-icon><Lightning /></el-icon> 继续测试
            </button>
          </div>

          <!-- Floating decorations -->
          <div class="hero-float-1 clay-animate-float">
            <el-icon :size="28"><StarFilled /></el-icon>
          </div>
          <div class="hero-float-2 clay-animate-bounce">
            <el-icon :size="24"><Reading /></el-icon>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features-section">
      <div class="features-header">
        <span class="features-badge">核心功能</span>
        <h2 class="features-title">全方位的词汇评估工具</h2>
        <p class="features-desc">从在线测试到批量分析，满足不同场景的词汇量评估需求</p>
      </div>

      <div class="features-grid">
        <div
          v-for="(item, idx) in features"
          :key="idx"
          class="feature-card"
          :style="{
            borderColor: item.borderColor,
            '--card-shadow': item.shadowColor
          }"
          @click="$router.push(item.route)"
        >
          <div class="feature-card-icon" :style="{ background: item.bgColor, borderColor: item.borderColor }">
            <el-icon :size="28" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
          <h3 class="feature-card-title">{{ item.title }}</h3>
          <p class="feature-card-desc">{{ item.desc }}</p>
          <div class="feature-card-action">
            <span>{{ item.action }} </span>
            <el-icon><DArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverviewStats } from '../api/statsApi'
import { registerUser } from '../api/userApi'
import { getTestHistory } from '../api/testApi'
import { ElMessage } from 'element-plus'

const currentUser = ref(null)
const loggingIn = ref(false)
const lastEstimate = ref(null)
const lastTestDate = ref(null)

const stats = ref([
  { label: '总用户数', value: '--' },
  { label: '总测试数', value: '--' },
  { label: '平均词汇量', value: '--' }
])

const features = [
  {
    title: '在线测试', desc: '从K/P/F/C四级难度随机抽取40题，选择正确释义，即时评估词汇量',
    icon: 'Edit', route: '/online-test',
    color: '#7C3AED', bgColor: '#EDE9FE', borderColor: '#C4B5FD', shadowColor: 'rgba(124,58,237,0.12)',
    action: '开始测试'
  },
  {
    title: '批处理', desc: '上传自定义词表，批量标记认识/不认识，快速估算整体词汇水平',
    icon: 'Upload', route: '/batch-manage',
    color: '#059669', bgColor: '#D1FAE5', borderColor: '#6EE7B7', shadowColor: 'rgba(5,150,105,0.12)',
    action: '上传词表'
  },
  {
    title: '语料分析', desc: '粘贴或上传英文文本，自动提取不重复单词并评估文本所需的词汇量',
    icon: 'DataAnalysis', route: '/corpus-analysis',
    color: '#F59E0B', bgColor: '#FEF3C7', borderColor: '#FDE68A', shadowColor: 'rgba(245,158,11,0.12)',
    action: '分析文本'
  },
  {
    title: '统计报表', desc: '查看四六级成绩与词汇量的相关性分析，了解你的词汇水平定位',
    icon: 'TrendCharts', route: '/stats',
    color: '#DB2777', bgColor: '#FCE7F3', borderColor: '#F9A8D4', shadowColor: 'rgba(236,72,153,0.10)',
    action: '查看报表'
  }
]

async function autoLogin() {
  loggingIn.value = true
  try {
    const code = 'Guest' + Date.now().toString().slice(-6)
    const res = await registerUser({ studentCode: code, nameAlias: '' })
    if (res.code === 200) {
      currentUser.value = res.data
      localStorage.setItem('currentUser', JSON.stringify(res.data))
      await loadLastTest()
    }
  } catch (e) {
    ElMessage.error('自动登录失败，请检查后端是否启动')
  } finally {
    loggingIn.value = false
  }
}

async function loadLastTest() {
  if (!currentUser.value) return
  try {
    const res = await getTestHistory(currentUser.value.id)
    if (res.code === 200 && res.data && res.data.length > 0) {
      const last = res.data[res.data.length - 1]
      lastEstimate.value = last.estimateVocab
      lastTestDate.value = last.createdAt ? last.createdAt.slice(0, 10) : null
    }
  } catch (e) { console.log(e) }
}

onMounted(async () => {
  const saved = localStorage.getItem('currentUser')
  if (saved) {
    currentUser.value = JSON.parse(saved)
    await loadLastTest()
  } else {
    await autoLogin()
  }
  try {
    const res = await getOverviewStats()
    if (res.code === 200) {
      stats.value = [
        { label: '总用户数', value: res.data.userCount },
        { label: '总测试数', value: res.data.testCount },
        { label: '平均词汇量', value: res.data.avgVocab }
      ]
    }
  } catch (e) { console.log('Stats unavailable') }
})
</script>

<style scoped>
.home-page {
  padding-bottom: 8px;
}

/* ===== Hero Section ===== */
.hero-section {
  position: relative;
  padding: 16px 0 32px;
}

.hero-inner {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  align-items: center;
}

/* Hero Content (Left) */
.hero-content {
  animation: popIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 999px;
  background: #EDE9FE;
  border: 2.5px solid #C4B5FD;
  font-family: 'Baloo 2', cursive;
  font-weight: 600;
  font-size: 13px;
  color: #5B21B6;
  margin-bottom: 20px;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #7C3AED;
}

.hero-title {
  font-family: 'Baloo 2', cursive;
  font-weight: 800;
  font-size: 42px;
  line-height: 1.15;
  color: var(--clay-text);
  margin: 0 0 16px;
}

.hero-highlight {
  color: #7C3AED;
  text-decoration: underline wavy 3px #C4B5FD;
  text-underline-offset: 8px;
}

.hero-desc {
  font-family: 'Comic Neue', sans-serif;
  font-size: 16px;
  color: var(--clay-text-secondary);
  line-height: 1.7;
  max-width: 440px;
  margin: 0 0 28px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 20px;
}

.hero-stat-item {
  text-align: center;
}

.hero-stat-value {
  font-family: 'Baloo 2', cursive;
  font-weight: 800;
  font-size: 26px;
  color: var(--clay-text);
}

.hero-stat-label {
  font-family: 'Comic Neue', sans-serif;
  font-size: 13px;
  color: var(--clay-text-secondary);
  margin-top: 2px;
}

.hero-stat-divider {
  width: 2px;
  height: 36px;
  background: #E7E5E4;
  border-radius: 2px;
}

/* Hero Visual Card (Right) */
.hero-visual {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: popIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.hero-card {
  background: white;
  border-radius: var(--clay-radius);
  border: 3.5px solid #C4B5FD;
  box-shadow: 6px 6px 0px rgba(124, 58, 237, 0.12), inset -2px -2px 8px rgba(0, 0, 0, 0.04), inset 2px 2px 8px rgba(255, 255, 255, 0.85);
  padding: 24px;
  width: 100%;
  max-width: 340px;
  position: relative;
  z-index: 2;
}

.hero-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.hero-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: #EDE9FE;
  border: 2.5px solid #C4B5FD;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7C3AED;
  flex-shrink: 0;
}

.hero-card-title {
  font-family: 'Baloo 2', cursive;
  font-weight: 700;
  font-size: 17px;
  color: var(--clay-text);
}

.hero-card-subtitle {
  font-family: 'Comic Neue', sans-serif;
  font-size: 12px;
  color: var(--clay-text-secondary);
  margin-top: 2px;
}

.hero-card-body {
  margin-bottom: 4px;
}

.hero-card-score {
  text-align: center;
  padding: 12px 0;
  margin-bottom: 12px;
}

.score-number {
  font-family: 'Baloo 2', cursive;
  font-weight: 800;
  font-size: 48px;
  color: #7C3AED;
  line-height: 1;
}

.score-unit {
  font-family: 'Baloo 2', cursive;
  font-weight: 600;
  font-size: 18px;
  color: var(--clay-text-secondary);
  margin-left: 4px;
}

.hero-card-progress {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-label {
  font-family: 'Comic Neue', sans-serif;
  font-size: 12px;
  color: var(--clay-text-secondary);
  white-space: nowrap;
  min-width: 70px;
}

.progress-track {
  flex: 1;
  height: 12px;
  background: #F5F5F4;
  border-radius: 999px;
  border: 2px solid #E7E5E4;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #C4B5FD, #7C3AED);
  border-radius: 999px;
  transition: width 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.hero-card-empty {
  text-align: center;
  padding: 24px 0;
  color: #D6D3D1;
}

.hero-card-empty p {
  font-family: 'Comic Neue', sans-serif;
  font-size: 14px;
  color: var(--clay-text-secondary);
  margin-top: 8px;
  line-height: 1.5;
}

/* Floating decorations */
.hero-float-1 {
  position: absolute;
  top: -16px;
  right: -24px;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: #EDE9FE;
  border: 3px solid #C4B5FD;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7C3AED;
  box-shadow: 4px 4px 0 rgba(124, 58, 237, 0.12);
  z-index: 3;
}

.hero-float-2 {
  position: absolute;
  bottom: -12px;
  left: -20px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #FEF3C7;
  border: 3px solid #FDE68A;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #F59E0B;
  box-shadow: 4px 4px 0 rgba(245, 158, 11, 0.12);
  z-index: 3;
}

/* ===== Features Section ===== */
.features-section {
  padding: 24px 0 8px;
  animation: popIn 0.7s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.features-header {
  text-align: center;
  margin-bottom: 24px;
}

.features-badge {
  display: inline-flex;
  padding: 4px 16px;
  border-radius: 999px;
  background: #EDE9FE;
  border: 2.5px solid #C4B5FD;
  font-family: 'Baloo 2', cursive;
  font-weight: 600;
  font-size: 13px;
  color: #5B21B6;
  margin-bottom: 12px;
}

.features-title {
  font-family: 'Baloo 2', cursive;
  font-weight: 700;
  font-size: 24px;
  color: var(--clay-text);
  margin: 0 0 8px;
}

.features-desc {
  font-family: 'Comic Neue', sans-serif;
  font-size: 15px;
  color: var(--clay-text-secondary);
  margin: 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.feature-card {
  background: white;
  border-radius: var(--clay-radius);
  border: 3px solid;
  box-shadow: 5px 5px 0 var(--card-shadow, rgba(0,0,0,0.08)), inset -1px -1px 4px rgba(0,0,0,0.04), inset 1px 1px 4px rgba(255,255,255,0.85);
  padding: 24px 20px;
  cursor: pointer;
  transition: all var(--clay-transition);
  display: flex;
  flex-direction: column;
}

.feature-card:hover {
  transform: translateY(-5px) scale(1.02);
  box-shadow: 8px 8px 0 var(--card-shadow, rgba(0,0,0,0.1));
}

.feature-card-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  border: 2.5px solid;
  transition: transform var(--clay-transition);
}

.feature-card:hover .feature-card-icon {
  animation: clayWiggle 0.5s ease-in-out;
}

.feature-card-title {
  font-family: 'Baloo 2', cursive;
  font-weight: 700;
  font-size: 18px;
  color: var(--clay-text);
  margin: 0 0 8px;
}

.feature-card-desc {
  font-family: 'Comic Neue', sans-serif;
  font-size: 13px;
  color: var(--clay-text-secondary);
  line-height: 1.5;
  margin: 0 0 16px;
  flex: 1;
}

.feature-card-action {
  font-family: 'Baloo 2', cursive;
  font-weight: 600;
  font-size: 14px;
  color: var(--clay-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color var(--clay-transition);
}

.feature-card:hover .feature-card-action {
  color: #7C3AED;
}

@media (max-width: 1024px) {
  .hero-inner {
    grid-template-columns: 1fr;
    gap: 32px;
  }
  .hero-visual { order: -1; }
  .features-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .hero-title { font-size: 32px; }
  .hero-stats { flex-wrap: wrap; gap: 12px; }
  .hero-stat-divider { display: none; }
  .features-grid { grid-template-columns: 1fr; }
}
</style>
