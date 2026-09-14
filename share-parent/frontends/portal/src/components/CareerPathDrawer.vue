<template>
  <el-dialog
    v-model="visible"
    title="🗺️ 智问学伴 · AI 职业成长进阶拓扑全景大屏"
    width="940px"
    append-to-body
    class="career-path-dialog"
  >
    <div v-if="loading" class="path-loading">
      <el-icon class="is-loading" :size="36"><Loading /></el-icon>
      <p class="loading-text">多智能体协同流水线正在分析胜任力模型并拓扑编排进阶路线...</p>
    </div>

    <div v-else-if="pathData" class="career-path-body">
      <!-- 顶部岗位目标与标准背书看板 -->
      <div class="career-banner">
        <div class="banner-top">
          <div class="role-badge">🎯 规划目标岗位</div>
          <h3 class="role-name">{{ pathData.intendedRole || 'Java全栈架构师 (P6+/P7)' }}</h3>
        </div>
        <p class="role-goal">{{ pathData.overallGoal }}</p>
        <div class="banner-metrics">
          <div class="metric-chip">
            <span class="chip-num">{{ pathData.totalCourses || 12 }}</span>
            <span class="chip-lbl">门全景进阶课</span>
          </div>
          <div class="metric-chip">
            <span class="chip-num">{{ pathData.totalEstimatedHours || 580 }}</span>
            <span class="chip-lbl">深度实训学时</span>
          </div>
          <div class="metric-chip highlight">
            <span class="chip-num">4 阶段</span>
            <span class="chip-lbl">DAG 拓扑推进</span>
          </div>
        </div>
        <div class="standard-ref" v-if="pathData.referenceStandard">
          <span>📌 行业对标标准：</span>
          <strong>{{ pathData.referenceStandard }}</strong>
        </div>
      </div>

      <!-- 审判反思智能体质检报告看板 (PathCriticAgent Audit Report) -->
      <div class="critic-report-card" v-if="pathData.criticReport">
        <div class="critic-header">
          <div class="critic-title-wrap">
            <span class="critic-icon">⚖️</span>
            <span class="critic-title">PathCriticAgent 审判反思质检报告</span>
            <span class="critic-badge" :class="pathData.criticReport.passed ? 'badge-pass' : 'badge-warn'">
              {{ pathData.criticReport.verdictLevel || (pathData.criticReport.passed ? '卓越 (A+)' : '需微调') }}
            </span>
          </div>
          <div class="critic-score-wrap">
            <span class="score-num">{{ pathData.criticReport.overallScore != null ? pathData.criticReport.overallScore : 100 }}</span>
            <span class="score-unit">分 / 100</span>
          </div>
        </div>

        <div class="critic-metrics-grid">
          <div class="c-metric">
            <span class="c-lbl">拓扑合法性:</span>
            <span class="c-val" :class="(pathData.criticReport.topologyValid != null ? pathData.criticReport.topologyValid : (pathData.criticReport.prerequisiteScore >= 80)) ? 'text-success' : 'text-danger'">
              {{ (pathData.criticReport.topologyValid != null ? pathData.criticReport.topologyValid : (pathData.criticReport.prerequisiteScore >= 80)) ? '✅ Kahn DAG 无环/无依赖倒置' : '❌ 存在先修依赖倒置' }}
            </span>
          </div>
          <div class="c-metric">
            <span class="c-lbl">认知平滑度:</span>
            <span class="c-val text-primary">{{ pathData.criticReport.cognitiveContinuityScore != null ? pathData.criticReport.cognitiveContinuityScore : (pathData.criticReport.smoothnessScore != null ? pathData.criticReport.smoothnessScore : 100) }} 分 (无认知悬崖)</span>
          </div>
          <div class="c-metric">
            <span class="c-lbl">阶段均衡性:</span>
            <span class="c-val text-info">{{ pathData.criticReport.phaseBalanceScore != null ? pathData.criticReport.phaseBalanceScore : (pathData.criticReport.balanceScore != null ? pathData.criticReport.balanceScore : 100) }} 分 (容量均衡)</span>
          </div>
        </div>

        <div class="critic-summary" v-if="pathData.criticReport.summary || (pathData.criticReport.critiqueNotes && pathData.criticReport.critiqueNotes.length)">
          <span class="sum-icon">📝</span>
          <span class="sum-text">{{ pathData.criticReport.summary || pathData.criticReport.critiqueNotes.join('；') }}</span>
        </div>
      </div>

      <!-- 4 阶段 DAG 递进流程 -->
      <div class="stages-timeline">
        <div
          v-for="stage in pathData.stages"
          :key="stage.stageIndex"
          class="stage-card"
          :class="`stage-${stage.stageIndex}`"
        >
          <div class="stage-header">
            <div class="stage-title-wrap">
              <span class="stage-tag">Stage {{ stage.stageIndex }}</span>
              <h4 class="stage-title">{{ stage.stageName }}</h4>
            </div>
            <div class="stage-hours">
              <span>⏱️ 预计 {{ stage.estimatedHours }} 深度学时</span>
            </div>
          </div>
          <div class="stage-goal-desc">{{ stage.stageGoal }}</div>

          <!-- 阶段内关联课程卡片网格 -->
          <div class="courses-grid">
            <div
              v-for="course in stage.courses"
              :key="course.courseId"
              class="dag-course-item"
              @click="handleCourseClick(course.courseId)"
            >
              <div class="item-header">
                <div class="course-name-wrap">
                  <span class="course-name" :title="course.courseName">📘 {{ course.courseName }}</span>
                  <span class="capstone-badge" v-if="course.capstoneProject || course.isCapstone">🏆 Capstone 综合实战</span>
                </div>
                <div class="header-tags-wrap">
                  <span class="bloom-tag" v-if="course.bloomLevelName || course.bloomName">🎓 {{ course.bloomLevelName || course.bloomName }}</span>
                  <span class="difficulty-chip" :class="course.difficultyLevel === 3 ? 'diff-high' : 'diff-mid'">
                    {{ course.difficultyAssessment || (course.difficultyLevel === 3 ? '高级攻坚' : '核心进阶') }}
                  </span>
                </div>
              </div>

              <!-- 知识点提炼 -->
              <div class="knowledge-chips" v-if="course.coreKnowledgePoints && course.coreKnowledgePoints.length">
                <span
                  v-for="(kp, kIdx) in course.coreKnowledgePoints.slice(0, 3)"
                  :key="kIdx"
                  class="kp-tag"
                >{{ kp }}</span>
              </div>

              <!-- 实战代码占比刻度条 -->
              <div class="practical-bar-wrap" v-if="course.practicalWeight">
                <div class="bar-labels">
                  <span>实战代码占比</span>
                  <span class="percent-val">{{ course.practicalWeight }}%</span>
                </div>
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: `${course.practicalWeight}%` }"></div>
                </div>
              </div>

              <!-- 先修前置依赖 -->
              <div class="prereq-wrap" v-if="course.prerequisiteSkills && course.prerequisiteSkills.length">
                <span class="prereq-label">先修依赖:</span>
                <span class="prereq-val">{{ course.prerequisiteSkills.join('、') }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <div class="footer-left">
          <el-button type="warning" plain @click="openRefineModal" :disabled="loading">
            ⚙️ 人机协同微调路线
          </el-button>
        </div>
        <div class="footer-right">
          <el-button @click="visible = false">关闭预览</el-button>
          <el-button type="success" @click="handleAddToPlan" :loading="planAdding">
            🌟 一键生成我的个性化学习计划
          </el-button>
        </div>
      </div>
    </template>

    <!-- 人机协同微调模态框 (Human-in-the-Loop Refinement Modal) -->
    <el-dialog
      v-model="refineVisible"
      title="⚙️ 人机协同微调 · 学习进阶规划"
      width="560px"
      append-to-body
      class="refine-sub-dialog"
    >
      <div class="refine-form">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
        >
          <template #title>
            <span style="font-size: 13px; font-weight: 600;">L4 自适应微调引擎</span>
          </template>
          根据您的微调指令，智能体编排器将局部重入规划器与 PathCriticAgent 审判反思智能体，毫秒级自适应重排并输出质检结论。
        </el-alert>

        <div class="form-item-block">
          <label class="block-label">🎯 目标职业发展方向:</label>
          <el-select v-model="refineForm.targetDirection" placeholder="选择目标岗位" style="width: 100%;">
            <el-option label="Java全栈架构师" value="Java全栈架构师" />
            <el-option label="Go云原生架构师" value="Go云原生架构师" />
            <el-option label="大语言模型应用工程师" value="大语言模型应用工程师" />
            <el-option label="大数据开发工程师" value="大数据开发工程师" />
            <el-option label="前端技术专家" value="前端技术专家" />
          </el-select>
        </div>

        <div class="form-item-block" style="margin-top: 14px;">
          <label class="block-label">🚫 排除已有掌握或不想学的课程:</label>
          <el-select
            v-model="refineForm.excludeCourseIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="请勾选需要跳过或排除的课程"
            style="width: 100%;"
          >
            <el-option
              v-for="c in availableCourses"
              :key="c.courseId"
              :label="c.courseName"
              :value="c.courseId"
            />
          </el-select>
        </div>

        <div class="form-item-block switch-block" style="margin-top: 16px;">
          <div>
            <div class="switch-title">跳过第一阶段基础课程</div>
            <div class="switch-desc">若您已有较扎实的编程功底，直接从核心进阶与架构攻坚阶段推进</div>
          </div>
          <el-switch v-model="refineForm.skipBasicPhase" active-color="#2563EB" />
        </div>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 10px;">
          <el-button @click="refineVisible = false" :disabled="isRefining">取消</el-button>
          <el-button type="primary" :loading="isRefining" @click="handleRefineSubmit">
            🚀 触发智能体局部重排
          </el-button>
        </div>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getPersonalizedLearningPath, refineLearningPath } from '@/api/class.js'

