-- 智问学伴：全站慢 SQL 深度优化与核心高频查询覆盖索引迁移 (V34)
-- 彻底消除全表扫描 (type: ALL) 与文件排序 (Using filesort)

SET NAMES utf8mb4;

-- ========================================================
-- 1. 系统与鉴权库 (share) - 根除 5000 用户量级全表扫描
-- ========================================================
-- 用户名+未删除唯一索引（高频登录鉴权与验重，降维为 const 级别）
CREATE UNIQUE INDEX uk_sys_user_username_del ON share.sys_user (user_name, del_flag);
-- 邮箱+未删除索引（邮箱验证码极速登录）
CREATE INDEX idx_sys_user_email_del ON share.sys_user (email, del_flag);
-- 手机号+未删除索引
CREATE INDEX idx_sys_user_phone_del ON share.sys_user (phonenumber, del_flag);
-- 部门+状态复合索引（后台用户列表分页检索）
CREATE INDEX idx_sys_user_dept_status ON share.sys_user (dept_id, status, del_flag);
-- 状态+用户类型复合索引（学员管理/教师筛选）
CREATE INDEX idx_sys_user_type_status ON share.sys_user (user_type, status, del_flag);
-- 角色关联表按角色查询索引（解决查教师/学员角色时的 5123 行全表扫描）
CREATE INDEX idx_sys_user_role_role_id ON share.sys_user_role (role_id);
-- 登录日志用户检索复合索引
CREATE INDEX idx_sys_logininfor_user_time ON share.sys_logininfor (user_name, access_time);

-- ========================================================
-- 2. 教育核心库 (zhiwen_education) - 消除大厅/小节/学习进度 filesort
-- ========================================================
-- 课程大厅默认无分类排序覆盖索引（消除 filesort）
CREATE INDEX idx_course_status_sort_asc_time_desc ON zhiwen_education.edu_course (status, sort_num ASC, create_time DESC);
-- 免费课程筛选复合索引
CREATE INDEX idx_course_status_free ON zhiwen_education.edu_course (status, is_free, sort_num);
-- 热门课程筛选复合索引
CREATE INDEX idx_course_status_hot ON zhiwen_education.edu_course (status, is_hot, sort_num);
-- 目录小节按课程与序号快速扫描索引（解决播放器与详情页跳过 parent_id 时的 filesort）
CREATE INDEX idx_catalog_course_sort_parent ON zhiwen_education.edu_course_catalog (course_id, sort_num, parent_id);
-- 学习记录多列排序复合索引（解决首页与个人中心 last_learn_time 与 update_time 倒序时的 filesort）
CREATE INDEX idx_learning_user_learn_update ON zhiwen_education.edu_learning_record (user_id, last_learn_time, update_time);
-- 用户单课学习记录极速定位复合索引
CREATE INDEX idx_learning_user_course_update ON zhiwen_education.edu_learning_record (user_id, course_id, update_time);
-- 教师课程关联表覆盖索引
CREATE INDEX idx_course_teacher_teacher_course ON zhiwen_education.edu_course_teacher (teacher_id, course_id);
-- 问答社区与回复通用时间索引
CREATE INDEX idx_question_create_time ON zhiwen_education.edu_question (create_time);
CREATE INDEX idx_reply_create_time ON zhiwen_education.edu_reply (create_time);

-- ========================================================
-- 3. 交易中心库 (zhiwen_trade) - 消除订单/退款列表全表扫描
-- ========================================================
-- 订单全局创建时间索引（管理端订单列表默认时间倒序命中）
CREATE INDEX idx_order_create_time ON zhiwen_trade.tr_order (create_time);
-- 用户各状态订单时间复合索引（用户端订单列表分类极速分页）
CREATE INDEX idx_order_user_status_time ON zhiwen_trade.tr_order (user_id, order_status, create_time);
-- 订单删除标记与创建时间索引
CREATE INDEX idx_order_del_create ON zhiwen_trade.tr_order (del_flag, create_time);
-- 退款申请创建时间与状态索引（管理端退款管理页面默认倒序命中）
CREATE INDEX idx_refund_create_time ON zhiwen_trade.tr_refund_apply (create_time);
CREATE INDEX idx_refund_status_time ON zhiwen_trade.tr_refund_apply (status, create_time);
-- 支付订单状态与关联索引
CREATE INDEX idx_payment_order_time ON zhiwen_trade.tr_payment_order (order_id, status);

-- ========================================================
-- 4. 智能客服与知识库 (zhiwen_customer) - 9000条知识库检索加速
-- ========================================================
-- 知识库分类+状态+更新时间复合覆盖索引（完全消除 9189 条知识库分类检索 filesort）
CREATE INDEX idx_knowledge_cat_status_time ON zhiwen_customer.cs_knowledge (category, status, update_time);
-- 知识库全局更新时间索引
CREATE INDEX idx_knowledge_status_time ON zhiwen_customer.cs_knowledge (status, update_time);
-- 客服会话列表更新时间索引（管理端会话大厅默认倒序命中）
CREATE INDEX idx_session_updated_at ON zhiwen_customer.cs_session (updated_at);
