<template>
  <main class="customer-service-page">
    <div class="service-container">
      <section class="service-hero">
        <div>
          <div class="hero-eyebrow"><el-icon><Service /></el-icon> 专属学习服务</div>
          <h1>客服中心</h1>
          <p>遇到课程、订单或账号问题？先问问小智，服务结束后还可以留下评价。</p>
        </div>
        <div class="hero-actions">
          <div class="hero-status">
            <span class="status-dot" :class="{ connected: pixelApiEnabled }" />
            <div>
            <strong>{{ aiStatusText }}</strong>
              <span>{{ aiStatusDescription }}</span>
            </div>
          </div>
          <el-button class="api-settings-button" @click="openPixelApiSettings">
            <el-icon><Setting /></el-icon>
            {{ pixelApiEnabled ? '接口设置' : '配置第三方 AI' }}
          </el-button>
        </div>
      </section>

      <section class="service-layout">
        <aside class="service-sidebar">
          <div class="sidebar-card service-intro-card">
            <div class="intro-icon"><el-icon :size="26"><Headset /></el-icon></div>
            <div>
              <h2>智问学伴客服</h2>
              <p>{{ statusText }}</p>
            </div>
            <span class="online-mark" />
            <div class="intro-divider" />
            <div class="service-feature">
              <el-icon><Clock /></el-icon>
              <span><strong>AI 智能客服</strong><small>全天候在线响应</small></span>
            </div>
            <div class="service-feature">
              <el-icon><CircleCheck /></el-icon>
              <span><strong>服务评价</strong><small>每次咨询都可反馈</small></span>
            </div>
          </div>

          <div class="sidebar-card faq-card">
            <div class="card-title-row">
              <div>
                <h3>常见问题</h3>
                <p>点击问题即可快速咨询</p>
              </div>
              <el-icon><QuestionFilled /></el-icon>
            </div>
            <div v-if="faqCategories.length" class="faq-categories">
              <button
                v-for="category in faqCategories"
                :key="category"
                type="button"
                :class="{ active: activeFaqCategory === category }"
                @click="activeFaqCategory = category"
              >
                {{ category }}
              </button>
            </div>
            <div v-if="filteredFaqs.length" class="faq-list">
              <button
                v-for="faq in filteredFaqs"
                :key="faq.id"
                type="button"
                class="faq-item"
                :disabled="isTyping || serviceStatus !== 'ai'"
                @click="sendQuickQuestion(faq.question)"
              >
                <span>{{ faq.question }}</span>
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
            <el-empty v-else :image-size="48" description="暂无常见问题" />
          </div>

          <div class="sidebar-card help-card">
            <div class="help-icon"><el-icon><Document /></el-icon></div>
            <div>
              <h3>服务小贴士</h3>
              <p>描述问题时提供课程名称或订单信息，能帮助我们更快定位问题。</p>
            </div>
          </div>
        </aside>

        <section class="chat-panel">
          <div class="chat-panel-header">
            <div class="chat-title">
              <div class="chat-avatar"><el-icon :size="24"><ChatDotRound /></el-icon></div>
              <div>
                <h2>在线咨询</h2>
                <span><i class="mini-status-dot" /> {{ statusText }}</span>
              </div>
            </div>
            <el-button class="new-session-button" text @click="resetSession">
              <el-icon><Refresh /></el-icon>
              新会话
            </el-button>
          </div>

          <div ref="messagesRef" class="chat-messages">
            <div class="conversation-date">今天</div>
            <div
              v-for="message in messages"
              :key="message.id"
              :class="['message', message.type === 'user' ? 'user-message' : 'service-message']"
            >
              <div v-if="message.type !== 'user'" class="message-avatar service-avatar">
                <el-icon :size="17"><Service /></el-icon>
              </div>
              <div class="message-content">
                <div class="message-meta">
                  <span>{{ message.type === 'user' ? '我' : (message.senderName || 'AI客服') }}</span>
                  <time>{{ formatMessageTime(message.time) }}</time>
                </div>
                <div class="message-bubble">{{ message.content }}</div>
              </div>
              <div v-if="message.type === 'user'" class="message-avatar user-avatar">
                <el-icon :size="17"><User /></el-icon>
              </div>
            </div>
            <div v-if="isTyping" class="message service-message">
              <div class="message-avatar service-avatar"><el-icon :size="17"><Service /></el-icon></div>
              <div class="message-content">
                <div class="message-meta"><span>AI客服</span></div>
                <div class="message-bubble typing-bubble"><i /><i /><i /></div>
              </div>
            </div>
          </div>

          <div v-if="serviceStatus === 'ai' && !isTyping && faqList.length" class="quick-questions">
            <div class="quick-title"><el-icon><Promotion /></el-icon> 你可能想问</div>
            <div class="quick-list">
              <button v-for="faq in faqList.slice(0, 4)" :key="faq.id" type="button" @click="sendQuickQuestion(faq.question)">
                {{ faq.question }}
              </button>
            </div>
          </div>

          <div class="service-actions">
            <div class="action-state">
              <el-icon v-if="serviceStatus === 'closed'" class="closed-icon"><CircleCheck /></el-icon>
              <span v-if="serviceStatus === 'ai'">AI 客服可以即时为您解答</span>
              <span v-else>本次服务已结束，感谢您的反馈</span>
            </div>
            <div class="action-buttons">
              <el-button v-if="messages.length > 1 && serviceStatus !== 'closed'" type="primary" @click="showEvaluation = true">
                结束并评价
              </el-button>
              <el-button v-if="serviceStatus === 'closed'" type="primary" @click="resetSession">开始新会话</el-button>
            </div>
          </div>

          <div v-if="showEvaluation" class="evaluation-panel">
            <div class="evaluation-heading">
              <div>
                <h3>为本次服务评价</h3>
                <p>您的反馈会帮助我们持续改进服务</p>
              </div>
              <el-button text @click="showEvaluation = false">稍后评价</el-button>
            </div>
            <div class="evaluation-content">
              <el-rate v-model="score" :texts="['很不满意', '不满意', '一般', '满意', '非常满意']" show-text />
              <el-checkbox-group v-model="selectedTags" class="evaluation-tags">
                <el-checkbox-button v-for="tag in evaluationTags" :key="tag" :label="tag">{{ tag }}</el-checkbox-button>
              </el-checkbox-group>
              <el-input v-model="comment" type="textarea" :rows="2" maxlength="100" show-word-limit placeholder="欢迎留下宝贵意见（选填）" />
              <el-button type="primary" :disabled="!score" @click="submitEvaluation">提交评价</el-button>
            </div>
          </div>

          <div class="chat-input-area">
            <el-input
              v-model="inputMessage"
              class="message-input"
              :disabled="isTyping || serviceStatus !== 'ai'"
              :placeholder="serviceStatus === 'closed' ? '本次服务已结束，请开启新会话' : '请输入您想咨询的问题...'
              "
              @keyup.enter="sendMessage"
            >
              <template #append>
                <el-button type="primary" :disabled="!inputMessage.trim() || isTyping || serviceStatus !== 'ai'" @click="sendMessage">发送</el-button>
              </template>
            </el-input>
            <span class="input-tip">按 Enter 发送</span>
          </div>
        </section>
      </section>

      <el-dialog v-model="showAiSettings" title="第三方 AI 接口设置" width="520px">
        <div class="api-settings-content">
          <p class="api-settings-description">输入第三方 AI API Key 后，客服会优先使用 Pixel API 回答问题。</p>
          <el-input
            v-model="apiKeyDraft"
            type="password"
            show-password
            clearable
            autocomplete="off"
            placeholder="请输入第三方 AI API Key"
          />
          <el-input v-model="modelDraft" class="model-input" placeholder="模型名称，例如 gpt-5.5">
            <template #prepend>模型</template>
          </el-input>
          <div class="api-security-tip">
            <el-icon><Lock /></el-icon>
            <span>Key 保存在当前浏览器本地，仅随本次咨询发送到本站客服服务，不写入数据库；客服服务只允许代理 Pixel 第三方地址。</span>
          </div>
        </div>
        <template #footer>
          <el-button @click="clearAiSettings">清除 Key</el-button>
          <el-button @click="showAiSettings = false">取消</el-button>
          <el-button type="primary" @click="saveAiSettings">保存并启用</el-button>
        </template>
      </el-dialog>
    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ArrowRight, ChatDotRound, CircleCheck, Clock, Document, Headset, Lock, Promotion, QuestionFilled, Refresh, Service, Setting, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { clearPixelApiKey, getPixelApiKey, getPixelModel, setPixelApiKey, setPixelModel } from '@/api/pixelApi'
