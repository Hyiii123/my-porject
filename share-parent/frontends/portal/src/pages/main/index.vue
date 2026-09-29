<template>
  <div class="home-page">
    <!-- 1. 宏伟科技 Hero Banner (Cosmic Obsidian & Prismatic Flare) -->
    <section class="home-banner">
      <!-- 径向呼吸弥散发光体 -->
      <div class="home-banner-glow"></div>
      <div class="home-banner-glow-secondary"></div>

      <!-- 动态霓虹青光微粒 -->
      <div class="home-banner-particles">
        <span></span><span></span><span></span><span></span><span></span>
        <span></span><span></span><span></span><span></span><span></span>
      </div>

      <!-- Banner 核心内容区 -->
      <div class="home-banner-content">
        <!-- 顶部呼吸徽章标签 -->
        <div class="home-banner-tag">
          <span class="dot"></span>
          <span>L5 级自省多智能体协同导学中心 · 实时在线</span>
        </div>

        <!-- 平台大标题与渐变高亮 -->
        <h1 class="home-banner-title">
          智问学伴 · 深度自省式 <span class="accent">产学研 AI 协同导学平台</span>
        </h1>

        <!-- 双语副标题 -->
        <div class="home-banner-subtitle">
          大模型圆桌博弈推演 · 布鲁姆认知阶梯 · Kahn DAG 拓扑质检 · BKT 知识追踪
        </div>
        <div class="home-banner-subtitle-en">
          EMPOWERING AI-DRIVEN COMPUTATIONAL PEDAGOGY & FULL-STACK EDUCATION
        </div>

        <!-- 平台愿景阐述 -->
        <p class="home-banner-desc">
          依托前沿复合多智能体协同架构，深度融合大厂技术委员会实战图谱与维果茨基教学法模型，为您量身定制系统化、防劝退、强对齐的高薪技术成长进阶路径。
        </p>

        <!-- CTA 操作按钮组 -->
        <div class="home-banner-btns">
          <button class="home-btn home-btn-primary" @click="scrollToHUD">
            <span>⚡ 探索 AI 导学路线</span>
          </button>
          <router-link :to="{ name: 'interviewIndex' }" class="home-btn home-btn-ghost">
            <span>🎙️ 进入全真模拟考场</span>
          </router-link>
        </div>

        <!-- 关键品质特性指标 -->
        <div class="home-banner-features">
          <div class="feat-pill"><span class="feat-check">✓</span><span>100% DAG 拓扑无环合规</span></div>
          <div class="feat-pill"><span class="feat-check">✓</span><span>95%+ 真实大纲证据强接地</span></div>
          <div class="feat-pill"><span class="feat-check">✓</span><span>毫秒级流式自省推演心流</span></div>
        </div>
      </div>

      <!-- 底部平滑过渡色板 -->
      <div class="home-banner-fade"></div>
    </section>

    <!-- 2. 悬浮式公告通知条 (Floating Glass Advisory Capsule) -->
    <div class="home-wrap home-notice">
      <div class="home-notice-bar">
        <div class="home-notice-chip">📢 平台公告</div>
        <div class="txt">2026 年度高校人工智能与全栈开发协同培养计划已全面启动，欢迎体验多智能体导学推演与云端微 IDE！</div>
        <div class="date">2026.09.29</div>
        <div class="more" @click="$router.push('/classList/index')">
          <span>选课中心</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M5 12h14M12 5l7 7-7 7"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- 3. 多智能体协同导学中心核心交互区 (HUD) -->
    <section class="home-section" id="hud-section">
      <div class="home-wrap">
        <div class="home-s-head">
          <div class="home-eyebrow">
            <span class="idx">01</span>
            <span class="bar"></span>
            <span>AI AGENT ROUNDTABLE DELIBERATION</span>
          </div>
          <h2>L5 级多智能体协同导学中心</h2>
          <p>基于学情自适应建模、大模型专家圆桌辩论与客观 Kahn DAG 数学质检闭环。</p>
        </div>

        <!-- 嵌入多智能体流式推理看板 -->
        <AgentReasoningHUD
          @recalculate="handleRecalculateRecommendations"
          @calibrated="handleProbingCalibrated"
          @view-path="openLearningPathModal"
        />
      </div>
    </section>

    <!-- 4. 核心技术深水区 Bento 矩阵 (Core Deep-Tech Showcase) -->
    <section class="home-section bento-section">
      <div class="home-wrap">
        <div class="home-s-head">
          <div class="home-eyebrow">
            <span class="idx">02</span>
            <span class="bar"></span>
            <span>COGNITIVE TRACING & CLOUD SANDBOX MATRIX</span>
          </div>
          <h2>前沿认知计算与云端沙箱工程矩阵</h2>
          <p>融合贝叶斯隐马尔可夫认知追踪、艾宾浩斯记忆衰减模型、极速微 IDE 与 RAGAS 工业评测体系。</p>
        </div>

        <div class="bento-grid">
          <!-- Bento 1 (大卡 2 列): BKT 贝叶斯微知识点追踪引擎 -->
          <div class="bento-card bento-wide bento-bkt">
            <div class="bento-head">
              <div class="bento-badge">
                <span class="dot-emerald"></span>
                <span>BKT 贝叶斯追踪引擎 · 实时在线</span>
              </div>
              <span class="mono-badge">P(L0)=0.25 · P(T)=0.18</span>
            </div>
            <h3>隐马尔可夫多维知识点掌握度建模</h3>
            <p class="bento-desc">
              根据学员做题正误序列，毫秒级反向推导微知识点掌握后验概率 P(Lt)，实时定位技能盲区断层。
            </p>
            <div class="bkt-meters-grid">
              <div v-for="item in bktConcepts" :key="item.name" class="bkt-meter-item">
                <div class="bkt-meter-label">
                  <span class="bkt-name">{{ item.name }}</span>
                  <span class="bkt-pct">{{ item.mastery }}%</span>
                </div>
                <div class="bkt-meter-bar">
                  <div class="bkt-meter-fill" :style="{ width: item.mastery + '%' }"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Bento 2 (单列高卡): 艾宾浩斯遗忘曲线衰减中枢 -->
          <div class="bento-card bento-ebbinghaus">
            <div class="bento-head">
              <div class="bento-badge bento-badge-amber">
                <span class="dot-amber"></span>
                <span>遗忘衰减预警</span>
              </div>
              <span class="mono-badge text-amber">R(t) = e^(-Δt/S)</span>
            </div>
            <h3>艾宾浩斯动态记忆留存中枢</h3>
            <p class="bento-desc">根据时间流逝动态计算历史卡点记忆留存率，自动在多智能体导学提示词中注入复习警报。</p>
            <div class="decay-circle-box">
              <div class="decay-stat">
                <span class="decay-val">58.2%</span>
                <span class="decay-lab">留存临界阈值</span>
              </div>
              <div class="decay-chip-warning">
                <span class="icon">⚠️</span>
                <span>迫切度 89 分 · 2 门核心课待复盘</span>
              </div>
            </div>
          </div>

          <!-- Bento 3 (单列卡): 云端微 IDE 在线代码沙箱 -->
          <div class="bento-card bento-sandbox">
            <div class="bento-head">
              <div class="bento-badge bento-badge-cyan">
                <span class="dot-cyan"></span>
                <span>微 IDE 沙箱内核</span>
              </div>
              <span class="mono-badge text-cyan">LATENCY: 8ms</span>
            </div>
            <h3>课程内嵌在线编译沙箱</h3>
            <p class="bento-desc">Java 17 / Vue3 免配环境实时编码，语法时空复杂度审计，全沙箱安全隔离。</p>
            <div class="mini-terminal">
              <div class="terminal-bar">
                <span class="t-dot r"></span><span class="t-dot y"></span><span class="t-dot g"></span>
                <span class="t-title">SandboxRunner.java</span>
              </div>
              <pre class="t-code"><code><span class="kw">public class</span> Solution {
    <span class="kw">public boolean</span> isValid(String s) {
        Deque&lt;Character&gt; st = <span class="kw">new</span> ArrayDeque&lt;&gt;();
        <span class="cm">// O(N) 一次遍历完成校验</span>
        <span class="kw">return</span> st.isEmpty();
    }
}</code></pre>
              <div class="terminal-result">
                <span class="t-tag pass">✓ 12/12 PASSED</span>
                <span class="t-tag complexity">TIME: O(N) · SPACE: O(N)</span>
              </div>
            </div>
          </div>

          <!-- Bento 4 (2 列卡): RAGAS 自动化质量评估体系 -->
          <div class="bento-card bento-wide bento-ragas">
            <div class="bento-head">
              <div class="bento-badge bento-badge-emerald">
                <span class="dot-emerald"></span>
                <span>RAGAS 质检跑批</span>
              </div>
              <span class="mono-badge text-emerald">GRADE: AAA (0.811)</span>
            </div>
            <h3>大模型导学内容全自动 RAGAS 评估</h3>
            <p class="bento-desc">工业级评估引擎常态化自动化跑批，涵盖四大核心评测维度，杜绝幻觉生成与教学偏航。</p>
            <div class="ragas-metrics-row">
              <div v-for="m in ragasMetrics" :key="m.name" class="ragas-col">
                <div class="ragas-score">{{ m.score }}</div>
                <div class="ragas-label">{{ m.name }}</div>
                <div class="ragas-badge">{{ m.grade }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 5. 专属个性化推荐课程区 (Track Cards Grid) -->
    <section class="home-section track-section" v-if="personalizedCourses.length">
      <div class="home-wrap">
        <div class="home-s-head flex-head">
          <div>
            <div class="home-eyebrow">
              <span class="idx">03</span>
              <span class="bar"></span>
              <span>PERSONALIZED CURRICULUM TRACKS</span>
            </div>
            <h2>🎯 为您专属推荐的硬核必修课</h2>
            <p>精准对标目标岗位技能短板，依托真实大纲知识点与证据链强接地。</p>
          </div>
          <div class="head-actions">
            <el-button type="success" plain round size="small" @click="openLearningPathModal" class="iaic-sub-btn">
              🗺️ 查看完整 4 阶段路线
            </el-button>
            <el-button type="primary" link @click="$router.push('/classList/index')" class="iaic-all-btn">
              全部课程 ➔
            </el-button>
          </div>
        </div>

        <div class="home-track-grid">
          <div
            v-for="(course, idx) in personalizedCourses"
            :key="course.id"
            class="home-track"
            @click="$router.push(`/details?id=${course.id}`)"
          >
            <!-- 科技直角括号光效 -->
            <div class="corner c1"></div>
            <div class="corner c2"></div>

            <div class="home-track-top">
              <span class="home-track-code">TRACK 0{{ idx + 1 }} · {{ course.difficulty === 1 ? 'BEGINNER' : course.difficulty === 3 ? 'ADVANCED' : 'CORE' }}</span>
              <div class="home-track-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                </svg>
              </div>
            </div>

            <div class="track-body">
              <div class="track-header-row">
                <h3>{{ course.title }}</h3>
                <span class="match-pill" v-if="course.matchScore">{{ course.matchScore }}% 契合</span>
              </div>

              <!-- 认知标签 & 实战项目 -->
              <div class="track-tags-row">
                <span class="track-tag bloom" v-if="course.bloomLevelName || course.bloomName">
                  🎓 {{ course.bloomLevelName || course.bloomName }}
                </span>
                <span class="track-tag capstone" v-if="course.capstoneProject || course.isCapstone">
                  🏆 综合实战大项目
                </span>
                <span class="track-tag stage-tag" v-if="course.learningStage">
                  📍 {{ course.learningStage.split('：')[0] }}
                </span>
              </div>

              <!-- 可解释性推荐理由 -->
              <p class="track-reason" v-if="course.recommendReason">
                <span class="bulb">💡</span> {{ course.recommendReason }}
              </p>
            </div>

            <div class="home-track-foot">
              <div class="open">
                <span>开课中 · {{ course.learners || 1000 }}+ 学员在学</span>
              </div>
              <div class="home-track-link">
                <span v-if="course.price > 0" class="track-price">¥{{ (course.price / 100).toFixed(2) }}</span>
                <span v-else class="track-price-free">免费学习</span>
                <span class="arrow">➔</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 6. 持续学习快捷进度卡片 (登录态) -->
    <div v-if="recentLearning && (recentLearning.courseName || recentLearning.title)" class="home-wrap continue-wrap">
      <div class="continue-iaic-card">
        <div class="c-left">
          <div class="c-icon-badge">
            <el-icon :size="24"><Reading /></el-icon>
          </div>
          <div class="c-text">
            <span class="c-label">您上次正在研习</span>
            <h4>{{ recentLearning.courseName || recentLearning.title }}</h4>
            <span class="c-sub">已掌握 {{ recentLearning.completedLessons ?? 0 }} / {{ recentLearning.totalLessons ?? 10 }} 课时</span>
          </div>
        </div>
        <div class="c-right">
          <div class="c-progress">
            <el-progress
              :percentage="calcProgress(recentLearning)"
              :stroke-width="8"
              color="var(--brand-cyan)"
            />
          </div>
          <button class="home-btn home-btn-primary mini-btn" @click="goLearning(recentLearning)">
            继续研习 ➔
          </button>
        </div>
      </div>
    </div>

    <!-- 7. 四维一体协同导学全流程 (Workflow Steps) -->
    <section class="home-section flow-section">
      <div class="home-wrap">
        <div class="home-s-head">
          <div class="home-eyebrow">
            <span class="idx">04</span>
            <span class="bar"></span>
            <span>PEDAGOGICAL PROGRESSION PIPELINE</span>
          </div>
          <h2>四维一体智能协同导学全流程</h2>
          <p>从多维学情摸底到拓扑反思重排，打造最严谨的自适应工程培养闭环。</p>
        </div>

        <div class="home-flow-grid">
          <div class="home-step">
            <div class="home-step-num">01</div>
            <h4>深度学情诊断</h4>
            <p>完备度模型自动识别冷启动访客，触发 3 项主动探针，建立真实技能基线。</p>
          </div>
          <div class="home-step">
            <div class="home-step-num">02</div>
            <h4>专家圆桌博弈</h4>
            <p>大厂总监立足岗位硬指标提案，学情导师基于 ZPD 教学法模型提出防劝退抗辩。</p>
          </div>
          <div class="home-step">
            <div class="home-step-num">03</div>
            <h4>审判法官质检</h4>
            <p>Kahn DAG 算法审计先修时序无环，布鲁姆难度方差把控梯级平滑，低于 80 分打回。</p>
          </div>
          <div class="home-step">
            <div class="home-step-num">04</div>
            <h4>闭环方案交付</h4>
            <p>Spring AI + RAG 胜任力知识切片强接地，生成 4 阶段个性化成长进阶图谱。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 学习路径弹窗 -->
    <el-dialog
      v-model="learningPathModalVisible"
      title="🗺️ AI 学习成长路径全景图"
      width="820px"
      append-to-body
      class="learning-path-dialog"
    >
      <div v-if="activeLearningPath" class="path-modal-body">
        <div class="path-modal-header">
          <div class="role-badge">🎯 {{ activeLearningPath.intendedRole }}</div>
          <p class="goal-text">{{ activeLearningPath.overallGoal }}</p>
          <div class="stats-row">
            <span>📚 规划课程: {{ activeLearningPath.totalCourses }} 门</span>
            <span>⏱️ 预估总学时: {{ activeLearningPath.totalEstimatedHours }} 小时</span>
          </div>
        </div>

        <div class="stages-timeline">
          <div
            v-for="stage in activeLearningPath.stages"
            :key="stage.stageIndex"
            class="timeline-stage-item"
          >
            <div class="stage-badge-circle">{{ stage.stageIndex }}</div>
            <div class="stage-card">
              <div class="stage-card-title-row">
                <h4 class="stg-name">{{ stage.stageName }}</h4>
                <span class="stg-hours">{{ stage.estimatedHours }} 小时</span>
              </div>
              <p class="stg-goal">{{ stage.stageGoal }}</p>
              <div class="stg-courses-chips" v-if="stage.courses && stage.courses.length">
                <span
                  v-for="c in stage.courses"
                  :key="c.courseId"
                  class="course-chip"
                  @click="$router.push(`/details?id=${c.courseId}`)"
                >
                  📖 {{ c.courseName }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Reading } from '@element-plus/icons-vue'
import AgentReasoningHUD from '@/components/AgentReasoningHUD.vue'
import { getRecommendClassList, getCourseLearning } from '@/api/class.js'

const router = useRouter()

const personalizedCourses = ref([])
const recentLearning = ref(null)
const learningPathModalVisible = ref(false)
const activeLearningPath = ref(null)

// Bento Grid 深度认知科技数据
const bktConcepts = ref([
  { name: 'Redis 分布式锁与 Redisson 续期', mastery: 96 },
  { name: 'MySQL B+树聚簇索引与页分裂', mastery: 92 },
  { name: 'Spring 三级缓存与循环依赖消除', mastery: 88 },
  { name: 'JVM ZGC 染色指针与读屏障', mastery: 84 }
])

const ragasMetrics = ref([
  { name: '忠实度 Faithfulness', score: '0.94', grade: 'AAA' },
  { name: '答案相关度 Relevance', score: '0.90', grade: 'AAA' },
  { name: '上下文精确度 Precision', score: '0.82', grade: 'AA' },
  { name: '上下文召回率 Recall', score: '0.78', grade: 'AA' }
])

const scrollToHUD = () => {
  const el = document.getElementById('hud-section')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const handleRecalculateRecommendations = (data) => {
  if (data && data.recommendations) {
    personalizedCourses.value = data.recommendations
  }
  if (data && data.learningPath) {
    activeLearningPath.value = data.learningPath
  }
}

const handleProbingCalibrated = (data) => {
  if (data && data.recommendations) {
    personalizedCourses.value = data.recommendations
  }
  if (data && data.learningPath) {
    activeLearningPath.value = data.learningPath
  }
}

const openLearningPathModal = (path) => {
  if (path) {
    activeLearningPath.value = path
  }
  learningPathModalVisible.value = true
}

const calcProgress = (record) => {
  if (!record) return 0
  if (record.progressPercent != null) return Number(record.progressPercent)
  const c = record.completedLessons ?? record.learnedSections ?? 0
  const t = record.totalLessons ?? record.sections ?? record.totalSections ?? 10
  return Math.min(100, Math.round((c / Math.max(1, t)) * 100))
}

const goLearning = (record) => {
  if (record && record.courseId) {
    router.push(`/learning/index?id=${record.courseId}`)
  }
}

const loadInitialData = async () => {
  try {
    const recRes = await getRecommendClassList({ limit: 6 })
    if (recRes && recRes.code === 200 && Array.isArray(recRes.data)) {
      personalizedCourses.value = recRes.data
    }
  } catch (e) {
    console.debug('Failed to load recommended courses:', e)
  }

  const token = sessionStorage.getItem('token')
  if (token) {
    try {
      const learnRes = await getCourseLearning()
      if (learnRes && learnRes.code === 200 && learnRes.data) {
        recentLearning.value = learnRes.data
      }
    } catch (e) {
      console.debug('Failed to load recent learning:', e)
    }
  }
}

onMounted(() => {
  loadInitialData()
})
</script>

<style lang="scss" scoped>
.home-page {
  background-color: #07090E;
  min-height: 100vh;
  color: #F8FAFC;
  overflow-x: hidden;
}

/* --- 1. 宏伟科技 Hero Banner --- */
.home-banner {
  position: relative;
  width: 100%;
  min-height: 580px;
  padding: 88px 24px 120px;
  background: 
    radial-gradient(80% 60% at 50% 0%, rgba(56, 189, 248, 0.16), transparent 70%),
    radial-gradient(60% 50% at 85% 20%, rgba(99, 102, 241, 0.12), transparent 65%),
    radial-gradient(50% 40% at 15% 65%, rgba(16, 185, 129, 0.08), transparent 60%),
    #07090E;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);

  &:before {
    content: "";
    position: absolute;
    top: 0; right: 0; bottom: 0; left: 0;
    background-image: 
      linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
    background-size: 48px 48px;
    mask-image: radial-gradient(ellipse at 50% 45%, #000 30%, transparent 80%);
    -webkit-mask-image: radial-gradient(ellipse at 50% 45%, #000 30%, transparent 80%);
    pointer-events: none;
  }
}

.home-banner-glow {
  position: absolute;
  width: 900px;
  height: 500px;
  background: radial-gradient(ellipse, rgba(56, 189, 248, 0.22), transparent 65%);
  top: 45%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  animation: pulseGlow 6s ease-in-out infinite;
}

.home-banner-glow-secondary {
  position: absolute;
  width: 700px;
  height: 400px;
  background: radial-gradient(ellipse, rgba(99, 102, 241, 0.18), transparent 65%);
  top: 60%;
  left: 60%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  animation: pulseGlow 7s ease-in-out infinite reverse;
}

.home-banner-particles {
  position: absolute;
  top: 0; right: 0; bottom: 0; left: 0;
  pointer-events: none;
  overflow: hidden;

  span {
    position: absolute;
    bottom: -10px;
    background: #38bdf8;
    border-radius: 50%;
    box-shadow: 0 0 10px #38bdf8;
    animation: floatParticles linear infinite;

    &:nth-child(1) { left: 10%; width: 4px; height: 4px; animation-duration: 9s; animation-delay: 0s; }
    &:nth-child(2) { left: 20%; width: 5px; height: 5px; animation-duration: 12s; animation-delay: 2s; }
    &:nth-child(3) { left: 35%; width: 3px; height: 3px; animation-duration: 8s; animation-delay: 4s; }
    &:nth-child(4) { left: 50%; width: 6px; height: 6px; animation-duration: 14s; animation-delay: 1s; }
    &:nth-child(5) { left: 65%; width: 4px; height: 4px; animation-duration: 10s; animation-delay: 3s; }
    &:nth-child(6) { left: 78%; width: 5px; height: 5px; animation-duration: 11s; animation-delay: 5s; }
    &:nth-child(7) { left: 88%; width: 3px; height: 3px; animation-duration: 7s; animation-delay: 2.5s; }
    &:nth-child(8) { left: 95%; width: 4px; height: 4px; animation-duration: 13s; animation-delay: 0.5s; }
    &:nth-child(9) { left: 28%; width: 3px; height: 3px; animation-duration: 9.5s; animation-delay: 3.5s; }
    &:nth-child(10) { left: 60%; width: 4px; height: 4px; animation-duration: 11.5s; animation-delay: 1.5s; }
  }
}

.home-banner-content {
  position: relative;
  z-index: 2;
  max-width: 1020px;
  margin: 0 auto;
}

.home-banner-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 18px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.08);
  border: 1px solid rgba(56, 189, 248, 0.25);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), 0 0 20px -4px rgba(56, 189, 248, 0.25);
  font-size: 13px;
  font-weight: 500;
  color: #38BDF8;
  margin-bottom: 26px;

  .dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #34D399;
    box-shadow: 0 0 10px #34D399;
    animation: radarBreathe 2s ease-in-out infinite;
  }
}

