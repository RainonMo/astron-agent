export interface WechatBotConfig {
  id?: number;
  botKey: string;
  corpId: string;
  agentId: string;
  token: string;
  encodingAesKey: string;
  agentIdRef: string;
  callbackUrl?: string;
  isActive?: boolean;
  createTime?: string;
  updateTime?: string;
}

export interface WechatBotPageParams {
  current: number;
  size: number;
  keyword?: string;
}

export interface WechatBotPageResponse {
  records: WechatBotConfig[];
  total: number;
  current: number;
  size: number;
}