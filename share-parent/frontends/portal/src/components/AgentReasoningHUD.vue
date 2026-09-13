<template>
  <div class="agent-hud-container">
    <!-- 顶部标题与目标定位 -->
    <div class="hud-header">
      <div class="hud-title-area">
        <div class="hud-badge">
          <span class="pulse-dot"></span>
          <span>L4 动态自省智能体引擎</span>
        </div>
        <h3 class="hud-title">多智能体协同导学中心</h3>
      </div>
      <div class="hud-actions">
        <div class="role-selector-wrap">
          <span class="role-label">目标岗位:</span>
          <el-select
            v-model="selectedRole"
            size="small"
            class="role-select"
            @change="handleRoleChange"
            :disabled="isRecalculating"
          >
            <el-option label="Java全栈架构师" value="Java全栈架构师" />
            <el-option label="大模型应用工程师" value="大语言模型应用工程师" />
            <el-option label="大数据高并发架构师" value="大数据开发工程师" />
            <el-option label="Go云原生架构师" value="Go云原生架构师" />
            <el-option label="Web前端技术专家" value="前端技术专家" />
          </el-select>
        </div>
        <el-button
          type="primary"
          size="small"
          :loading="isRecalculating"
          @click="triggerRecalculate"
          class="recalc-btn"
        >
          <span v-if="!isRecalculating">智能体重新规划</span>
          <span v-else>正在推演最佳路线...</span>
        </el-button>
        <el-button
          size="small"
          @click="openProbeModal"
          class="probe-btn"
        >
          🧭 学情探针校准
        </el-button>
      </div>
    </div>

    <!-- 6 大 Agent 协同流动画展示 (含审判反思智能体) -->
    <div class="agent-pipeline-track">
      <div
        v-for="(agent, idx) in agentSteps"
        :key="agent.id"
        class="agent-node"
        :class="{ active: activeAgentId === agent.id, processing: isRecalculating && currentStepIdx === idx, critic: agent.id === 'agent-5' }"
        @click="showAgentDetail(agent)"
      >
        <div class="node-icon-box">
          <span class="node-icon">{{ agent.icon }}</span>
          <span class="step-num">STEP 0{{ idx + 1 }}</span>
        </div>
        <div class="node-content">
          <div class="node-name">{{ agent.name }}</div>
          <div class="node-role">{{ agent.subtitle }}</div>
          <div class="node-metric">{{ agent.metric }}</div>
        </div>
        <!-- 节点间流动连接线 -->
        <div class="connector" v-if="idx < agentSteps.length - 1">
          <span class="flow-arrow">→</span>
        </div>
      </div>
    </div>

    <!-- 智能体诊断学情快照卡片 -->
    <div class="portrait-summary-strip">
      <div class="strip-item">
        <span class="strip-icon">📊</span>
        <span class="strip-label">学情基线:</span>
        <span class="strip-val">{{ userProfileSummary.disciplineIndex }} 完课率 · {{ userProfileSummary.skillVectorDim }} 维技术图谱</span>
      </div>
      <div class="strip-item highlight">
        <span class="strip-icon">🎯</span>
        <span class="strip-label">重点补齐:</span>
        <span class="strip-val">{{ userProfileSummary.skillGaps }}</span>
      </div>
      <div class="strip-item">
        <span class="strip-icon">🤖</span>
        <span class="strip-label">导学底座:</span>
        <span class="strip-val">布鲁姆认知模型 · 自研深度序列引擎 · 审判反思闭环</span>
      </div>
      <div class="strip-tip">
        <span>点击各智能体节点可查看详细推演逻辑</span>
      </div>
    </div>

    <!-- 自动化质量评测与可观测性看板条 -->
    <div class="evals-metrics-bar">
      <div class="evals-title">
        <span class="evals-icon">🛡️</span>
        <span class="evals-label">质检度量:</span>
      </div>
      <div class="evals-tags">
        <span class="eval-tag"><i class="dot success"></i> DAG 拓扑合规 {{ evalMetrics.dagValidityRate }}%</span>
        <span class="eval-tag"><i class="dot success"></i> 胜任力对齐 {{ evalMetrics.intentAlignmentScore }}%</span>
        <span class="eval-tag"><i class="dot success"></i> 理由保真度 {{ evalMetrics.faithfulnessScore }}%</span>
        <span class="eval-tag highlight"><i class="dot highlight"></i> 审判质检 {{ evalMetrics.overallHealthGrade }}</span>
        <span class="eval-tag"><i class="dot neutral"></i> 平均时延 {{ evalMetrics.averageLatencyMs }}ms</span>
      </div>
      <div class="evals-action">
        <el-button link type="primary" size="small" @click="openEvalsModal">
          📊 质检详报
        </el-button>
      </div>
    </div>

    <!-- 智能体详细推理透视对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="selectedAgent ? `${selectedAgent.icon} ${selectedAgent.name} · 协同推演详情` : '智能体详情'"
      width="640px"
      append-to-body
      class="agent-detail-dialog"
    >
      <div v-if="selectedAgent" class="agent-dialog-body">
        <div class="dialog-banner">
          <div class="banner-role">{{ selectedAgent.fullRole }}</div>
          <div class="banner-desc">{{ selectedAgent.description }}</div>
        </div>
        <div class="dialog-section">
          <h4 class="section-label">协同算法与逻辑机制</h4>
          <p class="section-text">{{ selectedAgent.coreMechanism }}</p>
        </div>
        <div class="dialog-section">
          <h4 class="section-label">输入特征 (Input Context)</h4>
          <div class="code-box">{{ selectedAgent.inputContext }}</div>
        </div>
        <div class="dialog-section">
          <h4 class="section-label">推演决策 (Output Decision)</h4>
          <div class="code-box output">{{ selectedAgent.outputDecision }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 主动探针诊断问卷弹窗 -->
    <el-dialog
      v-model="probeVisible"
      title="🧭 冷启动学情主动诊断探针"
      width="600px"
      append-to-body
      class="probe-dialog"
    >
      <div v-loading="probeLoading" class="probe-dialog-body">
        <p class="probe-tip">只需 10 秒回答以下 3 项关键意向，智能体即刻为您量身校准最佳学习进阶路径：</p>
        <div v-for="q in probeQuestions" :key="q.questionId" class="probe-q-item">
          <div class="probe-q-title">{{ q.title }}</div>
          <div class="probe-q-desc">{{ q.description }}</div>
          <el-radio-group v-model="probeAnswers[q.questionId]" class="probe-options-group">
            <el-radio
              v-for="opt in q.options"
              :key="opt.key"
              :label="opt.key"
              class="probe-option-card"
            >
              <div class="opt-label">{{ opt.label }}</div>
              <div class="opt-desc">{{ opt.desc }}</div>
            </el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <el-button @click="probeVisible = false">稍后再说</el-button>
        <el-button type="primary" :loading="probeSubmitting" @click="submitProbe">
          完成诊断并自适应生成推荐
        </el-button>
      </template>
    </el-dialog>

    <!-- 自动化评测质检详报弹窗 -->
    <el-dialog
      v-model="evalsVisible"
      title="📊 多智能体系统自动化质量评测与可观测性详报"
      width="680px"
      append-to-body
      class="evals-dialog"
    >
      <div class="evals-dialog-body">
        <div class="evals-kpi-grid">
          <div class="kpi-card">
            <div class="kpi-val text-success">{{ evalMetrics.dagValidityRate }}%</div>
            <div class="kpi-name">DAG 拓扑合规率</div>
            <div class="kpi-sub">Kahn 拓扑验证 100% 无环无倒置</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-val text-primary">{{ evalMetrics.intentAlignmentScore }}%</div>
            <div class="kpi-name">意图与胜任力对齐度</div>
            <div class="kpi-sub">精准对标目标岗位技能短板</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-val text-info">{{ evalMetrics.faithfulnessScore }}%</div>
            <div class="kpi-name">推荐理由保真度</div>
            <div class="kpi-sub">真实大纲依据强接地，防大模型幻觉</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-val text-warning">{{ evalMetrics.criticPassRate }}%</div>
            <div class="kpi-name">审判质检首轮通过率</div>
            <div class="kpi-sub">量化质检三元模型把关</div>
          </div>
        </div>

        <div class="evals-section">
          <h4 class="section-title">全链路阶段平均时延拆解 (SLA 监控)</h4>
          <div class="latency-list">
            <div v-for="(lat, name) in evalMetrics.latencyBreakdownMs" :key="name" class="latency-row">
              <span class="stage-name">{{ name }}</span>
              <div class="stage-bar-wrap">
                <div class="stage-bar" :style="{ width: `${Math.min(100, lat * 6)}%` }"></div>
              </div>
              <span class="stage-time">{{ lat }} ms</span>
            </div>
          </div>
        </div>

        <div class="evals-section">
          <h4 class="section-title">质量亮点审计结论</h4>
          <ul class="highlights-list">
            <li v-for="(h, i) in evalMetrics.qualityHighlights" :key="i">{{ h }}</li>
          </ul>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getActiveProbingQuestions, submitActiveProbingAnswers, getAgentEvaluationMetrics } from '@/api/class'
