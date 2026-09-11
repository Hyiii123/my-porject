<template>
  <div class="my-resume-container">
    <!-- 头部卡片 -->
    <div class="resume-header">
      <div class="header-left">
        <div class="header-tag">
          <el-tag type="danger" effect="dark" round>Zhiwen Resume AI Pro</el-tag>
          <span class="sub-text">基于千亿大模型与 10,000+ 大厂真题库精准对标</span>
        </div>
        <h2 class="title">📄 我的简历与 AI 深度诊断</h2>
        <p class="desc">
          支持简历多格式导入、31 个大厂岗位对标、核心技能萃取、项目亮点挖掘与 STAR 黄金法则重塑，一键直通定制化 AI 模拟面试。
        </p>
      </div>
      <div class="header-status" v-if="resumeData.id">
        <div class="status-badge" :class="getScoreClass(resumeData.matchScore)">
          <div class="score-num">{{ resumeData.matchScore || '--' }}<span class="score-unit">分</span></div>
          <div class="score-level">{{ resumeData.matchLevel || '待完成诊断' }}</div>
        </div>
      </div>
    </div>

    <!-- 简历录入与操作卡片 -->
    <el-card class="resume-input-card" shadow="hover">
      <template #header>
        <div class="card-header-bar">
          <div class="header-title">
            <span>✍️ 简历内容录入与管理</span>
            <span class="file-name-hint" v-if="resumeData.fileName">
              当前文件：<el-tag size="small" type="info">{{ resumeData.fileName }}</el-tag>
            </span>
          </div>
          <div class="header-actions">
            <el-button size="small" type="primary" plain @click="fillSampleResume">
              📋 填入大厂简历范例
            </el-button>
            <el-button size="small" type="danger" plain :disabled="!resumeContent" @click="handleClearResume">
              🗑️ 清空内容
            </el-button>
          </div>
        </div>
      </template>

      <!-- 上传区与文本区 -->
      <div class="input-grid">
        <div class="upload-zone">
          <el-upload
            class="resume-uploader"
            drag
            action=""
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileChange"
            accept=".pdf,.docx,.doc,.txt,.md"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将简历文件拖到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .pdf, .docx, .txt, .md 格式（自动提取纯文本与排版）
              </div>
            </template>
          </el-upload>
        </div>

        <div class="editor-zone">
          <el-input
            v-model="resumeContent"
            type="textarea"
            :rows="11"
            placeholder="也可直接在此处粘贴或编辑您的个人简历文本（包含教育背景、专业技能、核心项目经历与量化成果）..."
            show-word-limit
            maxlength="15000"
          />
        </div>
      </div>

      <!-- 对标配置条 -->
      <div class="benchmark-bar">
        <div class="bar-item job-select">
          <span class="label">🎯 对标目标岗位：</span>
          <el-select
            v-model="targetJob"
            filterable
            placeholder="请选择或搜索对标岗位"
            class="job-dropdown"
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
        </div>

        <div class="bar-item company-select">
          <span class="label">🏢 目标大厂：</span>
          <el-radio-group v-model="targetCompany" size="default">
            <el-radio-button label="阿里巴巴">阿里巴巴</el-radio-button>
            <el-radio-button label="字节跳动">字节跳动</el-radio-button>
            <el-radio-button label="腾讯科技">腾讯科技</el-radio-button>
            <el-radio-button label="美团点评">美团点评</el-radio-button>
            <el-radio-button label="大厂通用">大厂通用</el-radio-button>
          </el-radio-group>
        </div>

        <div class="bar-btns">
          <el-button :loading="saving" @click="handleSaveOnly">
            💾 保存草稿
          </el-button>
          <el-button
            type="primary"
            class="analyze-btn"
            :loading="analyzing"
            :disabled="!resumeContent.trim()"
            @click="handleAnalyze"
          >
            🔍 一键生成大厂对标与诊断报告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- AI 深度诊断结果大屏 -->
    <div v-if="hasReport" class="report-section">
      <!-- 诊断大屏总览卡片 -->
      <el-card class="dashboard-card" shadow="hover">
        <template #header>
          <div class="report-header">
            <div class="report-title">
              <span class="icon">📊</span>
              <span class="text">AI 深度职涯诊断大屏</span>
              <el-tag type="success" size="small" round>大厂招聘委员会评估标准</el-tag>
            </div>
            <div class="report-meta">
              <span>评估岗位：<b>{{ resumeData.targetJob }}</b></span>
              <span>目标企业：<b>{{ resumeData.targetCompany }}</b></span>
              <span class="time">评估时间：{{ formatTime(resumeData.updateTime) }}</span>
            </div>
          </div>
        </template>

        <!-- 关键指标网格 -->
        <div class="metric-grid">
          <div class="metric-card score-metric">
            <div class="metric-header">综合匹配度评分</div>
            <div class="score-display">
              <span class="num">{{ resumeData.matchScore }}</span>
              <span class="unit">/ 100</span>
            </div>
            <div class="score-badge-inline">{{ resumeData.matchLevel }}</div>
            <el-progress
              :percentage="resumeData.matchScore || 0"
              :color="getProgressColor(resumeData.matchScore)"
              :show-text="false"
              :stroke-width="8"
            />
            <div class="base-score-tip">基准及格分 60 分 + 五维工程能力实战加成</div>
          </div>

          <div class="metric-card tags-metric">
            <div class="metric-header">🏷️ 萃取核心技术栈技能标签</div>
            <div class="tags-cloud">
              <el-tag
                v-for="tag in resumeData.techTags"
                :key="tag"
                class="tech-tag"
                effect="light"
                type="primary"
                round
              >
                {{ tag }}
              </el-tag>
              <span v-if="!resumeData.techTags || resumeData.techTags.length === 0" class="empty-hint">暂无提取标签</span>
            </div>
          </div>
        </div>

        <!-- 五维工程技术能力量化细则面板 -->
        <div v-if="resumeData.scoreDetails && resumeData.scoreDetails.length > 0" class="breakdown-card">
          <div class="breakdown-title-bar">
            <div class="bt-left">
              <span class="bt-icon">📊</span>
              <span class="bt-main">大厂招聘委员会·五维工程能力量化细则</span>
            </div>
            <span class="bt-badge">起评分：60 分准入门槛 ｜ 满分：100 分</span>
          </div>
          <div class="breakdown-grid">
            <div v-for="item in resumeData.scoreDetails" :key="item.name" class="breakdown-item">
              <div class="bi-header">
                <span class="bi-name">{{ item.name }}</span>
                <span class="bi-score">
                  <span class="score-val" :class="getDimScoreClass(item.score, item.maxScore)">+{{ item.score }}</span>
                  <span class="score-max">/ {{ item.maxScore }}分</span>
                </span>
              </div>
              <el-progress
                :percentage="Math.round((item.score / item.maxScore) * 100)"
                :stroke-width="5"
                :show-text="false"
                :color="getDimProgressColor(item.score, item.maxScore)"
              />
              <div class="bi-desc">{{ item.description }}</div>
            </div>
          </div>
        </div>

        <!-- 诊断维度详情卡片列表 -->
        <div class="dimension-grid">
          <!-- 项目高光亮点 -->
          <div class="dimension-box highlight-box">
            <div class="box-title">
              <span class="box-icon">🌟</span>
              <span>简历核心高光亮点挖掘</span>
            </div>
            <ul class="bullet-list">
              <li v-for="(item, idx) in resumeData.projectHighlights" :key="idx">
                {{ item }}
              </li>
            </ul>
          </div>

          <!-- 薄弱项与风险点 -->
          <div class="dimension-box risk-box">
            <div class="box-title">
              <span class="box-icon">⚠️</span>
              <span>简历潜在薄弱项与面试风险点</span>
            </div>
            <ul class="bullet-list">
              <li v-for="(item, idx) in resumeData.resumeGaps" :key="idx">
                {{ item }}
              </li>
            </ul>
          </div>

          <!-- 大厂预测必考题 -->
          <div class="dimension-box predicted-box">
            <div class="box-title">
              <span class="box-icon">🎯</span>
              <span>大厂面试官预测深挖题（基于您的真实项目）</span>
            </div>
            <div class="question-list">
              <div v-for="(q, idx) in resumeData.predictedQuestions" :key="idx" class="q-item">
                <span class="q-badge">预测 Q{{ idx + 1 }}</span>
                <span class="q-text">{{ q }}</span>
              </div>
            </div>
          </div>

          <!-- STAR 黄金法则重塑建议 -->
          <div class="dimension-box star-box">
            <div class="box-title">
              <span class="box-icon">💡</span>
              <span>STAR 黄金法则履历重塑建议</span>
            </div>
            <div class="star-content">
              {{ resumeData.starAdvice }}
            </div>
          </div>
        </div>

        <!-- 联通模拟面试直达 Banner -->
        <div class="interview-link-banner">
          <div class="banner-left">
            <div class="b-title">🚀 简历已深度就绪，立即开启定制化 AI 模拟面试考场</div>
            <div class="b-desc">AI 面试官将精准结合本简历中的真实项目与技术栈进行剥洋葱连环深挖，检验实战答题实力！</div>
          </div>
          <el-button type="warning" size="large" class="link-interview-btn" @click="handleGoInterview">
            🎯 携带此简历进入 AI 模拟面试 ➔
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getMyResume, saveResume, analyzeResume, uploadResumeFile, clearResume } from '@/api/interview.js'

