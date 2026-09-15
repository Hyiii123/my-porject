<template>
  <main class="customer-service-page">
    <div class="service-container">
      <section class="service-hero">
        <div>
          <div class="hero-eyebrow"><el-icon><Service /></el-icon> 专属学习服务</div>
          <h1>客服中心</h1>
          <p>遇到课程、订单或账号问题？先问问小智，服务结束后还可以留下评价。</p>
        </div>
      </section>

      <section class="service-layout" :class="{ 'history-collapsed': isHistorySidebarCollapsed }">
        <!-- 1. 客服左侧：长方形框架可折叠菜单栏 -->
        <aside class="history-sidebar" :class="{ collapsed: isHistorySidebarCollapsed }">
          <!-- 展开状态 -->
          <div v-if="!isHistorySidebarCollapsed" class="history-sidebar-inner">
            <div class="history-header">
              <div class="history-title">
                <el-icon><ChatLineRound /></el-icon>
                <span>会话列表</span>
              </div>
              <el-button
                circle
                size="small"
                class="collapse-toggle-btn"
                title="收起菜单栏"
                @click="toggleHistorySidebar"
              >
                <el-icon><Fold /></el-icon>
              </el-button>
            </div>

            <el-button
              type="primary"
              class="new-chat-btn"
              round
              @click="handleCreateNewSession"
            >
              <el-icon><Plus /></el-icon>
              <span>新建对话</span>
            </el-button>

            <div class="session-tabs">
              <button
                type="button"
                :class="{ active: sessionTab === 'active' }"
                @click="sessionTab = 'active'"
              >
                活跃会话 ({{ activeSessions.length }})
              </button>
              <button
                type="button"
                :class="{ active: sessionTab === 'archived' }"
                @click="sessionTab = 'archived'"
              >
                已归档 ({{ archivedSessions.length }})
              </button>
            </div>

            <div class="session-list-wrap" v-loading="loadingSessions">
              <div v-if="currentTabSessions.length === 0" class="empty-sessions">
                <el-empty :image-size="40" :description="sessionTab === 'active' ? '暂无活跃会话' : '暂无归档会话'" />
              </div>
              <div
                v-for="item in currentTabSessions"
                :key="item.id"
                class="session-item-card"
                :class="{ active: String(item.id) === String(sessionId) }"
                @click="handleSwitchSession(item)"
              >
                <div class="session-card-content">
                  <div class="session-card-title">
                    <span class="session-indicator" />
                    <span class="title-text">{{ getSessionTitle(item) }}</span>
                  </div>
                  <div class="session-card-meta">
                    <span class="session-time">{{ formatSessionDate(item.updatedAt || item.createTime) }}</span>
                  </div>
                </div>

                <div class="session-item-actions" @click.stop>
                  <!-- 归档 / 取消归档按钮 -->
                  <el-button
                    link
                    size="small"
                    class="action-btn archive-btn"
                    :title="item.status === 4 ? '恢复至活跃会话' : '归档对话'"
                    @click.stop="handleArchiveSession(item, item.status !== 4)"
                  >
                    <el-icon v-if="item.status === 4"><FolderOpened /></el-icon>
                    <el-icon v-else><Folder /></el-icon>
                  </el-button>

                  <!-- 删除按钮 (带确认防误删) -->
                  <el-popconfirm
                    title="确定删除此对话记录吗？"
                    confirm-button-text="删除"
                    cancel-button-text="取消"
                    confirm-button-type="danger"
                    @confirm="handleDeleteSession(item)"
                  >
                    <template #reference>
                      <el-button
                        link
                        size="small"
                        class="action-btn delete-btn"
                        title="删除对话"
                        @click.stop
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </div>

          <!-- 折叠状态 (极简立柱) -->
          <div v-else class="history-sidebar-collapsed-inner">
            <el-button
              circle
              size="small"
              class="expand-toggle-btn"
              title="展开菜单栏"
              @click="toggleHistorySidebar"
            >
              <el-icon><Expand /></el-icon>
            </el-button>

            <el-tooltip content="新建对话" placement="right">
              <el-button
                type="primary"
                circle
                class="mini-new-btn"
                @click="handleCreateNewSession"
              >
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>

            <div class="mini-session-icons">
              <el-tooltip
                v-for="item in activeSessions.slice(0, 6)"
                :key="item.id"
                :content="getSessionTitle(item)"
                placement="right"
              >
                <div
                  class="mini-session-dot"
                  :class="{ active: String(item.id) === String(sessionId) }"
                  @click="handleSwitchSession(item)"
                >
                  <el-icon><ChatLineRound /></el-icon>
                </div>
              </el-tooltip>
            </div>
          </div>
        </aside>

        <!-- 2. 中间：主聊天面板 -->
        <section class="chat-panel">
          <div class="chat-panel-header">
            <div class="chat-title">
              <el-button
                v-if="isHistorySidebarCollapsed"
                circle
                size="small"
                class="header-expand-btn"
                title="展开会话菜单栏"
                @click="toggleHistorySidebar"
              >
                <el-icon><Expand /></el-icon>
              </el-button>
              <div class="chat-avatar"><el-icon :size="24"><ChatDotRound /></el-icon></div>
              <div>
                <h2>在线咨询</h2>
                <span><i class="mini-status-dot" /> {{ statusText }}</span>
              </div>
            </div>
            <el-button class="new-session-button" text @click="handleCreateNewSession">
              <el-icon><Refresh /></el-icon>
              新会话
            </el-button>
          </div>

          <div ref="messagesRef" class="chat-messages">
            <div class="conversation-date">今天</div>
            <div
              v-for="message in messages"
              :key="message.id"
              :class="['message', message.type === 'user' ? 'user-message' : 'service-message']"
            >
              <div v-if="message.type !== 'user'" class="message-avatar service-avatar">
                <el-icon :size="17"><Service /></el-icon>
              </div>
              <div class="message-content">
                <div class="message-meta">
                  <span>{{ message.type === 'user' ? '我' : (message.senderName || 'AI客服') }}</span>
                  <time>{{ formatMessageTime(message.time) }}</time>
                  <span v-if="message.costSeconds" class="meta-cost-badge">
                    ⚡ 耗时 {{ message.costSeconds }}s
                  </span>
                </div>
                <div class="message-bubble">
                  <div class="bubble-text">{{ cleanMessageContent(message.content) }}</div>
                  <div
                    v-if="hasActionPath(message.content)"
                    class="agent-action-card"
                    @click="goToAgentPath(extractActionRole(message.content))"
                  >
                    <div class="card-left">
                      <div class="card-badge">🎯 多智能体推演就绪</div>
                      <div class="card-title">前往首页查看【{{ extractActionRole(message.content) || '目标岗位' }}】4 阶段进阶拓扑图谱</div>
                      <div class="card-desc">已为您在首页 AI 协同推演仪表盘 (HUD) 部署全屏 DAG 拓扑、知识大纲拆解与审判质检报告</div>
                    </div>
                    <div class="card-right">
                      <span class="btn-text">前往大屏</span>
                      <el-icon><ArrowRight /></el-icon>
                    </div>
                  </div>

                  <!-- 智能体全端富交互操作卡片 (Agent Action Card) -->
                  <div
                    v-if="hasAgentActionCard(message.content)"
                    class="agent-interactive-container"
                  >
                    <!-- 1. 课程推荐与一键购课/加购卡片 -->
                    <div
                      v-if="extractAgentActionCard(message.content)?.action === 'course_purchase'"
                      class="agent-interactive-card course-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge course-badge">🛒 智能推荐 · 专属购课通道</span>
                        <span class="sub-badge">{{ extractAgentActionCard(message.content)?.lessons || 0 }} 课时</span>
                      </div>
                      <div class="card-body">
                        <div class="course-thumb">
                          <img
                            :src="extractAgentActionCard(message.content)?.cover || defaultCover"
                            :alt="extractAgentActionCard(message.content)?.title"
                            @error="handleImgError($event)"
                          />
                        </div>
                        <div class="course-meta">
                          <div class="course-title">{{ extractAgentActionCard(message.content)?.title }}</div>
                          <div class="course-teacher">主讲：{{ extractAgentActionCard(message.content)?.teacherName || '金牌讲师团队' }}</div>
                          <div class="course-pricing">
                            <span v-if="extractAgentActionCard(message.content)?.isFree == 1 || Number(extractAgentActionCard(message.content)?.price || 0) === 0" class="free-price">
                              限时免费
                            </span>
                            <template v-else>
                              <span class="curr-price">¥{{ (Number(extractAgentActionCard(message.content)?.price || 0) / 100).toFixed(2) }}</span>
                              <span v-if="Number(extractAgentActionCard(message.content)?.originalPrice || 0) > Number(extractAgentActionCard(message.content)?.price || 0)" class="orig-price">
                                ¥{{ (Number(extractAgentActionCard(message.content)?.originalPrice || 0) / 100).toFixed(2) }}
                              </span>
                            </template>
                          </div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <template v-if="extractAgentActionCard(message.content)?.isFree == 1 || Number(extractAgentActionCard(message.content)?.price || 0) === 0">
                          <el-button
                            type="success"
                            size="small"
                            round
                            :loading="actionLoading[message.id + '_enroll']"
                            :disabled="actionDone[message.id + '_enroll']"
                            @click="handleActionEnrollFree(extractAgentActionCard(message.content), message.id)"
                          >
                            {{ actionDone[message.id + '_enroll'] ? '✅ 已报名成功' : '🎓 立即免费报名' }}
                          </el-button>
                          <el-button
                            v-if="actionDone[message.id + '_enroll']"
                            type="primary"
                            size="small"
                            round
                            @click="goToLearning(extractAgentActionCard(message.content)?.courseId)"
                          >
                            ▶️ 立即开始学习
                          </el-button>
                        </template>
                        <template v-else>
                          <el-button
                            type="warning"
                            size="small"
                            round
                            plain
                            :loading="actionLoading[message.id + '_cart']"
                            :disabled="actionDone[message.id + '_cart']"
                            @click="handleActionAddToCart(extractAgentActionCard(message.content), message.id)"
                          >
                            {{ actionDone[message.id + '_cart'] ? '✅ 已在购物车' : '🛒 加入购物车' }}
                          </el-button>
                          <el-button
                            type="primary"
                            size="small"
                            round
                            @click="handleActionCheckout(extractAgentActionCard(message.content))"
                          >
                            💳 立即结算
                          </el-button>
                        </template>
                      </div>
                    </div>

                    <!-- 2. 全真模拟面试一键开考卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'interview_launch'"
                      class="agent-interactive-card interview-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge interview-badge">🎙️ 全真模拟考场 · 已就绪</span>
                        <span class="voice-badge">
                          <i class="wave-icon" /> 微软晓晓 (Xiaoxiao Neural) 考官
                        </span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box interview-icon">
                          <el-icon :size="26"><Headset /></el-icon>
                        </div>
                        <div class="interview-meta">
                          <div class="job-title">{{ extractAgentActionCard(message.content)?.targetJob || '技术开发工程师' }}</div>
                          <div class="company-tag">面向：{{ extractAgentActionCard(message.content)?.company || '大厂通用' }} ｜ 场次 #{{ extractAgentActionCard(message.content)?.sessionId }}</div>
                          <div class="status-tip">三环节20题60分钟限时架构 · 破题第一问与晓晓自然音色已就绪</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          class="launch-btn"
                          @click="handleActionLaunchInterview(extractAgentActionCard(message.content)?.sessionId)"
                        >
                          🚀 立即进入考场
                        </el-button>
                      </div>
                    </div>

                    <!-- 3. 每日学情打卡签到卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'sign_in'"
                      class="agent-interactive-card signin-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge signin-badge">✨ 每日学情打卡 · 积分奖励</span>
                        <span class="sub-badge">+10 积分 / 天</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box signin-icon">
                          <span class="coin-emoji">🪙</span>
                        </div>
                        <div class="signin-meta">
                          <div class="signin-title">{{ actionDone['today_signed'] ? '🎉 今日已完成打卡！' : '坚持学情打卡，学分兑好礼' }}</div>
                          <div class="signin-desc">每天打卡领取 10 积分，连续签到更有惊喜加成，购课立减抵扣现金</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="warning"
                          size="small"
                          round
                          :loading="actionLoading['signing_in']"
                          :disabled="actionDone['today_signed']"
                          @click="handleActionSignIn"
                        >
                          {{ actionDone['today_signed'] ? '🎉 今日已签到' : '✨ 一键打卡签到 (+10分)' }}
                        </el-button>
                        <el-button
                          type="info"
                          size="small"
                          plain
                          round
                          @click="router.push('/main/coupon')"
                        >
                          🎁 领券中心
                        </el-button>
                      </div>
                    </div>

                    <!-- 4. 简历诊断卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'resume_diagnose'"
                      class="agent-interactive-card resume-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge resume-badge">📄 AI 简历深度诊断</span>
                        <span class="sub-badge score-badge">契合度 {{ extractAgentActionCard(message.content)?.matchScore || 80 }} 分</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box resume-icon">
                          <el-icon :size="26"><Document /></el-icon>
                        </div>
                        <div class="resume-meta">
                          <div class="resume-title">{{ extractAgentActionCard(message.content)?.fileName || '我的简历' }}</div>
                          <div class="resume-desc">对标岗位：{{ extractAgentActionCard(message.content)?.targetJob || '技术研发' }} ｜ 真实项目与高并发深挖</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          @click="router.push('/personal/main/myResume')"
                        >
                          📄 查看完整诊断报告
                        </el-button>
                        <el-button
                          type="success"
                          size="small"
                          plain
                          round
                          @click="router.push('/interview')"
                        >
                          🎯 发起针对性模拟面试
                        </el-button>
                      </div>
                    </div>

                    <!-- 5. 待上传简历卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'resume_upload'"
                      class="agent-interactive-card resume-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge resume-badge">📄 简历诊断 · 待上传</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box resume-icon">
                          <el-icon :size="26"><Document /></el-icon>
                        </div>
                        <div class="resume-meta">
                          <div class="resume-title">尚未关联个人求职简历</div>
                          <div class="resume-desc">上传 PDF/Word 简历，解锁 AI 真实经历提取、契合度量化与对标深挖</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          @click="router.push('/personal/main/myResume')"
                        >
                          📤 前往上传简历
                        </el-button>
                      </div>
                    </div>

                    <!-- 6. 学习进度接力卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'continue_learning'"
                      class="agent-interactive-card learning-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge learning-badge">📚 学伴学习进度接力</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box learning-icon">
                          <el-icon :size="26"><Reading /></el-icon>
                        </div>
                        <div class="learning-meta">
                          <div class="learning-title">我的个人课表与学习轨迹</div>
                          <div class="learning-desc">无缝接力上次播放进度，按部就班夯实架构技术能力</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          @click="router.push('/personal/main/myClass')"
                        >
                          📖 前往我的课表
                        </el-button>
                        <el-button
                          type="info"
                          size="small"
                          plain
                          round
                          @click="router.push('/search/index')"
                        >
                          🔍 发现更多课程
                        </el-button>
                      </div>
                    </div>

                    <!-- 7. 历史模拟面试复盘卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'interview_report'"
                      class="agent-interactive-card interview-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge interview-badge">📊 面试复盘 · 成绩单</span>
                        <span class="sub-badge score-badge">综合得分 {{ extractAgentActionCard(message.content)?.score || 82 }} 分</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box interview-icon">
                          <el-icon :size="26"><Trophy /></el-icon>
                        </div>
                        <div class="interview-meta">
                          <div class="interview-title">【{{ extractAgentActionCard(message.content)?.company || '大厂通用' }}】{{ extractAgentActionCard(message.content)?.targetJob || '技术开发工程师' }}</div>
                          <div class="interview-desc">场次 #{{ extractAgentActionCard(message.content)?.sessionId }} ｜ {{ extractAgentActionCard(message.content)?.status === 2 ? '考核已完成' : '考核进行中' }} ｜ STAR 评价已就绪</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          @click="router.push(`/interview/report/${extractAgentActionCard(message.content)?.sessionId}`)"
                        >
                          📈 查看完整复盘大屏
                        </el-button>
                        <el-button
                          type="danger"
                          size="small"
                          plain
                          round
                          @click="router.push('/interview')"
                        >
                          🎯 进入面试大厅
                        </el-button>
                      </div>
                    </div>

                    <!-- 8. 个人订单管理卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'order_manage'"
                      class="agent-interactive-card order-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge order-badge">📦 订单与交易中枢</span>
                        <span class="sub-badge">数据专属隔离</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box order-icon">
                          <el-icon :size="26"><Wallet /></el-icon>
                        </div>
                        <div class="order-meta">
                          <div class="order-title">我的课程订单与售后管理</div>
                          <div class="order-desc">已严格绑定当前账户，可极速处理待支付订单或发起退款申请</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          @click="router.push('/personal/main/myOrder')"
                        >
                          📦 查看我的全部订单
                        </el-button>
                        <el-button
                          type="warning"
                          size="small"
                          plain
                          round
                          @click="router.push('/pay/carts')"
                        >
                          🛒 前往购物车
                        </el-button>
                      </div>
                    </div>

                    <!-- 9. 优惠券中心卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'coupon_center'"
                      class="agent-interactive-card coupon-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge coupon-badge">🎟️ 优惠券与专属福利</span>
                        <span class="sub-badge">立减大额好券</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box coupon-icon">
                          <el-icon :size="26"><Tickets /></el-icon>
                        </div>
                        <div class="coupon-meta">
                          <div class="coupon-title">平台新人减免与限时折扣券</div>
                          <div class="coupon-desc">领券后自动进入个人卡券包，购课结算时一键抵扣现金</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="danger"
                          size="small"
                          round
                          @click="router.push('/main/coupon')"
                        >
                          🎁 立即前往领券中心
                        </el-button>
                        <el-button
                          type="info"
                          size="small"
                          plain
                          round
                          @click="router.push('/personal/main/myCoupon')"
                        >
                          🎟️ 我的卡券包
                        </el-button>
                      </div>
                    </div>

                    <!-- 10. 购物车资产卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'cart_view'"
                      class="agent-interactive-card cart-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge cart-badge">🛒 我的购物车清单</span>
                        <span class="sub-badge">私密资产保护</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box cart-icon">
                          <el-icon :size="26"><ShoppingCart /></el-icon>
                        </div>
                        <div class="cart-meta">
                          <div class="cart-title">待结算课程与抢购通道</div>
                          <div class="cart-desc">支持勾选合并结算或一键下单报名，支持积分抵扣</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          @click="router.push('/pay/carts')"
                        >
                          🛒 查看我的购物车
                        </el-button>
                        <el-button
                          type="success"
                          size="small"
                          plain
                          round
                          @click="router.push('/pay/settlement')"
                        >
                          💳 前往结算台
                        </el-button>
                      </div>
                    </div>

                    <!-- 11. 考试考核与测验中心卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'exam_query'"
                      class="agent-interactive-card exam-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge exam-badge">📝 学情考试与测验</span>
                        <span class="sub-badge">错题解析</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box exam-icon">
                          <el-icon :size="26"><Document /></el-icon>
                        </div>
                        <div class="exam-meta">
                          <div class="exam-title">期末考核、随堂小测与实战答卷</div>
                          <div class="exam-desc">查阅专属考试成绩单与深度错题解析，夯实理论盲区</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          @click="router.push('/personal/main/myExam')"
                        >
                          📝 我的考试中心
                        </el-button>
                        <el-button
                          type="warning"
                          size="small"
                          plain
                          round
                          @click="router.push('/points/index')"
                        >
                          🏆 学霸天梯榜
                        </el-button>
                      </div>
                    </div>

                    <!-- 12. 随堂笔记与知识库卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'note_quick'"
                      class="agent-interactive-card note-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge note-badge">📒 随堂速记与知识库</span>
                        <span class="sub-badge">个人私密空间</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box note-icon">
                          <el-icon :size="26"><Notebook /></el-icon>
                        </div>
                        <div class="note-meta">
                          <div class="note-title">{{ extractAgentActionCard(message.content)?.noteContent || '个人随堂技术笔记本' }}</div>
                          <div class="note-desc">记录技术要点、高并发攻坚总结与架构心得，支持随时查阅与编辑</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          v-if="extractAgentActionCard(message.content)?.noteContent"
                          type="primary"
                          size="small"
                          round
                          :loading="actionLoading[message.id + '_note']"
                          :disabled="actionDone[message.id + '_note']"
                          @click="handleActionSaveNote(extractAgentActionCard(message.content), message.id)"
                        >
                          {{ actionDone[message.id + '_note'] ? '✅ 已存入笔记本' : '💾 一键存入我的笔记' }}
                        </el-button>
                        <el-button
                          type="info"
                          size="small"
                          plain
                          round
                          @click="router.push('/notes/index')"
                        >
                          📖 打开笔记本大厅
                        </el-button>
                      </div>
                    </div>

                    <!-- 13. 赛季学霸榜与积分资产卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'points_ranking'"
                      class="agent-interactive-card points-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge points-badge">🏆 赛季天梯榜 · 积分资产</span>
                        <span class="sub-badge">每日打卡奖励</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box points-icon">
                          <el-icon :size="26"><Medal /></el-icon>
                        </div>
                        <div class="points-meta">
                          <div class="points-title">学霸天梯竞技榜与个人学分</div>
                          <div class="points-desc">查看赛季积分段位排行，打卡赚取积分并在购课时直接抵现</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="warning"
                          size="small"
                          round
                          @click="router.push('/points/index')"
                        >
                          🏆 查看学霸天梯榜
                        </el-button>
                        <el-button
                          type="primary"
                          size="small"
                          plain
                          round
                          @click="router.push('/personal/main/myIntegral')"
                        >
                          🌟 我的积分明细
                        </el-button>
                      </div>
                    </div>

                    <!-- 14. 学员学习画像与技能雷达卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'learning_portrait'"
                      class="agent-interactive-card portrait-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge portrait-badge">📊 学员画像 · 技能雷达</span>
                        <span class="sub-badge">六维动态推演</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box portrait-icon">
                          <el-icon :size="26"><Reading /></el-icon>
                        </div>
                        <div class="portrait-meta">
                          <div class="portrait-title">多维能力模型与知识图谱</div>
                          <div class="portrait-desc">根据日常学习实训、真题测评与面试评级实时自适应校准</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          @click="router.push('/personal/main/overview')"
                        >
                          📊 查看完整能力画像
                        </el-button>
                        <el-button
                          type="success"
                          size="small"
                          plain
                          round
                          @click="router.push('/')"
                        >
                          🚀 查看多智能体进阶路线
                        </el-button>
                      </div>
                    </div>

                    <!-- 15. 智能页面穿梭卡片 -->
                    <div
                      v-else-if="extractAgentActionCard(message.content)?.action === 'page_navigator'"
                      class="agent-interactive-card nav-action-card"
                    >
                      <div class="card-header">
                        <span class="tag-badge nav-badge">🧭 智能页面穿梭通道</span>
                        <span class="sub-badge">一键直达目标功能</span>
                      </div>
                      <div class="card-body">
                        <div class="card-icon-box nav-icon">
                          <el-icon :size="26"><Compass /></el-icon>
                        </div>
                        <div class="nav-meta">
                          <div class="nav-title">目标页面：{{ extractAgentActionCard(message.content)?.pageTitle || '系统功能' }}</div>
                          <div class="nav-desc">对话中即可无缝穿梭全端任意页面，体验极致高效控制</div>
                        </div>
                      </div>
                      <div class="card-footer">
                        <el-button
                          type="primary"
                          size="small"
                          round
                          @click="router.push(extractAgentActionCard(message.content)?.targetRoute || '/main/index')"
                        >
                          🧭 立即前往【{{ extractAgentActionCard(message.content)?.pageTitle || '功能页' }}】
                        </el-button>
                      </div>
                    </div>
                  </div>

                  <!-- 智能体执行已完成耗时徽章 -->
                  <div v-if="message.costSeconds" class="execution-done-badge">
                    <span class="done-check">✓</span>
                    <span class="done-text">智能体执行已完成</span>
                    <span class="done-sep">·</span>
                    <span class="done-time">耗时 <strong>{{ message.costSeconds }}</strong> 秒</span>
                  </div>
                </div>
              </div>
              <div v-if="message.type === 'user'" class="message-avatar user-avatar">
                <el-icon :size="17"><User /></el-icon>
              </div>
            </div>

            <!-- 智能体执行中状态微卡片 (实时秒表 + 4 阶段动态推进流水) -->
            <div v-if="isTyping" class="message service-message executing-message">
              <div class="message-avatar service-avatar pulse-avatar">
                <el-icon :size="17"><Service /></el-icon>
              </div>
              <div class="message-content">
                <div class="message-meta">
                  <span>AI客服</span>
                  <span class="live-exec-tag">
                    <span class="live-pulse-dot" />
                    多智能体协同执行中
                  </span>
                </div>
                <div class="message-bubble executing-bubble">
                  <div class="exec-header">
                    <div class="exec-title-row">
                      <span class="exec-spinner-ring" />
                      <span class="exec-title">智问学伴算力集群深度推演中</span>
                    </div>
                    <div class="exec-timer-badge">
                      <el-icon class="timer-icon"><Clock /></el-icon>
                      <span>已执行 <strong class="timer-number">{{ executingSeconds.toFixed(1) }}</strong> 秒</span>
                    </div>
                  </div>

                  <!-- 动态微阶段步骤指示 -->
                  <div class="exec-stage-card">
                    <div class="stage-info-row">
                      <span class="stage-icon">{{ executingStage.icon }}</span>
                      <div class="stage-texts">
                        <span class="stage-name">{{ executingStage.label }}</span>
                        <span class="stage-desc">{{ executingStage.desc }}</span>
                      </div>
                    </div>
                    <div class="exec-progress-track">
                      <div class="exec-progress-fill" :style="{ width: executingProgress + '%' }" />
                    </div>
                  </div>

                  <div class="exec-footer-hint">
                    <span class="heartbeat-wave"><i /><i /><i /><i /></span>
                    <span>算力集群正在全力运算，绝无卡顿，请稍候...</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="serviceStatus === 'ai' && !isTyping && faqList.length" class="quick-questions">
            <div class="quick-title"><el-icon><Promotion /></el-icon> 你可能想问</div>
            <div class="quick-list">
              <button v-for="faq in faqList.slice(0, 4)" :key="faq.id" type="button" @click="sendQuickQuestion(faq.question)">
                {{ faq.question }}
              </button>
            </div>
          </div>

          <div class="service-actions">
            <div class="action-state">
              <el-icon v-if="serviceStatus === 'closed'" class="closed-icon"><CircleCheck /></el-icon>
              <span v-if="serviceStatus === 'ai'">AI 客服可以即时为您解答</span>
              <span v-else>本次服务已结束，感谢您的反馈</span>
            </div>
            <div class="action-buttons">
              <el-button v-if="messages.length > 1 && serviceStatus !== 'closed'" type="primary" @click="showEvaluation = true">
                结束并评价
              </el-button>
              <el-button v-if="serviceStatus === 'closed'" type="primary" @click="resetSession">开始新会话</el-button>
            </div>
          </div>

          <div v-if="showEvaluation" class="evaluation-panel">
            <div class="evaluation-heading">
              <div>
                <h3>为本次服务评价</h3>
                <p>您的反馈会帮助我们持续改进服务</p>
              </div>
              <el-button text @click="showEvaluation = false">稍后评价</el-button>
            </div>
            <div class="evaluation-content">
              <el-rate v-model="score" :texts="['很不满意', '不满意', '一般', '满意', '非常满意']" show-text />
              <el-checkbox-group v-model="selectedTags" class="evaluation-tags">
                <el-checkbox-button v-for="tag in evaluationTags" :key="tag" :label="tag">{{ tag }}</el-checkbox-button>
              </el-checkbox-group>
              <el-input v-model="comment" type="textarea" :rows="2" maxlength="100" show-word-limit placeholder="欢迎留下宝贵意见（选填）" />
              <el-button type="primary" :disabled="!score" @click="submitEvaluation">提交评价</el-button>
            </div>
          </div>

          <div class="chat-input-area">
            <el-input
              v-model="inputMessage"
              class="message-input"
              :disabled="isTyping || serviceStatus !== 'ai'"
              :placeholder="serviceStatus === 'closed' ? '本次服务已结束，请开启新会话' : '请输入您想咨询的问题...'
              "
              @keyup.enter="sendMessage"
            >
              <template #append>
                <el-button type="primary" :disabled="!inputMessage.trim() || isTyping || serviceStatus !== 'ai'" @click="sendMessage">发送</el-button>
              </template>
            </el-input>
            <span class="input-tip">按 Enter 发送</span>
          </div>
        </section>

        <!-- 3. 右侧：常见问题与客服小贴士 -->
        <aside class="service-sidebar">
          <div class="sidebar-card service-intro-card">
            <div class="intro-icon"><el-icon :size="26"><Headset /></el-icon></div>
            <div>
              <h2>智问学伴客服</h2>
              <p>{{ statusText }}</p>
            </div>
            <span class="online-mark" />
            <div class="intro-divider" />
            <div class="service-feature">
              <el-icon><Clock /></el-icon>
              <span><strong>AI 智能客服</strong><small>全天候在线响应</small></span>
            </div>
            <div class="service-feature">
              <el-icon><CircleCheck /></el-icon>
              <span><strong>服务评价</strong><small>每次咨询都可反馈</small></span>
            </div>
          </div>

          <div class="sidebar-card faq-card">
            <div class="card-title-row">
              <div>
                <h3>常见问题</h3>
                <p>点击问题即可快速咨询</p>
              </div>
              <el-icon><QuestionFilled /></el-icon>
            </div>
            <div v-if="faqCategories.length" class="faq-categories">
              <button
                v-for="category in faqCategories"
                :key="category"
                type="button"
                :class="{ active: activeFaqCategory === category }"
                @click="activeFaqCategory = category"
              >
                {{ category }}
              </button>
            </div>
            <div v-if="filteredFaqs.length" class="faq-list">
              <button
                v-for="faq in filteredFaqs"
                :key="faq.id"
                type="button"
                class="faq-item"
                :disabled="isTyping || serviceStatus !== 'ai'"
                @click="sendQuickQuestion(faq.question)"
              >
                <span>{{ faq.question }}</span>
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
            <el-empty v-else :image-size="48" description="暂无常见问题" />
          </div>

          <div class="sidebar-card help-card">
            <div class="help-icon"><el-icon><Document /></el-icon></div>
            <div>
              <h3>服务小贴士</h3>
              <p>描述问题时提供课程名称或订单信息，能帮助我们更快定位问题。</p>
            </div>
          </div>
        </aside>
      </section>

    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, ChatDotRound, ChatLineRound, CircleCheck, Clock, Coin, Compass, Delete, Document, Expand, Fold, Folder, FolderOpened, Headset, Medal, Notebook, Plus, Promotion, QuestionFilled, Reading, Refresh, Service, ShoppingCart, Tickets, Trophy, User, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPixelApiKey, getPixelModel } from '@/api/pixelApi'
