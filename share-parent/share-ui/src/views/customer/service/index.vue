<template>
  <div class="customer-service-page">
    <div class="service-header">
      <div class="header-title">
        <div class="brand-mark"><el-icon><Headset /></el-icon></div>
        <div>
          <h2>客服中心</h2>
          <p>智问学伴 · AI 在线咨询</p>
        </div>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/index')">返回首页</el-button>
        <el-button type="primary" @click="createNewSession" :loading="creating">新建会话</el-button>
      </div>
    </div>

    <div class="service-content">
      <aside class="session-panel">
        <div class="panel-title">
          <span>我的会话</span>
          <el-button link type="primary" @click="loadSessions" :loading="loadingSessions">刷新</el-button>
        </div>
        <div v-if="!sessions.length" class="empty-session">还没有会话<br />点击右上角开始咨询</div>
        <div
          v-for="item in sessions"
          :key="item.id"
          class="session-item"
          :class="{ active: session && session.id === item.id }"
          @click="selectSession(item)"
        >
          <div class="session-item-title">
            <span>{{ item.sessionNo || '客服会话' }}</span>
            <el-tag size="small" :type="item.status === 3 ? 'info' : 'success'">
              {{ item.status === 3 ? '已结束' : '进行中' }}
            </el-tag>
          </div>
          <div class="session-item-message">{{ item.lastMessage || '等待您的咨询' }}</div>
          <div class="session-item-time">{{ item.updatedAt || item.updateTime || '' }}</div>
        </div>
      </aside>

      <main class="chat-panel">
        <div v-if="session" class="chat-header">
          <div>
            <strong>智问学伴</strong>
            <span class="online-dot"></span><span class="online-text">AI 在线</span>
          </div>
          <div class="chat-header-actions">
            <span class="session-no">{{ session.sessionNo }}</span>
            <el-button link type="primary" @click="openEvaluation">服务评价</el-button>
          </div>
        </div>
        <div v-else class="chat-empty">
          <el-icon><ChatDotRound /></el-icon>
          <p>正在准备客服会话…</p>
        </div>

        <div v-if="session" ref="messageListRef" class="message-list">
          <div v-if="loadingMessages" class="loading-message"><el-icon class="is-loading"><Loading /></el-icon> 正在加载会话</div>
          <div v-for="item in messages" :key="item.id || item.createTime + item.content" class="message-row" :class="messageClass(item)">
            <div class="message-avatar">
              <el-icon v-if="item.messageType === 1"><User /></el-icon>
              <el-icon v-else><Headset /></el-icon>
            </div>
            <div class="message-main">
              <div class="message-name">{{ item.senderName || (item.messageType === 1 ? '我' : '智问学伴') }}</div>
              <div class="message-bubble">{{ item.content }}</div>
              <div class="message-time">{{ item.createTime || '' }}</div>
            </div>
          </div>
        </div>

        <div v-if="session" class="chat-footer">
          <div class="quick-questions">
            <span>猜你想问：</span>
            <el-button v-for="item in faqs.slice(0, 4)" :key="item.id" link @click="useFaq(item)">{{ item.question }}</el-button>
          </div>
          <div class="composer" :class="{ disabled: session.status === 3 }">
            <el-input
              v-model="draft"
              type="textarea"
              :rows="3"
              resize="none"
              :disabled="session.status === 3 || sending"
              placeholder="请输入您想咨询的问题，Enter 发送"
              @keydown.enter.exact.prevent="send"
            />
            <div class="composer-bottom">
              <span class="composer-hint">内容会保存到本次会话记录中</span>
              <el-button type="primary" :loading="sending" :disabled="session.status === 3 || !draft.trim()" @click="send">发送</el-button>
            </div>
          </div>
        </div>
      </main>

      <aside class="faq-panel">
        <div class="panel-title"><span>常见问题</span><el-tag type="info" effect="plain">{{ faqs.length }} 条</el-tag></div>
        <div v-if="!faqs.length" class="empty-faq">暂无常见问题</div>
        <div v-for="item in faqs" :key="item.id" class="faq-card" @click="useFaq(item)">
          <div class="faq-category">{{ item.category }}</div>
          <div class="faq-question">{{ item.question }}</div>
          <div class="faq-answer">{{ item.answer }}</div>
        </div>
      </aside>
    </div>

    <el-dialog v-model="evaluationVisible" title="评价本次服务" width="440px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="服务评分">
          <el-rate v-model="evaluation.score" :texts="['很差', '较差', '一般', '满意', '非常满意']" show-text />
        </el-form-item>
        <el-form-item label="评价标签">
          <el-check-tag v-for="tag in evaluationTags" :key="tag" :checked="evaluation.tags.includes(tag)" class="evaluation-tag" @change="toggleTag(tag)">{{ tag }}</el-check-tag>
        </el-form-item>
        <el-form-item label="补充意见">
          <el-input v-model="evaluation.comment" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="欢迎留下您的建议" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evaluationVisible = false">取消</el-button>
        <el-button type="primary" :loading="evaluating" @click="submitEvaluation">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Headset, Loading, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import {
  createCustomerSession,
  evaluateCustomerSession,
  listCustomerMessages,
  listMyCustomerSessions,
  listPublicCustomerFaq,
  sendCustomerMessage
} from '@/api/customer'

