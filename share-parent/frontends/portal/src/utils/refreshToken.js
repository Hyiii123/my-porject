import { tryRefreshToken as sharedTryRefreshToken } from '@zhiwen/shared';

const defaultBaseURL = import.meta.env.MODE === 'production' ? '' : 'http://localhost:8080';
const host = (import.meta.env.VITE_API_BASE_URL || defaultBaseURL).replace(/\/$/, '');

/**
 * 委托至 @zhiwen/shared 共享模块执行无感刷新令牌
 */
export async function tryRefreshToken() {
  return sharedTryRefreshToken(host);
}
