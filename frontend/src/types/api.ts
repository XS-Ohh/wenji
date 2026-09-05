export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface PageResponse<T> {
  records: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface User {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  role: 'STUDENT' | 'ADMIN'
  points: number
  level: number
}

export interface AuthResult {
  token: string
  tokenType: 'Bearer'
  expiresIn: number
  user: User
}

export interface Category {
  id: number
  name: string
  icon?: string
  defaultCover?: string
  sortOrder: number
}

export interface CultureResource {
  id: number
  categoryId: number
  categoryName: string
  name: string
  city: string
  district?: string
  address: string
  summary?: string
  description?: string
  openingHours?: string
  ticketInfo?: string
  recommendedMinutes: number
  coverImage?: string
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
  viewCount: number
  favoriteCount: number
  favorited: boolean
  averageRating: number
}

export interface CultureMapResource {
  id: number
  categoryId: number
  categoryName: string
  name: string
  city: string
  district?: string
  address: string
  longitude?: number
  latitude?: number
  summary?: string
  openingHours?: string
  coverImage?: string
  favoriteCount: number
  averageRating: number
}

export type PlanStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface PlanItem {
  id: number
  resourceId: number
  resourceName: string
  resourceAddress: string
  coverImage?: string
  visitDate?: string
  startTime?: string
  endTime?: string
  sortOrder: number
  transportation?: string
  reason?: string
  status: string
}

export interface StudyPlan {
  id: number
  title: string
  city: string
  startDate: string
  endDate: string
  budget?: number
  interests?: string
  startLocation?: string
  status: PlanStatus
  progress: number
  aiGenerated: boolean
  itemCount: number
  items: PlanItem[]
  createdAt?: string
  updatedAt?: string
}

export interface AiRouteItem {
  resourceId: number
  startTime: string
  endTime: string
  transportation?: string
  reason?: string
}

export interface AiRouteDay {
  date: string
  items: AiRouteItem[]
}

export interface AiRouteResult {
  title: string
  summary: string
  estimatedBudget: number
  days: AiRouteDay[]
  tips: string[]
  fallback: boolean
  plan: StudyPlan
}

export type CheckinStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface Checkin {
  id: number
  userId: number
  userNickname?: string
  planId?: number
  planTitle?: string
  resourceId: number
  resourceName: string
  resourceAddress: string
  checkinTime: string
  imageUrl?: string
  content?: string
  status: CheckinStatus
  auditComment?: string
  auditedBy?: number
  auditorNickname?: string
  auditedAt?: string
  createdAt: string
}
