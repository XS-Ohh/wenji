import type { ApiResponse, Category, CultureMapResource, CultureResource, PageResponse } from '../types/api'
import { http } from '../utils/http'

export interface ResourceQuery {
  page: number
  pageSize: number
  keyword?: string
  categoryId?: number
  city?: string
}

export interface MapResourceQuery {
  keyword?: string
  categoryId?: number
  city?: string
}

export const resourceApi = {
  async categories() {
    return (await http.get<ApiResponse<Category[]>>('/categories')).data.data
  },
  async list(params: ResourceQuery) {
    return (await http.get<ApiResponse<PageResponse<CultureResource>>>('/resources', { params })).data.data
  },
  async detail(id: number) {
    return (await http.get<ApiResponse<CultureResource>>(`/resources/${id}`)).data.data
  },
  async map(params: MapResourceQuery) {
    return (await http.get<ApiResponse<CultureMapResource[]>>('/resources/map', { params })).data.data
  },
  async favorite(id: number) {
    await http.post(`/resources/${id}/favorite`)
  },
  async unfavorite(id: number) {
    await http.delete(`/resources/${id}/favorite`)
  },
  async favorites() {
    return (await http.get<ApiResponse<CultureResource[]>>('/favorites')).data.data
  },
}
