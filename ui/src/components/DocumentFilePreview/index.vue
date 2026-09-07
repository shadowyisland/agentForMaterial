<template>
  <el-dialog
    :title="mode === 'template' ? '模版预览' : '上传文件预览'"
    :visible.sync="open"
    width="92%"
    top="3vh"
    append-to-body
    @close="cancelPending"
    @closed="$emit('close')"
  >
    <div class="preview-toolbar">
      <el-button-group>
        <el-button size="small" icon="el-icon-zoom-out" :disabled="loading || zoom <= 25" @click="changeZoom(-25)">缩小</el-button>
        <el-button size="small" :disabled="loading" @click="setZoom(100)">{{ zoom }}%</el-button>
        <el-button size="small" icon="el-icon-zoom-in" :disabled="loading || zoom >= 200" @click="changeZoom(25)">放大</el-button>
      </el-button-group>
      <el-button size="small" :disabled="loading" @click="fitWidth">适应宽度</el-button>
      <div v-if="pageCount > 1" class="preview-pagination">
        <el-button size="small" icon="el-icon-arrow-left" :disabled="loading || page === 1" @click="changePage(-1)">上一页</el-button>
        <span>{{ page }} / {{ pageCount }}</span>
        <el-button size="small" :disabled="loading || page === pageCount" @click="changePage(1)">下一页<i class="el-icon-arrow-right" /></el-button>
      </div>
    </div>
    <div ref="viewport" v-loading="loading" class="preview-viewport">
      <el-empty v-if="error" :description="error">
        <el-button size="small" @click="load">重试</el-button>
      </el-empty>
      <div v-show="!error" class="preview-page" :style="pageStyle">
        <div ref="content" class="preview-content" :style="contentStyle" @click.capture.prevent />
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { previewDocumentTemplate, getTemplatePreviewInfo, getUploadPreviewInfo, previewUploadedPage } from '@/api/system/document'
import { blobValidate } from '@/utils/ruoyi'

export default {
  name: 'DocumentFilePreview',
  props: {
    documentId: { type: Number, required: true },
    mode: { type: String, required: true }
  },
  data() {
    return {
      open: true,
      loading: true,
      error: '',
      zoom: 100,
      page: 1,
      pageCount: 1,
      contentWidth: 0,
      contentHeight: 0,
      requestId: 0,
      imageUrl: ''
    }
  },
  computed: {
    pageStyle() {
      return { width: this.contentWidth * this.zoom / 100 + 'px', height: this.contentHeight * this.zoom / 100 + 'px' }
    },
    contentStyle() {
      return { transform: `scale(${this.zoom / 100})` }
    }
  },
  beforeDestroy() {
    this.cancelPending()
    this.releaseImage()
  },
  mounted() {
    this.$nextTick(this.load)
  },
  methods: {
    async load() {
      const requestId = ++this.requestId
      this.loading = true
      this.error = ''
      let imageUrl = ''
      try {
        // 在独立容器完成渲染，关闭弹窗后到达的响应不能覆盖当前预览。
        const container = document.createElement('div')
        const isTemplate = this.mode === 'template'
        const getInfo = isTemplate ? getTemplatePreviewInfo : getUploadPreviewInfo
        const getPage = isTemplate ? previewDocumentTemplate : previewUploadedPage
        const info = await getInfo(this.documentId)
        if (requestId !== this.requestId) return
        this.pageCount = info.data.pageCount
        const blob = await getPage(this.documentId, this.page)
        await this.checkBlob(blob)
        imageUrl = URL.createObjectURL(blob)
        const img = new Image()
        await new Promise((resolve, reject) => {
          img.onload = resolve
          img.onerror = () => reject(new Error('图片加载失败'))
          img.src = imageUrl
        })
        // 固定版式页面以 144 dpi 渲染，按 96 dpi 展示，缩放不改变排版。
        img.style.width = (info.data.type === 'pdf' ? img.naturalWidth / 1.5 : img.naturalWidth) + 'px'
        img.style.display = 'block'
        img.alt = (isTemplate ? '模版' : '上传文件') + '第 ' + this.page + ' 页'
        container.appendChild(img)
        if (requestId !== this.requestId) return
        this.releaseImage()
        this.imageUrl = imageUrl
        imageUrl = ''
        this.$refs.content.replaceChildren(container)
        this.contentWidth = this.$refs.content.scrollWidth
        this.contentHeight = this.$refs.content.scrollHeight
        this.fitWidth()
      } catch (error) {
        if (requestId === this.requestId) this.error = error.message || '预览加载失败'
      } finally {
        if (imageUrl) URL.revokeObjectURL(imageUrl)
        if (requestId === this.requestId) this.loading = false
      }
    },
    async checkBlob(blob) {
      if (!blobValidate(blob)) {
        let message = '预览文件加载失败'
        try { message = JSON.parse(await blob.text()).msg || message } catch (e) {}
        throw new Error(message)
      }
    },
    changePage(offset) {
      this.page += offset
      this.load()
    },
    changeZoom(offset) {
      this.setZoom(this.zoom + offset)
    },
    setZoom(value) {
      this.zoom = Math.max(25, Math.min(200, value))
    },
    fitWidth() {
      if (this.contentWidth) this.setZoom(Math.min(100, Math.floor((this.$refs.viewport.clientWidth - 48) / this.contentWidth * 100)))
    },
    cancelPending() {
      this.requestId++
    },
    releaseImage() {
      if (this.imageUrl) URL.revokeObjectURL(this.imageUrl)
      this.imageUrl = ''
    }
  }
}
</script>

<style scoped>
.preview-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}
.preview-pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}
.preview-viewport {
  height: 70vh;
  padding: 24px;
  overflow: auto;
  background: #edf1f5;
}
.preview-page {
  position: relative;
  margin: 0 auto;
}
.preview-content {
  position: absolute;
  top: 0;
  left: 0;
  width: max-content;
  transform-origin: top left;
}
</style>
