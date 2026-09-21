<!-- 问答社区 - IAIC 科技蓝互动交流中心 -->
<template>
  <div class="ask-page">
    <!-- 1. 社区顶部 IAIC 科技 Banner -->
    <section class="ask-hero">
      <div class="hero-glow"></div>
      <div class="home-wrap hero-content">
        <div class="home-eyebrow">
          <span class="idx">01</span>
          <span class="bar"></span>
          <span>INTERACTIVE Q&A & PEER LEARNING</span>
        </div>
        <h1 class="hero-title">技术问答社区 · <span class="accent">产学研协同互助</span></h1>
        <p class="hero-desc">
          与讲师及万千技术同行共同探讨微服务架构、底层内核、大模型应用与工程实战难点。
        </p>

        <!-- 搜索与发起提问操作栏 -->
        <div class="hero-action-bar">
          <div class="search-input-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索感兴趣的技术疑难、关键词（如：MySQL、Redis、微服务）..."
              size="large"
              clearable
              class="ask-search-input"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          <button class="iaic-btn-primary" @click="openAskModal">
            <span>✍️ 发起提问</span>
          </button>
        </div>
      </div>
    </section>

    <!-- 2. 问答内容主体区 -->
    <div class="home-wrap ask-main-wrap">
      <div class="ask-layout">
        <!-- 左侧：分类与问题列表 -->
        <div class="ask-content-area">
          <!-- 筛选标签栏 -->
          <div class="filter-tab-strip">
            <div class="tab-pills">
              <button
                v-for="t in filterTabs"
                :key="t.key"
                :class="['filter-pill', { active: activeTab === t.key }]"
                @click="switchTab(t.key)"
              >
                {{ t.label }}
              </button>
            </div>
            <div class="total-badge">共 {{ totalCount }} 条讨论</div>
          </div>

          <!-- 问题列表卡片流 -->
          <div v-loading="loading" class="questions-stream">
            <div v-if="!loading && questionsList.length === 0" class="empty-state">
              <div class="empty-icon">💬</div>
              <p>暂无相关问答，快来发起第一个提问吧！</p>
              <el-button type="primary" round size="small" @click="openAskModal">立即发起提问</el-button>
            </div>

            <div
              v-for="item in questionsList"
              :key="item.id"
              class="question-card"
              @click="goQuestionDetail(item.id)"
            >
              <div class="corner c1"></div>
              <div class="corner c2"></div>

              <div class="q-header">
                <div class="author-info">
                  <div class="author-avatar">
                    <img v-if="item.userIcon" :src="formatAvatarUrl(item.userIcon)" alt="avatar" />
                    <span v-else>{{ (item.userName || '学')[0] }}</span>
                  </div>
                  <span class="author-name">{{ item.userName || '学习者' }}</span>
                  <span class="post-time">{{ formatTime(item.createTime) }}</span>
                </div>
                <span class="category-pill" v-if="item.category || item.sectionId">
                  {{ item.category || item.sectionId }}
                </span>
              </div>

              <h3 class="q-title">{{ item.title }}</h3>
              <p class="q-desc">{{ item.content || item.description || '暂无详细描述' }}</p>

              <!-- 最新回复卡片 -->
              <div class="latest-reply-box" v-if="item.latestReplyContent">
                <span class="reply-badge">最新解答</span>
                <span class="reply-user">{{ item.latestReplyUser || '讲师' }}:</span>
                <span class="reply-text">{{ item.latestReplyContent }}</span>
              </div>

              <div class="q-footer">
                <div class="stats-group">
                  <span class="stat-item"><el-icon><ChatDotRound /></el-icon> {{ item.replyCount || item.replyTimes || 0 }} 回答</span>
                  <span class="stat-item"><el-icon><Pointer /></el-icon> {{ item.likeCount || item.likedTimes || 0 }} 点赞</span>
                  <span class="stat-item"><el-icon><View /></el-icon> {{ item.viewCount || 10 }} 浏览</span>
                </div>
                <div class="view-detail-link">
                  <span>查看讨论 ➔</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页器 -->
          <div class="pagination-wrap" v-if="totalCount > pageSize">
            <el-pagination
              v-model:current-page="pageNo"
              :page-size="pageSize"
              :total="totalCount"
              layout="prev, pager, next"
              background
              @current-change="loadQuestions"
            />
          </div>
        </div>

        <!-- 右侧：侧边栏推荐 -->
        <div class="ask-sidebar">
          <!-- 提问互助倡议卡片 -->
          <div class="side-card">
            <h4>💡 社区提问倡议</h4>
            <ul class="side-tips-list">
              <li>清晰描述问题背景与复现步骤</li>
              <li>附带精简代码或异常堆栈日志</li>
              <li>提问前可先通过顶部搜索查找已有解答</li>
              <li>获得满意答案后及时点赞并标记采纳</li>
            </ul>
          </div>

          <!-- 热门答疑先锋 -->
          <div class="side-card">
            <h4>🏆 社区答疑先锋</h4>
            <div class="pioneer-list">
              <div class="pioneer-item">
                <span class="rank r1">01</span>
                <span class="p-name">张老师 (Spring Cloud 专家)</span>
                <span class="p-score">1,280 积分</span>
              </div>
              <div class="pioneer-item">
                <span class="rank r2">02</span>
                <span class="p-name">王老师 (高并发内核专家)</span>
                <span class="p-score">960 积分</span>
              </div>
              <div class="pioneer-item">
                <span class="rank r3">03</span>
                <span class="p-name">李老师 (算法与分布式讲师)</span>
                <span class="p-score">820 积分</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 发起提问弹窗 (Ask Modal) -->
    <el-dialog
      v-model="askModalVisible"
      title="✍️ 发起技术提问"
      width="640px"
      append-to-body
      class="ask-form-dialog"
    >
      <el-form
        ref="askFormRef"
        :model="askForm"
        :rules="askRules"
        label-position="top"
        class="iaic-ask-form"
      >
        <el-form-item label="所属课程" prop="courseId">
          <el-select
            v-model="askForm.courseId"
            placeholder="请选择关联课程"
            filterable
            class="full-select"
            @change="handleCourseChange"
          >
            <el-option
              v-for="c in courseOptions"
              :key="c.id"
              :label="c.title"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关联小节 (可选)" prop="sectionId">
          <el-cascader
            v-model="selectedSection"
            :options="sectionOptions"
            :props="{ expandTrigger: 'hover', label: 'label', value: 'value' }"
            placeholder="选择具体章节小节可获得更快专业解答"
            clearable
            class="full-select"
            :disabled="!askForm.courseId"
          />
        </el-form-item>

        <el-form-item label="问题标题" prop="title">
          <el-input
            v-model="askForm.title"
            placeholder="一句话清晰概括您遇到的问题（如：Redis 分布式锁续期机制如何实现？）"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="askForm.description"
            type="textarea"
            rows="6"
            placeholder="请提供详细的代码片段、环境版本或异常报错日志，有助于学伴与讲师快速定位..."
            maxlength="600"
            show-word-limit
          />
        </el-form-item>

        <div class="form-footer-bar">
          <el-checkbox v-model="askForm.anonymity" label="匿名发布" />
          <div class="modal-btns">
            <el-button @click="askModalVisible = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="submitQuestion">确认发布</el-button>
          </div>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Search, ChatDotRound, Pointer, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getAskList, postQuestions, getClassCourses } from '@/api/classDetails.js';