import { createServiceSession, evaluateService, getServiceFaqs, getServiceSession, sendServiceMessage } from '@/api/customerService'

const SESSION_KEY = 'customer_service_session_id'
const inputMessage = ref('')
const isTyping = ref(false)
const messagesRef = ref(null)
const sessionId = ref(sessionStorage.getItem(SESSION_KEY) || '')
const serviceStatus = ref('ai')
const faqList = ref([])
const activeFaqCategory = ref('全部')
const showEvaluation = ref(false)
const score = ref(0)
const comment = ref('')
const selectedTags = ref([])
const evaluationTags = ['回答准确', '响应及时', '表达清晰', '问题未解决']
const messages = ref([welcomeMessage()])
const pixelApiKey = ref(getPixelApiKey())
const apiKeyDraft = ref(pixelApiKey.value)
const pixelModel = ref(getPixelModel())
const modelDraft = ref(pixelModel.value)
const showAiSettings = ref(false)

const pixelApiEnabled = computed(() => Boolean(pixelApiKey.value.trim()))
const aiStatusText = computed(() => pixelApiEnabled.value ? '第三方 AI 已配置' : 'AI 客服在线')
const aiStatusDescription = computed(() => pixelApiEnabled.value ? '当前对话将由 Pixel API 回答' : '配置第三方 API Key 后可启用 AI')

