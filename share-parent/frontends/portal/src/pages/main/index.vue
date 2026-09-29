<template>
  <div class="home-page">
    <!-- 1. 逆向 1:1 IAIC 宏伟科技 Hero Banner -->
    <section class="home-banner">
      <!-- 径向呼吸弥散发光体 -->
      <div class="home-banner-glow"></div>

      <!-- 10 个轻灵浮动的霓虹青光微粒 -->
      <div class="home-banner-particles">
        <span></span><span></span><span></span><span></span><span></span>
        <span></span><span></span><span></span><span></span><span></span>
      </div>

      <!-- Banner 核心内容区 -->
      <div class="home-banner-content">
        <!-- 顶部呼吸徽章标签 -->
        <div class="home-banner-tag">
          <span class="dot"></span>
          <span>L5 级自省多智能体协同导学中心 · 全新上线</span>
        </div>

        <!-- 平台大标题与渐变高亮 -->
        <h1 class="home-banner-title">
          智问学伴 · 新一代 <span class="accent">产学研 AI 协同导学平台</span>
        </h1>

        <!-- 双语副标题 -->
        <div class="home-banner-subtitle">
          大模型圆桌博弈推演 · 布鲁姆认知阶梯 · Kahn DAG 拓扑质检
        </div>
        <div class="home-banner-subtitle-en">
          EMPOWERING AI-DRIVEN COMPUTATIONAL PEDAGOGY & FULL-STACK EDUCATION
        </div>

        <!-- 平台愿景阐述 -->
        <p class="home-banner-desc">
          依托前沿复合多智能体协同架构，深度融合大厂技术委员会实战图谱与维果茨基教学法模型，为您量身定制系统化、防劝退、强对齐的高薪技术成长闭环。
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
          <span>100% DAG 拓扑无环合规</span>
          <span>95%+ 真实大纲证据强接地</span>
          <span>毫秒级流式自省推演心流</span>
        </div>
      </div>

      <!-- 底部平滑过渡色板 -->
      <div class="home-banner-fade"></div>
    </section>

    <!-- 2. 悬浮式公告通知条 (Floating Notice Bar) -->
    <div class="home-wrap home-notice">
      <div class="home-notice-bar">
        <div class="home-notice-chip">📢 平台公告</div>
        <div class="txt">2026 年度高校人工智能与全栈开发协同培养计划已全面启动，欢迎体验多智能体导学推演！</div>
        <div class="date">2026.09.21</div>
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

    <!-- 4. 专属个性化推荐课程区 (1:1 IAIC Track Cards Grid) -->
    <section class="home-section track-section" v-if="personalizedCourses.length">
      <div class="home-wrap">
        <div class="home-s-head flex-head">
          <div>
            <div class="home-eyebrow">
              <span class="idx">02</span>
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
            <!-- 科技直角括号光效 (Hover 时点亮) -->
            <div class="corner c1"></div>
            <div class="corner c2"></div>

            <div class="home-track-top">
              <span class="home-track-code">TRACK 0{{ idx + 1 }} · {{ course.difficulty === 1 ? 'BEGINNER' : course.difficulty === 3 ? 'ADVANCED' : 'CORE' }}</span>
              <div class="home-track-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke-width="2">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                </svg>
              </div>
            </div>

            <div class="track-body">
              <div class="track-header-row">
                <h3>{{ course.title }}</h3>
                <span class="match-pill" v-if="course.matchScore">{{ course.matchScore }}% 契合</span>
              </div>

              <!-- 布鲁姆认知标签 & 实战项目 -->
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

    <!-- 5. 持续学习快捷进度卡片 (登录态) -->
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
              color="var(--azure)"
            />
          </div>
          <button class="home-btn home-btn-primary mini-btn" @click="goLearning(recentLearning)">
            继续研习 ➔
          </button>
        </div>
      </div>
    </div>

    <!-- 6. 逆向 1:1 IAIC 四维一体导学流程 (Workflow Steps) -->
    <section class="home-section flow-section">
      <div class="home-wrap">
        <div class="home-s-head">
          <div class="home-eyebrow">
            <span class="idx">03</span>
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
  font-family: var(--cn);
  background: var(--sky);
  color: var(--navy);
  -webkit-font-smoothing: antialiased;
  line-height: 1.6;
  overflow-x: hidden;
  min-height: 100vh;
}

