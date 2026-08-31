<template>
  <div class="app-container material-page">
    <section class="page-hero">
      <div>
        <div class="page-kicker">{{ isExternal ? 'EXTERNAL DOCUMENTS' : 'INTERNAL MATERIALS' }}</div>
        <h1>{{ pageTitle }}</h1>
      </div>
      <el-button v-hasPermi="['system:document:add']" type="primary" icon="el-icon-upload2" @click="handleAdd">上传文档</el-button>
    </section>

    <el-row :gutter="16" class="stat-grid">
      <el-col v-for="item in statCards" :key="item.key" :xs="12" :sm="6">
        <div class="stat-card" :class="'is-' + item.key">
          <i :class="item.icon" />
          <div><strong>{{ stats[item.key] || 0 }}</strong><span>{{ item.label }}</span></div>
        </div>
      </el-col>
    </el-row>

    <section class="content-card tag-panel">
      <div class="section-heading">
        <div><h2>分类标签</h2></div>
        <el-button v-if="isAdmin" type="text" icon="el-icon-setting" @click="openTagManager">管理标签</el-button>
      </div>
      <div class="tag-cloud">
        <el-tag :effect="!queryParams.searchTag ? 'dark' : 'plain'" class="filter-tag" @click="selectTag()">全部</el-tag>
        <el-tag v-for="tag in allTags" :key="tag" :effect="queryParams.searchTag === tag ? 'dark' : 'plain'" class="filter-tag" @click="selectTag(tag)">{{ tag }}</el-tag>
        <span v-if="!allTags.length" class="empty-hint">当前分类暂无标签</span>
      </div>
    </section>

    <section class="content-card document-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small" class="search-form">
        <el-form-item prop="documentName">
          <el-input v-model="queryParams.documentName" prefix-icon="el-icon-search" placeholder="搜索文档名称" clearable @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item prop="searchTag">
          <el-select v-model="queryParams.searchTag" filterable clearable placeholder="全部标签" @change="handleQuery">
            <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
        <div class="table-actions">
          <el-button v-hasPermi="['system:document:remove']" type="danger" plain size="small" icon="el-icon-delete" :disabled="!ids.length" @click="handleDelete()">批量删除</el-button>
        </div>
      </el-form>

      <el-table v-loading="loading" :data="documentList" row-key="documentId" class="document-table" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="44" fixed="left" />
        <el-table-column label="文档名称" prop="documentName" min-width="240" fixed="left" show-overflow-tooltip>
          <template slot-scope="scope">
            <button class="document-link" type="button" @click="handleDetail(scope.row)"><i class="el-icon-document" /><span>{{ scope.row.documentName }}</span></button>
          </template>
        </el-table-column>
        <el-table-column v-if="!isExternal && !isCompact" label="产品名称" prop="productName" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="isExternal && !isCompact" label="来源" prop="sourceName" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="!isCompact" label="文件标签" min-width="180">
          <template slot-scope="scope">
            <el-tag v-for="tag in (scope.row.tags || []).slice(0, 3)" :key="tag" size="mini" class="table-tag">{{ tag }}</el-tag>
            <span v-if="!scope.row.tags || !scope.row.tags.length" class="empty-hint">未添加</span>
          </template>
        </el-table-column>
        <el-table-column label="OCR 状态" width="112" align="center">
          <template slot-scope="scope"><el-tag :type="ocrMeta(scope.row).type" size="small" effect="plain"><i :class="ocrMeta(scope.row).icon" /> {{ ocrMeta(scope.row).label }}</el-tag></template>
        </el-table-column>
        <el-table-column v-if="!isCompact" label="上传人" prop="createBy" width="110" show-overflow-tooltip />
        <el-table-column v-if="!isCompact" label="上传时间" prop="createTime" width="168"><template slot-scope="scope">{{ parseTime(scope.row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="224" align="right" fixed="right" class-name="operation-column">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-view" @click="handleDetail(scope.row)">详情</el-button>
            <el-button type="text" icon="el-icon-download" @click="handleDownload(scope.row)">下载</el-button>
            <el-button v-hasPermi="['system:document:edit']" type="text" icon="el-icon-cpu" @click="handleOcr(scope.row)">{{ scope.row.isRecognized === 2 ? '重试' : 'OCR' }}</el-button>
            <el-dropdown v-if="canRemoveDocument" trigger="click" @command="handleRowCommand($event, scope.row)">
              <el-button type="text" class="more-button"><i class="el-icon-more" /></el-button>
              <el-dropdown-menu slot="dropdown"><el-dropdown-item command="delete" icon="el-icon-delete">删除文档</el-dropdown-item></el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog :title="uploadTitle" :visible.sync="uploadOpen" width="680px" custom-class="material-dialog" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="106px">
        <el-form-item label="文档名称" prop="documentName"><el-input v-model="form.documentName" placeholder="请输入文档显示名称" /></el-form-item>
        <el-form-item label="文件上传" prop="filePath">
          <el-upload ref="upload" drag :limit="1" accept=".pdf,.png,.jpg,.jpeg" :action="uploadUrl" :headers="headers" :file-list="fileList" :before-upload="handleBeforeUpload" :on-success="handleUploadSuccess" :on-remove="handleRemove">
            <i class="el-icon-upload" /><div class="el-upload__text">拖放文件到这里，或 <em>点击选择</em></div>
            <div slot="tip" class="el-upload__tip">支持 PDF、JPG、PNG，单个文件不超过 50MB；OCR 需上传后手动触发。</div>
          </el-upload>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :sm="8" :xs="24"><el-form-item label="产品名称"><el-input v-model="form.productName" /></el-form-item></el-col>
          <el-col :sm="8" :xs="24"><el-form-item label="产品型号"><el-input v-model="form.productModel" /></el-form-item></el-col>
          <el-col :sm="8" :xs="24"><el-form-item label="内部编号"><el-input v-model="form.internalCode" /></el-form-item></el-col>
        </el-row>
        <template v-if="isExternal">
          <el-form-item label="来源单位/网站"><el-input v-model="form.sourceName" placeholder="可选" /></el-form-item>
          <el-form-item label="原始链接" prop="sourceUrl"><el-input v-model="form.sourceUrl" placeholder="https://...（可选）" /></el-form-item>
          <el-form-item label="发布日期"><el-date-picker v-model="form.publishDate" type="date" value-format="yyyy-MM-dd" placeholder="可选" /></el-form-item>
        </template>
        <el-form-item label="文档标签">
          <el-select v-model="form.tags" multiple filterable allow-create default-first-option style="width:100%" placeholder="选择或输入新标签后回车"><el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" /></el-select>
        </el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="uploadOpen = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitForm">确认上传</el-button></div>
    </el-dialog>

    <el-drawer :visible.sync="detailOpen" :size="drawerSize" custom-class="document-detail-drawer" append-to-body @closed="resetDetail">
      <div slot="title" class="drawer-title"><span>文档详情</span><small>{{ detailForm.documentName }}</small></div>
      <div v-loading="detailLoading" class="drawer-body">
        <el-tabs v-model="detailTab" stretch>
          <el-tab-pane label="基本信息" name="basic">
            <el-form :model="detailForm" label-position="top" class="detail-form">
              <el-form-item label="文档名称"><el-input v-model="detailForm.documentName" :disabled="!canEditDocument" /></el-form-item>
              <el-row :gutter="14">
                <el-col :sm="8" :xs="24"><el-form-item label="产品名称"><el-input v-model="detailForm.productName" :disabled="!canEditDocument" /></el-form-item></el-col>
                <el-col :sm="8" :xs="24"><el-form-item label="产品型号"><el-input v-model="detailForm.productModel" :disabled="!canEditDocument" /></el-form-item></el-col>
                <el-col :sm="8" :xs="24"><el-form-item label="内部编号"><el-input v-model="detailForm.internalCode" :disabled="!canEditDocument" /></el-form-item></el-col>
              </el-row>
              <template v-if="detailForm.documentType === 'EXTERNAL'">
                <el-form-item label="来源单位/网站"><el-input v-model="detailForm.sourceName" :disabled="!canEditDocument" /></el-form-item>
                <el-form-item label="原始链接"><el-input v-model="detailForm.sourceUrl" :disabled="!canEditDocument" /></el-form-item>
                <el-form-item label="发布日期"><el-date-picker v-model="detailForm.publishDate" type="date" value-format="yyyy-MM-dd" :disabled="!canEditDocument" /></el-form-item>
              </template>
              <el-form-item label="标签"><el-select v-model="detailForm.tags" multiple filterable allow-create default-first-option style="width:100%" :disabled="!canEditDocument"><el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" /></el-select></el-form-item>
              <div class="metadata-row"><span><i class="el-icon-user" /> {{ detailForm.createBy || '-' }}</span><span><i class="el-icon-time" /> {{ parseTime(detailForm.createTime) }}</span><span><i class="el-icon-paperclip" /> {{ detailForm.fileOriginName || '-' }}</span></div>
              <div class="drawer-actions"><el-button icon="el-icon-download" @click="handleDownload(detailForm)">下载文件</el-button><el-button v-if="canEditDocument" type="primary" :loading="submitting" @click="saveDetail">保存修改</el-button></div>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="OCR 正文" name="ocr">
            <div class="ocr-toolbar"><div><el-tag :type="ocrMeta(detailForm).type">{{ ocrMeta(detailForm).label }}</el-tag><span v-if="detailForm.ocrTime">{{ parseTime(detailForm.ocrTime) }}</span></div><el-button v-if="canEditDocument" type="primary" plain icon="el-icon-cpu" @click="handleOcr(detailForm)">{{ detailForm.isRecognized === 2 ? '重试 OCR' : '执行 OCR' }}</el-button></div>
            <el-alert v-if="detailForm.ocrError" :title="detailForm.ocrError" type="error" show-icon :closable="false" class="ocr-error" />
            <div v-if="detailForm.ocrContent" class="ocr-content">{{ detailForm.ocrContent }}</div><el-empty v-else description="暂无 OCR 正文，请手动执行识别" />
          </el-tab-pane>
          <el-tab-pane name="records">
            <span slot="label">批注与记录 <el-badge :value="records.length" :hidden="!records.length" /></span>
            <div class="record-editor">
              <div class="record-editor-head">
                <strong>添加内容</strong>
                <el-radio-group v-model="recordKind" size="mini">
                  <el-radio-button label="remark">批注</el-radio-button>
                  <el-radio-button label="usage">使用记录</el-radio-button>
                </el-radio-group>
              </div>
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

    <el-drawer title="标签管理" :visible.sync="tagManagerOpen" :size="drawerSize" append-to-body>
      <div class="tag-manager-body">
        <el-alert title="停用标签后，用户无法在当前分类中选择该标签，已有文档关联不会丢失。" type="info" :closable="false" show-icon />
        <div class="new-tag-row"><el-input v-model="newTagName" maxlength="64" placeholder="输入新标签名称" @keyup.enter.native="createTag" /><el-button type="primary" icon="el-icon-plus" :loading="tagSubmitting" @click="createTag">新增</el-button></div>
        <el-table v-loading="tagManagerLoading" :data="managedTags">
          <el-table-column label="标签" prop="tagName" min-width="140" /><el-table-column label="文档数" prop="documentCount" width="90" align="center" /><el-table-column label="创建人" prop="createBy" width="110" />
          <el-table-column label="状态" width="100" align="center"><template slot-scope="scope"><el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="changeManagedTagStatus(scope.row)" /></template></el-table-column>
          <el-table-column label="操作" width="80" align="right"><template slot-scope="scope"><el-button type="text" class="danger-text" @click="deleteManagedTag(scope.row)">删除</el-button></template></el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { listDocument, getDocument, addDocument, updateDocument, delDocument, ocrDocument, getTopTags, getDocumentStats, listDocumentRecords, addDocumentRecord, updateDocumentRecord, delDocumentRecord } from '@/api/system/document'
import { listTag, addTag, delTag, changeTagStatus } from '@/api/system/tag'
import { getToken } from '@/utils/auth'

const CATEGORY_META = {
  RAW_MATERIAL: '原材料', ACRYLIC: '丙烯酸原材料', EPOXY: '环氧原材料', OTHER_RESIN: '其它树脂原材料',
  FILLER: '填料类原材料', SILICONE: '有机硅原材料', ADDITIVE: '助剂类原材料'
}

export default {
  name: 'Document',
  data() {
    return {
      windowWidth: window.innerWidth, loading: false, detailLoading: false, submitting: false,
      recordLoading: false, recordSubmitting: false, inlineRecordSubmitting: false, tagManagerLoading: false, tagSubmitting: false,
      ids: [], total: 0, documentList: [], allTags: [],
      stats: { total: 0, recognized: 0, pending: 0, failed: 0 },
      statCards: [
        { key: 'total', label: '文档总数', icon: 'el-icon-folder-opened' },
        { key: 'recognized', label: 'OCR 已完成', icon: 'el-icon-circle-check' },
        { key: 'pending', label: '待识别', icon: 'el-icon-time' },
        { key: 'failed', label: '识别失败', icon: 'el-icon-warning-outline' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, documentName: undefined, searchTag: undefined },
      uploadOpen: false, detailOpen: false, detailTab: 'basic', detailForm: {}, form: {}, fileList: [],
      rules: {
        documentName: [{ required: true, message: '文档名称不能为空', trigger: 'blur' }],
        filePath: [{ required: true, message: '请先上传文件', trigger: 'change' }],
        sourceUrl: [{ type: 'url', message: '请输入完整的 http(s) 链接', trigger: 'blur' }]
      },
      uploadUrl: process.env.VUE_APP_BASE_API + '/common/upload', headers: { Authorization: 'Bearer ' + getToken() },
      records: [], recordsLoaded: false, recordForm: { usageContent: '', remark: '' }, recordKind: 'remark', editingRecordId: null,
      inlineRecordForm: { usageContent: '', remark: '' }, inlineRecordKind: 'remark',
      tagManagerOpen: false, managedTags: [], newTagName: ''
    }
  },
  computed: {
    documentType() { return (this.$route.query && this.$route.query.documentType) || 'INTERNAL' },
    materialCategory() { return this.documentType === 'INTERNAL' ? ((this.$route.query && this.$route.query.materialCategory) || 'RAW_MATERIAL') : undefined },
    isExternal() { return this.documentType === 'EXTERNAL' },
    pageTitle() { return this.isExternal ? '外部文档' : (CATEGORY_META[this.materialCategory] || CATEGORY_META.RAW_MATERIAL) },
    uploadTitle() { return '上传至「' + this.pageTitle + '」' },
    isCompact() { return this.windowWidth < 1200 },
    drawerSize() { return this.windowWidth < 768 ? '100%' : '720px' },
    isAdmin() { return (this.$store.getters.roles || []).includes('admin') },
    canEditDocument() { return this.hasPermission('system:document:edit') },
    canRemoveDocument() { return this.hasPermission('system:document:remove') }
  },
  watch: {
    '$route.fullPath'() { this.initializePage() },
    detailTab(value) { if (value === 'records' && this.detailForm.documentId && !this.recordsLoaded) this.loadRecords() }
  },
  created() { this.resetFormData(); this.initializePage(); window.addEventListener('resize', this.handleResize) },
  beforeDestroy() { window.removeEventListener('resize', this.handleResize) },
  methods: {
    scopeParams() { return { documentType: this.documentType, materialCategory: this.materialCategory } },
    initializePage() {
      this.queryParams.pageNum = 1; this.queryParams.documentName = undefined; this.queryParams.searchTag = undefined
      this.getList(); this.getTagsList(); this.getStats()
      const documentId = this.$route.query && this.$route.query.documentId
      if (documentId) this.handleDetail({ documentId })
    },
    handleResize() { this.windowWidth = window.innerWidth },
    hasPermission(code) { const permissions = this.$store.getters.permissions || []; return permissions.includes('*:*:*') || permissions.includes(code) },
    getList() {
      this.loading = true
      listDocument(Object.assign({}, this.queryParams, this.scopeParams())).then(res => { this.documentList = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false })
    },
    getTagsList() { getTopTags(this.scopeParams()).then(res => { this.allTags = res.data || [] }) },
    getStats() { getDocumentStats(this.scopeParams()).then(res => { this.stats = Object.assign({}, this.stats, res.data || {}) }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams.documentName = undefined; this.queryParams.searchTag = undefined; this.handleQuery() },
    selectTag(tag) { this.queryParams.searchTag = tag; this.handleQuery() },
    handleSelectionChange(rows) { this.ids = rows.map(item => item.documentId) },
    resetFormData() {
      this.form = { documentName: '', filePath: '', fileOriginName: '', fileSuffix: '', fileSize: null, mimeType: '', productName: '', productModel: '', internalCode: '', sourceName: '', sourceUrl: '', publishDate: null, tags: [] }
      this.fileList = []
      if (this.$refs.form) this.$refs.form.clearValidate()
    },
    handleAdd() { this.resetFormData(); this.uploadOpen = true },
    handleBeforeUpload(file) { const valid = file.size / 1024 / 1024 <= 50; if (!valid) this.$modal.msgError('文件大小不能超过 50MB'); return valid },
    handleUploadSuccess(res, file) {
      if (res.code !== 200) { this.$modal.msgError(res.msg || '文件上传失败'); this.$refs.upload.clearFiles(); return }
      const originalName = res.originalFilename || file.name
      this.form.filePath = res.fileName; this.form.fileOriginName = originalName; this.form.fileSuffix = originalName.includes('.') ? originalName.split('.').pop() : ''
      this.form.fileSize = file.size; this.form.mimeType = file.raw ? file.raw.type : ''
      if (!this.form.documentName) this.form.documentName = originalName.replace(/\.[^.]+$/, '')
      this.$nextTick(() => this.$refs.form && this.$refs.form.validateField('filePath'))
    },
    handleRemove() { this.form.filePath = ''; this.form.fileOriginName = ''; this.form.fileSuffix = ''; this.form.fileSize = null; this.form.mimeType = '' },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        addDocument(Object.assign({}, this.form, this.scopeParams())).then(() => { this.$modal.msgSuccess('文档上传成功'); this.uploadOpen = false; this.getList(); this.getTagsList(); this.getStats() }).finally(() => { this.submitting = false })
      })
    },
    handleDetail(row) {
      this.detailOpen = true; this.detailLoading = true; this.detailTab = 'basic'
      this.records = []; this.recordsLoaded = false; this.cancelRecordEdit()
      getDocument(row.documentId).then(res => { this.detailForm = res.data || {}; if (!this.detailForm.tags) this.$set(this.detailForm, 'tags', []); this.loadRecords() }).finally(() => { this.detailLoading = false })
    },
    resetDetail() { this.detailForm = {}; this.records = []; this.recordsLoaded = false; this.resetRecordForm(); this.cancelRecordEdit() },
    saveDetail() {
      this.submitting = true
      updateDocument(this.detailForm).then(() => { this.$modal.msgSuccess('文档信息已更新'); this.getList(); this.getTagsList() }).finally(() => { this.submitting = false })
    },
    handleDownload(row) { if (row.filePath) this.$download.resource(row.filePath) },
    handleOcr(row) {
      const loading = this.$loading({ lock: true, text: '正在识别文档，请稍候…', spinner: 'el-icon-loading', background: 'rgba(7, 20, 39, .62)' })
      ocrDocument(row.documentId).then(() => { this.$modal.msgSuccess('OCR 识别完成'); this.getList(); this.getStats(); if (this.detailOpen) this.handleDetail(row) }).finally(() => loading.close())
    },
    ocrMeta(row) {
      if (Number(row.isRecognized) === 1) return { label: '已识别', type: 'success', icon: 'el-icon-circle-check' }
      if (Number(row.isRecognized) === 2) return { label: '识别失败', type: 'danger', icon: 'el-icon-warning-outline' }
      return { label: '待识别', type: 'info', icon: 'el-icon-time' }
    },
    handleRowCommand(command, row) { if (command === 'delete') this.handleDelete(row) },
    handleDelete(row) {
      const ids = row && row.documentId ? [row.documentId] : this.ids
      if (!ids.length) return
      this.$modal.confirm('删除后文档记录也会一并清理，是否继续？').then(() => delDocument(ids.join(','))).then(() => { this.$modal.msgSuccess('删除成功'); this.ids = []; this.getList(); this.getTagsList(); this.getStats() }).catch(() => {})
    },
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
    deleteRecord(record) { this.$modal.confirm('确认删除这条批注或使用记录？').then(() => delDocumentRecord(record.recordId)).then(() => { this.$modal.msgSuccess('内容已删除'); this.loadRecords() }).catch(() => {}) },
    openTagManager() { this.tagManagerOpen = true; this.loadManagedTags() },
    loadManagedTags() { this.tagManagerLoading = true; listTag(Object.assign({ pageNum: 1, pageSize: 1000 }, this.scopeParams())).then(res => { this.managedTags = res.rows || [] }).finally(() => { this.tagManagerLoading = false }) },
    createTag() {
      const name = this.newTagName.trim(); if (!name) return
      this.tagSubmitting = true
      addTag(Object.assign({ tagName: name, status: '0' }, this.scopeParams())).then(() => { this.$modal.msgSuccess('标签已新增'); this.newTagName = ''; this.loadManagedTags(); this.getTagsList() }).finally(() => { this.tagSubmitting = false })
    },
    changeManagedTagStatus(row) { changeTagStatus(row.tagId, row.status).then(() => { this.$modal.msgSuccess(row.status === '0' ? '标签已启用' : '标签已停用'); this.getTagsList(); this.getList() }).catch(() => { row.status = row.status === '0' ? '1' : '0' }) },
    deleteManagedTag(row) { this.$modal.confirm('删除标签“' + row.tagName + '”后，文档关联将一并移除，是否继续？').then(() => delTag(row.tagId)).then(() => { this.$modal.msgSuccess('标签已删除'); this.loadManagedTags(); this.getTagsList(); this.getList() }).catch(() => {}) }
  }
}
</script>

<style scoped lang="scss">
.material-page { background:#f3f7fb; min-height:calc(100vh - 84px); }
.page-hero { display:flex; align-items:center; justify-content:space-between; gap:24px; padding:28px 32px; border-radius:16px; color:#fff; background:linear-gradient(125deg,#0b5fd7 0%,#1677e8 52%,#24a2d8 100%); box-shadow:0 16px 36px rgba(20,92,181,.18); }
.page-hero h1 { margin:5px 0 0; font-size:27px; letter-spacing:.5px; }.page-kicker { font-size:11px; font-weight:700; letter-spacing:1.8px; color:#bfe2ff; }
.page-hero .el-button { border:0; color:#0b5fd7; background:#fff; box-shadow:0 8px 20px rgba(4,42,91,.16); }.stat-grid { margin-top:16px; }
.stat-card { display:flex; align-items:center; gap:14px; min-height:94px; padding:18px 20px; border:1px solid #e5edf6; border-radius:13px; background:#fff; box-shadow:0 6px 18px rgba(22,63,105,.05); }
.stat-card > i { display:flex; align-items:center; justify-content:center; width:42px; height:42px; border-radius:11px; font-size:21px; color:#176fd1; background:#eaf3ff; }.stat-card strong,.stat-card span { display:block; }.stat-card strong { font-size:24px; line-height:1.1; color:#13283f; }.stat-card span { margin-top:6px; font-size:12px; color:#74869a; }
.stat-card.is-recognized > i { color:#16936e; background:#e8f8f2; }.stat-card.is-pending > i { color:#c58419; background:#fff6e5; }.stat-card.is-failed > i { color:#d94d55; background:#fff0f1; }
.content-card { margin-top:16px; padding:20px 22px; border:1px solid #e5edf6; border-radius:13px; background:#fff; box-shadow:0 6px 18px rgba(22,63,105,.045); }.section-heading { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; }.section-heading h2 { margin:0 0 3px; font-size:16px; color:#17324f; }.section-heading span { font-size:12px; color:#8a9aae; }
.tag-cloud { display:flex; flex-wrap:wrap; gap:8px; }.filter-tag { cursor:pointer; border-radius:14px; user-select:none; }.empty-hint { font-size:12px; color:#a4b0bd; }.search-form { display:flex; align-items:flex-start; flex-wrap:wrap; gap:0 4px; }.search-form .table-actions { margin-left:auto; }.document-table { margin-top:4px; }
.document-link { display:flex; align-items:center; max-width:100%; gap:9px; padding:0; border:0; color:#175fae; background:transparent; cursor:pointer; font:inherit; }.document-link i { flex:0 0 auto; width:30px; height:30px; line-height:30px; border-radius:8px; color:#2379d8; background:#edf5ff; }.document-link span { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.table-tag { margin:2px 5px 2px 0; }.more-button { margin-left:9px; }
.drawer-title { display:flex; flex-direction:column; gap:4px; padding-right:36px; color:#19324d; }.drawer-title small { overflow:hidden; color:#8797a9; font-weight:400; text-overflow:ellipsis; white-space:nowrap; }.drawer-body,.tag-manager-body { height:calc(100vh - 78px); padding:0 24px 28px; overflow:auto; }.detail-form { padding-top:8px; }
.metadata-row { display:flex; flex-wrap:wrap; gap:12px 22px; padding:14px 16px; border-radius:9px; color:#65788c; background:#f5f8fc; }.drawer-actions { display:flex; justify-content:flex-end; gap:8px; margin-top:20px; }.ocr-toolbar { display:flex; align-items:center; justify-content:space-between; gap:12px; margin:8px 0 16px; }.ocr-toolbar span { margin-left:10px; font-size:12px; color:#8a98a7; }.ocr-error { margin-bottom:14px; }.ocr-content { min-height:360px; padding:20px; border:1px solid #e2eaf3; border-radius:10px; color:#33485e; background:#f8fafc; white-space:pre-wrap; word-break:break-word; line-height:1.8; }
.record-editor { display:grid; gap:12px; margin:8px 0 18px; padding:16px; border:1px solid #dce8f5; border-radius:11px; background:#f8fbff; }.record-editor-head { display:flex; align-items:center; justify-content:space-between; gap:12px; color:#27445f; }.record-editor-actions { text-align:right; }.record-list { min-height:220px; }.record-item { margin-bottom:12px; padding:16px; border:1px solid #e2eaf3; border-radius:11px; background:#fff; }.record-item header { display:flex; align-items:center; gap:10px; }.record-avatar { flex:0 0 34px; width:34px; height:34px; line-height:34px; border-radius:50%; color:#fff; background:linear-gradient(135deg,#176fd1,#22a1da); text-align:center; }.record-item header strong,.record-item header span { display:block; }.record-item header span,.record-item footer { margin-top:3px; font-size:11px; color:#96a3b1; }.record-actions { margin-left:auto; }.inline-record-editor { display:grid; gap:10px; margin-top:13px; padding:13px; border:1px solid #9ecbf3; border-radius:9px; background:#f7fbff; }.inline-record-editor > div:last-child { text-align:right; }.record-content { margin-top:13px; padding:11px 13px; border-radius:8px; background:#f5f8fc; }.record-content.remark { border-left:3px solid #e5a93d; background:#fff8eb; }.record-content b { color:#50677e; font-size:12px; }.record-content p { margin:5px 0 0; color:#2e4154; line-height:1.65; white-space:pre-wrap; }.record-item footer { text-align:right; }.danger-text { color:#e0525d !important; }.new-tag-row { display:flex; gap:10px; margin:18px 0; }
@media (max-width:767px) {
  .material-page { padding:12px; }.page-hero { align-items:flex-start; padding:22px 20px; }.page-hero h1 { font-size:22px; }.page-hero p { display:none; }.page-hero .el-button { padding:10px; font-size:0; }.page-hero .el-button i { margin:0; font-size:16px; }.stat-grid .el-col { margin-bottom:10px; }.stat-card { min-height:78px; padding:12px; }.stat-card > i { width:34px; height:34px; font-size:17px; }.stat-card strong { font-size:19px; }.content-card { margin-top:8px; padding:15px; }.search-form .el-form-item { width:100%; margin-right:0; }.search-form .el-input,.search-form .el-select { width:100%; }.search-form .table-actions { display:none; }
}
</style>
