import axios from 'axios';
import { TOKEN_NAME } from '../constants/index.js';

const sleep = (delay) => new Promise((resolve) => setTimeout(resolve, delay));
let isRefresh = false;
let success = false;

/**
 * 获取当前用户令牌
 */
export function getToken() {
  return sessionStorage.getItem(TOKEN_NAME);
}

/**
 * 存储用户令牌
 */
export function setToken(token) {
  sessionStorage.setItem(TOKEN_NAME, token);
}

/**
 * 移除用户令牌
 */
export function removeToken() {
  sessionStorage.removeItem(TOKEN_NAME);
}

/**
 * 全局统一无感刷新令牌 (Silent Token Refresh)
 *
 * @param {string} customHost 自定义网关地址，默认按同源空前缀或本地8080探测
 * @returns {Promise<boolean>} 是否刷新成功
 */
export async function tryRefreshToken(customHost) {
  if (isRefresh) {
    while (isRefresh) {
      await sleep(10);
    }
    return success;
  }
  isRefresh = true;
  try {
    const token = getToken();
    // 没有旧令牌时无法刷新，避免向受保护的刷新接口发送匿名请求
    if (!token) {
      success = false;
      return success;
    }

    const host = customHost !== undefined ? customHost.replace(/\/$/, '') : '';
    const resp = await axios.get(host + '/as/accounts/refresh', {
      headers: { Authorization: token },
      withCredentials: false,
    });

    const refreshedToken =
      resp.status === 200 && resp.data?.code === 200
        ? resp.data.data?.access_token || resp.data.data?.token || resp.data.data
        : null;

    if (refreshedToken) {
      setToken(refreshedToken);
      success = true;
    } else {
      removeToken();
      success = false;
    }
  } catch (error) {
    removeToken();
    success = false;
  } finally {
    isRefresh = false;
  }
  return success;
}