.home-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 28px;
}

/* --- 1. IAIC 签名 Hero Banner --- */
.home-banner {
  position: relative;
  width: 100%;
  min-height: 520px;
  padding: 72px 24px 110px;
  background: radial-gradient(60% 80% at 50% 0%, rgba(33, 198, 232, 0.14), transparent 60%),
              radial-gradient(50% 70% at 5% 100%, rgba(43, 134, 240, 0.12), transparent 60%),
              linear-gradient(135deg, #eaf4ff, #d6eaff 55%, #c8e0ff);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;

  &:before {
    content: "";
    position: absolute;
    top: 0; right: 0; bottom: 0; left: 0;
    background-image: linear-gradient(rgba(42, 143, 255, 0.07) 1px, transparent 1px),
                      linear-gradient(90deg, rgba(42, 143, 255, 0.07) 1px, transparent 1px);
    background-size: 44px 44px;
    mask-image: radial-gradient(ellipse at center, #000 35%, transparent 85%);
    -webkit-mask-image: radial-gradient(ellipse at center, #000 35%, transparent 85%);
    pointer-events: none;
  }
}

.home-banner-glow {
  position: absolute;
  width: 900px;
  height: 500px;
  background: radial-gradient(ellipse, rgba(56, 182, 255, 0.35), transparent 65%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  animation: pulseGlow 5s ease-in-out infinite;
}

.home-banner-particles {
  position: absolute;
  top: 0; right: 0; bottom: 0; left: 0;
  pointer-events: none;
  overflow: hidden;

  span {
    position: absolute;
    bottom: -10px;
    background: #38b6ff;
    border-radius: 50%;
    box-shadow: 0 0 10px #38b6ff, 0 0 18px rgba(56, 182, 255, 0.6);
    animation: floatUp linear infinite;
    opacity: 0.7;

    &:nth-child(1) { left: 8%; width: 5px; height: 5px; animation-duration: 11s; animation-delay: 0s; }
    &:nth-child(2) { left: 18%; width: 6px; height: 6px; animation-duration: 14s; animation-delay: 2s; }
    &:nth-child(3) { left: 28%; width: 4px; height: 4px; animation-duration: 12s; animation-delay: 4s; }
    &:nth-child(4) { left: 40%; width: 6px; height: 6px; animation-duration: 16s; animation-delay: 1s; }
    &:nth-child(5) { left: 52%; width: 4px; height: 4px; animation-duration: 10s; animation-delay: 3s; }
    &:nth-child(6) { left: 64%; width: 5px; height: 5px; animation-duration: 13s; animation-delay: 5s; }
    &:nth-child(7) { left: 76%; width: 6px; height: 6px; animation-duration: 15s; animation-delay: 2s; }
    &:nth-child(8) { left: 88%; width: 4px; height: 4px; animation-duration: 12s; animation-delay: 6s; }
    &:nth-child(9) { left: 34%; width: 5px; height: 5px; animation-duration: 9s; animation-delay: 7s; }
    &:nth-child(10) { left: 70%; width: 4px; height: 4px; animation-duration: 17s; animation-delay: 0s; }
  }
}

.home-banner-content {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1100px;
}

.home-banner-tag {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-family: var(--mono);
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--blue-deep);
  background: rgba(43, 134, 240, 0.1);
  border: 1px solid rgba(43, 134, 240, 0.25);
  border-radius: 100px;
  padding: 6px 16px;
  margin-bottom: 20px;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--cyan);
    box-shadow: 0 0 8px rgba(33, 198, 232, 0.8);
    animation: pulse 2.2s infinite;
  }
}

.home-banner-title {
  font-size: clamp(34px, 4.8vw, 62px);
  font-weight: 800;
  color: var(--ink);
  letter-spacing: 0.005em;
  line-height: 1.15;
  margin-bottom: 12px;

  .accent {
    background: var(--grad);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }
}

.home-banner-subtitle {
  font-size: 20px;
  font-weight: 700;
  color: var(--navy);
  margin-bottom: 6px;
}

.home-banner-subtitle-en {
  font-family: var(--display);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: var(--slate-2);
  text-transform: uppercase;
  margin-bottom: 14px;
}

.home-banner-desc {
  font-size: 15.5px;
  color: var(--slate);
  line-height: 1.75;
  max-width: 720px;
  margin: 0 auto 22px;
}

.home-banner-btns {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-bottom: 20px;
}

.home-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 26px;
  border-radius: 24px;
  font-size: 14.5px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  text-decoration: none;
  transition: transform 0.25s ease, box-shadow 0.25s ease, background 0.2s;
}

