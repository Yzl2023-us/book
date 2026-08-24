<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { afterSaleApi, type AfterSaleOrder } from '@/api/afterSale'
import { useAuth } from '@/stores/auth'
import { ArrowLeft, RotateCcw, Clock, CheckCircle, XCircle, Truck, Ban } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const route = useRoute()
const router = useRouter()
const { isLoggedIn } = useAuth()

const afterSaleId = Number(route.params.id)
const afterSale = ref<AfterSaleOrder | null>(null)
const loading = ref(true)
const actionLoading = ref(false)

const statusConfig: Record<string, { icon: any; color: string; bg: string; label: string }> = {
  'PENDING_REVIEW': { icon: Clock, color: 'text-amber-600', bg: 'bg-amber-50', label: '待审核' },
  'APPROVED': { icon: CheckCircle, color: 'text-blue-600', bg: 'bg-blue-50', label: '审核通过' },
  'RETURNED': { icon: Truck, color: 'text-indigo-600', bg: 'bg-indigo-50', label: '已退货' },
  'REJECTED': { icon: XCircle, color: 'text-red-600', bg: 'bg-red-50', label: '审核拒绝' },
  'REFUNDED': { icon: CheckCircle, color: 'text-green-600', bg: 'bg-green-50', label: '已退款' },
  'CANCELED': { icon: Ban, color: 'text-gray-500', bg: 'bg-gray-100', label: '已取消' },
}

const steps = ['PENDING_REVIEW', 'APPROVED', 'RETURNED', 'REFUNDED']

function getStepClass(step: string) {
  if (afterSale.value?.status === step) return 'bg-indigo-600 text-white'
  if (afterSale.value?.status === 'REJECTED' || afterSale.value?.status === 'CANCELED') {
    // Show completed steps as gray for terminated states
    const idx = steps.indexOf(step)
    const lastCompletedIdx = afterSale.value?.status === 'REJECTED' ? 0 : -1
    // For terminated states, only show the first step if reached
    return ''
  }
  const currentIdx = steps.indexOf(afterSale.value?.status || '')
  const stepIdx = steps.indexOf(step)
  if (stepIdx < currentIdx) return 'bg-green-500 text-white'
  if (stepIdx === currentIdx) return 'bg-indigo-600 text-white'
  return 'bg-gray-200 text-gray-400'
}

async function fetchAfterSale() {
  loading.value = true
  try {
    const res = await afterSaleApi.getDetail(afterSaleId)
    if (res.data.code === 200) {
      afterSale.value = res.data.data
    } else {
      alert(res.data.message)
      router.replace('/after-sales')
    }
  } catch {
    alert('加载失败')
  }
  finally { loading.value = false }
}

async function handleCancel() {
  if (!confirm('确认取消该售后申请？')) return
  actionLoading.value = true
  try {
    const res = await afterSaleApi.cancel(afterSaleId)
    if (res.data.code === 200) {
      await fetchAfterSale()
    } else {
      alert(res.data.message || '操作失败')
    }
  } catch {
    alert('操作失败')
  }
  finally { actionLoading.value = false }
}

async function handleConfirmReturn() {
  if (!confirm('确认已寄回商品？')) return
  actionLoading.value = true
  try {
    const res = await afterSaleApi.confirmReturn(afterSaleId)
    if (res.data.code === 200) {
      await fetchAfterSale()
    } else {
      alert(res.data.message || '操作失败')
    }
  } catch {
    alert('操作失败')
  }
  finally { actionLoading.value = false }
}

onMounted(() => {
  if (!isLoggedIn.value) {
    router.replace('/')
    return
  }
  fetchAfterSale()
})
</script>