const statusText = computed(() => ({
  ai: 'AI客服在线',
  closed: '服务已结束',
}[serviceStatus.value] || 'AI客服在线'))

const faqCategories = computed(() => ['全部', ...new Set(faqList.value.map((faq) => faq.category).filter(Boolean))])
const filteredFaqs = computed(() => {
  if (activeFaqCategory.value === '全部') return faqList.value.slice(0, 8)
  return faqList.value.filter((faq) => faq.category === activeFaqCategory.value).slice(0, 8)
})

function openPixelApiSettings() {
  apiKeyDraft.value = pixelApiKey.value
  modelDraft.value = pixelModel.value
  showAiSettings.value = true
}

function saveAiSettings() {
  const key = apiKeyDraft.value.trim()
  if (!key) {
    ElMessage.warning('请输入第三方 AI API Key')
    return
  }
  setPixelApiKey(key)
  setPixelModel(modelDraft.value)
  pixelApiKey.value = key
  pixelModel.value = modelDraft.value.trim() || 'gpt-5.5'
  showAiSettings.value = false
  ElMessage.success('第三方 AI 接口已启用')
}

function clearAiSettings() {
  clearPixelApiKey()
  pixelApiKey.value = ''
  apiKeyDraft.value = ''
  showAiSettings.value = false
  ElMessage.info('已清除第三方 API Key，将继续使用客服知识库')
}

function welcomeMessage() {
  return {
    id: `welcome-${Date.now()}`,
    type: 'ai',
    senderName: 'AI客服',
    content: '您好！我是智问学伴的智能客服小智 🤖\n课程学习、订单支付、账号登录等问题都可以咨询我。',
    time: getTime(),
  }
}

