import { reactive, computed } from 'vue'
import type { UserInfo } from '@/api/user'

interface AuthState {
  user: UserInfo | null
  token: string | null
}

const state = reactive<AuthState>({
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  token: localStorage.getItem('token'),
})

export function useAuth() {
  const isLoggedIn = computed(() => !!state.token)
  const isAdmin = computed(() => state.user?.isAdmin === 1)

  function setAuth(user: UserInfo, token: string) {
    state.user = user
    state.token = token
    localStorage.setItem('user', JSON.stringify(user))
    localStorage.setItem('token', token)
    localStorage.setItem('userId', String(user.userId))
  }

  function logout() {
    state.user = null
    state.token = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
  }

  return {
    user: computed(() => state.user),
    token: computed(() => state.token),
    isLoggedIn,
    isAdmin,
    setAuth,
    logout,
  }
}