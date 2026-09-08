<template>
  <el-dialog
    v-model="visible"
    title="🗺️ 智问学伴 · AI 职业成长进阶拓扑全景大屏"
    width="920px"
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
                <span class="course-name" :title="course.courseName">📘 {{ course.courseName }}</span>
                <span class="difficulty-chip" :class="course.difficultyLevel === 3 ? 'diff-high' : 'diff-mid'">
                  {{ course.difficultyAssessment || (course.difficultyLevel === 3 ? '高级攻坚' : '核心进阶') }}
                </span>
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
        <el-button @click="visible = false">关闭预览</el-button>
        <el-button type="success" @click="handleAddToPlan" :loading="planAdding">
          🌟 一键生成我的个性化学习计划
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getPersonalizedLearningPath } from '@/api/class.js'

const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const planAdding = ref(false)
const pathData = ref(null)

const openDrawer = async (data = null, isLoading = false) => {
  visible.value = true
  if (data) {
    pathData.value = data
    loading.value = isLoading
    return
  }
  if (!pathData.value) {
    loading.value = true
    try {
      const res = await getPersonalizedLearningPath()
      if (res && res.code === 200 && res.data) {
        pathData.value = res.data
      }
    } catch (e) {
      console.error('获取学习路径失败:', e)
    } finally {
      loading.value = false
    }
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
  margin-bottom: 24px;
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

      .course-name {
        font-size: 13px;
        font-weight: 600;
        color: #0F172A;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        flex: 1;
      }

      .difficulty-chip {
        font-size: 10px;
        padding: 2px 6px;
        border-radius: 4px;
        white-space: nowrap;
        font-weight: 600;

        &.diff-mid {
          background: #EFF6FF;
          color: #2563EB;
        }

        &.diff-high {
          background: #FEF3C7;
          color: #D97706;
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
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .courses-grid {
    grid-template-columns: 1fr;
  }
}
</style>
