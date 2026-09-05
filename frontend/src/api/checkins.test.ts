import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '../utils/http'
import { checkinApi } from './checkins'

vi.mock('../utils/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

describe('checkinApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('uploads an image then submits a checkin', async () => {
    const file = new File(['image'], 'footprint.png', { type: 'image/png' })
    vi.mocked(http.post)
      .mockResolvedValueOnce({ data: { data: { url: '/api/uploads/images/one.png' } } } as never)
      .mockResolvedValueOnce({ data: { data: { id: 8 } } } as never)

    const image = await checkinApi.uploadImage(file)
    await checkinApi.create({ planId: 3, resourceId: 6, imageUrl: image.url, content: '文化观察' })

    const form = vi.mocked(http.post).mock.calls[0][1] as FormData
    expect(http.post).toHaveBeenNthCalledWith(1, '/uploads/images', expect.any(FormData))
    expect(form.get('file')).toBe(file)
    expect(http.post).toHaveBeenNthCalledWith(2, '/checkins', {
      planId: 3,
      resourceId: 6,
      imageUrl: '/api/uploads/images/one.png',
      content: '文化观察',
    })
  })

  it('lists pending records and reviews one', async () => {
    vi.mocked(http.get).mockResolvedValue({ data: { data: [] } } as never)
    vi.mocked(http.patch).mockResolvedValue({ data: { data: { id: 8, status: 'APPROVED' } } } as never)

    await checkinApi.listForReview('PENDING')
    await checkinApi.review(8, 'APPROVED', '内容真实完整')

    expect(http.get).toHaveBeenCalledWith('/admin/checkins', { params: { status: 'PENDING' } })
    expect(http.patch).toHaveBeenCalledWith('/admin/checkins/8/review', {
      status: 'APPROVED',
      auditComment: '内容真实完整',
    })
  })
})
