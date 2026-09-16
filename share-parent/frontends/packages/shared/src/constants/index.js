/**
 * 智问学伴全域前端通用常量与枚举 (Global Shared Constants & Enums)
 */

// 会话与鉴权
export const TOKEN_NAME = 'TOKEN';
export const REFRESH_TOKEN_NAME = 'REFRESH_TOKEN';
export const USER_INFO_KEY = 'USER_INFO';

// 默认资源路径
export const DEFAULT_AVATAR = '/src/assets/images/users/default-avatar.svg';
export const DEFAULT_COVER = '/src/assets/images/courses/default-cover.svg';

// 用户身份角色
export const USER_ROLES = {
  ADMIN: 'admin',
  STUDENT: '01',
  TEACHER: '02'
};

// 模拟面试流程环节
export const INTERVIEW_ROUNDS = {
  INTRO: 1,      // 环节一：自我介绍
  BASIC: 2,      // 环节二：基础八股（小林coding真题）
  PROJECT: 3     // 环节三：项目深入剖析
};

// 订单与交易状态
export const ORDER_STATUS = {
  UNPAID: 0,
  PAID: 1,
  CANCELLED: 2,
  REFUNDING: 3,
  CLOSED: 4
};
