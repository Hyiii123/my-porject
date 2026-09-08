<template>
  <div class="agent-hud-container">
    <!-- 顶部标题与状态徽章 -->
    <div class="hud-header">
      <div class="hud-title-area">
        <div class="hud-badge">
          <span class="pulse-dot"></span>
          <span>Multi-Agent Live Pipeline</span>
        </div>
        <h3 class="hud-title">智问学伴 · AI 多智能体协同决策流水线</h3>
      </div>
      <div class="hud-actions">
        <div class="role-selector-wrap">
          <span class="role-label">🎯 目标岗位定位:</span>
          <el-select
            v-model="selectedRole"
            size="small"
            class="role-select"
            @change="handleRoleChange"
            :disabled="isRecalculating"
          >
            <el-option label="Java全栈架构师 (P6+/P7)" value="Java全栈架构师" />
            <el-option label="大语言模型 (LLM) 应用工程师" value="大语言模型应用工程师" />
            <el-option label="大数据高并发开发工程师" value="大数据开发工程师" />
            <el-option label="Go云原生微服务架构师" value="Go云原生架构师" />
            <el-option label="现代 Web 前端性能专家" value="前端技术专家" />
          </el-select>
        </div>
        <el-button
          type="primary"
          size="small"
          :loading="isRecalculating"
          @click="triggerRecalculate"
          class="recalc-btn"
        >
          <span v-if="!isRecalculating">⚡ 智能体重新推演</span>
          <span v-else>5 大智能体推演中...</span>
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
          <span class="step-num">0{{ idx + 1 }}</span>
        </div>
        <div class="node-content">
          <div class="node-name">{{ agent.name }}</div>
          <div class="node-role">{{ agent.subtitle }}</div>
          <div class="node-metric">{{ agent.metric }}</div>
        </div>
        <!-- 节点间流动连接线 -->
        <div class="connector" v-if="idx < agentSteps.length - 1">
          <span class="flow-arrow">➔</span>
        </div>
      </div>
    </div>

    <!-- 智能体诊断学情快照卡片 -->
    <div class="portrait-summary-strip">
      <div class="strip-item">
        <span class="strip-icon">📊</span>
        <span class="strip-label">学情画像诊断:</span>
        <span class="strip-val">{{ userProfileSummary.disciplineIndex }} 完课指数 · {{ userProfileSummary.skillVectorDim }} 维技术空间</span>
      </div>
      <div class="strip-item highlight">
        <span class="strip-icon">🔍</span>
        <span class="strip-label">AI 短板挖掘:</span>
        <span class="strip-val">{{ userProfileSummary.skillGaps }}</span>
      </div>
      <div class="strip-item">
        <span class="strip-icon">🧠</span>
        <span class="strip-label">解释生成底座:</span>
        <span class="strip-val">GPT-5.4-Mini + 软考/大厂P7知识图谱</span>
      </div>
      <div class="strip-tip">
        <span>💡 点击任意智能体节点可透视推理细节</span>
      </div>
    </div>

    <!-- 智能体详细推理透视对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="selectedAgent ? `${selectedAgent.icon} ${selectedAgent.name} · 内部推理透视` : '智能体详情'"
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
          <h4 class="section-label">⚙️ 核心算法与协同机制</h4>
          <p class="section-text">{{ selectedAgent.coreMechanism }}</p>
        </div>
        <div class="dialog-section">
          <h4 class="section-label">📥 智能体输入 (Input Context)</h4>
          <div class="code-box">{{ selectedAgent.inputContext }}</div>
        </div>
        <div class="dialog-section">
          <h4 class="section-label">📤 推演产出 (Output Decision)</h4>
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
    name: '用户画像 Agent',
    subtitle: '学情诊断与短板挖掘',
    icon: '👤',
    metric: '50 维技术掌握度',
    fullRole: 'UserProfileAgent (学情特征与能力建模智能体)',
    description: '深入分析学员历史学习时长、完课率、自律指数及技能标签，映射至 50 维 IT 技术掌握度空间，精准定位知识断层与技能短板。',
    coreMechanism: '基于标准化 50 维 IT 密集向量空间投影 + 遗忘曲线加权计算 + 技能掌握度余弦相似度。',
    inputContext: '学员历史学习时长: 48.5h | 完课率: 88.2% | 活跃技能: [Java, SpringBoot, MySQL, Vue3]',
    outputDecision: '定位目标角色: Java全栈架构师 | 挖掘技能短板: [高并发实战, 分布式缓存, 安全防御] | 建议难度: 中高级进阶'
  },
  {
    id: 'agent-2',
    name: '算法推荐 Agent',
    subtitle: '解耦自研算法 SPI',
    icon: '⚡',
    metric: '自研 Python / 50维余弦',
    fullRole: 'RecommendationAgent (多路召回与重排推荐智能体)',
    description: '通过统一 SPI 接口对接用户自研独立 Python 深度模型服务，支持毫秒级平滑降级至本地 50 维向量混合召回引擎，并执行同类打散防刷屏。',
    coreMechanism: 'IRecommendAlgorithmEngine SPI ➔ RemotePythonAlgorithmEngine (HTTP POST) ➔ 类别离散打散与多样性重排。',
    inputContext: '候选课程全集: 320 门全 IT 课程 | 用户技术向量 (50-dim) | 排除已修课程 ID 集合',
    outputDecision: '初筛召回 TOP 20 优质候选课程，执行分类配额打散（每个分类至多保留 2 门），输出最终推荐候选池。'
  },
  {
    id: 'agent-3',
    name: '课程分析 Agent',
    subtitle: '1820 节大纲解构',
    icon: '📚',
    metric: '先修依赖与实战占比',
    fullRole: 'CourseAnalysisAgent (大纲特征与先修图谱解构智能体)',
    description: '穿透全站 1,820 节大纲目录，解构章节知识点，计算实战代码占比（PracticalWeight），评估课程间的前置依赖约束。',
    coreMechanism: '正则与自然语言知识点抽提 + 实战篇章密度统计 + 先修技能倒排索引。',
    inputContext: '课程大纲目录、课时分布、课程详情简介、讲师配置与难度等级。',
    outputDecision: '输出课程核心知识图谱切片、先修前置依赖链 (如: 掌握 Java/MySQL 才可学习高并发)、实战比例评估 (71%~85%)。'
  },
  {
    id: 'agent-4',
    name: '路径规划 Agent',
    subtitle: 'DAG 拓扑阶段编排',
    icon: '🗺️',
    metric: '4 阶段渐进式成长',
    fullRole: 'PathPlanningAgent (学习进阶路径拓扑规划智能体)',
    description: '依据先修依赖图构建有向无环图 (DAG)，进行拓扑排序，按【基础夯实 ➔ 核心进阶 ➔ 架构实战 ➔ 综合突破】划分 4 阶段职业成长路径。',
    coreMechanism: 'DAG (Directed Acyclic Graph) 拓扑排序算法 + 学时容量约束 (阶段容量 30~50h)。',
    inputContext: '经过 CourseAnalysisAgent 标注先修依赖的课程候选集 + 行业职级标准。',
    outputDecision: '生成完整 4 阶段 DAG 学习路径拓扑树，总学时 580h，涵盖 12 门进阶递进专业课。'
  },
  {
    id: 'agent-5',
    name: '解释生成 Agent',
    subtitle: '大模型可解释依据',
    icon: '💡',
    metric: 'GPT-5.4 实时推演',
    fullRole: 'ExplanationGenerationAgent (可解释性 AI 理由生成智能体)',
    description: '结合 RAG 检索的阿里巴巴 P6/P7 职级胜任力标准切片，调用大模型（GPT-5.4-mini）为每一门课生成 45 字极具逻辑与温情的可解释性推荐理由。',
    coreMechanism: 'RAG 检索增强 + Prompt 约束 + GPT-5.4-mini 毫秒级推理 + 本地规则双模熔断兜底。',
    inputContext: '学员画像上下文 + 课程实战亮点 + 阿里 P6/P7 胜任力标准切片。',
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
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  border-radius: 12px;
  padding: 20px 24px;
  color: #F8FAFC;
  box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.4), 0 8px 10px -6px rgba(15, 23, 42, 0.3);
  margin-bottom: 24px;
  border: 1px solid rgba(56, 189, 248, 0.2);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    width: 320px;
    height: 100%;
    background: radial-gradient(circle, rgba(56, 189, 248, 0.08) 0%, transparent 70%);
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

  .hud-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: rgba(14, 165, 233, 0.15);
    border: 1px solid rgba(56, 189, 248, 0.4);
    color: #38BDF8;
    padding: 3px 10px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.5px;
    margin-bottom: 6px;

    .pulse-dot {
      width: 7px;
      height: 7px;
      background: #10B981;
      border-radius: 50%;
      box-shadow: 0 0 8px #10B981;
      animation: pulse 1.8s infinite;
    }
  }

  .hud-title {
    margin: 0;
    font-size: 17px;
    font-weight: 700;
    color: #FFFFFF;
    letter-spacing: 0.3px;
  }
}

