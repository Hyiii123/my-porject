import Layout from '@/pages/layouts/index.vue';

export default [
  {
    path: '/customer-service',
    name: 'customerService',
    component: Layout,
    redirect: '/customer-service/index',
    meta: { title: '客服中心' },
    children: [
      {
        path: 'index',
        name: 'customerServiceIndex',
        component: () => import('@/pages/customerService/index.vue'),
        meta: { title: '客服中心' },
      },
    ],
  },
  {
    path: '/interview',
    name: 'interview',
    component: Layout,
    redirect: '/interview/index',
    meta: { title: 'AI 模拟面试' },
    children: [
      {
        path: 'index',
        name: 'interviewIndex',
        component: () => import('@/pages/interview/index.vue'),
        meta: { title: 'AI 模拟面试大厅' },
      },
      {
        path: 'room/:id',
        name: 'interviewRoom',
        component: () => import('@/pages/interview/room.vue'),
        meta: { title: '全真模拟面试间' },
      },
      {
        path: 'report/:id',
        name: 'interviewReport',
        component: () => import('@/pages/interview/report.vue'),
        meta: { title: '多维能力诊断报告' },
      },
    ],
  },
  {
    path: '/my-class',
    name: 'myClass',
    component: Layout,
    redirect: '/my-class/index',
    meta: { title: '我的课表', icon: 'reading' },
    children: [
      {
        path: 'index',
        name: 'MyClassIndex',
        component: () => import('@/pages/myClass/index.vue'),
        meta: { title: '我的课表' },
      },
    ],
  },
  {
    path: '/ask',
    name: 'ask',
    component: Layout,
    redirect: '/ask/index',
    meta: { title: '发布、编辑问答'},
    children: [
      {
        path: 'index',
        name: 'ask',
        component: () => import('@/pages/ask/index.vue'),
        meta: { title: '发布问题' },
      }
    ],
  },
  {
    path: '/points',
    name: 'points',
    component: Layout,
    redirect: '/points/index',
    meta: { title: '积分中心', icon: 'star' },
    children: [
      {
        path: 'index',
        name: 'PointsIndex',
        component: () => import('@/pages/points/index.vue'),
        meta: { title: '积分排行' },
      },
    ],
  },
  {
    path: '/notes',
    name: 'notes',
    component: Layout,
    redirect: '/notes/index',
    meta: { title: '我的笔记', icon: 'edit' },
    children: [
      {
        path: 'index',
        name: 'NotesIndex',
        component: () => import('@/pages/notes/index.vue'),
        meta: { title: '笔记管理' },
      },
    ],
  },
  {
    path: '/result',
    name: 'result',
    component: Layout,
    redirect: '/result/success',
    meta: { title: '结果页', icon: 'check-circle' },
    children: [
      {
        path: 'success',
        name: 'ResultSuccess',
        component: () => import('@/pages/result/success/index.vue'),
        meta: { title: '成功页' },
      },
      {
        path: '404',
        name: 'Result404',
        component: () => import('@/pages/result/404/index.vue'),
        meta: { title: '访问页面不存在页' },
      },
    ],
  },
];
