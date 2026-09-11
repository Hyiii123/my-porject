<template>
  <div class="interview-room-container" v-loading="loading">
    <!-- 考情 HUD 状态栏 -->
    <div class="interview-hud">
      <div class="hud-left">
        <el-button link class="back-btn" @click="handleBack">
          &larr; 返回大厅
        </el-button>
        <div class="hud-divider"></div>
        <div class="persona-badge">
          <span class="persona-icon">{{ getPersonaMeta.icon }}</span>
          <span class="persona-name">{{ getPersonaMeta.name }}</span>
        </div>
        <el-tag effect="dark" type="info" size="small" class="dimension-tag">
          {{ currentDimension }}
        </el-tag>
      </div>

      <div class="hud-center">
        <!-- 剥洋葱深度等级 HUD -->
        <div class="depth-hud">
          <span class="depth-label">追问深度：</span>
          <div class="depth-step" :class="{ active: currentDepth >= 1 }">
            <span class="dot"></span>
            <span>L1 概念摸底</span>
          </div>
          <span class="arrow">&rarr;</span>
          <div class="depth-step" :class="{ active: currentDepth >= 2 }">
            <span class="dot"></span>
            <span>L2 底层原理</span>
          </div>
          <span class="arrow">&rarr;</span>
          <div class="depth-step" :class="{ active: currentDepth >= 3 }">
            <span class="dot"></span>
            <span>L3 线上排障</span>
          </div>
        </div>
      </div>

      <div class="hud-right">
        <div class="hud-timer">
          <span class="timer-icon">⏱️</span>
          <span>{{ formatTimer(timerSeconds) }}</span>
        </div>
        <div class="turn-progress">
          轮次：<b>{{ currentTurnNum }}/{{ sessionData.totalTurns || 6 }}</b>
        </div>
        <el-button
          type="danger"
          size="small"
          class="finish-btn"
          :loading="finishing"
          @click="handleFinishInterview"
        >
          交卷并生成报告
        </el-button>
      </div>
    </div>

    <!-- 考场双栏核心区域 -->
    <div class="room-workspace">
      <!-- 左栏：问答对话链路 -->
      <div class="dialogue-panel">
        <div class="panel-header">
          <div class="header-title">
            <span class="dot live"></span>
            <span>面试官连环问答与连麦</span>
          </div>
          <span class="header-tip">已开启 10,000 题库影子语义实时检定</span>
        </div>

        <div class="dialogue-scroll" ref="dialogueScrollRef">
          <!-- 轮次问答列表 -->
          <div v-for="turn in sessionData.turns" :key="turn.id" class="turn-block">
            <!-- 面试官提问气泡 -->
            <div class="bubble-row interviewer">
              <div class="avatar-box">
                <span class="avatar-icon">{{ getPersonaMeta.icon }}</span>
              </div>
              <div class="bubble-content">
                <div class="bubble-meta">
                  <span class="sender-name">{{ getPersonaMeta.name }}</span>
                  <el-tag size="small" effect="plain" type="primary">
                    第 {{ turn.turnNum }} 轮 · {{ getDepthText(turn.depthLevel) }}
                  </el-tag>
                  <span class="time">{{ formatTime(turn.createTime) }}</span>
                </div>
                <div class="bubble-text question-text">
                  {{ turn.question }}
                </div>
              </div>
            </div>

            <!-- 学员回答气泡 -->
            <div v-if="turn.userAnswer" class="bubble-row candidate">
              <div class="bubble-content">
                <div class="bubble-meta candidate-meta">
                  <span class="time">{{ formatTime(turn.answerTime) }}</span>
                  <span class="sender-name">候选人（我）</span>
                  <div class="avatar-box mini">
                    <span>👨‍💻</span>
                  </div>
                </div>
                <div class="bubble-text answer-text">
                  {{ turn.userAnswer }}
                </div>
              </div>
            </div>

            <!-- AI 点评与复盘卡片 -->
            <div v-if="turn.aiFeedback" class="feedback-card">
              <div class="feedback-header">
                <div class="score-badge">
                  <span>考官评分：</span>
                  <b :class="getScoreColor(turn.turnScore)">{{ turn.turnScore }} 分</b>
                </div>
                <div v-if="turn.matchedKnowledgeId" class="match-badge">
                  <el-tag size="small" type="success">
                    ✓ 命中 10,000 知识库 #{{ turn.matchedKnowledgeId }}
                  </el-tag>
                </div>
              </div>
              <div class="feedback-body">
                <div class="feedback-section">
                  <div class="section-title">💡 深度诊断剖析</div>
                  <div class="section-content">{{ turn.aiFeedback }}</div>
                </div>
                <div v-if="turn.standardReference" class="feedback-section standard-ref">
                  <div class="section-title">🏆 大厂标杆满分范式</div>
                  <div class="section-content">{{ turn.standardReference }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 若正在生成下一题 loading 占位 -->
          <div v-if="submittingAnswer" class="bubble-row interviewer thinking">
            <div class="avatar-box">
              <span class="avatar-icon">{{ getPersonaMeta.icon }}</span>
            </div>
            <div class="bubble-content">
              <div class="thinking-box">
                <span class="dot-pulse"></span>
                <span>AI 面试官正在通过 Qdrant 检定作答并组织连环追问...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部答题输入框 -->
        <div class="dialogue-input-bar">
          <div class="input-tip">
            <span>当前第 <b>{{ currentTurnNum }}</b> 轮：请结合实际项目指标与底层原理结构化作答</span>
          </div>
          <el-input
            v-model="currentAnswer"
            type="textarea"
            :rows="4"
            placeholder="在此输入您的回答...（例如：首先从底层数据结构谈起，其次结合锁升级机制，最后联系高并发生产排障经验...）"
            resize="none"
            :disabled="submittingAnswer || isCompleted"
          />
          <div class="input-actions">
            <span class="char-count">{{ currentAnswer.length }} 字</span>
            <el-button
              type="primary"
              size="default"
              :loading="submittingAnswer"
              :disabled="!currentAnswer.trim() || isCompleted"
              @click="handleSubmitAnswer"
            >
              提交本轮作答 & 迎接追问 ➔
            </el-button>
          </div>
        </div>
      </div>

      <!-- 右栏：算法手撕沙箱与架构审计 -->
      <div class="sandbox-panel">
        <div class="panel-header">
          <div class="header-title">
            <span class="code-icon">⚡</span>
            <span>算法手撕沙箱 & 代码异味审计</span>
          </div>
          <div class="header-tools">
            <el-select v-model="codeLanguage" size="small" class="lang-select">
              <el-option label="Java (JDK 17)" value="java" />
              <el-option label="Python 3" value="python" />
              <el-option label="Go 1.21" value="go" />
              <el-option label="C++ 20" value="cpp" />
            </el-select>
            <el-button size="small" @click="handleResetCode">重置模板</el-button>
          </div>
        </div>

        <div class="sandbox-body">
          <div class="problem-bar">
            <span class="problem-tag">现场手撕题</span>
            <span class="problem-title">{{ currentProblemTitle }}</span>
          </div>

          <!-- 代码编辑区域 -->
          <div class="code-editor-wrapper">
            <textarea
              v-model="userCode"
              class="code-editor"
              spellcheck="false"
              placeholder="// 在此编写您的算法实现代码..."
            ></textarea>
          </div>

          <div class="sandbox-actions">
            <el-button
              type="success"
              size="default"
              :loading="evaluatingCode"
              @click="handleSubmitCode"
            >
              ▶ 运行沙箱评测 & 审计架构异味
            </el-button>
          </div>

          <!-- 评测结果控制台 -->
          <div v-if="lastCodeSubmission" class="eval-console">
            <div class="console-header">
              <span class="status-tag" :class="lastCodeSubmission.executionStatus">
                {{ lastCodeSubmission.executionStatus.toUpperCase() }}
              </span>
              <span class="case-info">
                测试用例通过率：{{ lastCodeSubmission.passedTestCases }}/{{ lastCodeSubmission.totalTestCases }}
              </span>
            </div>

            <div class="complexity-row">
              <div class="comp-box">
                <span class="label">推演时间复杂度：</span>
                <span class="val">{{ lastCodeSubmission.timeComplexity || 'O(n)' }}</span>
              </div>
              <div class="comp-box">
                <span class="label">推演空间复杂度：</span>
                <span class="val">{{ lastCodeSubmission.spaceComplexity || 'O(1)' }}</span>
              </div>
            </div>

            <div v-if="lastCodeSubmission.codeSmells" class="smell-box">
              <div class="box-title">🔍 代码异味与缺陷审计：</div>
              <div class="box-content">{{ lastCodeSubmission.codeSmells }}</div>
            </div>

            <div v-if="lastCodeSubmission.refactoredCode" class="refactor-box">
              <div class="box-title">✨ AI 重构标杆范式（大厂生产级）：</div>
              <pre class="refactor-code"><code>{{ lastCodeSubmission.refactoredCode }}</code></pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInterviewDetail,
  submitInterviewAnswer,
  submitInterviewCode,
  finishInterview
} from '@/api/interview.js'

