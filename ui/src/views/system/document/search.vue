<template>
  <div class="app-container search-page">
    <section class="search-hero">
      <div class="search-icon"><i class="el-icon-search" /></div>
      <div class="hero-copy">
        <div class="page-kicker">DOCUMENT INTELLIGENCE</div>
        <h1>文档检索</h1>
      </div>
      <div class="search-box">
        <el-input v-model="query.keyword" size="medium" clearable placeholder="输入一个或多个关键词，以空格分隔" @keyup.enter.native="handleSearch">
          <el-button slot="append" icon="el-icon-search" :loading="loading" @click="handleSearch">检索</el-button>
        </el-input>
        <div class="search-options">
          <span>检索范围</span>
          <el-radio-group v-model="query.searchScope" size="small" @change="searched && handleSearch()">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="ocr">OCR 正文</el-radio-button>
            <el-radio-button label="record">批注与使用记录</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </section>

    <section v-if="searched" class="result-section">
      <div class="result-summary">
        <div><strong>{{ total }}</strong> 个匹配文档 <span v-if="lastKeyword">· “{{ lastKeyword }}”</span></div>
        <el-button type="text" icon="el-icon-refresh" @click="handleSearch">刷新结果</el-button>
      </div>

      <div v-loading="loading" class="result-list">
        <article v-for="item in resultList" :key="item.documentId" class="result-card" @click="openDocument(item)">
          <div class="file-mark"><i class="el-icon-document" /></div>
          <div class="result-content">
            <header>
              <h2>{{ item.documentName }}</h2>
              <div class="badges">
                <el-tag size="mini" :type="item.documentType === 'EXTERNAL' ? 'warning' : ''">{{ typeLabel(item) }}</el-tag>
                <el-tag size="mini" effect="plain" type="info">命中：{{ item.matchSource || '-' }}</el-tag>
                <el-tag size="mini" effect="plain" :type="ocrType(item)">{{ ocrLabel(item) }}</el-tag>
              </div>
            </header>
            <p class="snippet">
              <template v-for="(part, index) in highlightParts(item.matchSnippet)"><mark v-if="part.hit" :key="index">{{ part.text }}</mark><span v-else :key="index">{{ part.text }}</span></template>
            </p>
            <footer>
              <span><i class="el-icon-user" /> {{ item.createBy || '-' }}</span>
              <span><i class="el-icon-time" /> {{ parseTime(item.createTime) }}</span>
              <span v-if="item.sourceName"><i class="el-icon-link" /> {{ item.sourceName }}</span>
              <div class="result-actions">
                <el-button size="mini" plain icon="el-icon-download" @click.stop="downloadDocument(item)">下载</el-button>
                <el-button size="mini" type="primary" icon="el-icon-view" @click.stop="openDocument(item)">查看详情</el-button>
              </div>
            </footer>
          </div>
        </article>
        <el-empty v-if="!loading && !resultList.length" description="没有找到匹配文档，请调整关键词或检索范围" />
      </div>

      <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="search" />
    </section>

    <el-drawer :visible.sync="detailOpen" :size="drawerSize" custom-class="document-detail-drawer" append-to-body @closed="resetDetail">
      <div slot="title" class="drawer-title"><span>文档详情</span><small>{{ detailForm.documentName }}</small></div>
      <div v-loading="detailLoading" class="drawer-body">
        <el-tabs v-model="detailTab" stretch>
          <el-tab-pane label="基本信息" name="basic">
            <div class="detail-grid">
              <div class="detail-item is-wide"><span>文档名称</span><strong>{{ detailForm.documentName || '-' }}</strong></div>
              <div><span>产品名称</span><strong>{{ detailForm.productName || '-' }}</strong></div>
              <div><span>产品型号</span><strong>{{ detailForm.productModel || '-' }}</strong></div>
              <div><span>内部编号</span><strong>{{ detailForm.internalCode || '-' }}</strong></div>
              <template v-if="detailForm.documentType === 'EXTERNAL'">
                <div><span>来源单位/网站</span><strong>{{ detailForm.sourceName || '-' }}</strong></div>
                <div><span>发布日期</span><strong>{{ detailForm.publishDate || '-' }}</strong></div>
                <div class="is-wide"><span>原始链接</span><a v-if="detailForm.sourceUrl" :href="detailForm.sourceUrl" target="_blank" rel="noopener noreferrer">{{ detailForm.sourceUrl }}</a><strong v-else>-</strong></div>
              </template>
              <div class="is-wide"><span>标签</span><div class="detail-tags"><el-tag v-for="tag in detailForm.tags || []" :key="tag" size="small">{{ tag }}</el-tag><strong v-if="!detailForm.tags || !detailForm.tags.length">-</strong></div></div>
            </div>
            <div class="metadata-row"><span><i class="el-icon-user" /> {{ detailForm.createBy || '-' }}</span><span><i class="el-icon-time" /> {{ parseTime(detailForm.createTime) }}</span><span><i class="el-icon-paperclip" /> {{ detailForm.fileOriginName || '-' }}</span></div>
            <div class="drawer-actions"><el-button type="primary" icon="el-icon-download" @click="downloadDocument(detailForm)">下载文件</el-button></div>
          </el-tab-pane>
          <el-tab-pane label="OCR 正文" name="ocr">
            <div class="ocr-toolbar"><el-tag :type="ocrType(detailForm)">{{ ocrLabel(detailForm) }}</el-tag><span v-if="detailForm.ocrTime">{{ parseTime(detailForm.ocrTime) }}</span></div>
            <el-alert v-if="detailForm.ocrError" :title="detailForm.ocrError" type="error" show-icon :closable="false" class="ocr-error" />
            <div v-if="detailForm.ocrContent" class="ocr-content">{{ detailForm.ocrContent }}</div><el-empty v-else description="暂无 OCR 正文" />
          </el-tab-pane>
          <el-tab-pane name="records">
            <span slot="label">批注与记录 <el-badge :value="records.length" :hidden="!records.length" /></span>
            <div v-if="canAddRecord" class="record-editor">
              <div class="record-editor-head"><strong>添加内容</strong><el-radio-group v-model="recordKind" size="mini"><el-radio-button label="remark">批注</el-radio-button><el-radio-button label="usage">使用记录</el-radio-button></el-radio-group></div>
              <el-input v-if="recordKind === 'remark'" v-model="recordForm.remark" type="textarea" :rows="3" maxlength="4000" show-word-limit placeholder="写下对该文档的批注…" />
              <el-input v-else v-model="recordForm.usageContent" type="textarea" :rows="3" maxlength="4000" show-word-limit placeholder="记录该文档的使用情况…" />
              <div class="record-editor-actions"><el-button type="primary" :loading="recordSubmitting" @click="submitRecord">{{ recordKind === 'remark' ? '发布批注' : '添加记录' }}</el-button></div>
            </div>
            <div v-loading="recordLoading" class="record-list">
              <article v-for="record in records" :key="record.recordId" class="record-item">
                <header><div class="record-avatar">{{ (record.createBy || '?').slice(0, 1).toUpperCase() }}</div><div><strong>{{ record.createBy || '未知用户' }}</strong><span>{{ parseTime(record.createTime) }}</span></div><div v-if="editingRecordId !== record.recordId" class="record-actions"><el-button v-if="canEditRecord(record)" type="text" icon="el-icon-edit" @click="editRecord(record)">编辑</el-button><el-button v-if="isAdmin" type="text" icon="el-icon-delete" class="danger-text" @click="deleteRecord(record)">删除</el-button></div></header>
                <div v-if="editingRecordId === record.recordId" class="inline-record-editor">
                  <el-radio-group v-model="inlineRecordKind" size="mini"><el-radio-button label="remark">批注</el-radio-button><el-radio-button label="usage">使用记录</el-radio-button></el-radio-group>
                  <el-input v-if="inlineRecordKind === 'remark'" v-model="inlineRecordForm.remark" type="textarea" :rows="3" maxlength="4000" show-word-limit />
                  <el-input v-else v-model="inlineRecordForm.usageContent" type="textarea" :rows="3" maxlength="4000" show-word-limit />
                  <div><el-button size="small" @click="cancelRecordEdit">取消</el-button><el-button size="small" type="primary" :loading="inlineRecordSubmitting" @click="saveRecordEdit">保存修改</el-button></div>
                </div>
                <template v-else><div v-if="record.remark" class="record-content remark"><b>批注</b><p>{{ record.remark }}</p></div><div v-if="record.usageContent" class="record-content"><b>使用记录</b><p>{{ record.usageContent }}</p></div></template><footer v-if="record.updateTime && editingRecordId !== record.recordId">更新于 {{ parseTime(record.updateTime) }}</footer>
              </article>
              <el-empty v-if="!recordLoading && !records.length" description="还没有批注或使用记录" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { searchDocument, getDocument, listDocumentRecords, addDocumentRecord, updateDocumentRecord, delDocumentRecord } from '@/api/system/document'
