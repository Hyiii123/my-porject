<template>
  <div class="customer-service-page">
    <div class="page-title">
      <div><h2>AI 客服管理</h2><p>维护第三方 Pixel AI 配置、知识库、常见问题和会话数据。</p></div>
    </div>

    <div class="stat-grid">
      <div class="stat-card blue"><span class="stat-label">累计会话</span><strong>{{ stats.totalSessions }}</strong><small>近期开启的客服会话</small></div>
      <div class="stat-card green"><span class="stat-label">AI 回复量</span><strong>{{ stats.aiMessages }}</strong><small>第三方 AI 或知识库回复</small></div>
      <div class="stat-card orange"><span class="stat-label">当前活跃会话</span><strong>{{ stats.activeSessions }}</strong><small>仍在提供 AI 服务的会话</small></div>
      <div class="stat-card purple"><span class="stat-label">满意度</span><strong>{{ stats.satisfactionRate }}<em>%</em></strong><small>用户评价综合得分</small></div>
    </div>

    <el-tabs v-model="activeTab" class="service-tabs">
      <el-tab-pane label="统计分析" name="stats">
        <div class="analysis-grid">
          <section class="panel trend-panel">
            <div class="panel-title"><h3>近 5 日服务趋势</h3><span>会话量 / AI 解决量</span></div>
            <div class="trend-chart">
              <div v-for="item in stats.trend" :key="item.date" class="trend-item">
                <div class="bars"><i class="bar total" :style="{ height: `${Math.max(item.sessions * 6, 12)}px` }"><b>{{ item.sessions }}</b></i><i class="bar resolved" :style="{ height: `${Math.max(item.resolved * 6, 10)}px` }"><b>{{ item.resolved }}</b></i></div>
                <span>{{ item.date }}</span>
              </div>
            </div>
            <div class="legend"><span><i class="dot blue-dot" />全部会话</span><span><i class="dot green-dot" />AI 解决</span></div>
          </section>
          <section class="panel question-panel">
            <div class="panel-title"><h3>高频咨询问题</h3><span>来自用户会话</span></div>
            <el-empty v-if="!stats.topQuestions.length" description="暂无问题数据" :image-size="60" />
            <div v-for="(item, index) in stats.topQuestions" :key="item.question" class="question-row"><span class="rank">{{ index + 1 }}</span><span class="question-text">{{ item.question }}</span><el-tag size="small">{{ item.count }} 次</el-tag></div>
          </section>
        </div>
        <div class="quick-metrics panel"><div><span>平均消息数</span><strong>{{ stats.averageMessages }}</strong></div><div><span>知识库条目</span><strong>{{ knowledgeState.total }}</strong></div><div><span>常见问题</span><strong>{{ faqState.total }}</strong></div><div><span>今日会话</span><strong>{{ stats.todaySessions }}</strong></div></div>
      </el-tab-pane>

      <el-tab-pane label="知识库" name="knowledge">
        <section class="panel table-panel">
          <div class="toolbar"><el-input v-model="knowledgeState.keyword" clearable placeholder="搜索问题、关键词或答案" class="search-input" @keyup.enter="loadKnowledge" /><el-select v-model="knowledgeState.status" clearable placeholder="状态" class="status-select" @change="loadKnowledge"><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select><el-button type="primary" :icon="Plus" @click="openKnowledge()">新增知识</el-button></div>
          <el-table v-loading="knowledgeState.loading" :data="knowledgeState.list" stripe>
            <el-table-column prop="question" label="问题" min-width="210" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="keywords" label="关键词" min-width="170" show-overflow-tooltip />
            <el-table-column prop="answer" label="答案" min-width="280" show-overflow-tooltip />
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="toggleKnowledge(row)" /></template></el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="165" />
            <el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openKnowledge(row)">编辑</el-button><el-button link type="danger" @click="removeKnowledge(row)">删除</el-button></template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="knowledgeState.page" :page-size="knowledgeState.pageSize" :total="knowledgeState.total" layout="total, prev, pager, next" @current-change="loadKnowledge" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="常见问题" name="faqs">
        <section class="panel table-panel">
          <div class="toolbar"><el-input v-model="faqState.keyword" clearable placeholder="搜索常见问题" class="search-input" @keyup.enter="loadFaqs" /><el-select v-model="faqState.enabled" clearable placeholder="状态" class="status-select" @change="loadFaqs"><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select><el-button type="primary" :icon="Plus" @click="openFaq()">新增问题</el-button></div>
          <el-table v-loading="faqState.loading" :data="faqState.list" stripe>
            <el-table-column prop="question" label="问题" min-width="240" />
            <el-table-column prop="category" label="分类" width="130" />
            <el-table-column prop="answer" label="快捷答案" min-width="300" show-overflow-tooltip />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" @change="toggleFaq(row)" /></template></el-table-column>
            <el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openFaq(row)">编辑</el-button><el-button link type="danger" @click="removeFaq(row)">删除</el-button></template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="faqState.page" :page-size="faqState.pageSize" :total="faqState.total" layout="total, prev, pager, next" @current-change="loadFaqs" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI 配置" name="ai-config">
        <section class="panel config-panel">
          <div class="panel-title"><h3>第三方 Pixel AI</h3><span>仅使用 api.ai-pixel.online，不配置官方 OpenAI API</span></div>
          <el-form :model="aiConfig" label-width="110px" class="config-form">
            <el-row :gutter="20">
              <el-col :span="12"><el-form-item label="API 地址"><el-input v-model="aiConfig.baseUrl" placeholder="https://api.ai-pixel.online" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="接口路径"><el-input v-model="aiConfig.endpointPath" placeholder="/v1/responses" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="模型"><el-input v-model="aiConfig.model" placeholder="填写第三方服务支持的模型名称" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="API Key"><el-input v-model="aiConfig.apiKey" type="password" show-password :placeholder="aiConfig.apiKeyConfigured ? '已配置，留空保持不变' : '请输入第三方 API Key'" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="启用服务"><el-switch v-model="aiConfig.enabled" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="超时(ms)"><el-input-number v-model="aiConfig.timeoutMs" :min="1000" :max="120000" :step="1000" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="重试次数"><el-input-number v-model="aiConfig.maxRetries" :min="0" :max="3" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="系统提示词"><el-input v-model="aiConfig.systemPrompt" type="textarea" :rows="4" placeholder="约束 AI 使用中文、准确回答学习平台问题" /></el-form-item></el-col>
            </el-row>
          </el-form>
          <div class="config-actions"><el-tag :type="aiConfig.apiKeyConfigured ? 'success' : 'warning'">{{ aiConfig.apiKeyConfigured ? 'API Key 已配置' : '尚未配置 API Key' }}</el-tag><el-button :loading="aiTesting" @click="handleTestAi">测试连通性</el-button><el-button type="primary" :loading="aiSaving" @click="handleSaveAiConfig">保存配置</el-button></div>
          <el-alert title="安全提示" type="info" :closable="false" show-icon>API Key 只在保存请求中传输，服务端放入 Redis，不会回显到页面或保存到 MySQL 明文。</el-alert>
        </section>
      </el-tab-pane>

      <el-tab-pane label="会话记录" name="sessions">
        <section class="panel table-panel">
          <div class="toolbar"><el-input v-model="sessionState.keyword" clearable placeholder="搜索会话编号、用户或内容" class="search-input" @keyup.enter="loadSessions" /><el-select v-model="sessionState.status" clearable placeholder="会话状态" class="status-select" @change="loadSessions"><el-option label="AI 服务中" :value="0" /><el-option label="已结束" :value="3" /></el-select><el-button :icon="Refresh" @click="loadSessions">刷新</el-button></div>
          <el-table v-loading="sessionState.loading" :data="sessionState.list" stripe>
            <el-table-column prop="sessionNo" label="会话编号" width="205" />
            <el-table-column prop="userName" label="用户" width="110" />
            <el-table-column prop="source" label="服务来源" width="110" />
            <el-table-column label="状态" width="115"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="lastMessage" label="最近消息" min-width="270" show-overflow-tooltip />
            <el-table-column label="评价" width="110"><template #default="{ row }"><el-rate v-if="row.satisfactionScore" v-model="row.satisfactionScore" disabled size="small" /><span v-else class="muted">未评价</span></template></el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="165" />
            <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openSession(row)">查看</el-button></template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="sessionState.page" :page-size="sessionState.pageSize" :total="sessionState.total" layout="total, prev, pager, next" @current-change="loadSessions" />
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="knowledgeDialog" :title="knowledgeForm.id ? '编辑知识' : '新增知识'" width="620px" destroy-on-close>
      <el-form :model="knowledgeForm" label-width="90px"><el-form-item label="问题" required><el-input v-model="knowledgeForm.question" placeholder="例如：课程支持退款吗？" /></el-form-item><el-form-item label="分类"><el-input v-model="knowledgeForm.category" placeholder="账号与登录 / 课程学习 / 订单售后" /></el-form-item><el-form-item label="关键词"><el-input v-model="knowledgeForm.keywords" placeholder="用逗号分隔，帮助 AI 匹配问题" /></el-form-item><el-form-item label="答案" required><el-input v-model="knowledgeForm.answer" type="textarea" :rows="5" placeholder="输入 AI 客服的标准回答" /></el-form-item><el-form-item label="状态"><el-switch v-model="knowledgeForm.status" :active-value="1" :inactive-value="0" /></el-form-item></el-form>
      <template #footer><el-button @click="knowledgeDialog = false">取消</el-button><el-button type="primary" @click="submitKnowledge">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="faqDialog" :title="faqForm.id ? '编辑常见问题' : '新增常见问题'" width="620px" destroy-on-close>
      <el-form :model="faqForm" label-width="90px"><el-form-item label="问题" required><el-input v-model="faqForm.question" /></el-form-item><el-form-item label="分类"><el-input v-model="faqForm.category" /></el-form-item><el-form-item label="快捷答案" required><el-input v-model="faqForm.answer" type="textarea" :rows="4" /></el-form-item><el-form-item label="排序"><el-input-number v-model="faqForm.sort" :min="1" :max="99" /></el-form-item><el-form-item label="状态"><el-switch v-model="faqForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item></el-form>
      <template #footer><el-button @click="faqDialog = false">取消</el-button><el-button type="primary" @click="submitFaq">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="sessionDrawer" title="会话详情" size="520px">
      <template v-if="selectedSession">
        <div class="session-summary"><div><span>会话编号</span><strong>{{ selectedSession.sessionNo }}</strong></div><div><span>用户</span><strong>{{ selectedSession.userName }}</strong></div><div><span>状态</span><el-tag :type="statusType(selectedSession.status)">{{ statusText(selectedSession.status) }}</el-tag></div></div>
        <div class="transcript"><div v-for="message in selectedSession.messages" :key="message.id" :class="['transcript-item', message.type]"><span class="sender">{{ message.senderName }} · {{ message.time }}</span><p>{{ message.content }}</p></div></div>
        <div v-if="selectedSession.evaluation" class="evaluation-result"><div><span>用户评分</span><el-rate v-model="selectedSession.evaluation.score" disabled /></div><p v-if="selectedSession.evaluation.comment">“{{ selectedSession.evaluation.comment }}”</p><el-tag v-for="tag in selectedSession.evaluation.tags" :key="tag" size="small">{{ tag }}</el-tag></div>
      </template>
      <template #footer v-if="selectedSession && Number(selectedSession.status) !== 3">
        <div class="drawer-actions">
          <el-button type="warning" @click="handleCloseSession">结束会话</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { closeSession, deleteKnowledge, deleteFaq, getAiConfig, getKnowledgePage, getFaqPage, getServiceStats, getSessionDetails, getSessionPage, saveAiConfig, testAi, saveKnowledge, saveFaq, updateKnowledge, updateFaq } from '@/api/customerService'