const router = useRouter()

const saving = ref(false)
const analyzing = ref(false)
const resumeContent = ref('')
const targetJob = ref('Java 高级开发工程师')
const targetCompany = ref('阿里巴巴')

const resumeData = reactive({
  id: null,
  fileName: '',
  rawContent: '',
  targetJob: '',
  targetCompany: '',
  matchScore: 0,
  matchLevel: '',
  techTags: [],
  projectHighlights: [],
  resumeGaps: [],
  predictedQuestions: [],
  starAdvice: '',
  scoreDetails: [],
  updateTime: ''
})

const hasReport = computed(() => {
  return resumeData.matchScore > 0 || (resumeData.techTags && resumeData.techTags.length > 0)
})

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

const loadMyResume = async () => {
  try {
    const res = await getMyResume()
    if (res && res.code === 200 && res.data) {
      applyResumeData(res.data)
    }
  } catch (err) {
    console.warn('获取个人简历失败', err)
  }
}

const applyResumeData = (data) => {
  Object.assign(resumeData, data)
  if (data.rawContent) {
    if (data.rawContent.startsWith('%PDF-') || data.rawContent.startsWith('PK\x03\x04')) {
      resumeContent.value = ''
    } else {
      resumeContent.value = data.rawContent
    }
  }
  if (data.targetJob) {
    targetJob.value = data.targetJob
  }
  if (data.targetCompany) {
    targetCompany.value = data.targetCompany
  }
}

