import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '../api/auth'
import type { User } from '../types/api'

function storedUser(): User | null {
  const value = localStorage.getItem('wenji_user')
  if (!value) return null
  try {
    return JSON.parse(value) as User
  } catch {
    localStorage.removeItem('wenji_user')
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('wenji_token'))
  const user = ref<User | null>(storedUser())
  const isAuthenticated = computed(() => Boolean(token.value))

  function persist(nextToken: string, nextUser: User) {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem('wenji_token', nextToken)
    localStorage.setItem('wenji_user', JSON.stringify(nextUser))
  }

  async function login(username: string, password: string) {
    const result = await authApi.login({ username, password })
    persist(result.token, result.user)
  }

  async function register(username: string, password: string, nickname: string) {
    const result = await authApi.register({ username, password, nickname })
    persist(result.token, result.user)
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('wenji_token')
    localStorage.removeItem('wenji_user')
  }

  return { token, user, isAuthenticated, login, register, logout }
})

