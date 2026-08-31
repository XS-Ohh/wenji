import type { ApiResponse, PlanItem, StudyPlan } from '../types/api'
import { http } from '../utils/http'

export interface PlanPayload {
  title: string
  city: string
  startDate: string
  endDate: string
  budget?: number
  interests?: string
  startLocation?: string
}

export interface PlanItemPayload {
  resourceId: number
  visitDate?: string
  startTime?: string
  endTime?: string
  sortOrder?: number
  transportation?: string
  reason?: string
}

export const planApi = {
  async list() {
    return (await http.get<ApiResponse<StudyPlan[]>>('/plans')).data.data
  },
  async detail(id: number) {
    return (await http.get<ApiResponse<StudyPlan>>(`/plans/${id}`)).data.data
  },
  async create(payload: PlanPayload) {
    return (await http.post<ApiResponse<StudyPlan>>('/plans', payload)).data.data
  },
  async update(id: number, payload: PlanPayload) {
    return (await http.put<ApiResponse<StudyPlan>>(`/plans/${id}`, payload)).data.data
  },
  async remove(id: number) {
    await http.delete(`/plans/${id}`)
  },
  async changeStatus(id: number, status: StudyPlan['status']) {
    return (await http.patch<ApiResponse<StudyPlan>>(`/plans/${id}/status`, { status })).data.data
  },
  async addItem(planId: number, payload: PlanItemPayload) {
    return (await http.post<ApiResponse<PlanItem>>(`/plans/${planId}/items`, payload)).data.data
  },
  async updateItem(planId: number, itemId: number, payload: PlanItemPayload) {
    return (await http.put<ApiResponse<PlanItem>>(`/plans/${planId}/items/${itemId}`, payload)).data.data
  },
  async removeItem(planId: number, itemId: number) {
    await http.delete(`/plans/${planId}/items/${itemId}`)
  },
  async reorder(planId: number, itemIds: number[]) {
    return (await http.put<ApiResponse<PlanItem[]>>(`/plans/${planId}/items/reorder`, { itemIds })).data.data
  },
}
