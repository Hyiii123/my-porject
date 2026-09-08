-- ==========================================================================
-- 智问学伴：客服业务知识库与高频 FAQ 全面扩充对齐脚本
-- 依据：主流权威在线教育问答数据集（NLP-CEduCusSerC、EduChat、Bitext E-learning）
-- 适用版本：V25
-- ==========================================================================
USE `tj_customer`;
SET NAMES utf8mb4;

-- 1. 批量插入/更新专业客服知识库 (cs_knowledge: 80条核心业务条目)
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (1, '如何注册账号？', '点击平台右上角【注册】按钮，输入常用手机号码并获取短信验证码，设置 8-20 位登录密码后即可完成注册。平台亦支持微信扫码与 GitHub 快捷一键登录，首次登录后绑定手机号即可永久关联学习资产。', '注册,新用户注册,账号注册,注册账号,手机号注册,怎么注册,创建账号', '账号与登录', 1, 'kb-1', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (2, '课程支持退款吗？', '平台实行严格的【7天内无理由退款】保障政策。在购买课程之日起 7 天内，且该门课程已观看的小节进度不超过 30%（未完成章节测试与大作业），您可在【个人中心】-【我的订单】中自主提交退款申请，系统将即时审核办理退款。', '退款,退课,退学费,退款政策,支持退款,售后退款,能退款吗,申请退款', '订单售后', 1, 'kb-2', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (3, '有哪些支付方式？', '平台目前支持微信支付（微信扫码/微信App内调起）、支付宝（网页收银台/App扫码）以及网银转账。订单金额满 300 元以上支持花呗 3/6/12 期分期；企业客户批量采购支持签署电子采购合同并走公对公银行转账。', '支付方式,付款方式,怎么付款,支付宝,微信支付,银联,在线支付,支付渠道', '订单支付', 1, 'kb-3', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (4, '如何查看我的学习进度？', '登录后进入【个人中心】-【我的课程】，每个课程卡片下方均清晰展示当前已学百分比、已学小节数及总时长。进入课程播放页时，右侧小节目录列表会以绿色勾号标记已学完小节，方便您随时追踪复习进度。', '学习进度,查看进度,进度查询,学习记录,进度条,已学时长,查看已学', '课程学习', 1, 'kb-4', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (5, '视频播放不了怎么办？', '若视频无法播放、黑屏或一直缓冲转圈：1. 请尝试刷新页面或将清晰度由 1080P 切换为 720P；2. 检查浏览器硬件加速设置并尝试关闭；3. 推荐使用最新版 Google Chrome 或 Microsoft Edge 浏览器访问；4. 若在企业内网环境，请检查防火墙是否拦截流媒体端口，建议尝试切换手机热点。', '无法播放,播放失败,视频卡顿,视频黑屏,加载缓冲,转圈,看不了视频', '课程学习', 1, 'kb-5', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (6, '课程可以开发票吗？', '可以。购买付费课程后，您可在【个人中心】-【发票管理】中自主申请开票。平台支持开具增值税电子普通发票（1-2个工作日推送至邮箱）和增值税专用发票（顺丰寄送或数电专票推送），开票类目默认为合规的【*生活服务*非学历技术培训费】。', '开发票,开票,申请发票,索取发票,发票申请,开具发票,培训费发票', '发票与财税', 1, 'kb-6', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (7, '学完课程可以获得证书吗？', '可以。当整门课程学习进度达到 100% 且各章节课后编程作业与期末考核综合成绩达到 80 分以上，系统会自动在【个人中心】-【我的证书】生成具有唯一防伪编号与验真二维码的官方结业电子证书，支持直接下载打印。', '证书领取,考取证书,获得证书,认证证书,完课证明,结业证明,领证书', '就业与认证', 1, 'kb-7', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (8, '忘记密码怎么处理？', '在登录界面点击【忘记密码】，输入您绑定的手机号码，获取并输入短信验证码，完成人机安全验证后即可重新设定新密码并直接登录。如绑定手机已停用，请联系在线客服申请人工申诉换绑。', '忘记密码,重置密码,找回密码,修改密码,密码丢失,想不起密码', '账号与登录', 1, 'kb-8', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (9, '优惠券在哪里使用？', '在选定课程点击【立即购买】进入订单确认结算页时，系统会自动为您匹配并勾选面值最大、优惠力度最强的可用优惠券。您亦可在结算页【优惠券抵扣】下拉栏中手动切换使用其他卡券。请注意单笔订单仅限使用一张优惠券，不可叠加。', '优惠券,使用优惠券,卡券抵扣,折扣券,优惠码,结算抵扣,怎么用券', '优惠活动', 1, 'kb-9', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (10, '客服中心可以咨询哪些内容？', '智问学伴客服中心支持全天候解答：账号注册与登录安全、课程学习与视频播放、云端实训沙箱报错、技术导师答疑支持、订单支付与分期、退款与售后仲裁、发票开具与报销、结业证书与大厂内推等全流程业务。如遇复杂技术难题亦可转接专属教研老师。', '帮助中心,服务范围,咨询范围,业务范围,平台支持,客服职责,咨询内容', '客服服务', 1, 'kb-10', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (11, '如何修改绑定的手机号码？', '登录后点击右上角个人头像进入【个人中心】-【账号设置】-【手机绑定】，点击【更换手机号】。系统会向原手机号发送验证码进行身份验证，验证通过后输入新手机号并完成短信验证即可完成换绑。若原手机号已停用无法接收验证码，请联系人工客服提供实名认证身份证明进行人工审核换绑。', '手机,手机号,更换,换绑,修改手机,更换手机,手机绑定,修改绑定的手机,更换手机号,手机停用', '账号与登录', 1, 'kb-11', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (12, '一个账号可以在多台设备同时登录吗？', '为保护课程版权与学员账号安全，平台支持同一账号在 1 台 PC 电脑端和 1 台移动设备（手机或平板）同时在线。若检测到第 3 台设备或异地 IP 同时发起播放请求，系统将触发安全风控，自动将先前登录的设备踢出并要求短信验证码重新登录。请勿与他人共享账号以防封禁。', '设备,多设备,同时登录,异地登录,异地,踢出,被踢,限制登录,账号共享,几台设备,同时在线', '账号与登录', 1, 'kb-12', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (13, '为什么我没有收到短信验证码？', '短信验证码延迟或未收到通常有以下原因：1. 手机开启了短信拦截软件或垃圾短信过滤，请查看手机拦截信箱；2. 当前时段运营商短信网关拥堵，建议等待 60 秒后点击重新发送；3. 手机欠费停机或处于飞行模式/弱信号区；4. 频繁请求触发防刷限制（单个号码每小时最多获取 5 次）。如仍未收到，可尝试使用快捷扫码登录。', '短信,验证码,收不到,拦截,短信拦截,验证码未收到,没有验证码,接收短信,收不到验证码', '账号与登录', 1, 'kb-13', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (14, '如何进行实名认证？认证后可以修改吗？', '进入【个人中心】-【实名认证】，输入您的真实姓名与二代身份证号码，系统将调用公安部权威数据通道进行实时核验，通常在 10 秒内即可完成认证。实名认证关乎结业证书签发、学信备案及发票开具，一旦核验通过不可随意修改；若因法定改名需变更，须提交户籍管理部门出具的更名证明由后台人工更正。', '实名,认证,身份证,实名认证,修改实名,认证修改,姓名,身份认证,实名变更', '账号与登录', 1, 'kb-14', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (15, '微信或 GitHub 快捷登录后如何绑定手机号？', '使用微信扫码或 GitHub 第三方快捷登录的新用户，在首次进入平台时系统会自动引导弹出【完善账号信息】窗口，输入常用手机号并验证即可完成关联。后续您既可以使用微信/GitHub 一键授权登录，也可以直接使用该手机号接收验证码登录，两端学习数据与已购课程实时打通合并。', '微信,GitHub,第三方,快捷登录,绑定手机,微信登录,GitHub登录,关联账号,第三方账号', '账号与登录', 1, 'kb-15', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (16, '账号被系统风控冻结或限制登录怎么办？', '若账号出现异常高频请求、恶意抓取课程视频资源、发布违规技术广告或异地多 IP 异常登录，系统安全引擎会自动对账号进行临时风控冻结。您可以准备好注册手机号、本人手持身份证照片以及最近一笔订单的支付凭证截图，通过在线客服提交【账号解封申诉】，安全合规团队会在 1 个工作日内完成审核。', '冻结,封号,解封,风控,申诉,限制登录,账号被封,解冻账号,账号冻结', '账号与登录', 1, 'kb-16', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (17, '如何彻底注销平台账号？注销后课程还在吗？', '在【个人中心】-【账号与安全】最下方点击【注销账号】。请注意：账号注销属于不可逆操作，注销后您在该账号下购买的所有付费课程、学分积分、学习进度记录、电子证书及发票历史都将被彻底物理脱敏或清空，无法再次恢复。请务必在确保无待处理退款和发票的前提下谨慎操作。', '注销,销户,注销账号,清空数据,账号删除,彻底注销,注销后果,删除账号', '账号与登录', 1, 'kb-17', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (18, '如何设置或修改账号的安全登录密码？', '如果您之前是通过短信验证码注册登录的，可以进入【个人中心】-【账号设置】-【密码管理】，点击【设置密码】输入短信验证码后设定 8~20 位的强密码（需包含大写字母、小写字母及数字）。已有密码需要修改时，输入原密码及新密码验证即可生效。', '密码,修改密码,设置密码,重置密码,安全密码,密码长度,改密码,登录密码', '账号与登录', 1, 'kb-18', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (19, '视频播放支持哪些播放倍速？快捷键有哪些？', '平台视频播放器原生支持 0.75x、1.0x、1.25x、1.5x、2.0x 乃至 3.0x 多档无级无损变速播放。支持常用桌面快捷键：空格键（播放/暂停）、方向键左/右（快退 5 秒 / 快进 5 秒）、方向键上/下（音量增减）、F 键（全屏/退出全屏）、M 键（静音切换）。', '倍速,播放倍速,快捷键,全屏,快进,快退,空格键,调速,加速播放,播放器快捷键', '课程学习', 1, 'kb-19', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (20, '课程配套的源码、课件 PPT 与资料在哪里下载？', '在课程学习播放页面的右侧侧边栏或播放器下方，点击【资料包/课件】标签页，即可看到讲师上传的各章节随堂代码 Git 仓库链接、PPT 讲义 PDF 以及预置配置文件。点击【一键打包下载】即可下载完整的 ZIP 压缩包；若包含 GitHub/Gitee 仓库，亦可直接通过 git clone 同步。', '源码,代码,下载,课件,资料,资料包,PPT,Git,代码下载,源码下载,配套源码,下载课件,讲义下载,课程源码,课件下载', '课程学习', 1, 'kb-20', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (21, '视频播放时出现黑屏或一直加载缓冲怎么办？', '视频黑屏或转圈缓冲通常是由于浏览器硬件加速冲突或 CDN 线路延迟导致的。排查建议：1. 刷新页面或切换清晰度（从超清 1080P 切换为高清 720P）；2. 在 Chrome/Edge 浏览器设置中搜索并尝试关闭【使用图形加速功能（硬件加速）】；3. 清理浏览器缓存 Cookie 或使用无痕隐身窗口尝试；4. 若公司内网限制流媒体端口，请切换至移动热点网络。', '黑屏,缓冲,卡顿,无法播放,加载慢,播放失败,视频加载,转圈,绿屏,视频黑屏,一直加载', '课程学习', 1, 'kb-21', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (22, '为什么我的学习进度没有更新或显示未完成？', '课程小节进度达成需要满足播放完成率达到 90% 以上，且播放器心跳周期性向云端上报进度。常见原因：1. 拖动进度条跳跃播放导致实际观看时长未达标；2. 视频未播完即直接关闭了网页标签页；3. 随堂测试题或问卷尚未作答提交。建议您完整播放该小节至结尾，待小节列表右侧图标变为绿色打勾标记即代表进度记录成功。', '进度,不更新,未完成,打勾,进度同步,进度不走,未记录,学完不打勾,进度不动', '课程学习', 1, 'kb-22', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (23, '课程购买后有学习有效期吗？过期后能免费延期吗？', '绝大多数单门技术实战课程（如 Java、Vue、微服务等）均享有【永久无限次回放】权益，购买后可终身随时复习。少数含有导师 1v1 指导、定制简历修改与高算力 GPU 实验环境的“就业保障集训营”，服务期通常为 6~12 个月。若学员因生病、出差等客观原因需要延期，可凭有效凭证向客服申请一次免费延长 30 天服务期。', '有效期,永久有效,到期,延期,服务期,集训营,课程过期,无限回放,学习期限,课程有效期', '课程学习', 1, 'kb-23', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (24, '课程支持在手机或平板端离线缓存下载吗？', '支持。学员可通过移动端 App 打开已购课程，在小节列表右上方点击【批量缓存】，勾选需要离线学习的章节即可在 Wi-Fi 环境下缓存至本地存储。离线缓存视频采用加密切片存储，仅供绑定的登录设备在离线状态下播放，无法导出为独立 MP4 文件。', '离线,缓存,离线下载,离线播放,移动端下载,无网学习,离线看,离线看视频,下载视频,视频离线', '课程学习', 1, 'kb-24', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (25, '随堂测验和章节编程作业的批改规则是怎样的？', '随堂选择题与判断题由系统在学员点击【提交答案】后毫秒级自动判题评分，并展示详尽的答案解析；涉及编写代码或工程项目的章节大作业，提交后会进入云端自动化评测沙箱运行测试用例，并在 3~5 分钟内生成单元测试覆盖率与代码规范报告；主观实战项目由专业助教在 48 小时内进行人工 Code Review 批注。', '测验,批改,作业,随堂测验,作业批改,判题,自动化评测,Code Review,作业评分,大作业,编程题批改', '课程学习', 1, 'kb-25', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (26, '课程小节如何做笔记？笔记支持导出为 Markdown 吗？', '在学习播放页面右侧点击【我的笔记】，输入文字并点击保存，系统会自动将笔记与视频当前时间戳打点锚定。点击时间戳即可直接跳转定位到讲师讲授该知识点的视频画面。在【个人中心】-【我的笔记】中，支持将单门课程或全站所有学习笔记一键导出为标准 Markdown（.md）或 PDF 文件，方便同步到 Notion、Obsidian 等外部工具。', '笔记,做笔记,笔记导出,Markdown,时间戳,导出笔记,视频笔记,记笔记,随堂笔记', '课程学习', 1, 'kb-26', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (27, '为什么部分章节视频显示“正在转码中”？', '当讲师更新上传了最新技术版本的课件视频后，云端媒体转码集群需要对其进行 1080P/2K 多码率切片与防盗链水印压制。转码通常在上传后 15~30 分钟内完成。显示“正在转码中”说明视频刚更新完毕，请您稍候片刻刷新即可正常学习。', '转码,转码中,视频转码,更新中,暂未开放,转码失败,视频审核', '课程学习', 1, 'kb-27', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (28, '课程学完后如何申请开具学时证明？', '当整门课程的学习进度达到 100% 且章节作业全部及格后，进入课程详情页底部点击【申请学时证明】。系统将自动生成包含学员实名、身份证脱敏号、课程名称、总学时（换算为学时数）及平台防伪公章与电子二维码的官方学时证明 PDF，支持直接下载打印用于企业报销或评优备案。', '学时,证明,学时证明,证明文件,继续教育,报销证明,培训学时,开具学时,开学时证明', '课程学习', 1, 'kb-28', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (29, '什么是云端在线实验沙箱？如何启动实验？', '云端实验沙箱是平台为学员提供的免装环境、开箱即用的 Linux/Docker 独立计算容器。在含有动手实验的章节页面中，点击【启动实验环境】，后台集群会在 15~30 秒内秒级调度分配专属容器，并在浏览器内直接嵌入完整功能的 VS Code (WebIDE) 及远程 Terminal 终端，无需在本地配置繁琐开发环境。', '沙箱,实验,WebIDE,容器,启动实验,云端实验,实验环境,在线编程,启动沙箱', '在线实训与实验', 1, 'kb-29', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (30, '实验沙箱启动超时或连接断开如何处理？', '沙箱连接断开常见原因及处理方法：1. 长时间（超过 30 分钟）无键盘鼠标操作，系统为节约算力会自动进入休眠状态，点击【重新激活连接】即可唤醒；2. 浏览器 WebSocket 长连接被公司代理防火墙阻断，可尝试关闭公司 VPN 或切换手机热点；3. 若容器因 OOM（内存溢出）崩溃，点击控制台右上方【重启实验沙箱】即可重新拉起。', '沙箱,超时,断开,连接断开,启动超时,无法连接,沙箱超时,休眠,掉线,重连,连接超时,超时断开', '在线实训与实验', 1, 'kb-30', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (31, '在线实验的代码和修改会自动保存吗？', '是的。WebIDE 具备自动即时保存机制，您在编辑器中编写的代码与安装在 `/workspace` 用户主目录下的文件均挂载在分布式持久化云盘中。即便关闭浏览器窗口或沙箱由于超时休眠，24 小时内重新启动实验仍可无缝恢复之前写好的代码资产。', '代码,保存,自动保存,保存代码,实时保存,沙箱保存,数据保存,持久化,代码丢失', '在线实训与实验', 1, 'kb-31', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (32, '如何重置实验环境或还原初始代码？', '如果您在调试过程中修改了底层核心依赖导致环境损坏，或者希望从头重新练习该实验，可以点击实验窗口右上角工具栏的【重置实验】按钮。系统将清空当前沙箱的用户变更，并在 10 秒内将操作系统环境、初始脚手架代码及数据库种子数据彻底还原至讲师出厂预设状态。', '重置,还原,重置实验,还原代码,初始代码,清空环境,重新开始实验,还原实验,重置代码', '在线实训与实验', 1, 'kb-32', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (33, '实验环境的 GPU 算力配额如何申请与计算？', '针对大语言模型微调（LLM LoRA）、深度学习计算机视觉等 AI 系列课程，平台为学员配备了专属的云端 NVIDIA A10/V100 GPU 算力池。每位报名学员每月享有 20 小时的免费 GPU 实验配额。实验启动时开始计时，关闭沙箱时停止扣除。若配额用尽且课程作业尚未完成，可在实验控制面板发起【补充算力申请】，由教研团队免费补增 10 小时。', 'GPU,算力,显卡,配额,算力不足,深度学习环境,申请算力,显卡算力', '在线实训与实验', 1, 'kb-33', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (34, '本地开发环境（JDK/Node/Python）与课程版本冲突如何解决？', '推荐两种最佳实践：1. 直接使用平台提供的云端在线实验沙箱，环境已由讲师标准化锁定，绝无兼容问题；2. 若坚持在本地电脑开发，推荐使用版本管理工具隔离环境：Java 使用 SDKMAN、Node.js 使用 nvm、Python 使用 conda 或 uv 虚拟环境，课程资料包中均附有详细的多版本本地共存配置指南。', '版本冲突,本地环境,JDK版本,Node版本,Python版本,nvm,环境冲突,版本不兼容', '在线实训与实验', 1, 'kb-34', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (35, 'WebIDE 容器里启动的网页服务如何进行浏览器预览？', '在云端沙箱的内置终端中启动 Spring Boot / Vue / Vite 服务（如监听端口 8080 或 5173）后，VS Code 右下角会自动弹出【端口已映射】提示。点击【在浏览器中打开】即可通过平台分配的独立动态安全二级域名直接访问您的前端页面或后端接口，无需手动配置内网穿透。', '预览,浏览器,网页,WebIDE,端口,网页预览,浏览器预览,Web预览,打开网页,访问页面', '在线实训与实验', 1, 'kb-35', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (36, '如何在实验沙箱中提交代码以完成课后评测？', '在完成实验指导书规定的所有任务后，在 WebIDE 顶部菜单栏点击【评测与提交】，系统将自动运行后台测试套件校验您的核心函数与接口输出。测试用例全部通过后即自动记录该实验通关成就并颁发实训积分；若未通过，终端会详细高亮失败用例与预期输出对比。', '提交,评测,通关,测试,提交评测,代码评测,完成评测,通关评测,评测不通过,测试用例,提交代码', '在线实训与实验', 1, 'kb-36', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (37, '学习过程中遇到代码报错或 Bug 如何提问？', '在视频小节下方点击【发起问答】，建议按“三段式”规范提问可获得最快解决：1. 详细贴出报错终端堆栈日志（文字形式，避免发模糊手机照片）；2. 贴出引发报错的关键配置文件或 Java/Vue 代码段；3. 简要说明自己的操作系统环境（如 Win11 / macOS）及已尝试过的解决办法。专业教研助教会优先接单并回复排查思路。', '报错,Bug,提问,求助,代码报错,问答区,问问题,怎么提问,运行报错,助教答疑,找助教', '技术答疑与导师', 1, 'kb-37', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (38, '助教与讲师的答疑服务时间是几点到几点？', '官方技术团队的标准答疑服务时段为：工作日周一至周五 09:30 - 21:30，周末及法定节假日 10:00 - 18:00。在服务时段内提出的技术疑问，教研助教平均在 30~60 分钟内给出专业分析；非服务时段提出的问题，系统会优先调度 AI 导师秒级初步分析，并在次日服务时段由人工助教跟进确认。', '答疑时间,助教工作时间,助教作息,解答周期,服务时间,答疑时长,何时回复', '技术答疑与导师', 1, 'kb-38', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (39, '如何加入课程官方专属学习交流群（微信/QQ/钉钉）？', '在购买课程成功后，进入订单完成页或课程详情页右侧，会展示该课程专属技术交流群二维码（包含班主任微信号及 QQ 技术大群号）。若二维码过期未及时扫码，可在【个人中心】-【我的课程】- 点击该课程卡片右上角【班级社群】，即可随时调取最新加群二维码及入群暗号（您的学员 UID）。', '交流群,学员群,微信群,QQ群,加群,班主任,班级群,入群方式,社群', '技术答疑与导师', 1, 'kb-39', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (40, 'VIP 学员的 1v1 远程代码调试与专属答疑权益如何预约？', '购买就业班或 VIP 架构师课程的学员享有每周 1 次的专属 1v1 远程屏幕共享答疑权益（单次 45 分钟）。请提前至少 24 小时在【个人中心】-【1v1预约日历】中选择期望的时间段与意向导师，并简要填写需要攻坚的项目难题。预约成功后系统将自动发送腾讯会议/飞书会议链接至您的绑定手机。', '1v1答疑,远程协助,代码调试,屏幕共享,预约导师,远程调代码,一对一答疑', '技术答疑与导师', 1, 'kb-40', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (41, '错过了讲师的周度直播答疑课，可以在哪里看回放？', '每周举办的技术大咖直播答疑课，在直播结束后的 24 小时内，系统会自动进行云端视频切片与音频降噪剪辑，并同步更新上架至该课程的【直播答疑回放】章节。回放同样支持倍速播放与随堂笔记功能，永久提供给该班级学员复看。', '直播,回放,直播回放,错过直播,直播视频,答疑回放,看回放,直播补看,看直播', '技术答疑与导师', 1, 'kb-41', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (42, '实战大作业的人工 Code Review 包含哪些反馈内容？', '专业教研架构师会从 4 个核心维度对您提交的 GitHub 实战仓库进行逐行代码审查：1. 架构分层与设计模式运用合理性；2. 业务边界异常处理与高并发线程安全性；3. 数据库慢 SQL 索引优化与事务隔离级别把控；4. Google/阿里巴巴 Java 规范执行度。审核意见将直接以 Pull Request Review Comments 形式反馈给您。', 'Code Review,代码审查,代码批注,作业反馈,代码点评,批注指导', '技术答疑与导师', 1, 'kb-42', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (43, '如果觉得助教老师解答不清楚或态度不好怎么办？', '平台实行严苛的答疑服务质量考核。如果您对某次问答回复不满意，可直接在问答回复右下方点击【未解决】并勾选【申请督学介入】或给予差评。系统会将该问题即时升级转交教研主管进行二次复核，并于 4 小时内安排资深讲师重新为您进行深入剖析与答疑。', '解答不清楚,投诉助教,答疑差评,督学介入,服务态度,答疑不满意', '技术答疑与导师', 1, 'kb-43', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (44, '平台支持花呗分期或信用卡分期付款吗？', '支持。在课程结算页选择【支付宝】或【微信支付】渠道后，若订单金额满 300 元，支付收银台页面将自动出现分期选项。学员可选择花呗 3 期 / 6 期 / 12 期分期付款，部分活动期间可享平台免息补贴；使用带有银联标识的信用卡亦可直接在银行收银台办理分期。', '花呗,分期,分期付款,信用卡分期,分期免息,免息,分期购买,花呗分期', '订单支付', 1, 'kb-44', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (45, '银行卡已扣款但订单显示“未支付”怎么办？', '这是偶发的第三方支付网关（微信/支付宝）与教育平台之间的异步通知网络抖动导致（俗称“掉单”）。处理步骤：请不必重复支付！通常在网络畅通后 3~5 分钟内网关自动对账补单成功。若超过 10 分钟仍未开通，进入【我的订单】点击【同步支付状态】，系统将主动调用支付通道查单对账并秒级自动开通课程权限。', '已扣款未开通,掉单,扣费未支付,支付延迟,钱扣了课程没开,同步支付状态', '订单支付', 1, 'kb-45', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (46, '企业客户批量采购课程如何开具合同并走公对公转账？', '平台为企事业单位技术团队培训提供专属大客户服务通道。采购总金额满 5000 元以上可享受批量团购折扣并签署正规商业电子培训合同。您可以拨打企业采购专线或在网站底部点击【企业团购咨询】，大客户经理会在 2 小时内跟进对接，提供公对公银行账户信息并协助办理批量员工账号开通与增值税专票开具。', '企业采购,企业培训,对公转账,公对公,商业合同,大客户,团队团购,公司买单', '订单支付', 1, 'kb-46', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (47, '什么是课程兑换码？如何使用兑换码开通课程？', '课程兑换码是企业团购、技术大会活动礼赠或纸质书籍配套附赠的 16 位大写英数卡密。使用方法：登录个人账号后，进入【个人中心】-【卡券与兑换】页面，在兑换码输入框粘贴卡密并点击【立即兑换】。兑换成功后对应课程将立即加入您的【我的课程】书架中，永久有效。', '兑换码,卡密,课程卡,激活码,兑换课程,卡券兑换,输入兑换码', '订单支付', 1, 'kb-47', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (48, '为什么支付页面提示“当前交易存在安全风险”？', '该提示由微信或支付宝风控系统触发，通常是因为您使用了非本人的支付账户、短时间内异地大额支付或使用了公共免密 Wi-Fi 网络。建议切换为手机移动 5G 网络，或在支付方式中更换为银行卡/支付宝备用通道重新发起。', '交易风险,支付风险,支付拦截,支付限额,付款失败,风险拦截', '订单支付', 1, 'kb-48', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (49, '购买课程支持使用组合支付或跨币种支付吗？', '目前单笔订单支持【账户可用余额 + 微信支付】或【账户可用余额 + 支付宝】的组合扣款方式。针对海外或港澳台学员，平台支持通过绑定了 Visa / MasterCard 国际信用卡的支付宝或微信进行跨境结算，系统将根据实时离岸外汇汇率自动折算为相应本币扣款。', '组合支付,余额支付,海外信用卡,Visa,MasterCard,外卡支付,跨境支付', '订单支付', 1, 'kb-49', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (50, '未支付的待付款订单有效期是多久？', '在订单提交后，系统将为您锁定优惠券和拼团秒杀名额，待支付倒计时默认为 30 分钟。若超过 30 分钟未完成付款，系统将自动取消该笔订单并释放已锁定的优惠券与名额。若仍需购买，重新点击立即下单即可。', '待付款,订单有效期,订单失效,超时取消,自动取消,订单倒计时', '订单支付', 1, 'kb-50', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (51, '支付成功后在哪里查看电子订单收据与交易明细？', '支付完成后，您可在【个人中心】-【我的订单】-【已完成】中随时查看所有历史交易记录，点击【查看明细】可获取包含订单交易号、商户流水号、支付通道、交易金额及明细清单的标准电子收据，并支持直接导出为 PDF 存证。', '订单明细,交易记录,电子收据,交易流水,历史订单,购买记录', '订单支付', 1, 'kb-51', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (52, '课程退款成功后，款项一般多久原路到账？', '退款申请审核通过后，平台结算系统会在 10 分钟内向原支付机构发起原路退款指令。具体到账时间取决于您的原支付方式：微信零钱/支付宝余额即时到账（1~10分钟）；储蓄卡借记卡通常为 1~3 个工作日；信用卡退款受发卡行账期影响通常在 3~7 个工作日到账。', '退款,到账,退款到账,退款时间,原路退回,退款周期,几天到账,退款多久到账,款项到账,退款几天', '订单售后', 1, 'kb-52', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (53, '学习进度超过 30% 是否就绝对不能申请退课？', '平台标准退款政策以“7 天内且学习小节进度 ≤ 30%”为系统自动审核线。若超过 30% 但因课程内容与讲师技术栈存在严重偏差等客观质量原因，学员可发起【人工特殊申诉】，由教务仲裁委员会核实情况后给出折算部分折旧学费退款或无缝置换同等面值其他技术方向课程的妥善方案。', '进度超30%,退费申诉,特殊退费,课程置换,学了超过30%,学了一半退款', '订单售后', 1, 'kb-53', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (54, '赠送了实体技术书籍或开发板的课程，退款时如何折算？', '购买附赠实体教具（如实战技术书籍、嵌入式开发板、周边礼盒等）的套餐班级若发起退款：若实体商品尚未发出，全额无扣除退款；若实体商品已寄出且未拆封，学员承担回寄顺丰运费后全额退款；若商品已拆封使用影响二次销售，退款金额中将扣除该实体商品的官方成本价（书籍按标价 6 折扣除）。', '赠品退款,实体书,开发板,扣除运费,退货,寄回赠品,教具退款', '订单售后', 1, 'kb-54', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (55, '保就业或保过协议班的退费考核标准与流程是怎样的？', '就业协议班退费严格按《就业保障培训服务协议》执行。通常要求：学员须在协议规定期限内完成 100% 课程学习、大作业成绩达到良好以上、按时参与不少于 5 次真实企业面试且非主观故意放弃 Offer。若在结课 180 天内未顺利入职符合协议年薪标准的岗位，学员可提交面试拒信记录申请全额或按协议比例退费，法务团队在 7 个工作日内核验办理。', '保就业,就业协议,协议班,协议班退费,就业退款,保过,退款条件,就业保障退款,退费标准', '订单售后', 1, 'kb-55', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (56, '提交退款申请后可以在哪里查看审核进度？', '登录后进入【个人中心】-【我的订单】- 点击该笔订单详情进入【售后退款记录】，页面将完整显示退款生命周期时间轴（申请已提交 ➔ 客服审核中 ➔ 财务退款中 ➔ 已原路退回银行），每个节点均有详细操作日志与审核意见备注。', '退款进度,退款查询,售后进度,退款状态,查看退款,退款审核到哪了', '订单售后', 1, 'kb-56', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (57, '购买错技术方向课程可以申请免费转班调换吗？', '购买课程 7 天之内且学习进度 ≤ 15% 的情况下，支持在【个人中心】-【我的订单】发起【免费转班置换】申请。若新课程价格高于原课程只需补齐差价；若新课程价格低于原课程，差价将以等额无门槛学习代金券形式返还至您的账户。每位学员每门课程仅限申请一次转班。', '转班,换课,课程调换,买错课程,更换课程,改学其他方向', '订单售后', 1, 'kb-57', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (58, '购买课程可以开具增值税专用发票吗？', '可以。平台支持开具增值税电子普通发票和增值税专用发票（专票）。企业开具增值税专用发票须提供一般纳税人资质证明、准确的公司全称、18 位统一社会信用代码、注册地址电话及开户银行账号。专票开具后将通过中通/顺丰快递纸质寄送或发送全电专票 XML/PDF 至指定邮箱。', '专票,专用发票,增值税专用发票,企业专票,发票资质,开专票,开增值税专用发票', '发票与财税', 1, 'kb-58', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (59, '提交发票申请后多久可以收到电子发票？', '增值税电子普通发票由税务航天金税接口自动开具，申请提交后 1~2 个工作日内即可完成税局核验，并以短信及邮件附件形式发送至您的预留邮箱。学员亦可随时在【个人中心】-【开票记录】中点击【下载电子发票 PDF】。', '发票时间,多久收到发票,电子发票下载,发票邮箱,发票推送,开票几天', '发票与财税', 1, 'kb-59', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (60, '课程发票的发票内容和开票类目是什么？', '按照国家税务总局现代服务业培训服务规范，课程开票内容统一默认为【*生活服务*非学历技术培训费】或【*信息技术服务*技术咨询培训服务费】。税率严格按国家相关规定执行，发票项目合规规范，完全满足企事业单位财务报销合规要求。', '发票内容,开票类目,发票类目,培训费,技术服务费,发票品目,报销类目', '发票与财税', 1, 'kb-60', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (61, '发票抬头或税号填错了，如何申请作废重开？', '若当月开具的电子发票尚未报销入账，进入【个人中心】-【我的发票】，在对应开票记录旁点击【申请红冲重开】，输入红字作废原因及正确的新发票抬头与税号，财务专员审核后会在 2 个工作日内向税局发起原发票红字冲销并开具正确的新发票。', '抬头,写错,填错,重开,作废,红冲,发票重开,重开发票,开错发票,抬头写错,发票开错,发票改抬头,发票抬头', '发票与财税', 1, 'kb-61', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (62, '多笔订单可以合并开具在同一张发票上吗？', '可以。在【个人中心】-【开票申请】中，勾选需要合并开票的多笔已支付且未开票订单，点击【合并开票】即可。系统将自动汇总订单总金额并生成一张发票，清单附页中会详细列出各笔子订单的课程明细，方便企业财务集中入账报销。', '多笔,合并,发票,一张,同一张,多笔订单,合并开票,发票合并,开一张发票,多笔发票', '发票与财税', 1, 'kb-62', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (63, '结业证书是纸质的还是电子证书？具有行业权威度吗？', '所有全阶段体系课程学完并考核通过后，均可免费申领官方电子结业证书。证书具有唯一防伪防篡改哈希编号及可追溯的二维码，支持在智问学伴官网全球核验。针对高阶架构师训练营学员，亦可申请邮寄由平台联合行业名企技术委员会共同盖章认证的纸质精装装裱证书。', '结业证书,权威度,防伪查询,证书真伪,纸质证书,证书查询,证书含金量,证书权威', '就业与认证', 1, 'kb-63', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (64, '就业指导服务包含哪些具体内容？何时可以启动？', '就业指导包含三大核心模块：1. 资深猎头/技术专家 1v1 深度简历剖析与亮点挖掘；2. 典型大厂高频算法与高并发架构题库在线模拟面试；3. 薪资架构剖析与薪资谈判策略辅导。通常在学员核心课程学习进度达到 80% 并完成综合实战项目后即可主动发起预约启动。', '就业指导,简历修改,模拟面试,薪资谈判,找工作,求职辅导,就业帮扶', '就业与认证', 1, 'kb-64', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (65, '平台的合作大厂内推绿色通道有哪些企业？如何获取内推码？', '智问学伴与包括阿里巴巴、腾讯、字节跳动、美团、快手、京东、华为、蚂蚁集团等 50+ 知名互联网一线大厂及独角兽企业建立了官方招聘与人才直送合作关系。结业综合评定达到 A 级以上的优秀学员，教务就业老师将直接通过企业直聘绿色通道直推用人部门技术负责人，免除初筛简历直接进入笔试/技术一面环节。', '大厂内推,内推码,招聘通道,直推技术大厦,内推,名企内推,内推资格', '就业与认证', 1, 'kb-65', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (66, '课程里完成的企业级实战项目可以写进个人求职简历吗？', '完全可以。课程中的企业级实战项目均基于真实一线大厂千万级业务场景（如千万级秒杀高可用系统、电商微服务全链路架构、低代码智能工作流等）深度提炼脱敏重构。讲师在配套课件中提供了详尽的【简历项目包装指导指南】，教您如何将项目难点（如 Redis 缓存雪崩治理、分库分表海量数据迁移、慢 SQL 调优等）转化为简历上的个人核心亮点。', '简历,实战项目,写进简历,求职简历,简历项目,项目经验,面试亮点,简历怎么写,项目经历,写简历', '就业与认证', 1, 'kb-66', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (67, '结业考核如果没有及格，可以补考吗？有几次机会？', '期末结业综合考核设有 2 次免费补考机会。如果首次考试成绩未达到合格标准（80分），系统将在 48 小时后自动开放补考入口，建议学员在此期间根据系统输出的薄弱知识点分析报告进行针对性查漏补缺。若 2 次补考均未及格，可联系班主任申请重置考核资格。', '结业考核,补考,考试不及格,考核次数,期末补考,重考', '就业与认证', 1, 'kb-67', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (68, '学员积分有哪些获取途径？积分会过期清零吗？', '获取积分途径包括：每日登录签到（+5积分）、连续打卡7天（+50额外积分）、完整学习完一个视频小节（+2积分）、在问答区积极回答他人技术疑问被采纳（+20积分）、发表优质原创学习笔记（+30积分）。积分长期有效，不会年底清零，可随时在积分商城兑换抵扣券或实体极客周边。', '积分,学霸积分,获取积分,打卡签到,积分清零,积分规则,怎么赚积分,积分过期,清零,过期清零', '优惠活动', 1, 'kb-68', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (69, '积分商城兑换的实体周边多久发货？运费谁承担？', '在【积分商城】成功兑换实体礼品（如定制机械键盘、降噪耳机、技术图书、极客卫衣等）后，仓库会在 3 个工作日内统一通过顺丰或中通快递顺风寄出，全国大陆地区（偏远地区除外）一律由平台包邮承运，无需学员额外支付运费。发货后订单详情中会实时同步快递运单号。', '积分换礼,实体发货,礼品发货,包邮,快递单号,周边发货,积分礼品', '优惠活动', 1, 'kb-69', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (70, '邀请好友报名学习有什么奖励？返现或积分如何到账？', '平台常年推行【技术合伙人·邀请有礼】活动。在【个人中心】-【邀请有礼】中复制专属推广海报或链接，好友通过您的专属链接注册并成功购买付费课程，好友立得 50 元新人直减券，您将获得该订单金额最高 15% 的高额佣金奖励或等额大额无门槛学习券。佣金支持直接提现至微信零钱或支付宝账户。', '好友,返利,佣金,提成,邀请,推荐,推荐好友,邀请好友,邀请返利,推广佣金,返现,买课提成', '优惠活动', 1, 'kb-70', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (71, '新用户注册有哪些专属新手大礼包？', '首次注册智问学伴的新学员，登录后系统将自动发放价值 888 元的【新人专属成长礼包】，包含：100 元无门槛实战课程满减券、高并发微服务核心面试白皮书（PDF电子版）、以及 7 天云端在线实验沙箱 VIP 体验特权。礼包可在【个人中心】-【卡券包】中直接查收。', '新用户,礼包,大礼包,新人,新手,优惠券,注册福利,新人礼包,新手礼包,新手专享,成长礼包', '优惠活动', 1, 'kb-71', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (72, '课程拼团活动的规则是怎样的？拼团失败会自动退款吗？', '拼团是学员组团享受优惠的特惠活动。发起或加入拼团后，在 24 小时内达到规定成团人数（通常为 2~3 人）即视为拼团成功，系统将立即开通所有拼团成员的课程权限。若 24 小时内未达到成团人数，系统将判定拼团失败，原路退还所有已支付款项至原支付账户。', '拼团,组团学习,拼团失败,自动退款,拼团规则,拼课', '优惠活动', 1, 'kb-72', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (73, '限时秒杀活动抢购的课程支持退款吗？', '限时秒杀商品属于平台超低折扣特惠福利。根据活动协议，标有【限时秒杀】标签的特惠课程购买后不支持 7 天无理由退款，但享有与其他正式课程完全一致的视频永久回放、讲师答疑和配套课件下载权益。请在秒杀抢购前仔细确认技术方向。', '秒杀,限时秒杀,秒杀退款,秒杀规则,特惠抢购', '优惠活动', 1, 'kb-73', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (74, '优惠券过期后可以申请补发或延期吗？', '普通活动优惠券具有明确的有效期，逾期未使用的券系统将自动失效无法恢复。但若是由于平台服务器维护、支付通道故障等官方原因导致您在优惠券有效期内未能成功下单，可联系客服核验并申请补发一张同等面值的新优惠券。', '优惠券过期,优惠券延期,券补发,优惠券失效,补发优惠券', '优惠活动', 1, 'kb-74', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (75, '人工客服的在线工作时间是几点到几点？', '智问学伴在线人工客服服务时间为：周一至周日 08:30 - 23:00（全年无休）。在工作时段内点击客服悬浮球选择【转人工】，即可秒级接入专业座席服务；非工作时段由 AI 智能客服小伴全天候 24 小时值守解答，复杂问题亦可留言并在次日优先处理。', '上班,工作时间,上班时间,几点上班,服务时间,在线时间,几点下班,人工客服,人工,找人工,人工几点', '客服服务', 1, 'kb-75', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (76, '对课程内容质量不满意如何发起正式投诉？', '平台高度重视教研教学质量。如您认为课程存在内容严重过时、代码跑不通且讲师不予解答等重大瑕疵，可在课程页面点击【投诉课程】或发送邮件至教研督察组邮箱（`supervision@zhiwen.com`）。教研专家仲裁组将在 2 个工作日内调取教学日志核实，并给予公开答复或退费处理。', '投诉课程,课程投诉,教学质量,教研督察,投诉讲师,教学不满意', '客服服务', 1, 'kb-76', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (77, '如何向平台提出功能改进建议或提交体验 Bug？', '在网页右下角点击【建议与反馈】按钮，选择【产品功能建议】或【系统 Bug 报告】，详细描述您的使用痛点并上传截图。每条被产品技术团队采纳的优质改进建议，平台将回赠 50~200 学霸积分或大额课程体验代金券以示谢意。', '建议与反馈,提建议,功能建议,提交Bug,意见反馈,平台建议', '客服服务', 1, 'kb-77', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (78, '如何成为智问学伴平台的签约技术讲师？', '智问学伴常年欢迎一线大厂技术骨干与行业专家加盟。申请条件：具有 5 年以上一线互联网研发架构经验、对前沿技术栈有深刻理解且具备良好的表达输出能力。进入官网底部【讲师招募】页面提交个人简历与意向课程大纲，教研总监会在 3 个工作日内与您沟通试讲与签约合作细节。', '讲师,签约,老师,成为,签约讲师,讲师招募,申请讲师,当老师,成为讲师,讲师入驻,讲师合作', '客服服务', 1, 'kb-78', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (79, '客服咨询的对话记录与聊天历史会保留多久？', '您与 AI 智能客服及人工客服的全部沟通咨询会话均在云端加密留存。在咨询窗口顶部点击【历史咨询记录】，可查阅最近 90 天内的全部会话日志、解决方案与客服发送的技术参考链接，方便随时复习和追溯售后处理凭据。', '聊天记录,历史咨询,咨询记录,会话记录,查看聊天历史', '客服服务', 1, 'kb-79', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();
INSERT INTO `cs_knowledge` (`id`, `question`, `answer`, `keywords`, `category`, `status`, `legacy_id`, `update_time`)
VALUES (80, '什么是教务仲裁委员会？何时会介入用户争议？', '教务仲裁委员会是由平台资深技术架构师、法务顾问及第三方学员代表共同组成的独立评议机构。当学员在退款退费、保就业协议兑现或严重教学纠纷中与常规客服沟通存在分歧时，均可向仲裁委员会申请独立公断，仲裁委员会的调查评议决定具有平台最高执行效力。', '教务仲裁,争议解决,仲裁委员会,学员争议,纠纷处理,投诉维权', '客服服务', 1, 'kb-80', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `status` = VALUES(`status`), `update_time` = NOW();

-- 2. 批量插入/更新前台常见高频问题 (cs_faq: 30条热门条目)
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (1, '如何注册智问学伴平台账号？', '点击右上角【注册】，输入手机号获取短信验证码并设置密码即可。亦支持微信扫码一键登录。', '账号与登录', 1, 1, 'faq-1', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (2, '课程购买后支持无理由退款吗？', '支持。购买 7 天内且已学小节进度不超过 30%，可在【我的订单】中直接申请 7 天无理由退款。', '订单售后', 2, 1, 'faq-2', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (3, '平台目前支持哪些在线支付渠道？', '支持微信支付、支付宝付款；满 300 元支持花呗与信用卡分期；企业支持公对公转账。', '订单支付', 3, 1, 'faq-3', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (4, '在哪里查看我已学课程的进度？', '登录后进入【个人中心】-【我的课程】，卡片直观显示当前进度百分比与已学小节数。', '课程学习', 4, 1, 'faq-4', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (5, '视频无法播放、黑屏卡顿怎么办？', '可尝试刷新页面、切换清晰度至 720P，或在浏览器设置中关闭硬件加速功能。', '课程学习', 5, 1, 'faq-5', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (6, '购买课程后如何开具增值税发票？', '进入【个人中心】-【发票管理】提交开票申请，支持电子普票和企业增值税专用发票。', '发票与财税', 6, 1, 'faq-6', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (7, '学完课程后如何获取结业证书？', '整门课程进度达到 100% 且课后测试及格后，系统自动生成可验真的官方电子结业证书。', '就业与认证', 7, 1, 'faq-7', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (8, '忘记登录密码如何找回与重置？', '在登录页点击【忘记密码】，通过绑定的手机号接收验证码完成身份验证后即可重设新密码。', '账号与登录', 8, 1, 'faq-8', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (9, '优惠券在哪里查看并在何时使用？', '在【个人中心】-【卡券包】查看；在课程确认订单结算页会自动抵扣或手动选择使用。', '优惠活动', 9, 1, 'faq-9', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (10, '智能客服与人工客服的服务范围有哪些？', '全面覆盖账号安全、课程播放、沙箱实训、技术答疑、订单支付、退款售后及发票证书咨询。', '客服服务', 10, 1, 'faq-10', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (11, '视频支持在手机或平板端离线缓存播放吗？', '支持。学员可通过移动端 App 打开已购课程，在小节列表点击【批量缓存】即可将视频加密下载到本地，在通勤或无网环境下随时流畅播放。', '课程学习', 11, 1, 'faq-11', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (12, '配套源码和课件讲义在什么位置下载？', '进入课程学习播放页面，在右侧标签栏点击【资料包/课件】，即可看到随堂代码 Git 仓库地址及讲义 PDF，支持一键打包下载。', '课程学习', 12, 1, 'faq-12', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (13, '云端实验沙箱无法连接或启动超时怎么办？', '请检查网络长连接是否被代理阻断，或点击沙箱控制台右上方【重启实验沙箱】重新分配容器。若长时间未操作，点击【重新激活连接】即可唤醒。', '在线实训与实验', 13, 1, 'faq-13', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (14, '学习遇到代码报错如何获得助教老师帮助？', '在视频小节下方点击【发起问答】，贴出完整报错堆栈日志与核心代码段，教研助教在工作时间（09:30-21:30）内平均 30 分钟内给出专业排查方案。', '技术答疑与导师', 14, 1, 'faq-14', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (15, '平台支持花呗分期或信用卡分期付款吗？', '支持。满 300 元订单在收银台选择支付宝或微信支付时，均可选择 3/6/12 期分期付款，部分活动课程享受平台免息补贴。', '订单支付', 15, 1, 'faq-15', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (16, '企业批量采购如何走公对公转账和签署合同？', '采购满 5000 元以上可享受大客户团购折扣并签署正规商业电子培训合同。请拨打企业专线或联系在线客服转接专属企业大客户经理协助办理。', '订单支付', 16, 1, 'faq-16', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (17, '课程购买后退款到账一般需要多长时间？', '退款审核通过后系统 10 分钟内发起退款指令：微信零钱/支付宝通常即时到账；银行储蓄卡 1~3 个工作日；信用卡退款通常在 3~7 个工作日内到账。', '订单售后', 17, 1, 'faq-17', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (18, '可以开具增值税专用发票（专票）用于公司报销吗？', '可以。在【个人中心】-【我的发票】中提交申请并提供一般纳税人资质、开户行信息即可，支持开具增值税专用发票并顺丰寄送或全电发送。', '发票与财税', 18, 1, 'faq-18', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (19, '发票抬头或税号填错了如何重新开具？', '未报销入账的当月发票，在【我的发票】点击【申请红冲重开】，财务专员审核后将在 2 个工作日内办理原发票红字冲销并重新开具正确发票。', '发票与财税', 19, 1, 'faq-19', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (20, '结业证书可以在哪些渠道核验证伪？', '每张结业证书均包含唯一的防伪数字编码与二维码，可直接在智问学伴官网输入证书编号全球查验，证书具备广泛的行业用人企业认可度。', '就业与认证', 20, 1, 'faq-20', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (21, '课程里的实战项目可以直接写在求职简历上吗？', '完全可以。实战项目均取材自一线大厂真实高可用业务架构并脱敏重构，课程内附赠详尽的项目简历包装与面试亮点应答攻略。', '就业与认证', 21, 1, 'faq-21', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (22, '签到打卡获取的学霸积分可以用来做什么？', '积分可用于在【积分商城】中兑换无门槛课程抵扣券、限量版极客文化衫、机械键盘及技术书籍等实体周边，平台全国包邮。', '优惠活动', 22, 1, 'faq-22', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (23, '为什么账号登录时提示“异地登录被限制”？', '系统检测到您的账号存在多异地 IP 同时发起课程播放，为保障账号安全与版权，平台自动触发了保护性拦截，可通过接收手机短信验证码完成身份验证并解封。', '账号与登录', 23, 1, 'faq-23', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (24, '购买就业保障集训营享受哪些求职权益？', '包含 1v1 简历深度重构、大厂模拟面试、名企技术负责人绿色通道直推、导师 1v1 屏幕共享调代码以及结课后 180 天就业全周期贴身跟踪。', '就业与认证', 24, 1, 'faq-24', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (25, '如何加入课程专属的技术交流微信或 QQ 群？', '支付成功后在订单完成页或在【我的课程】卡片点击【班级社群】，即可查看最新微信群二维码与班主任联系方式。', '技术答疑与导师', 25, 1, 'faq-25', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (26, '错过了讲师每周的技术直播课在哪里看回放？', '直播结束后 24 小时内，系统自动将剪辑后的高清视频上架至课程的【直播回放】章节，永久有效支持倍速复看。', '技术答疑与导师', 26, 1, 'faq-26', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (27, '未支付的订单会保留多久？超时会被取消吗？', '未支付订单在提交后系统锁定优惠名额 30 分钟，若超过 30 分钟未付款系统将自动取消订单并释放优惠券。', '订单支付', 27, 1, 'faq-27', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (28, '实训沙箱里写好的代码关闭浏览器后会丢失吗？', '不会。沙箱挂载分布式持久化云盘并具备自动保存机制，24 小时内再次启动实验即可无缝恢复所有代码文件。', '在线实训与实验', 28, 1, 'faq-28', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (29, '人工客服的在线服务时间是几点到几点？', '人工客服在线服务时间为周一至周日 08:30 - 23:00 全年无休；其他时段由 AI 客服智能解答或支持在线留言。', '客服服务', 29, 1, 'faq-29', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();
INSERT INTO `cs_faq` (`id`, `question`, `answer`, `category`, `sort_num`, `enabled`, `legacy_id`, `update_time`)
VALUES (30, '什么是技术合伙人邀请有礼活动？如何拿佣金？', '在【个人中心】-【邀请有礼】分享专属链接，好友注册购买课程好友享减免，您可获得最高 15% 现金返现或等额代金券。', '优惠活动', 30, 1, 'faq-30', NOW())
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `category` = VALUES(`category`), `sort_num` = VALUES(`sort_num`), `enabled` = VALUES(`enabled`), `update_time` = NOW();