const handleFileChange = async (file) => {
  if (!file || !file.raw) return
  const rawFile = file.raw
  const isLt10M = rawFile.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('简历文件大小不能超过 10MB')
    return
  }

  resumeData.fileName = rawFile.name
  // 文本类直接前端读取
  if (rawFile.name.endsWith('.txt') || rawFile.name.endsWith('.md')) {
    const reader = new FileReader()
    reader.onload = (e) => {
      resumeContent.value = e.target.result || ''
      ElMessage.success('文本简历解析成功！请检查并点击下方按钮进行AI深度诊断')
    }
    reader.readAsText(rawFile)
  } else {
    // 调用后端文件文本提取（支持 PDF、Word DOCX）
    const formData = new FormData()
    formData.append('file', rawFile)
    try {
      const res = await uploadResumeFile(formData)
      if (res && res.code === 200 && res.data) {
        const text = res.data.content || ''
        if (text.startsWith('%PDF-') || text.startsWith('PK\x03\x04') || /[\x00-\x08\x0E-\x1F]/.test(text.slice(0, 200))) {
          ElMessage.error('上传文件包含未解析的二进制字节码，请重新上传标准 PDF/Word 文档或直接粘贴文字')
          return
        }
        resumeContent.value = text
        resumeData.fileName = res.data.fileName || rawFile.name
        ElMessage.success('简历文档文字提取成功！可核对或补充右侧内容后，点击进行 AI 深度对标')
      } else {
        ElMessage.warning(res ? res.msg : '简历文件已接收，请在右侧文本框补充核心经历')
      }
    } catch (e) {
      ElMessage.warning((e && e.message) ? e.message : '文件解析受限，请在右侧直接粘贴简历文本')
    }
  }
}

const handleSaveOnly = async () => {
  if (!resumeContent.value.trim()) {
    ElMessage.warning('请输入或上传简历内容后再保存')
    return
  }
  try {
    saving.value = true
    const res = await saveResume({
      fileName: resumeData.fileName,
      rawContent: resumeContent.value.trim(),
      targetJob: targetJob.value,
      targetCompany: targetCompany.value
    })
    if (res && res.code === 200) {
      applyResumeData(res.data)
      ElMessage.success('简历内容保存成功！')
    }
  } catch (err) {
    ElMessage.error('保存简历失败：' + (err.message || '网络异常'))
  } finally {
    saving.value = false
  }
}

