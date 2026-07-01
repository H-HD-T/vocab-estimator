<template>
  <div class="validation-page">
    <div class="page-title">
      <span class="title-icon"><el-icon><Check /></el-icon></span>
      算法验证
    </div>

    <!-- 采集进度弹窗 -->
    <el-dialog v-model="scrapProgress.visible" :close-on-click-modal="false" :show-close="false" width="420px" align-center>
      <div style="text-align: center; padding: 20px 0;">
        <el-icon :size="48" class="is-loading" color="#7C3AED"><Loading /></el-icon>
        <h3 style="margin: 16px 0 8px; font-family: 'Baloo 2', cursive; color: #7C3AED;">正在采集数据...</h3>
        <div style="margin: 20px 0;">
          <el-progress :percentage="Math.round((scrapProgress.step / (scrapSteps.length - 1)) * 100)" :stroke-width="10" color="#7C3AED" />
        </div>
        <div v-for="(s, i) in scrapSteps" :key="i" style="display: flex; align-items: center; gap: 10px; padding: 8px 0; text-align: left;">
          <el-icon v-if="i &lt; scrapProgress.step" color="#10B981" size="18"><Check /></el-icon>
          <el-icon v-else-if="i === scrapProgress.step" class="is-loading" color="#7C3AED" size="18"><Loading /></el-icon>
          <el-icon v-else color="#D1D5DB" size="18"><Circle /></el-icon>
          <span :style="{ fontSize: '14px', color: i === scrapProgress.step ? '#7C3AED' : i &lt; scrapProgress.step ? '#10B981' : '#9CA3AF' }">{{ s }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- TestYourVocab 自动化采集 -->
    <div class="clay-card" style="border-color: #F9A8D4;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #F9A8D4;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><Link /></el-icon> TestYourVocab 自动化采集验证
        </span>
      </div>
      <div style="padding: 16px;">
        <div style="background: #FCE7F3; border-radius: var(--clay-radius-sm); border: 2.5px solid #F9A8D4; padding: 12px 16px; margin-bottom: 16px; display: flex; align-items: center; gap: 10px;">
          <el-icon size="20" color="#DB2777"><InfoFilled /></el-icon>
          <span style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: var(--clay-text);">
            自动访问 testyourvocab.com 完成词汇测试，采集认识词(Ri)、不认识词(Ui)、网站估算值(Ci)，与本算法结果(Di)对比。
          </span>
        </div>
        <div style="display: flex; gap: 10px; flex-wrap: wrap; align-items: center;">
          <button class="clay-btn clay-btn-primary" @click="collectOne" :disabled="collecting" style="background: #DB2777; box-shadow: 0 4px 0 #9D174D;">
            <el-icon v-if="collecting" class="is-loading"><Loading /></el-icon>
            <el-icon v-else><Lightning /></el-icon>
            {{ collecting ? '采集中...' : '采集一轮' }}
          </button>
          <button class="clay-btn" @click="refreshStats" :disabled="loadingStats">
            <el-icon><Refresh /></el-icon> 刷新统计
          </button>
          <button class="clay-btn" style="background: #FEE2E2; border-color: #FCA5A5; color: #DC2626;" @click="clearAll">
            <el-icon><Delete /></el-icon> 清空数据
          </button>
          <span style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: #6B7280;">
            样本 <span style="background: #FCE7F3; border: 2.5px solid #F9A8D4; border-radius: 20px; padding: 2px 10px; font-weight: 600; color: #DB2777; margin: 0 4px;">{{ sampleCount }}</span> 条
          </span>
          <span v-if="lastResult" style="font-family: 'Comic Neue', sans-serif; font-size: 13px; color: #10B981;">
            <el-icon><Check /></el-icon> Ci={{ lastResult.standardEstimate }} Di={{ lastResult.algorithmEstimate }}
          </span>
        </div>
      </div>
    </div>

    <!-- 全局统计指标 -->
    <div v-if="stats" class="clay-card" style="margin-top: 20px; border-color: #6EE7B7;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #6EE7B7;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><TrendCharts /></el-icon> 全局统计指标
        </span>
      </div>
      <div style="padding: 16px;">
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 12px;">
          <div class="clay-stat-box" style="border-color: #C4B5FD;">
            <div class="clay-stat-value" style="color: #7C3AED; font-size: 20px;">{{ stats.meanError?.toFixed(0) }}</div>
            <div class="clay-stat-label">MAE 平均绝对误差</div>
          </div>
          <div class="clay-stat-box" style="border-color: #6EE7B7;">
            <div class="clay-stat-value" style="color: #10B981; font-size: 20px;">{{ stats.meanBias?.toFixed(0) }}</div>
            <div class="clay-stat-label">平均偏差 Di-Ci</div>
          </div>
          <div class="clay-stat-box" style="border-color: #F59E0B;">
            <div class="clay-stat-value" style="color: #F59E0B; font-size: 18px;">{{ stats.mse?.toFixed(0) }}</div>
            <div class="clay-stat-label">MSE 均方误差</div>
          </div>
          <div class="clay-stat-box" style="border-color: #F9A8D4;">
            <div class="clay-stat-value" style="color: #DB2777; font-size: 20px;">{{ stats.rmse?.toFixed(0) }}</div>
            <div class="clay-stat-label">RMSE 均方根误差</div>
          </div>
          <div class="clay-stat-box" style="border-color: #C4B5FD;">
            <div class="clay-stat-value" style="color: #7C3AED;">{{ stats.correlation?.toFixed(4) }}</div>
            <div class="clay-stat-label">相关系数</div>
          </div>
          <div class="clay-stat-box" style="border-color: #F87171;">
            <div class="clay-stat-value" style="color: #DC2626;">{{ stats.maxError?.toFixed(0) }}</div>
            <div class="clay-stat-label">最大误差</div>
          </div>
          <div class="clay-stat-box" style="border-color: #6EE7B7;">
            <div class="clay-stat-value" style="color: #10B981;">{{ stats.minError?.toFixed(0) }}</div>
            <div class="clay-stat-label">最小误差</div>
          </div>
          <div class="clay-stat-box" style="border-color: #F59E0B;">
            <div class="clay-stat-value" style="color: #F59E0B; font-size: 16px;">{{ (stats.meanRelativeError * 100)?.toFixed(1) }}%</div>
            <div class="clay-stat-label">平均相对误差</div>
          </div>
        </div>
        <div class="clay-stat-box" style="border-color: #C4B5FD; padding: 10px 16px;">
          <div style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: #6B7280;">
            误差分布：
            <span style="color: #10B981; font-weight: 600;">&#124;error&#124;&le;500: {{ stats.errorDistribution?.within500 }}</span>
            <span style="color: #F59E0B; font-weight: 600; margin-left: 12px;">&le;1000: {{ stats.errorDistribution?.within1000 }}</span>
            <span style="color: #F87171; font-weight: 600; margin-left: 12px;">&le;2000: {{ stats.errorDistribution?.within2000 }}</span>
            <span style="color: #DC2626; font-weight: 600; margin-left: 12px;">&gt;2000: {{ stats.errorDistribution?.beyond2000 }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表 -->
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 20px;" v-if="stats">
      <div class="clay-card" style="border-color: #C4B5FD;">
        <div style="padding: 12px 16px; border-bottom: 2.5px solid #C4B5FD;">
          <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 15px;">Ci-Di 散点图</span>
        </div>
        <div style="padding: 12px;">
          <div ref="scatterRef" style="height: 320px;"></div>
        </div>
      </div>
      <div class="clay-card" style="border-color: #6EE7B7;">
        <div style="padding: 12px 16px; border-bottom: 2.5px solid #6EE7B7;">
          <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 15px;">误差分布直方图 (Di-Ci)</span>
        </div>
        <div style="padding: 12px;">
          <div ref="histogramRef" style="height: 320px;"></div>
        </div>
      </div>
    </div>

    <!-- 采集历史 -->
    <div v-if="history.length > 0" class="clay-card" style="margin-top: 20px; border-color: #C4B5FD;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><Document /></el-icon> 采集历史 ({{ history.length }} 条)
        </span>
      </div>
      <div style="padding: 16px;">
        <el-table :data="history" border stripe size="small" max-height="400" v-loading="loadingHistory">
          <el-table-column label="#" type="index" width="50" />
          <el-table-column label="Ci (Preply)" width="110">
            <template #default="{ row }">{{ row.standardEstimate }}</template>
          </el-table-column>
          <el-table-column label="Di (本算法)" width="110">
            <template #default="{ row }">{{ row.algorithmEstimate }}</template>
          </el-table-column>
          <el-table-column label="差值" width="100">
            <template #default="{ row }">
              <span class="clay-tag" :class="Math.abs(row.diff) < 1000 ? 'clay-tag-green' : Math.abs(row.diff) < 2000 ? 'clay-tag-amber' : 'clay-tag-red'">{{ row.diff >= 0 ? '+' : '' }}{{ row.diff }}</span>
            </template>
          </el-table-column>
          <el-table-column label="绝对误差" width="90">
            <template #default="{ row }">{{ row.absoluteError }}</template>
          </el-table-column>
          <el-table-column label="相对误差" width="90">
            <template #default="{ row }">{{ (row.relativeError * 100)?.toFixed(1) }}%</template>
          </el-table-column>
          <el-table-column label="认识/总词" width="100">
            <template #default="{ row }">{{ row.knownCount }}/{{ row.knownCount + row.unknownCount }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 导入验证数据(JSON) -->
    <div class="clay-card" style="margin-top: 20px; border-color: #C4B5FD;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><DataAnalysis /></el-icon> 导入验证数据(JSON)
        </span>
      </div>
      <div style="padding: 16px;">
        <div style="background: #EDE9FE; border-radius: var(--clay-radius-sm); border: 2.5px solid #C4B5FD; padding: 12px 16px; margin-bottom: 20px; display: flex; align-items: center; gap: 10px;">
          <el-icon size="20" color="#7C3AED"><InfoFilled /></el-icon>
          <span style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: var(--clay-text);">导入验证数据集，与本算法结果进行对比分析。</span>
        </div>
        <el-input v-model="jsonData" type="textarea" :rows="6" placeholder='JSON格式: [{"knownWords":["word1"],"unknownWords":["word2"],"standardEstimate":8000}]' />
        <div style="margin-top: 12px; display: flex; gap: 10px;">
          <button class="clay-btn clay-btn-primary" @click="runValidation" :disabled="loading">
            <el-icon v-if="loading" class="is-loading"><Loading /></el-icon>
            <el-icon v-else><Lightning /></el-icon>
            {{ loading ? '验证中...' : '运行验证' }}
          </button>
          <el-upload action="#" accept=".json,.txt" :show-file-list="false" :before-upload="handleFileUpload">
            <el-button><el-icon><FolderOpened /></el-icon> 上传JSON文件</el-button>
          </el-upload>
        </div>
      </div>
    </div>

    <div v-if="validationResult" class="clay-card" style="margin-top: 20px; border-color: #6EE7B7;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #6EE7B7;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><SuccessFilled /></el-icon> JSON验证结果
        </span>
      </div>
      <div style="padding: 16px;">
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px;">
          <div class="clay-stat-box" style="border-color: #C4B5FD;">
            <div class="clay-stat-value" style="color: #7C3AED;">{{ validationResult.meanError?.toFixed(2) }}</div>
            <div class="clay-stat-label">平均绝对误差</div>
          </div>
          <div class="clay-stat-box" style="border-color: #6EE7B7;">
            <div class="clay-stat-value" style="color: #10B981;">{{ validationResult.meanBias?.toFixed(2) }}</div>
            <div class="clay-stat-label">平均偏差</div>
          </div>
          <div class="clay-stat-box" style="border-color: #C4B5FD;">
            <div class="clay-stat-value" style="color: #F59E0B;">{{ validationResult.correlation?.toFixed(4) }}</div>
            <div class="clay-stat-label">相关系数</div>
          </div>
          <div class="clay-stat-box" style="border-color: #F9A8D4;">
            <div class="clay-stat-value" style="color: #EC4899;">{{ validationResult.sampleCount }}</div>
            <div class="clay-stat-label">样本数</div>
          </div>
        </div>
        <div ref="chartRef" style="height: 350px; margin-bottom: 20px;"></div>
        <el-table :data="validationResult.items" border stripe size="small" max-height="400">
          <el-table-column label="标准值(Ci)" width="120"><template #default="{ row }">{{ row.standardEstimate }}</template></el-table-column>
          <el-table-column label="算法值(Di)" width="130"><template #default="{ row }">{{ row.algorithmEstimate }}</template></el-table-column>
          <el-table-column label="差值" width="100">
            <template #default="{ row }"><span class="clay-tag" :class="Math.abs(row.diff) < 1000 ? 'clay-tag-green' : Math.abs(row.diff) < 2000 ? 'clay-tag-amber' : 'clay-tag-red'">{{ row.diff >= 0 ? '+' : '' }}{{ row.diff }}</span></template>
          </el-table-column>
          <el-table-column label="认识单词" min-width="200" show-overflow-tooltip><template #default="{ row }">{{ row.knownWords?.join(', ') }}</template></el-table-column>
          <el-table-column label="不认识的单词" min-width="200" show-overflow-tooltip><template #default="{ row }">{{ row.unknownWords?.join(', ') }}</template></el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { importValidationData } from '../api/validationApi'

const jsonData = ref('')
const loading = ref(false)
const validationResult = ref(null)
const chartRef = ref(null)
let chart = null

// TestYourVocab automation
const collecting = ref(false)
const loadingStats = ref(false)
const loadingHistory = ref(false)
const sampleCount = ref(0)
const stats = ref(null)
const history = ref([])
const lastResult = ref(null)
const scatterRef = ref(null)
const histogramRef = ref(null)
const scrapProgress = ref({ visible: false, step: 0, message: '' })
const scrapSteps = [
  '正在启动浏览器...',
  '正在访问 testyourvocab.com...',
  '正在加载第 1 页单词...',
  '正在勾选认识的单词...',
  '正在翻到第 2 页...',
  '正在勾选第 2 页单词...',
  '正在计算结果...',
  '正在计算算法估算值 Di...',
  '完成!'
]
let scatterChart = null
let histogramChart = null

async function collectOne() {
  collecting.value = true
  scrapProgress.value = { visible: true, step: 0, message: '' }
  const updateStep = (s) => { scrapProgress.value = { visible: true, step: s, message: '' } }

  updateStep(0)
  await new Promise(r => setTimeout(r, 800))
  updateStep(1)
  await new Promise(r => setTimeout(r, 1500))
  updateStep(2)
  await new Promise(r => setTimeout(r, 1000))
  updateStep(3)

  try {
    const res = await fetch('/api/validation/collect-one', { method: 'POST', signal: AbortSignal.timeout(120000) })
    const data = await res.json()
    if (data.code === 200) {
      updateStep(7)
      await new Promise(r => setTimeout(r, 800))
      scrapProgress.value.visible = false
      lastResult.value = data.data.sample
      stats.value = data.data.stats
      sampleCount.value = data.data.stats.sampleCount || 0
      ElMessage.success('采集完成，共 ' + sampleCount.value + ' 条数据')
      await loadHistory()
      await nextTick()
      renderCharts()
    } else {
      scrapProgress.value.visible = false
      ElMessage.error(data.message || '采集失败')
    }
  } catch (e) {
    scrapProgress.value.visible = false
    ElMessage.error('采集失败: ' + e.message)
  } finally {
    collecting.value = false
  }
}

async function refreshStats() {
  loadingStats.value = true
  try {
    const res = await fetch('/api/validation/stats')
    const data = await res.json()
    if (data.code === 200) {
      stats.value = data.data
      sampleCount.value = data.data.sampleCount || 0
      await nextTick()
      renderCharts()
    }
  } catch (e) { ElMessage.error('刷新失败') }
  finally { loadingStats.value = false }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const res = await fetch('/api/validation/history')
    const data = await res.json()
    if (data.code === 200) { history.value = data.data || []; sampleCount.value = history.value.length }
  } catch (e) {}
  finally { loadingHistory.value = false }
}

async function clearAll() {
  try {
    await ElMessageBox.confirm('确定清空所有验证数据？', '确认')
    await fetch('/api/validation/clear-data', { method: 'DELETE' })
    stats.value = null; history.value = []; sampleCount.value = 0
    lastResult.value = null; validationResult.value = null
    if (scatterChart) { scatterChart.dispose(); scatterChart = null }
    if (histogramChart) { histogramChart.dispose(); histogramChart = null }
    ElMessage.success('已清空')
  } catch (e) { if (e !== 'cancel') ElMessage.error('清空失败') }
}

function renderCharts() {
  if (!stats.value) return
  if (scatterRef.value) {
    if (scatterChart) scatterChart.dispose()
    scatterChart = echarts.init(scatterRef.value)
    const sData = stats.value.scatterData || []
    scatterChart.setOption({
      tooltip: { trigger: 'item', formatter: p => 'Ci: ' + p.value[0] + '<br/>Di: ' + p.value[1] },
      grid: { left: 60, right: 20, top: 30, bottom: 40 },
      xAxis: { type: 'value', name: 'Ci (TestYourVocab)', nameLocation: 'center', nameGap: 30 },
      yAxis: { type: 'value', name: 'Di (本算法)', nameLocation: 'center', nameGap: 40 },
      series: [{ type: 'scatter', data: sData.map(p => [p.x, p.y]), itemStyle: { color: '#7C3AED' }, markLine: { data: [{ type: 'average' }] } }]
    })
  }
  if (histogramRef.value) {
    if (histogramChart) histogramChart.dispose()
    histogramChart = echarts.init(histogramRef.value)
    const hData = stats.value.histogramData || []
    histogramChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 60, right: 20, top: 30, bottom: 40 },
      xAxis: { type: 'category', data: hData.map(b => b.min.toFixed(0) + '~' + b.max.toFixed(0)) },
      yAxis: { type: 'value', name: '频数' },
      series: [{ type: 'bar', data: hData.map(b => b.count), itemStyle: { color: '#10B981', borderRadius: [4,4,0,0] } }]
    })
  }
}