function getTime() {
  const date = new Date()
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatMessageTime(value) {
  const text = String(value || '')
  const matched = text.match(/(\d{2}:\d{2})(?::\d{2})?$/)
  return matched ? matched[1] : text
}

function userInfo() {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || '{}')
  } catch (error) {
    return {}
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

function addLocalMessage(type, content, senderName) {
  messages.value.push({
    id: `${type}-${Date.now()}-${Math.random()}`,
    type,
    senderName: senderName || (type === 'user' ? '我' : 'AI客服'),
    content,
    time: getTime(),
  })
  scrollToBottom()
}

function normalizeStatus(value) {
  return Number(value) === 3 || value === 'closed' ? 'closed' : 'ai'
}

function normalizeMessage(message) {
  if (!message) return null
  const messageType = Number(message.messageType)
  return {
    id: message.id || `message-${Date.now()}-${Math.random()}`,
    type: message.type || (messageType === 1 ? 'user' : 'ai'),
    senderName: message.senderName || (messageType === 1 ? '我' : 'AI客服'),
    content: message.content || '',
    isFallback: Number(message.isFallback) === 1,
    time: message.time || message.createTime || getTime(),
  }
}

function applySession(session) {
  if (!session) return
  sessionId.value = String(session.id || session.sessionId || '')
  if (sessionId.value) sessionStorage.setItem(SESSION_KEY, sessionId.value)
  serviceStatus.value = normalizeStatus(session.status)
  const serverMessages = (session.messages || []).map(normalizeMessage).filter(Boolean)
  if (serverMessages.length) messages.value = serverMessages
  if (session.evaluation) {
    score.value = session.evaluation.score || 0
    selectedTags.value = session.evaluation.tags || []
    comment.value = session.evaluation.comment || ''
  }
}

async function ensureSession(user) {
  if (sessionId.value) return sessionId.value
  const response = await createServiceSession({ userName: user.nickname || user.username || '访客用户' })
  if (response.code !== 200 || !response.data?.id) throw new Error(response.msg || '客服会话创建失败')
  applySession(response.data)
  return sessionId.value
}

async function sendMessage() {
  const question = inputMessage.value.trim()
  if (!question || isTyping.value || serviceStatus.value !== 'ai') return

  const user = userInfo()
  addLocalMessage('user', question, user.nickname || user.username || '我')
  inputMessage.value = ''
  isTyping.value = true
  try {
    const currentSessionId = await ensureSession(user)
    const response = await sendServiceMessage(currentSessionId, {
      content: question,
      // Key 只在本次对话请求中传给本站客服服务，不会写入 Redis/MySQL。
      apiKey: pixelApiEnabled.value ? pixelApiKey.value : undefined,
      model: pixelApiEnabled.value ? pixelModel.value : undefined,
    })
    if (response.code !== 200 || !response.data) throw new Error(response.msg || response.message || '客服暂时不可用')
    applySession(response.data.session)
    const reply = normalizeMessage(response.data.message)
    const sessionMessages = response.data.session?.messages || []
    const replyAlreadyIncluded = reply && sessionMessages.some((message) => String(message.id) === String(reply.id))
    if (reply && !replyAlreadyIncluded) {
      const content = reply.isFallback && pixelApiEnabled.value
        ? `第三方 AI 暂时不可用，已切换知识库回答：\n${reply.content}`
        : reply.content
      addLocalMessage(reply.type, content, reply.senderName)
    }
  } catch (error) {
    const rawError = String(error.message || '')
    const isInvalidApiKey = /invalid api key|incorrect api key|无效.*key|401|403/i.test(rawError)
    const errorMessage = isInvalidApiKey
      ? '第三方 API Key 无效，请在 api.ai-pixel.online 创建并复制该平台的 API Key，不能使用官方 OpenAI Key。'
      : pixelApiEnabled.value
        ? `第三方 AI 接口调用失败：${rawError || '请点击“接口设置”检查 API Key、模型和网络连接。'}`
        : '抱歉，客服服务暂时不可用，请稍后再试。'
    addLocalMessage('ai', errorMessage)
  } finally {
    isTyping.value = false
  }
}

function sendQuickQuestion(question) {
  if (serviceStatus.value !== 'ai' || isTyping.value) return
  inputMessage.value = question
  sendMessage()
}

async function submitEvaluation() {
  if (!sessionId.value || !score.value) return
  try {
    const response = await evaluateService(sessionId.value, {
      score: score.value,
      tags: selectedTags.value,
      comment: comment.value,
    })
    if (response.code !== 200) throw new Error(response.message)
    serviceStatus.value = 'closed'
    showEvaluation.value = false
    ElMessage.success('感谢您的评价')
  } catch (error) {
    ElMessage.error('评价提交失败，请稍后重试')
  }
}

function resetSession() {
  sessionId.value = ''
  sessionStorage.removeItem(SESSION_KEY)
  serviceStatus.value = 'ai'
  showEvaluation.value = false
  score.value = 0
  comment.value = ''
  selectedTags.value = []
  messages.value = [welcomeMessage()]
  scrollToBottom()
}

onMounted(async () => {
  try {
    const faqResponse = await getServiceFaqs({ enabled: 1 })
    if (faqResponse.code === 200) faqList.value = faqResponse.data || []

    if (sessionId.value) {
      const sessionResponse = await getServiceSession(sessionId.value)
      if (sessionResponse.code === 200 && sessionResponse.data) {
        applySession(sessionResponse.data)
      }
    }
  } catch (error) {
    // 接口失败时仍保留页面的基础问候语和输入能力。
  }
  scrollToBottom()
})
</script>

<style scoped>
.customer-service-page {
  min-height: calc(100vh - 70px);
  padding: 32px 0 48px;
  background: #f5f7fb;
}

.service-container {
  width: 1200px;
  margin: 0 auto;
}

.service-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 26px;
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  color: #667eea;
  font-size: 13px;
  font-weight: 600;
}

