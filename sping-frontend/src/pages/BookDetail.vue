<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { bookApi, type BookInfo } from '@/api/book'
import { cartApi } from '@/api/cart'
import { messageApi } from '@/api/message'
import { useAuth } from '@/stores/auth'
import { BookOpen, ShoppingCart, MessageSquare, ArrowLeft } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const route = useRoute()
const router = useRouter()
const { isLoggedIn, user } = useAuth()

const book = ref<BookInfo | null>(null)
const loading = ref(true)
const messages = ref<any[]>([])
const cartLoading = ref(false)
const messageContent = ref('')
const sendLoading = ref(false)

async function fetchBook() {
  loading.value = true
  try {
    const res = await bookApi.getBookById(Number(route.params.id))
    if (res.data.code === 200) {
      book.value = res.data.data
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function fetchMessages() {
  try {
    const res = await messageApi.getBookMessages(Number(route.params.id))
    if (res.data.code === 200) {
      messages.value = res.data.data || []
    }
  } catch { /* ignore */ }
}

async function handleAddToCart() {
  if (!isLoggedIn.value) {
    router.push('/')
    return
  }
  cartLoading.value = true
  try {
    const res = await cartApi.addToCart(Number(route.params.id), 1)
    if (res.data.code === 200) {
      alert('已添加到购物车')
    } else {
      alert(res.data.message || '添加失败')
    }
  } catch (e: any) {
    alert(e.response?.data?.message || '添加失败')
  } finally {
    cartLoading.value = false
  }
}

async function handleSendMessage() {
  if (!isLoggedIn.value) {
    router.push('/')
    return
  }
  if (!messageContent.value.trim()) return
  sendLoading.value = true
  try {
    const res = await messageApi.sendMessage({
      content: messageContent.value,
      bookId: Number(route.params.id),
      receiverId: 0,
    })
    if (res.data.code === 200) {
      messageContent.value = ''
      await fetchMessages()
    }
  } catch { /* ignore */ }
  finally { sendLoading.value = false }
}

onMounted(() => {
  fetchBook()
  fetchMessages()
})
</script>

<template>
  <Layout>
    <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

    <div v-else-if="book" class="max-w-4xl mx-auto">
      <!-- Back -->
      <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-indigo-600 mb-6 transition-colors">
        <ArrowLeft class="w-4 h-4" />
        返回
      </button>

      <div class="bg-white rounded-xl shadow-sm p-8">
        <div class="flex flex-col md:flex-row gap-8">
          <!-- Cover -->
          <div class="w-full md:w-48 h-64 rounded-xl flex items-center justify-center flex-shrink-0 overflow-hidden">
            <img
              v-if="book.bookImg"
              :src="book.bookImg"
              :alt="book.bookName"
              class="w-full h-full object-cover"
            />
            <div v-else class="w-full h-full bg-gradient-to-br from-indigo-50 to-purple-50 flex items-center justify-center">
              <BookOpen class="w-20 h-20 text-indigo-300" />
            </div>
          </div>

          <!-- Info -->
          <div class="flex-1">
            <h1 class="text-2xl font-bold text-gray-900">{{ book.bookName }}</h1>
            <p class="text-gray-500 mt-2">作者：{{ book.bookAuthor }}</p>
            <div class="flex items-center gap-3 mt-3">
              <span class="text-3xl font-bold text-indigo-600">¥{{ book.bookPrice }}</span>
              <span class="bg-gray-100 text-gray-600 text-sm px-3 py-1 rounded-full">{{ book.bookTypeName }}</span>
            </div>
            <p class="text-sm text-gray-500 mt-2">
              库存：<span :class="(book.bookStock ?? 0) > 0 ? 'text-green-600' : 'text-red-500'" class="font-medium">{{ book.bookStock ?? 0 }} 本</span>
            </p>
            <p class="text-gray-600 mt-4 leading-relaxed">{{ book.bookDesc }}</p>

            <!-- 审核状态 -->
            <div v-if="book.status === 'PENDING_REVIEW'" class="mt-3 inline-flex items-center gap-1.5 px-3 py-1.5 bg-amber-50 border border-amber-200 text-amber-700 rounded-lg text-sm">
              <span class="w-2 h-2 rounded-full bg-amber-500"></span>
              待审核 — 审核通过后将自动上架
            </div>
            <div v-else-if="book.status === 'REJECTED'" class="mt-3 inline-flex items-center gap-1.5 px-3 py-1.5 bg-red-50 border border-red-200 text-red-600 rounded-lg text-sm">
              <span class="w-2 h-2 rounded-full bg-red-400"></span>
              审核未通过，暂不可购买
            </div>

            <div class="flex gap-3 mt-6">
              <button
                v-if="!book.status || book.status === 'APPROVED'"
                @click="handleAddToCart"
                :disabled="cartLoading"
                class="bg-indigo-600 text-white px-6 py-2.5 rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors flex items-center gap-2"
              >
                <ShoppingCart class="w-4 h-4" />
                {{ cartLoading ? '添加中...' : '加入购物车' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Messages -->
      <div class="bg-white rounded-xl shadow-sm p-8 mt-6">
        <div class="flex items-center gap-2 mb-6">
          <MessageSquare class="w-5 h-5 text-indigo-600" />
          <h2 class="text-lg font-bold text-gray-900">图书留言</h2>
        </div>

        <div v-if="messages.length === 0" class="text-center py-8 text-gray-400">
          暂无留言，来发表第一条留言吧
        </div>

        <div v-else class="space-y-4 mb-6">
          <div v-for="msg in messages" :key="msg.id" class="border-b border-gray-100 pb-4 last:border-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-medium text-gray-900">{{ msg.senderName || '用户' + msg.senderId }}</span>
              <span class="text-xs text-gray-400">{{ new Date(msg.createTime).toLocaleString() }}</span>
            </div>
            <p class="text-gray-600">{{ msg.content }}</p>
          </div>
        </div>

        <!-- Send message -->
        <div v-if="isLoggedIn" class="flex gap-3">
          <input
            v-model="messageContent"
            @keyup.enter="handleSendMessage"
            type="text"
            placeholder="输入留言..."
            class="flex-1 px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
          />
          <button
            @click="handleSendMessage"
            :disabled="sendLoading || !messageContent.trim()"
            class="bg-indigo-600 text-white px-4 py-2.5 rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors"
          >
            {{ sendLoading ? '发送中...' : '发送' }}
          </button>
        </div>
        <p v-else class="text-center text-gray-400">
          <router-link to="/" class="text-indigo-600 hover:underline">登录</router-link> 后即可留言
        </p>
      </div>
    </div>
  </Layout>
</template>