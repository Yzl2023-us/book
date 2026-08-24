<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '@/stores/auth'
import { userApi } from '@/api/user'
import { User, Shield, Upload, Camera } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'
import { useRouter } from 'vue-router'

const { user, isLoggedIn, isAdmin, logout } = useAuth()
const router = useRouter()

const uploading = ref(false)

function handleLogout() {
  logout()
  router.push('/')
}

async function handleAvatarUpload(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files || !input.files[0]) return

  const file = input.files[0]
  uploading.value = true
  try {
    // 上传头像
    const uploadRes = await userApi.uploadAvatar(file)
    if (uploadRes.data.code === 200) {
      const avatarUrl = uploadRes.data.data.url
      // 更新用户头像
      const userId = Number(localStorage.getItem('userId'))
      const updateRes = await userApi.updateAvatar(userId, avatarUrl)
      if (updateRes.data.code === 200) {
        // 更新本地 user 信息
        if (user.value) {
          user.value.avatar = avatarUrl
          localStorage.setItem('user', JSON.stringify(user.value))
        }
      }
    } else {
      alert(uploadRes.data.message || '上传失败')
    }
  } catch (e: any) {
    alert(e.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <Layout>
    <div v-if="!isLoggedIn" class="text-center py-12 text-gray-400">
      <User class="w-12 h-12 mx-auto mb-3 opacity-30" />
      <p>请先登录</p>
    </div>

    <div v-else class="max-w-2xl mx-auto">
      <div class="flex items-center gap-2 mb-6">
        <User class="w-5 h-5 text-indigo-600" />
        <h1 class="text-2xl font-bold text-gray-900">个人中心</h1>
      </div>

      <div class="bg-white rounded-xl shadow-sm p-8">
        <div class="flex items-center gap-4 mb-6">
          <div class="relative group">
            <div v-if="user?.avatar" class="w-16 h-16 rounded-full overflow-hidden border-2 border-indigo-200">
              <img :src="user.avatar" alt="头像" class="w-full h-full object-cover" />
            </div>
            <div v-else class="w-16 h-16 bg-indigo-100 rounded-full flex items-center justify-center">
              <User class="w-8 h-8 text-indigo-600" />
            </div>
            <!-- Upload overlay -->
            <label class="absolute inset-0 rounded-full bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
              <Camera class="w-5 h-5 text-white" />
              <input
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleAvatarUpload"
                :disabled="uploading"
              />
            </label>
            <div v-if="uploading" class="absolute inset-0 rounded-full bg-black/30 flex items-center justify-center">
              <span class="text-white text-xs">上传中...</span>
            </div>
          </div>
          <div>
            <h2 class="text-xl font-bold text-gray-900">{{ user?.userName }}</h2>
            <div class="flex items-center gap-1 mt-1">
              <Shield v-if="isAdmin" class="w-4 h-4 text-amber-500" />
              <span class="text-sm" :class="isAdmin ? 'text-amber-600' : 'text-gray-500'">
                {{ isAdmin ? '管理员' : '普通用户' }}
              </span>
            </div>
          </div>
        </div>

        <div class="border-t border-gray-100 pt-6 space-y-3">
          <router-link
            to="/messages"
            class="block w-full text-left px-4 py-3 rounded-lg hover:bg-indigo-50 text-gray-700 hover:text-indigo-600 transition-colors"
          >
            消息中心
          </router-link>
          <router-link
            v-if="isAdmin"
            to="/admin/books"
            class="block w-full text-left px-4 py-3 rounded-lg hover:bg-indigo-50 text-gray-700 hover:text-indigo-600 transition-colors"
          >
            图书管理
          </router-link>
        </div>

        <div class="border-t border-gray-100 pt-6 mt-6">
          <button
            @click="handleLogout"
            class="w-full text-center px-4 py-3 bg-red-50 text-red-600 rounded-lg hover:bg-red-100 transition-colors"
          >
            退出登录
          </button>
        </div>
      </div>
    </div>
  </Layout>
</template>