<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { bookApi, type BookInfo, type BookType } from '@/api/book'
import { cartApi } from '@/api/cart'
import { useAuth } from '@/stores/auth'
import { BookOpen, Search, RotateCcw, ShoppingCart } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isLoggedIn } = useAuth()

const books = ref<BookInfo[]>([])
const bookTypes = ref<BookType[]>([])
const loading = ref(true)
const addingMap = ref<Record<number, boolean>>({})

const keyword = ref('')
const selectedTypeId = ref<number | null>(null)

async function fetchBookTypes() {
  try {
    const res = await bookApi.getBookTypes()
    if (res.data.code === 200) {
      bookTypes.value = res.data.data || []
    }
  } catch (e) {
    console.error('获取分类失败')
  }
}

async function fetchBooks() {
  loading.value = true
  try {
    const params: any = { page: 0, size: 100 }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (selectedTypeId.value !== null) params.bookTypeId = selectedTypeId.value
    const res = await bookApi.searchBooks(params)
    if (res.data.code === 200) {
      const data = res.data.data
      books.value = data.content || []
    }
  } catch (e: any) {
    console.error('searchBooks error:', e.message || e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchBooks()
}

function handleReset() {
  keyword.value = ''
  selectedTypeId.value = null
  fetchBooks()
}

function goToDetail(bookId: number) {
  router.push(`/book/${bookId}`)
}

async function handleAddToCart(book: BookInfo) {
  if (!isLoggedIn.value) {
    router.push('/')
    return
  }
  addingMap.value[book.bookId] = true
  try {
    const res = await cartApi.addToCart(book.bookId, 1)
    if (res.data.code === 200) {
      alert('已添加到购物车')
    } else {
      alert(res.data.message || '添加失败')
    }
  } catch (e: any) {
    alert(e.response?.data?.message || '添加失败')
  } finally {
    addingMap.value[book.bookId] = false
  }
}

onMounted(async () => {
  await fetchBookTypes()
  fetchBooks()
})
</script>

<template>
  <Layout>
    <div class="mb-6">
      <div class="flex items-center gap-2 mb-6">
        <BookOpen class="w-5 h-5 text-indigo-600" />
        <h1 class="text-2xl font-bold text-gray-900">图书列表</h1>
      </div>

      <!-- 搜索栏 -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 mb-6">
        <div class="flex flex-wrap items-end gap-3">
          <div class="flex-1 min-w-[200px]">
            <label class="block text-sm text-gray-500 mb-1">书名 / 作者 / 描述</label>
            <input
              v-model="keyword"
              type="text"
              placeholder="输入关键字搜索"
              class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="w-40">
            <label class="block text-sm text-gray-500 mb-1">分类</label>
            <select
              v-model="selectedTypeId"
              class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"
            >
              <option :value="null">全部分类</option>
              <option v-for="t in bookTypes" :key="t.bookTypeId" :value="t.bookTypeId">
                {{ t.bookTypeName }}
              </option>
            </select>
          </div>
          <button
            class="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors flex items-center gap-1"
            @click="handleSearch"
          >
            <Search class="w-4 h-4" />
            查询
          </button>
          <button
            class="px-4 py-2 border border-gray-200 text-gray-600 text-sm rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-1"
            @click="handleReset"
          >
            <RotateCcw class="w-4 h-4" />
            重置
          </button>
        </div>
      </div>

      <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

      <div v-else-if="books.length === 0" class="text-center py-12 text-gray-400">
        <BookOpen class="w-12 h-12 mx-auto mb-3 opacity-30" />
        <p>暂无图书</p>
      </div>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
        <div
          v-for="book in books"
          :key="book.bookId"
          class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
          @click="goToDetail(book.bookId)"
        >
          <!-- Book Cover -->
          <div class="h-48 bg-gradient-to-br from-indigo-50 to-purple-50 flex items-center justify-center overflow-hidden">
            <img
              v-if="book.bookImg"
              :src="book.bookImg"
              :alt="book.bookName"
              class="w-full h-full object-cover"
            />
            <BookOpen v-else class="w-12 h-12 text-indigo-300" />
          </div>
          <div class="p-5">
            <h3 class="text-lg font-semibold text-gray-900 truncate">{{ book.bookName }}</h3>
            <p class="text-sm text-gray-500 mt-1">{{ book.bookAuthor }}</p>
            <p class="text-sm text-gray-400 mt-1 line-clamp-2">{{ book.bookDesc }}</p>
            <div class="flex items-center justify-between mt-4">
              <span class="text-indigo-600 font-bold text-lg">¥{{ book.bookPrice }}</span>
              <span class="text-xs text-gray-400 bg-gray-100 px-2 py-1 rounded">{{ book.bookTypeName || '未分类' }}</span>
            </div>
            <p class="text-xs text-gray-400 mt-1">
              库存：<span :class="(book.bookStock ?? 0) > 0 ? 'text-green-500' : 'text-red-400'">{{ book.bookStock ?? 0 }}</span>
            </p>
            <button
              @click.stop="handleAddToCart(book)"
              :disabled="addingMap[book.bookId]"
              class="mt-3 w-full bg-indigo-50 text-indigo-600 px-4 py-2 rounded-lg hover:bg-indigo-100 disabled:opacity-50 transition-colors flex items-center justify-center gap-1 text-sm font-medium"
            >
              <ShoppingCart class="w-4 h-4" />
              {{ addingMap[book.bookId] ? '添加中...' : '加入购物车' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>