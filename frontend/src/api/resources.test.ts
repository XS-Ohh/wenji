import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '../utils/http'
import { resourceApi } from './resources'

vi.mock('../utils/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('resourceApi favorites', () => {
  beforeEach(() => vi.clearAllMocks())

  it('uses the favorite endpoints', async () => {
    vi.mocked(http.post).mockResolvedValue({} as never)
    vi.mocked(http.delete).mockResolvedValue({} as never)

    await resourceApi.favorite(7)
    await resourceApi.unfavorite(7)

    expect(http.post).toHaveBeenCalledWith('/resources/7/favorite')
    expect(http.delete).toHaveBeenCalledWith('/resources/7/favorite')
  })

  it('returns the favorite resource list', async () => {
    const resources = [{ id: 7, name: '测试资源', favorited: true }]
    vi.mocked(http.get).mockResolvedValue({ data: { data: resources } } as never)

    await expect(resourceApi.favorites()).resolves.toEqual(resources)
    expect(http.get).toHaveBeenCalledWith('/favorites')
  })

  it('loads lightweight resources for the culture map', async () => {
    const resources = [{ id: 7, name: '上海博物馆', longitude: 121.47, latitude: 31.23 }]
    vi.mocked(http.get).mockResolvedValue({ data: { data: resources } } as never)

    await expect(resourceApi.map({ city: '上海', categoryId: 1 })).resolves.toEqual(resources)
    expect(http.get).toHaveBeenCalledWith('/resources/map', { params: { city: '上海', categoryId: 1 } })
  })
})
