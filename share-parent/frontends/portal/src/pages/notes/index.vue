<template>
  <div class="notes-container">
    <div class="container">
      <h2 class="page-title">我的笔记</h2>

      <!-- 笔记列表 -->
      <div class="notes-content">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="全部笔记" name="all" />
          <el-tab-pane label="公开笔记" name="public" />
          <el-tab-pane label="私密笔记" name="private" />
        </el-tabs>

        <div v-if="filteredNotes.length > 0" class="notes-list">
          <div v-for="note in filteredNotes" :key="note.id" class="note-card">
            <div class="note-header">
              <div class="note-course">
                <el-icon><Reading /></el-icon>
                <span>{{ note.courseName }}</span>
              </div>
              <div class="note-meta">
                <el-tag :type="note.isPublic ? 'success' : 'info'" size="small">
                  {{ note.isPublic ? '公开' : '私密' }}
                </el-tag>
                <span class="note-time">{{ note.createTime }}</span>
              </div>
            </div>
            <div class="note-section">
              <el-icon><Document /></el-icon>
              <span>{{ note.sectionName }}</span>
            </div>
            <div class="note-content">{{ note.content }}</div>
            <div class="note-actions">
              <el-button type="primary" link @click="handleEdit(note)">编辑</el-button>
              <el-button :type="note.isPublic ? 'warning' : 'success'" link @click="handleTogglePublic(note)">
                {{ note.isPublic ? '设为私密' : '设为公开' }}
              </el-button>
              <el-button type="danger" link @click="handleDelete(note)">删除</el-button>
            </div>
          </div>
        </div>

        <div v-else class="empty-notes">
          <el-empty description="暂无笔记">
            <el-button type="primary" @click="$router.push('/search/index')">去学习</el-button>
          </el-empty>
        </div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑笔记" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="课程">
          <span>{{ editForm.courseName }}</span>
        </el-form-item>
        <el-form-item label="章节">
          <span>{{ editForm.sectionName }}</span>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.content" type="textarea" :rows="4" placeholder="请输入笔记内容" />
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="editForm.isPublic">
            <el-radio :label="true">公开</el-radio>
            <el-radio :label="false">私密</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Reading, Document } from '@element-plus/icons-vue'
import { getAllNotes, updateNotes, delNote } from '@/api/notes.js'

// Tab 切换
const activeTab = ref('all')

// 笔记数据
const notes = ref([])

const normalizeNote = (note = {}) => ({
  ...note,
  isPublic: note.isPublic ?? Number(note.visibility ?? 1) === 1,
  courseName: note.courseName || '课程笔记',
  sectionName: note.sectionName || note.title || '学习笔记',
  content: note.content || '',
  createTime: note.createTime || ''
})

// 筛选后的笔记
const filteredNotes = computed(() => {
  if (activeTab.value === 'public') {
    return notes.value.filter(n => n.isPublic)
  } else if (activeTab.value === 'private') {
    return notes.value.filter(n => !n.isPublic)
  }
  return notes.value
})

// Tab 切换
const handleTabChange = () => {
  // 触发计算属性更新
}

// 编辑弹窗
const editDialogVisible = ref(false)
const editForm = ref({
  id: null,
  courseName: '',
  sectionName: '',
  content: '',
  isPublic: true
})

// 编辑笔记
const handleEdit = (note) => {
  editForm.value = { ...note }
  editDialogVisible.value = true
}

// 保存编辑
const handleSaveEdit = async () => {
  if (!editForm.value.content?.trim()) {
    ElMessage.warning('请输入笔记内容')
    return
  }
  try {
    const response = await updateNotes({
      id: editForm.value.id,
      title: editForm.value.title || editForm.value.sectionName || '学习笔记',
      content: editForm.value.content,
      courseId: editForm.value.courseId,
      catalogId: editForm.value.catalogId || editForm.value.sectionId,
      visibility: editForm.value.isPublic ? 1 : 0
    })
    if (response?.code !== 200) throw new Error(response?.msg || '笔记更新失败')
    const index = notes.value.findIndex(n => n.id === editForm.value.id)
    if (index !== -1) notes.value[index] = normalizeNote({ ...notes.value[index], ...editForm.value, visibility: editForm.value.isPublic ? 1 : 0 })
    editDialogVisible.value = false
    ElMessage.success('笔记已更新')
  } catch (error) {
    ElMessage.error(error?.message || '笔记更新失败')
  }
}

// 切换公开/私密
const handleTogglePublic = async (note) => {
  const nextPublic = !note.isPublic
  try {
    const response = await updateNotes({ id: note.id, visibility: nextPublic ? 1 : 0, content: note.content, title: note.title || note.sectionName, courseId: note.courseId, catalogId: note.catalogId || note.sectionId })
    if (response?.code !== 200) throw new Error(response?.msg || '可见性更新失败')
    note.isPublic = nextPublic
    ElMessage.success(`笔记已设为${nextPublic ? '公开' : '私密'}`)
  } catch (error) {
    ElMessage.error(error?.message || '可见性更新失败')
  }
}

// 删除笔记
const handleDelete = (note) => {
  ElMessageBox.confirm('确定要删除这条笔记吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    const response = await delNote(note.id)
    if (response?.code !== 200) throw new Error(response?.msg || '笔记删除失败')
    notes.value = notes.value.filter(item => item.id !== note.id)
    ElMessage.success('笔记已删除')
  }).catch(error => {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '笔记删除失败')
  })
}

onMounted(async () => {
  try {
    const response = await getAllNotes({ pageNo: 1, pageSize: 100, onlyMine: true })
    if (response?.code !== 200) throw new Error(response?.msg || '笔记加载失败')
    const data = response.data || {}
    const rows = Array.isArray(data) ? data : (data.list || data.rows || [])
    notes.value = rows.map(normalizeNote)
  } catch (error) {
    notes.value = []
    ElMessage.error(error?.message || '笔记加载失败，请先登录')
  }
})
</script>

<style scoped>
.notes-container {
  background: #f5f7fa;
  min-height: 100vh;
  padding: 30px 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 24px;
}

.notes-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}

.note-card {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.2s;
}

.note-card:hover {
  background: #ecf5ff;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.note-course {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.note-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.note-time {
  font-size: 13px;
  color: #909399;
}

.note-section {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #409eff;
  margin-bottom: 12px;
}

.note-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
}

.note-actions {
  display: flex;
  gap: 12px;
}

.empty-notes {
  padding: 60px 0;
}
</style>