import { createServiceSession, evaluateService, getServiceFaqs, getServiceSession, sendServiceMessage, getMyServiceSessions, deleteServiceSession, archiveServiceSession } from '@/api/customerService'
import { putCarts, enrolledFreeCourse } from '@/api/order.js'
import { pointsSign } from '@/api/class.js'
import { addNotes } from '@/api/notes.js'
import defaultCover from '@/assets/images/courses/default-cover.svg'

const router = useRouter()

const actionLoading = ref({})
const actionDone = ref({})

function hasActionPath(content) {
  return /\[ACTION_VIEW_PATH:(.+?)\]/.test(content || '')
}

function extractActionRole(content) {
  const match = (content || '').match(/\[ACTION_VIEW_PATH:(.+?)\]/)
  return match ? match[1].trim() : ''
}

function hasAgentActionCard(content) {
  return /\[AGENT_ACTION_CARD:(\{.+?\})\]/.test(content || '')
}

function extractAgentActionCard(content) {
  const match = (content || '').match(/\[AGENT_ACTION_CARD:(\{.+?\})\]/)
  if (!match) return null
  try {
    return JSON.parse(match[1])
  } catch (e) {
    console.error('Failed to parse agent action card:', e)
    return null
  }
}

function cleanMessageContent(content) {
  return (content || '')
    .replace(/\[ACTION_VIEW_PATH:.+?\]/g, '')
    .replace(/\[AGENT_ACTION_CARD:\{.+?\}\]/g, '')
    .trim()
}