.home-banner-title {
  font-family: var(--display);
  font-size: clamp(34px, 4.4vw, 56px);
  font-weight: 800;
  letter-spacing: -0.035em;
  line-height: 1.15;
  color: #FFFFFF;
  margin: 0 auto 20px;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.6);

  .accent {
    background: linear-gradient(135deg, #38BDF8 0%, #818CF8 50%, #34D399 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    display: inline-block;
  }
}

.home-banner-subtitle {
  font-family: var(--cn);
  font-size: clamp(16px, 1.8vw, 20px);
  font-weight: 500;
  color: #CBD5E1;
  letter-spacing: 0.02em;
  margin-bottom: 8px;
}

.home-banner-subtitle-en {
  font-family: var(--mono);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: #38BDF8;
  font-weight: 600;
  margin-bottom: 22px;
  opacity: 0.9;
}

.home-banner-desc {
  max-width: 760px;
  margin: 0 auto 36px;
  font-size: 15.5px;
  line-height: 1.75;
  color: #94A3B8;
}

.home-banner-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.home-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  text-decoration: none;
}

.home-btn-primary {
  background: linear-gradient(135deg, #0284C7 0%, #2563EB 50%, #4F46E5 100%);
  color: #FFFFFF;
  border: 1px solid rgba(56, 189, 248, 0.5);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3), 0 8px 30px -4px rgba(37, 99, 235, 0.5);
  padding: 13px 32px;
  border-radius: 999px;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    transform: translateY(-2px) scale(1.02);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45), 0 16px 40px -4px rgba(56, 189, 248, 0.6);
  }
  &:active {
    transform: scale(0.98);
  }
}

