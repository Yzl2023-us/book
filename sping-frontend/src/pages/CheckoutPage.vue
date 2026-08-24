<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { cartApi, type CartItem } from '@/api/cart'
import { purchaseApi } from '@/api/purchase'
import { useAuth } from '@/stores/auth'
import { ArrowLeft, ShoppingCart } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isLoggedIn } = useAuth()

const items = ref<CartItem[]>([])
const loading = ref(true)
const submitting = ref(false)

const form = ref({
  recipientName: '',
  recipientPhone: '',
  recipientAddress: '',
})

const totalPrice = computed(() => {
  return items.value.reduce((sum, item) => {
    return sum + (item.bookPrice || 0) * item.quantity
  }, 0)
})

const canSubmit = computed(() => {
  return form.value.recipientName.trim()
    && form.value.recipientPhone.trim()
    && form.value.recipientAddress.trim()
    && items.value.length > 0
    && !submitting.value
})

async function fetchCart() {
  loading.value = true
  try {
    const res = await cartApi.getMyCart()
    if (res.data.code === 200) {
      items.value = res.data.data || []
      if (items.value.length === 0) {
        router.replace('/cart')
      }
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleCheckout() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const res = await purchaseApi.checkout({
      recipientName: form.value.recipientName.trim(),
      recipientPhone: form.value.recipientPhone.trim(),
      recipientAddress: form.value.recipientAddress.trim(),
    })
    if (res.data.code === 200) {
      router.replace(`/order/${res.data.data.orderId}`)
    } else {
      alert(res.data.message || '下单失败')
    }
  } catch {
    alert('下单失败，请重试')
  }
  finally { submitting.value = false }
}

onMounted(() => {
  if (!isLoggedIn.value) {
    router.replace('/')
    return
  }
  fetchCart()
})
</script>

<template>
  <Layout>
    <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-indigo-600 mb-6 transition-colors">
      <ArrowLeft class="w-4 h-4" />
      返回购物车
    </button>

    <h1 class="text-2xl font-bold text-gray-900 mb-6">确认订单</h1>

    <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

    <div v-else class="grid lg:grid-cols-3 gap-6">
      <!-- Left: Recipient form -->
      <div class="lg:col-span-2 space-y-6">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">收件人信息</h2>
          <div class="space-y-4">
            <div>
              <label class="block text-sm text-gray-600 mb-1">收件人姓名</label>
              <input
                v-model="form.recipientName"
                type="text"
                placeholder="请输入收件人姓名"
                class="w-full border border-gray-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label class="block text-sm text-gray-600 mb-1">联系电话</label>
              <input
                v-model="form.recipientPhone"
                type="text"
                placeholder="请输入联系电话"
                class="w-full border border-gray-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label class="block text-sm text-gray-600 mb-1">收货地址</label>
              <textarea
                v-model="form.recipientAddress"
                rows="3"
                placeholder="请输入详细收货地址"
                class="w-full border border-gray-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
              ></textarea>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">商品清单</h2>
          <div class="space-y-3">
            <div
              v-for="item in items"
              :key="item.cartItemId"
              class="flex items-center gap-3 py-2 border-b border-gray-50 last:border-0"
            >
              <div class="w-10 h-12 bg-gradient-to-br from-indigo-50 to-purple-50 rounded flex items-center justify-center flex-shrink-0">
                <ShoppingCart class="w-4 h-4 text-indigo-300" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-gray-900 truncate">{{ item.bookName }}</p>
                <p class="text-xs text-gray-500">x{{ item.quantity }}</p>
              </div>
              <p class="text-sm font-semibold text-gray-900">¥{{ ((item.bookPrice || 0) * item.quantity).toFixed(2) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Order summary -->
      <div class="lg:col-span-1">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 sticky top-20">
          <h2 class="text-lg font-semibold text-gray-900 mb-4">订单摘要</h2>
          <div class="space-y-2 text-sm">
            <div class="flex justify-between text-gray-600">
              <span>商品数量</span>
              <span>{{ items.length }} 种</span>
            </div>
            <div class="flex justify-between text-gray-600">
              <span>商品总价</span>
              <span>¥{{ totalPrice.toFixed(2) }}</span>
            </div>
            <div class="border-t border-gray-100 pt-2 mt-2">
              <div class="flex justify-between font-semibold text-gray-900">
                <span>应付金额</span>
                <span class="text-indigo-600 text-lg">¥{{ totalPrice.toFixed(2) }}</span>
              </div>
            </div>
          </div>
          <button
            @click="handleCheckout"
            :disabled="!canSubmit"
            class="mt-6 w-full py-3 rounded-lg font-medium transition-colors"
            :class="canSubmit ? 'bg-indigo-600 text-white hover:bg-indigo-700' : 'bg-gray-200 text-gray-400 cursor-not-allowed'"
          >
            {{ submitting ? '提交中...' : '提交订单' }}
          </button>
        </div>
      </div>
    </div>
  </Layout>
</template>