const CATEGORY_LABEL = {
  RAW_MATERIAL: '原材料', ACRYLIC: '丙烯酸原材料', EPOXY: '环氧原材料', OTHER_RESIN: '其它树脂原材料',
  FILLER: '填料类原材料', SILICONE: '有机硅原材料', ADDITIVE: '助剂类原材料'
}

export default {
  name: 'DocumentSearch',
  data() {
    return {
      loading: false,
      searched: false,
      lastKeyword: '',
      total: 0,
      resultList: [],
      query: { keyword: '', searchScope: 'all', pageNum: 1, pageSize: 10 },
      windowWidth: window.innerWidth,
      detailOpen: false,
      detailLoading: false,
      detailTab: 'basic',
      detailForm: {},
      recordLoading: false,
      recordSubmitting: false,
      inlineRecordSubmitting: false,
      records: [],
      recordsLoaded: false,
      recordForm: { usageContent: '', remark: '' },
      recordKind: 'remark',
      editingRecordId: null,
      inlineRecordForm: { usageContent: '', remark: '' },
      inlineRecordKind: 'remark'
    }
  },
  computed: {
    drawerSize() { return this.windowWidth < 768 ? '100%' : '720px' },
    isAdmin() { return (this.$store.getters.roles || []).includes('admin') },
    canAddRecord() { return this.hasPermission('system:document:record:add') }
  },
  watch: {
    detailTab(value) { if (value === 'records' && this.detailForm.documentId && !this.recordsLoaded) this.loadRecords() }
  },
  created() { window.addEventListener('resize', this.handleResize) },
  beforeDestroy() { window.removeEventListener('resize', this.handleResize) },
  methods: {
    handleResize() { this.windowWidth = window.innerWidth },
    hasPermission(code) { const permissions = this.$store.getters.permissions || []; return permissions.includes('*:*:*') || permissions.includes(code) },
    handleSearch() {
      if (!this.query.keyword || !this.query.keyword.trim()) {
        this.$modal.msgWarning('请输入检索关键词')
        return
      }
      this.query.pageNum = 1
      this.search()
    },
    search() {
      this.loading = true
      this.searched = true
      this.lastKeyword = this.query.keyword.trim()
      searchDocument(Object.assign({}, this.query, { keyword: this.lastKeyword })).then(res => {
        this.resultList = res.rows || []
        this.total = res.total || 0
      }).finally(() => { this.loading = false })
    },
    typeLabel(item) {
      return item.documentType === 'EXTERNAL' ? '外部文档' : (CATEGORY_LABEL[item.materialCategory] || '内部文档')
    },
    ocrLabel(item) {
      if (Number(item.isRecognized) === 1) return 'OCR 已完成'
      if (Number(item.isRecognized) === 2) return 'OCR 失败'
      return '未 OCR'
    },
    ocrType(item) {
      if (Number(item.isRecognized) === 1) return 'success'
      if (Number(item.isRecognized) === 2) return 'danger'
      return 'info'
    },
    highlightParts(text) {
      const source = text || '命中内容暂无可展示摘要'
      const words = (this.lastKeyword.match(/\S+/g) || []).filter(Boolean)
      if (!words.length) return [{ text: source, hit: false }]
      const escaped = words.map(word => word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
      const pattern = new RegExp('(' + escaped.join('|') + ')', 'gi')
      return source.split(pattern).filter(Boolean).map(part => ({ text: part, hit: words.some(word => word.toLowerCase() === part.toLowerCase()) }))
    },
    openDocument(item) {
      this.detailOpen = true
      this.detailLoading = true
      this.detailTab = 'basic'
      this.records = []; this.recordsLoaded = false; this.cancelRecordEdit()
      getDocument(item.documentId).then(res => { this.detailForm = res.data || {}; this.loadRecords() }).finally(() => { this.detailLoading = false })
    },
    resetDetail() { this.detailForm = {}; this.records = []; this.recordsLoaded = false; this.resetRecordForm(); this.cancelRecordEdit() },
    downloadDocument(item) { if (item.filePath) this.$download.resource(item.filePath) },
    loadRecords() { this.recordLoading = true; listDocumentRecords(this.detailForm.documentId).then(res => { this.records = res.data || []; this.recordsLoaded = true }).finally(() => { this.recordLoading = false }) },
    resetRecordForm() { this.recordForm = { usageContent: '', remark: '' }; this.recordKind = 'remark' },
    submitRecord() {
      if (!this.recordForm.usageContent.trim() && !this.recordForm.remark.trim()) { this.$modal.msgWarning('请填写批注或使用记录'); return }
      this.recordSubmitting = true
      addDocumentRecord(this.detailForm.documentId, this.recordForm).then(() => { this.$modal.msgSuccess(this.recordKind === 'remark' ? '批注已发布' : '记录已添加'); this.resetRecordForm(); this.loadRecords() }).finally(() => { this.recordSubmitting = false })
    },
    canEditRecord(record) { return this.isAdmin || String(record.createUserId) === String(this.$store.getters.id) },
    editRecord(record) { this.editingRecordId = record.recordId; this.inlineRecordKind = record.remark ? 'remark' : 'usage'; this.inlineRecordForm = { usageContent: record.usageContent || '', remark: record.remark || '' } },
    cancelRecordEdit() { this.editingRecordId = null; this.inlineRecordKind = 'remark'; this.inlineRecordForm = { usageContent: '', remark: '' } },
    saveRecordEdit() {
      if (!this.inlineRecordForm.usageContent.trim() && !this.inlineRecordForm.remark.trim()) { this.$modal.msgWarning('请填写批注或使用记录'); return }
      this.inlineRecordSubmitting = true
      updateDocumentRecord(this.editingRecordId, this.inlineRecordForm).then(() => { this.$modal.msgSuccess('修改已保存'); this.cancelRecordEdit(); this.loadRecords() }).finally(() => { this.inlineRecordSubmitting = false })
    },
    deleteRecord(record) { this.$modal.confirm('确认删除这条批注或使用记录？').then(() => delDocumentRecord(record.recordId)).then(() => { this.$modal.msgSuccess('内容已删除'); this.loadRecords() }).catch(() => {}) }
  }
}
</script>

<style scoped lang="scss">
.search-page { min-height:calc(100vh - 84px); padding:28px; background:radial-gradient(circle at 88% 5%,#e3f2ff 0,transparent 28%),#f3f7fb; }
.search-hero { display:grid; grid-template-columns:auto minmax(190px,280px) minmax(360px,1fr); align-items:center; gap:24px; padding:30px 34px; border-radius:17px; color:#fff; background:linear-gradient(125deg,#0b4fae,#1678dc 62%,#1d9bc8); box-shadow:0 18px 42px rgba(13,77,151,.2); }
.search-icon { display:flex; align-items:center; justify-content:center; width:62px; height:62px; border:1px solid rgba(255,255,255,.24); border-radius:17px; font-size:29px; background:rgba(255,255,255,.12); }.page-kicker { color:#bfe2ff; font-size:11px; font-weight:700; letter-spacing:1.7px; }.hero-copy h1 { margin:5px 0 7px; font-size:28px; }.hero-copy p { margin:0; color:rgba(255,255,255,.78); line-height:1.6; }
.search-box { padding:19px; border:1px solid rgba(255,255,255,.2); border-radius:13px; background:rgba(4,48,101,.2); backdrop-filter:blur(8px); }.search-box ::v-deep .el-input__inner { height:46px; border:0; }.search-box ::v-deep .el-input-group__append { border:0; color:#fff; background:#113f75; }.search-options { display:flex; align-items:center; flex-wrap:wrap; gap:10px; margin-top:13px; }.search-options > span { font-size:12px; color:#d9edff; }
.result-section { max-width:1120px; margin:24px auto 0; }.result-summary { display:flex; align-items:center; justify-content:space-between; margin-bottom:11px; color:#5a6e83; }.result-summary strong { color:#126bd0; font-size:20px; }.result-summary span { color:#91a0ae; }.result-list { min-height:220px; }
.result-card { display:flex; gap:16px; margin-bottom:12px; padding:20px; border:1px solid #e1eaf4; border-radius:13px; background:#fff; box-shadow:0 6px 18px rgba(20,57,96,.045); cursor:pointer; transition:.2s ease; }.result-card:hover { transform:translateY(-2px); border-color:#b9d8f8; box-shadow:0 12px 28px rgba(18,89,163,.1); }.file-mark { display:flex; align-items:center; justify-content:center; flex:0 0 45px; width:45px; height:45px; border-radius:12px; color:#1a73d2; font-size:21px; background:#eaf4ff; }.result-content { min-width:0; flex:1; }.result-card header { display:flex; align-items:flex-start; gap:14px; }.result-card h2 { flex:1; margin:1px 0 0; color:#163452; font-size:17px; }.badges { display:flex; flex-wrap:wrap; justify-content:flex-end; gap:6px; }.snippet { margin:13px 0; padding:10px 13px; border-left:3px solid #5aa6eb; color:#52677c; background:#f6f9fc; line-height:1.65; word-break:break-word; }.snippet mark { padding:1px 2px; border-radius:3px; color:#785000; background:#ffe8a6; }.result-card footer { display:flex; align-items:center; flex-wrap:wrap; gap:14px 20px; color:#8b9aaa; font-size:12px; }.result-actions { display:flex; gap:8px; margin-left:auto; }
.drawer-title { display:flex; flex-direction:column; gap:4px; padding-right:36px; color:#19324d; }.drawer-title small { overflow:hidden; color:#8797a9; font-weight:400; text-overflow:ellipsis; white-space:nowrap; }.drawer-body { height:calc(100vh - 78px); padding:0 24px 28px; overflow:auto; }.detail-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; padding-top:10px; }.detail-grid > div { min-width:0; padding:14px; border:1px solid #e4ebf3; border-radius:10px; background:#f8fafc; }.detail-grid .is-wide { grid-column:1 / -1; }.detail-grid span,.detail-grid strong { display:block; }.detail-grid span { margin-bottom:6px; color:#8797a9; font-size:12px; }.detail-grid strong,.detail-grid a { color:#2c4359; font-weight:500; word-break:break-word; }.detail-tags { display:flex; flex-wrap:wrap; gap:7px; }.metadata-row { display:flex; flex-wrap:wrap; gap:12px 22px; margin-top:14px; padding:14px 16px; border-radius:9px; color:#65788c; background:#f5f8fc; }.drawer-actions { display:flex; justify-content:flex-end; margin-top:18px; }.ocr-toolbar { display:flex; align-items:center; gap:12px; margin:8px 0 16px; }.ocr-toolbar span { color:#8a98a7; font-size:12px; }.ocr-error { margin-bottom:14px; }.ocr-content { min-height:360px; padding:20px; border:1px solid #e2eaf3; border-radius:10px; color:#33485e; background:#f8fafc; white-space:pre-wrap; word-break:break-word; line-height:1.8; }.record-editor { display:grid; gap:12px; margin:8px 0 18px; padding:16px; border:1px solid #dce8f5; border-radius:11px; background:#f8fbff; }.record-editor-head { display:flex; align-items:center; justify-content:space-between; gap:12px; color:#27445f; }.record-editor-actions { text-align:right; }.record-list { min-height:220px; }.record-item { margin-bottom:12px; padding:16px; border:1px solid #e2eaf3; border-radius:11px; background:#fff; }.record-item header { display:flex; align-items:center; gap:10px; }.record-avatar { flex:0 0 34px; width:34px; height:34px; line-height:34px; border-radius:50%; color:#fff; background:linear-gradient(135deg,#176fd1,#22a1da); text-align:center; }.record-item header strong,.record-item header span { display:block; }.record-item header span,.record-item footer { margin-top:3px; font-size:11px; color:#96a3b1; }.record-actions { margin-left:auto; }.inline-record-editor { display:grid; gap:10px; margin-top:13px; padding:13px; border:1px solid #9ecbf3; border-radius:9px; background:#f7fbff; }.inline-record-editor > div:last-child { text-align:right; }.record-content { margin-top:13px; padding:11px 13px; border-radius:8px; background:#f5f8fc; }.record-content.remark { border-left:3px solid #e5a93d; background:#fff8eb; }.record-content b { color:#50677e; font-size:12px; }.record-content p { margin:5px 0 0; color:#2e4154; line-height:1.65; white-space:pre-wrap; }.record-item footer { text-align:right; }.danger-text { color:#e0525d !important; }
@media (max-width:900px) { .search-hero { grid-template-columns:auto 1fr; }.search-box { grid-column:1 / -1; } }
@media (max-width:600px) { .search-page { padding:12px; }.search-hero { display:block; padding:22px 18px; }.search-icon { display:none; }.hero-copy { margin-bottom:18px; }.result-card { padding:15px; }.file-mark { display:none; }.result-card header { display:block; }.badges { justify-content:flex-start; margin-top:9px; }.result-actions { width:100%; margin-left:0; }.detail-grid { grid-template-columns:1fr; }.detail-grid .is-wide { grid-column:auto; } }
</style>
