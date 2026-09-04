// Mock 适配器 - 使用新的处理器系统
import { handleRequest } from './handler'

// 创建 Mock 适配器
export function createMockAdapter() {
  return function mockAdapter(config) {
    return new Promise((resolve, reject) => {
      // 解析 URL
      let url = config.url || ''
      let method = (config.method || 'get').toUpperCase()

      // 处理 baseURL - 移除 baseURL，只保留相对路径
      if (config.baseURL && !url.startsWith('http')) {
        // 移除 baseURL 前缀
        url = url.replace(config.baseURL, '')
      }

      // 确保 url 以 / 开头
      if (!url.startsWith('/')) {
        url = '/' + url
      }

      // 解析请求数据
      let requestData = null
      if (config.data) {
        try {
          requestData = typeof config.data === 'string' ? JSON.parse(config.data) : config.data
        } catch {
          requestData = config.data
        }
      }

      // 合并查询参数
      if (config.params) {
        requestData = { ...requestData, ...config.params }
      }

      console.log(`[Mock] ${method} ${url}`, requestData)

      // 调用处理器
      handleRequest(url, method, requestData)
        .then(result => {
          console.log(`[Mock] Response:`, result)
          resolve({
            data: result,
            status: 200,
            statusText: 'OK',
            headers: { 'content-type': 'application/json' },
            config: config
          })
        })
        .catch(error => {
          console.error('[Mock] Error:', error)
          reject(error)
        })
    })
  }
}

// 应用 Mock 适配器
export function applyMockAdapter(axiosInstance) {
  console.log('[Mock] Applying mock adapter...')
  axiosInstance.defaults.adapter = createMockAdapter()
  console.log('[Mock] ✅ Mock adapter applied successfully')
}
