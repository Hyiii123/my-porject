import Layout from '@/pages/layouts/index.vue'

export default [
  {
    path: '/customer-service',
    component: Layout,
    redirect: '/customer-service/index',
    name: 'customerService',
    meta: { title: 'AI客服管理', icon: '&#xe60e;' },
    children: [
      {
        path: 'index',
        name: 'customerServiceIndex',
        component: () => import('@/pages/customer-service/index.vue'),
        meta: { title: '客服工作台' },
      },
    ],
  },
]
