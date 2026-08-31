import type { ApiResponse, AuthResult, User } from '../types/api'
import { http } from '../utils/http'

export const authApi = {
  async login(payload: { username: string; password: string }) {
    return (await http.post<ApiResponse<AuthResult>>('/auth/login', payload)).data.data
  },
  async register(payload: { username: string; password: string; nickname: string }) {
    return (await http.post<ApiResponse<AuthResult>>('/auth/register', payload)).data.data
  },
  async me() {
    return (await http.get<ApiResponse<User>>('/users/me')).data.data
  },
}