const handleAnalyze = async () => {
  if (!resumeContent.value.trim()) {
    ElMessage.warning('请先输入或上传简历内容')
    return
  }
  try {
    analyzing.value = true
    const res = await analyzeResume({
      targetJob: targetJob.value,
      companyTarget: targetCompany.value,
      resumeContent: resumeContent.value.trim(),
      fileName: resumeData.fileName
    })
    if (res && res.code === 200 && res.data) {
      applyResumeData(res.data)
      ElMessage.success('AI 深度诊断与大厂对标已完成！')
    } else {
      ElMessage.error(res.msg || 'AI 诊断失败，请重试')
    }
  } catch (err) {
    ElMessage.error('AI 诊断异常：' + (err.message || '网络超时'))
  } finally {
    analyzing.value = false
  }
}

const handleClearResume = async () => {
  try {
    await ElMessageBox.confirm('确定要清空当前的个人简历与诊断报告吗？清空后不可恢复。', '提示', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await clearResume()
    resumeContent.value = ''
    Object.assign(resumeData, {
      id: null,
      fileName: '',
      rawContent: '',
      matchScore: 0,
      matchLevel: '',
      techTags: [],
      projectHighlights: [],
      resumeGaps: [],
      predictedQuestions: [],
      starAdvice: '',
      updateTime: ''
    })
    ElMessage.success('简历数据已清空')
  } catch (e) {}
}

const fillSampleResume = () => {
  resumeData.fileName = '李同学_Java高级架构_5年.md'
  targetJob.value = 'Java 高级开发工程师'
  resumeContent.value = `# 李华 - Java高级开发工程师 / 架构师
- **工作经验**：5年互联网大厂分布式微服务研发背景
- **核心技能栈**：Java 17 / Spring Boot 3 / Spring Cloud Alibaba / Netty / MySQL / Redis / Kafka / RocketMQ / Docker / Kubernetes / Qdrant 向量引擎

## 核心项目经历
### 1. 亿级电商分布式秒杀与订单履约系统重构 (核心负责人)
- **业务痛点与指标**：原有单体架构在双十一峰值遭遇 10 万 QPS 瞬间打垮 MySQL 连接池，主从延迟超 30 秒，超卖率达 0.4%。
- **核心架构改造 (Action)**：
  - 引入 Redis + Lua 脚本实现分布式原子库存扣减与令牌桶限流，前置拦截 92% 非法请求；
  - 针对热点商品采用本地 Caffeine + 分布式 Redis 两级缓存架构，并基于 RocketMQ 事务消息实现订单最终一致性异步履约；
  - 针对分库分表（ShardingSphere 16库128表）跨分片慢查询，通过 ElasticSearch 与 CANAL 增量宽表索引方案，将深分页查询 RT 由 2.5s 压降至 35ms。
- **业务结果 (Result)**：在大促期间顶住 12.8 万 QPS 突发流量，零超卖，核心交易链路可用性达到 99.995%。

### 2. 智问分布式 AI 知识引擎与智能网关中台
- **架构设计**：采用 Netty + SSE (Server-Sent Events) 构建高吞吐低延迟问答流，结合 Qdrant 向量检索与 BM25 混合索引；
- **排障攻坚**：定位并修复了高并发自旋下的线程饥饿与 DirectByteBuffer 内存泄漏故障，压降系统 GC 停顿 60%`
  ElMessage.success('已填入大厂范例简历！您可以点击下方按钮直接启动 AI 诊断')
}

const handleGoInterview = () => {
  router.push({
    path: '/interview',
    query: {
      fromResume: '1',
      job: targetJob.value,
      company: targetCompany.value
    }
  })
}

const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  return 'score-normal'
}

const getProgressColor = (score) => {
  if (score >= 90) return '#67C23A'
  if (score >= 80) return '#409EFF'
  return '#E6A23C'
}

const getDimScoreClass = (score, maxScore) => {
  if (score >= maxScore) return 'score-perfect'
  if (score >= maxScore * 0.7) return 'score-good'
  return 'score-normal'
}

