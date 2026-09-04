<template>
  <div class="app-container customer-management">
    <div class="page-heading">
      <div>
        <h2>客服管理</h2>
        <p>维护客服知识内容，查看 AI 会话质量与第三方接口状态</p>
      </div>
      <el-button type="primary" plain @click="refreshAll" :loading="refreshing"><el-icon><Refresh /></el-icon>刷新数据</el-button>
    </div>

    <el-row :gutter="14" class="stat-row">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" :class="card.color"><el-icon><component :is="card.icon" /></el-icon></div>
          <div><div class="stat-label">{{ card.label }}</div><div class="stat-value">{{ statistics[card.key] ?? 0 }}</div></div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="management-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="会话记录" name="sessions">
          <el-form :inline="true" :model="sessionQuery" class="filter-form">
            <el-form-item label="关键词"><el-input v-model="sessionQuery.keyword" clearable placeholder="会话编号/用户/内容" @keyup.enter="loadSessions" /></el-form-item>
            <el-form-item label="状态">
              <el-select v-model="sessionQuery.status" clearable placeholder="全部状态" style="width: 130px">
                <el-option label="进行中" :value="0" /><el-option label="已结束" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="loadSessions">查询</el-button><el-button @click="resetSessionQuery">重置</el-button></el-form-item>
          </el-form>
          <el-table v-loading="loading.sessions" :data="sessions" stripe>
            <el-table-column prop="sessionNo" label="会话编号" min-width="185" />
            <el-table-column prop="userName" label="咨询用户" width="120" />
            <el-table-column label="来源" width="100"><template #default="{ row }"><el-tag size="small" type="primary">{{ row.source || 'AI客服' }}</el-tag></template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag size="small" :type="row.status === 3 ? 'info' : 'success'">{{ row.status === 3 ? '已结束' : '进行中' }}</el-tag></template></el-table-column>
            <el-table-column prop="lastMessage" label="最近消息" min-width="260" show-overflow-tooltip />
            <el-table-column prop="satisfactionScore" label="评分" width="80"><template #default="{ row }">{{ row.satisfactionScore ? row.satisfactionScore + ' 分' : '-' }}</template></el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="175" />
            <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }">
              <el-button link type="primary" @click="viewSession(row)" v-hasPermi="['customer:session:query']">详情</el-button>
              <el-button v-if="row.status !== 3" link type="danger" @click="closeSession(row)" v-hasPermi="['customer:session:close']">结束</el-button>
            </template></el-table-column>
          </el-table>
          <el-pagination v-if="sessionTotal > 0" v-model:current-page="sessionQuery.pageNum" v-model:page-size="sessionQuery.pageSize" layout="total, sizes, prev, pager, next" :total="sessionTotal" @current-change="loadSessions" @size-change="loadSessions" />
        </el-tab-pane>

        <el-tab-pane label="知识库" name="knowledge">
          <el-form :inline="true" :model="knowledgeQuery" class="filter-form">
            <el-form-item label="搜索"><el-input v-model="knowledgeQuery.keyword" clearable placeholder="请输入问题关键词" @keyup.enter="loadKnowledge" /></el-form-item>
            <el-form-item label="状态"><el-select v-model="knowledgeQuery.status" clearable placeholder="全部" style="width: 120px"><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select></el-form-item>
            <el-form-item><el-button type="primary" @click="loadKnowledge">查询</el-button><el-button @click="resetKnowledgeQuery">重置</el-button></el-form-item>
          </el-form>
          <div class="table-toolbar"><el-button type="primary" @click="openKnowledgeDialog()" v-hasPermi="['customer:knowledge:add']"><el-icon><Plus /></el-icon>新增知识</el-button><span>共 {{ knowledgeTotal }} 条知识</span></div>
          <el-table v-loading="loading.knowledge" :data="knowledgeList" stripe>
            <el-table-column prop="question" label="问题" min-width="280" show-overflow-tooltip /><el-table-column prop="answer" label="答案" min-width="360" show-overflow-tooltip /><el-table-column prop="category" label="分类" width="120" /><el-table-column prop="keywords" label="关键词" min-width="160" show-overflow-tooltip /><el-table-column prop="hitCount" label="命中" width="80" /><el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openKnowledgeDialog(row)" v-hasPermi="['customer:knowledge:edit']">编辑</el-button><el-button link type="danger" @click="removeKnowledge(row)" v-hasPermi="['customer:knowledge:remove']">删除</el-button></template></el-table-column>
          </el-table>
          <el-pagination v-if="knowledgeTotal > 0" v-model:current-page="knowledgeQuery.pageNum" v-model:page-size="knowledgeQuery.pageSize" layout="total, sizes, prev, pager, next" :total="knowledgeTotal" @current-change="loadKnowledge" @size-change="loadKnowledge" />
        </el-tab-pane>

        <el-tab-pane label="常见问题" name="faq">
          <el-form :inline="true" :model="faqQuery" class="filter-form"><el-form-item label="搜索"><el-input v-model="faqQuery.keyword" clearable placeholder="请输入问题关键词" @keyup.enter="loadFaq" /></el-form-item><el-form-item label="状态"><el-select v-model="faqQuery.enabled" clearable placeholder="全部" style="width: 120px"><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select></el-form-item><el-form-item><el-button type="primary" @click="loadFaq">查询</el-button><el-button @click="resetFaqQuery">重置</el-button></el-form-item></el-form>
          <div class="table-toolbar"><el-button type="primary" @click="openFaqDialog()" v-hasPermi="['customer:faq:add']"><el-icon><Plus /></el-icon>新增问题</el-button><span>共 {{ faqTotal }} 条常见问题</span></div>
          <el-table v-loading="loading.faq" :data="faqList" stripe><el-table-column prop="sortNum" label="排序" width="80" /><el-table-column prop="question" label="问题" min-width="280" show-overflow-tooltip /><el-table-column prop="answer" label="答案" min-width="380" show-overflow-tooltip /><el-table-column prop="category" label="分类" width="120" /><el-table-column prop="hitCount" label="命中" width="80" /><el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openFaqDialog(row)" v-hasPermi="['customer:faq:edit']">编辑</el-button><el-button link type="danger" @click="removeFaq(row)" v-hasPermi="['customer:faq:remove']">删除</el-button></template></el-table-column></el-table>
          <el-pagination v-if="faqTotal > 0" v-model:current-page="faqQuery.pageNum" v-model:page-size="faqQuery.pageSize" layout="total, sizes, prev, pager, next" :total="faqTotal" @current-change="loadFaq" @size-change="loadFaq" />
        </el-tab-pane>

        <el-tab-pane label="AI 配置" name="ai">
          <div class="ai-notice"><el-icon><Lock /></el-icon><span>只允许使用第三方 Pixel API：<code>https://api.ai-pixel.online</code>。API Key 仅发送到服务端并保存于 Redis，不会回显或写入 MySQL 明文。</span></div>
          <el-form ref="aiFormRef" :model="aiForm" :rules="aiRules" label-width="130px" class="ai-form">
            <el-form-item label="第三方 API 地址" prop="baseUrl"><el-input v-model="aiForm.baseUrl" /></el-form-item>
            <el-form-item label="接口路径" prop="endpointPath"><el-input v-model="aiForm.endpointPath" placeholder="/v1/responses，也可填写 /v1/chat/completions" /></el-form-item>
            <el-form-item label="模型名称" prop="model"><el-input v-model="aiForm.model" /></el-form-item>
            <el-form-item label="API Key"><el-input v-model="aiForm.apiKey" type="password" show-password placeholder="留空表示保留已配置的 Key" /><div class="form-tip">当前状态：<el-tag size="small" :type="aiForm.apiKeyConfigured ? 'success' : 'info'">{{ aiForm.apiKeyConfigured ? '已配置' : '未配置' }}</el-tag></div></el-form-item>
            <el-form-item label="启用 AI"><el-switch v-model="aiForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
            <el-form-item label="系统提示词" prop="systemPrompt"><el-input v-model="aiForm.systemPrompt" type="textarea" :rows="4" maxlength="4000" show-word-limit /></el-form-item>
            <el-form-item><el-button type="primary" :loading="savingAi" @click="saveAi" v-hasPermi="['customer:ai:edit']">保存配置</el-button><el-button :loading="testingAi" @click="testAi" v-hasPermi="['customer:ai:test']">测试连通性</el-button></el-form-item>
          </el-form>
          <div v-if="aiTestResult" class="ai-test-result"><span>最近测试结果：</span>{{ aiTestResult }}</div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="knowledgeDialog.visible" :title="knowledgeDialog.form.id ? '编辑知识' : '新增知识'" width="620px" append-to-body>
      <el-form ref="knowledgeFormRef" :model="knowledgeDialog.form" :rules="knowledgeRules" label-width="90px"><el-form-item label="问题" prop="question"><el-input v-model="knowledgeDialog.form.question" maxlength="500" /></el-form-item><el-form-item label="答案" prop="answer"><el-input v-model="knowledgeDialog.form.answer" type="textarea" :rows="5" /></el-form-item><el-form-item label="关键词"><el-input v-model="knowledgeDialog.form.keywords" placeholder="多个关键词用逗号分隔" maxlength="1000" /></el-form-item><el-form-item label="分类" prop="category"><el-input v-model="knowledgeDialog.form.category" maxlength="64" /></el-form-item><el-form-item label="状态"><el-switch v-model="knowledgeDialog.form.status" :active-value="1" :inactive-value="0" /></el-form-item></el-form>
      <template #footer><el-button @click="knowledgeDialog.visible = false">取消</el-button><el-button type="primary" :loading="savingKnowledge" @click="saveKnowledge">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="faqDialog.visible" :title="faqDialog.form.id ? '编辑常见问题' : '新增常见问题'" width="620px" append-to-body>
      <el-form ref="faqFormRef" :model="faqDialog.form" :rules="faqRules" label-width="90px"><el-form-item label="问题" prop="question"><el-input v-model="faqDialog.form.question" maxlength="500" /></el-form-item><el-form-item label="答案" prop="answer"><el-input v-model="faqDialog.form.answer" type="textarea" :rows="5" /></el-form-item><el-form-item label="分类" prop="category"><el-input v-model="faqDialog.form.category" maxlength="64" /></el-form-item><el-form-item label="排序"><el-input-number v-model="faqDialog.form.sortNum" :min="0" :max="9999" /></el-form-item><el-form-item label="状态"><el-switch v-model="faqDialog.form.enabled" :active-value="1" :inactive-value="0" /></el-form-item></el-form>
      <template #footer><el-button @click="faqDialog.visible = false">取消</el-button><el-button type="primary" :loading="savingFaq" @click="saveFaq">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="sessionDrawer.visible" title="会话详情" size="540px"><div v-if="sessionDrawer.session" class="session-detail"><div class="detail-summary"><div><span class="detail-label">会话编号</span>{{ sessionDrawer.session.sessionNo }}</div><div><span class="detail-label">用户</span>{{ sessionDrawer.session.userName }}</div><div><span class="detail-label">状态</span><el-tag size="small" :type="sessionDrawer.session.status === 3 ? 'info' : 'success'">{{ sessionDrawer.session.status === 3 ? '已结束' : '进行中' }}</el-tag></div></div><div class="drawer-messages"><div v-for="item in sessionDrawer.messages" :key="item.id" class="drawer-message" :class="{ user: item.messageType === 1 }"><div class="drawer-message-name">{{ item.senderName }}</div><div class="drawer-message-content">{{ item.content }}</div><div class="drawer-message-time">{{ item.createTime }}</div></div></div><div v-if="sessionDrawer.session.status !== 3" class="drawer-footer"><el-button type="danger" @click="closeSession(sessionDrawer.session)">结束本次会话</el-button></div></div></el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Collection, Document, Lock, Message, Plus, Refresh, Star, User } from '@element-plus/icons-vue'
