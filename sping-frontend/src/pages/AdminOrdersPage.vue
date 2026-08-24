<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { purchaseApi, type PurchaseOrder } from '@/api/purchase'
import { useAuth } from '@/stores/auth'
import { Package, ArrowLeft, Check, X, Truck, ChevronDown, ChevronUp } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isAdmin } = useAuth()

const orders = ref<PurchaseOrder[]>([])
const loading = ref(true)
const expandedId = ref<number | null>(null)
const reviewRemark = ref('')
const processing = ref(false)

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
    const res = await purchaseApi.getAllOrders()
    if (res.data.code === 200) {
      orders.value = res.data.data || []
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function toggleExpand(orderId: number) {
  expandedId.value = expandedId.value === orderId ? null : orderId
}

async function handleReview(orderId: number, action: string) {
  processing.value = true
  try {
    const remark = action === 'REJECT' ? reviewRemark.value : undefined
    const res = await purchaseApi.reviewOrder(orderId, action, remark)
    if (res.data.code === 200) {
      reviewRemark.value = ''
      expandedId.value = null
      await fetchOrders()
    } else {
      alert(res.data.message)
    }
  } catch {
    alert('操作失败')
  }
  finally { processing.value = false }
}

async function handleShip(orderId: number) {
  if (!confirm('确认发货？')) return
  processing.value = true
  try {
    const res = await purchaseApi.shipOrder(orderId)
    if (res.data.code === 200) {
      await fetchOrders()
    } else {
      alert(res.data.message)
    }
  } catch {
    alert('操作失败')
  }
  finally { processing.value = false }
}

onMounted(() => {
  if (!isAdmin.value) {
    router.replace('/home')
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

    <h1 class="text-2xl font-bold text-gray-900 mb-6">订单管理</h1>

    <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

    <div v-else-if="orders.length === 0" class="text-center py-16">
      <Package class="w-16 h-16 mx-auto mb-4 text-gray-300" />
      <p class="text-gray-400 text-lg">暂无订单</p>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="order in orders"
        :key="order.orderId"
        class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden"
      >
        <!-- Order header -->
        <div
          class="p-4 cursor-pointer hover:bg-gray-50 transition-colors"
          @click="toggleExpand(order.orderId)"
        >
          <div class="flex items-center justify-between">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-xs text-gray-400">{{ order.orderNo }}</span>
                <span class="px-2 py-0.5 rounded-full text-xs font-medium" :class="statusColor[order.status] || 'text-gray-600 bg-gray-50'">
                  {{ order.statusText }}
                </span>
              </div>
              <p class="text-sm text-gray-600">用户：{{ order.userName }} | 收件人：{{ order.recipientName }} {{ order.recipientPhone }}</p>
              <p class="text-sm font-semibold text-gray-900 mt-1">¥{{ order.totalAmount }}</p>
            </div>
            <div class="flex items-center gap-3">
              <!-- Quick actions -->
              <template v-if="order.status === 'PENDING_REVIEW'">
                <button
                  @click.stop="handleReview(order.orderId, 'APPROVE')"
                  :disabled="processing"
                  class="px-3 py-1.5 bg-green-500 text-white rounded-lg text-sm hover:bg-green-600 transition-colors disabled:opacity-50"
                >
                  <Check class="w-4 h-4 inline" /> 通过
                </button>
                <button
                  @click.stop="toggleExpand(order.orderId)"
                  class="px-3 py-1.5 bg-red-500 text-white rounded-lg text-sm hover:bg-red-600 transition-colors"
                >
                  <X class="w-4 h-4 inline" /> 拒绝
                </button>
              </template>
              <button
                v-if="order.status === 'APPROVED'"
                @click.stop="handleShip(order.orderId)"
                :disabled="processing"
                class="px-3 py-1.5 bg-indigo-500 text-white rounded-lg text-sm hover:bg-indigo-600 transition-colors disabled:opacity-50"
              >
                <Truck class="w-4 h-4 inline" /> 发货
              </button>
              <component :is="expandedId === order.orderId ? ChevronUp : ChevronDown" class="w-5 h-5 text-gray-400" />
            </div>
          </div>
        </div>

        <!-- Expanded details -->
        <div v-if="expandedId === order.orderId" class="border-t border-gray-100 p-4 bg-gray-50">
          <!-- Reject with remark -->
          <div v-if="order.status === 'PENDING_REVIEW'" class="mb-4">
            <label class="block text-sm text-gray-600 mb-1">拒绝原因</label>
            <div class="flex gap-2">
              <input
                v-model="reviewRemark"
                type="text"
                placeholder="请输入拒绝原因"
                class="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
              />
              <button
                @click="handleReview(order.orderId, 'REJECT')"
                :disabled="processing || !reviewRemark.trim()"
                class="px-4 py-2 bg-red-500 text-white rounded-lg text-sm hover:bg-red-600 transition-colors disabled:opacity-50"
              >
                确认拒绝
              </button>
            </div>
          </div>

          <div class="text-sm text-gray-600 space-y-1">
            <p><span class="font-medium">收货地址：</span>{{ order.recipientAddress }}</p>
            <p><span class="font-medium">下单时间：</span>{{ order.createTime }}</p>
            <p v-if="order.payTime"><span class="font-medium">支付时间：</span>{{ order.payTime }}</p>
            <p v-if="order.reviewTime"><span class="font-medium">审核时间：</span>{{ order.reviewTime }}</p>
            <p v-if="order.shipTime"><span class="font-medium">发货时间：</span>{{ order.shipTime }}</p>
            <p v-if="order.cancelTime"><span class="font-medium">取消时间：</span>{{ order.cancelTime }}</p>
            <p v-if="order.reviewRemark" class="text-red-600"><span class="font-medium">审核备注：</span>{{ order.reviewRemark }}</p>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>
