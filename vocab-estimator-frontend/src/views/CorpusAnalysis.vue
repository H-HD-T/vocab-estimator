<template>
  <div class="corpus-page">
    <div class="page-title">
      <span class="title-icon"><el-icon><DataAnalysis /></el-icon></span>
      语料分析
    </div>

    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
      <div class="clay-card" style="border-color: #C4B5FD;">
        <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD;">
          <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
            <el-icon style="vertical-align: middle; margin-right: 6px;"><EditPen /></el-icon> 语料输入
          </span>
        </div>
        <div style="padding: 16px;">
          <el-input v-model="corpusText" type="textarea" :rows="8" placeholder="在此粘贴英文文本, 例如:&#10;I enjoy reading books and learning new words every day." />
          <div style="margin-top: 12px; display: flex; gap: 10px; flex-wrap: wrap;">
            <el-upload action="#" accept=".txt" :show-file-list="false" :before-upload="handleFileUpload" style="display: inline-block;">
              <el-button><el-icon><FolderOpened /></el-icon> 上传TXT文件</el-button>
            </el-upload>
            <button class="clay-btn clay-btn-primary" @click="analyzeText" :disabled="analyzing">
              <el-icon v-if="analyzing" class="is-loading"><Loading /></el-icon>
              <el-icon v-else><Lightning /></el-icon>
              {{ analyzing ? '分析中...' : '分析词汇量' }}
            </button>
          </div>
        </div>
      </div>

      <div class="clay-card" style="border-color: #C4B5FD;">
        <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD;">
          <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
            <el-icon style="vertical-align: middle; margin-right: 6px;"><TrendCharts /></el-icon> 分析结果
          </span>
        </div>
        <div style="padding: 16px;">
          <div v-if="analysisResult">
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px;">
              <div class="clay-stat-box" style="border-color: #6EE7B7;">
                <div class="clay-stat-value" style="color: #10B981;">{{ analysisResult.estimate }}</div>
                <div class="clay-stat-label">估算词汇量</div>
              </div>
              <div class="clay-stat-box" style="border-color: #C4B5FD;">
                <div class="clay-stat-value" style="color: #7C3AED; font-size: 20px;">{{ analysisResult.minRange }} - {{ analysisResult.maxRange }}</div>
                <div class="clay-stat-label">估算范围</div>
              </div>
              <div class="clay-stat-box" style="border-color: #C4B5FD;">
                <div class="clay-stat-value" style="color: #F59E0B;">{{ (analysisResult.confidence || 0).toFixed(1) }}%</div>
                <div class="clay-stat-label">置信度</div>
              </div>
            </div>
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px;">
              <div style="background: #F1F5F9; border-radius: 10px; border: 2px solid #CBD5E1; padding: 12px; text-align: center;">
                <div style="font-family: 'Baloo 2', cursive; font-weight: 700; font-size: 20px; color: #7C3AED;">{{ analysisResult.totalWords }}</div>
                <div style="font-family: 'Comic Neue', sans-serif; font-size: 12px; color: #64748B;">总词数</div>
              </div>
              <div style="background: #F1F5F9; border-radius: 10px; border: 2px solid #CBD5E1; padding: 12px; text-align: center;">
                <div style="font-family: 'Baloo 2', cursive; font-weight: 700; font-size: 20px; color: #F59E0B;">{{ extractedWords.length }}</div>
                <div style="font-family: 'Comic Neue', sans-serif; font-size: 12px; color: #64748B;">不重复词数</div>
              </div>
              <div style="background: #F1F5F9; border-radius: 10px; border: 2px solid #CBD5E1; padding: 12px; text-align: center;">
                <div style="font-family: 'Baloo 2', cursive; font-weight: 700; font-size: 20px; color: #EC4899;">{{ analysisResult.validSamples }}</div>
                <div style="font-family: 'Comic Neue', sans-serif; font-size: 12px; color: #64748B;">有效样本</div>
              </div>
            </div>
          </div>
          <div v-else-if="!analyzing" style="text-align: center; padding: 30px; color: var(--clay-text-secondary);">
            <el-icon :size="40"><DataAnalysis /></el-icon>
            <p style="margin-top: 8px;">输入或上传英文文本后点击"分析词汇量"</p>
          </div>
          <div v-else style="text-align: center; padding: 30px; color: var(--clay-text-secondary);">
            <el-icon :size="40" class="is-loading"><Loading /></el-icon>
            <p style="margin-top: 8px;">分析中...</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="extractedWords.length > 0" class="clay-card" style="margin-top: 20px; border-color: #C4B5FD;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><Notebook /></el-icon> 提取的单词({{ extractedWords.length }}个)
        </span>
      </div>
      <div style="padding: 16px; max-height: 350px; overflow-y: auto;">
        <span class="clay-tag clay-tag-blue" style="margin: 3px; font-size: 13px;" v-for="(w, idx) in extractedWords" :key="idx">{{ w }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { importCorpusText, analyzeCorpus, analyzeAllCorpuses } from '../api/corpusApi'

const corpusText = ref('')
const analysisResult = ref(null)
const extractedWords = ref([])
const analyzing = ref(false)

function handleFileUpload(file) {
  const reader = new FileReader()
  reader.onload = (e) => { corpusText.value = e.target.result; ElMessage.success('文件已加载: ' + file.name) }
  reader.readAsText(file); return false
}

async function analyzeText() {
  if (!corpusText.value.trim()) { ElMessage.warning('请输入或上传英文文本'); return }
  analyzing.value = true; analysisResult.value = null; extractedWords.value = []
  try {
    const importRes = await importCorpusText('C', corpusText.value)
    if (importRes.code !== 200) { ElMessage.error('导入失败: ' + (importRes.message || '')); analyzing.value = false; return }
    const corpusId = importRes.data?.id
    if (importRes.data && importRes.data.extractedWords) {
      try { 
        const parsed = JSON.parse(importRes.data.extractedWords)
        extractedWords.value = parsed
      } catch (e) {}
    }
    if (!corpusId) { ElMessage.error('导入后未获取到语料ID'); analyzing.value = false; return }
    const analyzeRes = await analyzeCorpus(corpusId)
    if (analyzeRes.code === 200 && analyzeRes.data) {
      const r = analyzeRes.data
      analysisResult.value = {
        estimate: r.estimate?.estimate || 0,
        minRange: r.estimate?.minRange || 0,
        maxRange: r.estimate?.maxRange || 0,
        confidence: r.estimate?.confidence || 0,
        totalWords: r.totalWords || 0,
        validSamples: (r.estimate?.knownCount || 0) + (r.estimate?.unknownCount || 0)
      }
    } else {
      ElMessage.warning('分析没有返回结果')
    }
  } catch (e) { ElMessage.error('分析失败: ' + e.message) }
  finally { analyzing.value = false }
}
</script>

<style scoped>
.corpus-page { animation: popIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
</style>