function handleImgError(e) {
  if (e && e.target) {
    e.target.src = defaultCover
  }
}

async function handleActionAddToCart(card, msgId) {
  if (!card || !card.courseId) return
  actionLoading.value[msgId + '_cart'] = true
  try {
    const res = await putCarts({ courseId: card.courseId })
    if (res?.code === 200 || res?.code === 0) {
      actionDone.value[msgId + '_cart'] = true
      ElMessage.success(`《${card.title || '课程'}》已成功加入购物车！`)
    } else {
      ElMessage.warning(res?.msg || '加入购物车失败，请稍后重试')
    }
  } catch (err) {
    ElMessage.error(err?.message || '加入购物车请求异常')
  } finally {
    actionLoading.value[msgId + '_cart'] = false
  }
}

function handleActionCheckout(card) {
  if (!card || !card.courseId) return
  router.push({
    path: '/pay/settlement',
    query: { courseId: card.courseId }
  })
}

async function handleActionEnrollFree(card, msgId) {
  if (!card || !card.courseId) return
  actionLoading.value[msgId + '_enroll'] = true
  try {
    const res = await enrolledFreeCourse(card.courseId)
    if (res?.code === 200 || res?.code === 0) {
      actionDone.value[msgId + '_enroll'] = true
      ElMessage.success(`《${card.title || '课程'}》免费报名成功！`)
    } else {
      ElMessage.warning(res?.msg || '报名失败，请稍后重试')
    }
  } catch (err) {
    ElMessage.error(err?.message || '报名请求异常')
  } finally {
    actionLoading.value[msgId + '_enroll'] = false
  }
}