.home-btn-ghost {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(16px);
  color: #F8FAFC;
  border: 1px solid rgba(255, 255, 255, 0.14);
  padding: 13px 32px;
  border-radius: 999px;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(56, 189, 248, 0.4);
    color: #38BDF8;
    transform: translateY(-2px);
    box-shadow: 0 8px 24px -4px rgba(0, 0, 0, 0.5);
  }
  &:active {
    transform: scale(0.98);
  }
}

.home-banner-features {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;

  .feat-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.08);
    font-size: 12.5px;
    color: #CBD5E1;
    font-family: var(--mono);

    .feat-check {
      color: #34D399;
      font-weight: 700;
    }
  }
}

.home-banner-fade {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 60px;
  background: linear-gradient(to top, #07090E 0%, transparent 100%);
  pointer-events: none;
}

/* --- 2. 悬浮公告条 --- */
.home-notice {
  margin-top: -24px;
  position: relative;
  z-index: 10;
}

.home-notice-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: rgba(16, 23, 38, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  backdrop-filter: blur(20px);
  box-shadow: 0 12px 32px -6px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.12);

  .home-notice-chip {
    font-size: 11px;
    font-weight: 700;
    color: #38BDF8;
    background: rgba(56, 189, 248, 0.15);
    border: 1px solid rgba(56, 189, 248, 0.3);
    padding: 3px 10px;
    border-radius: 999px;
    flex-shrink: 0;
  }

  .txt {
    flex: 1;
    font-size: 13.5px;
    color: #E2E8F0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .date {
    font-family: var(--mono);
    font-size: 12px;
    color: #64748B;
  }

  .more {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #38BDF8;
    cursor: pointer;
    font-weight: 600;
    transition: color 0.2s;

    svg { width: 14px; height: 14px; }
    &:hover { color: #7DD3FC; }
  }
}

/* --- 通用 Section --- */
.home-wrap {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 28px;
}

.home-section {
  padding: 72px 0;
  position: relative;
}

.home-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--mono);
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #38BDF8;

  .idx {
    color: #34D399;
    font-weight: 800;
  }
  .bar {
    width: 24px;
    height: 2px;
    background: linear-gradient(90deg, #38BDF8, #818CF8);
    border-radius: 2px;
  }
}

