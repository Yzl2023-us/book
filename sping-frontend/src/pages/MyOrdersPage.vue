<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { purchaseApi, type PurchaseOrder } from '@/api/purchase'
import { useAuth } from '@/stores/auth'
import { Package, ArrowLeft, ChevronRight, X } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isLoggedIn } = useAuth()

const orders = ref<PurchaseOrder[]>([])
const loading = ref(true)

const statusColor: Record<string, string> = {
  'PENDING_PAYMENT': 'text-amber-600 bg-amber-50',
  'PENDING_REVIEW': 'text-blue-600 bg-blue-50',
  'APPROVED': 'text-green-600 bg-green-50',
  'REJECTED': 'text-red-600 bg-red-50',
  'SHIPPED': 'text-indigo-600 bg-indigo-50',
  'COMPLETED': 'text-green-600 bg-green-50',
  'CANCELED': 'text-gray-500 bg-gray-100',
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await purchaseApi.getMyOrders()
    if (res.data.code === 200) {
      orders.value = res.data.data || []
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function viewOrder(orderId: number) {
  router.push(`/order/${orderId}`)
}

const cancelLoading = ref<number | null>(null)
async function cancelOrder(orderId: number) {
  if (!confirm('确认取消该订单？')) return
  cancelLoading.value = orderId
  try {
    const res = await purchaseApi.cancelOrder(orderId)
    if (res.data.code === 200) {
      await fetchOrders()
    } else {
      alert(res.data.message || '取消订单失败')
    }
  } catch {
    alert('取消订单失败，请重试')
  }
  finally { cancelLoading.value = null }
}

onMounted(() => {
  if (!isLoggedIn.value) {
    router.replace('/')
    return
  }
  fetchOrders()
})
</script>

<template>
  <Layout>
    <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-indigo-600 mb-6 transition-colors">
      <ArrowLeft class="w-4 h-4" />
      返回
    </button>

    <h1 class="text-2xl font-bold text-gray-900 mb-6">我的订单</h1>

    <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

    <div v-else-if="orders.length === 0" class="text-center py-16">
      <Package class="w-16 h-16 mx-auto mb-4 text-gray-300" />
      <p class="text-gray-400 text-lg mb-4">暂无订单</p>
      <button
        @click="router.push('/home')"
        class="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700 transition-colors"
      >
        去逛逛
      </button>
    </div>

    <div v-else class="space-y-4">
      <div
        v-for="order in orders"
        :key="order.orderId"
        @click="viewOrder(order.orderId)"
        class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 cursor-pointer hover:shadow-md transition-shadow"
      >
        <div class="flex items-center justify-between">
          <div class="flex-1 min-w-0">
            <p class="text-sm text-gray-500">订单号：{{ order.orderNo }}</p>
            <p class="text-lg font-bold text-gray-900 mt-1">¥{{ order.totalAmount }}</p>
            <p class="text-xs text-gray-400 mt-1">{{ order.createTime }}</p>
          </div>
          <div class="flex items-center gap-3">
            <button
              v-if="order.status === 'PENDING_PAYMENT' || order.status === 'PENDING_REVIEW'"
              @click.stop="cancelOrder(order.orderId)"
              :disabled="cancelLoading === order.orderId"
              class="px-3 py-1 rounded-lg text-xs font-medium bg-red-50 text-red-600 border border-red-200 hover:bg-red-100 transition-colors disabled:opacity-50"
            >
              <X class="w-3.5 h-3.5 inline" /> {{ cancelLoading === order.orderId ? '取消中...' : '取消订单' }}
            </button>
            <span class="px-3 py-1 rounded-full text-xs font-medium" :class="statusColor[order.status] || 'text-gray-600 bg-gray-50'">
              {{ order.statusText }}
            </span>
            <ChevronRight class="w-5 h-5 text-gray-400" />
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>
