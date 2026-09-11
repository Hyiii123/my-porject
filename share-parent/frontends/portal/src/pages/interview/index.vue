<template>
  <div class="interview-lobby-container">
    <!-- 顶部 Banner -->
    <div class="lobby-banner">
      <div class="banner-content">
        <div class="banner-tag">
          <el-tag effect="dark" type="danger" round>Zhiwen AI Interview Pro</el-tag>
          <span class="tag-sub">基于 10,000 大厂真题库与 GPT-5.6-Luna 深度驱动</span>
        </div>
        <h1 class="banner-title">全真沉浸式 AI 模拟面试与职涯评测</h1>
        <p class="banner-desc">
          还原互联网大厂（阿里、字节、腾讯等）极限高压考场。支持“剥洋葱”三级连环追问、Qdrant 向量影子实时检定、在线代码沙箱手撕与终局多维能力职级诊断。
        </p>
      </div>
      <div class="banner-stats">
        <div class="stat-card">
          <div class="stat-num">10,000+</div>
          <div class="stat-label">题库向量底座</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">&lt;15ms</div>
          <div class="stat-label">语义检定延迟</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">3-Level</div>
          <div class="stat-label">剥洋葱深度追问</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">阿里P6/P7</div>
          <div class="stat-label">职级精准对标</div>
        </div>
      </div>
    </div>

    <!-- 主体区域：左侧发起面试，右侧历史记录 -->
    <div class="lobby-body">
      <!-- 发起新面试配置卡片 -->
      <el-card class="config-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="title">🎯 开启全新模拟面试</span>
            <el-tag type="success" size="small">实时并发就绪</el-tag>
          </div>
        </template>

        <el-form :model="form" label-position="top" class="interview-form">
          <!-- 个人中心简历深度联动状态栏 -->
          <div class="resume-link-status" :class="{ 'linked': hasLinkedResume }">
            <div class="status-left">
              <div class="status-title">
                <span class="icon">{{ hasLinkedResume ? '✅' : '📄' }}</span>
                <span class="txt">{{ hasLinkedResume ? '已挂载个人中心简历：' : '尚未关联个人简历' }}</span>
                <b class="resume-name" v-if="hasLinkedResume">
                  {{ linkedResume.fileName || '我的求职简历' }}
                </b>
                <el-tag v-if="hasLinkedResume && linkedResume.matchScore" type="success" size="small" round>
                  对标匹配度 {{ linkedResume.matchScore }} 分
                </el-tag>
              </div>
              <div class="status-desc">
                {{ hasLinkedResume ? '🎯 AI 面试官将精准结合简历中的实际项目与技术栈进行连环深挖' : '💡 前往个人中心导入简历，可解锁基于真实项目与核心技能的个性化深度追问' }}
              </div>
            </div>
            <div class="status-right">
              <el-switch
                v-if="hasLinkedResume"
                v-model="form.enableResumeCustomization"
                active-text="启用简历出题"
                inline-prompt
              />
              <el-button link type="primary" size="small" @click="goToMyResume">
                {{ hasLinkedResume ? '管理简历 ➔' : '前往导入简历 ➔' }}
              </el-button>
            </div>
          </div>

          <el-form-item label="目标岗位">
            <el-select
              v-model="form.targetJob"
              placeholder="请选择或搜索目标岗位（涵盖8大赛道31个细分岗位）"
              class="w-100"
              filterable
            >
              <el-option-group
                v-for="group in jobGroups"
                :key="group.label"
                :label="group.label"
              >
                <el-option
                  v-for="item in group.options"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-option-group>
            </el-select>
          </el-form-item>

          <el-form-item label="目标企业">
            <el-radio-group v-model="form.companyTarget" class="company-group">
              <el-radio-button label="阿里巴巴">阿里巴巴</el-radio-button>
              <el-radio-button label="字节跳动">字节跳动</el-radio-button>
              <el-radio-button label="腾讯科技">腾讯科技</el-radio-button>
              <el-radio-button label="美团点评">美团点评</el-radio-button>
              <el-radio-button label="大厂通用">大厂通用</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="面试官风格与考核侧重">
            <div class="style-grid">
              <div
                v-for="item in styleOptions"
                :key="item.value"
                class="style-card"
                :class="{ active: form.interviewerStyle === item.value }"
                @click="form.interviewerStyle = item.value"
              >
                <div class="style-title">
                  <span class="icon">{{ item.icon }}</span>
                  <span class="name">{{ item.name }}</span>
                </div>
                <div class="style-desc">{{ item.desc }}</div>
                <div class="style-quote">“{{ item.quote }}”</div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="答题轮次规划">
            <el-radio-group v-model="form.totalTurns">
              <el-radio-button :label="3">3 轮（极速摸底）</el-radio-button>
              <el-radio-button :label="6">6 轮（标准大厂全流程）</el-radio-button>
              <el-radio-button :label="8">8 轮（深度架构连环追问）</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <div class="form-action">
            <el-button
              type="primary"
              size="large"
              class="start-btn"
              :loading="starting"
              @click="handleStartInterview"
            >
              🚀 立即进入模拟面试考场
            </el-button>
          </div>
        </el-form>
      </el-card>

      <!-- 右侧：我的面试战绩与报告库 -->
      <el-card class="history-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="title">📋 职涯评测战报记录</span>
            <el-button link type="primary" @click="loadMyHistory">刷新记录</el-button>
          </div>
        </template>

        <div v-loading="loadingHistory" class="history-list">
          <div v-if="historyList.length === 0" class="empty-state">
            <el-empty description="暂无模拟面试记录，点击左侧立即开启实战！" />
          </div>

          <div
            v-for="item in historyList"
            :key="item.id"
            class="history-item"
            @click="handleOpenSession(item)"
          >
            <div class="item-top">
              <div class="job-tag">{{ item.targetJob }}</div>
              <el-tag :type="getStatusTag(item.status).type" size="small">
                {{ getStatusTag(item.status).text }}
              </el-tag>
            </div>
            <div class="item-meta">
              <span>企业：{{ item.companyTarget }}</span>
              <span>轮次：{{ item.currentTurn }}/{{ item.totalTurns }}</span>
              <span>得分：<b class="score-text">{{ item.score != null ? item.score + '分' : '--' }}</b></span>
            </div>
            <div v-if="item.report" class="item-report-badge">
              <el-tag effect="plain" type="warning" size="small">
                {{ item.report.offerDecision }} · {{ item.report.levelMatch }}
              </el-tag>
            </div>
            <div class="item-footer">
              <span class="time">{{ formatTime(item.createTime) }}</span>
              <el-button link type="primary" size="small">
                {{ item.status === 2 ? '查看诊断大屏 ➔' : '继续答题 ➔' }}
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { startInterview, getMyInterviews, getMyResume } from '@/api/interview.js'