const router = useRouter()
const session = ref(null)
const sessions = ref([])
const messages = ref([])
const faqs = ref([])
const draft = ref('')
const creating = ref(false)
const sending = ref(false)
const evaluating = ref(false)
const loadingSessions = ref(false)
const loadingMessages = ref(false)
const evaluationVisible = ref(false)
const messageListRef = ref(null)
const evaluationTags = ['回答准确', '响应及时', '表达清晰', '问题已解决']
const evaluation = reactive({ score: 5, tags: [], comment: '' })

onMounted(async () => {
  await Promise.all([loadFaqs(), loadSessions()])
  if (!session.value) {
    await createNewSession()
  }
})

async function loadFaqs() {
  try {
    const result = await listPublicCustomerFaq({ limit: 30 })
    faqs.value = Array.isArray(result) ? result : (result.data || [])
  } catch (error) {
    faqs.value = []
  }
}

async function loadSessions() {
  loadingSessions.value = true
  try {
    const result = await listMyCustomerSessions({ pageNum: 1, pageSize: 30 })
    sessions.value = result.rows || []
    const current = sessions.value.find(item => session.value && item.id === session.value.id)
    const next = current || sessions.value.find(item => item.status !== 3) || sessions.value[0]
    if (next) {
      await selectSession(next)
    }
  } finally {
    loadingSessions.value = false
  }
}

async function createNewSession() {
  creating.value = true
  try {
    const result = await createCustomerSession({})
    const created = result.data
    if (created) {
      sessions.value = [created, ...sessions.value.filter(item => item.id !== created.id)]
      await selectSession(created)
    }
  } finally {
    creating.value = false
  }
}

async function selectSession(item) {
  session.value = item
  loadingMessages.value = true
  try {
    const result = await listCustomerMessages(item.id)
    messages.value = result.data || []
    await scrollToBottom()
  } finally {
    loadingMessages.value = false
  }
}

async function send() {
  if (!session.value || session.value.status === 3 || !draft.value.trim() || sending.value) return
  const content = draft.value.trim()
  draft.value = ''
  sending.value = true
  try {
    await sendCustomerMessage(session.value.id, { content })
    await selectSession(session.value)
    await loadSessions()
  } finally {
    sending.value = false
  }
}

function useFaq(item) {
  draft.value = item.question
}

function messageClass(item) {
  if (item.messageType === 1) return 'message-user'
  if (item.messageType === 4) return 'message-system'
  return 'message-ai'
}

function openEvaluation() {
  evaluation.score = session.value?.satisfactionScore || 5
  evaluation.tags = []
  evaluation.comment = ''
  evaluationVisible.value = true
}

function toggleTag(tag) {
  const index = evaluation.tags.indexOf(tag)
  if (index >= 0) evaluation.tags.splice(index, 1)
  else evaluation.tags.push(tag)
}

async function submitEvaluation() {
  if (!session.value || !evaluation.score) {
    ElMessage.warning('请先选择评分')
    return
  }
  evaluating.value = true
  try {
    await evaluateCustomerSession(session.value.id, { ...evaluation })
    session.value.satisfactionScore = evaluation.score
    evaluationVisible.value = false
    ElMessage.success('感谢您的评价')
  } finally {
    evaluating.value = false
  }
}

function scrollToBottom() {
  return nextTick(() => {
    if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  })
}
</script>

<style scoped lang="scss">
.customer-service-page {
  min-height: calc(100vh - 50px);
  padding: 24px 28px 28px;
  background: #f5f7fb;
  color: #24324a;
}

