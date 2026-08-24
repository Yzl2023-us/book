<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { cartApi, type CartItem } from '@/api/cart'
import { useAuth } from '@/stores/auth'
import { ShoppingCart, Trash2, Minus, Plus, ArrowLeft } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isLoggedIn } = useAuth()

const items = ref<CartItem[]>([])
const loading = ref(true)

const totalPrice = computed(() => {
  return items.value.reduce((sum, item) => {
    return sum + (item.bookPrice || 0) * item.quantity
  }, 0)
})

async function fetchCart() {
  loading.value = true
  try {
    const res = await cartApi.getMyCart()
    if (res.data.code === 200) {
      items.value = res.data.data || []
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleUpdateQuantity(item: CartItem, delta: number) {
  const newQty = item.quantity + delta
  if (newQty < 1) {
    await handleRemove(item)
    return
  }
  try {
    const res = await cartApi.updateQuantity(item.cartItemId, newQty)
    if (res.data.code === 200) {
      item.quantity = newQty
    }
  } catch { /* ignore */ }
}

async function handleRemove(item: CartItem) {
  try {
    const res = await cartApi.removeItem(item.cartItemId)
    if (res.data.code === 200) {
      items.value = items.value.filter(i => i.cartItemId !== item.cartItemId)
    }
  } catch { /* ignore */ }
}

async function handleClear() {
  if (!confirm('确定要清空购物车吗？')) return
  try {
    const res = await cartApi.clearCart()
    if (res.data.code === 200) {
      items.value = []
    }
  } catch { /* ignore */ }
}

function goToBook(bookId: number) {
  router.push(`/book/${bookId}`)
}

onMounted(() => {
  if (!isLoggedIn.value) {
    router.push('/')
    return
  }
  fetchCart()
})
</script>

<template>
  <Layout>
    <!-- Back -->
    <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-indigo-600 mb-6 transition-colors">
      <ArrowLeft class="w-4 h-4" />
      返回
    </button>

    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-2">
        <ShoppingCart class="w-6 h-6 text-indigo-600" />
        <h1 class="text-2xl font-bold text-gray-900">我的购物车</h1>
      </div>
      <button
        v-if="items.length > 0"
        @click="handleClear"
        class="text-sm text-red-500 hover:text-red-600 transition-colors flex items-center gap-1"
      >
        <Trash2 class="w-4 h-4" />
        清空购物车
      </button>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

    <div v-else-if="items.length === 0" class="text-center py-16">
      <ShoppingCart class="w-16 h-16 mx-auto mb-4 text-gray-300" />
      <p class="text-gray-400 text-lg mb-4">购物车是空的</p>
      <button
        @click="router.push('/home')"
        class="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700 transition-colors"
      >
        去逛逛
      </button>
    </div>

    <div v-else class="space-y-4">
      <div
        v-for="item in items"
        :key="item.cartItemId"
        class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 flex items-center gap-4"
      >
        <!-- Book cover placeholder -->
        <div
          class="w-16 h-20 bg-gradient-to-br from-indigo-50 to-purple-50 rounded-lg flex items-center justify-center flex-shrink-0 cursor-pointer"
          @click="goToBook(item.bookId)"
        >
          <ShoppingCart class="w-6 h-6 text-indigo-300" />
        </div>

        <!-- Info -->
        <div class="flex-1 min-w-0 cursor-pointer" @click="goToBook(item.bookId)">
          <h3 class="font-semibold text-gray-900 truncate">{{ item.bookName }}</h3>
          <p class="text-sm text-gray-500">{{ item.bookAuthor }}</p>
          <p class="text-indigo-600 font-bold mt-1">¥{{ item.bookPrice }}</p>
        </div>

        <!-- Quantity controls -->
        <div class="flex items-center gap-2">
          <button
            @click="handleUpdateQuantity(item, -1)"
            class="w-8 h-8 rounded-lg border border-gray-200 flex items-center justify-center hover:bg-gray-50 transition-colors"
          >
            <Minus class="w-4 h-4 text-gray-500" />
          </button>
          <span class="w-8 text-center font-medium text-gray-900">{{ item.quantity }}</span>
          <button
            @click="handleUpdateQuantity(item, 1)"
            class="w-8 h-8 rounded-lg border border-gray-200 flex items-center justify-center hover:bg-gray-50 transition-colors"
          >
            <Plus class="w-4 h-4 text-gray-500" />
          </button>
        </div>

        <!-- Subtotal -->
        <div class="text-right flex-shrink-0 w-24">
          <p class="text-indigo-600 font-bold">¥{{ ((item.bookPrice || 0) * item.quantity).toFixed(2) }}</p>
        </div>

        <!-- Remove -->
        <button
          @click="handleRemove(item)"
          class="text-gray-400 hover:text-red-500 transition-colors"
        >
          <Trash2 class="w-4 h-4" />
        </button>
      </div>

      <!-- Bottom bar -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 flex items-center justify-between sticky bottom-0">
        <div>
          <span class="text-gray-500 text-sm">共 {{ items.length }} 种图书</span>
        </div>
        <div class="flex items-center gap-4">
          <div>
            <span class="text-gray-500 text-sm">合计：</span>
            <span class="text-2xl font-bold text-indigo-600">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
          <button
            @click="router.push('/checkout')"
            class="bg-indigo-600 text-white px-8 py-2.5 rounded-lg hover:bg-indigo-700 transition-colors font-medium"
          >
            结算
          </button>
        </div>
      </div>
    </div>
  </Layout>
</template>