<template>
  <Layout>
    <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-indigo-600 mb-6 transition-colors">
      <ArrowLeft class="w-4 h-4" />
      返回
    </button>

    <div v-if="loading" class="text-center py-12 text-gray-500">加载中...</div>

    <div v-else-if="afterSale" class="max-w-3xl mx-auto space-y-6">
      <!-- Header -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <RotateCcw class="w-6 h-6 text-orange-500" />
            <h1 class="text-xl font-bold text-gray-900">售后详情</h1>
          </div>
          <span
            class="px-3 py-1 rounded-full text-sm font-medium"
            :class="[statusConfig[afterSale.status]?.bg || 'bg-gray-50', statusConfig[afterSale.status]?.color || 'text-gray-600']"
          >
            {{ afterSale.statusText }}
          </span>
        </div>

        <div class="text-sm text-gray-500 space-y-1">
          <p>关联订单：{{ afterSale.orderNo }}</p>
          <p>售后类型：{{ afterSale.typeText }}</p>
          <p>申请时间：{{ afterSale.createTime }}</p>
          <p v-if="afterSale.reviewTime">审核时间：{{ afterSale.reviewTime }}</p>
          <p v-if="afterSale.returnTime">退货时间：{{ afterSale.returnTime }}</p>
          <p v-if="afterSale.refundTime">退款时间：{{ afterSale.refundTime }}</p>
          <p v-if="afterSale.cancelTime">取消时间：{{ afterSale.cancelTime }}</p>
        </div>
      </div>

      <!-- Progress steps -->
      <div v-if="afterSale.type === 'RETURN_REFUND' && afterSale.status !== 'REJECTED' && afterSale.status !== 'CANCELED'" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">退款进度</h2>
        <div class="flex items-center justify-between">
          <template v-for="(step, idx) in [
            { key: 'PENDING_REVIEW', label: '提交申请' },
            { key: 'APPROVED', label: '审核通过' },
            { key: 'RETURNED', label: '已退货' },
            { key: 'REFUNDED', label: '已退款' },
          ]" :key="step.key">
            <div class="flex flex-col items-center">
              <div
                class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold"
                :class="getStepClass(step.key)"
              >
                {{ getStepClass(step.key) ? idx + 1 : idx + 1 }}
              </div>
              <span class="text-xs mt-1" :class="getStepClass(step.key) ? 'text-indigo-600 font-medium' : 'text-gray-400'">
                {{ step.label }}
              </span>
            </div>
            <div v-if="idx < 3" class="flex-1 h-0.5 mx-2" :class="getStepClass(steps[idx + 1]) ? 'bg-green-500' : 'bg-gray-200'" />
          </template>
        </div>
      </div>

      <!-- Refund amount -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">退款信息</h2>
        <p class="text-2xl font-bold text-red-600">¥{{ afterSale.refundAmount }}</p>
      </div>

      <!-- Reason -->
      <div v-if="afterSale.reason" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">申请原因</h2>
        <p class="text-sm text-gray-600">{{ afterSale.reason }}</p>
      </div>

      <!-- Admin remark -->
      <div v-if="afterSale.adminRemark" class="bg-orange-50 rounded-xl border border-orange-100 p-4">
        <div class="flex items-center gap-2 mb-1">
          <span class="font-semibold text-orange-700">管理员备注</span>
        </div>
        <p class="text-sm text-orange-700">{{ afterSale.adminRemark }}</p>
      </div>

      <!-- Action buttons -->
      <div v-if="afterSale.status === 'PENDING_REVIEW'" class="flex gap-3">
        <button
          @click="handleCancel"
          :disabled="actionLoading"
          class="px-6 py-3 bg-white text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors font-medium disabled:opacity-50"
        >
          {{ actionLoading ? '处理中...' : '取消申请' }}
        </button>
      </div>
      <div v-if="afterSale.status === 'APPROVED' && afterSale.type === 'RETURN_REFUND'" class="flex gap-3">
        <button
          @click="handleConfirmReturn"
          :disabled="actionLoading"
          class="px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-medium disabled:opacity-50"
        >
          {{ actionLoading ? '处理中...' : '确认退货（模拟寄回）' }}
        </button>
        <button
          @click="handleCancel"
          :disabled="actionLoading"
          class="px-6 py-3 bg-white text-red-600 border border-red-300 rounded-lg hover:bg-red-50 transition-colors font-medium disabled:opacity-50"
        >
          {{ actionLoading ? '处理中...' : '取消申请' }}
        </button>
      </div>
    </div>
  </Layout>
</template>