const router = useRouter()
const route = useRoute()

const starting = ref(false)
const loadingHistory = ref(false)
const historyList = ref([])
const linkedResume = ref(null)
const hasLinkedResume = computed(() => Boolean(linkedResume.value && linkedResume.value.id))

const jobGroups = [
  {
    label: '一、后端开发与微服务架构',
    options: [
      { label: 'Java 高级开发工程师', value: 'Java 高级开发工程师' },
      { label: '后端架构师 (分布式/高并发)', value: '后端架构师 (分布式/高并发)' },
      { label: '微服务系统架构师', value: '微服务系统架构师' },
      { label: 'DDD 领域驱动设计专家', value: 'DDD 领域驱动设计专家' }
    ]
  },
  {
    label: '二、跨语言系统与高性能研发',
    options: [
      { label: 'Go 高并发/分布式开发工程师', value: 'Go 高并发/分布式开发工程师' },
      { label: 'C++ 底层系统与基础架构研发', value: 'C++ 底层系统与基础架构研发' },
      { label: 'Rust 高性能系统研发工程师', value: 'Rust 高性能系统研发工程师' },
      { label: 'Python 高级服务端开发工程师', value: 'Python 高级服务端开发工程师' }
    ]
  },
  {
    label: '三、Web 前端与跨端移动',
    options: [
      { label: 'Web 前端高级开发工程师', value: 'Web 前端高级开发工程师' },
      { label: 'Vue3 / React 核心架构专家', value: 'Vue3 / React 核心架构专家' },
      { label: '全栈开发工程师 (Full-Stack)', value: '全栈开发工程师 (Full-Stack)' },
      { label: 'iOS 高级客户端研发工程师', value: 'iOS 高级客户端研发工程师' },
      { label: 'Android 高级架构工程师', value: 'Android 高级架构工程师' }
    ]
  },
  {
    label: '四、AI 与大模型算法',
    options: [
      { label: '大语言模型 (LLM) 算法工程师', value: '大语言模型 (LLM) 算法工程师' },
      { label: 'RAG 与 Agent 智能体研发专家', value: 'RAG 与 Agent 智能体研发专家' },
      { label: 'NLP 自然语言处理算法专家', value: 'NLP 自然语言处理算法专家' },
      { label: '推荐系统与搜索排序算法专家', value: '推荐系统与搜索排序算法专家' },
      { label: '计算机视觉 (CV) 算法专家', value: '计算机视觉 (CV) 算法专家' }
    ]
  },
  {
    label: '五、大数据与流批计算',
    options: [
      { label: '大数据开发工程师 (Spark/Hadoop)', value: '大数据开发工程师 (Spark/Hadoop)' },
      { label: '实时计算工程师 (Flink/Kafka)', value: '实时计算工程师 (Flink/Kafka)' },
      { label: '数据仓库与湖仓一体架构师', value: '数据仓库与湖仓一体架构师' },
      { label: '大数据基础平台运维专家', value: '大数据基础平台运维专家' }
    ]
  },
  {
    label: '六、数据库与存储中间件',
    options: [
      { label: 'MySQL DBA / 数据库内核开发', value: 'MySQL DBA / 数据库内核开发' },
      { label: '分布式存储研发工程师', value: '分布式存储研发工程师' },
      { label: '消息中间件专家 (RocketMQ/Kafka)', value: '消息中间件专家 (RocketMQ/Kafka)' }
    ]
  },
  {
    label: '七、云原生与运维 SRE',
    options: [
      { label: 'Kubernetes 云原生平台专家', value: 'Kubernetes 云原生平台专家' },
      { label: 'DevOps 与 CI/CD 平台专家', value: 'DevOps 与 CI/CD 平台专家' },
      { label: 'SRE 线上稳定性保障工程师', value: 'SRE 线上稳定性保障工程师' }
    ]
  },
  {
    label: '八、质量测试与网络安全',
    options: [
      { label: '自动化测试开发专家 (SDET)', value: '自动化测试开发专家 (SDET)' },
      { label: '性能压测与高可用调优专家', value: '性能压测与高可用调优专家' },
      { label: '网络安全与攻防渗透工程师', value: '网络安全与攻防渗透工程师' }
    ]
  }
]