function goToLearning(courseId) {
  if (!courseId) return
  router.push({
    path: '/learning/index',
    query: { courseId }
  })
}

function handleActionLaunchInterview(sessionId) {
  if (!sessionId) {
    router.push('/interview')
    return
  }
  router.push({
    path: '/interview/room',
    query: { id: sessionId }
  })
}

async function handleActionSignIn() {
  actionLoading.value['signing_in'] = true
  try {
    const res = await pointsSign()
    if (res?.code === 200 || res?.code === 0) {
      actionDone.value['today_signed'] = true
      ElMessage.success('🎉 恭喜！每日签到打卡成功，+10 积分已到账！')
    } else {
      ElMessage.info(res?.msg || '今日已完成签到打卡')
      actionDone.value['today_signed'] = true
    }
  } catch (err) {
    ElMessage.info(err?.message || '今日已签到')
    actionDone.value['today_signed'] = true
  } finally {
    actionLoading.value['signing_in'] = false
  }
}

function goToAgentPath(targetRole) {
  router.push({
    path: '/',
    query: targetRole ? { targetRole } : {}
  })
}

async function handleActionSaveNote(card, msgId) {
  if (!card?.noteContent) {
    router.push('/notes/index')
    return
  }
  actionLoading.value[msgId + '_note'] = true
  try {
    const res = await addNotes({
      title: 'AI客服随堂速记',
      content: card.noteContent
    })
    if (res?.code === 200 || res?.code === 0) {
      actionDone.value[msgId + '_note'] = true
      ElMessage.success('🎉 笔记已成功存入您的个人笔记本！')
    } else {
      ElMessage.warning(res?.msg || '保存笔记失败')
    }
  } catch (err) {
    ElMessage.error(err?.message || '保存笔记请求异常')
  } finally {
    actionLoading.value[msgId + '_note'] = false
  }
}

