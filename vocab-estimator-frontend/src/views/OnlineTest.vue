<template>
  <div class="online-test-page">
    <div class="page-title">
      <span class="title-icon"><el-icon><Edit /></el-icon></span>
      在线词汇测试
    </div>

    <!-- Start screen -->
    <div v-if="!testing && !testResult" class="clay-card" style="border-color: #C4B5FD;">
      <div style="padding: 40px 20px; text-align: center;">
        <div style="width: 80px; height: 80px; border-radius: 20px; background: #EDE9FE; border: 3px solid #C4B5FD; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; font-size: 40px; color: #7C3AED;">
          <el-icon :size="40"><EditPen /></el-icon>
        </div>
        <h2 style="font-family: 'Baloo 2', cursive; font-size: 28px; color: var(--clay-text); margin-bottom: 8px;">词汇量测试</h2>
        <p style="color: var(--clay-text-secondary); font-size: 15px; line-height: 1.8; max-width: 500px; margin: 0 auto 24px;">
          共 40 题，从 K/P/F/C 四个难度等级随机抽取<br/>
          选择单词对应的正确释义，测出你的词汇量
        </p>
        <div style="display: flex; justify-content: center; gap: 20px; margin-bottom: 28px; flex-wrap: wrap;">
          <div class="clay-stat-box">
            <div class="clay-stat-value" style="color: #7C3AED;">40</div>
            <div class="clay-stat-label">题数</div>
          </div>
          <div class="clay-stat-box" style="border-color: #C4B5FD;">
            <div class="clay-stat-value" style="color: #F59E0B; font-size: 18px;">K/P/F/C</div>
            <div class="clay-stat-label">四级难度</div>
          </div>
          <div class="clay-stat-box" style="border-color: #F9A8D4;">
            <div class="clay-stat-value" style="color: #EC4899;">--</div>
            <div class="clay-stat-label">你的词汇量</div>
          </div>
        </div>
        <button class="clay-btn clay-btn-primary clay-btn-lg" @click="startTest" :disabled="loading">
          <el-icon v-if="loading" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><MagicStick /></el-icon>
          {{ loading ? '生成试卷中...' : '开始测词汇' }}
        </button>
      </div>
    </div>

    <!-- Test screen -->
    <div v-if="testing" class="clay-card" style="border-color: #C4B5FD;">
      <div style="padding: 20px;">
        <!-- Progress -->
        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 20px;">
          <span style="font-family: 'Baloo 2', cursive; font-size: 15px; color: var(--clay-text-secondary); white-space: nowrap;">
            <el-icon><List /></el-icon> {{ answeredCount }} / {{ totalWords }}
          </span>
          <div style="flex: 1;">
            <el-progress :percentage="progressPercent" :show-text="false" :stroke-width="8" color="#7C3AED" />
          </div>
          <span v-if="currentWord" class="clay-tag" :class="levelTagCls">{{ currentWord.difficulty }}</span>
        </div>

        <!-- Word -->
        <div style="text-align: center; padding: 20px 0 24px;">
          <div style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: var(--clay-text-secondary); margin-bottom: 8px;">第 {{ currentIndex + 1 }} 题</div>
          <div style="font-family: 'Baloo 2', cursive; font-size: 48px; font-weight: 800; color: var(--clay-text); letter-spacing: 2px;">
            {{ currentWord?.word }}
          </div>
        </div>

        <!-- Options -->
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; max-width: 560px; margin: 0 auto;">
          <div
            v-for="(opt, idx) in options"
            :key="idx"
            class="clay-option"
            :class="{
              correct: selectedOption !== null && opt.isCorrect,
              wrong: selectedOption === idx && !opt.isCorrect,
              disabled: selectedOption !== null
            }"
            @click="selectOption(idx)"
          >
            <span class="clay-option-label">{{ optionLabels[idx] }}</span>
            <span class="clay-option-text">{{ opt.label }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Result screen -->
    <div v-if="testResult && !testing" class="clay-card" style="border-color: #6EE7B7; text-align: center;">
      <div style="padding: 30px 20px;">
        <div style="width: 72px; height: 72px; border-radius: 20px; display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; font-size: 36px;"
          :style="{ background: resultBadge.bg, border: '3px solid ' + resultBadge.border }">
          <el-icon :size="36" :color="resultBadge.color"><Medal /></el-icon>
        </div>
        <h2 style="font-family: 'Baloo 2', cursive; font-size: 24px; color: var(--clay-text); margin-bottom: 16px;">测试完成!</h2>

        <div style="margin-bottom: 16px;">
          <div style="font-family: 'Baloo 2', cursive; font-size: 48px; font-weight: 800; color: #10B981;">
            {{ testResult.knownCount }}<span style="font-size: 20px; color: var(--clay-text-secondary);">/{{ testResult.totalWords }}</span>
          </div>
          <div style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: var(--clay-text-secondary);">答对题数</div>
        </div>

        <div style="border-top: 2.5px solid #E2E8F0; padding-top: 20px; display: flex; justify-content: center; gap: 40px; flex-wrap: wrap;">
          <div class="clay-stat-box" style="border-color: #7C3AED;">
            <div class="clay-stat-value" style="color: #7C3AED;">{{ testResult.estimate }}</div>
            <div class="clay-stat-label">估算词汇量</div>
          </div>
          <div class="clay-stat-box" style="border-color: #F59E0B;">
            <div class="clay-stat-value" style="color: #F59E0B; font-size: 22px;">{{ testResult.minRange }} - {{ testResult.maxRange }}</div>
            <div class="clay-stat-label">估算范围</div>
          </div>
          <div class="clay-stat-box" style="border-color: #EC4899;">
            <div class="clay-stat-value" style="color: #EC4899;">{{ testResult.confidence?.toFixed(1) }}%</div>
            <div class="clay-stat-label">置信度</div>
          </div>
        </div>

        <div style="margin-top: 24px; display: flex; gap: 12px; justify-content: center;">
          <button class="clay-btn clay-btn-outline" @click="resetTest">再测一次</button>
          <button class="clay-btn clay-btn-primary" @click="showChart = !showChart">
            <el-icon><DataLine /></el-icon> 查看历史
          </button>
        </div>
      </div>
    </div>

    <!-- History chart -->
    <div v-if="historyRecords.length > 0" class="clay-card" style="margin-top: 20px; border-color: #C4B5FD;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD; display: flex; justify-content: space-between; align-items: center;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px; color: var(--clay-text);">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><Clock /></el-icon> 历史测试记录
        </span>
        <button class="clay-btn clay-btn-sm clay-btn-white" @click="showChart = !showChart">
          {{ showChart ? '隐藏图表' : '显示图表' }}
        </button>
      </div>
      <div style="padding: 16px;">
        <div ref="chartRef" style="height: 280px;" v-show="showChart"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { generatePaper, submitTest, getTestHistory } from '../api/testApi'
import * as echarts from 'echarts'

const currentUser = ref(null)
const testing = ref(false)
const loading = ref(false)
const testResult = ref(null)
const words = ref([])
const currentIndex = ref(0)
const options = ref([])
const selectedOption = ref(null)
const optionLabels = ['A', 'B', 'C', 'D']
const historyRecords = ref([])
const chartRef = ref(null)
const showChart = ref(false)
let chartInstance = null
const answerRecords = ref([])

const currentWord = computed(() => words.value[currentIndex.value])
const totalWords = computed(() => words.value.length)
const answeredCount = computed(() => answerRecords.value.filter(r => r.answered).length)
const progressPercent = computed(() => totalWords.value === 0 ? 0 : Math.round((answeredCount.value / totalWords.value) * 100))

const levelTagCls = computed(() => {
  const map = { K: 'clay-tag-gray', P: 'clay-tag-blue', F: 'clay-tag-amber', C: 'clay-tag-red' }
  return map[currentWord.value?.difficulty] || 'clay-tag-gray'
})

const resultBadge = computed(() => {
  if (!testResult.value) return { bg: '#EDE9FE', border: '#C4B5FD', color: '#7C3AED' }
  const e = testResult.value.estimate
  if (e >= 6000) return { bg: '#FEE2E2', border: '#FCA5A5', color: '#EF4444' }
  if (e >= 4000) return { bg: '#FEF3C7', border: '#C4B5FD', color: '#F59E0B' }
  if (e >= 2000) return { bg: '#EDE9FE', border: '#C4B5FD', color: '#7C3AED' }
  return { bg: '#F1F5F9', border: '#CBD5E1', color: '#64748B' }
})

async function startTest() {
  if (!currentUser.value) { ElMessage.warning('请先进入首页自动登录'); return }
  loading.value = true
  testResult.value = null
  words.value = []; currentIndex.value = 0; answerRecords.value = []
  try {
    const res = await generatePaper(currentUser.value.id, 40)
    if (res.code === 200) {
      words.value = res.data.words || []
      if (words.value.length === 0) { ElMessage.error('题库为空，无法生成试卷'); loading.value = false; return }
      answerRecords.value = words.value.map(w => ({ wordId: w.id, word: w.word, known: false, answered: false }))
      currentIndex.value = 0; selectedOption.value = null; testing.value = true
      buildOptions()
    } else { ElMessage.error('生成试卷失败: ' + (res.message || '未知错误')) }
  } catch (e) { ElMessage.error('生成试卷失败，请检查后端是否启动') }
  finally { loading.value = false }
}

function buildOptions() {
  const word = currentWord.value
  if (!word) return
  const correct = { label: word.definition || word.word, isCorrect: true }
  const pool = words.value.filter(w => w.id !== word.id && w.definition)
  const shuffled = [...pool].sort(() => Math.random() - 0.5)
  const distractors = shuffled.slice(0, 3).map(w => ({ label: w.definition, isCorrect: false }))
  const allOptions = [correct, ...distractors].sort(() => Math.random() - 0.5)
  options.value = allOptions
  selectedOption.value = null
}

function selectOption(idx) {
  if (selectedOption.value !== null) return
  selectedOption.value = idx
  const isCorrect = options.value[idx].isCorrect
  if (answerRecords.value[currentIndex.value]) {
    answerRecords.value[currentIndex.value].known = isCorrect
    answerRecords.value[currentIndex.value].answered = true
  }
  if (currentIndex.value < words.value.length - 1) {
    setTimeout(() => { currentIndex.value++; buildOptions() }, isCorrect ? 500 : 1000)
  } else {
    setTimeout(async () => { await doSubmit() }, isCorrect ? 500 : 1000)
  }
}

async function doSubmit() {
  try {
    const answeredWords = answerRecords.value.filter(r => r.answered)
    if (answeredWords.length === 0) { ElMessage.warning('没有回答任何题目'); testing.value = false; return }
    const submitData = { answers: answeredWords.map(r => ({ wordId: r.wordId, word: r.word, known: r.known })), testType: 'GUI' }
    const res = await submitTest(currentUser.value.id, submitData)
    if (res.code === 200) {
      testResult.value = res.data; testing.value = false
      await loadHistory(); await nextTick(); renderChart()
    } else { ElMessage.error('提交失败: ' + (res.message || '未知错误')) }
  } catch (e) { ElMessage.error('提交失败') }
}

function resetTest() {
  testResult.value = null; words.value = []; currentIndex.value = 0
  options.value = []; selectedOption.value = null; answerRecords.value = []
  startTest()
}

async function loadHistory() {
  if (!currentUser.value) return
  try { const res = await getTestHistory(currentUser.value.id); if (res.code === 200) historyRecords.value = (res.data || []).slice().reverse() }
  catch (e) { console.log(e) }
}

function renderChart() {
  if (!chartRef.value || historyRecords.value.length === 0) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: historyRecords.value.map((_, i) => '第' + (i + 1) + '次'), axisLabel: { fontFamily: 'Comic Neue' } },
    yAxis: { type: 'value', name: '词汇量', nameTextStyle: { fontFamily: 'Comic Neue', fontSize: 12 } },
    series: [{
      name: '词汇量', type: 'line', data: historyRecords.value.map(r => r.estimateVocab),
      markLine: { data: [{ type: 'average', name: '均值' }] },
      itemStyle: { color: '#7C3AED' },
      areaStyle: { color: 'rgba(124,58,237,0.1)' },
      smooth: true
    }]
  })
}

onMounted(() => {
  const saved = localStorage.getItem('currentUser')
  if (saved) { currentUser.value = JSON.parse(saved); loadHistory() }
})
</script>

<style scoped>
.online-test-page { animation: popIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
</style>