const getDimProgressColor = (score, maxScore) => {
  const ratio = score / maxScore
  if (ratio >= 0.9) return '#10B981'
  if (ratio >= 0.7) return '#2563EB'
  if (ratio >= 0.5) return '#F59E0B'
  return '#94A3B8'
}

const formatTime = (timeStr) => {
  if (!timeStr) return '--'
  return timeStr.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  loadMyResume()
})
</script>

<style lang="scss" scoped>
.my-resume-container {
  padding: 0 0 30px;

  .resume-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
    border-radius: 12px;
    padding: 24px 30px;
    color: #fff;
    margin-bottom: 20px;
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);

    .header-tag {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;

      .sub-text {
        font-size: 13px;
        color: #94a3b8;
      }
    }

    .title {
      font-size: 24px;
      font-weight: 700;
      margin: 0 0 8px;
    }

    .desc {
      font-size: 13px;
      color: #cbd5e1;
      line-height: 1.6;
      margin: 0;
      max-width: 680px;
    }

    .header-status {
      .status-badge {
        text-align: center;
        padding: 12px 24px;
        border-radius: 12px;
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.15);

        .score-num {
          font-size: 32px;
          font-weight: 800;
          color: #38bdf8;
          line-height: 1.1;

          .score-unit {
            font-size: 14px;
            font-weight: normal;
            color: #94a3b8;
            margin-left: 2px;
          }
        }

        .score-level {
          font-size: 12px;
          color: #e2e8f0;
          margin-top: 4px;
        }

        &.score-excellent .score-num {
          color: #4ade80;
        }
      }
    }
  }

  .resume-input-card {
    border-radius: 12px;
    margin-bottom: 24px;

    .card-header-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-title {
        font-size: 16px;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .header-actions {
        display: flex;
        gap: 10px;
      }
    }

    .input-grid {
      display: grid;
      grid-template-columns: 280px 1fr;
      gap: 20px;
      margin-bottom: 20px;

      .upload-zone {
        :deep(.el-upload-dragger) {
          height: 100%;
          min-height: 250px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          border-radius: 8px;
          background: #f8fafc;
          border: 2px dashed #cbd5e1;

          &:hover {
            border-color: #2563eb;
          }
        }
      }

      .editor-zone {
        :deep(.el-textarea__inner) {
          font-family: 'Consolas', 'Courier New', monospace;
          font-size: 13px;
          line-height: 1.6;
          border-radius: 8px;
          background: #fafafa;
        }
      }
    }

    .benchmark-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding-top: 16px;
      border-top: 1px solid #f1f5f9;

      .bar-item {
        display: flex;
        align-items: center;
        gap: 8px;

        .label {
          font-size: 14px;
          font-weight: 600;
          color: #334155;
          white-space: nowrap;
        }
      }

      .job-dropdown {
        width: 280px;
      }

      .bar-btns {
        display: flex;
        gap: 12px;

        .analyze-btn {
          font-weight: 600;
          background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
          border: none;
        }
      }
    }
  }

  .report-section {
    .dashboard-card {
      border-radius: 12px;

      .report-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 12px;

        .report-title {
          font-size: 18px;
          font-weight: 700;
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .report-meta {
          font-size: 13px;
          color: #64748b;
          display: flex;
          gap: 16px;

          b {
            color: #1e293b;
          }
        }
      }

      .metric-grid {
        display: grid;
        grid-template-columns: 280px 1fr;
        gap: 20px;
        margin-bottom: 24px;

        .metric-card {
          padding: 20px;
          border-radius: 10px;
          background: #f8fafc;
          border: 1px solid #e2e8f0;

          .metric-header {
            font-size: 14px;
            font-weight: 600;
            color: #475569;
            margin-bottom: 12px;
          }
        }

        .score-metric {
          text-align: center;

          .score-display {
            font-size: 38px;
            font-weight: 800;
            color: #2563eb;
            line-height: 1;

            .unit {
              font-size: 14px;
              color: #94a3b8;
              font-weight: normal;
            }
          }

          .score-badge-inline {
            font-size: 13px;
            font-weight: 600;
            color: #059669;
            margin: 10px 0 6px;
          }

          .base-score-tip {
            font-size: 11px;
            color: #64748b;
            margin-top: 4px;
            font-weight: 500;
          }
        }

        .tags-metric {
          .tags-cloud {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;

            .tech-tag {
              font-size: 13px;
              padding: 4px 12px;
            }

            .empty-hint {
              color: #94a3b8;
              font-size: 13px;
            }
          }
        }
      }

      .breakdown-card {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 18px 20px;
        margin-bottom: 24px;

        .breakdown-title-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 14px;
          flex-wrap: wrap;
          gap: 8px;

          .bt-left {
            display: flex;
            align-items: center;
            gap: 8px;

            .bt-icon {
              font-size: 16px;
            }

            .bt-main {
              font-size: 15px;
              font-weight: 700;
              color: #1e293b;
            }
          }

          .bt-badge {
            font-size: 12px;
            color: #2563eb;
            background: #eff6ff;
            border: 1px solid #dbeafe;
            padding: 3px 10px;
            border-radius: 12px;
            font-weight: 600;
          }
        }

        .breakdown-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
          gap: 14px;

          .breakdown-item {
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 12px 14px;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);

            .bi-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 6px;

              .bi-name {
                font-size: 13px;
                font-weight: 600;
                color: #334155;
              }

              .bi-score {
                font-size: 12px;

                .score-val {
                  font-size: 14px;
                  font-weight: 700;

                  &.score-perfect {
                    color: #10b981;
                  }
                  &.score-good {
                    color: #2563eb;
                  }
                  &.score-normal {
                    color: #d97706;
                  }
                }

                .score-max {
                  color: #94a3b8;
                  margin-left: 2px;
                }
              }
            }

            :deep(.el-progress) {
              margin-bottom: 8px;
            }

            .bi-desc {
              font-size: 11px;
              color: #64748b;
              line-height: 1.5;
            }
          }
        }
      }

      .dimension-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 20px;
        margin-bottom: 24px;

        .dimension-box {
          padding: 20px;
          border-radius: 10px;
          background: #fff;
          border: 1px solid #e2e8f0;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

          .box-title {
            font-size: 15px;
            font-weight: 700;
            margin-bottom: 14px;
            display: flex;
            align-items: center;
            gap: 8px;
          }

          .bullet-list {
            padding-left: 18px;
            margin: 0;

            li {
              font-size: 13px;
              line-height: 1.8;
              color: #334155;
              margin-bottom: 8px;
            }
          }
        }

        .highlight-box {
          border-left: 4px solid #10b981;
          .box-title { color: #065f46; }
        }

        .risk-box {
          border-left: 4px solid #f59e0b;
          .box-title { color: #92400e; }
        }

        .predicted-box {
          border-left: 4px solid #6366f1;
          .box-title { color: #3730a3; }

          .question-list {
            display: flex;
            flex-direction: column;
            gap: 10px;

            .q-item {
              display: flex;
              align-items: flex-start;
              gap: 8px;
              background: #f8fafc;
              padding: 10px 12px;
              border-radius: 6px;

              .q-badge {
                flex-shrink: 0;
                font-size: 11px;
                font-weight: 700;
                background: #e0e7ff;
                color: #4338ca;
                padding: 2px 6px;
                border-radius: 4px;
              }

              .q-text {
                font-size: 13px;
                color: #1e293b;
                line-height: 1.5;
              }
            }
          }
        }

        .star-box {
          border-left: 4px solid #3b82f6;
          .box-title { color: #1e40af; }

          .star-content {
            font-size: 13px;
            line-height: 1.8;
            color: #334155;
            background: #eff6ff;
            padding: 12px 16px;
            border-radius: 6px;
          }
        }
      }

      .interview-link-banner {
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        border: 1px solid #bfdbfe;
        border-radius: 10px;
        padding: 20px 26px;

        .banner-left {
          .b-title {
            font-size: 16px;
            font-weight: 700;
            color: #1e3a8a;
            margin-bottom: 4px;
          }

          .b-desc {
            font-size: 13px;
            color: #3b82f6;
          }
        }

        .link-interview-btn {
          font-weight: 700;
          box-shadow: 0 4px 14px rgba(245, 158, 11, 0.35);
        }
      }
    }
  }
}

@media (max-width: 900px) {
  .my-resume-container {
    .resume-input-card .input-grid {
      grid-template-columns: 1fr;
    }
    .report-section .dashboard-card .metric-grid,
    .report-section .dashboard-card .dimension-grid {
      grid-template-columns: 1fr;
    }
    .report-section .dashboard-card .interview-link-banner {
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
    }
  }
}
</style>
