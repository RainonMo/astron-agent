import { request } from '@/utils/request';
import type { WechatBotConfig } from '@/types/wechat-bot';

// 分页查询机器人配置
export async function getWechatBotPage(params: {
  current: number;
  size: number;
  keyword?: string;
}) {
  return request.get<{
    records: WechatBotConfig[];
    total: number;
    current: number;
    size: number;
  }>('/api/wechat-bot/page', { params });
}

// 根据ID查询机器人配置
export async function getWechatBotById(id: number) {
  return request.get<WechatBotConfig>(`/api/wechat-bot/${id}`);
}

// 创建机器人配置
export async function createWechatBot(data: Partial<WechatBotConfig>) {
  return request.post<boolean>('/api/wechat-bot', data);
}

// 更新机器人配置
export async function updateWechatBot(data: Partial<WechatBotConfig>) {
  return request.put<boolean>('/api/wechat-bot', data);
}

// 删除机器人配置
export async function deleteWechatBot(id: number) {
  return request.delete<boolean>(`/api/wechat-bot/${id}`);
}

// 生成机器人唯一标识
export async function generateBotKey() {
  return request.get<string>('/api/wechat-bot/generate-key');
}