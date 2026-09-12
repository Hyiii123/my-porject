<template>
  <div class="agent-hud-container">
    <!-- 顶部标题与目标定位 -->
    <div class="hud-header">
      <div class="hud-title-area">
        <div class="hud-badge">
          <span class="pulse-dot"></span>
          <span>AI 智能学习引擎</span>
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
      </div>
    </div>

    <!-- 5 大 Agent 协同流动画展示 -->
    <div class="agent-pipeline-track">
      <div
        v-for="(agent, idx) in agentSteps"
        :key="agent.id"
        class="agent-node"
        :class="{ active: activeAgentId === agent.id, processing: isRecalculating && currentStepIdx === idx }"
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
        <span class="strip-val">自研深度学习引擎 · 行业胜任力图谱</span>
      </div>
      <div class="strip-tip">
        <span>点击节点可查看详细推演逻辑</span>
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
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  userPortrait: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['recalculate'])

const selectedRole = ref('Java全栈架构师')
const isRecalculating = ref(false)
const currentStepIdx = ref(-1)
const activeAgentId = ref('agent-5')
const detailVisible = ref(false)
const selectedAgent = ref(null)

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
    name: '能力拆解 Agent',
    subtitle: '大纲与实战深度解构',
    icon: '📚',
    metric: '先修依赖图谱',
    fullRole: 'CourseAnalysisAgent (大纲特征与先修图谱解构智能体)',
    description: '穿透全站 1,820 节大纲目录，解构章节知识点，计算实战代码占比（PracticalWeight），评估课程间的前置依赖约束。',
    coreMechanism: '正则与自然语言知识点抽提 + 实战篇章密度统计 + 先修技能倒排索引。',
    inputContext: '课程大纲目录、课时分布、课程详情简介、讲师配置与难度等级。',
    outputDecision: '输出课程核心知识图谱切片、先修前置依赖链 (如: 掌握 Java/MySQL 才可学习高并发)、实战比例评估 (71%~85%)。'
  },
  {
    id: 'agent-4',
    name: '路径规划 Agent',
    subtitle: '阶段化渐进式编排',
    icon: '🗺️',
    metric: '4 阶段科学成长',
    fullRole: 'PathPlanningAgent (学习进阶路径拓扑规划智能体)',
    description: '依据先修依赖图构建有向无环图 (DAG)，进行拓扑排序，按【基础夯实 ➔ 核心进阶 ➔ 架构实战 ➔ 综合突破】划分 4 阶段职业成长路径。',
    coreMechanism: 'DAG (Directed Acyclic Graph) 拓扑排序算法 + 学时容量约束 (阶段容量 30~50h)。',
    inputContext: '经过 CourseAnalysisAgent 标注先修依赖的课程候选集 + 行业职级标准。',
    outputDecision: '生成完整 4 阶段 DAG 学习路径拓扑树，总学时 580h，涵盖 12 门进阶递进专业课。'
  },
  {
    id: 'agent-5',
    name: '专属导学 Agent',
    subtitle: '可解释性智能推理',
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

  // 模拟 5 大智能体逐级点亮动效
  for (let i = 0; i < agentSteps.value.length; i++) {
    currentStepIdx.value = i
    activeAgentId.value = agentSteps.value[i].id
    await new Promise(r => setTimeout(r, 260))
  }

  emit('recalculate', selectedRole.value)
  isRecalculating.value = false
  currentStepIdx.value = -1
}
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
  gap: 12px;

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
      width: 190px;
    }
  }

  .recalc-btn {
    border-radius: 8px;
    font-weight: 500;
    padding: 8px 16px;
    height: 32px;
  }
}

/* 5 大 Agent 流水线链路 */
.agent-pipeline-track {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 18px;
  position: relative;

  .agent-node {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 14px;
    cursor: pointer;
    transition: all 0.25s ease;
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 6px;

    &:hover {
      background: #FFFFFF;
      border-color: #93C5FD;
      box-shadow: 0 6px 16px -2px rgba(37, 99, 235, 0.08);
      transform: translateY(-2px);
    }

    &.active {
      border-color: #3B82F6;
      background: #EFF6FF;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.12);

      .node-name {
        color: #1D4ED8;
      }
    }

    &.processing {
      border-color: #10B981;
      background: #ECFDF5;
      animation: glow 0.8s infinite alternate;
    }

    .node-icon-box {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .node-icon {
        font-size: 18px;
      }

      .step-num {
        font-size: 11px;
        color: #94A3B8;
        font-weight: 700;
        letter-spacing: 0.5px;
      }
    }

    .node-name {
      font-size: 13px;
      font-weight: 700;
      color: #1E293B;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      transition: color 0.2s;
    }

    .node-role {
      font-size: 11px;
      color: #64748B;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .node-metric {
      font-size: 10px;
      color: #2563EB;
      background: #DBEAFE;
      padding: 2px 7px;
      border-radius: 4px;
      width: fit-content;
      font-weight: 500;
    }

    .connector {
      position: absolute;
      right: -10px;
      top: 50%;
      transform: translateY(-50%);
      color: #CBD5E1;
      font-size: 12px;
      pointer-events: none;
      z-index: 2;
    }
  }
}

/* 学情快照横条 */
.portrait-summary-strip {
  display: flex;
  align-items: center;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 10px 16px;
  gap: 20px;
  font-size: 12px;
  flex-wrap: wrap;

  .strip-item {
    display: flex;
    align-items: center;
    gap: 6px;

    .strip-icon {
      font-size: 13px;
    }

    .strip-label {
      color: #64748B;
      font-weight: 600;
    }

    .strip-val {
      color: #334155;
    }

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

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.85); }
}

@keyframes glow {
  from { box-shadow: 0 0 4px #10B981; }
  to { box-shadow: 0 0 14px #10B981; }
}

@media (max-width: 992px) {
  .agent-pipeline-track {
    grid-template-columns: repeat(2, 1fr);
  }
  .connector {
    display: none;
  }
}
</style>