import {
  addCustomerFaq,
  addCustomerKnowledge,
  closeCustomerSession,
  delCustomerFaq,
  delCustomerKnowledge,
  getCustomerAiConfig,
  getCustomerStatistics,
  listCustomerAdminMessages,
  listCustomerFaq,
  listCustomerKnowledge,
  listCustomerSessions,
  testCustomerAi,
  updateCustomerAiConfig,
  updateCustomerFaq,
  updateCustomerKnowledge
} from '@/api/customer'

const activeTab = ref('sessions')
const refreshing = ref(false)
const savingKnowledge = ref(false)
const savingFaq = ref(false)
const savingAi = ref(false)
const testingAi = ref(false)
const aiTestResult = ref('')
const knowledgeFormRef = ref(null)
const faqFormRef = ref(null)
const aiFormRef = ref(null)
const loading = reactive({ sessions: false, knowledge: false, faq: false })
const statistics = reactive({ totalSessions: 0, activeSessions: 0, todaySessions: 0, totalMessages: 0, averageScore: 0 })
const statCards = [
  { key: 'totalSessions', label: '累计会话', icon: ChatDotRound, color: 'blue' },
  { key: 'activeSessions', label: '进行中', icon: Message, color: 'green' },
  { key: 'todaySessions', label: '今日会话', icon: User, color: 'orange' },
  { key: 'averageScore', label: '平均评分', icon: Star, color: 'purple' }
]
const sessions = ref([])
const sessionTotal = ref(0)
const sessionQuery = reactive({ keyword: '', status: undefined, pageNum: 1, pageSize: 10 })
const knowledgeList = ref([])
const knowledgeTotal = ref(0)
const knowledgeQuery = reactive({ keyword: '', category: '', status: undefined, pageNum: 1, pageSize: 10 })
const faqList = ref([])
const faqTotal = ref(0)
const faqQuery = reactive({ keyword: '', category: '', enabled: undefined, pageNum: 1, pageSize: 10 })
const knowledgeDialog = reactive({ visible: false, form: emptyKnowledge() })
const faqDialog = reactive({ visible: false, form: emptyFaq() })
const aiForm = reactive({ baseUrl: 'https://api.ai-pixel.online', endpointPath: '/v1/responses', model: 'gpt-5.5', apiKey: '', apiKeyConfigured: false, enabled: 0, timeoutMs: 30000, maxRetries: 1, systemPrompt: '' })
const sessionDrawer = reactive({ visible: false, session: null, messages: [] })