import { getFreeClassList } from '@/api/class.js';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const submitting = ref(false);
const searchKeyword = ref('');
const activeTab = ref('all');
const pageNo = ref(1);
const pageSize = 10;
const totalCount = ref(0);
const questionsList = ref([]);

const filterTabs = [
  { key: 'all', label: '全部问答' },
  { key: 'latest', label: '最新提问' },
  { key: 'hot', label: '最热讨论' },
  { key: 'unsolved', label: '待解答' }
];

const askModalVisible = ref(false);
const askFormRef = ref();
const selectedSection = ref([]);
const courseOptions = ref([]);
const sectionOptions = ref([]);

const askForm = reactive({
  courseId: '',
  sectionId: '',
  title: '',
  description: '',
  anonymity: false
});

const askRules = {
  courseId: [{ required: true, message: '请选择关联课程', trigger: 'change' }],
  title: [{ required: true, message: '请输入问题标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入详细描述', trigger: 'blur' }]
};

const formatAvatarUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
  return base ? `${base}${url}` : url;
};

const formatTime = (timeStr) => {
  if (!timeStr) return '刚刚';
  return timeStr.replace('T', ' ').slice(0, 16);
};

const loadQuestions = async () => {
  loading.value = true;
  try {
    const params = {
      pageNo: pageNo.value,
      pageSize,
      keyword: searchKeyword.value.trim()
    };
    if (activeTab.value === 'unsolved') {
      params.solved = 'unsolved';
    }
    const res = await getAskList(params);
    if (res && res.code === 200 && res.data) {
      const data = res.data;
      totalCount.value = Number(data.total || 0);
      questionsList.value = Array.isArray(data.list) ? data.list : (data.records || []);
    }
  } catch (e) {
    console.error('Failed to load questions:', e);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pageNo.value = 1;
  loadQuestions();
};

const switchTab = (tabKey) => {
  activeTab.value = tabKey;
  pageNo.value = 1;
  loadQuestions();
};

const goQuestionDetail = (id) => {
  if (id) {
    router.push({ path: '/askDetails', query: { id } });
  }
};

const loadCourses = async () => {
  try {
    const res = await getFreeClassList({ pageNo: 1, pageSize: 30 });
    if (res && res.code === 200 && res.data) {
      const list = res.data.list || res.data.records || [];
      courseOptions.value = list.map(c => ({
        id: c.id,
        title: c.title || c.courseName || `课程 #${c.id}`
      }));
    }
  } catch (e) {
    console.error('Failed to load courses:', e);
  }
};

const handleCourseChange = async (courseId) => {
  selectedSection.value = [];
  sectionOptions.value = [];
  if (!courseId) return;

  try {
    const res = await getClassCourses(courseId);
    if (res && res.code === 200 && Array.isArray(res.data)) {
      sectionOptions.value = res.data.map(ch => ({
        value: ch.id,
        label: ch.catalogTitle || ch.name || '章节',
        children: (ch.children || []).map(sec => ({
          value: sec.id,
          label: sec.catalogTitle || sec.name || '小节'
        }))
      }));
    }
  } catch (e) {
    console.error('Failed to load sections:', e);
  }
};

const openAskModal = () => {
  const token = sessionStorage.getItem('token');
  if (!token) {
    ElMessage.warning('请先登录后再发起提问');
    router.push('/login?redirect=/ask/index');
    return;
  }
  askModalVisible.value = true;
  if (courseOptions.value.length === 0) {
    loadCourses();
  }
};

const submitQuestion = async () => {
  if (!askFormRef.value) return;
  await askFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        let secId = '';
        if (selectedSection.value && selectedSection.value.length) {
          secId = String(selectedSection.value[selectedSection.value.length - 1]);
        }
        const payload = {
          courseId: askForm.courseId,
          sectionId: secId,
          title: askForm.title.trim(),
          description: askForm.description.trim(),
          anonymity: askForm.anonymity ? 1 : 0
        };
        const res = await postQuestions(payload);
        if (res && res.code === 200) {
          ElMessage.success('问题发布成功！讲师与学伴将尽快为您解答');
          askModalVisible.value = false;
          askForm.title = '';
          askForm.description = '';
          pageNo.value = 1;
          loadQuestions();
        } else {
          ElMessage.error(res?.msg || '发布失败，请稍后重试');
        }
      } catch (e) {
        ElMessage.error(e.message || '发布失败');
      } finally {
        submitting.value = false;
      }
    }
  });
};