.service-hero h1 {
  color: #202943;
  font-size: 32px;
  line-height: 1.25;
}

.service-hero p {
  margin-top: 8px;
  color: #7c849b;
  font-size: 14px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-status {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  color: #303b58;
  background: #fff;
  border: 1px solid #edf0f6;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(45, 58, 93, 0.04);
}

.hero-status strong,
.hero-status span {
  display: block;
}

.hero-status strong {
  font-size: 14px;
}

.hero-status div span {
  margin-top: 3px;
  color: #99a1b4;
  font-size: 12px;
}

.status-dot,
.mini-status-dot,
.online-mark {
  display: inline-block;
  border-radius: 50%;
  background: #67c23a;
  box-shadow: 0 0 0 4px rgba(103, 194, 58, 0.12);
}

.status-dot {
  width: 9px;
  height: 9px;
}

.status-dot.connected {
  background: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.12);
}

.api-settings-button {
  height: 48px;
  color: #667eea;
  background: #fff;
  border-color: #e4e7ff;
}

.api-settings-button:hover {
  color: #5969d9;
  background: #f6f7ff;
  border-color: #cfd4ff;
}

.service-layout {
  display: grid;
  grid-template-columns: 316px minmax(0, 1fr);
  align-items: start;
  gap: 20px;
}

.service-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  padding: 20px;
  background: #fff;
  border: 1px solid #edf0f6;
  border-radius: 14px;
  box-shadow: 0 5px 20px rgba(45, 58, 93, 0.04);
}

.service-intro-card {
  position: relative;
  display: grid;
  grid-template-columns: 54px 1fr 8px;
  align-items: center;
  gap: 12px;
}

.intro-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  color: #fff;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 16px;
}

.service-intro-card h2 {
  color: #27304a;
  font-size: 16px;
}

.service-intro-card p {
  margin-top: 6px;
  color: #67c23a;
  font-size: 12px;
}

.online-mark {
  width: 8px;
  height: 8px;
}

.intro-divider {
  grid-column: 1 / -1;
  width: 100%;
  height: 1px;
  margin: 4px 0 2px;
  background: #f0f2f7;
}

.service-feature {
  display: flex;
  grid-column: 1 / -1;
  align-items: flex-start;
  gap: 12px;
  color: #8b94aa;
}