.service-header, .service-content { max-width: 1500px; margin: 0 auto; }
.service-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.header-title { display: flex; align-items: center; gap: 12px; }
.brand-mark { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 13px; color: #fff; background: linear-gradient(135deg, #4f8cff, #6a6ff5); font-size: 22px; box-shadow: 0 8px 18px rgba(79, 140, 255, .24); }
h2 { margin: 0; font-size: 22px; }
.header-title p { margin: 5px 0 0; color: #8a96aa; font-size: 13px; }
.header-actions { display: flex; gap: 10px; }
.service-content { display: grid; grid-template-columns: 245px minmax(440px, 1fr) 295px; gap: 16px; min-height: calc(100vh - 150px); }
.session-panel, .chat-panel, .faq-panel { background: #fff; border: 1px solid #e9edf5; border-radius: 14px; box-shadow: 0 8px 30px rgba(46, 67, 110, .05); overflow: hidden; }
.session-panel, .faq-panel { padding: 18px; }
.panel-title { display: flex; justify-content: space-between; align-items: center; font-weight: 600; margin-bottom: 14px; }
.empty-session, .empty-faq { padding: 44px 12px; color: #a3aec0; text-align: center; line-height: 1.9; font-size: 13px; }
.session-item { padding: 12px; margin-bottom: 8px; border: 1px solid transparent; border-radius: 10px; cursor: pointer; transition: .2s; }
.session-item:hover { background: #f6f8ff; }
.session-item.active { border-color: #cbd9ff; background: #f2f5ff; }
.session-item-title { display: flex; align-items: center; justify-content: space-between; gap: 6px; font-size: 12px; color: #45536b; }
.session-item-message { margin-top: 8px; color: #7b879b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 13px; }
.session-item-time { margin-top: 6px; color: #b0bac9; font-size: 11px; }
.chat-panel { display: flex; min-width: 0; flex-direction: column; }
.chat-header { height: 66px; padding: 0 22px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eef1f6; }
.chat-header strong { font-size: 16px; }
.online-dot { display: inline-block; width: 7px; height: 7px; margin-left: 12px; border-radius: 50%; background: #45c68a; vertical-align: middle; }
.online-text { margin-left: 5px; color: #7d8b9f; font-size: 12px; }
.chat-header-actions { display: flex; gap: 14px; align-items: center; }
.session-no { color: #a1adbd; font-size: 12px; }
.message-list { flex: 1; min-height: 380px; max-height: calc(100vh - 360px); overflow-y: auto; padding: 24px 26px; background: #fbfcff; }
.loading-message { color: #9ba7b9; text-align: center; font-size: 13px; }
.message-row { display: flex; gap: 10px; margin-bottom: 22px; }
.message-row.message-user { flex-direction: row-reverse; }
.message-avatar { flex: 0 0 32px; width: 32px; height: 32px; display: grid; place-items: center; color: #fff; border-radius: 10px; background: #91a5c5; }
.message-ai .message-avatar { background: linear-gradient(135deg, #4f8cff, #6a6ff5); }
.message-main { max-width: 76%; }
.message-user .message-main { text-align: right; }
.message-name { margin-bottom: 5px; color: #8d99ab; font-size: 12px; }
.message-bubble { display: inline-block; padding: 11px 14px; color: #34435b; border-radius: 4px 13px 13px 13px; background: #fff; box-shadow: 0 4px 12px rgba(42, 63, 105, .06); line-height: 1.65; white-space: pre-wrap; text-align: left; }
.message-user .message-bubble { color: #fff; border-radius: 13px 4px 13px 13px; background: #587df4; }
.message-time { margin-top: 5px; color: #b2bccb; font-size: 11px; }
.chat-footer { padding: 13px 20px 18px; border-top: 1px solid #eef1f6; }
.quick-questions { display: flex; align-items: center; gap: 4px; margin-bottom: 10px; overflow: hidden; white-space: nowrap; }
.quick-questions > span { flex: 0 0 auto; color: #9ba7b9; font-size: 12px; }
.quick-questions .el-button { max-width: 180px; overflow: hidden; text-overflow: ellipsis; }
.composer { border: 1px solid #dce3ef; border-radius: 10px; padding: 2px 10px 8px; transition: .2s; }
.composer:focus-within { border-color: #6c8ff5; box-shadow: 0 0 0 2px rgba(91, 126, 243, .1); }
.composer.disabled { background: #f7f8fb; }
.composer :deep(.el-textarea__inner) { padding: 8px 2px; border: 0; box-shadow: none; }
.composer-bottom { display: flex; justify-content: space-between; align-items: center; }
.composer-hint { color: #aab4c3; font-size: 11px; }
.faq-card { padding: 13px; margin-bottom: 10px; border: 1px solid #edf0f6; border-radius: 10px; cursor: pointer; transition: .2s; }
.faq-card:hover { border-color: #cdd9ff; background: #fafbff; transform: translateY(-1px); }
.faq-category { color: #6c8ff5; font-size: 11px; }
.faq-question { margin: 7px 0; color: #41506a; font-size: 13px; font-weight: 600; line-height: 1.5; }
.faq-answer { display: -webkit-box; overflow: hidden; color: #909bad; font-size: 12px; line-height: 1.55; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.evaluation-tag { margin-right: 7px; margin-bottom: 7px; }
.chat-empty { flex: 1; display: grid; place-items: center; color: #aab4c3; }
.chat-empty .el-icon { font-size: 46px; color: #b8c6f3; }
.chat-empty p { margin-top: -80px; }

@media (max-width: 1100px) {
  .service-content { grid-template-columns: 210px minmax(400px, 1fr); }
  .faq-panel { display: none; }
}
@media (max-width: 720px) {
  .customer-service-page { padding: 14px; }
  .service-header { align-items: flex-start; gap: 12px; }
  .header-actions .el-button:first-child { display: none; }
  .service-content { display: block; }
  .session-panel { display: none; }
  .message-list { min-height: 390px; }
}
</style>