const knowledgeRules = { question: [{ required: true, message: '请输入问题', trigger: 'blur' }], answer: [{ required: true, message: '请输入答案', trigger: 'blur' }], category: [{ required: true, message: '请输入分类', trigger: 'blur' }] }
const faqRules = knowledgeRules
const aiRules = { baseUrl: [{ required: true, message: '请输入第三方 API 地址', trigger: 'blur' }], endpointPath: [{ required: true, message: '请输入接口路径', trigger: 'blur' }], model: [{ required: true, message: '请输入模型名称', trigger: 'blur' }] }

onMounted(refreshAll)

function emptyKnowledge() { return { id: null, question: '', answer: '', keywords: '', category: '其他', status: 1 } }
function emptyFaq() { return { id: null, question: '', answer: '', category: '其他', sortNum: 0, enabled: 1 } }

async function refreshAll() {
  refreshing.value = true
  try {
    await Promise.all([loadStatistics(), loadSessions(), loadKnowledge(), loadFaq(), loadAiConfig()])
  } finally { refreshing.value = false }
}

async function loadStatistics() {
  const result = await getCustomerStatistics()
  Object.assign(statistics, result.data || {})
}

async function loadSessions() {
  loading.sessions = true
  try {
    const result = await listCustomerSessions({ ...sessionQuery })
    sessions.value = result.rows || []
    sessionTotal.value = result.total || 0
  } finally { loading.sessions = false }
}

