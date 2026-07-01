<template>
  <div class="wordlib-page">
    <div class="page-title">
      <span class="title-icon"><el-icon><Notebook /></el-icon></span>
      词汇库管理
    </div>

    <div class="clay-card" style="border-color: #C4B5FD;">
      <div style="padding: 16px 20px; border-bottom: 2.5px solid #C4B5FD; display: flex; justify-content: space-between; align-items: center;">
        <span style="font-family: 'Baloo 2', cursive; font-weight: 600; font-size: 17px;">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><Collection /></el-icon> 词汇库
        </span>
        <button class="clay-btn clay-btn-primary clay-btn-sm" @click="showAddDialog">
          <el-icon><Plus /></el-icon> 添加单词
        </button>
      </div>
      <div style="padding: 16px;">
        <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: center;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-family: 'Baloo 2', cursive; font-weight: 500; font-size: 14px; color: var(--clay-text-secondary);">关键词</span>
            <el-input v-model="searchForm.keyword" placeholder="单词/释义" clearable style="width: 180px;" />
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-family: 'Baloo 2', cursive; font-weight: 500; font-size: 14px; color: var(--clay-text-secondary);">难度</span>
            <el-select v-model="searchForm.difficulty" clearable placeholder="全部" style="width: 120px;">
              <el-option label="K (小学)" value="K" />
              <el-option label="P (初中)" value="P" />
              <el-option label="F (高中)" value="F" />
              <el-option label="C (大学)" value="C" />
            </el-select>
          </div>
          <button class="clay-btn clay-btn-primary clay-btn-sm" @click="search"><el-icon><Search /></el-icon> 搜索</button>
          <button class="clay-btn clay-btn-white clay-btn-sm" @click="resetSearch">重置</button>
        </div>

        <el-table :data="wordList" border stripe v-loading="loading">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="word" label="单词" width="140" />
          <el-table-column prop="difficulty" label="等级" width="80">
            <template #default="{ row }">
              <span class="clay-tag" :class="levelTagCls(row.difficulty)" style="font-size: 12px;">{{ row.difficulty }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="frequency" label="词频" width="100">
            <template #default="{ row }">
              <el-progress :percentage="Math.round((row.frequency || 0) * 100)" :width="50" type="circle" :stroke-width="6" color="#7C3AED" />
            </template>
          </el-table-column>
          <el-table-column prop="definition" label="释义" min-width="200" show-overflow-tooltip />
          <el-table-column prop="cetLabel" label="四六级标签" width="100">
            <template #default="{ row }">
              <span v-if="row.cetLabel !== 'NONE'" class="clay-tag" :class="row.cetLabel === 'CET4' ? 'clay-tag-blue' : row.cetLabel === 'CET6' ? 'clay-tag-purple' : 'clay-tag-green'" style="font-size: 12px;">{{ row.cetLabel }}</span>
              <span v-else style="color: #CBD5E1;">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div style="display: flex; gap: 6px;">
                <button class="clay-btn clay-btn-sm clay-btn-white" @click="editWord(row)"><el-icon><Edit /></el-icon> 编辑</button>
                <button class="clay-btn clay-btn-sm clay-btn-success" @click="deleteWordItem(row)"><el-icon><Delete /></el-icon> 删除</button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
          <el-pagination v-model:current-page="pageParams.page" v-model:page-size="pageParams.size" :total="total" layout="prev, pager, next, total" @change="loadWords" />
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑单词' : '添加单词'" width="500px" :close-on-click-modal="false">
      <el-form :model="wordForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="单词" prop="word">
          <el-input v-model="wordForm.word" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="wordForm.difficulty">
            <el-option label="K (小学)" value="K" />
            <el-option label="P (初中)" value="P" />
            <el-option label="F (高中)" value="F" />
            <el-option label="C (大学)" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item label="词频" prop="frequency">
          <el-slider v-model="wordForm.frequency" :min="0" :max="1" :step="0.01" style="width: 300px;" />
        </el-form-item>
        <el-form-item label="释义" prop="definition">
          <el-input v-model="wordForm.definition" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="四六级标签">
          <el-select v-model="wordForm.cetLabel">
            <el-option label="无" value="NONE" />
            <el-option label="四级" value="CET4" />
            <el-option label="六级" value="CET6" />
            <el-option label="都有" value="BOTH" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="clay-btn clay-btn-white" @click="dialogVisible = false">取消</button>
        <button class="clay-btn clay-btn-primary" @click="saveWord">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWordPage, addWord, updateWord, deleteWord } from '../api/wordApi'

const wordList = ref([]); const loading = ref(false); const total = ref(0)
const dialogVisible = ref(false); const isEdit = ref(false); const formRef = ref(null)
const searchForm = reactive({ keyword: '', difficulty: '' })
const pageParams = reactive({ page: 1, size: 20 })
const wordForm = reactive({ id: null, word: '', difficulty: 'K', frequency: 0.5, definition: '', cetLabel: 'NONE' })
const rules = { word: [{ required: true, message: '请输入单词', trigger: 'blur' }], difficulty: [{ required: true, message: '请选择难度等级', trigger: 'change' }] }

function levelTagCls(level) { const map = { K: 'clay-tag-gray', P: 'clay-tag-blue', F: 'clay-tag-amber', C: 'clay-tag-red' }; return map[level] || 'clay-tag-gray' }

async function loadWords() {
  loading.value = true
  try {
    const params = { page: pageParams.page, size: pageParams.size, ...(searchForm.keyword && { keyword: searchForm.keyword }), ...(searchForm.difficulty && { difficulty: searchForm.difficulty }) }
    const res = await getWordPage(params)
    if (res.code === 200) { wordList.value = res.data.records; total.value = res.data.total }
  } catch (e) { console.log('加载词汇表失败') }
  finally { loading.value = false }
}

function search() { pageParams.page = 1; loadWords() }
function resetSearch() { searchForm.keyword = ''; searchForm.difficulty = ''; pageParams.page = 1; loadWords() }
function showAddDialog() { isEdit.value = false; Object.assign(wordForm, { id: null, word: '', difficulty: 'K', frequency: 0.5, definition: '', cetLabel: 'NONE' }); dialogVisible.value = true }
function editWord(row) { isEdit.value = true; Object.assign(wordForm, row); dialogVisible.value = true }

async function saveWord() {
  try {
    if (isEdit.value) { await updateWord(wordForm); ElMessage.success('单词已更新') }
    else { await addWord(wordForm); ElMessage.success('单词已添加') }
    dialogVisible.value = false; await loadWords()
  } catch (e) { ElMessage.error('保存失败') }
}

function deleteWordItem(row) {
  ElMessageBox.confirm('删除单词 "' + row.word + '"?', '确认', { type: 'warning' })
    .then(async () => { await deleteWord(row.id); ElMessage.success('单词已删除'); await loadWords() })
    .catch(() => {})
}

onMounted(() => { loadWords() })
</script>

<style scoped>
.wordlib-page { animation: popIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
</style>