async function runValidation() {
  if (!jsonData.value.trim()) { ElMessage.warning('请输入验证数据'); return }
  loading.value = true
  try {
    const res = await importValidationData(jsonData.value)
    if (res.code === 200) { validationResult.value = res.data; ElMessage.success('验证完成'); await nextTick(); renderChart() }
  } catch (e) { ElMessage.error('验证失败') }
  finally { loading.value = false }
}

function handleFileUpload(file) {
  const reader = new FileReader()
  reader.onload = (e) => { jsonData.value = e.target.result; ElMessage.success('文件已加载: ' + file.name) }
  reader.readAsText(file); return false
}

function renderChart() {
  if (!chartRef.value || !validationResult.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)
  const items = validationResult.value.items?.slice(0, 20) || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['标准值(Ci)', '算法值(Di)', '差值'] },
    grid: { left: 50, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: items.map((_, i) => '样本 ' + (i + 1)) },
    yAxis: [{ type: 'value', name: '词汇量' }, { type: 'value', name: '差值' }],
    series: [
      { name: '标准值(Ci)', type: 'bar', data: items.map(i => i.standardEstimate), itemStyle: { color: '#7C3AED', borderRadius: [4,4,0,0] } },
      { name: '算法值(Di)', type: 'bar', data: items.map(i => i.algorithmEstimate), itemStyle: { color: '#10B981', borderRadius: [4,4,0,0] } },
      { name: '差值', type: 'line', yAxisIndex: 1, data: items.map(i => i.diff), itemStyle: { color: '#EF4444' }, lineStyle: { type: 'dashed' } }
    ]
  })
}

onMounted(async () => {
  await loadHistory()
  await refreshStats()
})
</script>

<style scoped>
.validation-page { animation: popIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
</style>