const activeTab = ref('stats')
const stats = ref({ totalSessions: 0, aiMessages: 0, activeSessions: 0, todaySessions: 0, satisfactionRate: 0, averageMessages: 0, topQuestions: [], trend: [] })
const knowledgeState = reactive({ list: [], total: 0, page: 1, pageSize: 8, keyword: '', status: '', loading: false })
const faqState = reactive({ list: [], total: 0, page: 1, pageSize: 8, keyword: '', enabled: '', loading: false })
const sessionState = reactive({ list: [], total: 0, page: 1, pageSize: 8, keyword: '', status: '', loading: false })
const knowledgeDialog = ref(false); const faqDialog = ref(false); const sessionDrawer = ref(false); const selectedSession = ref(null)
const knowledgeForm = reactive({ id: '', question: '', answer: '', keywords: '', category: '账号与登录', status: 1 })
const faqForm = reactive({ id: '', question: '', answer: '', category: '客服服务', sort: 1, enabled: 1 })
const aiConfig = reactive({ baseUrl: 'https://api.ai-pixel.online', endpointPath: '/v1/responses', model: 'gpt-5.5', apiKey: '', enabled: 0, timeoutMs: 30000, maxRetries: 1, systemPrompt: '', apiKeyConfigured: false })
const aiSaving = ref(false)
const aiTesting = ref(false)