const SESSION_KEY = 'customer_service_session_id'
const inputMessage = ref('')
const isTyping = ref(false)
const messagesRef = ref(null)
const sessionId = ref(sessionStorage.getItem(SESSION_KEY) || '')
const serviceStatus = ref('ai')
const faqList = ref([])
const activeFaqCategory = ref('全部')
const showEvaluation = ref(false)
const score = ref(0)
const comment = ref('')
const selectedTags = ref([])
const evaluationTags = ['回答准确', '响应及时', '表达清晰', '问题未解决']
const messages = ref([welcomeMessage()])
const pixelApiKey = ref(getPixelApiKey())
const pixelModel = ref(getPixelModel())

const pixelApiEnabled = computed(() => Boolean(pixelApiKey.value.trim()))

const isHistorySidebarCollapsed = ref(false)
const sessionTab = ref('active')
const mySessions = ref([])
const loadingSessions = ref(false)

const activeSessions = computed(() => mySessions.value.filter((s) => s.status !== 4))
const archivedSessions = computed(() => mySessions.value.filter((s) => s.status === 4))
const currentTabSessions = computed(() => sessionTab.value === 'active' ? activeSessions.value : archivedSessions.value)

// 智能体执行状态与实时秒表
const executingSeconds = ref(0)
let executingTimer = null

const executingStage = computed(() => {
  const s = executingSeconds.value
  if (s < 1.2) {
    return { icon: '🔍', label: '意图语义解析', desc: '深度解析自然语言指令与实体提取...' }
  } else if (s < 2.8) {
    return { icon: '🛡️', label: '安全防越权核验', desc: '严格验证当前登录权限与数据隔离准则...' }
  } else if (s < 5.0) {
    return { icon: '🤖', label: '智能体协同推演', desc: '跨微服务检索业务数据与调度动作引擎...' }
  } else {
    return { icon: '⚡', label: '动态动作卡片装配', desc: '正在组装富交互卡片并执行自省排版...' }
  }
})

const executingProgress = computed(() => {
  const s = executingSeconds.value
  if (s <= 1.2) return Math.round(15 + (s / 1.2) * 25)
  if (s <= 2.8) return Math.round(40 + ((s - 1.2) / 1.6) * 25)
  if (s <= 5.0) return Math.round(65 + ((s - 2.8) / 2.2) * 20)
  return Math.min(95, Math.round(85 + Math.min(s - 5.0, 10) * 1.0))
})

function toggleHistorySidebar() {
  isHistorySidebarCollapsed.value = !isHistorySidebarCollapsed.value
}

async function loadMySessions() {
  try {
    loadingSessions.value = true
    const res = await getMyServiceSessions({ pageNum: 1, pageSize: 50 })
    if (res && res.code === 200 && res.rows) {
      mySessions.value = res.rows
    }
  } catch (e) {
    console.warn('获取历史会话列表异常:', e)
  } finally {
    loadingSessions.value = false
  }
}

async function handleSwitchSession(session) {
  if (String(session.id) === String(sessionId.value)) return
  try {
    const res = await getServiceSession(session.id)
    if (res && res.code === 200 && res.data) {
      applySession(res.data)
      scrollToBottom()
      ElMessage.success(`已切换至会话【${getSessionTitle(session)}】`)
    }
  } catch (e) {
    ElMessage.error('加载会话失败：' + (e.message || '未知错误'))
  }
}

async function handleCreateNewSession() {
  if (isTyping.value) return
  try {
    const user = userInfo()
    const res = await createServiceSession({ userName: user.nickname || user.username || '访客用户' })
    if (res && res.code === 200 && res.data) {
      applySession(res.data)
      messages.value = [welcomeMessage()]
      ElMessage.success('已为您开启全新会话')
      await loadMySessions()
    }
  } catch (e) {
    resetSession()
  }
}

