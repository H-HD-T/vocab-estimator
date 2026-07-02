<template>
  <div class="stats-page">
    <div class="page-title">
      <span class="title-icon"><el-icon><TrendCharts /></el-icon></span>
      统计报表
    </div>

    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
      <div class="clay-card" style="border-color: #C4B5FD;">
        <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD;">
          <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
            <el-icon style="vertical-align: middle; margin-right: 6px;"><DataLine /></el-icon> 四六级成绩与词汇量相关性
          </span>
        </div>
        <div style="padding: 16px;">
          <div ref="scatterRef" style="height: 400px;"></div>
        </div>
      </div>

      <div class="clay-card" style="border-color: #C4B5FD;">
        <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD;">
          <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
            <el-icon style="vertical-align: middle; margin-right: 6px;"><Coin /></el-icon> 概率统计
          </span>
        </div>
        <div style="padding: 16px;">
          <div v-if="overview">
            <div style="display: grid; gap: 10px;">
              <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: #F1F5F9; border-radius: 10px; border: 2px solid #CBD5E1;">
                <span style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: #64748B;">总用户数</span>
                <span style="font-family: 'Baloo 2', cursive; font-weight: 700; font-size: 20px; color: #7C3AED;">{{ overview.userCount }}</span>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: #F1F5F9; border-radius: 10px; border: 2px solid #CBD5E1;">
                <span style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: #64748B;">总测试数</span>
                <span style="font-family: 'Baloo 2', cursive; font-weight: 700; font-size: 20px; color: #10B981;">{{ overview.testCount }}</span>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: #F1F5F9; border-radius: 10px; border: 2px solid #CBD5E1;">
                <span style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: #64748B;">平均词汇量</span>
                <span style="font-family: 'Baloo 2', cursive; font-weight: 700; font-size: 20px; color: #F59E0B;">{{ overview.avgVocab }}</span>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: #F1F5F9; border-radius: 10px; border: 2px solid #CBD5E1;">
                <span style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: #64748B;">平均置信度</span>
                <span style="font-family: 'Baloo 2', cursive; font-weight: 700; font-size: 20px; color: #F59E0B;">{{ overview.avgConfidence }}%</span>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: #F1F5F9; border-radius: 10px; border: 2px solid #CBD5E1;">
                <span style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: #64748B;">四级相关系数</span>
                <span class="clay-tag" :class="overview.cet4Correlation > 0.5 ? 'clay-tag-green' : 'clay-tag-amber'" style="font-size: 15px; padding: 6px 16px;">
                  {{ overview.cet4Correlation }}
                </span>
              </div>
            </div>
          </div>
          <div v-else style="text-align: center; padding: 40px; color: var(--clay-text-secondary);">
            <el-icon :size="40" class="is-loading"><Loading /></el-icon>
            <p style="margin-top: 8px;">加载统计中...</p>
          </div>
        </div>
      </div>
    </div>

    <div class="clay-card" style="margin-top: 20px; border-color: #C4B5FD;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD; display: flex; justify-content: space-between; align-items: center;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><List /></el-icon> 详细数据
        </span>
        <button class="clay-btn clay-btn-sm clay-btn-white" @click="exportReport">
          <el-icon><Download /></el-icon> 导出报表
        </button>
      </div>
      <div style="padding: 12px;">
        <el-table :data="correlationItems" border stripe size="small" max-height="400">
          <el-table-column prop="studentCode" label="学生" width="120" />
          <el-table-column prop="cet4Score" label="四级" width="80" />
          <el-table-column prop="cet6Score" label="六级" width="80" />
          <el-table-column prop="estimateVocab" label="估算词汇量" width="130" />
          <el-table-column prop="avgConfidence" label="平均置信度" width="130">
            <template #default="{ row }">{{ row.avgConfidence?.toFixed(1) }}%</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import * as XLSX from 'xlsx'
import { getCorrelationStats, getOverviewStats } from '../api/statsApi'

const scatterRef = ref(null)
const overview = ref(null)
const correlationItems = ref([])
let scatterChart = null

onMounted(async () => {
  try {
    const [corrRes, overviewRes] = await Promise.all([ getCorrelationStats(), getOverviewStats() ])
    if (overviewRes.code === 200) { overview.value = overviewRes.data }
    if (corrRes.code === 200) { correlationItems.value = corrRes.data.correlationItems || []; await nextTick(); renderScatter() }
  } catch (e) { console.log('Stats not available') }
})

function renderScatter() {
  if (!scatterRef.value || correlationItems.value.length === 0) return
  if (scatterChart) scatterChart.dispose()
  scatterChart = echarts.init(scatterRef.value)
  const hasCET4 = correlationItems.value.some(i => i.cet4Score != null)
  const hasCET6 = correlationItems.value.some(i => i.cet6Score != null)
  const series = []
  if (hasCET4) { series.push({ name: '四级', type: 'scatter', data: correlationItems.value.filter(i => i.cet4Score != null).map(i => [i.cet4Score, i.estimateVocab]), itemStyle: { color: '#7C3AED' } }) }
  if (hasCET6) { series.push({ name: '六级', type: 'scatter', data: correlationItems.value.filter(i => i.cet6Score != null).map(i => [i.cet6Score, i.estimateVocab]), itemStyle: { color: '#EC4899' } }) }
  scatterChart.setOption({
    tooltip: { trigger: 'item', formatter: p => p.seriesName + '<br/>分数: ' + p.value[0] + '<br/>词汇量: ' + p.value[1] },
    grid: { left: 50, right: 20, top: 40, bottom: 40 },
    xAxis: { type: 'value', name: '四六级分数', nameTextStyle: { fontFamily: 'Comic Neue' } },
    yAxis: { type: 'value', name: '估算词汇量', nameTextStyle: { fontFamily: 'Comic Neue' } },
    series, legend: { data: ['四级', '六级'] }
  })
}

function exportReport() {
  const data = correlationItems.value.map(i => ({ 学生: i.studentCode, CET4: i.cet4Score, CET6: i.cet6Score, VocabEstimate: i.estimateVocab, AvgConfidence: (i.avgConfidence?.toFixed(1) || '') + '%' }))
  const ws = XLSX.utils.json_to_sheet(data); const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '统计报表'); XLSX.writeFile(wb, 'vocab_stats_report.xlsx'); ElMessage.success('报表已导出')
}
</script>

<style scoped>
.stats-page { animation: popIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
</style>