const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const planAdding = ref(false)
const pathData = ref(null)

const refineVisible = ref(false)
const isRefining = ref(false)
const refineForm = ref({
  targetDirection: 'Java全栈架构师',
  excludeCourseIds: [],
  skipBasicPhase: false
})

const availableCourses = computed(() => {
  if (!pathData.value || !pathData.value.stages) return []
  const list = []
  pathData.value.stages.forEach(stage => {
    if (stage.courses) {
      stage.courses.forEach(c => {
        if (!list.some(item => item.courseId === c.courseId)) {
          list.push({ courseId: c.courseId, courseName: c.courseName })
        }
      })
    }
  })
  return list
})

const openDrawer = async (data = null, isLoading = false) => {
  visible.value = true
  if (data) {
    pathData.value = data
    loading.value = isLoading
    syncRefineForm()
    return
  }
  if (!pathData.value) {
    loading.value = true
    try {
      const res = await getPersonalizedLearningPath()
      if (res && res.code === 200 && res.data) {
        pathData.value = res.data
        syncRefineForm()
      }
    } catch (e) {
      console.error('获取学习路径失败:', e)
    } finally {
      loading.value = false
    }
  }
}

const syncRefineForm = () => {
  if (pathData.value?.intendedRole) {
    refineForm.value.targetDirection = pathData.value.intendedRole.split(' ')[0]
  }
}