async function handleArchiveSession(session, archive) {
  try {
    const res = await archiveServiceSession(session.id, archive)
    if (res && res.code === 200) {
      ElMessage.success(archive ? '会话已成功归档' : '会话已恢复至活跃列表')
      await loadMySessions()
    } else {
      ElMessage.error(res?.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('归档操作失败：' + (e.message || '网络异常'))
  }
}

async function handleDeleteSession(session) {
  try {
    const res = await deleteServiceSession(session.id)
    if (res && res.code === 200) {
      ElMessage.success('会话已成功删除')
      if (String(session.id) === String(sessionId.value)) {
        await handleCreateNewSession()
      } else {
        await loadMySessions()
      }
    } else {
      ElMessage.error(res?.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除会话失败：' + (e.message || '网络异常'))
  }
}

function getSessionTitle(item) {
  if (item.lastMessage) {
    const clean = item.lastMessage.replace(/\[ACTION_VIEW_PATH:.+?\]/g, '')
      .replace(/\[AGENT_ACTION_CARD:\{.+?\}\]/g, '')
      .replace(/[#*`_>]/g, '')
      .trim()
    if (clean) return clean.length > 18 ? clean.substring(0, 18) + '...' : clean
  }
  return `咨询会话 #${String(item.id).slice(-4)}`
}

function formatSessionDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return String(dateStr).slice(0, 10)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return isToday ? `今天 ${h}:${min}` : `${m}-${day} ${h}:${min}`
}

const statusText = computed(() => ({
  ai: 'AI客服在线',
  closed: '服务已结束',
}[serviceStatus.value] || 'AI客服在线'))

const faqCategories = computed(() => ['全部', ...new Set(faqList.value.map((faq) => faq.category).filter(Boolean))])
const filteredFaqs = computed(() => {
  if (activeFaqCategory.value === '全部') return faqList.value.slice(0, 8)
  return faqList.value.filter((faq) => faq.category === activeFaqCategory.value).slice(0, 8)
})

function welcomeMessage() {
  return {
    id: `welcome-${Date.now()}`,
    type: 'ai',
    senderName: 'AI客服',
    content: '您好！我是智问学伴的智能客服小智 🤖\n课程学习、订单支付、账号登录等问题都可以咨询我。',
    time: getTime(),
  }
}

function getTime() {
  const date = new Date()
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatMessageTime(value) {
  const text = String(value || '')
  const matched = text.match(/(\d{2}:\d{2})(?::\d{2})?$/)
  return matched ? matched[1] : text
}

function userInfo() {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || '{}')
  } catch (error) {
    return {}
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

function addLocalMessage(type, content, senderName, costSeconds = null) {
  messages.value.push({
    id: `${type}-${Date.now()}-${Math.random()}`,
    type,
    senderName: senderName || (type === 'user' ? '我' : 'AI客服'),
    content,
    time: getTime(),
    costSeconds,
  })
  scrollToBottom()
}

function normalizeStatus(value) {
  return Number(value) === 3 || value === 'closed' ? 'closed' : 'ai'
}

function normalizeMessage(message) {
  if (!message) return null
  const messageType = Number(message.messageType)
  return {
    id: message.id || `message-${Date.now()}-${Math.random()}`,
    type: message.type || (messageType === 1 ? 'user' : 'ai'),
    senderName: message.senderName || (messageType === 1 ? '我' : 'AI客服'),
    content: message.content || '',
    isFallback: Number(message.isFallback) === 1,
    time: message.time || message.createTime || getTime(),
    costSeconds: message.costSeconds || null,
  }
}

function applySession(session) {
  if (!session) return
  sessionId.value = String(session.id || session.sessionId || '')
  if (sessionId.value) sessionStorage.setItem(SESSION_KEY, sessionId.value)
  serviceStatus.value = normalizeStatus(session.status)
  const serverMessages = (session.messages || []).map(normalizeMessage).filter(Boolean)
  if (serverMessages.length) messages.value = serverMessages
  if (session.evaluation) {
    score.value = session.evaluation.score || 0
    selectedTags.value = session.evaluation.tags || []
    comment.value = session.evaluation.comment || ''
  }
}

async function ensureSession(user) {
  if (sessionId.value) return sessionId.value
  const response = await createServiceSession({ userName: user.nickname || user.username || '访客用户' })
  if (response.code !== 200 || !response.data?.id) throw new Error(response.msg || '客服会话创建失败')
  applySession(response.data)
  return sessionId.value
}

async function sendMessage() {
  const question = inputMessage.value.trim()
  if (!question || isTyping.value || serviceStatus.value !== 'ai') return

  const user = userInfo()
  addLocalMessage('user', question, user.nickname || user.username || '我')
  inputMessage.value = ''
  isTyping.value = true

  const startTs = performance.now()
  executingSeconds.value = 0
  if (executingTimer) clearInterval(executingTimer)
  executingTimer = setInterval(() => {
    executingSeconds.value = Math.max(0, (performance.now() - startTs) / 1000)
  }, 100)

  let costSec = null
  try {
    const currentSessionId = await ensureSession(user)
    const response = await sendServiceMessage(currentSessionId, {
      content: question,
      apiKey: pixelApiEnabled.value ? pixelApiKey.value : undefined,
      model: pixelApiEnabled.value ? pixelModel.value : undefined,
    })
    if (response.code !== 200 || !response.data) throw new Error(response.msg || response.message || '客服暂时不可用')
    costSec = ((performance.now() - startTs) / 1000).toFixed(1)
    applySession(response.data.session)
    const reply = normalizeMessage(response.data.message)
    if (reply) reply.costSeconds = costSec

    const sessionMessages = response.data.session?.messages || []
    const replyAlreadyIncluded = reply && sessionMessages.some((message) => String(message.id) === String(reply.id))
    if (reply && !replyAlreadyIncluded) {
      const content = reply.isFallback && pixelApiEnabled.value
        ? `智能服务繁忙，已为您切换知识库回答：\n${reply.content}`
        : reply.content
      addLocalMessage(reply.type, content, reply.senderName, costSec)
    } else if (messages.value.length > 0) {
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg.type === 'ai') {
        lastMsg.costSeconds = costSec
      }
    }
  } catch (error) {
    costSec = ((performance.now() - startTs) / 1000).toFixed(1)
    const rawError = String(error.message || '')
    const isInvalidApiKey = /invalid api key|incorrect api key|无效.*key|401|403/i.test(rawError)
    const errorMessage = isInvalidApiKey
      ? '智能客服服务凭证无效或已过期，请在管理端配置正确的接入凭证。'
      : pixelApiEnabled.value
        ? `智能客服接口响应异常：${rawError || '请检查网络连接或稍后重试。'}`
        : '抱歉，客服服务暂时不可用，请稍后再试。'
    addLocalMessage('ai', errorMessage, 'AI客服', costSec)
  } finally {
    if (executingTimer) {
      clearInterval(executingTimer)
      executingTimer = null
    }
    isTyping.value = false
    loadMySessions()
  }
}

function sendQuickQuestion(question) {
  if (serviceStatus.value !== 'ai' || isTyping.value) return
  inputMessage.value = question
  sendMessage()
}

async function submitEvaluation() {
  if (!sessionId.value || !score.value) return
  try {
    const response = await evaluateService(sessionId.value, {
      score: score.value,
      tags: selectedTags.value,
      comment: comment.value,
    })
    if (response.code !== 200) throw new Error(response.message)
    serviceStatus.value = 'closed'
    showEvaluation.value = false
    ElMessage.success('感谢您的评价')
  } catch (error) {
    ElMessage.error('评价提交失败，请稍后重试')
  }
}

function resetSession() {
  sessionId.value = ''
  sessionStorage.removeItem(SESSION_KEY)
  serviceStatus.value = 'ai'
  showEvaluation.value = false
  score.value = 0
  comment.value = ''
  selectedTags.value = []
  messages.value = [welcomeMessage()]
  scrollToBottom()
}

onMounted(async () => {
  try {
    loadMySessions()
    const faqResponse = await getServiceFaqs({ enabled: 1 })
    if (faqResponse.code === 200) faqList.value = faqResponse.data || []

    if (sessionId.value) {
      const sessionResponse = await getServiceSession(sessionId.value)
      if (sessionResponse.code === 200 && sessionResponse.data) {
        applySession(sessionResponse.data)
      }
    }
  } catch (error) {
    // 接口失败时仍保留页面的基础问候语和输入能力。
  }
  scrollToBottom()
})
</script>

<style scoped lang="scss">
.customer-service-page {
  min-height: calc(100vh - 70px);
  padding: 32px 0 48px;
  background: #f5f7fb;
}

.service-container {
  max-width: 1380px;
  width: 96%;
  margin: 0 auto;
}

.service-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 26px;
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  color: #2563EB;
  font-size: 13px;
  font-weight: 600;
}

.service-hero h1 {
  color: #202943;
  font-size: 32px;
  line-height: 1.25;
}

.service-hero p {
  margin-top: 8px;
  color: #7c849b;
  font-size: 14px;
}

.mini-status-dot,
.online-mark {
  display: inline-block;
  border-radius: 50%;
  background: #67c23a;
  box-shadow: 0 0 0 4px rgba(103, 194, 58, 0.12);
}

.service-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 280px;
  align-items: start;
  gap: 16px;
  transition: all 0.3s ease;
}

.service-layout.history-collapsed {
  grid-template-columns: 60px minmax(0, 1fr) 280px;
}

@media (max-width: 1200px) {
  .service-layout {
    grid-template-columns: 240px minmax(0, 1fr);
  }
  .service-layout.history-collapsed {
    grid-template-columns: 58px minmax(0, 1fr);
  }
  .service-sidebar {
    grid-column: 1 / -1;
  }
}

/* 1. 左侧长方形框架可折叠菜单栏 */
.history-sidebar {
  background: #fff;
  border: 1px solid #edf0f6;
  border-radius: 12px;
  box-shadow: 0 5px 20px rgba(45, 58, 93, 0.04);
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.history-sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 690px;
  padding: 16px;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.history-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.collapse-toggle-btn {
  color: #64748b;
  background: #f1f5f9;
  border: 0;

  &:hover {
    color: #2563eb;
    background: #e2e8f0;
  }
}

.new-chat-btn {
  width: 100%;
  height: 38px;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border: 0;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);

  &:hover {
    background: linear-gradient(135deg, #1d4ed8, #2563eb);
  }
}

.session-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  padding: 3px;
  background: #f1f5f9;
  border-radius: 8px;

  button {
    flex: 1;
    padding: 6px 0;
    font-size: 12px;
    font-weight: 500;
    color: #64748b;
    background: transparent;
    border: 0;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;

    &.active {
      color: #2563eb;
      background: #fff;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
      font-weight: 600;
    }
  }
}

.session-list-wrap {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-right: 2px;
  scrollbar-width: thin;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 4px;
  }
}

.session-item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f1f5f9;
    border-color: #e2e8f0;

    .session-item-actions {
      opacity: 1;
      pointer-events: auto;
    }
  }

  &.active {
    background: #eff6ff;
    border-color: #bfdbfe;

    .session-indicator {
      background: #2563eb;
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
    }

    .title-text {
      color: #1d4ed8;
      font-weight: 600;
    }
  }
}

.session-card-content {
  flex: 1;
  min-width: 0;
}

.session-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
}

.title-text {
  font-size: 13px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-card-meta {
  margin-top: 3px;
  padding-left: 14px;
}

.session-time {
  font-size: 11px;
  color: #94a3b8;
}

.session-item-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s;

  .action-btn {
    padding: 4px;
    font-size: 14px;
    color: #64748b;
    border-radius: 4px;

    &.archive-btn:hover {
      color: #f59e0b;
      background: #fef3c7;
    }

    &.delete-btn:hover {
      color: #ef4444;
      background: #fee2e2;
    }
  }
}

/* 折叠极简状态 */
.history-sidebar-collapsed-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 690px;
  padding: 14px 6px;
  gap: 14px;

  .expand-toggle-btn {
    color: #64748b;
    background: #f1f5f9;
    border: 0;

    &:hover {
      color: #2563eb;
      background: #e2e8f0;
    }
  }

  .mini-new-btn {
    background: #2563eb;
    border: 0;
    box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
  }

  .mini-session-icons {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 10px;
  }

  .mini-session-dot {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: #f1f5f9;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #64748b;
    font-size: 16px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #e2e8f0;
      color: #2563eb;
    }

    &.active {
      background: #eff6ff;
      color: #2563eb;
      border: 1px solid #bfdbfe;
    }
  }
}

.header-expand-btn {
  margin-right: 6px;
  color: #64748b;
  background: #f1f5f9;
  border: 0;

  &:hover {
    color: #2563eb;
    background: #e2e8f0;
  }
}

/* 2. 智能体执行中状态微卡片 */
.executing-message {
  .service-avatar.pulse-avatar {
    animation: avatarPulse 2s infinite ease-in-out;
  }
}

@keyframes avatarPulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 0 6px rgba(37, 99, 235, 0);
  }
}

.live-exec-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
  font-size: 11px;
  color: #2563eb;
  background: #eff6ff;
  padding: 2px 8px;
  border-radius: 12px;
  border: 1px solid #dbeafe;

  .live-pulse-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #2563eb;
    animation: dotBlink 1.2s infinite;
  }
}

