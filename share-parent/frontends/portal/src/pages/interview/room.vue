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
          v-if="sessionData.status === 2"
          type="success"
          size="small"
          class="finish-btn completed"
          @click="goToReport"
        >
          📊 查看终局报告
        </el-button>
        <el-button
          v-else-if="sessionData.status === 3"
          type="info"
          size="small"
          class="finish-btn"
          disabled
        >
          🛑 已终止
        </el-button>
        <el-button
          v-else
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

    <!-- 考场问答核心工作区 -->
    <div class="room-workspace">
      <!-- 问答对话链路 -->
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
                  <el-button
                    link
                    size="small"
                    class="tts-btn"
                    :class="{ playing: speakingTurnId === turn.id }"
                    @click="toggleSpeakQuestion(turn)"
                  >
                    {{ speakingTurnId === turn.id ? '🔊 停止播放' : '🔈 考官发音' }}
                  </el-button>
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

        <!-- 底部答题输入区 / 完赛横幅 -->
        <div v-if="isCompleted" class="dialogue-completed-banner">
          <div class="banner-left">
            <span class="banner-icon">{{ isTerminated ? '🛑' : '🎉' }}</span>
            <div class="banner-texts">
              <div class="title">{{ isTerminated ? '本场模拟面试已终止' : '本次模拟面试已圆满完成！' }}</div>
              <div class="desc">{{ isTerminated ? '您可在此回顾问答记录与考官深度诊断，如需重新挑战请返回大厅开启新场次。' : '大厂面试官评审委员会已完成终局职级裁决与六维能力雷达图评定。' }}</div>
            </div>
          </div>
          <el-button v-if="sessionData.status === 2" type="primary" size="default" @click="goToReport">
            查看终局评测报告 ➔
          </el-button>
        </div>
        <div v-else class="dialogue-input-bar">
          <div class="input-tip">
            <span>当前第 <b>{{ currentTurnNum }}</b> 轮：请结合实际项目指标与底层原理结构化作答</span>
            <span class="tip-shortcut">（支持 Ctrl + Enter 快捷提交）</span>
          </div>
          <el-input
            v-model="currentAnswer"
            type="textarea"
            :rows="4"
            placeholder="在此输入您的回答...（建议按结构化阐述：首先从核心概念与底层原理谈起，其次剖析运行机制与关键设计权衡，最后联系生产实战与排障经验...）"
            resize="none"
            :disabled="submittingAnswer || isCompleted"
            @keydown.ctrl.enter="handleSubmitAnswer"
          />
          <div class="input-actions">
            <div class="voice-tool">
              <el-button
                size="small"
                :type="isRecording ? 'danger' : 'default'"
                class="voice-btn"
                :class="{ recording: isRecording }"
                :disabled="submittingAnswer || isCompleted"
                @click="toggleVoiceRecognition"
              >
                <span class="voice-icon">{{ isRecording ? '⏹ 停止连麦' : '🎙️ 麦克风录音' }}</span>
              </el-button>
              <span v-if="isRecording" class="recording-pulse">正在收音辨识中...</span>
            </div>
            <div class="submit-actions">
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
  finishInterview
} from '@/api/interview.js'

const route = useRoute()
const router = useRouter()
const sessionId = route.params.id

const loading = ref(true)
const submittingAnswer = ref(false)
const finishing = ref(false)
const dialogueScrollRef = ref(null)

const sessionData = ref({
  turns: []
})

const currentAnswer = ref('')

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
const isCompleted = computed(() => sessionData.value.status === 2 || sessionData.value.status === 3)
const isTerminated = computed(() => sessionData.value.status === 3)

const goToReport = () => {
  const targetId = sessionId || (sessionData.value && sessionData.value.id)
  if (!targetId) {
    ElMessage.warning('面试场次信息缺失，无法跳转报告')
    return
  }
  router.push(`/interview/report/${targetId}`)
}

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
      if (sessionData.value.status === 2) {
        ElMessageBox.alert(
          '恭喜您已完成全部轮次考核！大厂评审委员会已完成终局职级裁决与六维能力雷达报告。',
          '模拟面试已完成',
          {
            confirmButtonText: '查看终局报告',
            type: 'success',
            callback: () => goToReport()
          }
        )
      }
    }
  } catch (err) {
    ElMessage.error('提交回答失败：' + (err.message || '系统繁忙'))
  } finally {
    submittingAnswer.value = false
  }
}