.home-btn-primary {
  background: linear-gradient(90deg, #38b6ff, #2a8fff);
  color: #fff;
  box-shadow: 0 6px 18px rgba(42, 143, 255, 0.4);
  position: relative;
  overflow: hidden;

  &:after {
    content: "";
    position: absolute;
    top: 0; left: -75%;
    width: 50%; height: 100%;
    background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.55), transparent);
    transform: skew(-20deg);
    animation: shine 3s infinite;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 22px rgba(42, 143, 255, 0.55);
  }
}

.home-btn-ghost {
  background: rgba(255, 255, 255, 0.85);
  color: var(--navy);
  border: 1px solid rgba(42, 143, 255, 0.35);
  backdrop-filter: blur(4px);

  &:hover {
    transform: translateY(-2px);
    background: #fff;
    color: var(--blue-deep);
  }
}

.home-banner-features {
  display: flex;
  justify-content: center;
  gap: 28px;
  color: #2a8fff;
  font-size: 13.5px;
  font-weight: 500;
  flex-wrap: wrap;

  span {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 6px;

    &:before {
      content: "";
      display: inline-block;
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: #38b6ff;
      box-shadow: 0 0 8px #38b6ff;
      animation: blink 1.6s ease-in-out infinite;
    }
    &:nth-child(2):before { animation-delay: 0.4s; }
    &:nth-child(3):before { animation-delay: 0.8s; }
  }
}

.home-banner-fade {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 90px;
  background: linear-gradient(to bottom, transparent 0%, rgba(242, 248, 254, 0.85) 70%, var(--sky) 100%);
  z-index: 0;
  pointer-events: none;
}

/* --- 2. 悬浮公告栏 --- */
.home-notice {
  position: relative;
  z-index: 10;
  margin-top: -32px;
  margin-bottom: 12px;
}

.home-notice-bar {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  background: linear-gradient(110deg, #1b6fe0, #2b86f0 45%, #21c6e8);
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px 24px;
  box-shadow: 0 16px 36px -18px rgba(27, 98, 214, 0.55);

  &:after {
    content: "";
    position: absolute;
    top: 0; right: 0; bottom: 0; left: 0;
    background-image: radial-gradient(rgba(255, 255, 255, 0.16) 1px, transparent 1px);
    background-size: 18px 18px;
    opacity: 0.5;
    pointer-events: none;
  }
}

.home-notice-chip {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  font-size: 12.5px;
  font-weight: 700;
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 6px;
  padding: 6px 12px;
}

.home-notice-bar .txt {
  position: relative;
  z-index: 1;
  flex: 1;
  font-size: 14.5px;
  font-weight: 600;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-notice-bar .date {
  position: relative;
  z-index: 1;
  font-family: var(--mono);
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.8);
}

.home-notice-bar .more {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--blue-deep);
  background: #fff;
  padding: 7px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  svg {
    width: 14px;
    height: 14px;
    transition: transform 0.2s;
  }

  &:hover {
    background: #f0f7ff;
    svg { transform: translateX(3px); }
  }
}

/* --- 通用 Section & 标题 --- */
.home-section {
  padding: 56px 0;
  position: relative;
}

