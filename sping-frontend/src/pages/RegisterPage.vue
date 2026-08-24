<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import { UserPlus } from 'lucide-vue-next'

const router = useRouter()
const form = reactive({ username: '', password: '', confirmPassword: '' })
const error = ref('')
const success = ref('')
const loading = ref(false)

async function handleRegister() {
  error.value = ''
  success.value = ''
  if (!form.username || !form.password) {
    error.value = '请填写用户名和密码'
    return
  }
  if (form.password !== form.confirmPassword) {
    error.value = '两次密码输入不一致'
    return
  }
  loading.value = true
  try {
    const res = await userApi.register({ username: form.username, password: form.password })
    if (res.data.code === 200) {
      success.value = '注册成功！即将跳转登录页...'
      setTimeout(() => router.push('/'), 1500)
    } else {
      error.value = res.data.message || '注册失败'
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex items-center justify-center px-4">
    <div class="max-w-md w-full">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-900">Sping 书城</h1>
        <p class="text-gray-500 mt-2">创建新账户</p>
      </div>

      <div class="bg-white rounded-xl shadow-sm p-8">
        <div v-if="error" class="mb-4 p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-sm">
          {{ error }}
        </div>
        <div v-if="success" class="mb-4 p-3 bg-green-50 border border-green-200 text-green-600 rounded-lg text-sm">
          {{ success }}
        </div>

        <form @submit.prevent="handleRegister" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
            <input
              v-model="form.username"
              type="text"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-colors"
              placeholder="请输入用户名"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">密码</label>
            <input
              v-model="form.password"
              type="password"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-colors"
              placeholder="请输入密码"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">确认密码</label>
            <input
              v-model="form.confirmPassword"
              type="password"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-colors"
              placeholder="请再次输入密码"
            />
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full bg-indigo-600 text-white py-2.5 rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
          >
            <UserPlus class="w-4 h-4" />
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </form>

        <p class="text-center text-sm text-gray-500 mt-6">
          已有账户？
          <router-link to="/" class="text-indigo-600 hover:text-indigo-700 font-medium">
            立即登录
          </router-link>
        </p>
      </div>
    </div>
  </div>
</template>