/**
 * 统一日期时间工具函数 (Date and Time Utilities)
 */

/**
 * 格式化时间为 YYYY-MM-DD HH:mm:ss 或自定义格式
 * @param {Date|number|string} date
 * @param {string} format 格式模板，默认 'YYYY-MM-DD HH:mm:ss'
 */
export function formatDateTime(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return '';
  const d = new Date(date);
  if (isNaN(d.getTime())) return String(date);

  const pad = (n) => String(n).padStart(2, '0');
  const year = d.getFullYear();
  const month = pad(d.getMonth() + 1);
  const day = pad(d.getDate());
  const hours = pad(d.getHours());
  const minutes = pad(d.getMinutes());
  const seconds = pad(d.getSeconds());

  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds);
}

/**
 * 格式化为仅日期 YYYY-MM-DD
 */
export function formatDate(date) {
  return formatDateTime(date, 'YYYY-MM-DD');
}

/**
 * 人性化时间展示 (如 "5分钟前", "2小时前", "昨天")
 */
export function timeAgo(date) {
  if (!date) return '';
  const d = new Date(date);
  if (isNaN(d.getTime())) return '';

  const now = Date.now();
  const diff = (now - d.getTime()) / 1000;

  if (diff < 60) return '刚刚';
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`;
  if (diff < 2592000) return `${Math.floor(diff / 86400)} 天前`;
  return formatDate(d);
}