onMounted(() => {
  loadQuestions();
  // 若带特定课程 ID 进入，自动展开提问弹窗
  if (route.query.id || route.query.courseId) {
    const cId = route.query.id || route.query.courseId;
    askForm.courseId = Number(cId);
    openAskModal();
    handleCourseChange(cId);
  }
});
</script>

<style lang="scss" scoped>
.ask-page {
  background: var(--sky);
  min-height: 100vh;
  font-family: var(--cn);
  padding-bottom: 64px;
}

.home-wrap {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
}

/* 1. Hero Banner */
.ask-hero {
  position: relative;
  overflow: hidden;
  padding: 48px 0 36px;
  background: radial-gradient(60% 80% at 50% 0%, rgba(33, 198, 232, 0.14), transparent 60%),
              radial-gradient(50% 70% at 5% 100%, rgba(43, 134, 240, 0.12), transparent 60%),
              linear-gradient(135deg, #eaf4ff, #d6eaff 55%, #c8e0ff);
  border-bottom: 1px solid var(--line);

  .hero-glow {
    position: absolute;
    width: 600px;
    height: 300px;
    background: radial-gradient(ellipse, rgba(56, 182, 255, 0.3), transparent 65%);
    top: 50%; left: 50%;
    transform: translate(-50%, -50%);
    pointer-events: none;
  }
}

.hero-content {
  position: relative;
  z-index: 2;

  .hero-title {
    font-size: clamp(28px, 3.6vw, 42px);
    font-weight: 800;
    color: var(--ink);
    margin: 10px 0 8px;

    .accent {
      background: var(--grad);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }
  }

  .hero-desc {
    font-size: 15px;
    color: var(--slate);
    max-width: 640px;
    margin-bottom: 24px;
  }
}

.hero-action-bar {
  display: flex;
  gap: 16px;
  max-width: 720px;

  .search-input-box {
    flex: 1;
    :deep(.el-input__wrapper) {
      border-radius: 12px;
      background: #fff;
      box-shadow: 0 4px 16px rgba(30, 137, 241, 0.08);
      border: 1px solid var(--line);
    }
  }

  .iaic-btn-primary {
    padding: 0 28px;
    height: 40px;
    border-radius: 20px;
    background: linear-gradient(90deg, #38b6ff, #2a8fff);
    color: #fff;
    font-size: 14.5px;
    font-weight: 700;
    border: none;
    cursor: pointer;
    box-shadow: 0 4px 14px rgba(42, 143, 255, 0.35);
    transition: all 0.2s;
    white-space: nowrap;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 18px rgba(42, 143, 255, 0.5);
    }
  }
}

/* 2. Main Layout */
.ask-main-wrap {
  margin-top: 32px;
}

.ask-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 28px;
}