const form = reactive({
  targetJob: 'Java 高级开发工程师',
  companyTarget: '阿里巴巴',
  interviewerStyle: 'p7_architect',
  totalTurns: 6,
  enableResumeCustomization: true
})

const styleOptions = [
  {
    value: 'p7_architect',
    icon: '⚡',
    name: '阿里 P7 架构师',
    desc: '极其犀利，直击汇编底层、并发锁状态升级、千万级大促线上排障。',
    quote: '在 10 万 QPS 峰值下，你这个方案会引发什么内存或线程灾难？'
  },
  {
    value: 'bytedance_tech',
    icon: '💻',
    name: '字节跳动技术专家',
    desc: '务实深究，看重手撕算法复杂度、数据结构设计与极端异常防御边界。',
    quote: '请推导该逻辑的最坏时间复杂度，写一下边界用例防御代码。'
  },
  {
    value: 'gentle_hr',
    icon: '🤝',
    name: '资深大厂 HRBP',
    desc: '敏锐洞察，考察自驱力、高压团队协作、技术方案权衡与职涯规划。',
    quote: '请用 STAR 原则复盘你经历过最痛苦的一次跨部门冲突与业务延期。'
  },
  {
    value: 'standard',
    icon: '⚖️',
    name: '大厂评审委员会',
    desc: '全面严谨，客观考核计算机核心网络、操作系统、分布式高可用设计。',
    quote: '请系统性对比两种架构选型的优劣势与长远运维成本。'
  }
]

const getStatusTag = (status) => {
  if (status === 2) return { type: 'success', text: '已完成' }
  if (status === 3) return { type: 'info', text: '已终止' }
  return { type: 'warning', text: '进行中' }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 16)
}

const fetchLinkedResume = async () => {
  try {
    const res = await getMyResume()
    if (res && res.code === 200 && res.data && res.data.id) {
      linkedResume.value = res.data
    }
  } catch (err) {
    console.warn('获取个人简历关联失败', err)
  }
}

const goToMyResume = () => {
  router.push('/personal/main/myResume')
}