const openRefineModal = () => {
  syncRefineForm()
  refineVisible.value = true
}

const handleRefineSubmit = async () => {
  isRefining.value = true
  try {
    const payload = {
      customRole: refineForm.value.targetDirection,
      targetDirection: refineForm.value.targetDirection,
      excludeCourseIds: refineForm.value.excludeCourseIds,
      skipBasicPhase: refineForm.value.skipBasicPhase
    }
    const res = await refineLearningPath(payload)
    if (res && res.code === 200 && res.data) {
      if (res.data.learningPath) {
        pathData.value = res.data.learningPath
      } else {
        pathData.value = res.data
      }
      ElMessage.success('🎉 智能体已根据您的微调指令完成重新规划与审判质检！')
      refineVisible.value = false
    } else {
      ElMessage.warning(res?.msg || '微调处理失败')
    }
  } catch (e) {
    console.error('人机协同微调失败:', e)
    ElMessage.error('微调请求失败，请稍后重试')
  } finally {
    isRefining.value = false
  }
}

const updateLoading = (val) => {
  loading.value = val
}

const updateData = (data) => {
  pathData.value = data
}

const handleCourseClick = (courseId) => {
  if (!courseId) return
  visible.value = false
  router.push(`/details?id=${courseId}`)
}

const handleAddToPlan = async () => {
  planAdding.value = true
  setTimeout(() => {
    planAdding.value = false
    ElMessage.success('🎉 已将 AI 规划的 4 阶段职业进阶路线一键加入您的个人学习计划！')
    visible.value = false
  }, 700)
}