// AjaxResult 使用 data，TableDataInfo 则直接使用 rows/total；两种都是若依标准返回结构。
function dataOf(response) {
  if (!response || response.code !== 200) return null
  return response.data !== undefined && response.data !== null ? response.data : response
}
async function loadStats() { const data = dataOf(await getServiceStats()); if (data) stats.value = data }
function pageOf(data, mapper = (item) => item) { return { list: (data?.rows || data?.list || []).map(mapper), total: Number(data?.total || 0) } }
function faqView(item) { return { ...item, sort: item.sortNum ?? item.sort ?? 1 } }
function sessionView(item) { return { ...item, status: Number(item.status), satisfactionScore: item.satisfactionScore || 0 } }
function messageView(item) { return { ...item, type: item.messageType === 1 ? 'user' : item.messageType === 2 ? 'agent' : 'system', time: item.createTime || '' } }
async function loadKnowledge() { knowledgeState.loading = true; try { const data = dataOf(await getKnowledgePage({ ...knowledgeState, pageNum: knowledgeState.page, pageSize: knowledgeState.pageSize })); if (data) Object.assign(knowledgeState, pageOf(data)) } finally { knowledgeState.loading = false } }
async function loadFaqs() { faqState.loading = true; try { const data = dataOf(await getFaqPage({ ...faqState, pageNum: faqState.page, pageSize: faqState.pageSize, enabled: faqState.enabled })); if (data) Object.assign(faqState, pageOf(data, faqView)) } finally { faqState.loading = false } }
async function loadSessions() { sessionState.loading = true; try { const data = dataOf(await getSessionPage({ ...sessionState, pageNum: sessionState.page, pageSize: sessionState.pageSize })); if (data) Object.assign(sessionState, pageOf(data, sessionView)) } finally { sessionState.loading = false } }
async function loadAiConfig() { const data = dataOf(await getAiConfig()); if (data) Object.assign(aiConfig, data, { apiKey: '' }) }
async function loadAll() { await Promise.all([loadStats(), loadKnowledge(), loadFaqs(), loadSessions(), loadAiConfig()]) }
function openKnowledge(row) { Object.assign(knowledgeForm, row || { id: '', question: '', answer: '', keywords: '', category: '账号与登录', status: 1 }); knowledgeDialog.value = true }
async function submitKnowledge() { if (!knowledgeForm.question || !knowledgeForm.answer) return ElMessage.warning('请填写问题和答案'); const response = knowledgeForm.id ? await updateKnowledge({ ...knowledgeForm }) : await saveKnowledge({ ...knowledgeForm }); if (response.code === 200) { ElMessage.success('知识保存成功'); knowledgeDialog.value = false; loadKnowledge(); loadStats() } }
async function toggleKnowledge(row) { const response = await updateKnowledge({ id: row.id, status: row.status }); if (response.code !== 200) { row.status = row.status ? 0 : 1; ElMessage.error('状态更新失败') } }
async function removeKnowledge(row) { if (!await confirmRemove('确定删除这条知识吗？')) return; const response = await deleteKnowledge(row.id); if (response.code === 200) { ElMessage.success('删除成功'); loadKnowledge() } }
function openFaq(row) { Object.assign(faqForm, row || { id: '', question: '', answer: '', category: '客服服务', sort: 1, enabled: 1 }); faqDialog.value = true }
async function submitFaq() { if (!faqForm.question || !faqForm.answer) return ElMessage.warning('请填写问题和答案'); const response = faqForm.id ? await updateFaq({ ...faqForm, sortNum: faqForm.sort }) : await saveFaq({ ...faqForm, sortNum: faqForm.sort }); if (response.code === 200) { ElMessage.success('常见问题保存成功'); faqDialog.value = false; loadFaqs(); loadStats() } }
async function toggleFaq(row) { const response = await updateFaq({ id: row.id, enabled: row.enabled }); if (response.code !== 200) { row.enabled = row.enabled ? 0 : 1; ElMessage.error('状态更新失败') } }
async function removeFaq(row) { if (!await confirmRemove('确定删除这条常见问题吗？')) return; const response = await deleteFaq(row.id); if (response.code === 200) { ElMessage.success('删除成功'); loadFaqs() } }
async function confirmRemove(message) { try { await ElMessageBox.confirm(message, '请确认', { type: 'warning' }); return true } catch (error) { return false } }
async function openSession(row) { const response = await getSessionDetails(row.id); const data = dataOf(response); selectedSession.value = data ? { ...sessionView(data), messages: (data.messages || []).map(messageView) } : row; sessionDrawer.value = true }
async function handleCloseSession() {
  if (!selectedSession.value || Number(selectedSession.value.status) === 3) return
  if (!await confirmRemove('结束后该会话将不能继续接收 AI 咨询，确定结束吗？')) return
  const response = await closeSession(selectedSession.value.id)
  const data = dataOf(response)
  if (data) {
    selectedSession.value = { ...selectedSession.value, ...sessionView(data) }
    ElMessage.success('会话已结束')
    await Promise.all([loadSessions(), loadStats()])
  }
}
function statusText(status) { return ({ 0: 'AI 服务中', 3: '已结束' }[Number(status)] || '未知') }
function statusType(status) { return ({ 0: '', 3: 'success' }[Number(status)] || 'info') }
async function handleSaveAiConfig() { aiSaving.value = true; try { const response = await saveAiConfig({ ...aiConfig }); const data = dataOf(response); if (data) { Object.assign(aiConfig, data, { apiKey: '' }); ElMessage.success('第三方 AI 配置已保存') } } finally { aiSaving.value = false } }
async function handleTestAi() { aiTesting.value = true; try { const response = await testAi({ message: '请用一句话介绍智问学伴。' }); const data = dataOf(response); if (data) ElMessage.success(`连通成功：${data}`) } finally { aiTesting.value = false } }
onMounted(loadAll)
</script>