const handleStartInterview = async () => {
  try {
    starting.value = true
    const res = await startInterview({
      targetJob: form.targetJob,
      companyTarget: form.companyTarget,
      interviewerStyle: form.interviewerStyle,
      totalTurns: form.totalTurns,
      resumeId: hasLinkedResume.value && form.enableResumeCustomization ? linkedResume.value.id : null,
      enableResumeCustomization: form.enableResumeCustomization
    })
    if (res && res.data && res.data.id) {
      ElMessage.success(
        hasLinkedResume.value && form.enableResumeCustomization
          ? '考场已生成！AI 面试官已锁定您的简历，将深度结合实际项目出题！'
          : '面试考场已生成，AI 面试官已就绪！'
      )
      router.push({ name: 'interviewRoom', params: { id: res.data.id } })
    } else {
      ElMessage.error(res.msg || '开启面试失败，请重试')
    }
  } catch (err) {
    ElMessage.error('开启面试异常：' + (err.message || '网络连接失败'))
  } finally {
    starting.value = false
  }
}

const loadMyHistory = async () => {
  try {
    loadingHistory.value = true
    const res = await getMyInterviews({ pageNum: 1, pageSize: 20 })
    if (res && res.rows) {
      historyList.value = res.rows
    }
  } catch (err) {
    console.error('加载历史战报失败:', err)
  } finally {
    loadingHistory.value = false
  }
}

const handleOpenSession = (item) => {
  if (item.status === 2) {
    router.push({ name: 'interviewReport', params: { id: item.id } })
  } else {
    router.push({ name: 'interviewRoom', params: { id: item.id } })
  }
}

onMounted(() => {
  if (route.query.fromResume === '1') {
    if (route.query.job) form.targetJob = route.query.job
    if (route.query.company) form.companyTarget = route.query.company
  }
  fetchLinkedResume()
  loadMyHistory()
})
</script>

<style scoped>
.interview-lobby-container {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 16px;
}

.lobby-banner {
  background: linear-gradient(135deg, #0d1b2a 0%, #1b263b 50%, #2b3a4a 100%);
  color: #fff;
  border-radius: 16px;
  padding: 36px 40px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 12px 32px rgba(13, 27, 42, 0.25);
}

.banner-content {
  max-width: 650px;
}

.banner-tag {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.tag-sub {
  font-size: 13px;
  color: #a0aec0;
}

.banner-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 12px 0;
  letter-spacing: 0.5px;
}

.banner-desc {
  font-size: 14px;
  color: #cbd5e0;
  line-height: 1.6;
  margin: 0;
}

.banner-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 16px 20px;
  text-align: center;
  backdrop-filter: blur(8px);
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #63b3ed;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #a0aec0;
}

.lobby-body {
  display: grid;
  grid-template-columns: 7fr 5fr;
  gap: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.resume-link-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 20px;
  gap: 12px;
}

.resume-link-status.linked {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.resume-link-status .status-left {
  flex: 1;
}

.resume-link-status .status-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.resume-link-status .resume-name {
  color: #0f766e;
}

.resume-link-status .status-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.resume-link-status .status-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.w-100 {
  width: 100%;
}

.company-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}

.style-card {
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: #f8fafc;
}

.style-card:hover {
  border-color: #3182ce;
  background: #ebf8ff;
}

.style-card.active {
  border-color: #3182ce;
  background: #ebf8ff;
  box-shadow: 0 4px 12px rgba(49, 130, 206, 0.18);
}

.style-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #2b6cb0;
  margin-bottom: 6px;
}

.style-desc {
  font-size: 12px;
  color: #4a5568;
  line-height: 1.5;
  margin-bottom: 6px;
}

.style-quote {
  font-size: 11px;
  color: #718096;
  font-style: italic;
  line-height: 1.4;
}

.form-action {
  margin-top: 24px;
}

.start-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #3182ce 0%, #2b6cb0 100%);
  border: none;
}

.history-card {
  height: fit-content;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 600px;
  overflow-y: auto;
}

.history-item {
  border: 1px solid #edf2f7;
  border-radius: 10px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.history-item:hover {
  border-color: #cbd5e0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transform: translateY(-1px);
}

.item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.job-tag {
  font-weight: 600;
  font-size: 14px;
  color: #1a202c;
}

.item-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #718096;
  margin-bottom: 8px;
}

.score-text {
  color: #e53e3e;
  font-weight: 700;
}

.item-report-badge {
  margin-bottom: 8px;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #a0aec0;
  border-top: 1px dashed #edf2f7;
  padding-top: 8px;
}
</style>