.service-feature .el-icon {
  margin-top: 2px;
  color: #667eea;
}

.service-feature span {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.service-feature strong {
  color: #3d4761;
  font-size: 13px;
  font-weight: 500;
}

.service-feature small {
  color: #9ba3b5;
  font-size: 12px;
}

.card-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.card-title-row h3,
.help-card h3 {
  color: #27304a;
  font-size: 16px;
}

.card-title-row p {
  margin-top: 5px;
  color: #9ba3b5;
  font-size: 12px;
}

.card-title-row > .el-icon {
  color: #b3bbd0;
  font-size: 20px;
}

.faq-categories {
  display: flex;
  gap: 7px;
  margin: 16px 0 10px;
  overflow-x: auto;
  scrollbar-width: none;
}

.faq-categories::-webkit-scrollbar {
  display: none;
}

.faq-categories button {
  flex: 0 0 auto;
  padding: 5px 10px;
  color: #8b94aa;
  font-size: 12px;
  background: #f6f7fb;
  border: 0;
  border-radius: 14px;
  cursor: pointer;
}

.faq-categories button.active {
  color: #667eea;
  background: #eef0ff;
}

.faq-list {
  display: flex;
  flex-direction: column;
}

.faq-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 11px 0;
  color: #556079;
  text-align: left;
  background: transparent;
  border: 0;
  border-bottom: 1px solid #f1f3f7;
  cursor: pointer;
}

.faq-item:last-child {
  border-bottom: 0;
}

.faq-item span {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.faq-item .el-icon {
  flex-shrink: 0;
  color: #b4bdd0;
  transition: transform 0.2s;
}

.faq-item:hover {
  color: #667eea;
}

.faq-item:hover .el-icon {
  transform: translateX(3px);
  color: #667eea;
}

.faq-item:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.help-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: linear-gradient(135deg, #f4f6ff, #fbfaff);
  border-color: #e8eaff;
}

.help-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  color: #667eea;
  background: #e7e9ff;
  border-radius: 10px;
}

.help-card p {
  margin-top: 6px;
  color: #8b94aa;
  font-size: 12px;
  line-height: 1.6;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 720px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #edf0f6;
  border-radius: 16px;
  box-shadow: 0 7px 26px rgba(45, 58, 93, 0.06);
}

.chat-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 19px 24px;
  border-bottom: 1px solid #eff1f5;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: #fff;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 13px;
}

.chat-title h2 {
  color: #27304a;
  font-size: 17px;
}

.chat-title span {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 5px;
  color: #8f98ab;
  font-size: 12px;
}

.mini-status-dot {
  width: 6px;
  height: 6px;
  box-shadow: none;
}

.new-session-button {
  color: #7d879d;
}

.new-session-button:hover {
  color: #667eea;
  background: #f3f4ff;
}

.chat-messages {
  flex: 1;
  min-height: 365px;
  max-height: 470px;
  overflow-y: auto;
  padding: 20px 24px 12px;
  background: #fafbfe;
}

.conversation-date {
  width: fit-content;
  margin: 0 auto 20px;
  padding: 4px 11px;
  color: #a2a9ba;
  font-size: 11px;
  background: #f0f2f7;
  border-radius: 12px;
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.user-message {
  justify-content: flex-end;
}

.message-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  margin-top: 19px;
  color: #fff;
  border-radius: 10px;
}

.service-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.user-avatar {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
}

.message-content {
  max-width: 74%;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 19px;
  color: #949db0;
  font-size: 12px;
}

.user-message .message-meta {
  justify-content: flex-end;
}

.message-meta time {
  color: #b4bbc9;
  font-size: 11px;
}

.message-bubble {
  padding: 11px 14px;
  color: #424d68;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-line;
  word-break: break-word;
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 4px 13px 13px 13px;
  box-shadow: 0 2px 7px rgba(45, 58, 93, 0.03);
}

.user-message .message-bubble {
  color: #fff;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: 0;
  border-radius: 13px 4px 13px 13px;
}