.home-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--mono);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--azure);

  .idx {
    color: var(--cyan);
    font-weight: 800;
  }
  .bar {
    width: 24px;
    height: 2px;
    background: var(--grad);
    border-radius: 2px;
  }
}

.home-s-head {
  margin-bottom: 32px;
  h2 {
    font-size: clamp(26px, 3.2vw, 36px);
    font-weight: 800;
    color: var(--ink);
    letter-spacing: -0.01em;
    line-height: 1.15;
    margin-top: 10px;
  }
  p {
    margin-top: 8px;
    color: var(--slate);
    max-width: 600px;
    font-size: 15px;
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
}

/* --- 4. 1:1 IAIC Track Cards Grid --- */
.home-track-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 22px;
}

.home-track {
  position: relative;
  background: var(--card);
  border-radius: 18px;
  border: 1px solid var(--line);
  padding: 26px 28px 22px;
  box-shadow: 0 1px 3px rgba(19, 41, 79, 0.05);
  overflow: hidden;
  transition: transform 0.22s, box-shadow 0.22s, border-color 0.22s;
  display: flex;
  flex-direction: column;
  cursor: pointer;

  &:after {
    content: "";
    position: absolute;
    top: 0; left: -60%;
    width: 50%; height: 100%;
    background: linear-gradient(100deg, transparent, rgba(33, 198, 232, 0.16), transparent);
    transform: skew(-18deg);
    transition: left 0.55s ease;
    pointer-events: none;
  }

  &:hover {
    transform: translateY(-5px);
    box-shadow: var(--shadow);
    border-color: rgba(43, 134, 240, 0.4);

    &:after {
      left: 130%;
    }
    .corner {
      opacity: 0.75;
    }
    .home-track-icon {
      background: var(--grad);
      border-color: transparent;
      svg { stroke: #fff; }
    }
  }

  .corner {
    position: absolute;
    width: 14px;
    height: 14px;
    border: 2px solid var(--cyan);
    opacity: 0;
    transition: opacity 0.25s;
    pointer-events: none;

    &.c1 { top: 10px; left: 10px; border-right: none; border-bottom: none; }
    &.c2 { bottom: 10px; right: 10px; border-left: none; border-top: none; }
  }
}

.home-track-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.home-track-code {
  font-family: var(--mono);
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--azure);
  background: var(--sky-2);
  padding: 4px 10px;
  border-radius: 6px;
}

.home-track-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--grad-soft);
  border: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.25s, border-color 0.25s;

  svg {
    width: 22px;
    height: 22px;
    stroke: var(--azure);
    transition: stroke 0.25s;
  }
}

.track-body {
  flex: 1;

  .track-header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;

    h3 {
      font-size: 18px;
      font-weight: 800;
      color: var(--ink);
      line-height: 1.3;
      margin: 0;
    }
  }

  .match-pill {
    font-size: 12px;
    font-weight: 700;
    color: var(--green);
    background: var(--green-bg);
    padding: 3px 8px;
    border-radius: 6px;
    flex-shrink: 0;
  }

  .track-tags-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;

    .track-tag {
      font-size: 11.5px;
      font-weight: 600;
      padding: 3px 8px;
      border-radius: 6px;

      &.bloom {
        background: #ede9fe;
        color: #7c3aed;
      }
      &.capstone {
        background: #fef3c7;
        color: #d97706;
      }
      &.stage-tag {
        background: var(--sky-2);
        color: var(--azure);
      }
    }
  }

  .track-reason {
    font-size: 13.5px;
    color: var(--slate);
    line-height: 1.6;
    margin-bottom: 16px;
    background: var(--sky);
    padding: 8px 12px;
    border-radius: 8px;
    border: 1px dashed var(--line);

    .bulb {
      font-size: 14px;
    }
  }
}