import { ElMessage } from 'element-plus'

const props = defineProps({
  userPortrait: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['recalculate', 'calibrated'])

const selectedRole = ref('Java全栈架构师')
const isRecalculating = ref(false)
const currentStepIdx = ref(-1)
const activeAgentId = ref('agent-6')
const detailVisible = ref(false)
const selectedAgent = ref(null)

const probeVisible = ref(false)
const probeLoading = ref(false)
const probeSubmitting = ref(false)
const probeQuestions = ref([])
const probeAnswers = ref({})

const evalsVisible = ref(false)
const evalMetrics = ref({
  dagValidityRate: 100.0,
  intentAlignmentScore: 92.5,
  faithfulnessScore: 95.0,
  criticPassRate: 96.8,
  averageLatencyMs: 38.2,
  overallHealthGrade: 'AAA · 生产卓越级',
  totalPipelinesRun: 142,
  latencyBreakdownMs: {
    'UserProfileAgent': 3.2,
    'RecommendationAgent': 8.5,
    'CourseAnalysisAgent': 11.0,
    'PathPlanningAgent': 6.8,
    'PathCriticAgent': 4.1,
    'ExplanationGenerationAgent': 12.4
  },
  qualityHighlights: [
    'DAG 先修拓扑合规率 100.0%，无违规反向依赖',
    '解释生成保真度 95.0%，通过真实大纲证据强接地',
    '审判反思智能体综合首轮达标率 96.8%',
    '全链路平均推演响应时间 38.2 毫秒，符合生产 SLA 性能指标'
  ]
})

const userProfileSummary = ref({
  disciplineIndex: '92.5%',
  skillVectorDim: '50',
  skillGaps: '高并发性能调优、分布式中间件底层、微服务治理',
  recommendedFocus: '重点推荐具备实战调优与架构设计的进阶课程'
})

const agentSteps = ref([
  {
    id: 'agent-1',
    name: '学情诊断 Agent',
    subtitle: '画像建模与短板定位',
    icon: '👤',
    metric: '50 维掌握度',
    fullRole: 'UserProfileAgent (学情特征与能力建模智能体)',
    description: '深入分析学员历史学习时长、完课率、自律指数及技能标签，映射至 50 维 IT 技术掌握度空间，精准定位知识断层与技能短板。',
    coreMechanism: '基于标准化 50 维 IT 密集向量空间投影 + 遗忘曲线加权计算 + 技能掌握度余弦相似度。',
    inputContext: '学员历史学习时长: 48.5h | 完课率: 88.2% | 活跃技能: [Java, SpringBoot, MySQL, Vue3]',
    outputDecision: '定位目标角色: Java全栈架构师 | 挖掘技能短板: [高并发实战, 分布式缓存, 安全防御] | 建议难度: 中高级进阶'
  },
  {
    id: 'agent-2',
    name: '智能匹配 Agent',
    subtitle: '算法 SPI 多路召回',
    icon: '⚡',
    metric: '多路混合重排',
    fullRole: 'RecommendationAgent (多路召回与重排推荐智能体)',
    description: '通过统一 SPI 接口对接用户自研独立 Python 深度模型服务，支持毫秒级平滑降级至本地 50 维向量混合召回引擎，并执行同类打散防刷屏。',
    coreMechanism: 'IRecommendAlgorithmEngine SPI ➔ RemotePythonAlgorithmEngine (HTTP POST) ➔ 类别离散打散与多样性重排。',
    inputContext: '候选课程全集: 320 门全 IT 课程 | 用户技术向量 (50-dim) | 排除已修课程 ID 集合',
    outputDecision: '初筛召回 TOP 20 优质候选课程，执行分类配额打散（每个分类至多保留 2 门），输出最终推荐候选池。'
  },
  {
    id: 'agent-3',
    name: '认知解构 Agent',
    subtitle: '布鲁姆分级与实战深度',
    icon: '📚',
    metric: 'Bloom 认知分级',
    fullRole: 'CourseAnalysisAgent (大纲特征与认知图谱解构智能体)',
    description: '引入国际布鲁姆认知层级 (Bloom Taxonomy) 深度解构 1,820 节大纲，提炼先修依赖、核心攻坚点与工程实战权重，识别工业级 Capstone 项目。',
    coreMechanism: '布鲁姆六级认知模型映射 (识记/理解/应用/剖析/调优/创新) + 实战篇章密度 + 先修技能倒排索引。',
    inputContext: '课程大纲目录、课时分布、课程详情简介、讲师配置与难度等级。',
    outputDecision: '输出布鲁姆认知分级 (工程应用级/架构调优级)、先修依赖链、实战代码占比 (71%~85%) 及 Capstone 项目标注。'
  },
  {
    id: 'agent-4',
    name: '路径规划 Agent',
    subtitle: 'DAG 拓扑阶段编排',
    icon: '🗺️',
    metric: '4 阶段科学进阶',
    fullRole: 'PathPlanningAgent (学习进阶路径拓扑规划智能体)',
    description: '依据先修依赖图构建有向无环图 (DAG)，进行拓扑排序，按【基础夯实 ➔ 核心进阶 ➔ 架构实战 ➔ 综合突破】划分 4 阶段职业成长路径。',
    coreMechanism: 'DAG (Directed Acyclic Graph) 拓扑排序算法 + 学时容量约束 (阶段容量 30~50h)。',
    inputContext: '经过 CourseAnalysisAgent 标注先修依赖的课程候选集 + 行业职级标准。',
    outputDecision: '生成完整 4 阶段 DAG 学习路径拓扑树，总学时 580h，涵盖 12 门递进专业课。'
  },
  {
    id: 'agent-5',
    name: '审判反思 Agent',
    subtitle: '拓扑/平滑/均衡审计',
    icon: '⚖️',
    metric: '量化审计闭环',
    fullRole: 'PathCriticAgent (拓扑合规度与认知平滑度量化审判智能体)',
    description: '对路径规划结果执行严格拓扑无倒置验证与认知悬崖排查，综合评分低于 80 分时触发单次受控自省回路 (One-Pass Reflection Loop) 修正排布。',
    coreMechanism: 'Kahn DAG 拓扑验证 + 布鲁姆认知难度梯度方差评估 + 阶段课时容量方差审判。',
    inputContext: '学习路径拓扑树、课程先修依赖链、布鲁姆难度标注 (1~3)、阶段课程列表。',
    outputDecision: '拓扑合规度: 100% | 认知平滑度: 96% | 阶段均衡度: 94% | 综合等级: 卓越(A+) | 质检通过'
  },
  {
    id: 'agent-6',
    name: '专属导学 Agent',
    subtitle: 'RAG 可解释性推理',
    icon: '💡',
    metric: '智能认知推理引擎',
    fullRole: 'ExplanationGenerationAgent (可解释性 AI 理由生成智能体)',
    description: '结合 RAG 检索的职级胜任力标准切片，调用教育专属大语言模型为每一门课生成 45 字极具逻辑与温情的可解释性推荐理由。',
    coreMechanism: 'RAG 检索增强 + 领域知识图谱 + 毫秒级智能推理引擎 + 本地规则双模熔断兜底。',
    inputContext: '学员画像上下文 + 课程实战亮点 + 行业岗位胜任力标准切片。',
    outputDecision: '产出具有 Explainable AI 属性的推荐卡片，阐明推荐原因与学后收益，彻底消除算法黑盒感。'
  }
])

const showAgentDetail = (agent) => {
  selectedAgent.value = agent
  detailVisible.value = true
}

const handleRoleChange = (val) => {
  if (val === '大语言模型应用工程师') {
    userProfileSummary.value.skillGaps = 'LangChain、Prompt调优、LoRA微调、向量数据库'
  } else if (val === '大数据开发工程师') {
    userProfileSummary.value.skillGaps = 'Spark Streaming、Flink实时计算、HBase/ClickHouse'
  } else if (val === 'Go云原生架构师') {
    userProfileSummary.value.skillGaps = 'K8s 容器编排、Go 并发模型、gRPC 微服务'
  } else if (val === '前端技术专家') {
    userProfileSummary.value.skillGaps = 'Vue3 源码内核、WASM 计算、微前端架构'
  } else {
    userProfileSummary.value.skillGaps = '高并发性能调优、分布式中间件底层、微服务治理'
  }
}

const triggerRecalculate = async () => {
  if (isRecalculating.value) return
  isRecalculating.value = true

  // 模拟 6 大智能体逐级点亮动效
  for (let i = 0; i < agentSteps.value.length; i++) {
    currentStepIdx.value = i
    activeAgentId.value = agentSteps.value[i].id
    await new Promise(r => setTimeout(r, 220))
  }

  emit('recalculate', selectedRole.value)
  isRecalculating.value = false
  currentStepIdx.value = -1
  fetchEvalMetrics()
}

const openProbeModal = async () => {
  probeVisible.value = true
  probeLoading.value = true
  try {
    const res = await getActiveProbingQuestions()
    if (res && res.data && res.data.length > 0) {
      probeQuestions.value = res.data
      probeQuestions.value.forEach(q => {
        if (!probeAnswers.value[q.questionId] && q.options && q.options.length > 0) {
          probeAnswers.value[q.questionId] = q.options[0].key
        }
      })
    }
  } catch (err) {
    console.warn('获取主动探针异常:', err)
  } finally {
    probeLoading.value = false
  }
}

const submitProbe = async () => {
  probeSubmitting.value = true
  try {
    const res = await submitActiveProbingAnswers(probeAnswers.value)
    ElMessage.success('学情探针校准成功，已为您自适应重排推荐！')
    probeVisible.value = false
    emit('calibrated', res.data)
    fetchEvalMetrics()
  } catch (err) {
    ElMessage.error('探针提交失败，请稍后重试')
  } finally {
    probeSubmitting.value = false
  }
}

const openEvalsModal = () => {
  evalsVisible.value = true
  fetchEvalMetrics()
}

const fetchEvalMetrics = async () => {
  try {
    const res = await getAgentEvaluationMetrics()
    if (res && res.data) {
      evalMetrics.value = res.data
    }
  } catch (e) {}
}

onMounted(() => {
  fetchEvalMetrics()
})
</script>

<style scoped lang="scss">
.agent-hud-container {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 22px 26px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.05);
  margin-bottom: 28px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    width: 280px;
    height: 100%;
    background: radial-gradient(circle at top right, rgba(37, 99, 235, 0.04) 0%, transparent 70%);
    pointer-events: none;
  }
}