const route = useRoute()
const router = useRouter()
const sessionId = route.params.id

const loading = ref(true)
const submittingAnswer = ref(false)
const evaluatingCode = ref(false)
const finishing = ref(false)
const dialogueScrollRef = ref(null)

const sessionData = ref({
  turns: [],
  codeSubmissions: []
})

const currentAnswer = ref('')
const codeLanguage = ref('java')
const currentProblemTitle = ref('高并发滑动窗口限流器 / 线程安全缓存容器')
const userCode = ref(`public class RateLimiter {
    private final int maxRequests;
    private final long windowSizeMillis;
    private final java.util.concurrent.ConcurrentLinkedQueue<Long> timestamps = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public RateLimiter(int maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
    }

    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        while (!timestamps.isEmpty() && now - timestamps.peek() > windowSizeMillis) {
            timestamps.poll();
        }
        if (timestamps.size() < maxRequests) {
            timestamps.offer(now);
            return true;
        }
        return false;
    }
}`)

const lastCodeSubmission = ref(null)

// 计时器
const timerSeconds = ref(0)
let timerInterval = null

const currentTurn = computed(() => {
  const turns = sessionData.value.turns || []
  if (turns.length === 0) return null
  // 找到未作答的最新轮次，否则取最后一个
  const unanswered = turns.find(t => !t.userAnswer)
  return unanswered || turns[turns.length - 1]
})