@keyframes dotBlink {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(0.7); }
}

.executing-bubble {
  background: linear-gradient(145deg, #ffffff, #f8fafc) !important;
  border: 1px solid #e2e8f0 !important;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08) !important;
  padding: 16px 18px !important;
  min-width: 340px;
  max-width: 480px;
}

.exec-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.exec-title-row {
  display: flex;
  align-items: center;
  gap: 8px;

  .exec-spinner-ring {
    width: 14px;
    height: 14px;
    border: 2px solid #93c5fd;
    border-top-color: #2563eb;
    border-radius: 50%;
    animation: spinRing 0.8s linear infinite;
  }

  .exec-title {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
  }
}

@keyframes spinRing {
  to { transform: rotate(360deg); }
}

.exec-timer-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #475569;
  background: #f1f5f9;
  padding: 3px 8px;
  border-radius: 12px;

  .timer-icon {
    color: #2563eb;
  }

  .timer-number {
    color: #2563eb;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }
}

.exec-stage-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
}

.stage-info-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;

  .stage-icon {
    font-size: 16px;
    line-height: 1.2;
  }

  .stage-texts {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .stage-name {
    font-size: 12px;
    font-weight: 600;
    color: #1e293b;
  }

  .stage-desc {
    font-size: 11px;
    color: #64748b;
  }
}

.exec-progress-track {
  width: 100%;
  height: 4px;
  background: #e2e8f0;
  border-radius: 2px;
  overflow: hidden;

  .exec-progress-fill {
    height: 100%;
    background: linear-gradient(90deg, #3b82f6, #60a5fa, #2563eb);
    background-size: 200% 100%;
    animation: progressGradient 2s ease infinite;
    transition: width 0.3s ease;
  }
}

@keyframes progressGradient {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

.exec-footer-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #94a3b8;

  .heartbeat-wave {
    display: flex;
    align-items: center;
    gap: 3px;

    i {
      display: inline-block;
      width: 3px;
      height: 10px;
      background: #3b82f6;
      border-radius: 2px;
      animation: waveHeight 1.2s infinite ease-in-out;

      &:nth-child(2) { animation-delay: 0.15s; }
      &:nth-child(3) { animation-delay: 0.3s; }
      &:nth-child(4) { animation-delay: 0.45s; }
    }
  }
}

@keyframes waveHeight {
  0%, 100% { height: 4px; opacity: 0.4; }
  50% { height: 12px; opacity: 1; }
}

/* 3. 执行完成耗时徽章 */
.meta-cost-badge {
  margin-left: 8px;
  font-size: 11px;
  color: #10b981;
  background: #ecfdf5;
  padding: 1px 6px;
  border-radius: 8px;
  border: 1px solid #d1fae5;
}

.execution-done-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 4px 10px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 6px;
  font-size: 12px;
  color: #64748b;

  .done-check {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: #10b981;
    color: #fff;
    font-size: 10px;
    font-weight: 700;
  }

  .done-time strong {
    color: #059669;
    font-weight: 600;
  }
}

.service-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  padding: 20px;
  background: #fff;
  border: 1px solid #edf0f6;
  border-radius: 8px;
  box-shadow: 0 5px 20px rgba(45, 58, 93, 0.04);
}

.service-intro-card {
  position: relative;
  display: grid;
  grid-template-columns: 54px 1fr 8px;
  align-items: center;
  gap: 12px;
}

.intro-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  color: #fff;
  background: #2563EB;
  border-radius: 8px;
}

.service-intro-card h2 {
  color: #27304a;
  font-size: 16px;
}

.service-intro-card p {
  margin-top: 6px;
  color: #67c23a;
  font-size: 12px;
}

.online-mark {
  width: 8px;
  height: 8px;
}

.intro-divider {
  grid-column: 1 / -1;
  width: 100%;
  height: 1px;
  margin: 4px 0 2px;
  background: #f0f2f7;
}

.service-feature {
  display: flex;
  grid-column: 1 / -1;
  align-items: flex-start;
  gap: 12px;
  color: #8b94aa;
}

.service-feature .el-icon {
  margin-top: 2px;
  color: #2563EB;
}

.service-feature span {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.service-feature strong {
  color: #3d4761;
  font-size: 13px;
  font-weight: 500;
}

.service-feature small {
  color: #9ba3b5;
  font-size: 12px;
}

.card-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.card-title-row h3,
.help-card h3 {
  color: #27304a;
  font-size: 16px;
}

.card-title-row p {
  margin-top: 5px;
  color: #9ba3b5;
  font-size: 12px;
}

.card-title-row > .el-icon {
  color: #b3bbd0;
  font-size: 20px;
}

.faq-categories {
  display: flex;
  gap: 7px;
  margin: 16px 0 10px;
  overflow-x: auto;
  scrollbar-width: none;
}

.faq-categories::-webkit-scrollbar {
  display: none;
}

.faq-categories button {
  flex: 0 0 auto;
  padding: 5px 10px;
  color: #8b94aa;
  font-size: 12px;
  background: #f6f7fb;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}

.faq-categories button.active {
  color: #2563EB;
  background: #eef0ff;
}

.faq-list {
  display: flex;
  flex-direction: column;
}

.faq-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 11px 0;
  color: #556079;
  text-align: left;
  background: transparent;
  border: 0;
  border-bottom: 1px solid #f1f3f7;
  cursor: pointer;
}

.faq-item:last-child {
  border-bottom: 0;
}

.faq-item span {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.faq-item .el-icon {
  flex-shrink: 0;
  color: #b4bdd0;
  transition: transform 0.2s;
}

.faq-item:hover {
  color: #2563EB;
}

.faq-item:hover .el-icon {
  transform: translateX(3px);
  color: #2563EB;
}

.faq-item:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.help-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: linear-gradient(135deg, #f4f6ff, #fbfaff);
  border-color: #e8eaff;
}

.help-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  color: #2563EB;
  background: #e7e9ff;
  border-radius: 10px;
}

.help-card p {
  margin-top: 6px;
  color: #8b94aa;
  font-size: 12px;
  line-height: 1.6;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-height: 720px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #edf0f6;
  border-radius: 8px;
  box-shadow: 0 7px 26px rgba(45, 58, 93, 0.06);
}

.chat-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 19px 24px;
  border-bottom: 1px solid #eff1f5;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: #fff;
  background: #2563EB;
  border-radius: 13px;
}

.chat-title h2 {
  color: #27304a;
  font-size: 17px;
}

.chat-title span {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 5px;
  color: #8f98ab;
  font-size: 12px;
}

.mini-status-dot {
  width: 6px;
  height: 6px;
  box-shadow: none;
}

.new-session-button {
  color: #7d879d;
}

.new-session-button:hover {
  color: #2563EB;
  background: #f3f4ff;
}

.chat-messages {
  flex: 1;
  min-height: 365px;
  max-height: 470px;
  overflow-y: auto;
  padding: 20px 24px 12px;
  background: #fafbfe;
}

.conversation-date {
  width: fit-content;
  margin: 0 auto 20px;
  padding: 4px 11px;
  color: #a2a9ba;
  font-size: 11px;
  background: #f0f2f7;
  border-radius: 12px;
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.user-message {
  justify-content: flex-end;
}

.message-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  margin-top: 19px;
  color: #fff;
  border-radius: 10px;
}

.service-avatar {
  background: #2563EB;
}

.user-avatar {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
}

.message-content {
  max-width: 74%;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 19px;
  color: #949db0;
  font-size: 12px;
}

.user-message .message-meta {
  justify-content: flex-end;
}

.message-meta time {
  color: #b4bbc9;
  font-size: 11px;
}

.message-bubble {
  padding: 12px 16px;
  color: #334155;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 4px 13px 13px 13px;
  box-shadow: 0 2px 7px rgba(45, 58, 93, 0.03);
}

.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.agent-action-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #e0edff 100%);
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.08);
}

.agent-action-card:hover {
  background: linear-gradient(135deg, #e5f0ff 0%, #d4e7fe 100%);
  border-color: #93c5fd;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.14);
}

.card-left {
  flex: 1;
}

.card-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: #1d4ed8;
  background: #dbeafe;
  padding: 2px 7px;
  border-radius: 4px;
  margin-bottom: 5px;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e3a8a;
  margin-bottom: 3px;
}

.card-desc {
  font-size: 11px;
  color: #4b5563;
  line-height: 1.4;
}

.card-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
  background: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #bfdbfe;
  transition: all 0.2s ease;
}

.agent-action-card:hover .card-right {
  background: #2563eb;
  color: #ffffff;
  border-color: #2563eb;
}

.agent-interactive-container {
  margin-top: 12px;
  width: 100%;
}