async function loadKnowledge() {
  loading.knowledge = true
  try {
    const result = await listCustomerKnowledge({ ...knowledgeQuery })
    knowledgeList.value = result.rows || []
    knowledgeTotal.value = result.total || 0
  } finally { loading.knowledge = false }
}

async function loadFaq() {
  loading.faq = true
  try {
    const result = await listCustomerFaq({ ...faqQuery })
    faqList.value = result.rows || []
    faqTotal.value = result.total || 0
  } finally { loading.faq = false }
}

async function loadAiConfig() {
  const result = await getCustomerAiConfig()
  Object.assign(aiForm, result.data || {})
  aiForm.apiKey = ''
}

function handleTabChange(tab) {
  if (tab === 'sessions') loadSessions()
  if (tab === 'knowledge') loadKnowledge()
  if (tab === 'faq') loadFaq()
  if (tab === 'ai') loadAiConfig()
}

function resetSessionQuery() { Object.assign(sessionQuery, { keyword: '', status: undefined, pageNum: 1 }); loadSessions() }
function resetKnowledgeQuery() { Object.assign(knowledgeQuery, { keyword: '', category: '', status: undefined, pageNum: 1 }); loadKnowledge() }
function resetFaqQuery() { Object.assign(faqQuery, { keyword: '', category: '', enabled: undefined, pageNum: 1 }); loadFaq() }