const currentTurnNum = computed(() => currentTurn.value?.turnNum || 1)
const currentDepth = computed(() => currentTurn.value?.depthLevel || 1)
const currentDimension = computed(() => currentTurn.value?.dimension || 'Java核心与高并发')
const isCompleted = computed(() => sessionData.value.status === 2)

const getPersonaMeta = computed(() => {
  const style = sessionData.value.interviewerStyle || 'p7_architect'
  if (style === 'bytedance_tech') {
    return { icon: '💻', name: '字节跳动技术专家' }
  }
  if (style === 'gentle_hr') {
    return { icon: '🤝', name: '资深大厂 HRBP' }
  }
  if (style === 'standard') {
    return { icon: '⚖️', name: '大厂评审委员会' }
  }
  return { icon: '⚡', name: '阿里 P7 资深架构师' }
})

const getDepthText = (depth) => {
  if (depth === 2) return 'Level 2 底层原理深挖'
  if (depth === 3) return 'Level 3 线上极限排障'
  return 'Level 1 概念摸底'
}

const getScoreColor = (score) => {
  if (score >= 85) return 'score-high'
  if (score >= 70) return 'score-mid'
  return 'score-low'
}

const formatTimer = (secs) => {
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(11, 19)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (dialogueScrollRef.value) {
      dialogueScrollRef.value.scrollTop = dialogueScrollRef.value.scrollHeight
    }
  })
}