.agent-interactive-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06);
  padding: 14px 16px;
  transition: all 0.25s ease;
  overflow: hidden;

  &:hover {
    box-shadow: 0 6px 20px rgba(15, 23, 42, 0.1);
    border-color: #cbd5e1;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f1f5f9;

    .tag-badge {
      font-size: 12px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 6px;
      display: inline-flex;
      align-items: center;
      gap: 4px;

      &.course-badge {
        background: #eff6ff;
        color: #2563eb;
      }
      &.interview-badge {
        background: #fdf2f8;
        color: #db2777;
      }
      &.signin-badge {
        background: #fefce8;
        color: #ca8a04;
      }
      &.resume-badge {
        background: #f0fdf4;
        color: #16a34a;
      }
      &.learning-badge {
        background: #f5f3ff;
        color: #7c3aed;
      }
      &.order-badge {
        background: #fff7ed;
        color: #ea580c;
      }
      &.coupon-badge {
        background: #fef2f2;
        color: #ef4444;
      }
      &.cart-badge {
        background: #f0fdfa;
        color: #0d9488;
      }
      &.exam-badge {
        background: #f1f5f9;
        color: #475569;
      }
      &.note-badge {
        background: #fefce8;
        color: #b45309;
      }
      &.points-badge {
        background: #fffbeb;
        color: #d97706;
      }
      &.portrait-badge {
        background: #eef2ff;
        color: #4f46e5;
      }
      &.nav-badge {
        background: #f3f4f6;
        color: #374151;
      }
    }

    .sub-badge {
      font-size: 11px;
      font-weight: 600;
      color: #64748b;
      background: #f1f5f9;
      padding: 2px 8px;
      border-radius: 12px;

      &.score-badge {
        background: #dcfce7;
        color: #15803d;
      }
    }

    .voice-badge {
      font-size: 11px;
      color: #be185d;
      background: #fce7f3;
      padding: 2px 8px;
      border-radius: 12px;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-weight: 600;

      .wave-icon {
        display: inline-block;
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #ec4899;
        animation: pulse 1.5s infinite;
      }
    }
  }

  .card-body {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 12px;

    .course-thumb {
      width: 80px;
      height: 52px;
      border-radius: 6px;
      overflow: hidden;
      flex-shrink: 0;
      background: #0f172a;
      border: 1px solid #e2e8f0;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }

    .course-meta {
      flex: 1;
      min-width: 0;

      .course-title {
        font-size: 13px;
        font-weight: 700;
        color: #0f172a;
        margin-bottom: 4px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .course-teacher {
        font-size: 11px;
        color: #64748b;
        margin-bottom: 4px;
      }

      .course-pricing {
        display: flex;
        align-items: baseline;
        gap: 6px;

        .free-price {
          font-size: 13px;
          font-weight: 700;
          color: #16a34a;
        }

        .curr-price {
          font-size: 14px;
          font-weight: 800;
          color: #e11d48;
        }

        .orig-price {
          font-size: 11px;
          color: #94a3b8;
          text-decoration: line-through;
        }
      }
    }

    .card-icon-box {
      width: 48px;
      height: 48px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.interview-icon {
        background: #fdf2f8;
        color: #db2777;
      }
      &.signin-icon {
        background: #fef9c3;
        .coin-emoji {
          font-size: 24px;
        }
      }
      &.resume-icon {
        background: #f0fdf4;
        color: #16a34a;
      }
      &.learning-icon {
        background: #f5f3ff;
        color: #7c3aed;
      }
      &.order-icon {
        background: #ffedd5;
        color: #ea580c;
      }
      &.coupon-icon {
        background: #fee2e2;
        color: #ef4444;
      }
      &.cart-icon {
        background: #ccfbf1;
        color: #0d9488;
      }
      &.exam-icon {
        background: #f1f5f9;
        color: #475569;
      }
      &.note-icon {
        background: #fef3c7;
        color: #d97706;
      }
      &.points-icon {
        background: #fef3c7;
        color: #b45309;
      }
      &.portrait-icon {
        background: #e0e7ff;
        color: #4338ca;
      }
      &.nav-icon {
        background: #f3f4f6;
        color: #374151;
      }
    }

    .interview-meta,
    .signin-meta,
    .resume-meta,
    .learning-meta,
    .order-meta,
    .coupon-meta,
    .cart-meta,
    .exam-meta,
    .note-meta,
    .points-meta,
    .portrait-meta,
    .nav-meta {
      flex: 1;
      min-width: 0;

      .job-title,
      .signin-title,
      .resume-title,
      .learning-title,
      .interview-title,
      .order-title,
      .coupon-title,
      .cart-title,
      .exam-title,
      .note-title,
      .points-title,
      .portrait-title,
      .nav-title {
        font-size: 13px;
        font-weight: 700;
        color: #0f172a;
        margin-bottom: 3px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .company-tag,
      .signin-desc,
      .resume-desc,
      .learning-desc,
      .interview-desc,
      .order-desc,
      .coupon-desc,
      .cart-desc,
      .exam-desc,
      .note-desc,
      .points-desc,
      .portrait-desc,
      .nav-desc {
        font-size: 11px;
        color: #475569;
        margin-bottom: 2px;
        line-height: 1.4;
      }

      .status-tip {
        font-size: 11px;
        color: #94a3b8;
      }
    }
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
    padding-top: 8px;
    border-top: 1px dashed #e2e8f0;

    .el-button {
      font-weight: 600;
      padding: 6px 14px;
      font-size: 12px;
    }

    .launch-btn {
      background: linear-gradient(135deg, #2563eb, #1d4ed8);
      border: none;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
      &:hover {
        background: linear-gradient(135deg, #1d4ed8, #1e40af);
      }
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.85); }
}

.user-message .message-bubble {
  color: #fff;
  background: #2563EB;
  border: 0;
  border-radius: 13px 4px 13px 13px;
}

.typing-bubble {
  display: flex;
  align-items: center;
  gap: 5px;
  width: 64px;
  padding: 15px 14px;
}

.typing-bubble i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #aab2c2;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-bubble i:nth-child(2) { animation-delay: -0.16s; }
.typing-bubble i:nth-child(3) { animation-delay: -0.32s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.quick-questions {
  padding: 13px 24px 15px;
  border-top: 1px solid #eff1f5;
}

.quick-title {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 9px;
  color: #8b94aa;
  font-size: 12px;
}

.quick-title .el-icon {
  color: #2563EB;
}

.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-list button {
  padding: 6px 11px;
  color: #2563EB;
  font-size: 12px;
  background: #f2f3ff;
  border: 1px solid #e8eaff;
  border-radius: 8px;
  cursor: pointer;
}

.quick-list button:hover {
  color: #fff;
  background: #2563EB;
  border-color: #2563EB;
}

.service-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 62px;
  padding: 10px 24px;
  border-top: 1px solid #eff1f5;
}

.action-state {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #8b94aa;
  font-size: 12px;
}

.closed-icon {
  color: #67c23a;
  font-size: 16px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.evaluation-panel {
  padding: 17px 24px 19px;
  background: #fffaf2;
  border-top: 1px solid #f4e4c7;
}

.evaluation-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.evaluation-heading h3 {
  color: #544735;
  font-size: 14px;
}

.evaluation-heading p {
  margin-top: 4px;
  color: #aa987d;
  font-size: 12px;
}

.evaluation-heading .el-button {
  padding-top: 0;
  color: #aa987d;
}

.evaluation-content {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 13px;
}

.evaluation-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.evaluation-content :deep(.el-textarea) {
  width: 220px;
}

.chat-input-area {
  position: relative;
  padding: 16px 24px 20px;
  border-top: 1px solid #eff1f5;
}

.message-input :deep(.el-input__wrapper) {
  min-height: 42px;
  box-shadow: 0 0 0 1px #e3e7ef inset;
}

.message-input :deep(.el-input-group__append) {
  padding: 0;
  background: #2563EB;
  border-color: transparent;
}

.message-input :deep(.el-input-group__append .el-button) {
  height: 42px;
  padding: 0 22px;
  color: #fff;
  border: 0;
}

.input-tip {
  display: block;
  margin-top: 7px;
  color: #b1b8c7;
  font-size: 11px;
}


@media (max-width: 1220px) {
  .service-container {
    width: calc(100% - 40px);
  }
}

@media (max-width: 900px) {
  .service-layout {
    grid-template-columns: 1fr;
  }

  .service-sidebar {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .help-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 620px) {
  .customer-service-page {
    padding-top: 20px;
  }

  .service-container {
    width: calc(100% - 24px);
  }

  .service-hero {
    display: block;
  }


  .service-sidebar {
    display: flex;
  }

  .chat-panel {
    min-height: 650px;
  }

  .chat-panel-header,
  .chat-messages,
  .quick-questions,
  .service-actions,
  .evaluation-panel,
  .chat-input-area {
    padding-left: 16px;
    padding-right: 16px;
  }

  .service-actions,
  .evaluation-content {
    align-items: flex-start;
    flex-direction: column;
  }

  .action-buttons,
  .action-buttons .el-button {
    width: 100%;
  }

  .evaluation-content :deep(.el-textarea) {
    width: 100%;
  }
}
</style>