function openKnowledgeDialog(row) { knowledgeDialog.form = row ? { ...row } : emptyKnowledge(); knowledgeDialog.visible = true }
async function saveKnowledge() {
  await knowledgeFormRef.value.validate()
  savingKnowledge.value = true
  try { const result = knowledgeDialog.form.id ? await updateCustomerKnowledge(knowledgeDialog.form) : await addCustomerKnowledge(knowledgeDialog.form); ElMessage.success(result.msg || '保存成功'); knowledgeDialog.visible = false; await loadKnowledge(); await loadStatistics() } finally { savingKnowledge.value = false }
}
async function removeKnowledge(row) {
  await ElMessageBox.confirm(`确定删除知识“${row.question}”吗？`, '提示', { type: 'warning' })
  await delCustomerKnowledge(row.id); ElMessage.success('删除成功'); await loadKnowledge(); await loadStatistics()
}

function openFaqDialog(row) { faqDialog.form = row ? { ...row } : emptyFaq(); faqDialog.visible = true }
async function saveFaq() {
  await faqFormRef.value.validate()
  savingFaq.value = true
  try { const result = faqDialog.form.id ? await updateCustomerFaq(faqDialog.form) : await addCustomerFaq(faqDialog.form); ElMessage.success(result.msg || '保存成功'); faqDialog.visible = false; await loadFaq(); await loadStatistics() } finally { savingFaq.value = false }
}
async function removeFaq(row) {
  await ElMessageBox.confirm(`确定删除问题“${row.question}”吗？`, '提示', { type: 'warning' })
  await delCustomerFaq(row.id); ElMessage.success('删除成功'); await loadFaq(); await loadStatistics()
}

async function viewSession(row) {
  sessionDrawer.session = row; sessionDrawer.messages = []; sessionDrawer.visible = true
  const result = await listCustomerAdminMessages(row.id); sessionDrawer.messages = result.data || []
}
async function closeSession(row) {
  await ElMessageBox.confirm('结束后该会话将不能继续产生新消息，确定结束吗？', '提示', { type: 'warning' })
  await closeCustomerSession(row.id); ElMessage.success('会话已结束'); sessionDrawer.visible = false; await Promise.all([loadSessions(), loadStatistics()])
}