.typing-bubble {
  display: flex;
  align-items: center;
  gap: 5px;
  width: 64px;
  padding: 15px 14px;
}

.typing-bubble i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #aab2c2;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-bubble i:nth-child(2) { animation-delay: -0.16s; }
.typing-bubble i:nth-child(3) { animation-delay: -0.32s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.quick-questions {
  padding: 13px 24px 15px;
  border-top: 1px solid #eff1f5;
}

.quick-title {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 9px;
  color: #8b94aa;
  font-size: 12px;
}

.quick-title .el-icon {
  color: #667eea;
}

.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-list button {
  padding: 6px 11px;
  color: #667eea;
  font-size: 12px;
  background: #f2f3ff;
  border: 1px solid #e8eaff;
  border-radius: 14px;
  cursor: pointer;
}

.quick-list button:hover {
  color: #fff;
  background: #667eea;
  border-color: #667eea;
}

.service-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 62px;
  padding: 10px 24px;
  border-top: 1px solid #eff1f5;
}

.action-state {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #8b94aa;
  font-size: 12px;
}

.closed-icon {
  color: #67c23a;
  font-size: 16px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.evaluation-panel {
  padding: 17px 24px 19px;
  background: #fffaf2;
  border-top: 1px solid #f4e4c7;
}

.evaluation-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.evaluation-heading h3 {
  color: #544735;
  font-size: 14px;
}

.evaluation-heading p {
  margin-top: 4px;
  color: #aa987d;
  font-size: 12px;
}

.evaluation-heading .el-button {
  padding-top: 0;
  color: #aa987d;
}

.evaluation-content {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 13px;
}

.evaluation-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.evaluation-content :deep(.el-textarea) {
  width: 220px;
}

.chat-input-area {
  position: relative;
  padding: 16px 24px 20px;
  border-top: 1px solid #eff1f5;
}

.message-input :deep(.el-input__wrapper) {
  min-height: 42px;
  box-shadow: 0 0 0 1px #e3e7ef inset;
}

.message-input :deep(.el-input-group__append) {
  padding: 0;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-color: transparent;
}

.message-input :deep(.el-input-group__append .el-button) {
  height: 42px;
  padding: 0 22px;
  color: #fff;
  border: 0;
}

.input-tip {
  display: block;
  margin-top: 7px;
  color: #b1b8c7;
  font-size: 11px;
}

.api-settings-content {
  padding: 4px 2px 10px;
}

.api-settings-description {
  margin-bottom: 14px;
  color: #606a80;
  font-size: 14px;
  line-height: 1.6;
}

.api-security-tip {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin-top: 12px;
  padding: 10px 12px;
  color: #9a835f;
  font-size: 12px;
  line-height: 1.6;
  background: #fff8e9;
  border: 1px solid #f7e7c3;
  border-radius: 8px;
}

.api-security-tip .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: #c99a4b;
}

@media (max-width: 1220px) {
  .service-container {
    width: calc(100% - 40px);
  }
}

@media (max-width: 900px) {
  .service-layout {
    grid-template-columns: 1fr;
  }

  .service-sidebar {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .help-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 620px) {
  .customer-service-page {
    padding-top: 20px;
  }

  .service-container {
    width: calc(100% - 24px);
  }

  .service-hero {
    display: block;
  }

  .hero-status {
    width: fit-content;
    margin-top: 18px;
  }

  .hero-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .service-sidebar {
    display: flex;
  }

  .chat-panel {
    min-height: 650px;
  }

  .chat-panel-header,
  .chat-messages,
  .quick-questions,
  .service-actions,
  .evaluation-panel,
  .chat-input-area {
    padding-left: 16px;
    padding-right: 16px;
  }

  .service-actions,
  .evaluation-content {
    align-items: flex-start;
    flex-direction: column;
  }

  .action-buttons,
  .action-buttons .el-button {
    width: 100%;
  }

  .evaluation-content :deep(.el-textarea) {
    width: 100%;
  }
}
</style>
