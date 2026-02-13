import axios from './http';

// 为了兼容旧的导入方式，导出 axios 实例作为 request
export const request = axios;

export default axios;