.home-s-head {
  margin-bottom: 40px;

  h2 {
    font-size: clamp(26px, 3vw, 36px);
    font-weight: 800;
    color: #FFFFFF;
    letter-spacing: -0.025em;
    line-height: 1.2;
    margin-top: 10px;
  }
  p {
    margin-top: 8px;
    color: #94A3B8;
    max-width: 680px;
    font-size: 15px;
    line-height: 1.6;
  }

  &.flex-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    flex-wrap: wrap;
    gap: 16px;
  }
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;

  .iaic-sub-btn {
    background: rgba(16, 185, 129, 0.12) !important;
    border-color: rgba(16, 185, 129, 0.3) !important;
    color: #34D399 !important;
    font-weight: 600;
    &:hover {
      background: rgba(16, 185, 129, 0.22) !important;
    }
  }

  .iaic-all-btn {
    color: #38BDF8 !important;
    font-weight: 600;
    &:hover { color: #7DD3FC !important; }
  }
}

/* --- 4. 核心技术深水区 Bento 矩阵 (Bento Grid) --- */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.bento-card {
  position: relative;
  background: rgba(16, 23, 38, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 30px;
  backdrop-filter: blur(20px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 16px 40px -8px rgba(0, 0, 0, 0.6);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(56, 189, 248, 0.4);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2), 0 24px 50px -10px rgba(14, 165, 233, 0.25);
  }

  &.bento-wide {
    grid-column: span 2;
  }

  .bento-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .bento-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      border-radius: 999px;
      font-size: 11.5px;
      font-weight: 600;
      color: #38BDF8;
      background: rgba(56, 189, 248, 0.12);
      border: 1px solid rgba(56, 189, 248, 0.25);

      .dot-emerald { width: 6px; height: 6px; border-radius: 50%; background: #34D399; box-shadow: 0 0 8px #34D399; }
      .dot-amber { width: 6px; height: 6px; border-radius: 50%; background: #F59E0B; box-shadow: 0 0 8px #F59E0B; }
      .dot-cyan { width: 6px; height: 6px; border-radius: 50%; background: #06B6D4; box-shadow: 0 0 8px #06B6D4; }

      &.bento-badge-amber {
        color: #F59E0B;
        background: rgba(245, 158, 11, 0.12);
        border-color: rgba(245, 158, 11, 0.25);
      }
      &.bento-badge-cyan {
        color: #06B6D4;
        background: rgba(6, 182, 212, 0.12);
        border-color: rgba(6, 182, 212, 0.25);
      }
      &.bento-badge-emerald {
        color: #34D399;
        background: rgba(16, 185, 129, 0.12);
        border-color: rgba(16, 185, 129, 0.25);
      }
    }

    .mono-badge {
      font-family: var(--mono);
      font-size: 11px;
      color: #64748B;
      letter-spacing: 0.05em;

      &.text-amber { color: #FBBF24; }
      &.text-cyan { color: #38BDF8; }
      &.text-emerald { color: #34D399; }
    }
  }

  h3 {
    font-size: 20px;
    font-weight: 700;
    color: #FFFFFF;
    margin: 0 0 10px;
    letter-spacing: -0.015em;
  }

  .bento-desc {
    font-size: 13.5px;
    line-height: 1.65;
    color: #94A3B8;
    margin: 0 0 20px;
  }
}

// BKT 仪表
.bkt-meters-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-top: auto;

  .bkt-meter-item {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.06);
    border-radius: 10px;
    padding: 10px 14px;

    .bkt-meter-label {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      margin-bottom: 6px;
      .bkt-name { color: #CBD5E1; }
      .bkt-pct { font-family: var(--mono); color: #38BDF8; font-weight: 700; }
    }

    .bkt-meter-bar {
      height: 4px;
      background: rgba(255, 255, 255, 0.08);
      border-radius: 2px;
      overflow: hidden;

      .bkt-meter-fill {
        height: 100%;
        background: linear-gradient(90deg, #38BDF8, #818CF8);
        border-radius: 2px;
      }
    }
  }
}

// 艾宾浩斯
.decay-circle-box {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;

  .decay-stat {
    display: flex;
    flex-direction: column;
    align-items: center;
    .decay-val {
      font-family: var(--mono);
      font-size: 38px;
      font-weight: 800;
      color: #F59E0B;
      line-height: 1;
      text-shadow: 0 0 24px rgba(245, 158, 11, 0.35);
    }
    .decay-lab {
      font-size: 11.5px;
      color: #94A3B8;
      margin-top: 4px;
    }
  }

  .decay-chip-warning {
    display: flex;
    align-items: center;
    gap: 6px;
    background: rgba(245, 158, 11, 0.12);
    border: 1px solid rgba(245, 158, 11, 0.3);
    color: #FCD34D;
    padding: 6px 12px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 500;
  }
}

// 微沙箱终端
.mini-terminal {
  margin-top: auto;
  background: #060911;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  overflow: hidden;

  .terminal-bar {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.03);
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);

    .t-dot {
      width: 8px; height: 8px; border-radius: 50%;
      &.r { background: #F43F5E; }
      &.y { background: #F59E0B; }
      &.g { background: #10B981; }
    }
    .t-title {
      font-family: var(--mono);
      font-size: 10px;
      color: #64748B;
      margin-left: 6px;
    }
  }

  .t-code {
    margin: 0;
    padding: 10px 14px;
    font-family: var(--mono);
    font-size: 11px;
    line-height: 1.6;
    color: #E2E8F0;

    .kw { color: #818CF8; }
    .cm { color: #64748B; }
  }

  .terminal-result {
    display: flex;
    justify-content: space-between;
    padding: 6px 12px;
    background: rgba(16, 185, 129, 0.06);
    border-top: 1px solid rgba(16, 185, 129, 0.15);
    font-family: var(--mono);
    font-size: 10px;

    .pass { color: #34D399; font-weight: 700; }
    .complexity { color: #94A3B8; }
  }
}

// RAGAS 指标
.ragas-metrics-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: auto;

  .ragas-col {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.06);
    border-radius: 12px;
    padding: 12px 10px;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .ragas-score {
      font-family: var(--mono);
      font-size: 22px;
      font-weight: 800;
      color: #34D399;
    }
    .ragas-label {
      font-size: 11px;
      color: #94A3B8;
    }
    .ragas-badge {
      font-family: var(--mono);
      font-size: 10px;
      color: #38BDF8;
      background: rgba(56, 189, 248, 0.15);
      padding: 1px 6px;
      border-radius: 4px;
      margin-top: 2px;
    }
  }
}

/* --- 5. 专属个性化推荐课程区 --- */
.home-track-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.home-track {
  position: relative;
  background: rgba(16, 23, 38, 0.72);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 28px 30px;
  backdrop-filter: blur(20px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 16px 40px -8px rgba(0, 0, 0, 0.6);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(56, 189, 248, 0.4);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2), 0 24px 50px -10px rgba(14, 165, 233, 0.3);

    .corner { opacity: 1; }
    .arrow { transform: translateX(4px); color: #38BDF8; }
  }

  .corner {
    position: absolute;
    width: 12px;
    height: 12px;
    border: 2px solid #38BDF8;
    opacity: 0;
    transition: opacity 0.25s ease;
    &.c1 { top: 8px; left: 8px; border-right: none; border-bottom: none; }
    &.c2 { bottom: 8px; right: 8px; border-left: none; border-top: none; }
  }

  .home-track-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .home-track-code {
      font-family: var(--mono);
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.08em;
      color: #38BDF8;
      background: rgba(56, 189, 248, 0.1);
      padding: 3px 10px;
      border-radius: 999px;
      border: 1px solid rgba(56, 189, 248, 0.25);
    }

    .home-track-icon {
      width: 28px;
      height: 28px;
      color: #64748B;
      svg { width: 100%; height: 100%; }
    }
  }

  .track-body {
    flex: 1;

    .track-header-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
      margin-bottom: 12px;

      h3 {
        font-size: 18px;
        font-weight: 700;
        color: #FFFFFF;
        line-height: 1.35;
        margin: 0;
      }

      .match-pill {
        font-family: var(--mono);
        font-size: 11px;
        font-weight: 700;
        color: #34D399;
        background: rgba(16, 185, 129, 0.15);
        border: 1px solid rgba(16, 185, 129, 0.3);
        padding: 2px 8px;
        border-radius: 6px;
        white-space: nowrap;
      }
    }

    .track-tags-row {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-bottom: 14px;

      .track-tag {
        font-size: 11.5px;
        padding: 3px 9px;
        border-radius: 6px;
        font-weight: 500;

        &.bloom {
          background: rgba(99, 102, 241, 0.15);
          color: #A5B4FC;
          border: 1px solid rgba(99, 102, 241, 0.3);
        }
        &.capstone {
          background: rgba(245, 158, 11, 0.15);
          color: #FCD34D;
          border: 1px solid rgba(245, 158, 11, 0.3);
        }
        &.stage-tag {
          background: rgba(255, 255, 255, 0.06);
          color: #CBD5E1;
          border: 1px solid rgba(255, 255, 255, 0.1);
        }
      }
    }

    .track-reason {
      font-size: 13px;
      line-height: 1.6;
      color: #94A3B8;
      background: rgba(255, 255, 255, 0.03);
      border-left: 2px solid #38BDF8;
      padding: 8px 12px;
      border-radius: 0 8px 8px 0;
      margin: 0 0 16px;
    }
  }

  .home-track-foot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    padding-top: 16px;
    margin-top: 8px;

    .open {
      font-size: 12.5px;
      color: #64748B;
    }

    .home-track-link {
      display: flex;
      align-items: center;
      gap: 8px;

      .track-price {
        font-family: var(--mono);
        font-size: 18px;
        font-weight: 700;
        color: #34D399;
      }
      .track-price-free {
        font-size: 14px;
        font-weight: 600;
        color: #38BDF8;
      }
      .arrow {
        color: #64748B;
        transition: all 0.2s ease;
      }
    }
  }
}

/* --- 6. 持续学习卡片 --- */
.continue-wrap {
  margin-top: 10px;
  margin-bottom: 20px;
}

.continue-iaic-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(16, 23, 38, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 20px 28px;
  backdrop-filter: blur(20px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);

  .c-left {
    display: flex;
    align-items: center;
    gap: 18px;

    .c-icon-badge {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      background: linear-gradient(135deg, rgba(56, 189, 248, 0.2) 0%, rgba(99, 102, 241, 0.2) 100%);
      border: 1px solid rgba(56, 189, 248, 0.4);
      color: #38BDF8;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .c-text {
      .c-label {
        font-size: 11px;
        color: #94A3B8;
        font-family: var(--mono);
        text-transform: uppercase;
      }
      h4 {
        margin: 2px 0;
        font-size: 16px;
        font-weight: 700;
        color: #FFFFFF;
      }
      .c-sub {
        font-size: 12px;
        color: #64748B;
      }
    }
  }

  .c-right {
    display: flex;
    align-items: center;
    gap: 24px;

    .c-progress {
      width: 180px;
    }

    .mini-btn {
      padding: 8px 20px;
      font-size: 13px;
    }
  }
}

/* --- 7. 四维一体导学全流程 --- */
.home-flow-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.home-step {
  position: relative;
  background: rgba(16, 23, 38, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  padding: 28px 24px;
  backdrop-filter: blur(16px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(56, 189, 248, 0.4);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2), 0 16px 36px -8px rgba(0, 0, 0, 0.6);
  }

  .home-step-num {
    font-family: var(--mono);
    font-size: 28px;
    font-weight: 800;
    color: rgba(56, 189, 248, 0.3);
    line-height: 1;
    margin-bottom: 12px;
  }

  h4 {
    font-size: 17px;
    font-weight: 700;
    color: #FFFFFF;
    margin: 0 0 8px;
  }

  p {
    font-size: 13px;
    line-height: 1.6;
    color: #94A3B8;
    margin: 0;
  }
}

/* 学习路径弹窗 */
.learning-path-dialog {
  :deep(.el-dialog__body) {
    padding: 24px;
    background: #0B0F19;
    color: #F8FAFC;
  }
}

.path-modal-body {
  .path-modal-header {
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);

    .role-badge {
      font-size: 14px;
      font-weight: 700;
      color: #38BDF8;
      margin-bottom: 6px;
    }
    .goal-text {
      font-size: 14px;
      color: #CBD5E1;
      line-height: 1.6;
      margin: 0 0 10px;
    }
    .stats-row {
      display: flex;
      gap: 16px;
      font-family: var(--mono);
      font-size: 12px;
      color: #94A3B8;
    }
  }

  .stages-timeline {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .timeline-stage-item {
      display: flex;
      gap: 16px;

      .stage-badge-circle {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: linear-gradient(135deg, #0284C7, #2563EB);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-family: var(--mono);
        font-weight: 700;
        font-size: 14px;
        flex-shrink: 0;
      }

      .stage-card {
        flex: 1;
        background: rgba(255, 255, 255, 0.04);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 12px;
        padding: 14px 18px;

        .stage-card-title-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 6px;

          .stg-name {
            font-size: 15px;
            font-weight: 700;
            color: #FFFFFF;
            margin: 0;
          }
          .stg-hours {
            font-family: var(--mono);
            font-size: 12px;
            color: #38BDF8;
          }
        }

        .stg-goal {
          font-size: 13px;
          color: #94A3B8;
          line-height: 1.5;
          margin: 0 0 10px;
        }

        .stg-courses-chips {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;

          .course-chip {
            font-size: 12px;
            padding: 4px 10px;
            border-radius: 6px;
            background: rgba(56, 189, 248, 0.1);
            border: 1px solid rgba(56, 189, 248, 0.25);
            color: #E2E8F0;
            cursor: pointer;
            transition: all 0.2s;

            &:hover {
              background: rgba(56, 189, 248, 0.2);
              border-color: #38BDF8;
              color: #38BDF8;
            }
          }
        }
      }
    }
  }
}

@media (max-width: 1024px) {
  .bento-grid {
    grid-template-columns: 1fr;
    .bento-card.bento-wide { grid-column: span 1; }
  }
  .home-track-grid {
    grid-template-columns: 1fr;
  }
  .home-flow-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .home-flow-grid {
    grid-template-columns: 1fr;
  }
  .continue-iaic-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    .c-right {
      width: 100%;
      justify-content: space-between;
    }
  }
}
</style>
