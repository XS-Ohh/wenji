import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from '../src/api/auth'
import { useAuthStore } from '../src/stores/auth'

vi.mock('../src/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
  },
}))

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('persists a successful login and clears it on logout', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'signed-token',
      tokenType: 'Bearer',
      expiresIn: 7200,
      user: { id: 2, username: 'student', nickname: '研学同学', role: 'STUDENT', points: 0, level: 1 },
    })
    const store = useAuthStore()

    await store.login('student', 'Student123!')

    expect(store.isAuthenticated).toBe(true)
    expect(localStorage.getItem('wenji_token')).toBe('signed-token')
    expect(store.user?.role).toBe('STUDENT')

    store.logout()
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('wenji_token')).toBeNull()
  })
})