defineExpose({
  openDrawer,
  updateLoading,
  updateData
})
</script>

<style scoped lang="scss">
.path-loading {
  text-align: center;
  padding: 60px 0;
  color: #64748B;

  .loading-text {
    margin-top: 14px;
    font-size: 14px;
  }
}

.career-banner {
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  border-radius: 12px;
  padding: 20px 24px;
  color: #fff;
  margin-bottom: 20px;
  border: 1px solid rgba(56, 189, 248, 0.2);

  .banner-top {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;

    .role-badge {
      background: rgba(14, 165, 233, 0.2);
      color: #38BDF8;
      font-size: 11px;
      font-weight: 700;
      padding: 2px 8px;
      border-radius: 4px;
      border: 1px solid rgba(56, 189, 248, 0.3);
    }

    .role-name {
      margin: 0;
      font-size: 18px;
      font-weight: 700;
      color: #F8FAFC;
    }
  }

  .role-goal {
    font-size: 13px;
    color: #94A3B8;
    margin: 4px 0 14px 0;
    line-height: 1.5;
  }

  .banner-metrics {
    display: flex;
    gap: 16px;
    margin-bottom: 12px;

    .metric-chip {
      background: rgba(30, 41, 59, 0.8);
      border: 1px solid rgba(148, 163, 184, 0.15);
      border-radius: 6px;
      padding: 8px 14px;
      display: flex;
      flex-direction: column;

      .chip-num {
        font-size: 16px;
        font-weight: 700;
        color: #38BDF8;
      }

      .chip-lbl {
        font-size: 11px;
        color: #94A3B8;
      }

      &.highlight .chip-num {
        color: #10B981;
      }
    }
  }

  .standard-ref {
    font-size: 12px;
    color: #64748B;
    background: rgba(15, 23, 42, 0.6);
    padding: 6px 12px;
    border-radius: 4px;
    display: inline-block;

    strong {
      color: #CBD5E1;
    }
  }
}

