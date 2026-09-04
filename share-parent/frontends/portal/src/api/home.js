import request from '@/utils/request.js'

// 首页内容统一从教育服务读取，不在页面脚本中维护课程或 Banner 数组。
export const getBanners = () => request({
  url: '/cs/banners',
  method: 'get'
})