async function saveAi() {
  await aiFormRef.value.validate()
  savingAi.value = true
  try { const result = await updateCustomerAiConfig({ ...aiForm }); Object.assign(aiForm, result.data || {}); aiForm.apiKey = ''; ElMessage.success('AI 配置已保存') } finally { savingAi.value = false }
}
async function testAi() {
  testingAi.value = true
  try { const result = await testCustomerAi({ message: '你好，请简要介绍一下你能提供哪些帮助？' }); aiTestResult.value = result.data || '测试成功'; ElMessage.success('第三方 AI 调用成功') } finally { testingAi.value = false }
}
</script>

<style scoped lang="scss">
.customer-management { background: #f6f8fc; min-height: calc(100vh - 84px); }
.page-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.page-heading h2 { margin: 0; color: #253553; font-size: 22px; }
.page-heading p { margin: 6px 0 0; color: #98a4b7; font-size: 13px; }
.stat-row { margin-bottom: 16px; }
.stat-card { display: flex; align-items: center; gap: 13px; padding: 18px; min-height: 86px; background: #fff; border: 1px solid #edf0f6; border-radius: 10px; }
.stat-icon { display: grid; place-items: center; width: 43px; height: 43px; border-radius: 12px; font-size: 21px; }.stat-icon.blue { color: #5f85f5; background: #eef3ff; }.stat-icon.green { color: #3ab782; background: #eaf9f2; }.stat-icon.orange { color: #ed9c3d; background: #fff5e7; }.stat-icon.purple { color: #9b74e8; background: #f5efff; }
.stat-label { color: #8b98ab; font-size: 12px; }.stat-value { margin-top: 6px; color: #283957; font-size: 22px; font-weight: 600; }
.management-card { border: 0; }.filter-form { margin-bottom: 2px; }.table-toolbar { display: flex; align-items: center; justify-content: space-between; margin: 3px 0 12px; color: #99a5b7; font-size: 12px; }
.el-pagination { justify-content: flex-end; margin-top: 16px; }.ai-notice { display: flex; gap: 8px; align-items: flex-start; max-width: 780px; padding: 12px 14px; margin: 10px 0 24px 20px; color: #67758b; background: #f5f8ff; border: 1px solid #dbe5ff; border-radius: 8px; line-height: 1.6; font-size: 13px; }.ai-notice .el-icon { flex: 0 0 auto; margin-top: 3px; color: #6687ee; }.ai-notice code { color: #4e73df; }.ai-form { max-width: 820px; padding: 0 20px; }.ai-form .el-input, .ai-form .el-textarea { max-width: 620px; }.form-tip { display: inline-block; margin-left: 10px; color: #9aa6b7; font-size: 12px; }.ai-test-result { max-width: 780px; padding: 14px 18px; margin: 20px; color: #52647e; background: #f7fbf9; border: 1px solid #d9f0e2; border-radius: 8px; line-height: 1.7; }.session-detail { height: 100%; display: flex; flex-direction: column; }.detail-summary { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 0 0 16px; color: #42536e; border-bottom: 1px solid #edf0f5; font-size: 13px; }.detail-label { display: block; margin-bottom: 4px; color: #9aa6b8; font-size: 11px; }.drawer-messages { flex: 1; overflow-y: auto; padding: 18px 0; }.drawer-message { padding: 10px 12px; margin-bottom: 12px; background: #f6f8fc; border-radius: 8px; }.drawer-message.user { background: #eef3ff; }.drawer-message-name { margin-bottom: 5px; color: #6983c9; font-size: 12px; }.drawer-message-content { color: #465773; line-height: 1.65; white-space: pre-wrap; }.drawer-message-time { margin-top: 5px; color: #a6b0bf; font-size: 11px; }.drawer-footer { padding-top: 12px; border-top: 1px solid #edf0f5; }
</style>