// Web Speech API - 语音合成 (TTS)
const speakingTurnId = ref(null)
const toggleSpeakQuestion = (turn) => {
  if (!('speechSynthesis' in window)) {
    ElMessage.warning('当前浏览器不支持 Web Speech 语音朗读功能')
    return
  }
  if (speakingTurnId.value === turn.id) {
    window.speechSynthesis.cancel()
    speakingTurnId.value = null
    return
  }
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(turn.question)
  utterance.lang = 'zh-CN'
  utterance.rate = 1.0
  utterance.pitch = 1.0
  utterance.onend = () => {
    speakingTurnId.value = null
  }
  utterance.onerror = () => {
    speakingTurnId.value = null
  }
  speakingTurnId.value = turn.id
  window.speechSynthesis.speak(utterance)
}

// Web Speech API - 语音识别 (STT)
const isRecording = ref(false)
let speechRecognitionInstance = null

const toggleVoiceRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessage.info('您的浏览器暂未开放 Web 语音识别接口（推荐使用 Chrome / Edge 浏览器）')
    return
  }

  if (isRecording.value) {
    if (speechRecognitionInstance) {
      speechRecognitionInstance.stop()
    }
    isRecording.value = false
    return
  }

  try {
    const recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'
    recognition.continuous = true
    recognition.interimResults = true

    recognition.onstart = () => {
      isRecording.value = true
      ElMessage.success('已开启麦克风收音，请开始阐述您的回答...')
    }

    recognition.onresult = (event) => {
      let finalTranscript = ''
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) {
          finalTranscript += event.results[i][0].transcript
        }
      }
      if (finalTranscript) {
        currentAnswer.value = (currentAnswer.value ? currentAnswer.value + ' ' : '') + finalTranscript.trim()
      }
    }

    recognition.onerror = (event) => {
      console.warn('语音识别异常:', event.error)
      isRecording.value = false
      if (event.error !== 'no-speech') {
        ElMessage.warning('麦克风收音中断：' + event.error)
      }
    }

    recognition.onend = () => {
      isRecording.value = false
    }

    speechRecognitionInstance = recognition
    recognition.start()
  } catch (err) {
    isRecording.value = false
    ElMessage.error('启动麦克风失败：' + (err.message || '权限被拒绝'))
  }
}

const handleFinishInterview = () => {
  const turns = sessionData.value.turns || []
  const hasAnyAnswer = turns.some(t => Boolean(t.userAnswer && t.userAnswer.trim()))
  const confirmMsg = hasAnyAnswer
    ? '确认现在交卷并结束面试？系统将立即触发阿里/字节多维评审委员会终局裁决与六维能力雷达图生成。'
    : '【注意】：您当前尚未作答任何题目，直接交卷将按【缺考/未达标】终局裁定且综合得分为 0。确认现在交卷吗？'

  ElMessageBox.confirm(
    confirmMsg,
    hasAnyAnswer ? '交卷终审确认' : '缺考直接交卷警示',
    {
      confirmButtonText: '立即交卷',
      cancelButtonText: '继续答题',
      type: hasAnyAnswer ? 'warning' : 'danger'
    }
  ).then(async () => {
    try {
      finishing.value = true
      const res = await finishInterview(sessionId)
      if (res && res.data) {
        ElMessage.success('终局报告已生成！')
        goToReport()
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
  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel()
  }
  if (speechRecognitionInstance) {
    speechRecognitionInstance.stop()
  }
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

/* 工作区布局 */
.room-workspace {
  flex: 1;
  display: flex;
  justify-content: center;
  overflow: hidden;
  background: #f1f5f9;
}

/* 核心问答面板 */
.dialogue-panel {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
  overflow: hidden;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.05);
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

.tts-btn {
  font-size: 11px;
  color: #0284c7;
  padding: 0 4px;
}

.tts-btn:hover {
  color: #0369a1;
}

.tts-btn.playing {
  color: #ea580c;
  font-weight: 600;
  animation: pulse 1.2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
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

/* 完赛横幅与底部输入框 */
.dialogue-completed-banner {
  padding: 16px 20px;
  border-top: 1px solid #bbf7d0;
  background: #f0fdf4;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.dialogue-completed-banner .banner-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialogue-completed-banner .banner-icon {
  font-size: 26px;
}

.dialogue-completed-banner .banner-texts .title {
  font-size: 14px;
  font-weight: 600;
  color: #166534;
  margin-bottom: 2px;
}

.dialogue-completed-banner .banner-texts .desc {
  font-size: 12px;
  color: #15803d;
  line-height: 1.4;
}

.dialogue-input-bar {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

.input-tip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.tip-shortcut {
  font-size: 11px;
  color: #94a3b8;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.voice-tool {
  display: flex;
  align-items: center;
  gap: 10px;
}

.voice-btn.recording {
  animation: pulse 1s infinite;
}

.recording-pulse {
  font-size: 12px;
  color: #ef4444;
  font-weight: 500;
  animation: pulse 1s infinite;
}

.submit-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.char-count {
  font-size: 12px;
  color: #94a3b8;
}
</style>
