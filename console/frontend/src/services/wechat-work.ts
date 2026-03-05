import http from '@/utils/http';

export interface WechatWorkSyncUser {
  id: number;
  userid: string;
  department: number;
  phone: string | null;
  syncBatchId: string;
  syncStatus: number;
  errorMsg: string | null;
  createTime: string;
  updateTime: string;
}

/**
 * 获取企微配置
 */
export async function getWechatWorkConfig() {
  return http.get('/hub/wechat-work/config');
}

/**
 * 保存企微配置
 */
export async function saveWechatWorkConfig(params: { corpid: string; corpsecret: string }) {
  return http.post(`/hub/wechat-work/config?corpid=${params.corpid}&corpsecret=${params.corpsecret}`);
}

/**
 * 同步企业微信用户
 */
export async function syncWechatWorkUsers() {
  return http.post('/hub/wechat-work/sync');
}

/**
 * 获取同步用户列表
 */
export async function getWechatWorkSyncUsers(): Promise<WechatWorkSyncUser[]> {
  return http.get('/hub/wechat-work/users');
}

/**
 * 新增Casdoor用户
 * @param userId 用户ID
 * @param phone 手机号
 */
export async function addCasdoorUser(userId: string, phone: string) {
  return http.post('/hub/wechat-work/add-user', { userId, phone });
}