const loadSession = async () => {
  try {
    loading.value = true
    const res = await getInterviewDetail(sessionId)
    if (res && res.data) {
      sessionData.value = res.data
      timerSeconds.value = res.data.durationSeconds || 0
      if (res.data.codeSubmissions && res.data.codeSubmissions.length > 0) {
        lastCodeSubmission.value = res.data.codeSubmissions[0]
      }
      if (res.data.status === 2) {
        ElMessage.info('本场面试已交卷完成，可查看能力诊断报告')
      }
    }
  } catch (err) {
    ElMessage.error('加载考场详情异常：' + (err.message || '网络错误'))
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const handleSubmitAnswer = async () => {
  if (!currentTurn.value || !currentAnswer.value.trim()) return
  try {
    submittingAnswer.value = true
    const res = await submitInterviewAnswer({
      sessionId: Number(sessionId),
      turnId: currentTurn.value.id,
      userAnswer: currentAnswer.value.trim()
    })
    if (res && res.data) {
      ElMessage.success('本轮回答已提交，AI 面试官已完成评分！')
      currentAnswer.value = ''
      await loadSession()
    }
  } catch (err) {
    ElMessage.error('提交回答失败：' + (err.message || '系统繁忙'))
  } finally {
    submittingAnswer.value = false
  }
}

const handleSubmitCode = async () => {
  if (!userCode.value.trim()) {
    ElMessage.warning('代码内容不能为空')
    return
  }
  try {
    evaluatingCode.value = true
    const res = await submitInterviewCode({
      sessionId: Number(sessionId),
      turnId: currentTurn.value?.id,
      problemTitle: currentProblemTitle.value,
      language: codeLanguage.value,
      userCode: userCode.value
    })
    if (res && res.data) {
      lastCodeSubmission.value = res.data
      ElMessage.success('代码沙箱评测与异味审计已完成！')
    }
  } catch (err) {
    ElMessage.error('评测失败：' + (err.message || '沙箱异常'))
  } finally {
    evaluatingCode.value = false
  }
}

const handleResetCode = () => {
  userCode.value = `// 请在此实现核心算法逻辑\nclass Solution {\n    public void solve() {\n        \n    }\n}`
}

const handleFinishInterview = () => {
  ElMessageBox.confirm(
    '确认现在交卷并结束面试？系统将立即触发阿里/字节多维评审委员会终局裁决与六维能力雷达图生成。',
    '交卷终审确认',
    {
      confirmButtonText: '立即交卷',
      cancelButtonText: '继续答题',
      type: 'warning'
    }
  ).then(async () => {
    try {
      finishing.value = true
      const res = await finishInterview(sessionId)
      if (res && res.data) {
        ElMessage.success('终局报告已生成！')
        router.push({ name: 'interviewReport', params: { id: sessionId } })
      }
    } catch (err) {
      ElMessage.error('交卷异常：' + (err.message || '网络错误'))
    } finally {
      finishing.value = false
    }
  }).catch(() => {})
}

const handleBack = () => {
  router.push({ name: 'interviewIndex' })
}

onMounted(() => {
  loadSession()
  timerInterval = setInterval(() => {
    if (!isCompleted.value) {
      timerSeconds.value++
    }
  }, 1000)
})

onBeforeUnmount(() => {
  if (timerInterval) clearInterval(timerInterval)
})
</script>

<style scoped>
.interview-room-container {
  height: calc(100vh - 72px);
  display: flex;
  flex-direction: column;
  background: #f1f5f9;
  overflow: hidden;
}

/* 考情 HUD 状态栏 */
.interview-hud {
  height: 56px;
  background: #0f172a;
  color: #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #1e293b;
  flex-shrink: 0;
}

.hud-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.back-btn {
  color: #94a3b8;
  font-size: 13px;
}

.back-btn:hover {
  color: #fff;
}

.hud-divider {
  width: 1px;
  height: 20px;
  background: #334155;
}

.persona-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
  color: #38bdf8;
}

.hud-center {
  display: flex;
  align-items: center;
}

.depth-hud {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.depth-label {
  color: #64748b;
}

.depth-step {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #475569;
  font-weight: 500;
}

.depth-step.active {
  color: #38bdf8;
  font-weight: 600;
}

.depth-step .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #475569;
}

.depth-step.active .dot {
  background: #38bdf8;
  box-shadow: 0 0 8px #38bdf8;
}

.arrow {
  color: #334155;
}

.hud-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hud-timer {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: monospace;
  font-size: 15px;
  font-weight: 700;
  color: #fbbf24;
}

.turn-progress {
  font-size: 13px;
  color: #94a3b8;
}

.turn-progress b {
  color: #f8fafc;
}

/* 双栏工作区 */
.room-workspace {
  flex: 1;
  display: grid;
  grid-template-columns: 55% 45%;
  overflow: hidden;
}

/* 左栏：问答面板 */
.dialogue-panel {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #e2e8f0;
  overflow: hidden;
}

.panel-header {
  height: 44px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 13px;
  color: #334155;
}

.dot.live {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 6px #10b981;
}

.header-tip {
  font-size: 11px;
  color: #94a3b8;
}

.dialogue-scroll {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.bubble-row {
  display: flex;
  gap: 12px;
  max-width: 90%;
}

.bubble-row.interviewer {
  align-self: flex-start;
}

.bubble-row.candidate {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.avatar-box {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #0f172a;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 20px;
  flex-shrink: 0;
}

.avatar-box.mini {
  width: 28px;
  height: 28px;
  font-size: 14px;
  background: #2563eb;
}

.bubble-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}

.candidate-meta {
  justify-content: flex-end;
}

.bubble-text {
  padding: 14px 18px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.question-text {
  background: #f1f5f9;
  color: #0f172a;
  border-top-left-radius: 2px;
  border: 1px solid #e2e8f0;
}

.answer-text {
  background: #2563eb;
  color: #fff;
  border-top-right-radius: 2px;
  white-space: pre-wrap;
}

.thinking-box {
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.dot-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #3b82f6;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.8); opacity: 0.5; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.8); opacity: 0.5; }
}

/* 反馈卡片 */
.feedback-card {
  margin: 12px 0 12px 50px;
  background: #fafaf9;
  border: 1px solid #e7e5e4;
  border-left: 4px solid #f59e0b;
  border-radius: 8px;
  padding: 14px 18px;
}

.feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.score-badge {
  font-size: 13px;
  font-weight: 600;
  color: #44403c;
}

.score-high { color: #16a34a; font-size: 16px; }
.score-mid { color: #d97706; font-size: 16px; }
.score-low { color: #dc2626; font-size: 16px; }

.feedback-section {
  margin-bottom: 8px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #78716c;
  margin-bottom: 4px;
}

.section-content {
  font-size: 13px;
  color: #292524;
  line-height: 1.5;
}

.feedback-section.standard-ref {
  background: #f5f5f4;
  border-radius: 6px;
  padding: 10px 12px;
  margin-top: 8px;
}

/* 底部输入框 */
.dialogue-input-bar {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

.input-tip {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.char-count {
  font-size: 12px;
  color: #94a3b8;
}

/* 右栏：沙箱面板 */
.sandbox-panel {
  display: flex;
  flex-direction: column;
  background: #1e293b;
  color: #f8fafc;
  overflow: hidden;
}

.sandbox-panel .panel-header {
  background: #0f172a;
  border-bottom: 1px solid #334155;
}

.sandbox-panel .header-title {
  color: #f8fafc;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.lang-select {
  width: 120px;
}

.sandbox-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow-y: auto;
  gap: 14px;
}

.problem-bar {
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.problem-tag {
  background: #e11d48;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.problem-title {
  font-size: 13px;
  font-weight: 500;
  color: #e2e8f0;
}

.code-editor-wrapper {
  flex: 1;
  min-height: 240px;
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 8px;
  overflow: hidden;
}

.code-editor {
  width: 100%;
  height: 100%;
  background: transparent;
  color: #38bdf8;
  border: none;
  padding: 14px;
  font-family: 'Fira Code', Consolas, Monaco, monospace;
  font-size: 13px;
  line-height: 1.5;
  outline: none;
  resize: none;
}

.sandbox-actions {
  display: flex;
  justify-content: flex-end;
}

/* 评测控制台 */
.eval-console {
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 8px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.console-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-tag {
  font-weight: 700;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
}

.status-tag.accepted {
  background: #16a34a;
  color: #fff;
}

.case-info {
  font-size: 12px;
  color: #94a3b8;
}

.complexity-row {
  display: flex;
  gap: 20px;
  font-size: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #334155;
}

.comp-box .label {
  color: #94a3b8;
}

.comp-box .val {
  color: #38bdf8;
  font-weight: 700;
  font-family: monospace;
}

.smell-box, .refactor-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.box-title {
  font-size: 12px;
  font-weight: 600;
  color: #fbbf24;
}

.box-content {
  font-size: 12px;
  color: #cbd5e1;
  line-height: 1.5;
}

.refactor-code {
  background: #020617;
  border: 1px solid #1e293b;
  border-radius: 6px;
  padding: 10px;
  font-family: monospace;
  font-size: 12px;
  color: #a7f3d0;
  overflow-x: auto;
  max-height: 180px;
  margin: 0;
}
</style>