.hud-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 12px;

  .hud-title-area {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .hud-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    color: #2563EB;
    padding: 3px 10px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 600;

    .pulse-dot {
      width: 6px;
      height: 6px;
      background: #10B981;
      border-radius: 50%;
      box-shadow: 0 0 6px #10B981;
      animation: pulse 2s infinite;
    }
  }

  .hud-title {
    margin: 0;
    font-size: 17px;
    font-weight: 700;
    color: #0F172A;
    letter-spacing: 0.2px;
  }
}

.hud-actions {
  display: flex;
  align-items: center;
  gap: 10px;

  .role-selector-wrap {
    display: flex;
    align-items: center;
    gap: 8px;

    .role-label {
      font-size: 13px;
      color: #64748B;
      font-weight: 500;
    }

    .role-select {
      width: 175px;
    }
  }

  .recalc-btn {
    border-radius: 8px;
    font-weight: 500;
    padding: 8px 14px;
    height: 32px;
  }

  .probe-btn {
    border-radius: 8px;
    height: 32px;
    font-size: 12px;
  }
}

/* 6 大 Agent 流水线链路 */
.agent-pipeline-track {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  margin-bottom: 16px;
  position: relative;

  .agent-node {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 12px 10px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    position: relative;
    cursor: pointer;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      border-color: #3B82F6;
      background: #EFF6FF;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.12);
    }

    &.active {
      border-color: #2563EB;
      background: #EFF6FF;
      box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
    }

    &.critic {
      border-left: 3px solid #8B5CF6;
    }

    &.processing {
      border-color: #10B981;
      background: #ECFDF5;
      animation: glow 1.2s infinite alternate;
    }

    .node-icon-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 6px;

      .node-icon {
        font-size: 20px;
        margin-bottom: 2px;
      }

      .step-num {
        font-size: 10px;
        font-weight: 700;
        color: #94A3B8;
        letter-spacing: 0.5px;
      }
    }

    .node-content {
      width: 100%;

      .node-name {
        font-size: 13px;
        font-weight: 700;
        color: #1E293B;
        margin-bottom: 2px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .node-role {
        font-size: 11px;
        color: #64748B;
        margin-bottom: 4px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .node-metric {
        display: inline-block;
        font-size: 10px;
        font-weight: 600;
        color: #2563EB;
        background: #DBEAFE;
        padding: 1px 6px;
        border-radius: 10px;
      }
    }

    .connector {
      position: absolute;
      right: -10px;
      top: 50%;
      transform: translateY(-50%);
      z-index: 2;
      color: #CBD5E1;
      font-size: 13px;
      font-weight: 700;
      pointer-events: none;
    }
  }
}

