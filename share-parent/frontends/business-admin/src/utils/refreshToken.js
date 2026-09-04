import axios from 'axios';
import {USER_KEY, TOKEN_NAME} from "../config/global";
const host = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

const sleep = (delay) => new Promise((resolve) => setTimeout(resolve, delay))
let isRefresh = false;
let success = false;
export async function tryRefreshToken(){
  if(isRefresh){
    while (isRefresh){
      await sleep(10)
    }
    return success;
  }
  isRefresh = true;
  try {
    const token = sessionStorage.getItem(TOKEN_NAME);
    // 没有旧令牌时无法刷新，避免向受保护的刷新接口发送匿名请求。
    if (!token) {
      success = false;
      return success;
    }
    const resp = await axios.get(host + "/as/accounts/refresh", {
      headers: { Authorization: token },
      withCredentials: false,
    });
    const refreshedToken = resp.status === 200 && resp.data?.code === 200
      ? (resp.data.data?.access_token || resp.data.data?.token || resp.data.data)
      : null;
    if (refreshedToken) {
      sessionStorage.setItem(TOKEN_NAME, refreshedToken);
      success = true;
    } else {
      sessionStorage.removeItem(TOKEN_NAME);
      success = false;
    }
  } catch (error) {
    sessionStorage.removeItem(TOKEN_NAME);
    success = false;
  } finally {
    isRefresh = false;
  }
  return success;
}
