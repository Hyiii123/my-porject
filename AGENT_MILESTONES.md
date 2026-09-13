# 智问学伴项目重大里程碑与技术演进记录 (Project Milestones & Architecture Evolution)

> 本文件为智问学伴（Zhiwen Study Companion）项目真实工程交付物、重大业务功能上线、架构演进与技术突破的**唯一专属记录文件**。
> 关联文档：[`AGENT_WORKLOG.md`](file:///D:/education%20system/my-porject/AGENT_WORKLOG.md)（环境资产索引、核心踩坑手册与执行清单）｜[`share-parent/docs/AGENT_HANDOFF.md`](file:///D:/education%20system/my-porject/share-parent/docs/AGENT_HANDOFF.md)（架构全景图）

---

## 📌 里程碑记录准则 (Milestone Logging Rules)
1. **严格限定高价值交付**：仅记录真实工程交付物、重大业务功能上线、底层架构演进、核心缺陷根治、大规模数据集迁移与算法突破。
2. **严禁日常琐碎记录**：严禁将日常启动/重启服务、容器健康检查、简单依赖查看、常规基础测试记录为里程碑。日常运维踩坑与解决方案请记录至 [`AGENT_WORKLOG.md`](file:///D:/education%20system/my-porject/AGENT_WORKLOG.md) 的第四节。
3. **结构化与时间溯源**：每个里程碑须标明明确时间戳（`YYYY-MM-DD HH:mm:ss`）、演进主题以及清晰的「核心成果」要点列表。

---

## 🚀 重大里程碑与工作演进记录 (Milestones & Evolution)

### 2026-09-13 16:00:00 - 课程随堂测评闭环上线与考试审计全链路贯通：学习室随堂测验Tab开箱即用、未答题目完整审计轨迹入库、交卷与重考结果幂等回显、答卷参数全兼容容错

* **核心成果**：
  1. **课程学习室随堂测验交互闭环全新上线** (`frontends/portal/src/pages/learning/index.vue`)：
     - 激活长期闲置的 `api/subject.js` 测验接口，在视频学习主页面全新开辟「随堂测验」功能 Tab；
     - 随课程加载自动异步拉取配套测试题与测验规格（总题数、满分值、及格线），支持单选、多选/不定项、判断题与主观问答题等全题型自适应渲染与作答校验；
     - 提交答卷后直接展示考生成绩大屏（得分、通过/未通过状态横幅、错题数量），并在原题卡片下方即时展开正确答案与考点精析；
     - 提供「重置作答」、「重新测试」以及「跳转个人中心查看完整批阅报告」的连贯体验。
  2. **考试判分与全题目审计轨迹入库机制** (`EducationService.submitExam`)：
     - 排查并修复了学员部分作答或遗漏题目时未作答题目未写入 `tj_education.edu_exam_answer` 导致的数据库审计记录断层问题；
     - 自动遍历当前考试全部题目，对所有未作答题目统一写入审计记录（`userAnswer = ""`，`isCorrect = 0`，`score = 0`），确保每场考试所有题目的作答轨迹百分之百完整归档。
  3. **重复交卷幂等保护与视图完整回显**：
     - 解决已交卷记录再次提交时只返回简单记录视图而丢失题目和作答明细导致的页面白屏缺陷；已提交记录重复请求时自动回显完整题目、得分、正确答案与判分明细；
     - `examRecordDetails` 接口同时补齐 `answers`、`details` 与 `list` 视图，满足不同端和老版本的兼容要求。
  4. **答题入参健壮性防御与双契约兼容**：
     - `submitExam` 增加智能映射兼容：题目 ID 字段同时支持 `questionId` 与 `id`，答案字段同时支持 `answer` 与 `userAnswer`；
     - 前端同步提交 `answer` 与 `userAnswer`，彻底消除不同版本 API 之间的字段不匹配风险。
  5. **云端生产部署与全流程自动化定向验证**：
     - `share-education.jar` 与 `portal-dist` 生产包平滑同步至云端 `tianji-education` 与 `tianji-portal-ui` 容器；
     - 真实 Token 自动化测试验证通过：获取随堂测验题目（200 OK）、单题正确提交并留空其余题目判分准确（200 OK）、数据库完整生成 5 道题目的完整审计记录（未答题目均标为空与 0 分）、重复交卷幂等回显完整视图（200 OK）。

### 2026-09-13 15:00:00 - 交易结算与订单履约全生命周期重构：优惠券核销与退款/取消自动原路返还状态机、超时订单自动过期关闭、全链路订单履约进度节点与退款状态打通、视频连播自动化升级

* **核心成果**：
  1. **优惠券核销与防重用状态机闭环**：
     - 排查并根治 `TradeService.placeOrder` 中 `MktUserCoupon` 状态从未更新导致已领取的优惠券可以无限次重复下单使用的严重资产损失漏洞；
     - 重构 `prePlaceOrder`：优先智能查询当前登录用户在 `mkt_user_coupon` 中已领且未使用的有效优惠券，透传 `userCouponId`，杜绝未领券直接调用或跨人冒用；
     - 重构 `placeOrder`：匹配用户优惠券主键 ID 或优惠券 ID，下单时原子更新 `mkt_user_coupon.status = 1`（已使用）、`used_at = now()`、`used_order_id = order.getId()`；对于使用公共优惠券下单的用户自动补全核销追溯记录；
     - 扩展 `placeOrder` 支持 `courseIds` 数组入参及单 `courseId` 双模式兼容。
  2. **订单取消与超时自动关单原路返还优惠券机制**：
     - 重构 `cancelOrder` 与 `deleteOrder`：当未支付订单被主动取消或删除时，自动触发 `restoreOrderCoupon`，将对应关联的 `mkt_user_coupon` 原路恢复为 `status = 0`、`used_at = null`、`used_order_id = null`，保障学员资产权益；
     - 新增订单超时关闭与状态机自愈：在查看订单详情或列表时自动比对 `expireTime`，若未支付且已超时则自动触发流转为已关闭（`orderStatus = 4`）并自动释放并退回优惠券。
  3. **订单详情全链路履约时间轴节点与退款状态打通**：
     - 根治 `TradeService.orderView` 中 `progressNodes` 硬编码空列表导致前端订单详情页进度时间轴白屏/空圆点缺陷；
     - 动态生成「提交订单」、「完成支付」、「订单关闭」、「申请退款/退款成功/退款驳回」等真实生命周期事件节点，透传真实时间戳与节点 ID；
     - 关联 `TrRefundApply` 真实审批状态计算并透传各课程细项的 `refundStatus`（1: 待审核, 4: 已驳回, 5: 退款成功）与 `canRefund` 可退款布尔标识；
     - 丰富 `refundView` 退款详情，补齐 `orderTime` 与 `paySuccessTime`。
  4. **前端订单详情与课程播放链路体验升级**：
     - `myOrderDetails.vue`：修复时间分割 `:key="it.name"`（字符串无 name 属性）及 null 安全防护；修正申请退款提交后的反馈提示为更准确的「退款申请已提交，请耐心等待审核」；
     - `learning/index.vue`：优化视频播放完毕事件 `handleVideoEnded`，学习完当前小节后在恭喜提示的同时自动检测章节播放列表，若存在下一小节则倒计时 2.5 秒自动无缝切播至下一课时，大幅提升学员沉浸式学习体验。
  5. **生产热更新与真实鉴权自动化验证**：
     - 本地完成 `share-trade.jar` 与 `portal-ui` 离线打包，安全同步至云端 `tianji-trade` 与 `tianji-portal-ui` 容器并重载生效；
     - 通过 Python 真实请求鉴权测试：`prePlaceOrder` 正确返回用户专属可用优惠券（200 OK）；下单使用优惠券后 `mkt_user_coupon` 立即置为已核销不可再次预选；取消订单后优惠券状态原子原路退回（200 OK）；订单详情获取完整时间轴事件节点与退款状态透传（200 OK）。

### 2026-09-13 14:00:00 - 社区问答与互动讨论全生命周期架构重构：多级嵌套回复隔离、跨库真实用户身份穿透、点赞底层模型兼容与游客免登录友好浏览升级

* **核心成果**：
  1. **层级化回复模型与嵌套评论混杂隔离治理**：
     - 后端 `EduReply` 实体扩展 `@TableField(exist = false)` 属性，支持 `answerId`、`targetReplyId`、`targetUserId`、`anonymity` 等前端表单参数；
     - 重构 `EducationService.saveReply`：建立规范的父子关联树。顶级回答严格挂载为 `parentId = 0`，楼中楼回复根据 `targetReplyId` 或 `answerId` 精确关联父级评论，杜绝脏层级；
     - 重构 `EducationService.replyPage`：当未传 `answerId`（或为 0）时，严格限定仅查询一级顶级回答（`parentId IS NULL OR parentId = 0`），彻底根除顶级回答列表中将楼中楼子评论当成独立主回答混合展示的严重架构缺陷；当传入 `answerId` 时，精准下钻查询指定回答下的子回复。
  2. **同实例跨库秒级穿透与真实用户身份/点赞状态透传**：
     - 充分利用 MySQL 单实例架构，在 `EducationService` 中引入轻量级 `JdbcTemplate` 直接跨库穿透关联 `share.sys_user`，实现 <1ms 极速查询提问者、回答者与回复目标的真实昵称（`nick_name`）、用户名与头像；
     - 重构 `questionView` 与 `replyView`：完整计算与透传 `description`、`sectionId`、`answerTimes`、真实头像与提问者/回答者身份，以及当前登录用户在该问题/回答下的实时独立点赞状态（`liked: true/false`）；
     - 修复 `questionPage` 中分类过滤条件将主键 `id` 错误比对 `sectionId` 的严重代码笔误（修正为匹配 `category`）。
  3. **互动点赞底层服务对问答与回答模型全面兼容**：
     - 修复 `EducationService.like` 方法仅校验 `EduQuestion` 导致给任何回答或子评论点赞时必抛 500 异常「问题不存在: ...」的严重缺陷；
     - 优先判定并递增/递减 `EduReply` 的点赞量与 Redis 集合，无法命中时再校验 `EduQuestion`，使问答区所有层级的点赞功能 100% 顺畅工作。
  4. **前端提问详情、课程问答与游客体验深度重构**：
     - 提问页 (`ask/index.vue`)：修复原分类标签笔误（“笔记归属于分项于分类”），规范化提交后携带返回路由参数跳转结果页；
     - 成功结果页 (`result/success/index.vue`)：支持动态文案与路由参数（`btnText`、`to`），确保问答发布后平滑返回所属问答或课程页；
     - 问答详情页 (`ask/askDetails.vue`)：登录用户兜底头像优化、重构顶级回答与楼中楼回复参数隔离（提交顶级回答时重置临时 `answerId`）、空内容警告规范为 `ElMessage.warning`、增加无回答清爽 Empty 状态；
     - 课程学习问答组件 (`ClassAsk.vue`)：解绑未登录游客强制鉴权，允许游客直接查阅课程问答与对应章节讨论，仅在发起提问与查阅「我的问答」时弹出温和的登录引导；补全列表 `:key` 绑定与空状态展示。
  5. **生产热更新部署与真实鉴权定向验证**：
     - 本地完成 `share-education.jar` 与 `portal-ui` 静态打包并热更新至云端生产环境；
     - 云端通过 Python 真实请求鉴权接口验证：用户登录获取 Token、详情接口 200 OK（`description`、`userName`、`answerTimes`、`latestReplyContent` 正确透传）、二级回答列表隔离无杂质（2条顶级回答 + 子评论数精准统计）、点赞接口对问题与特定回答双向测试 200 OK 且无任何 500 异常。

### 2026-09-13 13:30:00 - AI全真模拟面试评分与答题防作弊防御、音视频Web Speech双向交互、考试详情真假值逻辑缺陷修复与结算优惠券算法重构上线

* **核心成果**：

  1. **AI 模拟面试空卷 0 分防御与实质答题率动态加权**：
     - 后端 `InterviewServiceImpl.finishSession`：重构得分计算逻辑，彻底根除未作答即交卷却默认保底 75 分并误发阿里 P6 录用判定（Hire）的严重缺陷；
     - 引入实质有效答题统计：若 `answeredCount == 0`，总分严格锁定为 0 分；若候选人中途提前交卷，按照完成度比例（$\frac{\text{answeredCount}}{\text{plannedTurns}}$）折算总分；
     - 终局委员会诊断报告加入零分/未作答防御：裁决严格为 `Reject`，职级为 `未达标 (本场面试未完成实质作答)`，六维雷达统一下调至 20 分基准，并给出严肃的警告与重试建议；
     - 细化职级阶梯映射：$< 50$ 分统一对标 `未达标 (建议系统性补强基础)`。
  2. **AI 模拟面试全真 Web Speech 语音双向交互**：
     - 面试考场 (`interview/room.vue`)：集成原生 Web Speech API，实现考官问题一键朗读（`speechSynthesis` TTS），具备播放/暂停/停止切换与波形动效反馈；
     - 集成麦克风语音转文字（`SpeechRecognition` STT），候选人可直接通过语音录入作答，实时流式转录至答题输入框；
     - 增加 `Ctrl + Enter` 极速提交答题快捷键支持；
     - 强化交卷前置防御：若检测到候选人全场未答任何题目，点击交卷时弹出高风险警示确认框（警告 0 分与淘汰风险），防止用户误触交卷。
  3. **终局能力诊断报告无缝联动商城搜索**：
     - 面试报告 (`interview/report.vue`)：修复原推荐课程跳转无响应的缺陷，自动剥离《》书名号并无缝路由至 `/search/index?keyword=...` 课程检索列表，形成「面试评测 ➔ 弱项定位 ➔ 对应课程补强」的商业学习闭环。
  4. **学员考试详情页面致命真假值逻辑缺陷根除**：
     - 个人中心考试详情 (`personal/myExamDetails.vue`)：修复判断题解析逻辑 `val ? '正确' : '错误'` 的致命 JavaScript 隐式转换 Bug（由于 `'B'` 为非空字符串判定为 Truthy，导致所有选「错误」或正确答案为「错误」的判断题全部被错误渲染为「正确」）；
     - 答题卡渲染优化：修复未作答题目被标红的缺陷，增加 `item.answer != null && item.answer !== ''` 判断，未作答题目精准保持中性灰色；
     - 工具类重构 (`utils/tool.js`)：`upperAlpha` 函数由硬编码 5 个字母重构为支持 26 个大写英文字母转换（`String.fromCharCode`），并兼容 0-based 与 1-based 索引；
     - 考试记录入口联动 (`personal/components/ExamTable.vue`)：优化「查看」按钮为基于命名的路由跳转 `{ name: 'myExamDetails', query: { id: scope.row.id } }`。
  5. **购物车与结算中心优惠券算法与全选交互重构**：
     - 结算中心 (`pay/settlement.vue`)：彻底修复折扣券除以 10 导致 8 折变成 0.8 折进而误扣 92% 金额的严重计算 Bug；规范化 `percent`、`fixed`（满减）、`direct`（立减）三种券型的展示文案（如「8折」「满¥200减¥30」「立减¥50」）与实际减免金额计算，严格执行封顶限额与商品总价上限；
     - 购物车页面 (`pay/carts.vue`)：修复全选多选框 `isAllChecked` 的 setter 逻辑，使其双向绑定生效，支持一键全选与一键反选全部课程。
  6. **云端生产编译热部署与真实接口验证全通**：
     - 本地 JDK 17 高速编译 `share-customer.jar`，Vite 编译生产纯净 `portal` 静态产物并上传云端 ECS，无缝平滑热更新对应容器；
     - 通过 Python 真实接口测试：学生账号发起模拟面试并立即交卷，服务端精准返回 `Score: 0`、`OfferDecision: Reject` 与 `未达标` 报告，断言 100% 通过。


* **核心成果**：
  1. **视频学习进度心跳上报与断点续播机制落地**：
     - 用户端学习页 (`learning/index.vue`)：修复 `<video>` 播放器缺乏进度持久化的问题，绑定 `@timeupdate`（15s 节流上报）、`@pause`、`@ended` 事件，实时调用 `/ls/learning-records` 保存当前小节进度；
     - 实现断点续学能力：当视频元数据就绪后调用 `getLearningLog`，自动跳转至上次学习的时间戳（秒级恢复），并动态刷新课程总进度百分比。
  2. **后端教育服务课表去重与课程总进度智能聚合**：
     - 重构 `EducationService.learningPage`：针对同一课程存在概要记录（`catalog_id = null`）与多个小节学习记录的数据库设计，按 `courseId` 分组去重合并，提取最新学习时间与综合进度，彻底根除「我的课程」中相同课程出现重复卡片的严重显示缺陷；
     - 重构 `EducationService.saveLearning`：保存或更新小节学习记录时，自动统计当前学员在该课程下已学完课时数（进度 $\ge 90\%$ 或已完成），动态更新课程维度的汇总记录，保证整体进度数据严格一致。
  3. **个人中心订单「评价课程」交互全流程闭环**：
     - `personal/myOrder.vue`：修复原「评价课程」按钮未挂载点击事件且判断条件失效的问题，补全已支付/已完成订单的状态计算与防重评价机制；
     - 新增精致的课程评价弹窗组件：集成课程基本信息展示、5星好评打分、技术标签多选、学习心得体会输入，并联动积分奖励体系（完成评价即时提示「+5 学习积分已到账」）。
  4. **全站路由元数据、生产静态资源与异常文案深度净化**：
     - 路由修正：`base.js` 中将课程详情路由 `/details` 的 `meta.title` 从「问题详情」修正为「课程详情」，学霸天梯榜 `myIntegralRanking` 的当前面包屑从「优惠券说明」修正为「学霸天梯榜」；
     - 生产静态资源安全：全面消除 `/src/assets/...` 未编译的原始路径引用，改用 ESM 静态模块导入（`anonymity.png`、`vue3.svg`、`springboot.svg` 等），彻底杜绝生产环境 404 裂图；
     - 防御性空指针保护：业务管理端 `detailBaseInfo.vue` 增加退款状态越界防护，彻底消除 `undefined.msg` 导致的 Vue 渲染运行时崩溃；
     - 异常文案精准校正：对 `myCoupon`、`myCouponExplain`、`myOrderDetails`、`myExam`、`myExamDetails`、`myClass` 中全盘复制粘贴的「订单列表请求失败！」「最近学习数据请求出错！」进行精准重写与场景化匹配。

### 2026-09-12 07:05:00 - 用户端首页继续学习横幅治理：彻底剔除公共缓存污染、对接真实微服务课表、无记录严格置空与健壮性容错

* **核心成果**：
  1. **定位并根除假数据与跨用户缓存污染**：排查发现首页 SWR 缓存机制此前将属于私密用户态的 `recentLearning` 一并写入了 `localStorage` 的公共缓存键 `tianji_portal_home_cache_v2` 中。当新用户登录或换账号时，首屏直接读取了上一位用户的本地学习残留（如课程「数据可视化」），且 `getMylessons()` 接口在面对新用户返回空数组时遗漏了 `else` 清空逻辑，导致虚假数据永远无法被重置；
  2. **公共/私有缓存边界解耦与自动净化**：
     - `recentLearning` 彻底从公共 `HOME_CACHE_KEY` 中剥离，状态初始严格为 `null`；
     - 增加主动缓存净化守卫：在 `readHomeCache` 与登出 `handleLogout` 时，主动探测并抹除 `localStorage` 中可能遗留的历史 `recentLearning` 脏字段；
  3. **基于微服务真实学习记录精准渲染与动态响应**：
     - `loadRecentLearning` 严格对接真实课表接口 `getMylessons({ pageNo: 1, pageSize: 5 })`；
     - 有学习记录时：精准映射后端 `completedLessons`、`totalLessons` 与 `progressPercent`，修复原本进度计算错误回退为 `0/10 节 0%` 的缺陷；
     - 无学习记录或新用户时：严格置空 `recentLearning.value = null`，首页快捷卡片完全隐匿，杜绝一切虚假提示；
     - 挂载 `user-profile-updated` 与 `cart-updated` 全局事件监听并在组件卸载时及时解绑，实现跨组件状态平滑同步；
  4. **云端纯净构建与即时生效**：本地完成无报错编译，同步静态包至 `tianji-portal-ui` 容器并热重载 Nginx，新用户首页恢复清爽干净状态。

### 2026-09-12 06:55:00 - 用户端登录模块短信登录全面改造为 QQ 邮箱验证码登录：前后端直连腾讯 SMTP、免密自动注册与防重放全链路闭环

* **核心成果**：
  1. **前端用户端登录重构升级**：
     - 登录页面 (`pages/login/index.vue`)：登录 Tab 标签由原先的「短信登录」修改为「邮箱验证码登录」，支持 `act === 'email' || act === 'phone'` 状态自适应；
     - 邮箱登录子组件 (`components/LoginPhone.vue`)：彻底移除旧版手机号校验与输入，替换为 QQ 邮箱输入框（支持 `@qq.com`、`@vip.qq.com`、`@foxmail.com` 正则验证）与 6 位验证码输入框，集成 60s 发送倒计时与防抖防重复触发机制；
     - 接口封装与兼容 (`api/user.js`)：新增 `emailLogin` 接口（透传 `type: "email"`），并对旧版 `phoneLogins` 增加邮箱透明兼容路由。
  2. **后端 `share-auth` 鉴权中心全新邮箱验证码登录流**：
     - 请求体 `LoginBody.java`：扩充 `email`、`code`、`type` 字段；
     - `QQMailService.java`：邮件标题通用化为 `【智问学伴】安全身份验证码`，正文模板升级为通用于登录和注册场景；
     - `SysLoginService.java`：实现 `loginByEmailCode(email, code)`，校验 Redis 中 `zhiwen:auth:emailcode:{email}` 并在校验成功后立即原子删除验证码（防重放攻击）；
     - **新用户自动免密注册闭环**：若检测到该邮箱尚未注册，系统自动调用远程用户服务完成学员账号（`user_type: "01"`，默认昵称 `QQ用户_{prefix}`）的无感创建与初始化，并直接签发 JWT 访问令牌，打造极佳用户体验；
     - `TokenController.java`：重构 `/accounts/login` 路由分发逻辑，对 `type === "email"` 或包含 `@` 符号的登录请求精准派发至邮箱验证码登录处理器。
  3. **云端生产部署与实测全闭环**：
     - 本地 JDK 17 打包最新 `share-auth.jar` 并同步部署替换云端 `tianji-auth` 容器，Tomcat 9200 启动成功；
     - 本地 Vite 构建零报错纯净生产包，替换 `tianji-portal-ui` 容器静态资源并热重载 Nginx；
     - 定向接口实测：发送验证码成功收到邮件并落库 Redis，使用验证码成功登录取得 `access_token`，且复用同一验证码即时被安全拒绝，网关携带 Token 请求 `/us/users/me` 100% 成功返回完整学员画像。

### 2026-09-12 06:45:00 - 用户端导航栏购物车角标假数据彻底修复：剔除静态硬编码、对接真实购物车接口与全站响应式事件同步

* **核心成果**：
  1. **定位并根除假数据硬编码**：排查发现顶部导航栏 [`Header.vue`](file:///d:/education%20system/my-porject/share-parent/frontends/portal/src/components/Header.vue) 中购物车角标变量被硬编码为 `const cartCount = ref(2);`，且没有与真实购物车接口（`/ts/carts`）做任何数据拉取联动，导致无论购物车为空还是未登录均强行显示红点「2」；
  2. **对接真实购物车接口**：重构为 `ref(0)`，接入 `getCarts()` 接口，在用户登录后自动拉取真实课程项条数，未登录或购物车为空时严格为 0 并自动隐藏红点；
  3. **建立跨页面响应式联动**：在详情页加入购物车（`classDetails`）、购物车列表删除/清空课程（`carts.vue`）以及退出登录时，全面通过自定义事件 `cart-updated` 实时同步导航栏徽标数字，保持数据精准一致；
  4. **云端热重载发布**：本地完成无报错编译，排空生产容器注入最新代码并热重载 Nginx，彻底消除虚假数字显示。

### 2026-09-12 06:30:00 - 用户端注册全面升级为 QQ 邮箱注册：集成 Spring Mail SMTP SSL (465) 真实直发、账号与邮箱统一双向认证及全链路闭环部署上线

* **核心成果**：
  1. **前端注册与登录全面切换至 QQ 邮箱**：
     - 用户端注册组件 (`Register.vue`)：由旧版手机号短信注册全面重构为 QQ 邮箱专属注册表单，支持 `@qq.com` 格式实时校验、6 位数字验证码获取、60s 发送倒计时与防重锁；
     - 用户端密码登录组件 (`LoginPass.vue`)：占位提示与规则升级为「请输入用户名或QQ邮箱」，支持用户直接使用注册邮箱作为账号登录；
     - 本地 Vite 构建零报错生成纯净生产 `dist` 产物，彻底排空 `tianji-portal-ui` 历史残包完成部署并热重载 Nginx。
  2. **后端引入原生 JavaMailSender + SSL 465 真实直发 QQ 邮箱**：
     - `share-auth` 引入 `spring-boot-starter-mail` 依赖，开发专属 `QQMailService` 服务；
     - 采用原生 Jakarta Mail 规范，直连腾讯官方 `smtp.qq.com:465`（强制开启 SSL/TLS），配置认证授权码并内置优雅精致的 HTML 验证码邮件模板与别名容错重试机制；
     - 验证码生成采用 `SecureRandom` 强随机数，存入 Redis（Key 前缀 `zhiwen:auth:emailcode:{email}`，TTL 300秒），校验通过后原子销毁，严格防止重放。
  3. **账号与邮箱双向打通与若依底层兼容**：
     - `UserConstants.USERNAME_MAX_LENGTH` 从 20 扩展至 50，完美支持长邮箱作为 `user_name` 登录；
     - `SysUserMapper.xml`：重构 `selectUserByUserName`、`checkUserNameUnique`、`checkEmailUnique`，支持 `user_name = #{userName} OR email = #{userName}` 统一匹配，并添加 `limit 1` 防止大表扫描；
     - 注册流程自动补齐 `user_type: "01"`（学员）与默认昵称（`QQ用户_{prefix}`）。
  4. **云端生产部署与真实全链路定向验收通过**：
     - Nacos 动态配置更新 `share-auth-dev.yml`（配置 `qq.mail.*` 连接参数与授权凭证）；
     - 串行无缝更新 `share-auth.jar` 与 `share-system.jar` 容器运行时，Tomcat 及 Sentinel 均 100% 正常就绪；
     - 定向接口验收：实测调用 `/as/code/verifycode?email=a2416363666@qq.com`，腾讯 SMTP 服务器即时响应并投递验证码邮件；调用 `/as/users/register` 完成用户 `a2416363666@qq.com` 注册落库（User ID 6001）；调用 `/as/accounts/login` 凭 QQ 邮箱与密码成功取得 JWT 鉴权令牌。

### 2026-09-12 06:15:00 - 全平台前端模型信息脱敏与安全治理：彻底清除模型型号（GPT-5.6-Luna）与第三方代理接口暴露，三端重构构建与云端容器无死角净化上线

* **核心成果**：
  1. **全链路拉网式脱敏排查与根因定位**：
     - 用户端（`frontends/portal`）：
       - 首页实时多智能体推理流 (`AgentReasoningHUD.vue`)：推演底座标签从 `GPT-5.6-Luna · 行业胜任力图谱` 脱敏为 `自研深度学习引擎 · 行业胜任力图谱`；Agent 5 推演弹窗中 `GPT-5.6-Luna` 描述全面替换为 `智能认知推理引擎`、`教育专属大语言模型`；
       - 模拟面试专区 (`interview/index.vue`)：顶栏标签由 `基于 10,000 大厂真题库与 GPT-5.6-Luna 深度驱动` 升级为 `基于 10,000+ 大厂真题库与智能大模型算法深度驱动`；
       - 客服 API 客户端 (`pixelApi.js` & `customerService/index.vue`)：清空默认模型名称，全面清理报错信息中的第三方提供商域名（`api.ai-pixel.online`）与 `OpenAI Key` 等外露敏感词汇，替换为平台专属的规范化错误提示。
     - 业务管理端 (`frontends/business-admin`)：
       - 客服配置页 (`customer-service/index.vue`)：将「第三方 Pixel AI」文案脱敏为「AI 智能客服引擎」；将输入框 placeholder 及默认 reactive 表单中硬编码的 `https://ai-pixel.online` 与 `gpt-5.6-luna` 彻底清除（改为占位提示与留空继承系统配置）。
     - 若依运营中台 (`share-ui`)：
       - 客服管理页 (`views/customer/management/index.vue`)：全面清理「使用第三方 Pixel API」说明、硬编码接口地址与模型名称，实现端到端脱敏。
  2. **三端前端本地生产环境构建与历史哈希残包无死角净化**：
     - 本地完成 `frontends/portal`、`frontends/business-admin`、`share-ui` 生产级打包编译，生成干净无任何模型泄露的 `dist` 产物；
     - 深度排查发现云端 Nginx 容器内历史部署遗留了上千个旧 Hash 代码包（如 `index-mAlp4y8x.js`），存在潜在的旧代码残存泄漏风险；
     - 采用容器内部沙箱彻底清理机制（`rm -rf /usr/share/nginx/html/*`）彻底排空 `tianji-portal-ui`、`tianji-business-admin-ui`、`tianji-ruoyi-ui` 三大容器并灌入最新编译产物，热重载 Nginx。
  3. **三容器云端全量断言验证通过**：
     - 分别对三个前端容器的 `/usr/share/nginx/html/` 执行不区分大小写的 `luna|gpt-5|ai-pixel` 全局正则检索，断言结果均为 `100% CLEAN`，达成零敏感词暴露。

### 2026-09-12 05:58:00 - 用户端首页加载性能与图片渲染彻底优化：SWR 客户端预热 + 请求拓扑解耦 + Nginx 防旧包缓存 + 0ms 冷启动保底呈现

* **核心成果**：
  1. **定位首页图片与内容延迟加载根因**：
     - **全链路串联阻塞**：原有代码将 7 个接口集中在 `Promise.allSettled` 中，导致哪怕只要有一个接口稍有延迟，首页所有课程卡片 DOM 节点均无法挂载，图片请求被整体延后数秒；
     - **无效全量大表扫描**：发现页面为了计算分类课程数，发起 `classSeach({ pageNo: 1, pageSize: 200 })` 拉取 200 门全量课程记录，增加无谓的巨大网络往返传输；
     - **首屏图片被错误打上懒加载**：首屏高优先级卡片带有 `loading="lazy"`，浏览器等待布局重绘后才发起图片加载；
     - **SPA 单页缓存旧包驻留**：原 Nginx 未给 `index.html` 声明 `no-cache`，导致浏览器缓存了 HTML 入口，且单页应用登录跳转使用内存 `router.push`，未重新加载最新 JS 脚本。
  2. **内置 0ms 极速冷启动基准数据 (Cold-Start Preheating)**：
     - 在前端内置热门骨干课程与分类预置数据（《Vue3 从入门到精通》、《Java SpringBoot 实战》、《机器学习入门》、《MySQL 数据库优化》），即使首次打开或清空缓存，也能在 **0ms 瞬间直出完整封面与卡片**，彻底消除白屏与图片闪跳。
  3. **前端架构 SWR (Stale-While-Revalidate) 本地预热治理**：
     - 引入专属缓存机制 `tianji_portal_home_cache_v2`，页面初始化阶段直接从 `localStorage` 同步读入分类、推荐好课、个性化推荐、热门榜、最新榜、点赞排行及最近学习记录，**达成 0ms 秒级首屏直出渲染与骨架呈现**；
     - 数据变更通过 300ms 防抖自动写回本地缓存，下一次进入即时秒开；
  4. **请求拓扑解耦与独立流式响应**：
     - 将原本阻塞的 `Promise.allSettled` 重构为各业务域独立非阻塞 Promise 异步流（分类 ~20ms、重磅推荐 ~15ms、热门好课 ~15ms、最新上线 ~15ms、点赞排行榜 ~10ms）；
     - 彻底剔除 200 门课程的冗余大查询，分类数量直接依托已聚合的 `courseCount` 字段；
     - 个性化推荐与 AI 多智能体流作为独立背景流进行无感增量覆写；若本地暂无推荐，立即采用重磅推荐进行秒级平滑降级兜底；
  5. **Nginx 缓存治理与运行时刷新**：
     - `frontends/nginx.conf` 与 `index.html` 中严密加入 `Cache-Control: no-cache, no-store, must-revalidate`、`Pragma: no-cache`、`Expires: 0`，确保入口 HTML 永远最新；
     - 登录（`LoginPhone.vue`/`LoginPass.vue`）与登出（`Header.vue`）使用 `window.location` 强制刷新，保障切换账号或重新登录后加载最新运行时上下文；
  6. **定向验证与平滑发布**：
     - 本地完成编译打包 `frontends/portal`（无警告报错）；
     - 静态产物与 Nginx 规则同步部署至云端 `tianji-portal-ui` 容器并平滑热重载 Nginx；定向接口验证全链路 200 OK，响应耗时均在 10ms~200ms 内。

### 2026-09-11 20:00:00 - 媒资管理与课程小节双向深度绑定升级：数据库表级一致性对齐 + 级联选择器联动 + 云端热更新上线

* **核心成果**：
  1. **跨库数据表深度对齐与基准数据治理**：
     - 排查并确定媒资表 `tj_file.file_media` 与课程大纲表 `tj_education.edu_course_catalog` 的物理拓扑；
     - 在 `tj_file.file_media` 表新增 `course_id`（课程ID）、`course_name`（课程名称）、`section_id`（小节ID）、`section_name`（小节名称）四项业务字段并创建复合索引 `idx_file_media_course_section`；
     - 沉淀增量 SQL 脚本 `sql/migrations/V32__file_media_course_binding.sql`，同步更新初始化全量基准 `sql/zhiwen-file.sql`；
     - 线上执行数据回填与断言验证，将历史 12 条存量视频与课程大纲小节完成 100% 互相对齐（`status='used'`）。
  2. **跨微服务接口协同与原子级联动保障**：
     - 文件微服务 (`share-file`)：
       - 实体 `FileMedia.java` 拓展字段映射；
       - `FileMediaService.java` 重构保存（`save`）、分页列表（`page`/`pageView`）与详情输出（`view`），支持接收课程与小节信息、按 `courseId` 精准过滤及自动设置 `used` 状态；
     - 教育微服务 (`share-education`)：
       - `EducationService.java` 新增 `bindCatalogMedia` 与 `unbindCatalogMedia` 核心业务逻辑，更新 `media_id`、`media_name` 与 `duration_seconds`；
       - `EducationPortalController.java` 暴露 `POST /courses/media/bind` 与 `POST /courses/media/unbind` 接口；
     - 针对双微服务进行 Java 17 独立本地编译，热更新云端 `tianji-file` 与 `tianji-education` 容器。
  3. **业务管理端 (`business-admin`) UI 与交互全流程重构**：
     - `curriculum.js` 扩展 `getSimpleCourses`（320门课程列表）、`bindCourseMedia` 与 `unbindCourseMedia` API 调用；
     - 媒资列表 (`media/index.vue`)：
       - 搜索栏新增「所属课程」下拉筛选框（支持 320 门课程模糊搜索与快速过滤）；
       - 数据表格新增「关联课程」与「对应小节」列，展示专属彩色徽章标签；
       - 上传视频弹窗：重构为课程视频关联弹窗，加入「所属课程」选择器与「对应小节（第几节）」级联选择器（按章节分组展示，动态标注小节是否已绑定视频或可绑定），并在上传保存成功后自动触发大纲小节绑定；
       - 编辑弹窗：支持查看与动态改绑所属课程和小节，改绑时自动触发原小节解绑与新小节绑定；
       - 删除视频：若已绑定小节，删除前自动完成关联小节解绑，确保双向数据绝对一致。
  4. **全链路自动化断言与定向验证**：
     - 本地执行 `npm run build` 产出静态产物，打包同步至云端 `tianji-business-admin-ui` 容器并重载 Nginx；
     - 执行 `.scratch/verify_media_course_binding.py` 对登录、课程列表获取、大纲解析、媒资上传保存、大纲小节双向绑定、`courseId` 精准过滤、解绑与删除清理全流程进行自动化断言，验证通过率 100%。

### 2026-09-11 19:00:00 - 全平台课程简介 HTML 标签彻底排查与清洗：全库 320 门课程纯文本治理 + 前后端立体过滤与结构化呈现升级

* **核心成果**：
  1. **全库 320 门课程数据彻底清洗与断言校验**：
     - 排查发现 `tj_education.edu_course` 中历史批量插入的 300 门 IT 课程（ID 21 至 320）的 `description` 字段被包裹了 `<h3>` 和 `<p>` 标签；
     - 线上执行 SQL 原地数据清洗，将 300 门富文本课程的 `description` 重置为纯净摘要文本（与 `short_description` 对齐），并将 ID 1~20 的 NULL 简介补齐；
     - 线上执行聚合查询断言：`total_courses=320, desc_has_html=0, desc_is_null=0`，全库 100% 达成无标签纯净数据。
  2. **数据库基准与增量迁移落地**：
     - 新增 `V31__clean_course_descriptions.sql` 增量迁移脚本，记录本次清洗变更；
     - 同步修正 `V22__add_300_real_world_it_courses.sql` 初始插入脚本中的 300 门课程描述，确保未来从零初始化数据时原生纯净。
  3. **后端防御性过滤与适学人群增强**：
     - `EducationService.java` 新增 `cleanHtmlTags(String text)` 静态过滤方法；
     - 在 `courseView` 统一输出层对 `description` 与 `shortDescription` 做强制过滤保障；
     - 在 `legacyCourse` 中对 `detail` 和 `introduce` 做强制过滤，并将 `course.getPrerequisites()` 映射至 `usePeople`，补全管理端与前端旧接口的适学人群字段；
     - 在 `saveLegacyCourse` 保存入口处增加自动清洗过滤，杜绝后续通过管理端录入带标签脏数据。
  4. **前端防御过滤与智能结构化呈现升级**：
     - 学生端详情页 `classDetails/index.vue`：
       - 头部简介与“课程介绍”Tab 增加前端 `cleanHtml` 过滤，消除标签暴露；
       - 重构“课程介绍”Tab：简介段落清爽展示，不再出现“课程简介”重复双重标题；
       - “适合人群”与“学习目标”动态读取课程实体的 `prerequisites`、`targetRole` 与 `skills`，呈现专业化岗位与技术栈要求；
     - 学生端学习页 `learning/index.vue`：
       - 增加 `cleanHtml` 过滤，移除冗余重复的“课程说明”双重段落，以模块化清晰展示技术栈与适合人群；
     - 课程组件 `ClassAbout.vue` 与业务管理端 `CourseAbout.vue`：
       - 全面增加 `cleanHtml` 过滤，确保全平台所有端绝对不展示任何 HTML 原始标签。
  5. **定向回归测试与生产部署**：
     - 本地通过 Java 17 离线打包 `share-education.jar` 并同步至云端热更新 `tianji-education` 容器；
     - 本地执行 `npm run build` 产出 `portal` 与 `business-admin` 静态产物，分别热更新 `tianji-portal-ui` 与 `tianji-business-admin-ui` 容器并重载 Nginx；
     - 编写并运行定向测试脚本 `.scratch/verify_course_descriptions.py`，针对网关课程接口（ID 109、21、1、320）及前端静态资源进行自动化断言，100% 通过验证。

### 2026-09-11 15:30:00 - 简历工程能力量化评分机制深度重构：60分及格基准分 + 五维工程能力细则面板与大模型规则双引擎升级

* **核心成果**：
  1. **评分底线与分值模型重构**：
     - 将过往较高的预设底分（82分）彻底重构为**60 分准入门槛基准分**，对标大厂简历筛选准入基准；
     - 扩展并落地五维工程能力量化加成模型（总分 60~100 分）：
       - 维度一：核心技术栈广度与深度（0-10分，覆盖语言、微服务、中间件、持久化与分布式架构）；
       - 维度二：重点项目经历与系统复杂度（0-10分，考查项目数量、业务体量与分布式深度）；
       - 维度三：STAR 法则与量化业务成效（0-10分，深度识别 QPS 吞吐、耗时压降、SLA 可用性等量化指标）；
       - 维度四：高可用工程与容灾逃生规范（0-5分，重点审视限流熔断、链路监控与测试规范）；
       - 维度五：目标岗位与名企契合度（0-5分，智能对标当前投递目标岗位及一线大厂要求）；
  2. **后端 DTO 结构升级与双引擎算法对齐**：
     - `ResumeAnalysisVO` 扩充 `scoreDetails` 属性及 `ScoreDimensionItem(name, score, maxScore, rating, description)` 结构；
     - `UserResumeServiceImpl` 在大模型 Prompt 中确立 60 底分五维打分指令，并升级启发式本地兜底引擎 `applyHeuristicAnalysis`；在 `toVO` 转换层实现历史存量简历的五维动态计算兼容；
  3. **前端「大厂招聘委员会·五维工程能力量化细则」大屏上线**：
     - `myResume.vue` 在职涯诊断大屏中新增专属细则面板，结合动态色阶（绿/蓝/黄）与进度条直观呈现各项得分、满分占比与专家评语；并在综合匹配度处标注“基准及格分 60 分 + 五维工程能力实战加成”；
  4. **云端生产部署与定向回归测试**：
     - 串行在本地生成静态 `dist/` 与后端 JAR 并同步热更新云端 `tianji-portal-ui` 与 `tianji-customer` 容器；
     - 编写并在本地运行 `.scratch/test_resume_scoring.py` 进行定向范围校验，实测真实大厂简历诊断与已有简历查询，60 分基准、各项维度满分累计 100 分、单项描述与分值断言 100% 通过。

### 2026-09-11 14:35:00 - 个人中心「我的简历与 AI 深度诊断」研发上线、31岗位大厂对标与 AI 模拟面试全真数据联通闭环

* **核心成果**：
  1. **数据模型设计与简历持久化体系建立**：
     - 在 `tj_customer` 库创建 `cs_user_resume` 用户简历表，涵盖简历原始内容、对标岗位、目标公司、匹配分数、职级评级、技术栈标签、项目亮点、薄弱项风险点、预测必考题与 STAR 重构建议；
     - 扩展 `cs_interview_session` 表新增 `resume_id` 与 `resume_summary` 快照字段，实现每场模拟面试与特定简历版本的一对一溯源。
  2. **后端简历解析、AI 深度对标与智能诊断双引擎**：
     - `UserResumeServiceImpl` 支持纯文本直接录入、txt/md 客户端直读、以及服务端附件文本解析；
     - 深度对标 31 个精选技术岗位与大厂标准，结合大模型提示词工程与健壮的启发式规则降级分析，一键输出六维全真评估（技能匹配度、职级梯队、亮点提炼、薄弱风险、大厂面试官预测题、STAR 优化示范）。
  3. **简历与全真 AI 模拟面试系统全链路深度互联**：
     - 面试大厅（`/interview`）启动前自动嗅探当前用户已建档简历并呈现联动卡片，支持候选人自由切换“根据此简历定制考题”；
     - 面试开启时将简历核心画像深度注入上下文，第 1 轮破冰题直接锁定候选人真实业务项目与技术栈进行开题；后续多轮追问自适应结合简历声称的技术亮点与薄弱点进行剥洋葱连环攻防；终局报告中综合评判答题表现与简历声称的契合度。
  4. **学生端个人中心 UI 页面研发与多端联动**：
     - 个人中心左侧导航栏无缝挂载「📄 我的简历与 AI 深度诊断」（`/personal/main/myResume`）；
     - 提供大厂标准范例一键填入、多格式上传、31 岗位分赛道级联选择、动态评分徽章、技术栈标签展示、大厂预测考题高亮与 STAR 法则重塑建议展示；
     - 页面底部提供一键直通 AI 模拟面试专属通道，参数自动化透传至考场大厅。
  5. **云端生产热部署与端到端闭环验证**：
     - 定向打包并安全更新 `tianji-customer` 与 `tianji-portal-ui` 容器，排查并规范容器内 `/app/app.jar` 运行路径；
     - 通过 Gateway 接口定向验证简历存取、AI 对标、第一轮自适应个性化出题及 MySQL 落库，100% 验证通过。

### 2026-09-11 13:50:00 - 全真沉浸式 AI 模拟面试与职涯评测超级子系统 (Zhiwen AI Interview Pro) 研发上线与全链路闭环验证

* **核心成果**：
  1. **高阶业务架构设计与微服务复用架构落地**：
     - 基于已有 `share-customer` (tj_customer) 复用 FastEmbed、Qdrant 向量检索底座与 GPT-5.6-Luna 客户端，避免新增容器带来额外 RAM 消耗；
     - 研发出全真互联网大厂模拟面试矩阵，支持阿里 P7 资深架构师、字节跳动技术专家、资深大厂 HRBP 与大厂评审委员会 4 大专业考官风格；
  2. **数据库持久化与 Flyway V30 迁移落地**：
     - 设计并应用 Flyway 迁移脚本 `V30__create_ai_interview_module_tables.sql`，建立 4 张核心业务表：
       - `cs_interview_session` (场次与大厂目标表)
       - `cs_interview_turn` (问答轮次与三级深度追问表)
       - `cs_interview_code` (算法代码手撕与沙箱评测表)
       - `cs_interview_report` (终局六维能力雷达与职级诊断大屏表)；
  3. **3-Level "剥洋葱" 连环深度追问与 Qdrant 影子检定核心引擎**：
     - 实现 Level 1 概念摸底 ➔ Level 2 底层原理深挖 ➔ Level 3 线上极限排障三级追问递进机制；
     - 问答提交时实时并发调用 Qdrant 10,000 真题向量库进行影子语义检定，毫秒级召回标杆参考答案并比对答题亮点与漏洞；
  4. **算法代码手撕沙箱与架构异味审计引擎**：
     - 在线实时推演时空复杂度（Time: O(N), Space: O(1)）、审计工程代码异味（空参防御、并发死锁风险、魔法值抽取），并自动生成阿里生产级重构示范代码；
  5. **终局职涯能力诊断报告与原生响应式 SVG 六维雷达图**：
     - 终局委员会多维裁决（Strong Hire / Hire / Weak Hire / Reject）、对标阿里 P6/P7 / 字节 2-1/2-2 职级；
     - 原生自绘响应式 SVG 六维胜任力雷达图（Java核心、架构设计、存储数据库、分布式高并发、算法工程、沟通表达）；
     - 答题话术 STAR 法则重塑对比（原版弱回答 ➔ 大厂 STAR 标准示范）与平台定制补强课程推荐；
  6. **前端三页联动与生产部署验证**：
     - 研发上线 `/interview/index` (模拟面试大厅)、`/interview/room/:id` (沉浸式考场 HUD 与双栏工作台)、`/interview/report/:id` (多维诊断大屏)；
     - 完成服务器生产容器重启与 Nginx 静态分发，定向范围接口与全链路真机验证 100% 通过。
  7. **31+ 大厂热门岗位矩阵（8大技术赛道）与动态自适应考查引擎升级**：
     - **8 大赛道 31 个精选企业级岗位**：涵盖后端与微服务架构、跨语言系统与高性能（Go/C++/Rust/Python）、Web前端与移动端、AI与大模型算法（LLM/RAG/Agent/NLP/CV/推荐）、大数据与流批计算（Flink/Spark/湖仓一体）、数据库与存储中间件、云原生与SRE、测试开发与网络安全；
     - **前端交互升级**：采用 `<el-option-group>` 赛道分组与 `filterable` 即时搜索，代码沙箱扩展支持 Java、Go、C++、Python、TypeScript/JS 与 Rust 6 大手撕语言与专属模板；
     - **后端自适应引擎架构**：实现 `detectJobTrack`、`resolveFirstDimension`、`resolveNextDimension` 与专属手撕题 `resolveCodingProblem` 调度，根据候选人选择岗位自适应决定各轮维度、出题 Prompt 与终局 STAR 职涯重塑建议，告别 JVM 题目硬编码；
     - **云端发布与定向范围验证**：轻量打包同步部署并热更新 `tianji-portal-ui` 与 `tianji-customer`，4 大跨赛道典型岗位真实连调 100% 满分通过。

### 2026-09-10 22:20:00 - 知识库规模化扩充至 10,000 条（JavaGuide 与牛客网双源清洗入库）、全局零重复排重与 Qdrant 512 维全量向量化落地

* **核心成果**：
  1. **JavaGuide 与牛客网专项双源知识库深度采集与清洗**：
     - 从 JavaGuide 体系全量提取 Java 基础/并发/JVM、Spring 微服务生态、MySQL/Redis、计算机网络、操作系统、分布式架构、高可用设计等 5,536 条深度问答，并补齐文档上下文消除碎片标题；
     - 从牛客网专项练习（tagId=21000）39 大技术专题中并发采集 2,094 道涵盖数据结构与算法、SQL实战、AI大模型、前端Web、Linux/Shell 的编程真题与详尽解析；
  2. **多级混合去重机制与全局 100% 唯一性保障**：
     - 结合标头归一化、全角半角符号/圈号数字消除、精确文本匹配及 2-gram Jaccard 相似度排重，累计剔除 631 条与已有题库或彼此重叠的重复题目；
     - MySQL 库级校验 `GROUP BY question HAVING count(*) > 1` 返回 0 条，达成万条真题 100% 绝对唯一；
  3. **数据持久化与数据库扩容 (V29 迁移)**：
     - 生成 Flyway 迁移脚本 `V29__expand_knowledge_base_to_10000.sql`（净增 7,630 条，IDs: 2391 ~ 10020），写入线上 `tj_customer.cs_knowledge`，全站知识库总数精确达到 **10,000 条**；
  4. **Qdrant 向量数据库 10,000 维向量全量索引与微服务秒级召回**：
     - 使用 FastEmbed `BAAI/bge-small-zh-v1.5` 在本地环境完成 7,630 条新知识的 512 维向量密集空间计算（用时 35.1s），分批上传至服务器一键写入 Qdrant `tianji_knowledge` 集合；
     - Qdrant `points_count` 扩容至 **10,000**（`status: green`）；实测 JVM 垃圾回收、Spring 三级缓存、快速排序、Redis 缓存穿透等专业技术检索，Top-1 语义匹配度达 0.75~0.87，平均召回延迟 < 15ms。

### 2026-09-09 01:55:00 - Qdrant 向量库全量 2370 题向量化、FastEmbed 语义检索微服务与客服智能问答闭环落地

* **核心成果**：
  1. **Qdrant 向量数据库部署与全量向量化**：
     - 在轻量 Rust 向量数据库 Qdrant 中创建 `tianji_knowledge` 集合（512 维向量，Cosine 余弦相似度）；
     - 使用轻量 ONNX 推理引擎 FastEmbed 加载中文向量模型 `BAAI/bge-small-zh-v1.5`，全量 2370 道题目与答案高性能向量化批量 upsert 入库（points_count: 2370, status: green）；
  2. **FastAPI 语义检索微服务容器化（tianji-embedding）**：
     - 构建轻量 Python 3.11 镜像并部署独立容器 `tianji-embedding`（常驻内存仅 172MB，端口 8000，宿主机 18000），挂载离线模型缓存实现零外网依赖启动；
     - 提供 `GET /health`、`POST /embed` 及 `GET /search?q={text}&limit={n}` 检索接口，向量检索延迟低至 15ms；
     - 将 `embedding` 服务正式纳入 `docker-compose.yml` 编排管理；
  3. **客服微服务（share-customer）全链路语义集成与防护**：
     - `CustomerService.java` 新增 `findSemanticAnswer`：优先通过语义检索服务进行向量相似度匹配（阈值 >= 0.70 判定命中），不满足则平滑降级至 FAQ 与关键词；
     - 解决真题详尽解析数千字引发的数据库截断异常：将 MySQL `cs_session.last_message` 字段类型由 `varchar(1000)` 平滑修改为 `TEXT`，并在 Java 层增加 `formatLastMessagePreview` 安全截断；
  4. **验证**：真实提问“Java为什么支持跨平台？”、“Redis缓存雪崩怎么解决？”秒级精准召回知识点，冒烟测试 62/62 项满分通过。

### 2026-09-09 01:25:00 - 小林coding后端面试题库全量导入（1322题）与本地严禁占用C盘铁律建立

* **核心成果**：
  1. **题库抓取清洗与入库 (V28 迁移)**：
     - 深度抓取小林coding全域 23 个技术专题（Java基础/并发/JVM、C++底层、Go语言、MySQL/Redis、计网、操作系统、算法、MQ、分布式系统设计等）；
     - 提炼 **1322 道**工业级面试真题与详尽解析，生成持久化脚本 `V28__import_xiaolincoding_interview_knowledge.sql` 导入线上 `tj_customer`，知识库总规模跃升至 **2370 条**；
  2. **本地磁盘保护铁律确立**：
     - 明确禁止使用本地 C: 盘，所有临时脚本与数据处理严格落地 D: 盘项目空间（`.scratch/`），彻底清理 C: 盘历史缓存并恢复 23.5GB+ 纯净空间。

### 2026-09-09 01:00:00 - 非业务辅助服务（gen/job/monitor）永久关停，系统释放 1.4GB 内存

* **核心成果**：
  1. **关停容器与彻底移除**：关停并永久下线 `tianji-gen`（若依代码生成，省 520MB）、`tianji-job`（Quartz调度脚手架，省 528MB）、`tianji-monitor`（Admin监控，省 322MB），累计为 ECS 服务器**释放 1.4GB 物理内存**与 210+ 系统线程；
  2. **编排永久禁用规范**：在 `docker-compose.yml` 中将上述三项服务打上 `profiles: ["dev-tool"]` 标签并设置 `restart: "no"`，禁止生产与日常运维默认启动，服务器空闲内存大幅提升至 2.1GiB+。

### 2026-09-09 00:35:00 - API 密钥 AES-256 加密存储、运行时自愈解密与 GPT-5.6-Luna 全栈模型收敛

* **核心成果**：
  1. **代码与版本库零明文密钥净化**：清理配置文件、Nacos 配置模版、代码类、Docker Compose 及历史工作日志中的全部明文 `sk-` 密钥，配置 `.gitignore` 阻断敏感凭据；
  2. **AES-256 密文存储与运行时自愈解密**：
     - 在 `share-common-core` 新增 `AesCryptoUtil.java`（AES/CBC/PKCS5Padding + SHA-256 密钥派生）；
     - 密钥密文存入 MySQL `tj_customer.cs_ai_config.api_key_ciphertext`，`CustomerService` 在 Redis 缓存缺失时自动自愈解密注入缓存，保障全流程无明文硬编码；
  3. **模型名称全链路统一收敛**：后端微服务、Nacos、前端学生端/管理端模型统一对齐为 `gpt-5.6-luna`。

### 2026-09-08 06:05:00 - 客服业务与 IT 行业技术双知识库全域规模化导入（V25 & V26 迁移）

* **核心成果**：
  1. **IT 行业主流技术图谱导入 (V26)**：
     - 对齐 CS-Notes、JavaGuide 及大厂高频技术标准，覆盖 Java、Spring微服务、MySQL、Redis、MQ、云原生/Linux、计网安全、算法、现代大模型(RAG/Agent)、系统架构等 11 大核心技术领域，新增 122 条工业级专业问答条目（IDs 101~222）；
  2. **客服业务知识库与 FAQ 全量扩充 (V25)**：
     - 对齐 NLP-CEduCusSerC 与 EduQA 等权威教育客服语料库，覆盖账号登录、课程播放、沙箱实验、导师答疑、支付订单、退款售后、发票财税等 10 大教育业务领域，知识库扩充至 80 条、FAQ 扩充至 30 条；
  3. **高精粒度分词与双基准测试验证**：优化关键词分词打分机制，消除短特征误匹配，152 组全域联合基准问答测试 100% 满分通过。

### 2026-09-08 05:25:00 - 全站“天机”/“tianji”全域对齐与品牌重命名“智问”/“zhiwen”

* **核心成果**：
  1. **前端工程与静态资产全面更名**：`portal` 与 `business-admin` 的 `package.json`、代理配置、Mock 数据、SVG Banner 资产、Iconfont、README 统一迁移对齐为“智问”/“zhiwen”；
  2. **后端微服务架构代码对齐**：讲师署名对齐为“智问教研团队”，重构 `LegacyZhiwenUserController`，短信验证码前缀对齐为 `zhiwen:auth:verifycode:`，演示支付协议对齐为 `demo://zhiwen-pay/`，各微服务 POM 及 Application 统一更名；
  3. **线上数据资产无损迁移 (V24)**：线上 `share.sys_user` 5,100 名学员与管理员邮箱统一迁移至 `@zhiwen.com`，教师与课程表全面清洗对齐。

### 2026-09-08 05:10:00 - 学员端首页视觉统一与极简浅色风重构

* **核心成果**：
  1. **消除暗黑控制台割裂感**：将顶部 `AgentReasoningHUD.vue` 由生硬刺眼的暗黑赛博控制台重构为纯白高阶卡片（`#FFFFFF`）与浅柔底色（`#F8FAFC`），融入全站教育质感；
  2. **以学员为中心去术语化**：去除底层工程名词，转换为亲切直观的五步导学流（`01 学情诊断` ➔ `02 智能匹配` ➔ `03 能力拆解` ➔ `04 进阶规划` ➔ `05 专属导学`）；
  3. **推荐卡片视觉净化与排版优化**：彻底清除封面多重盖章色块，下线模板化通用轮播图，提升 AI 导学与专属推荐至首屏核心区，推荐卡片呈现杂志级呼吸感。

### 2026-09-08 04:18:00 - 多智能体个性化推荐体系与 Python SPI 算法解耦全栈落地

* **核心成果**：
  1. **五大多智能体协同流水线**：
     - 构建用户画像 Agent ➔ 推荐 Agent ➔ 课程分析 Agent ➔ 路径规划 Agent ➔ RAG 知识检索 ➔ 解释生成 Agent 的完整流水线；
     - 抽象 `IRecommendAlgorithmEngine` 算法 SPI，默认接入 `RemotePythonAlgorithmEngine` 并支持自动无缝降级；
  2. **前端多智能体协同推理看板与职业大屏**：
     - 沉浸式协同推理动态看板（`AgentReasoningHUD.vue`）支持 5 阶段点亮、目标岗位实时推演与智能体推理透视；
     - 可解释性深度透视推荐卡片（Explainable AI Cards）展示 GPT 生成的推荐理由、突破短板徽章与先修依赖；
     - 职业成长进阶全景大屏（`CareerPathDrawer.vue`）呈现 4 阶段 DAG 课程递进路线与一键生成计划。

### 2026-09-07 14:50:00 - 扩充 300 门真实 IT 专业课程与 1,820 节教学大纲上线 (V22 迁移)

* **核心成果**：
  1. **100% 纯 IT 专业覆盖**：基于真实 MOOCCubeX / Coursera IT 课程体系，覆盖前端、后端、移动端、数据库、云原生DevOps、人工智能、数据科学、网安、游戏、区块链 10 大专业分类，全站课程扩充至 **320 门**；
  2. **多表联动与完整教学大纲**：同步填充 `edu_course_teacher` 讲师绑定与 `edu_course_catalog` 真实章节结构，全站目录扩充至 **1,820 节**，支持点播与打卡学习；
  3. **推荐引擎特征对接**：技能标签全量对齐 50 维技术图谱向量空间，实现高精度契合度匹配。

### 2026-09-07 14:20:00 - 5,000 名真实 IT 学员画像还原与嵌入式密集向量检索引擎 (V20 & V21 迁移)

* **核心成果**：
  1. **万人级真实学员分布数据拟合**：
     - 深入挖掘清华大学 MOOCCubeX 与英国开放大学 OULAD 数据集，导入 **5,000 名真实 IT 学员**（涵盖 Java、前端、AI算法、全栈、DevOps、Go 工程师）；
     - 生成 9,487 条真实选课打卡与章节进度记录 (`edu_learning_record`)，全站真实学员数扩至 5,123 人；
  2. **嵌入式密集向量余弦相似度检索引擎**：
     - 在微服务内存中构建 50 维 IT 技能密集向量空间字典，实现 `computeCosineSimilarity` 余弦夹角计算引擎，额外内存开销 < 10MB，微秒级极速匹配；
     - 服务端学员管理页面重构为标准分页与异步精准统计（5,106 名学员）。

### 2026-09-07 12:00:00 - 用户画像数据模型与多路召回推荐引擎底座构建 (V19 迁移)

* **核心成果**：
  1. **课程技能标签与难度标定**：Flyway 迁移 `V19__course_skills_and_user_portrait.sql` 为课程建立 50+ 技能标签、目标岗位与前置依赖基座；
  2. **用户画像数据模型**：新增 `edu_user_portrait` 表，根据学员学习打卡与考试记录动态计算技术偏好向量与自律指数；
  3. **多路召回推荐引擎与原生 SVG 技能雷达图**：落地包含技能匹配、岗位匹配、难度适配的多路召回算法，前端实现纯原生响应式 SVG 蜘蛛网雷达图（零 ECharts 依赖）。

### 2026-09-07 04:30:00 - Redis 高并发秒杀抢购、高频点赞榜与模拟沙箱支付闭环落地

* **核心成果**：
  1. **Redis 基础设施升级**：在 `share-common-redis` 补齐原子递减、分布式互斥锁、Set 去重与 ZSet 有序集合排行；
  2. **高频点赞热榜**：Set 键用户防重 + ZSet 键点赞计分，开放免鉴权排行榜接口与冷启动预热；
  3. **库存预扣与秒杀直购**：Redis 预扣配额结合 Set 防刷防超领，秒杀直购通道自动生单报读；
  4. **全链路模拟沙箱支付**：支持 `/ts/pay/order/{orderId}/demo-success` 支付回调，自动发放课程权益与学习记录。

### 2026-09-07 01:00:00 - 在线教育全站前端视觉现代重构

* **核心成果**：
  1. 依据 `frontend-design` 规范去除廉价模板感，废弃紫蓝发光渐变与浮球装饰；
  2. 确立 **Deep Slate（#0F172A）** + **Academic Blue（#2563EB）** 高阶学术科技色调；
  3. 卡片统一规范为 8px，表单、输入框统一为 6px，彻底移除 100px 跑马道胶囊；
  4. 重塑 16:9 标准比例课程卡片、个人中心与 AI 客服聊天气泡视觉体系。

### 2026-09-11 15:00:00 - 简历文档解析引擎与真实紧扣履历的 AI 职涯深度诊断上线

* **核心成果**：
  1. **Apache PDFBox 与 DOCX 双解析引擎**：在 `share-customer` 集成 `org.apache.pdfbox:pdfbox:2.0.30`，结合轻量 `ZipInputStream` 结构化解析 Word (.docx) 文档，精准抽取文字层与排版断行，彻底终结以前将 PDF 二进制字节流当作 UTF-8 读取导致 23 万字符 `%PDF-1.6` 乱码爆破编辑区的致命缺陷；
  2. **端到端二进制防护网**：前端 `myResume.vue` 与后端 `UserResumeServiceImpl` 双向部署魔数与非打印控制字符检测机制，坚决拒绝 `%PDF-` 与 `PK\x03\x04` 二进制注入，从根源保护数据库与大模型上下文；
  3. **基于真实经历的深度诊断与大厂对标**：彻底废弃以前空洞通用的虚构模板，建立覆盖 70+ 核心技术标签库、候选人真实项目名称抽取器与生产量化指标提取器（QPS/ms/降幅）。大模型 Prompt 强制绑定真实项目，兜底算法亦 100% 提取候选人真实项目与技术，动态生成高光亮点、薄弱项、大厂连环深挖考题与 STAR 重塑建议。

### 2026-09-11 18:00:00 - 职涯诊断打分机制彻底重塑与零同情分/零虚构治理

* **核心成果**：
  1. **60 分及格基准分机制确立与 5 维严谨计分重塑**：
     - 依据大厂校招与社招准入门槛，建立 60 分基准门槛（合法简历即享有基准 60 分），总分 100 分 = 60 分基础门槛 + 40 分专业进阶（核心技术栈广度与深度 10 分、项目经历与系统复杂度 10 分、STAR 量化业务成效 10 分、高可用工程与容灾规范 5 分、目标岗位与名企契合度 5 分）；
  2. **跨赛道不相干简历“零同情分”治理**：
     - 彻底删除旧版无技术标签时注入假标签（Java/Spring Boot/Redis）的逻辑；
     - 引入 8 大主流赛道 31 个技术岗位的对口关键词字典与严格契合度算法，对完全不相干简历（如幼师、文员、财务投递架构师岗位）实行“零同情分”机制，D1~D5 维度严谨打出 0 分，评级明确给出【严重脱节】、【匮乏】与【缺失】，总分严格锁定 60 分；
  3. **高光亮点与大厂考题 100% 紧扣真实经历（零虚构）**：
     - 大模型 Prompt 与 Java 后端审计双重设防：对无对口项目与技能的履历，坚决禁止虚构任何高并发、微服务或 Redis 考题，强制如实输出【无对口技术亮点】与【工程实战缺失】风险提示，考题针对性转向转岗动机核查、独立代码实践证明与差距补齐建议；
  4. **前端大屏得分视觉多态与定向双场景验证通过**：
     - 诊断大屏 breakdown 维度新增动态彩色评级徽标（卓越/良好/基础/偏弱/匮乏/严重脱节）与 0 分红字告警；
     - 双场景端到端自动化测试 100% 通过（场景A：跨赛道不相干简历严格 60 分且零虚构；场景B：真实专业架构师简历 95 分卓越对标）。


### 2026-09-13 06:15:00 - 课程随堂测验 Tab 激活、未答题全量审计留痕与考试重交自愈治理

* **核心成果**：
  1. **课程播放室随堂测验交互全面激活**：
     - `frontends/portal/src/pages/learning/index.vue` 激活沉浸式“随堂测验”功能，基于课程 ID 动态拉取测验题目；
     - 实现题型徽标、富文本题干渲染、单选/多选/判断专属答题卡、即时交卷与考后得分/错题解析折叠展示；
  2. **后端测验未答题审计与落库修复**：
     - `share-education` 中修正提交试卷时未答题丢失的缺陷，在 `tj_education.edu_exam_answer` 全量保留未答题审计记录（标记 user_answer 为空字符串与 is_correct=0）；
     - 增强 `examRecordDetails` 接口，回显题目清单时自动聚合答卷详情，解决二次查询试卷题目丢失问题；
     - 消除字段别名差异（兼容 `questionId`/`id` 与 `userAnswer`/`answer`），实现交卷与成绩查看幂等闭环；
  3. **云端生产部署与定向回归验证**：
     - 热部署 `tianji-education` 与 `tianji-portal-ui`，自动化测试覆盖模拟答卷、未答题审计与成绩详情查询，100% 通过。

### 2026-09-13 06:22:00 - 学员个人安全中心重构、越权权限修复与安全绑定治理

* **核心成果**：
  1. **学员密码修改越权漏洞与鉴权机制修复**：
     - `LegacyZhiwenUserController` 修正 `PUT /students/password` 原先强依赖管理员权限 `@RequiresPermissions("system:user:edit")` 导致学员修改密码报 403 的重大缺陷；
     - 切换为 `@RequiresLogin`，并建立自主修改与管理员代修严格边界；自主修改时必须通过 BCrypt 校验旧密码、验证新密码长度（>=6 位）及新旧密码防重，管理员修改保留权限校验；
  2. **学员资料手机号与邮箱唯一性校验补齐**：
     - `PUT /students` 补齐 `userService.checkPhoneUnique` 与 `userService.checkEmailUnique`，杜绝手机号或邮箱冲突被静默覆盖；
  3. **学员端个人安全中心 UI 激活**：
     - `frontends/portal/src/pages/personal/mySet.vue` 废弃“暂未开放”提示，正式上线修改密码、绑定/更换手机号（11位正则）、绑定/更换邮箱专用模态对话框与即时状态同步；
  4. **云端生产热部署与端到端自动化测试**：
     - 热部署 `tianji-system` 与 `tianji-portal-ui`；
     - 编写并执行 `.scratch/test_user_security.py`，验证错误旧密码阻断、短密码阻断、密码修改后成功登录及重置恢复，全链路 100% 满分通过。


### 2026-09-13 06:35:00 - AI 模拟面试考场沉浸式 HUD 完赛流重构、终局大屏零分防击穿与负向弃考启发式评分加固

* **核心成果**：
  1. **AI 面试官启发式打分与弃考回答严格防御**：
     - `InterviewServiceImpl.java` 治理旧逻辑下无论回答质量均一律给 60 分并违心评价“切中核心要点”的缺陷；
     - 增加对“不会”、“不知道”、“pass”、“跳过”、“没学过”及 5 字符以内负向/无效作答的严格检定，得分严格归 0，并给出警示性评语与基础学习建议；
  2. **终局诊断大屏报告零分视觉防击穿修复**：
     - 修复 `frontends/portal/src/pages/interview/report.vue` 中 `{{ sessionData.score || 80 }}` 在得分为 0 时因 JS 假值被错误击穿为 80 分的严重漏洞；
     - 修复六维胜任力雷达图及文本标签 `|| 75` 在维度 0 分时被虚增至 75 分的问题，全面切换为非空合并运算 (??) 与严格数值类型守卫；
     - 优化 `generateFinalReport` 兜底算法，使六维能力雷达数据与综合综述严格随总分动态阶梯适配，彻底消除 30 分学员被评为“具备独立架构攻坚能力”且雷达全满 60+ 的虚假泡沫；
  3. **考场 HUD 状态机与沉浸式交互升级**：
     - `frontends/portal/src/pages/interview/room.vue` 区分进行中、已完成（`status=2`）与已终止（`status=3`）状态；
     - 已完成时顶部 HUD 动态切换为绿色“查看终局报告”，底部输入框替换为高质感 `dialogue-completed-banner`；已终止时清晰提示复盘说明；
     - 最后一轮提交作答后自动感知终局状态并弹出恭贺模态框，提供一键直达终局诊断大屏交互；
     - `index.vue` 历史战报卡片支持“查看诊断大屏”/“回顾问答实录”/“继续答题”三态智能分流；
  4. **云端生产热部署与端到端自动化测试**：
     - 串行打包热部署 `tianji-customer` 与 `tianji-portal-ui`；
     - 执行 `.scratch/test_interview_flow.py`，完整覆盖开题、负向弃考答题 0 分拦截、终局 Reject 裁决与真实低分雷达渲染，100% 满分通过。

### 2026-09-13 06:42:00 - 交易结算与退款逆向全链路状态机治理：已付/关闭/退款订单防重付与防复活拦截、退款驳回后二次申诉通道打通、待审退款假驳回显示修复、全格式折扣券多端渲染校准

* **核心成果**：
  1. **订单支付生命周期严格拦截与防复活闭环**：
     - 排查并根治 `TradeService.createPayment` 与 `simulatePayment` 缺少订单初始状态校验的问题，彻底杜绝已支付（`orderStatus=1`）、已关闭（`orderStatus=4`）或已退款（`orderStatus=3`）订单被恶意重新拉起支付并覆盖写入开课权限的严重商业安全漏洞；
     - 增加强制状态校验 `require(order.getOrderStatus() == 0, ...)`，对非待支付状态订单精准阻断并提示对应业务原因。
  2. **退款逆向流全链路状态机与二次申诉支持**：
     - 修复 `applyRefund` 在历史退款被驳回（`status=2`）时直接返回旧驳回记录、且前端锁定 `canRefund=false` 导致学员被永久剥夺退款权利的缺陷；
     - 重构状态判断：历史退款被驳回后允许学员修改问题描述与退款原因重新发起申诉，并在订单详情页动态呈现「重新申请」专属操作按钮；
     - 修复 `approveRefund` 退款成功后遗漏返还优惠券的问题，自动触发 `restoreOrderCoupon` 原路退回学员可用优惠券资产。
  3. **退款详情弹窗假驳回严重前端缺陷根治**：
     - 排查发现 `myOrderDetails.vue` 弹窗审批结果通过 `v-if="refundDetailsData.remark != null"` 判断，而在后端待审核（`status=0`）时 `remark` 返回 `false`，导致 JS 将 `false != null` 误判为真，直接对刚提交退款申请的学员触目惊心地显示「审批结果：拒绝退款」；
     - 重构前后端状态协议：后端补充 `statusText`、`payChannel` 并在待审核时置 `remark=null`；前端细化为 0: 审核中（黄标提醒）、1: 同意退款（绿标提醒）、2: 拒绝退款（红标提醒），彻底消除假驳回乌龙。
  4. **全格式折扣券与优惠券卡片折率多端渲染校准**：
     - 修复 `class.js` 与 `CouponCards.vue` 中对折扣券（`discountType=2/5`）粗暴除以 10 导致数据库分值 800（8折）被错误渲染为荒谬的「打80折」与「80 折」超大字体的视觉缺陷；
     - 建立统一折率自适应算法，自动识别百倍分值（800 -> 8折）、十倍分值（80 -> 8折）与原始数值（8 -> 8折），并全面兼容 8.5 折等小数折率；
     - `TradeService.discount` 同步兼容多源百分比、十倍率与纯小数计算，杜绝算错折减金额风险；个人订单列表支持删除已退款（`status=6`）订单。
  5. **云端生产热部署与端到端自动化测试验证**：
     - 本地编译生成 `share-trade.jar` 与 `portal` 生产包，安全热更新云端 `tianji-trade` 与 `tianji-portal-ui`；
     - 编写并执行 `.scratch/test_trade_flow.py`，完整覆盖通过网关 `/ts` 与 `/prs` 鉴权、领券、下单、支付、重复支付严格阻断拦截、退款申请、管理端驳回、订单回显可重新申请、二次申诉申请、管理端同意退款、优惠券恢复以及已退款订单防支付拦截等全部 11 步关键流程，100% 满分通过。

