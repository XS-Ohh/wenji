import type { ApiResponse, Checkin, CheckinStatus } from '../types/api'
import { http } from '../utils/http'

export interface CheckinPayload {
  planId?: number
  resourceId: number
  imageUrl?: string
  content?: string
}

export const checkinApi = {
  async listMine() {
    return (await http.get<ApiResponse<Checkin[]>>('/checkins')).data.data
  },
  async create(payload: CheckinPayload) {
    return (await http.post<ApiResponse<Checkin>>('/checkins', payload)).data.data
  },
  async uploadImage(file: File) {
    const form = new FormData()
    form.append('file', file)
    return (await http.post<ApiResponse<{ url: string }>>('/uploads/images', form)).data.data
  },
  async listForReview(status?: CheckinStatus) {
    return (await http.get<ApiResponse<Checkin[]>>('/admin/checkins', { params: { status } })).data.data
  },
  async review(id: number, status: 'APPROVED' | 'REJECTED', auditComment?: string) {
    return (await http.patch<ApiResponse<Checkin>>(`/admin/checkins/${id}/review`, { status, auditComment })).data.data
  },
}
