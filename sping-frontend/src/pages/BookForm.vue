<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { bookApi, type BookParams } from '@/api/book'
import { useAuth } from '@/stores/auth'
import { Save, Upload, X } from 'lucide-vue-next'
import Layout from '@/components/Layout.vue'

const route = useRoute()
const router = useRouter()
const { isAdmin, isLoggedIn } = useAuth()

const isEdit = ref(false)
const loading = ref(false)
const fetchingBook = ref(false)
const error = ref('')
const uploadingCover = ref(false)
const coverFile = ref<File | null>(null)
const coverPreview = ref<string>('')
const existingCoverUrl = ref<string>('')

const form = reactive<BookParams>({
  bookName: '',
  bookAuthor: '',
  bookPrice: 0,
  bookTypeId: 1,
  bookDesc: '',
  bookImg: '',
  bookStock: 0,
})

const bookTypes = ref<{ bookTypeId: number; bookTypeName: string }[]>([])

async function fetchBookTypes() {
  try {
    const res = await bookApi.getBookTypes()
    if (res.data.code === 200) {
      bookTypes.value = res.data.data || []
      if (bookTypes.value.length > 0 && !isEdit.value) {
        form.bookTypeId = bookTypes.value[0].bookTypeId
      }
    }
  } catch { /* ignore */ }
}

async function fetchBook() {
  const id = Number(route.params.id)
  if (!id) return
  fetchingBook.value = true
  error.value = ''
  try {
    const res = await bookApi.getBookById(id)
    if (res.data.code === 200) {
      const book = res.data.data
      form.bookName = book.bookName
      form.bookAuthor = book.bookAuthor
      form.bookPrice = book.bookPrice
      form.bookTypeId = book.bookTypeId
      form.bookDesc = book.bookDesc
      form.bookImg = book.bookImg || ''
      existingCoverUrl.value = book.bookImg || ''
      form.bookStock = book.bookStock ?? 0
    } else {
      error.value = res.data.message || '获取图书信息失败'
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || '获取图书信息失败，请检查网络'
  } finally {
    fetchingBook.value = false
  }
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    coverFile.value = input.files[0]
    coverPreview.value = URL.createObjectURL(input.files[0])
  }
}

function clearCover() {
  coverFile.value = null
  coverPreview.value = ''
  existingCoverUrl.value = ''
  form.bookImg = ''
}

async function handleSubmit() {
  error.value = ''
  if (!form.bookName || !form.bookAuthor || !form.bookDesc) {
    error.value = '请填写所有必填字段'
    return
  }
  loading.value = true
  try {
    // 如果有新文件，先上传封面
    if (coverFile.value) {
      uploadingCover.value = true
      const uploadRes = await bookApi.uploadCover(coverFile.value)
      if (uploadRes.data.code === 200) {
        form.bookImg = uploadRes.data.data.url
      } else {
        error.value = uploadRes.data.message || '封面上传失败'
        loading.value = false
        uploadingCover.value = false
        return
      }
      uploadingCover.value = false
    }

    let res
    if (isEdit.value) {
      res = await bookApi.updateBook(Number(route.params.id), form)
    } else {
      res = await bookApi.addBook(form)
    }
    if (res.data.code === 200) {
      router.push('/admin/books')
    } else {
      error.value = res.data.message || '操作失败'
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || '操作失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!isLoggedIn.value || !isAdmin.value) {
    router.push('/')
    return
  }
  fetchBookTypes()
  if (route.params.id && route.params.id !== 'new') {
    isEdit.value = true
    fetchBook()
  }
})

// 监听路由参数变化，确保进入不同编辑页能正确加载
watch(() => route.params.id, () => {
  if (route.params.id && route.params.id !== 'new') {
    isEdit.value = true
    fetchBook()
  } else {
    isEdit.value = false
  }
})
</script>

<template>
  <Layout>
    <div class="max-w-2xl mx-auto">
      <div class="flex items-center gap-2 mb-6">
        <Save class="w-5 h-5 text-indigo-600" />
        <h1 class="text-2xl font-bold text-gray-900">{{ isEdit ? '编辑图书' : '添加图书' }}</h1>
      </div>

      <div class="bg-white rounded-xl shadow-sm p-8">
        <div v-if="fetchingBook" class="text-center py-12 text-gray-500">加载图书信息...</div>

        <template v-else>
        <div v-if="error" class="mb-4 p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-sm">
          {{ error }}
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">书名 *</label>
            <input
              v-model="form.bookName"
              type="text"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              placeholder="请输入书名"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">作者 *</label>
            <input
              v-model="form.bookAuthor"
              type="text"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              placeholder="请输入作者"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">价格</label>
              <input
                v-model.number="form.bookPrice"
                type="number"
                step="0.01"
                min="0"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">分类</label>
              <select
                v-model.number="form.bookTypeId"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              >
                <option v-for="type in bookTypes" :key="type.bookTypeId" :value="type.bookTypeId">{{ type.bookTypeName }}</option>
              </select>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">库存</label>
            <input
              v-model.number="form.bookStock"
              type="number"
              min="0"
              step="1"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              placeholder="请输入库存数量"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">描述 *</label>
            <textarea
              v-model="form.bookDesc"
              rows="4"
              class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none"
              placeholder="请输入图书描述"
            ></textarea>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">封面图片</label>
            <div class="flex items-start gap-4">
              <!-- Preview -->
              <div v-if="coverPreview || existingCoverUrl" class="w-32 h-40 rounded-lg overflow-hidden border border-gray-200 flex-shrink-0">
                <img
                  :src="coverPreview || existingCoverUrl"
                  alt="封面预览"
                  class="w-full h-full object-cover"
                />
              </div>
              <div v-else class="w-32 h-40 rounded-lg border-2 border-dashed border-gray-300 flex items-center justify-center flex-shrink-0 bg-gray-50">
                <Upload class="w-6 h-6 text-gray-400" />
              </div>

              <div class="flex-1">
                <label class="inline-flex items-center gap-2 px-4 py-2.5 bg-white border border-gray-300 rounded-lg cursor-pointer hover:border-indigo-400 hover:text-indigo-600 transition-colors text-sm">
                  <Upload class="w-4 h-4" />
                  选择图片
                  <input
                    type="file"
                    accept="image/*"
                    class="hidden"
                    @change="handleFileSelect"
                  />
                </label>
                <p class="text-xs text-gray-400 mt-2">支持 jpg、png、gif、webp 格式</p>
                <button
                  v-if="coverPreview || existingCoverUrl"
                  @click="clearCover"
                  type="button"
                  class="mt-2 text-xs text-red-500 hover:text-red-600 flex items-center gap-1"
                >
                  <X class="w-3 h-3" />
                  移除封面
                </button>
              </div>
            </div>
          </div>

          <div class="flex gap-3 pt-2">
            <button
              type="submit"
              :disabled="loading"
              class="flex-1 bg-indigo-600 text-white py-2.5 rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors flex items-center justify-center gap-2"
            >
              <Save class="w-4 h-4" />
              {{ loading ? '保存中...' : '保存' }}
            </button>
            <button
              type="button"
              @click="router.back()"
              class="px-6 py-2.5 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
            >
              取消
            </button>
          </div>
        </form>
        </template>
      </div>
    </div>
  </Layout>
</template>