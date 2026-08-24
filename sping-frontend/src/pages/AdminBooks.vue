<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { bookApi, type BookInfo } from '@/api/book'
import { BookOpen, Plus, Edit, Trash2, Check, X, Clock } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isAdmin, isLoggedIn } = useAuth()
const books = ref<BookInfo[]>([])
const loading = ref(true)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 10
const tab = ref<'all' | 'pending'>('all')

async function fetchBooks(page = 0) {
  loading.value = true
  try {
    let res
    if (tab.value === 'pending') {
      res = await bookApi.getPendingBooks(page, pageSize)
    } else {
      res = await bookApi.adminSearchBooks({ page, size: pageSize })
    }
    if (res.data.code === 200) {
      const data = res.data.data
      books.value = data.content || []
      totalPages.value = data.totalPages || 0
      currentPage.value = data.number || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function switchTab(t: 'all' | 'pending') {
  tab.value = t
  fetchBooks(0)
}

async function handleApprove(bookId: number) {
  try {
    const res = await bookApi.reviewBook(bookId, 'APPROVED')
    if (res.data.code === 200) {
      fetchBooks(currentPage.value)
    } else {
      alert(res.data.message || '操作失败')
    }
  } catch (e: any) {
    alert(e.response?.data?.message || '审核失败')
  }
}

async function handleReject(bookId: number) {
  if (!confirm('确认拒绝该图书？')) return
  try {
    const res = await bookApi.reviewBook(bookId, 'REJECTED')
    if (res.data.code === 200) {
      fetchBooks(currentPage.value)
    } else {
      alert(res.data.message || '操作失败')
    }
  } catch (e: any) {
    alert(e.response?.data?.message || '审核失败')
  }
}

async function handleDelete(bookId: number) {
  if (!confirm('确认删除该图书？')) return
  try {
    const res = await bookApi.deleteBook(bookId)
    if (res.data.code === 200) {
      await fetchBooks(currentPage.value)
    } else {
      alert(res.data.message || '删除失败')
    }
  } catch (e: any) {
    alert(e.response?.data?.message || '删除失败')
  }
}

function changePage(page: number) {
  if (page >= 0 && page < totalPages.value) {
    fetchBooks(page)
  }
}

function statusLabel(status?: string) {
  if (status === 'PENDING_REVIEW') return '待审核'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已拒绝'
  return status || '-'
}

function statusClass(status?: string) {
  if (status === 'PENDING_REVIEW') return 'bg-amber-100 text-amber-700'
  if (status === 'APPROVED') return 'bg-green-100 text-green-700'
  if (status === 'REJECTED') return 'bg-red-100 text-red-700'
  return 'bg-gray-100 text-gray-600'
}

onMounted(() => {
  if (!isLoggedIn.value || !isAdmin.value) {
    router.push('/')
    return
  }
  fetchBooks()
})
</script>

<template>
  <Layout>
    <div v-if="!isAdmin" class="text-center py-12 text-gray-400">
      <p>无权访问</p>
    </div>

    <div v-else>
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-2">
            <BookOpen class="w-5 h-5 text-indigo-600" />
            <h1 class="text-2xl font-bold text-gray-900">图书管理</h1>
          </div>
          <!-- Tabs -->
          <div class="flex bg-gray-100 rounded-lg p-1">
            <button
              @click="switchTab('all')"
              :class="tab === 'all' ? 'bg-white shadow-sm text-indigo-600' : 'text-gray-500 hover:text-gray-700'"
              class="px-4 py-1.5 rounded-md text-sm font-medium transition-colors"
            >全部</button>
            <button
              @click="switchTab('pending')"
              :class="tab === 'pending' ? 'bg-white shadow-sm text-amber-600' : 'text-gray-500 hover:text-gray-700'"
              class="px-4 py-1.5 rounded-md text-sm font-medium transition-colors flex items-center gap-1.5"
            >
              <Clock class="w-3.5 h-3.5" />
              待审核
            </button>
          </div>
        </div>
        <router-link
          to="/admin/books/new"
          class="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 transition-colors flex items-center gap-2"
        >
          <Plus class="w-4 h-4" />
          添加图书
        </router-link>
      </div>

      <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

      <div v-else-if="books.length === 0" class="text-center py-12 text-gray-400">
        <BookOpen class="w-12 h-12 mx-auto mb-3 opacity-30" />
        <p>暂无图书</p>
      </div>

      <div v-else>
        <div class="bg-white rounded-xl shadow-sm overflow-hidden">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-100 bg-gray-50">
                <th class="text-left px-6 py-3 text-sm font-medium text-gray-500">ID</th>
                <th class="text-left px-6 py-3 text-sm font-medium text-gray-500">书名</th>
                <th class="text-left px-6 py-3 text-sm font-medium text-gray-500">作者</th>
                <th class="text-left px-6 py-3 text-sm font-medium text-gray-500">价格</th>
                <th class="text-left px-6 py-3 text-sm font-medium text-gray-500">库存</th>
                <th class="text-left px-6 py-3 text-sm font-medium text-gray-500">状态</th>
                <th class="text-right px-6 py-3 text-sm font-medium text-gray-500">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="book in books" :key="book.bookId" class="border-b border-gray-50 hover:bg-gray-50 transition-colors">
                <td class="px-6 py-4 text-sm text-gray-600">{{ book.bookId }}</td>
                <td class="px-6 py-4 text-sm font-medium text-gray-900">{{ book.bookName }}</td>
                <td class="px-6 py-4 text-sm text-gray-600">{{ book.bookAuthor }}</td>
                <td class="px-6 py-4 text-sm text-indigo-600 font-medium">¥{{ book.bookPrice }}</td>
                <td class="px-6 py-4 text-sm text-gray-600">{{ book.bookStock ?? 0 }}</td>
                <td class="px-6 py-4 text-sm">
                  <span :class="statusClass(book.status)" class="px-2 py-0.5 rounded-full text-xs font-medium">
                    {{ statusLabel(book.status) }}
                  </span>
                </td>
                <td class="px-6 py-4 text-right">
                  <div class="flex items-center justify-end gap-2">
                    <!-- Pending: show approve/reject -->
                    <template v-if="book.status === 'PENDING_REVIEW'">
                      <button
                        @click="handleApprove(book.bookId)"
                        class="p-1.5 text-green-500 hover:bg-green-50 rounded transition-colors"
                        title="审核通过"
                      >
                        <Check class="w-4 h-4" />
                      </button>
                      <button
                        @click="handleReject(book.bookId)"
                        class="p-1.5 text-red-400 hover:bg-red-50 rounded transition-colors"
                        title="拒绝"
                      >
                        <X class="w-4 h-4" />
                      </button>
                    </template>
                    <!-- Edit always available -->
                    <router-link
                      :to="`/admin/books/${book.bookId}`"
                      class="p-2 text-gray-400 hover:text-indigo-600 transition-colors"
                    >
                      <Edit class="w-4 h-4" />
                    </router-link>
                    <button
                      @click="handleDelete(book.bookId)"
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
    </div>
  </Layout>
</template>