<style lang="scss" scoped>
.customer-service-page { min-height: calc(100vh - 86px); padding: 24px 4px 40px; color: #303133; background: #f5f7fa; }.page-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 22px; }.page-title h2 { margin: 0 0 7px; color: #1f2d3d; font-size: 24px; }.page-title p { margin: 0; color: #909399; font-size: 13px; }.page-title :deep(.el-tag) { display: flex; align-items: center; gap: 5px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 22px; }.stat-card { position: relative; overflow: hidden; padding: 20px 22px; border-radius: 12px; color: #fff; box-shadow: 0 7px 20px rgba(30,60,90,.08); }.stat-card::after { position: absolute; right: -22px; bottom: -35px; width: 120px; height: 120px; border: 18px solid rgba(255,255,255,.12); border-radius: 50%; content: ''; }.stat-card.blue { background: linear-gradient(135deg,#409eff,#6a8cff); }.stat-card.green { background: linear-gradient(135deg,#67c23a,#3dbb8b); }.stat-card.orange { background: linear-gradient(135deg,#e6a23c,#f78989); }.stat-card.purple { background: linear-gradient(135deg,#9b8afb,#667eea); }.stat-label { display: block; margin-bottom: 10px; font-size: 13px; opacity: .9; }.stat-card strong { display: block; font-size: 30px; font-weight: 600; }.stat-card strong em { margin-left: 2px; font-size: 15px; font-style: normal; }.stat-card small { display: block; margin-top: 8px; font-size: 12px; opacity: .8; }
.service-tabs :deep(.el-tabs__header) { margin-bottom: 18px; }.panel { padding: 20px; background: #fff; border-radius: 10px; box-shadow: 0 2px 12px rgba(0,0,0,.04); }.analysis-grid { display: grid; grid-template-columns: 1.25fr 1fr; gap: 16px; }.panel-title { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 20px; }.panel-title h3 { margin: 0; font-size: 16px; }.panel-title span { color: #a0a6ad; font-size: 12px; }.trend-chart { display: flex; align-items: flex-end; justify-content: space-around; height: 210px; padding: 12px 20px 0; border-bottom: 1px solid #ebeef5; }.trend-item { display: flex; flex: 1; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }.bars { display: flex; align-items: flex-end; gap: 5px; height: 170px; }.bar { position: relative; display: block; width: 18px; min-height: 10px; border-radius: 5px 5px 0 0; }.bar b { position: absolute; top: -18px; left: 50%; color: #909399; font-size: 11px; font-weight: 400; transform: translateX(-50%); }.bar.total { background: #8fb8ff; }.bar.resolved { background: #67c23a; }.trend-item > span { margin-top: 10px; color: #909399; font-size: 12px; }.legend { display: flex; gap: 18px; margin-top: 14px; color: #909399; font-size: 12px; }.legend span { display: flex; align-items: center; gap: 5px; }.dot { width: 8px; height: 8px; border-radius: 50%; }.blue-dot { background: #8fb8ff; }.green-dot { background: #67c23a; }.question-row { display: flex; align-items: center; gap: 10px; padding: 13px 0; border-bottom: 1px solid #f3f4f6; }.question-row:last-child { border-bottom: 0; }.rank { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; color: #fff; font-size: 12px; background: #c0c4cc; border-radius: 50%; }.question-row:nth-child(3) .rank { background: #e6a23c; }.question-text { flex: 1; overflow: hidden; color: #606266; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }.quick-metrics { display: grid; grid-template-columns: repeat(4,1fr); gap: 1px; margin-top: 16px; padding: 0; overflow: hidden; }.quick-metrics div { padding: 18px 24px; border-right: 1px solid #ebeef5; }.quick-metrics div:last-child { border-right: 0; }.quick-metrics span { display: block; color: #909399; font-size: 12px; }.quick-metrics strong { display: block; margin-top: 9px; color: #303133; font-size: 24px; }
.table-panel { padding: 0 20px 20px; }.toolbar { display: flex; align-items: center; gap: 10px; padding: 20px 0; }.search-input { width: 300px; }.status-select { width: 130px; }.table-panel :deep(.el-table) { min-height: 330px; }.table-panel :deep(.el-pagination) { justify-content: flex-end; margin-top: 18px; }.muted { color: #c0c4cc; font-size: 12px; }
.config-panel { max-width: 980px; }.config-form { margin-top: 8px; }.config-actions { display: flex; align-items: center; gap: 10px; margin: 4px 0 18px; }.config-actions .el-tag { margin-right: auto; }.config-panel :deep(.el-alert) { line-height: 1.6; }
 .session-summary { display: grid; grid-template-columns: 1.5fr 1fr 1fr; gap: 10px; padding: 14px; background: #f5f7fa; border-radius: 8px; }.session-summary span { display: block; margin-bottom: 6px; color: #909399; font-size: 12px; }.session-summary strong { font-size: 13px; }.transcript { max-height: 480px; margin: 18px 0; overflow-y: auto; }.transcript-item { margin: 12px 0; padding: 10px 12px; border-radius: 8px; background: #f5f7fa; }.transcript-item.user { background: #ecf5ff; }.transcript-item.agent { background: #f0f9eb; }.sender { color: #909399; font-size: 11px; }.transcript-item p { margin: 6px 0 0; color: #303133; font-size: 13px; line-height: 1.6; white-space: pre-line; }.reply-box { display: flex; gap: 10px; align-items: flex-end; }.reply-box :deep(.el-textarea) { flex: 1; }.drawer-actions { display: flex; justify-content: flex-end; }.evaluation-result { margin-top: 18px; padding: 14px; background: #fdf6ec; border-radius: 8px; }.evaluation-result > div { display: flex; align-items: center; gap: 12px; font-size: 13px; }.evaluation-result p { color: #606266; font-size: 13px; }.evaluation-result .el-tag { margin: 0 6px 0 0; }
@media (max-width: 1200px) { .stat-grid { grid-template-columns: repeat(2,1fr); }.analysis-grid { grid-template-columns: 1fr; } } @media (max-width: 700px) { .stat-grid, .quick-metrics { grid-template-columns: 1fr; }.quick-metrics div { border-right: 0; border-bottom: 1px solid #ebeef5; }.toolbar { flex-wrap: wrap; }.search-input { width: 100%; }.page-title { align-items: flex-start; gap: 12px; flex-direction: column; } }
</style>