.filter-tab-strip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: var(--card);
  padding: 10px 18px;
  border-radius: 12px;
  border: 1px solid var(--line);
}

.tab-pills {
  display: flex;
  gap: 8px;

  .filter-pill {
    background: transparent;
    border: none;
    font-size: 13.5px;
    font-weight: 600;
    color: var(--slate);
    padding: 6px 14px;
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover { color: var(--azure); }
    &.active {
      background: var(--azure);
      color: #fff;
      box-shadow: 0 2px 8px rgba(30, 137, 241, 0.3);
    }
  }
}

.total-badge {
  font-family: var(--mono);
  font-size: 12.5px;
  color: var(--slate-2);
}

/* Question Cards Stream */
.questions-stream {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid var(--line);

  .empty-icon { font-size: 44px; margin-bottom: 12px; }
  p { color: var(--slate); font-size: 14px; margin-bottom: 16px; }
}

.question-card {
  position: relative;
  background: var(--card);
  border-radius: 16px;
  border: 1px solid var(--line);
  padding: 24px 28px;
  box-shadow: 0 1px 3px rgba(19, 41, 79, 0.04);
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.22s, border-color 0.22s, box-shadow 0.22s;

  &:hover {
    transform: translateY(-3px);
    border-color: rgba(43, 134, 240, 0.4);
    box-shadow: var(--shadow);
    .corner { opacity: 0.7; }
    .q-title { color: var(--azure); }
  }

  .corner {
    position: absolute;
    width: 12px;
    height: 12px;
    border: 2px solid var(--cyan);
    opacity: 0;
    transition: opacity 0.25s;

    &.c1 { top: 10px; left: 10px; border-right: none; border-bottom: none; }
    &.c2 { bottom: 10px; right: 10px; border-left: none; border-top: none; }
  }
}

