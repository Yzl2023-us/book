<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { userApi, type UserInfo } from '@/api/user'
import { Users, Plus, Edit, Trash2, X } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isAdmin, isLoggedIn } = useAuth()
const users = ref<UserInfo[]>([])
const loading = ref(true)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 10
const showModal = ref(false)
const editMode = ref(false)
const submitting = ref(false)

const form = reactive({
  userId: 0,
  username: '',
  password: '',
  isAdmin: 0,
})

async function fetchUsers(page = 0) {
  loading.value = true
  try {
    const res = await userApi.listUsers(page, pageSize)
    if (res.data.code === 200) {
      const data = res.data.data
      users.value = data.content || []
      totalPages.value = data.totalPages || 0
      currentPage.value = data.number || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openAdd() {
  editMode.value = false
  form.userId = 0
  form.username = ''
  form.password = ''
  form.isAdmin = 0
  showModal.value = true
}

function openEdit(user: UserInfo) {
  editMode.value = true
  form.userId = user.userId
  form.username = user.userName
  form.password = ''
  form.isAdmin = user.isAdmin
  showModal.value = true
}

async function handleSubmit() {
  if (!form.username.trim()) return alert('用户名不能为空')
  if (!editMode.value && !form.password.trim()) return alert('密码不能为空')

  submitting.value = true
  try {
    if (editMode.value) {
      const payload: any = { userName: form.username.trim() }
      if (form.password.trim()) payload.password = form.password.trim()
      payload.isAdmin = form.isAdmin
      const res = await userApi.updateUser(form.userId, payload)
      if (res.data.code === 200) {
        showModal.value = false
        await fetchUsers(currentPage.value)
      } else {
        alert(res.data.message || '更新失败')
      }
    } else {
      const res = await userApi.addUser({ username: form.username.trim(), password: form.password.trim() })
      if (res.data.code === 200) {
        showModal.value = false
        await fetchUsers(currentPage.value)
      } else {
        alert(res.data.message || '添加失败')
      }
    }
  } catch (e: any) {
    alert(e.response?.data?.message || '操作失败')
  }
  finally { submitting.value = false }
}

async function handleDelete(userId: number) {
  if (!confirm('确认删除该用户？')) return
  try {
    const res = await userApi.deleteUser(userId)
    if (res.data.code === 200) {
      await fetchUsers(currentPage.value)
    } else {
      alert(res.data.message || '删除失败')
    }
  } catch (e: any) {
    alert(e.response?.data?.message || '删除失败')
  }
}

function changePage(page: number) {
  if (page >= 0 && page < totalPages.value) {
    fetchUsers(page)
  }
}

onMounted(() => {
  if (!isLoggedIn.value || !isAdmin.value) {
    router.push('/')
    return
  }
  fetchUsers()
})
</script>

<template>
  <Layout>
    <div v-if="!isAdmin" class="text-center py-12 text-gray-400">
      <p>无权访问</p>
    </div>

    <div v-else>
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-2">
          <Users class="w-5 h-5 text-indigo-600" />
          <h1 class="text-2xl font-bold text-gray-900">用户管理</h1>
        </div>
        <button
          @click="openAdd"
          class="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 transition-colors flex items-center gap-2"
        >
          <Plus class="w-4 h-4" />
          添加用户
        </button>
      </div>

      <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

      <div v-else-if="users.length === 0" class="text-center py-12 text-gray-400">
        <Users class="w-12 h-12 mx-auto mb-3 opacity-30" />
        <p>暂无用户</p>
      </div>

      <div v-else>
        <div class="bg-white rounded-xl shadow-sm overflow-hidden">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-100 bg-gray-50">
                <th class="text-left px-6 py-3 text-sm font-medium text-gray-500">ID</th>
                <th class="text-left px-6 py-3 text-sm font-medium text-gray-500">用户名</th>
                <th class="text-left px-6 py-3 text-sm font-medium text-gray-500">角色</th>
                <th class="text-right px-6 py-3 text-sm font-medium text-gray-500">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.userId" class="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                <td class="px-6 py-4 text-sm text-gray-600">{{ user.userId }}</td>
                <td class="px-6 py-4 text-sm font-medium text-gray-900">{{ user.userName }}</td>
                <td class="px-6 py-4 text-sm">
                  <span
                    :class="user.isAdmin === 1 ? 'bg-indigo-100 text-indigo-700' : 'bg-gray-100 text-gray-600'"
                    class="px-2 py-1 rounded-full text-xs font-medium"
                  >
                    {{ user.isAdmin === 1 ? '管理员' : '普通用户' }}
                  </span>
                </td>
                <td class="px-6 py-4 text-right">
                  <div class="flex items-center justify-end gap-2">
                    <button
                      @click="openEdit(user)"
                      class="p-2 text-gray-400 hover:text-indigo-600 transition-colors"
                    >
                      <Edit class="w-4 h-4" />
                    </button>
                    <button
                      @click="handleDelete(user.userId)"
                      class="p-2 text-gray-400 hover:text-red-500 transition-colors"
                    >
                      <Trash2 class="w-4 h-4" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-6">
          <button
            @click="changePage(currentPage - 1)"
            :disabled="currentPage === 0"
            class="px-3 py-2 border rounded-lg text-sm disabled:opacity-40 hover:bg-gray-100 transition-colors"
          >
            上一页
          </button>
          <span class="text-sm text-gray-500">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button
            @click="changePage(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="px-3 py-2 border rounded-lg text-sm disabled:opacity-40 hover:bg-gray-100 transition-colors"
          >
            下一页
          </button>
        </div>
      </div>

      <!-- Modal -->
      <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/40" @click="showModal = false"></div>
        <div class="relative bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-bold text-gray-900">
              {{ editMode ? '编辑用户' : '添加用户' }}
            </h2>
            <button @click="showModal = false" class="text-gray-400 hover:text-gray-600">
              <X class="w-5 h-5" />
            </button>
          </div>

          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
              <input
                v-model="form.username"
                type="text"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                placeholder="请输入用户名"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">
                密码
                <span v-if="editMode" class="text-gray-400 font-normal">（留空则不修改）</span>
              </label>
              <input
                v-model="form.password"
                type="password"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                placeholder="请输入密码"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">角色</label>
              <select
                v-model="form.isAdmin"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              >
                <option :value="0">普通用户</option>
                <option :value="1">管理员</option>
              </select>
            </div>
          </div>

          <div class="flex gap-3 mt-6">
            <button
              @click="showModal = false"
              class="flex-1 border border-gray-300 text-gray-700 py-2 rounded-lg hover:bg-gray-50 transition-colors"
            >
              取消
            </button>
            <button
              @click="handleSubmit"
              :disabled="submitting"
              class="flex-1 bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50"
            >
              {{ submitting ? '提交中...' : '确定' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>
