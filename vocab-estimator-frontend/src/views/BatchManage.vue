<template>
  <div class="batch-page">
    <div class="page-title">
      <span class="title-icon"><el-icon><Upload /></el-icon></span>
      批处理
    </div>

    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
      <div class="clay-card" style="border-color: #6EE7B7;">
        <div style="padding: 16px 20px; border-bottom: 2.5px solid #6EE7B7;">
          <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
            <el-icon style="vertical-align: middle; margin-right: 6px;"><EditPen /></el-icon> 词表输入
          </span>
        </div>
        <div style="padding: 16px;">
          <el-input
            v-model="textInput"
            type="textarea"
            :rows="10"
            placeholder="每行一个单词，格式: 单词, 认识(或不认识)&#10;例如:&#10;apple, known&#10;philosophy, unknown&#10;abandon, recognized"
          />
          <div style="margin-top: 12px; display: flex; gap: 10px; flex-wrap: wrap;">
            <el-upload action="#" accept=".txt" :show-file-list="false" :before-upload="handleFileUpload">
              <el-button>
                <el-icon><FolderOpened /></el-icon> 上传TXT文件
              </el-button>
            </el-upload>
            <button class="clay-btn clay-btn-primary" @click="processBatch" :disabled="loading">
              <el-icon v-if="loading" class="is-loading"><Loading /></el-icon>
              <el-icon v-else><Lightning /></el-icon>
              {{ loading ? '计算中...' : '批量计算' }}
            </button>
          </div>
        </div>
      </div>

      <div class="clay-card" style="border-color: #C4B5FD;">
        <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD;">
          <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
            <el-icon style="vertical-align: middle; margin-right: 6px;"><DataAnalysis /></el-icon> 批量估算结果
          </span>
        </div>
        <div style="padding: 16px;">
          <div v-if="batchResult" style="text-align: center;">
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px;">
              <div class="clay-stat-box" style="border-color: #6EE7B7;">
                <div class="clay-stat-value" style="color: #10B981;">{{ batchResult.estimate }}</div>
                <div class="clay-stat-label">估算词汇量</div>
              </div>
              <div class="clay-stat-box" style="border-color: #C4B5FD;">
                <div class="clay-stat-value" style="color: #7C3AED; font-size: 20px;">{{ batchResult.minRange }} - {{ batchResult.maxRange }}</div>
                <div class="clay-stat-label">估算范围</div>
              </div>
              <div class="clay-stat-box" style="border-color: #C4B5FD;">
                <div class="clay-stat-value" style="color: #F59E0B;">{{ batchResult.confidence.toFixed(1) }}%</div>
                <div class="clay-stat-label">置信度</div>
              </div>
            </div>
            <span class="clay-tag clay-tag-green" style="margin-top: 10px;">
              认识: {{ batchResult.knownCount }} / {{ batchResult.knownCount + batchResult.unknownCount }} 词
            </span>
          </div>
          <div v-else-if="parsedWords.length > 0" style="padding: 40px 20px; text-align: center; color: var(--clay-text-secondary);">
            <el-icon :size="40"><DataAnalysis /></el-icon>
            <p style="margin-top: 8px;">请点击"批量计算"进行分析</p>
          </div>
          <div v-else style="padding: 40px 20px; text-align: center; color: var(--clay-text-secondary);">
            <el-icon :size="40"><Tickets /></el-icon>
            <p style="margin-top: 8px;">请在左侧输入词表后点击批量计算</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="parsedWords.length > 0" class="clay-card" style="margin-top: 20px; border-color: #C4B5FD;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD; display: flex; justify-content: space-between; align-items: center;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><Notebook /></el-icon> 单词明细({{ parsedWords.length }}词)
        </span>
        <button v-if="batchResult" class="clay-btn clay-btn-sm clay-btn-white" @click="exportExcel">
          <el-icon><Download /></el-icon> 导出Excel
        </button>
      </div>
      <div style="padding: 12px;">
        <el-table :data="parsedWords" max-height="350" border stripe size="small">
          <el-table-column prop="word" label="单词" width="140" />
          <el-table-column prop="known" label="标记" width="100">
            <template #default="{ row }">
              <span class="clay-tag" :class="row.known ? 'clay-tag-green' : 'clay-tag-red'" style="font-size: 12px; padding: 2px 12px;">{{ row.known ? '认识' : '不认识' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="80" />
          <el-table-column prop="definition" label="释义" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>
    </div>

    <div class="clay-card" style="margin-top: 20px; border-color: #F9A8D4;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #F9A8D4;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><Refresh /></el-icon> 采样测试(稳定性验证)
        </span>
      </div>
      <div style="padding: 16px;">
        <div style="display: flex; align-items: center; gap: 20px; flex-wrap: wrap;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-family: 'Baloo 2', cursive; font-weight: 500; font-size: 14px; color: var(--clay-text-secondary);">采样长度</span>
            <el-select v-model="samplingForm.sampleLength" style="width: 120px;">
              <el-option label="200 词" :value="200" />
              <el-option label="300 词" :value="300" />
              <el-option label="400 词" :value="400" />
            </el-select>
          </div>
          <div style="display: flex; align-items: center; gap: 8px; flex: 1; min-width: 200px;">
            <span style="font-family: 'Baloo 2', cursive; font-weight: 500; font-size: 14px; color: var(--clay-text-secondary); white-space: nowrap;">认识比例(%)</span>
            <el-slider v-model="samplingForm.knowRatio" :min="10" :max="90" style="flex: 1;" show-input />
          </div>
          <button class="clay-btn clay-btn-accent clay-btn-sm" @click="runSampling" :disabled="samplingLoading">
            <el-icon v-if="samplingLoading" class="is-loading"><Loading /></el-icon>
            <el-icon v-else><Lightning /></el-icon>
            运行900组采样
          </button>
        </div>
        <div v-if="samplingResult" style="margin-top: 12px;">
          <div class="clay-card" style="border-color: #6EE7B7; padding: 12px 16px; border-radius: var(--clay-radius-sm);">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-icon size="20" color="#10B981"><SuccessFilled /></el-icon>
              <span style="font-family: 'Comic Neue', sans-serif; font-size: 14px; color: var(--clay-text);">
                均值: {{ samplingResult.meanEstimate || '-' }} | 方差: {{ samplingResult.variance || '-' }} | 样本数: {{ samplingResult.sampleCount || '-' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadBatchText, uploadBatchFile, runSamplingTest } from '../api/batchApi'
import * as XLSX from 'xlsx'

const textInput = ref('')
const parsedWords = ref([])
const batchResult = ref(null)
const loading = ref(false)
const samplingLoading = ref(false)
const samplingResult = ref(null)
const samplingForm = reactive({ sampleLength: 200, knowRatio: 50 })

async function processBatch() {
  if (!textInput.value.trim()) { ElMessage.warning('请先输入词表'); return }
  loading.value = true; batchResult.value = null; parsedWords.value = []
  try {
    const res = await uploadBatchText({ textContent: textInput.value })
    if (res.code === 200) {
      const results = res.data.results || []
      const words = []; let totalKnown = 0, totalUnknown = 0, sumEstimate = 0, sumMin = 0, sumMax = 0, sumConfidence = 0, validCount = 0
      for (const r of results) {
        const parts = (r.wordLine || '').split(',')
        const word = parts[0]?.trim() || ''
        const known = (parts[1]?.trim() || '').toLowerCase() === 'known' || (parts[1]?.trim() || '').toLowerCase() === '认识'
        words.push({ word, known, difficulty: r.estimate?.difficulty || '', definition: r.estimate?.definition || '' })
        if (known) totalKnown++; else totalUnknown++
        if (r.estimate) { sumEstimate += r.estimate.estimate || 0; sumMin += r.estimate.minRange || 0; sumMax += r.estimate.maxRange || 0; sumConfidence += r.estimate.confidence || 0; validCount++ }
      }
      parsedWords.value = words
      if (validCount > 0) { batchResult.value = { estimate: Math.round(sumEstimate / validCount), minRange: Math.round(sumMin / validCount), maxRange: Math.round(sumMax / validCount), confidence: sumConfidence / validCount, knownCount: totalKnown, unknownCount: totalUnknown } }
      else { ElMessage.warning('无法解析词表') }
    }
  } catch (e) { ElMessage.error('批量计算失败') }
  finally { loading.value = false }
}

function handleFileUpload(file) {
  const reader = new FileReader()
  reader.onload = (e) => { textInput.value = e.target.result; ElMessage.success('文件已加载: ' + file.name) }
  reader.readAsText(file); return false
}

async function runSampling() {
  samplingLoading.value = true
  try { const res = await runSamplingTest({ sampleLength: samplingForm.sampleLength, knowRatio: samplingForm.knowRatio }); if (res.code === 200) { samplingResult.value = res.data; ElMessage.success('采样测试完成') } }
  catch (e) { ElMessage.error('采样测试失败') }
  finally { samplingLoading.value = false }
}

function exportExcel() {
  const data = parsedWords.value.map(r => ({ 单词: r.word, 标记: r.known ? '认识' : '不认识', 难度: r.difficulty || '', 释义: r.definition || '' }))
  const ws = XLSX.utils.json_to_sheet(data); const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '批量结果'); XLSX.writeFile(wb, 'batch_results.xlsx'); ElMessage.success('导出成功')
}
</script>

<style scoped>
.batch-page { animation: popIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
</style>