.q-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .author-info {
    display: flex;
    align-items: center;
    gap: 10px;

    .author-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: var(--grad);
      color: #fff;
      font-size: 12px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;

      img { width: 100%; height: 100%; object-fit: cover; }
    }

    .author-name {
      font-size: 13px;
      font-weight: 600;
      color: var(--navy);
    }

    .post-time {
      font-size: 12px;
      color: var(--slate-2);
      font-family: var(--mono);
    }
  }

  .category-pill {
    font-size: 11px;
    color: var(--azure);
    background: var(--sky-2);
    padding: 3px 8px;
    border-radius: 6px;
    font-weight: 600;
  }
}

.q-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--ink);
  line-height: 1.35;
  margin-bottom: 8px;
  transition: color 0.2s;
}

.q-desc {
  font-size: 13.5px;
  color: var(--slate);
  line-height: 1.6;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.latest-reply-box {
  background: var(--sky);
  border: 1px dashed var(--line-2);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12.5px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;

  .reply-badge {
    background: var(--green-bg);
    color: var(--green);
    padding: 2px 6px;
    border-radius: 4px;
    font-weight: 700;
    font-size: 10.5px;
    flex-shrink: 0;
  }

  .reply-user {
    font-weight: 600;
    color: var(--navy);
    flex-shrink: 0;
  }

  .reply-text {
    color: var(--slate);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.q-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--line);

  .stats-group {
    display: flex;
    gap: 18px;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 12px;
      color: var(--slate);
    }
  }

  .view-detail-link {
    font-size: 13px;
    font-weight: 600;
    color: var(--azure);
  }
}

/* 3. Sidebar */
.ask-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .side-card {
    background: var(--card);
    border-radius: 16px;
    border: 1px solid var(--line);
    padding: 22px;
    box-shadow: 0 1px 3px rgba(19, 41, 79, 0.04);

    h4 {
      font-size: 15px;
      font-weight: 800;
      color: var(--ink);
      margin-bottom: 14px;
    }
  }

  .side-tips-list {
    margin: 0; padding-left: 18px;
    font-size: 13px; color: var(--slate);
    line-height: 1.8;
  }

  .pioneer-list {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .pioneer-item {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 13px;

      .rank {
        width: 22px;
        height: 22px;
        border-radius: 6px;
        font-family: var(--mono);
        font-size: 11px;
        font-weight: 800;
        display: flex;
        align-items: center;
        justify-content: center;

        &.r1 { background: #fef3c7; color: #d97706; }
        &.r2 { background: #f1f5f9; color: #475569; }
        &.r3 { background: #ffedd5; color: #c2410c; }
      }

      .p-name { flex: 1; color: var(--navy); font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      .p-score { font-family: var(--mono); color: var(--azure); font-size: 12px; font-weight: 600; }
    }
  }
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* Modal Form */
.full-select {
  width: 100%;
}

.form-footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
}

@media (max-width: 900px) {
  .ask-layout {
    grid-template-columns: 1fr;
  }
  .ask-sidebar {
    display: none;
  }
}
</style>
