<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { bookApi, type BookInfo } from '@/api/book'
import { Package, Plus, Clock, Check, X } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isLoggedIn } = useAuth()
const books = ref<BookInfo[]>([])
const loading = ref(true)
const totalPages = ref(0)
const currentPage = ref(0)

async function fetchBooks(page = 0) {
  loading.value = true
  try {
    const res = await bookApi.getMyPublished(page, 10)
    if (res.data.code === 200) {
      const data = res.data.data
      books.value = data.content || []
      totalPages.value = data.totalPages || 0
      currentPage.value = data.number || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function changePage(page: number) {
  if (page >= 0 && page < totalPages.value) fetchBooks(page)
}

function statusLabel(s?: string) {
  if (s === 'PENDING_REVIEW') return '待审核'
  if (s === 'APPROVED') return '已通过'
  if (s === 'REJECTED') return '已拒绝'
  return s || '-'
}
function statusClass(s?: string) {
  if (s === 'PENDING_REVIEW') return 'bg-amber-100 text-amber-700'
  if (s === 'APPROVED') return 'bg-green-100 text-green-700'
  if (s === 'REJECTED') return 'bg-red-100 text-red-700'
  return 'bg-gray-100 text-gray-600'
}

onMounted(() => {
  if (!isLoggedIn.value) {
    router.push('/')
    return
  }
  fetchBooks()
})
</script>

<template>
  <Layout>
    <div class="max-w-4xl mx-auto">
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-2">
          <Package class="w-5 h-5 text-indigo-600" />
          <h1 class="text-2xl font-bold text-gray-900">我的发布</h1>
        </div>
        <router-link
          to="/publish"
          class="bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 transition-colors flex items-center gap-2"
        >
          <Plus class="w-4 h-4" />
          发布图书
        </router-link>
      </div>

      <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

      <div v-else-if="books.length === 0" class="text-center py-12 text-gray-400">
        <Package class="w-12 h-12 mx-auto mb-3 opacity-30" />
        <p>还没有发布过图书</p>
        <router-link to="/publish" class="mt-2 inline-block text-indigo-600 hover:text-indigo-700 text-sm font-medium">
          去发布 →</router-link>
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="book in books"
          :key="book.bookId"
          class="bg-white rounded-xl shadow-sm p-5 flex items-center gap-4 hover:shadow-md transition-shadow"
        >
          <!-- Cover -->
          <div class="w-16 h-20 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
            <img v-if="book.bookImg" :src="book.bookImg" :alt="book.bookName" class="w-full h-full object-cover" />
            <Package v-else class="w-8 h-8 text-gray-300 m-auto mt-4" />
          </div>

          <div class="flex-1 min-w-0">
            <h3 class="font-semibold text-gray-900 truncate">{{ book.bookName }}</h3>
            <p class="text-sm text-gray-500 mt-0.5">{{ book.bookAuthor }}</p>
            <div class="flex items-center gap-2 mt-1.5">
              <span class="text-indigo-600 font-medium text-sm">¥{{ book.bookPrice }}</span>
              <span class="text-xs text-gray-400">库存 {{ book.bookStock ?? 0 }}</span>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <span :class="statusClass(book.status)" class="px-2.5 py-1 rounded-full text-xs font-medium">
              {{ statusLabel(book.status) }}
            </span>
            <router-link
              :to="`/book/${book.bookId}`"
              class="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
            >查看详情 →</router-link>
          </div>
        </div>

        <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-6">
          <button
            @click="changePage(currentPage - 1)"
            :disabled="currentPage === 0"
            class="px-3 py-2 border rounded-lg text-sm disabled:opacity-40 hover:bg-gray-100 transition-colors"
          >上一页</button>
          <span class="text-sm text-gray-500">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button
            @click="changePage(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="px-3 py-2 border rounded-lg text-sm disabled:opacity-40 hover:bg-gray-100 transition-colors"
          >下一页</button>
        </div>
      </div>
    </div>
  </Layout>
</template>