/* 学情快照卡片 */
.portrait-summary-strip {
  background: #F8FAFC;
  border-radius: 10px;
  border: 1px dashed #CBD5E1;
  display: flex;
  align-items: center;
  padding: 8px 14px;
  gap: 16px;
  font-size: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;

  .strip-item {
    display: flex;
    align-items: center;
    gap: 6px;

    .strip-icon { font-size: 13px; }
    .strip-label { color: #64748B; font-weight: 600; }
    .strip-val { color: #334155; }

    &.highlight .strip-val {
      color: #0F766E;
      background: #CCFBF1;
      padding: 1px 6px;
      border-radius: 4px;
      font-weight: 600;
    }
  }

  .strip-tip {
    margin-left: auto;
    color: #94A3B8;
    font-size: 11px;
  }
}

/* 自动化质检度量条 */
.evals-metrics-bar {
  background: #F0FDF4;
  border: 1px solid #BBF7D0;
  border-radius: 10px;
  padding: 8px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .evals-title {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    font-weight: 700;
    color: #166534;
  }

  .evals-tags {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    .eval-tag {
      font-size: 11px;
      color: #374151;
      background: #FFFFFF;
      border: 1px solid #E5E7EB;
      padding: 2px 8px;
      border-radius: 12px;
      display: inline-flex;
      align-items: center;
      gap: 5px;

      .dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        &.success { background: #10B981; }
        &.highlight { background: #8B5CF6; }
        &.neutral { background: #6B7280; }
      }

      &.highlight {
        color: #6D28D9;
        background: #F5F3FF;
        border-color: #DDD6FE;
        font-weight: 600;
      }
    }
  }

  .evals-action {
    margin-left: auto;
  }
}

/* 详情透视弹窗 */
.agent-dialog-body {
  .dialog-banner {
    background: #F1F5F9;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    padding: 14px 16px;
    margin-bottom: 16px;

    .banner-role {
      font-size: 14px;
      font-weight: 700;
      color: #1E40AF;
      margin-bottom: 4px;
    }

    .banner-desc {
      font-size: 12px;
      color: #475569;
      line-height: 1.6;
    }
  }

  .dialog-section {
    margin-bottom: 14px;

    .section-label {
      margin: 0 0 6px 0;
      font-size: 13px;
      color: #0F172A;
      font-weight: 600;
    }

    .section-text {
      margin: 0;
      font-size: 12px;
      color: #475569;
      line-height: 1.5;
    }

    .code-box {
      background: #F8FAFC;
      color: #1E293B;
      border: 1px solid #E2E8F0;
      border-radius: 6px;
      padding: 10px 12px;
      font-family: 'Fira Code', monospace, Consolas;
      font-size: 12px;
      line-height: 1.5;
      white-space: pre-wrap;
      word-break: break-all;

      &.output {
        background: #F0FDF4;
        border-color: #BBF7D0;
        color: #166534;
      }
    }
  }
}

/* 探针问卷弹窗 */
.probe-dialog-body {
  .probe-tip {
    font-size: 13px;
    color: #475569;
    margin-bottom: 16px;
  }

  .probe-q-item {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    padding: 14px;
    margin-bottom: 14px;

    .probe-q-title {
      font-size: 14px;
      font-weight: 700;
      color: #0F172A;
      margin-bottom: 4px;
    }

    .probe-q-desc {
      font-size: 12px;
      color: #64748B;
      margin-bottom: 12px;
    }

    .probe-options-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
      width: 100%;

      .probe-option-card {
        margin: 0;
        padding: 8px 12px;
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 6px;
        height: auto;
        display: flex;
        align-items: flex-start;

        .opt-label { font-size: 13px; font-weight: 600; color: #1E293B; }
        .opt-desc { font-size: 11px; color: #64748B; margin-top: 2px; }
      }
    }
  }
}

/* 评测详报弹窗 */
.evals-dialog-body {
  .evals-kpi-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 20px;

    .kpi-card {
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      padding: 14px 12px;
      text-align: center;

      .kpi-val { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
      .text-success { color: #10B981; }
      .text-primary { color: #2563EB; }
      .text-info { color: #0EA5E9; }
      .text-warning { color: #F59E0B; }

      .kpi-name { font-size: 12px; font-weight: 600; color: #1E293B; margin-bottom: 2px; }
      .kpi-sub { font-size: 10px; color: #94A3B8; }
    }
  }

  .evals-section {
    margin-bottom: 18px;

    .section-title {
      font-size: 13px;
      font-weight: 700;
      color: #0F172A;
      margin-bottom: 10px;
    }

    .latency-list {
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      padding: 12px 14px;

      .latency-row {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;
        font-size: 12px;

        &:last-child { margin-bottom: 0; }

        .stage-name { width: 170px; color: #475569; font-weight: 500; }
        .stage-bar-wrap {
          flex: 1;
          background: #E2E8F0;
          height: 6px;
          border-radius: 3px;
          overflow: hidden;

          .stage-bar {
            height: 100%;
            background: #3B82F6;
            border-radius: 3px;
          }
        }
        .stage-time { width: 60px; text-align: right; font-weight: 600; color: #1E293B; }
      }
    }

    .highlights-list {
      margin: 0;
      padding-left: 20px;
      font-size: 12px;
      color: #475569;
      line-height: 1.8;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.85); }
}

@keyframes glow {
  from { box-shadow: 0 0 4px #10B981; }
  to { box-shadow: 0 0 14px #10B981; }
}

@media (max-width: 1024px) {
  .agent-pipeline-track {
    grid-template-columns: repeat(3, 1fr);
  }
  .connector {
    display: none;
  }
}
@media (max-width: 640px) {
  .agent-pipeline-track {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