.home-track-foot {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px dashed var(--line-2);
  display: flex;
  align-items: center;
  justify-content: space-between;

  .open {
    font-family: var(--mono);
    font-size: 12px;
    color: var(--green);
    display: flex;
    align-items: center;
    gap: 6px;

    &:before {
      content: "";
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--green);
      box-shadow: 0 0 6px var(--green);
      animation: pulse 2s infinite;
    }
  }

  .home-track-link {
    font-size: 14px;
    font-weight: 700;
    color: var(--azure);
    display: inline-flex;
    align-items: center;
    gap: 6px;

    .track-price {
      color: #ef4444;
      font-size: 16px;
    }
    .track-price-free {
      color: var(--green);
      font-size: 14px;
    }
    .arrow {
      transition: transform 0.2s;
    }
  }

  &:hover .arrow {
    transform: translateX(3px);
  }
}

/* --- 5. 继续学习卡片 --- */
.continue-wrap {
  margin-bottom: 24px;
}

.continue-iaic-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 20px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(19, 41, 79, 0.04);

  .c-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .c-icon-badge {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: var(--grad-soft);
    color: var(--azure);
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .c-text {
    .c-label { font-size: 11.5px; color: var(--slate); font-weight: 500; }
    h4 { font-size: 16px; font-weight: 800; color: var(--ink); margin: 3px 0; }
    .c-sub { font-size: 12px; color: var(--slate-2); }
  }
  .c-right {
    display: flex;
    align-items: center;
    gap: 20px;
  }
  .c-progress {
    width: 160px;
  }
  .mini-btn {
    padding: 8px 18px;
    font-size: 13px;
  }
}

/* --- 6. 1:1 IAIC 四维一体导学流程 --- */
.home-flow-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.home-step {
  position: relative;
  background: var(--card);
  border-radius: 16px;
  border: 1px solid var(--line);
  padding: 28px 24px;
  box-shadow: 0 1px 2px rgba(19, 41, 79, 0.04);
  transition: transform 0.2s, border-color 0.2s;

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(43, 134, 240, 0.4);
    box-shadow: var(--shadow);
  }

  .home-step-num {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    background: var(--grad);
    color: #fff;
    font-family: var(--display);
    font-weight: 900;
    font-size: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;
    box-shadow: 0 4px 12px rgba(33, 198, 232, 0.35);
  }

  h4 {
    font-size: 16.5px;
    font-weight: 800;
    color: var(--ink);
    margin-bottom: 8px;
  }

  p {
    font-size: 13px;
    color: var(--slate);
    line-height: 1.65;
    margin: 0;
  }
}

/* 学习路径弹窗 */
.learning-path-dialog {
  .path-modal-header {
    background: var(--sky-2);
    border-radius: 12px;
    padding: 16px 20px;
    margin-bottom: 20px;

    .role-badge {
      font-size: 15px;
      font-weight: 800;
      color: var(--azure);
    }
    .goal-text {
      font-size: 13.5px;
      color: var(--navy);
      margin: 6px 0;
    }
    .stats-row {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: var(--slate);
      font-family: var(--mono);
    }
  }

  .timeline-stage-item {
    display: flex;
    gap: 16px;
    margin-bottom: 16px;

    .stage-badge-circle {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--azure);
      color: #fff;
      font-weight: 800;
      font-size: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .stage-card {
      flex: 1;
      background: var(--sky);
      border: 1px solid var(--line);
      border-radius: 10px;
      padding: 12px 16px;

      .stage-card-title-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        .stg-name { font-size: 14.5px; font-weight: 700; color: var(--ink); margin: 0; }
        .stg-hours { font-family: var(--mono); font-size: 12px; color: var(--azure); font-weight: 600; }
      }

      .stg-goal { font-size: 12.5px; color: var(--slate); margin: 4px 0 8px; }

      .stg-courses-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        .course-chip {
          font-size: 11.5px;
          background: #fff;
          border: 1px solid var(--line);
          padding: 3px 8px;
          border-radius: 6px;
          cursor: pointer;
          color: var(--navy);
          &:hover { color: var(--azure); border-color: var(--azure); }
        }
      }
    }
  }
}

@media (max-width: 992px) {
  .home-track-grid {
    grid-template-columns: 1fr;
  }
  .home-flow-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