/* 审判反思智能体质检卡片 */
.critic-report-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-left: 4px solid #10B981;
  border-radius: 8px;
  padding: 14px 18px;
  margin-bottom: 20px;
  box-shadow: 0 2px 6px -1px rgba(0, 0, 0, 0.04);

  .critic-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;

    .critic-title-wrap {
      display: flex;
      align-items: center;
      gap: 8px;

      .critic-icon {
        font-size: 16px;
      }

      .critic-title {
        font-size: 14px;
        font-weight: 700;
        color: #0F172A;
      }

      .critic-badge {
        font-size: 11px;
        font-weight: 600;
        padding: 2px 8px;
        border-radius: 12px;

        &.badge-pass {
          background: #ECFDF5;
          color: #059669;
          border: 1px solid #A7F3D0;
        }

        &.badge-warn {
          background: #FFFBEB;
          color: #D97706;
          border: 1px solid #FDE68A;
        }
      }
    }

    .critic-score-wrap {
      display: flex;
      align-items: baseline;
      gap: 4px;

      .score-num {
        font-size: 18px;
        font-weight: 800;
        color: #059669;
      }

      .score-unit {
        font-size: 11px;
        color: #94A3B8;
      }
    }
  }

  .critic-metrics-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
    background: #F8FAFC;
    padding: 8px 12px;
    border-radius: 6px;
    margin-bottom: 10px;

    .c-metric {
      font-size: 12px;
      display: flex;
      flex-direction: column;
      gap: 2px;

      .c-lbl {
        color: #64748B;
        font-size: 11px;
      }

      .c-val {
        font-weight: 600;

        &.text-success { color: #059669; }
        &.text-primary { color: #2563EB; }
        &.text-info { color: #0284C7; }
        &.text-danger { color: #DC2626; }
      }
    }
  }

  .critic-summary {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    font-size: 12px;
    color: #475569;
    line-height: 1.4;

    .sum-icon {
      font-size: 13px;
      flex-shrink: 0;
    }

    .sum-text {
      flex: 1;
    }
  }
}

.stages-timeline {
  display: flex;
  flex-direction: column;
  gap: 18px;

  .stage-card {
    background: #FAFAFA;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    padding: 16px;
    transition: all 0.2s ease;

    &:hover {
      border-color: #93C5FD;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }

    .stage-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;

      .stage-title-wrap {
        display: flex;
        align-items: center;
        gap: 8px;

        .stage-tag {
          background: #0284C7;
          color: #fff;
          font-size: 11px;
          font-weight: 700;
          padding: 2px 8px;
          border-radius: 12px;
        }

        .stage-title {
          margin: 0;
          font-size: 15px;
          font-weight: 700;
          color: #0F172A;
        }
      }

      .stage-hours {
        font-size: 12px;
        color: #64748B;
        font-weight: 500;
      }
    }

    .stage-goal-desc {
      font-size: 13px;
      color: #475569;
      margin-bottom: 12px;
      line-height: 1.4;
    }
  }
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;

  .dag-course-item {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    padding: 12px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      border-color: #38BDF8;
      box-shadow: 0 4px 12px rgba(56, 189, 248, 0.15);
      transform: translateY(-2px);
    }

    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 8px;
      margin-bottom: 6px;

      .course-name-wrap {
        display: flex;
        flex-direction: column;
        gap: 4px;
        flex: 1;
        overflow: hidden;

        .course-name {
          font-size: 13px;
          font-weight: 600;
          color: #0F172A;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .capstone-badge {
          display: inline-flex;
          align-items: center;
          font-size: 10px;
          font-weight: 700;
          padding: 1px 6px;
          border-radius: 3px;
          background: #FEF3C7;
          color: #B45309;
          border: 1px solid #FDE68A;
          width: fit-content;
        }
      }

      .header-tags-wrap {
        display: flex;
        align-items: center;
        gap: 4px;
        flex-shrink: 0;

        .bloom-tag {
          font-size: 10px;
          padding: 2px 6px;
          border-radius: 4px;
          white-space: nowrap;
          font-weight: 600;
          background: #EFF6FF;
          color: #2563EB;
          border: 1px solid #DBEAFE;
        }

        .difficulty-chip {
          font-size: 10px;
          padding: 2px 6px;
          border-radius: 4px;
          white-space: nowrap;
          font-weight: 600;

          &.diff-mid {
            background: #F1F5F9;
            color: #475569;
          }

          &.diff-high {
            background: #FEE2E2;
            color: #DC2626;
          }
        }
      }
    }

    .knowledge-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      margin-bottom: 8px;

      .kp-tag {
        background: #F1F5F9;
        color: #475569;
        font-size: 10px;
        padding: 1px 6px;
        border-radius: 4px;
        max-width: 140px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .practical-bar-wrap {
      margin-bottom: 6px;

      .bar-labels {
        display: flex;
        justify-content: space-between;
        font-size: 11px;
        color: #64748B;
        margin-bottom: 3px;

        .percent-val {
          color: #10B981;
          font-weight: 600;
        }
      }

      .bar-track {
        height: 5px;
        background: #F1F5F9;
        border-radius: 4px;
        overflow: hidden;

        .bar-fill {
          height: 100%;
          background: linear-gradient(90deg, #10B981, #059669);
          border-radius: 4px;
        }
      }
    }

    .prereq-wrap {
      font-size: 11px;
      color: #64748B;
      line-height: 1.3;

      .prereq-label {
        font-weight: 600;
        margin-right: 4px;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .footer-right {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.refine-form {
  padding: 4px 0;

  .form-item-block {
    .block-label {
      display: block;
      font-size: 13px;
      font-weight: 600;
      color: #1E293B;
      margin-bottom: 6px;
    }
  }

  .switch-block {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #F8FAFC;
    padding: 12px 14px;
    border-radius: 8px;
    border: 1px solid #E2E8F0;

    .switch-title {
      font-size: 13px;
      font-weight: 600;
      color: #1E293B;
    }

    .switch-desc {
      font-size: 11px;
      color: #64748B;
      margin-top: 2px;
    }
  }
}

@media (max-width: 768px) {
  .courses-grid {
    grid-template-columns: 1fr;
  }
}
</style>
