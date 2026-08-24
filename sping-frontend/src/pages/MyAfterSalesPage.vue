<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { afterSaleApi, type AfterSaleOrder } from '@/api/afterSale'
import { useAuth } from '@/stores/auth'
import { RotateCcw, ArrowLeft, ChevronRight } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const router = useRouter()
const { isLoggedIn } = useAuth()

const afterSales = ref<AfterSaleOrder[]>([])
const loading = ref(true)

const statusColor: Record<string, string> = {
  'PENDING_REVIEW': 'text-amber-600 bg-amber-50',
  'APPROVED': 'text-blue-600 bg-blue-50',
  'RETURNED': 'text-indigo-600 bg-indigo-50',
  'REJECTED': 'text-red-600 bg-red-50',
  'REFUNDED': 'text-green-600 bg-green-50',
  'CANCELED': 'text-gray-500 bg-gray-100',
}

async function fetchAfterSales() {
  loading.value = true
  try {
    const res = await afterSaleApi.getMyAfterSales()
    if (res.data.code === 200) {
      afterSales.value = res.data.data || []
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function viewDetail(id: number) {
  router.push(`/after-sale/${id}`)
}

onMounted(() => {
  if (!isLoggedIn.value) {
    router.replace('/')
    return
  }
  fetchAfterSales()
})
</script>

<template>
  <Layout>
    <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-indigo-600 mb-6 transition-colors">
      <ArrowLeft class="w-4 h-4" />
      返回
    </button>

    <h1 class="text-2xl font-bold text-gray-900 mb-6">我的售后</h1>

    <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

    <div v-else-if="afterSales.length === 0" class="text-center py-16">
      <RotateCcw class="w-16 h-16 mx-auto mb-4 text-gray-300" />
      <p class="text-gray-400 text-lg">暂无售后记录</p>
      <button
        @click="router.push('/orders')"
        class="mt-4 text-indigo-600 hover:text-indigo-700 transition-colors text-sm"
      >
        去订单列表看看
      </button>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="a in afterSales"
        :key="a.afterSaleId"
        @click="viewDetail(a.afterSaleId)"
        class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 cursor-pointer hover:shadow-md transition-shadow"
      >
        <div class="flex items-center justify-between">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-xs text-gray-400">{{ a.orderNo }}</span>
              <span class="px-2 py-0.5 rounded-full text-xs font-medium" :class="statusColor[a.status] || 'text-gray-600 bg-gray-50'">
                {{ a.statusText }}
              </span>
              <span class="px-2 py-0.5 rounded-full text-xs bg-gray-100 text-gray-500">{{ a.typeText }}</span>
            </div>
            <p class="text-sm font-semibold text-red-600">退款金额：¥{{ a.refundAmount }}</p>
            <p class="text-xs text-gray-400 mt-1">申请时间：{{ a.createTime }}</p>
          </div>
          <ChevronRight class="w-5 h-5 text-gray-400 flex-shrink-0" />
        </div>
      </div>
    </div>
  </Layout>
</template>