.hud-actions {
  display: flex;
  align-items: center;
  gap: 12px;

  .role-selector-wrap {
    display: flex;
    align-items: center;
    gap: 6px;

    .role-label {
      font-size: 13px;
      color: #94A3B8;
    }

    .role-select {
      width: 200px;
    }
  }

  .recalc-btn {
    background: linear-gradient(135deg, #0284C7 0%, #2563EB 100%);
    border: none;
    font-weight: 600;
    border-radius: 6px;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
    transition: all 0.2s;

    &:hover {
      background: linear-gradient(135deg, #0369A1 0%, #1D4ED8 100%);
      transform: translateY(-1px);
    }
  }
}

/* 5 大 Agent 流水线链路 */
.agent-pipeline-track {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 16px;
  position: relative;

  .agent-node {
    background: rgba(30, 41, 59, 0.7);
    border: 1px solid rgba(148, 163, 184, 0.15);
    border-radius: 8px;
    padding: 12px;
    cursor: pointer;
    transition: all 0.25s ease;
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 6px;

    &:hover {
      background: rgba(51, 65, 85, 0.8);
      border-color: rgba(56, 189, 248, 0.5);
      transform: translateY(-2px);
    }

    &.active {
      border-color: #38BDF8;
      background: rgba(14, 165, 233, 0.12);
      box-shadow: 0 0 14px rgba(56, 189, 248, 0.25);
    }

    &.processing {
      border-color: #10B981;
      background: rgba(16, 185, 129, 0.15);
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
        font-size: 10px;
        color: #64748B;
        font-weight: 700;
        letter-spacing: 0.5px;
      }
    }

    .node-name {
      font-size: 13px;
      font-weight: 700;
      color: #F1F5F9;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .node-role {
      font-size: 11px;
      color: #94A3B8;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .node-metric {
      font-size: 10px;
      color: #38BDF8;
      background: rgba(56, 189, 248, 0.1);
      padding: 2px 6px;
      border-radius: 4px;
      width: fit-content;
    }

    .connector {
      position: absolute;
      right: -10px;
      top: 50%;
      transform: translateY(-50%);
      color: rgba(148, 163, 184, 0.4);
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
  background: rgba(15, 23, 42, 0.6);
  border: 1px dashed rgba(148, 163, 184, 0.2);
  border-radius: 6px;
  padding: 10px 14px;
  gap: 18px;
  font-size: 12px;
  flex-wrap: wrap;

  .strip-item {
    display: flex;
    align-items: center;
    gap: 6px;

    .strip-label {
      color: #94A3B8;
      font-weight: 600;
    }

    .strip-val {
      color: #E2E8F0;
    }

    &.highlight .strip-val {
      color: #FBBF24;
      font-weight: 600;
    }
  }

  .strip-tip {
    margin-left: auto;
    color: #64748B;
    font-size: 11px;
  }
}

/* 详情透视弹窗 */
.agent-dialog-body {
  .dialog-banner {
    background: linear-gradient(135deg, #1E293B, #0F172A);
    border-radius: 8px;
    padding: 14px;
    margin-bottom: 16px;

    .banner-role {
      font-size: 15px;
      font-weight: 700;
      color: #38BDF8;
      margin-bottom: 6px;
    }

    .banner-desc {
      font-size: 13px;
      color: #CBD5E1;
      line-height: 1.5;
    }
  }

  .dialog-section {
    margin-bottom: 14px;

    .section-label {
      margin: 0 0 6px 0;
      font-size: 13px;
      color: #0F172A;
      font-weight: 700;
    }

    .section-text {
      margin: 0;
      font-size: 13px;
      color: #475569;
      line-height: 1.5;
    }

    .code-box {
      background: #0F172A;
      color: #38BDF8;
      border-radius: 6px;
      padding: 10px;
      font-family: 'Fira Code', monospace, Consolas;
      font-size: 12px;
      line-height: 1.4;
      white-space: pre-wrap;
      word-break: break-all;

      &.output {
        color: #34D399;
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
