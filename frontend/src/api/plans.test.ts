import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '../utils/http'
import { planApi } from './plans'

vi.mock('../utils/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('planApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('creates a plan and changes its status', async () => {
    const payload = {
      title: '上海文化研学',
      city: '上海',
      startDate: '2026-09-01',
      endDate: '2026-09-02',
    }
    vi.mocked(http.post).mockResolvedValue({ data: { data: { id: 9 } } } as never)
    vi.mocked(http.patch).mockResolvedValue({ data: { data: { id: 9, status: 'IN_PROGRESS' } } } as never)

    await planApi.create(payload)
    await planApi.changeStatus(9, 'IN_PROGRESS')

    expect(http.post).toHaveBeenCalledWith('/plans', payload)
    expect(http.patch).toHaveBeenCalledWith('/plans/9/status', { status: 'IN_PROGRESS' })
  })

  it('submits the complete route order', async () => {
    vi.mocked(http.put).mockResolvedValue({ data: { data: [] } } as never)

    await planApi.reorder(9, [3, 2, 1])

    expect(http.put).toHaveBeenCalledWith('/plans/9/items/reorder', { itemIds: [3, 2, 1] })
  })

  it('requests an AI route with the daily time window', async () => {
    const payload = { desiredPlaces: 4 as const, dailyStartTime: '09:00:00', dailyEndTime: '17:00:00' }
    vi.mocked(http.post).mockResolvedValue({ data: { data: { fallback: true } } } as never)

    await planApi.generateAiRoute(9, payload)

    expect(http.post).toHaveBeenCalledWith('/plans/9/ai-route', payload)
  })
})
