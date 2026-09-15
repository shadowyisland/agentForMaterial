<template>
  <div class="app-container material-page">
    <section class="page-hero">
      <div>
        <div class="page-kicker">
          {{ isExternal ? "EXTERNAL DOCUMENTS" : "INTERNAL MATERIALS" }}
        </div>
        <h1>{{ pageTitle }}</h1>
      </div>
      <el-button
        v-hasPermi="['system:document:add']"
        type="primary"
        icon="el-icon-upload2"
        @click="handleAdd"
      >
        上传文档
      </el-button>
    </section>

    <el-row :gutter="16" class="stat-grid">
      <el-col v-for="item in statCards" :key="item.key" :xs="12" :sm="6">
        <div class="stat-card" :class="'is-' + item.key">
          <i :class="item.icon" />
          <div>
            <strong> {{ stats[item.key] || 0 }} </strong
            ><span>{{ item.label }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <section class="content-card tag-panel">
      <div class="section-heading">
        <div><h2>分类标签</h2></div>
        <el-button
          v-if="isAdmin"
          type="text"
          icon="el-icon-setting"
          @click="openTagManager"
        >
          管理标签
        </el-button>
      </div>
      <div class="tag-cloud">
        <el-tag
          :effect="!queryParams.searchTag ? 'dark' : 'plain'"
          class="filter-tag"
          @click="selectTag()"
        >
          全部
        </el-tag>
        <el-tag
          v-for="tag in allTags"
          :key="tag"
          :effect="queryParams.searchTag === tag ? 'dark' : 'plain'"
          class="filter-tag"
          @click="selectTag(tag)"
        >
          {{ tag }}
        </el-tag>
        <span v-if="!allTags.length" class="empty-hint">
          当前分类暂无标签
        </span>
      </div>
    </section>

    <section class="content-card document-card">
      <el-form
        ref="queryForm"
        :model="queryParams"
        :inline="true"
        size="small"
        class="search-form"
      >
        <el-form-item prop="documentName">
          <el-input
            v-model="queryParams.documentName"
            prefix-icon="el-icon-search"
            placeholder="搜索文档名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="searchTag">
          <el-select
            v-model="queryParams.searchTag"
            filterable
            clearable
            placeholder="全部标签"
            @change="handleQuery"
          >
            <el-option
              v-for="tag in allTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">
            查询
          </el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">
            重置
          </el-button>
        </el-form-item>
        <div class="table-actions">
          <el-button
            v-hasPermi="['system:document:remove']"
            type="danger"
            plain
            size="small"
            icon="el-icon-delete"
            :disabled="!ids.length"
            @click="handleDelete()"
          >
            批量删除
          </el-button>
        </div>
      </el-form>

      <el-table
        v-loading="loading"
        :data="documentList"
        row-key="documentId"
        class="document-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" fixed="left" />
        <el-table-column
          label="文档名称"
          prop="documentName"
          min-width="200"
          fixed="left"
          show-overflow-tooltip
        >
          <template slot-scope="scope">
            <button
              class="document-link"
              type="button"
              @click="handleDetail(scope.row)"
            >
              <i class="el-icon-document" />
              <span> {{ scope.row.documentName }} </span>
            </button>
          </template>
        </el-table-column>
        <el-table-column
          v-if="!isExternal && !isCompact"
          label="产品名称"
          prop="productName"
          width="120"
          show-overflow-tooltip
        />
        <el-table-column
          v-if="isExternal && !isCompact"
          label="来源"
          prop="sourceName"
          width="130"
          show-overflow-tooltip
        />
        <el-table-column
          v-if="!isCompact"
          label="资料类型"
          width="96"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag v-if="scope.row.documentKind" size="mini" effect="plain">
              {{ scope.row.documentKind }}
            </el-tag>
            <span v-else class="empty-hint">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isCompact" label="文件标签" width="150">
          <template slot-scope="scope">
            <el-tag
              v-for="tag in (scope.row.tags || []).slice(0, 3)"
              :key="tag"
              size="mini"
              class="table-tag"
            >
              {{ tag }}
            </el-tag>
            <span
              v-if="!scope.row.tags || !scope.row.tags.length"
              class="empty-hint"
            >
              未添加
            </span>
          </template>
        </el-table-column>
        <el-table-column label="OCR 状态" width="104" align="center">
          <template slot-scope="scope">
            <el-tag :type="ocrMeta(scope.row).type" size="small" effect="plain">
              <i :class="ocrMeta(scope.row).icon" />
              {{ ocrMeta(scope.row).label }}
            </el-tag></template
          >
        </el-table-column>
        <el-table-column
          v-if="!isCompact"
          label="上传人"
          prop="createBy"
          width="90"
          show-overflow-tooltip
        />
        <el-table-column
          v-if="!isCompact"
          label="上传时间"
          prop="createTime"
          width="160"
        >
          <template slot-scope="scope">
            {{ parseTime(scope.row.createTime) }}
          </template></el-table-column
        >
        <el-table-column
          label="操作"
          width="112"
          align="center"
          fixed="right"
          class-name="operation-column"
        >
          <template slot-scope="scope">
            <el-tooltip content="详情" placement="top">
              <el-button
                type="text"
                icon="el-icon-view"
                class="operation-button"
                @click="handleDetail(scope.row)"
              />
            </el-tooltip>
            <el-tooltip
              v-if="canViewExtract && scope.row.documentKind"
              content="解析结果"
              placement="top"
            >
              <el-button
                type="text"
                icon="el-icon-document-checked"
                class="operation-button"
                @click="handleExtract(scope.row)"
              />
            </el-tooltip>
            <el-tooltip v-if="canRemoveDocument" content="删除文件" placement="top">
              <el-button
                type="text"
                icon="el-icon-delete"
                class="operation-button danger-text"
                @click="handleDelete(scope.row)"
              />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </section>

    <el-dialog
      :title="uploadTitle"
      :visible.sync="uploadOpen"
      width="680px"
      custom-class="material-dialog"
      append-to-body
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="106px">
        <el-form-item label="文档名称" prop="documentName">
          <el-input
            v-model="form.documentName"
            placeholder="请输入文档显示名称"
          />
        </el-form-item>
        <el-form-item label="文件上传" prop="filePath">
          <el-upload
            ref="upload"
            drag:limit="1"
            accept=".pdf,.png,.jpg,.jpeg"
            :action="uploadUrl"
            :headers="headers"
            :file-list="fileList"
            :before-upload="handleBeforeUpload"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
          >
            <i class="el-icon-upload" />
            <div class="el-upload__text">
              拖放文件到这里， 或 <em> 点击选择 </em>
            </div>
            <div slot="tip" class="el-upload__tip">
              支持 PDF、 JPG、 PNG， 单个文件不超过 50 MB； {{ uploadProcessTip }}
            </div>
          </el-upload>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :sm="8" :xs="24">
            <el-form-item label="产品名称">
              <el-input v-model="form.productName" /> </el-form-item
          ></el-col>
          <el-col :sm="8" :xs="24">
            <el-form-item label="产品型号">
              <el-input v-model="form.productModel" /> </el-form-item
          ></el-col>
          <el-col :sm="8" :xs="24">
            <el-form-item label="内部编号">
              <el-input v-model="form.internalCode" /> </el-form-item
          ></el-col>
        </el-row>
        <el-form-item v-if="!isExternal" label="文档类型">
          <el-radio-group v-model="form.documentKind">
            <el-radio label="TDS"> TDS </el-radio>
            <el-radio label="MSDS"> MSDS </el-radio>
          </el-radio-group>
          <div class="document-kind-tip">
            可选；不选择时仅进行 MinerU OCR，不进行 AI 解析。
          </div>
        </el-form-item>
        <template v-if="isExternal">
          <el-form-item label="来源单位/网站">
            <el-input v-model="form.sourceName" placeholder="可选" />
          </el-form-item>
          <el-form-item label="原始链接" prop="sourceUrl">
            <el-input
              v-model="form.sourceUrl"
              placeholder="https://...（可选）"
            />
          </el-form-item>
          <el-form-item label="发布日期">
            <el-date-picker
              v-model="form.publishDate"
              type="date"
              value-format="yyyy-MM-dd"
              placeholder="可选"
            />
          </el-form-item>
        </template>
        <el-form-item label="文档标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            style="width: 100%"
            placeholder="选择或输入新标签后回车"
          >
            <el-option
              v-for="tag in allTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="uploadOpen = false"> 取消 </el-button
        ><el-button type="primary" :loading="submitting" @click="submitForm"
          >确认上传</el-button
        >
      </div>
    </el-dialog>

    <el-dialog
      :visible="processingUpload"
      width="460px"
      append-to-body
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      custom-class="document-processing-dialog"
    >
      <div class="document-processing">
        <i class="el-icon-loading document-processing-icon" />
        <h3>正在解析文档</h3>
        <p>{{ processingDescription }}</p>
        <el-progress
          :percentage="processingProgress"
          :stroke-width="9"
          :show-text="false"
          color="#2585db"
        />
        <span class="processing-elapsed">已等待 {{ processingElapsedText }}</span>
        <strong>请耐心等待，若五分钟以上无反应，请刷新</strong>
      </div>
    </el-dialog>

    <el-dialog
      :visible.sync="detailOpen"
      :width="detailDialogWidth"
      top="5vh"
      custom-class="document-detail-dialog"
      append-to-body
      @closed="resetDetail"
    >
      <div slot="title" class="drawer-title">
        <span> 文档详情 </span><small>{{ detailForm.documentName }}</small>
      </div>
      <div v-loading="detailLoading" class="drawer-body">
        <el-tabs v-model="detailTab" stretch>
          <el-tab-pane label="基本信息" name="basic">
            <el-form
              :model="detailForm"
              label-position="top"
              class="detail-form"
            >
              <el-form-item label="文档名称">
                <el-input
                  v-model="detailForm.documentName"
                  :disabled="!canEditDocument"
                />
              </el-form-item>
              <el-row :gutter="14">
                <el-col :sm="8" :xs="24">
                  <el-form-item label="产品名称">
                    <el-input
                      v-model="detailForm.productName"
                      :disabled="!canEditDocument"
                    /> </el-form-item
                ></el-col>
                <el-col :sm="8" :xs="24">
                  <el-form-item label="产品型号">
                    <el-input
                      v-model="detailForm.productModel"
                      :disabled="!canEditDocument"
                    /> </el-form-item
                ></el-col>
                <el-col :sm="8" :xs="24">
                  <el-form-item label="内部编号">
                    <el-input
                      v-model="detailForm.internalCode"
                      :disabled="!canEditDocument"
                    /> </el-form-item
                ></el-col>
              </el-row>
              <el-form-item v-if="detailForm.documentType !== 'EXTERNAL'" label="文档类型">
                <el-input
                  :value="detailForm.documentKind || '未选择（仅 OCR）'"
                  disabled
                />
              </el-form-item>
              <template v-if="detailForm.documentType === 'EXTERNAL'">
                <el-form-item label="来源单位/网站">
                  <el-input
                    v-model="detailForm.sourceName"
                    :disabled="!canEditDocument"
                  />
                </el-form-item>
                <el-form-item label="原始链接">
                  <el-input
                    v-model="detailForm.sourceUrl"
                    :disabled="!canEditDocument"
                  />
                </el-form-item>
                <el-form-item label="发布日期">
                  <el-date-picker
                    v-model="detailForm.publishDate"
                    type="date"
                    value-format="yyyy-MM-dd"
                    :disabled="!canEditDocument"
                  />
                </el-form-item>
              </template>
              <el-form-item label="标签">
                <el-select
                  v-model="detailForm.tags"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  style="width: 100%"
                  :disabled="!canEditDocument"
                >
                  <el-option
                    v-for="tag in allTags"
                    :key="tag"
                    :label="tag"
                    :value="tag"
                  /> </el-select
              ></el-form-item>
              <div class="metadata-row">
                <span>
                  <i class="el-icon-user" />
                  {{ detailForm.createBy || "-" }} </span
                ><span
                  ><i class="el-icon-time" />
                  {{ parseTime(detailForm.createTime) }} </span
                ><span
                  ><i class="el-icon-paperclip" />
                  {{ detailForm.fileOriginName || "-" }}
                </span>
              </div>
              <div class="drawer-actions">
                <el-button
                  v-if="canEditDocument"
                  type="primary"
                  :loading="submitting"
                  @click="saveDetail"
                >
                  保存修改
                </el-button>
              </div>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="OCR 正文" name="ocr">
            <div class="ocr-toolbar">
              <div>
                <el-tag :type="ocrMeta(detailForm).type">
                  {{ ocrMeta(detailForm).label }} </el-tag
                ><span v-if="detailForm.ocrTime">{{
                  parseTime(detailForm.ocrTime)
                }}</span>
              </div>
            </div>
            <el-alert
              v-if="detailForm.ocrError"
              :title="detailForm.ocrError"
              type="error"
              show-icon:closable="false"
              class="ocr-error"
            />
            <div v-if="detailForm.ocrContent" class="ocr-content">
              {{ detailForm.ocrContent }}
            </div>
            <el-empty v-else description="暂无 OCR 正文" />
          </el-tab-pane>
          <el-tab-pane name="records">
            <span slot="label">
              批注与记录
              <el-badge :value="records.length" :hidden="!records.length" />
            </span>
            <div class="record-editor">
              <div class="record-editor-head">
                <strong> 添加内容 </strong>
                <el-radio-group v-model="recordKind" size="mini">
                  <el-radio-button label="remark"> 批注 </el-radio-button>
                  <el-radio-button label="usage"> 使用记录 </el-radio-button>
                </el-radio-group>
              </div>
              <el-input
                v-if="recordKind === 'remark'"
                v-model="recordForm.remark"
                type="textarea"
                :rows="3"
                maxlength="4000"
                show-word-limit
                placeholder="写下对该文档的批注…"
              />
              <el-input
                v-else
                v-model="recordForm.usageContent"
                type="textarea"
                :rows="3"
                maxlength="4000"
                show-word-limit
                placeholder="记录该文档的使用情况…"
              />
              <div class="record-editor-actions">
                <el-button
                  type="primary"
                  :loading="recordSubmitting"
                  @click="submitRecord"
                >
                  {{ recordKind === "remark" ? "发布批注" : "添加记录" }}
                </el-button>
              </div>
            </div>
            <div v-loading="recordLoading" class="record-list">
              <article
                v-for="record in records"
                :key="record.recordId"
                class="record-item"
              >
                <header>
                  <div class="record-avatar">
                    {{ (record.createBy || "?").slice(0, 1).toUpperCase() }}
                  </div>
                  <div>
                    <strong>{{ record.createBy || "未知用户" }}</strong>
                    <span> {{ parseTime(record.createTime) }} </span>
                  </div>
                  <div
                    v-if="editingRecordId !== record.recordId"
                    class="record-actions"
                  >
                    <el-button
                      v-if="canEditRecord(record)"
                      type="text"
                      icon="el-icon-edit"
                      @click="editRecord(record)"
                    >
                      编辑 </el-button
                    ><el-button
                      v-if="isAdmin"
                      type="text"
                      icon="el-icon-delete"
                      class="danger-text"
                      @click="deleteRecord(record)"
                      >删除</el-button
                    >
                  </div>
                </header>
                <div
                  v-if="editingRecordId === record.recordId"
                  class="inline-record-editor"
                >
                  <el-radio-group v-model="inlineRecordKind" size="mini">
                    <el-radio-button label="remark"> 批注 </el-radio-button
                    ><el-radio-button label="usage">使用记录</el-radio-button>
                  </el-radio-group>
                  <el-input
                    v-if="inlineRecordKind === 'remark'"
                    v-model="inlineRecordForm.remark"
                    type="textarea"
                    :rows="3"
                    maxlength="4000"
                    show-word-limit
                  />
                  <el-input
                    v-else
                    v-model="inlineRecordForm.usageContent"
                    type="textarea"
                    :rows="3"
                    maxlength="4000"
                    show-word-limit
                  />
                  <div>
                    <el-button size="small" @click="cancelRecordEdit">
                      取消 </el-button
                    ><el-button
                      size="small"
                      type="primary"
                      :loading="inlineRecordSubmitting"
                      @click="saveRecordEdit"
                      >保存修改</el-button
                    >
                  </div>
                </div>
                <template v-else>
                  <div v-if="record.remark" class="record-content remark">
                    <b> 批注 </b>
                    <p>{{ record.remark }}</p>
                  </div>
                  <div v-if="record.usageContent" class="record-content">
                    <b>使用记录</b>
                    <p>{{ record.usageContent }}</p>
                  </div>
                </template>
                <footer
                  v-if="
                    record.updateTime && editingRecordId !== record.recordId
                  "
                >
                  更新于 {{ parseTime(record.updateTime) }}
                </footer>
              </article>
              <el-empty
                v-if="!recordLoading && !records.length"
                description="还没有批注或使用记录"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <el-dialog
      :visible.sync="extractOpen"
      width="92%"
      top="4vh"
      custom-class="extract-dialog"
      append-to-body
      @closed="resetExtract"
    >
      <div slot="title" class="extract-dialog-title">
        <div>
          <strong> 解析结果 </strong
          ><small
            >{{ extractDocumentInfo.documentName || "" }}
            {{ extractDocumentInfo.documentKind || "" }}</small
          >
        </div>
        <div class="extract-dialog-actions">
          <el-button
            :disabled="extractLoading || !extractDocumentInfo.documentId"
            icon="el-icon-view"
            @click="openFilePreview('template')"
          >模版预览</el-button>
          <el-button
            :disabled="extractLoading || !extractDocumentInfo.filePath"
            icon="el-icon-document"
            @click="openFilePreview('upload')"
          >上传文件预览</el-button>
          <el-button
            v-if="canSaveExtract"
            :loading="extractSubmitting"
            @click="saveExtract"
          >
            保存 </el-button
          ><el-button
            v-if="canDownloadExtract"
            type="primary"
            :loading="extractSubmitting"
            icon="el-icon-download"
            @click="saveAndDownload"
            >保存并下载</el-button
          >
        </div>
      </div>
      <div v-loading="extractLoading" class="extract-dialog-body">
        <document-extract-editor
          v-if="extractForm"
          :value="extractForm"
          :material-category="extractDocumentInfo.materialCategory"
          :document-kind="extractDocumentInfo.documentKind"
          :document-id="extractDocumentInfo.documentId"
          :current-document-name="extractDocumentInfo.fileOriginName || extractDocumentInfo.documentName"
        />
        <el-empty v-else description="暂无可编辑的解析结果" />
      </div>
    </el-dialog>

    <document-file-preview
      v-if="filePreviewOpen"
      :document-id="extractDocumentInfo.documentId"
      :mode="filePreviewMode"
      @close="filePreviewOpen = false"
    />

    <el-drawer
      title="标签管理"
      :visible.sync="tagManagerOpen"
      :size="drawerSize"
      append-to-body
    >
      <div class="tag-manager-body">
        <el-alert
          title="停用标签后，用户无法在当前分类中选择该标签，已有文档关联不会丢失。"
          type="info"
          :closable="false"
          show-icon
        />
        <div class="new-tag-row">
          <el-input
            v-model="newTagName"
            maxlength="64"
            placeholder="输入新标签名称"
            @keyup.enter.native="createTag"
          />
          <el-button
            type="primary"
            icon="el-icon-plus"
            :loading="tagSubmitting"
            @click="createTag"
          >
            新增
          </el-button>
        </div>
        <el-table v-loading="tagManagerLoading" :data="managedTags">
          <el-table-column label="标签" prop="tagName" min-width="140" />
          <el-table-column
            label="文档数"
            prop="documentCount"
            width="90"
            align="center"
          />
          <el-table-column label="创建人" prop="createBy" width="110" />
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="scope">
              <el-switch
                v-model="scope.row.status"
                active-value="0"
                inactive-value="1"
                @change="changeManagedTagStatus(scope.row)"
              /> </template
          ></el-table-column>
          <el-table-column label="操作" width="80" align="right">
            <template slot-scope="scope">
              <el-button
                type="text"
                class="danger-text"
                @click="deleteManagedTag(scope.row)"
              >
                删除
              </el-button></template
            >
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  listDocument,
  getDocument,
  addDocument,
  updateDocument,
  delDocument,
  getLatestExtract,
  saveExtractFinal,
  downloadExtract,
  getTopTags,
  getDocumentStats,
  listDocumentRecords,
  addDocumentRecord,
  updateDocumentRecord,
  delDocumentRecord,
} from "@/api/system/document";
import { listTag, addTag, delTag, changeTagStatus } from "@/api/system/tag";
import { getToken } from "@/utils/auth";
import { blobValidate } from "@/utils/ruoyi";
import { saveAs } from "file-saver";
import DocumentExtractEditor from "@/components/DocumentExtractEditor";
import DocumentFilePreview from "@/components/DocumentFilePreview";
import {
  createExtractTemplate,
  mergeExtractTemplate,
} from "@/components/DocumentExtractEditor/templateDefaults";

const CATEGORY_META = {
  RAW_MATERIAL: "原材料",
  ACRYLIC: "丙烯酸原材料",
  EPOXY: "环氧原材料",
  OTHER_RESIN: "其它树脂原材料",
  FILLER: "填料类原材料",
  SILICONE: "有机硅原材料",
  ADDITIVE: "助剂类原材料",
};

export default {
  name: "Document",
  components: {
    DocumentExtractEditor,
    DocumentFilePreview,
  },
  data() {
    return {
      windowWidth: window.innerWidth,
      loading: false,
      detailLoading: false,
      submitting: false,
      processingUpload: false,
      processingElapsed: 0,
      processingTimer: null,
      extractLoading: false,
      extractSubmitting: false,
      recordLoading: false,
      recordSubmitting: false,
      inlineRecordSubmitting: false,
      tagManagerLoading: false,
      tagSubmitting: false,
      ids: [],
      total: 0,
      documentList: [],
      allTags: [],
      stats: {
        total: 0,
        recognized: 0,
        pending: 0,
        failed: 0,
      },
      statCards: [
        {
          key: "total",
          label: "文档总数",
          icon: "el-icon-folder-opened",
        },
        {
          key: "recognized",
          label: "OCR 已完成",
          icon: "el-icon-circle-check",
        },
        {
          key: "pending",
          label: "待识别",
          icon: "el-icon-time",
        },
        {
          key: "failed",
          label: "识别失败",
          icon: "el-icon-warning-outline",
        },
      ],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        documentName: undefined,
        searchTag: undefined,
      },
      uploadOpen: false,
      detailOpen: false,
      detailTab: "basic",
      detailForm: {},
      form: {},
      fileList: [],
      extractOpen: false,
      filePreviewOpen: false,
      filePreviewMode: "template",
      extractRecord: null,
      extractForm: null,
      extractDocumentInfo: {},
      rules: {
        documentName: [
          {
            required: true,
            message: "文档名称不能为空",
            trigger: "blur",
          },
        ],
        filePath: [
          {
            required: true,
            message: "请先上传文件",
            trigger: "change",
          },
        ],
        sourceUrl: [
          {
            type: "url",
            message: "请输入完整的 http(s) 链接",
            trigger: "blur",
          },
        ],
      },
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      headers: {
        Authorization: "Bearer " + getToken(),
      },
      records: [],
      recordsLoaded: false,
      recordForm: {
        usageContent: "",
        remark: "",
      },
      recordKind: "remark",
      editingRecordId: null,
      inlineRecordForm: {
        usageContent: "",
        remark: "",
      },
      inlineRecordKind: "remark",
      tagManagerOpen: false,
      managedTags: [],
      newTagName: "",
    };
  },
  computed: {
    documentType() {
      return (
        (this.$route.query && this.$route.query.documentType) || "INTERNAL"
      );
    },
    materialCategory() {
      return this.documentType === "INTERNAL"
        ? (this.$route.query && this.$route.query.materialCategory) ||
            "RAW_MATERIAL"
        : undefined;
    },
    isExternal() {
      return this.documentType === "EXTERNAL";
    },
    pageTitle() {
      return this.isExternal
        ? "外部文档"
        : CATEGORY_META[this.materialCategory] || CATEGORY_META.RAW_MATERIAL;
    },
    uploadTitle() {
      return "上传至「" + this.pageTitle + "」";
    },
    uploadProcessTip() {
      return this.isExternal
        ? "确认上传后自动进行 OCR，不进行 AI 解析。"
        : "可选择 TDS 或 MSDS 进入 AI 解析；不选择时仅进行 MinerU OCR。";
    },
    isOcrOnlyUpload() {
      return this.isExternal || !this.form.documentKind;
    },
    processingDescription() {
      return this.isOcrOnlyUpload
        ? "正在调用 MinerU 进行 OCR"
        : "正在调用 MinerU 和 AI 处理文件";
    },
    processingProgress() {
      return Math.min(95, 6 + Math.floor((this.processingElapsed / 300) * 89));
    },
    processingElapsedText() {
      const minutes = Math.floor(this.processingElapsed / 60);
      const seconds = String(this.processingElapsed % 60).padStart(2, "0");
      return minutes ? minutes + "分" + seconds + "秒" : seconds + "秒";
    },
    isCompact() {
      return this.windowWidth < 1200;
    },
    drawerSize() {
      return this.windowWidth < 768 ? "100%" : "720px";
    },
    detailDialogWidth() {
      return this.windowWidth < 768 ? "96%" : "88%";
    },
    isAdmin() {
      return (this.$store.getters.roles || []).includes("admin");
    },
    canEditDocument() {
      return this.hasPermission("system:document:edit");
    },
    canViewExtract() {
      return (
        !this.isExternal &&
        this.hasPermission("system:document:extract:query")
      );
    },
    canSaveExtract() {
      return this.hasPermission("system:document:extract:edit");
    },
    canDownloadExtract() {
      return this.hasPermission("system:document:extract:download");
    },
    canRemoveDocument() {
      return this.hasPermission("system:document:remove");
    },
  },
  watch: {
    "$route.fullPath"() {
      this.initializePage();
    },
    detailTab(value) {
      if (
        value === "records" &&
        this.detailForm.documentId &&
        !this.recordsLoaded
      )
        this.loadRecords();
    },
  },
  created() {
    this.resetFormData();
    this.initializePage();
    window.addEventListener("resize", this.handleResize);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
    this.stopDocumentProcessing();
  },
  methods: {
    scopeParams() {
      return {
        documentType: this.documentType,
        materialCategory: this.materialCategory,
      };
    },
    initializePage() {
      this.queryParams.pageNum = 1;
      this.queryParams.documentName = undefined;
      this.queryParams.searchTag = undefined;
      this.getList();
      this.getTagsList();
      this.getStats();
      const documentId = this.$route.query && this.$route.query.documentId;
      if (documentId)
        this.handleDetail({
          documentId,
        });
    },
    handleResize() {
      this.windowWidth = window.innerWidth;
    },
    hasPermission(code) {
      const permissions = this.$store.getters.permissions || [];
      return permissions.includes("*:*:*") || permissions.includes(code);
    },
    getList() {
      this.loading = true;
      listDocument(Object.assign({}, this.queryParams, this.scopeParams()))
        .then((res) => {
          this.documentList = res.rows || [];
          this.total = res.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    getTagsList() {
      getTopTags(this.scopeParams()).then((res) => {
        this.allTags = res.data || [];
      });
    },
    getStats() {
      getDocumentStats(this.scopeParams()).then((res) => {
        this.stats = Object.assign({}, this.stats, res.data || {});
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.queryParams.documentName = undefined;
      this.queryParams.searchTag = undefined;
      this.handleQuery();
    },
    selectTag(tag) {
      this.queryParams.searchTag = tag;
      this.handleQuery();
    },
    handleSelectionChange(rows) {
      this.ids = rows.map((item) => item.documentId);
    },
    resetFormData() {
      this.form = {
        documentName: "",
        filePath: "",
        fileOriginName: "",
        fileSuffix: "",
        fileSize: null,
        mimeType: "",
        productName: "",
        productModel: "",
        internalCode: "",
        documentKind: "",
        sourceName: "",
        sourceUrl: "",
        publishDate: null,
        tags: [],
      };
      this.fileList = [];
      if (this.$refs.form) this.$refs.form.clearValidate();
    },
    handleAdd() {
      this.resetFormData();
      this.uploadOpen = true;
    },
    handleBeforeUpload(file) {
      const valid = file.size / 1024 / 1024 <= 50;
      if (!valid) this.$modal.msgError("文件大小不能超过 50MB");
      return valid;
    },
    handleUploadSuccess(res, file) {
      if (res.code !== 200) {
        this.$modal.msgError(res.msg || "文件上传失败");
        this.$refs.upload.clearFiles();
        return;
      }
      const originalName = res.originalFilename || file.name;
      this.form.filePath = res.fileName;
      this.form.fileOriginName = originalName;
      this.form.fileSuffix = originalName.includes(".")
        ? originalName.split(".").pop()
        : "";
      this.form.fileSize = file.size;
      this.form.mimeType = file.raw ? file.raw.type : "";
      if (!this.form.documentName)
        this.form.documentName = originalName.replace(/\.[^.]+$/, "");
      this.$nextTick(
        () => this.$refs.form && this.$refs.form.validateField("filePath")
      );
    },
    handleRemove() {
      this.form.filePath = "";
      this.form.fileOriginName = "";
      this.form.fileSuffix = "";
      this.form.fileSize = null;
      this.form.mimeType = "";
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        const ocrOnly = this.isOcrOnlyUpload;
        this.submitting = true;
        this.startDocumentProcessing();
        addDocument(Object.assign({}, this.form, this.scopeParams()))
          .then((res) => {
            const documentId = res.data;
            this.$modal.msgSuccess(
              ocrOnly
                ? "文档上传成功，OCR 已处理"
                : "文档上传成功，已进入解析结果"
            );
            this.uploadOpen = false;
            this.getList();
            this.getTagsList();
            this.getStats();
            if (ocrOnly) {
              this.stopDocumentProcessing();
              this.handleDetail({ documentId }, "ocr");
            } else {
              this.handleExtract({ documentId });
            }
          })
          .finally(() => {
            this.submitting = false;
            this.stopDocumentProcessing();
          });
      });
    },
    startDocumentProcessing() {
      this.processingElapsed = 0;
      this.processingUpload = true;
      this.processingTimer = window.setInterval(() => {
        this.processingElapsed += 1;
      }, 1000);
    },
    stopDocumentProcessing() {
      if (this.processingTimer) window.clearInterval(this.processingTimer);
      this.processingTimer = null;
      this.processingUpload = false;
    },
    handleDetail(row, activeTab = "basic") {
      this.detailOpen = true;
      this.detailLoading = true;
      this.detailTab = activeTab;
      this.records = [];
      this.recordsLoaded = false;
      this.cancelRecordEdit();
      getDocument(row.documentId)
        .then((res) => {
          this.detailForm = res.data || {};
          if (!this.detailForm.tags) this.$set(this.detailForm, "tags", []);
          this.loadRecords();
        })
        .finally(() => {
          this.detailLoading = false;
        });
    },
    resetDetail() {
      this.detailForm = {};
      this.records = [];
      this.recordsLoaded = false;
      this.resetRecordForm();
      this.cancelRecordEdit();
    },
    saveDetail() {
      this.submitting = true;
      updateDocument(this.detailForm)
        .then(() => {
          this.$modal.msgSuccess("文档信息已更新");
          this.getList();
          this.getTagsList();
        })
        .finally(() => {
          this.submitting = false;
        });
    },
    ocrMeta(row) {
      if (Number(row.isRecognized) === 1)
        return {
          label: "已识别",
          type: "success",
          icon: "el-icon-circle-check",
        };
      if (Number(row.isRecognized) === 2)
        return {
          label: "识别失败",
          type: "danger",
          icon: "el-icon-warning-outline",
        };
      return {
        label: "待识别",
        type: "info",
        icon: "el-icon-time",
      };
    },
    handleDelete(row) {
      const ids = row && row.documentId ? [row.documentId] : this.ids;
      if (!ids.length) return;
      this.$modal
        .confirm("删除后文档记录也会一并清理，是否继续？")
        .then(() => delDocument(ids.join(",")))
        .then(() => {
          this.$modal.msgSuccess("删除成功");
          this.ids = [];
          this.getList();
          this.getTagsList();
          this.getStats();
        })
        .catch(() => {});
    },
    handleExtract(row) {
      this.extractOpen = true;
      this.extractLoading = true;
      this.extractForm = null;
      this.extractRecord = null;
      this.extractDocumentInfo = {};
      getDocument(row.documentId)
        .then((documentRes) => {
          this.extractDocumentInfo = documentRes.data || {};
          return getLatestExtract(row.documentId);
        })
        .then((res) => {
          this.extractRecord = res.data;
          if (!this.extractRecord) {
            this.$modal.msgWarning(
              "暂无解析结果，请确认 OCR、提示词和 AI 抽取是否完成"
            );
            return;
          }
          const json =
            this.extractRecord.finalJson || this.extractRecord.aiJson;
          const source = JSON.parse(json);
          const form = mergeExtractTemplate(
            createExtractTemplate(
              this.extractDocumentInfo.materialCategory,
              this.extractDocumentInfo.documentKind
            ),
            source
          );
          this.extractForm = form;
        })
        .catch(() => {
          this.extractOpen = false;
        })
        .finally(() => {
          this.extractLoading = false;
        });
    },
    resetExtract() {
      this.extractRecord = null;
      this.extractForm = null;
      this.extractDocumentInfo = {};
    },
    extractJsonText() {
      return JSON.stringify(this.extractForm || {});
    },
    saveExtract() {
      if (!this.extractRecord || !this.extractForm) return;
      this.extractSubmitting = true;
      saveExtractFinal(
        this.extractDocumentInfo.documentId,
        this.extractRecord.extractId,
        this.extractJsonText()
      )
        .then(() => {
          this.$modal.msgSuccess("解析结果已保存");
        })
        .finally(() => {
          this.extractSubmitting = false;
        });
    },
    openFilePreview(mode) {
      this.filePreviewMode = mode;
      this.filePreviewOpen = true;
    },
    async saveAndDownload() {
      if (!this.extractRecord || !this.extractForm) return;
      this.extractSubmitting = true;
      const fileName =
        (this.extractDocumentInfo.documentName || "材料文档") +
        "-" +
        (this.extractDocumentInfo.documentKind || "TDS") +
        ".docx";
      try {
        const blob = await downloadExtract(
          this.extractDocumentInfo.documentId,
          this.extractRecord.extractId,
          this.extractJsonText()
        );
        if (!blobValidate(blob)) {
          const text = await blob.text();
          let message = "生成 Word 文件失败";
          try {
            message = JSON.parse(text).msg || message;
          } catch (e) {}
          throw new Error(message);
        }
        saveAs(blob, fileName);
        this.$modal.msgSuccess("已保存并开始下载");
        this.extractOpen = false;
        this.getList();
      } catch (error) {
        this.$modal.msgError(error.message || "生成 Word 文件失败");
      } finally {
        this.extractSubmitting = false;
      }
    },
    loadRecords() {
      this.recordLoading = true;
      listDocumentRecords(this.detailForm.documentId)
        .then((res) => {
          this.records = res.data || [];
          this.recordsLoaded = true;
        })
        .finally(() => {
          this.recordLoading = false;
        });
    },
    resetRecordForm() {
      this.recordForm = {
        usageContent: "",
        remark: "",
      };
      this.recordKind = "remark";
    },
    submitRecord() {
      if (
        !this.recordForm.usageContent.trim() &&
        !this.recordForm.remark.trim()
      ) {
        this.$modal.msgWarning("请填写批注或使用记录");
        return;
      }
      this.recordSubmitting = true;
      addDocumentRecord(this.detailForm.documentId, this.recordForm)
        .then(() => {
          this.$modal.msgSuccess(
            this.recordKind === "remark" ? "批注已发布" : "记录已添加"
          );
          this.resetRecordForm();
          this.loadRecords();
        })
        .finally(() => {
          this.recordSubmitting = false;
        });
    },
    canEditRecord(record) {
      return (
        this.isAdmin ||
        String(record.createUserId) === String(this.$store.getters.id)
      );
    },
    editRecord(record) {
      this.editingRecordId = record.recordId;
      this.inlineRecordKind = record.remark ? "remark" : "usage";
      this.inlineRecordForm = {
        usageContent: record.usageContent || "",
        remark: record.remark || "",
      };
    },
    cancelRecordEdit() {
      this.editingRecordId = null;
      this.inlineRecordKind = "remark";
      this.inlineRecordForm = {
        usageContent: "",
        remark: "",
      };
    },
    saveRecordEdit() {
      if (
        !this.inlineRecordForm.usageContent.trim() &&
        !this.inlineRecordForm.remark.trim()
      ) {
        this.$modal.msgWarning("请填写批注或使用记录");
        return;
      }
      this.inlineRecordSubmitting = true;
      updateDocumentRecord(this.editingRecordId, this.inlineRecordForm)
        .then(() => {
          this.$modal.msgSuccess("修改已保存");
          this.cancelRecordEdit();
          this.loadRecords();
        })
        .finally(() => {
          this.inlineRecordSubmitting = false;
        });
    },
    deleteRecord(record) {
      this.$modal
        .confirm("确认删除这条批注或使用记录？")
        .then(() => delDocumentRecord(record.recordId))
        .then(() => {
          this.$modal.msgSuccess("内容已删除");
          this.loadRecords();
        })
        .catch(() => {});
    },
    openTagManager() {
      this.tagManagerOpen = true;
      this.loadManagedTags();
    },
    loadManagedTags() {
      this.tagManagerLoading = true;
      listTag(
        Object.assign(
          {
            pageNum: 1,
            pageSize: 1000,
          },
          this.scopeParams()
        )
      )
        .then((res) => {
          this.managedTags = res.rows || [];
        })
        .finally(() => {
          this.tagManagerLoading = false;
        });
    },
    createTag() {
      const name = this.newTagName.trim();
      if (!name) return;
      this.tagSubmitting = true;
      addTag(
        Object.assign(
          {
            tagName: name,
            status: "0",
          },
          this.scopeParams()
        )
      )
        .then(() => {
          this.$modal.msgSuccess("标签已新增");
          this.newTagName = "";
          this.loadManagedTags();
          this.getTagsList();
        })
        .finally(() => {
          this.tagSubmitting = false;
        });
    },
    changeManagedTagStatus(row) {
      changeTagStatus(row.tagId, row.status)
        .then(() => {
          this.$modal.msgSuccess(
            row.status === "0" ? "标签已启用" : "标签已停用"
          );
          this.getTagsList();
          this.getList();
        })
        .catch(() => {
          row.status = row.status === "0" ? "1" : "0";
        });
    },
    deleteManagedTag(row) {
      this.$modal
        .confirm(
          "删除标签“" + row.tagName + "”后，文档关联将一并移除，是否继续？"
        )
        .then(() => delTag(row.tagId))
        .then(() => {
          this.$modal.msgSuccess("标签已删除");
          this.loadManagedTags();
          this.getTagsList();
          this.getList();
        })
        .catch(() => {});
    },
  },
};
</script>

<style scoped lang="scss">
.document-processing {
  padding: 18px 20px 12px;
  text-align: center;
}
.document-processing-icon {
  color: #2585db;
  font-size: 42px;
}
.document-processing h3 {
  margin: 16px 0 8px;
  color: #243b53;
  font-size: 19px;
}
.document-processing p {
  margin: 0 0 22px;
  color: #718096;
}
.processing-elapsed {
  display: block;
  margin-top: 9px;
  color: #8a98a7;
  font-size: 12px;
}
.document-processing strong {
  display: block;
  margin-top: 22px;
  color: #486078;
  font-size: 14px;
  font-weight: 500;
}
.extract-dialog-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding-right: 30px;
}
.extract-dialog-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  margin-left: auto;
}
.extract-dialog-actions .el-button {
  margin-left: 0;
}
::v-deep .extract-dialog {
  display: flex;
  flex-direction: column;
  height: 92vh;
  margin: 4vh auto !important;
}
::v-deep .extract-dialog .el-dialog__header {
  flex: 0 0 auto;
}
::v-deep .extract-dialog .el-dialog__body {
  display: flex;
  flex: 1;
  min-height: 0;
  padding: 18px 20px;
  overflow: hidden;
}
.extract-dialog-body {
  flex: 1;
  min-height: 0;
  padding-right: 10px;
  overflow-x: hidden;
  overflow-y: scroll;
}
.material-page {
  background: #f3f7fb;
  min-height: calc(100vh - 84px);
}
.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(125deg, #0b5fd7 0%, #1677e8 52%, #24a2d8 100%);
  box-shadow: 0 16px 36px rgba(20, 92, 181, 0.18);
}
.page-hero h1 {
  margin: 5px 0 0;
  font-size: 27px;
  letter-spacing: 0.5px;
}
.page-kicker {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.8px;
  color: #bfe2ff;
}
.page-hero .el-button {
  border: 0;
  color: #0b5fd7;
  background: #fff;
  box-shadow: 0 8px 20px rgba(4, 42, 91, 0.16);
}
.stat-grid {
  margin-top: 16px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 94px;
  padding: 18px 20px;
  border: 1px solid #e5edf6;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(22, 63, 105, 0.05);
}
.stat-card > i {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 11px;
  font-size: 21px;
  color: #176fd1;
  background: #eaf3ff;
}
.stat-card strong,
.stat-card span {
  display: block;
}
.stat-card strong {
  font-size: 24px;
  line-height: 1.1;
  color: #13283f;
}
.stat-card span {
  margin-top: 6px;
  font-size: 12px;
  color: #74869a;
}
.stat-card.is-recognized > i {
  color: #16936e;
  background: #e8f8f2;
}
.stat-card.is-pending > i {
  color: #c58419;
  background: #fff6e5;
}
.stat-card.is-failed > i {
  color: #d94d55;
  background: #fff0f1;
}
.content-card {
  margin-top: 16px;
  padding: 20px 22px;
  border: 1px solid #e5edf6;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(22, 63, 105, 0.045);
}
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.section-heading h2 {
  margin: 0 0 3px;
  font-size: 16px;
  color: #17324f;
}
.section-heading span {
  font-size: 12px;
  color: #8a9aae;
}
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.filter-tag {
  cursor: pointer;
  border-radius: 14px;
  user-select: none;
}
.empty-hint {
  font-size: 12px;
  color: #a4b0bd;
}
.search-form {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 0 4px;
}
.search-form .table-actions {
  margin-left: auto;
}
.document-table {
  margin-top: 4px;
}
.document-table ::v-deep .operation-column .cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 0 6px;
  white-space: nowrap;
}
.operation-button {
  min-width: 28px;
  margin-left: 0 !important;
  padding: 6px;
  font-size: 16px;
}
.document-link {
  display: flex;
  align-items: center;
  max-width: 100%;
  gap: 9px;
  padding: 0;
  border: 0;
  color: #175fae;
  background: transparent;
  cursor: pointer;
  font: inherit;
}
.document-link i {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  line-height: 30px;
  border-radius: 8px;
  color: #2379d8;
  background: #edf5ff;
}
.document-link span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.table-tag {
  margin: 2px 5px 2px 0;
}
.document-kind-tip {
  margin-top: 7px;
  color: #8a98a7;
  font-size: 12px;
  line-height: 1.5;
}
.more-button {
  margin-left: 9px;
}
.drawer-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 36px;
  color: #19324d;
}
.drawer-title small {
  overflow: hidden;
  color: #8797a9;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.drawer-body {
  flex: 1;
  min-height: 0;
  height: auto;
  padding: 0 24px 28px;
  overflow: auto;
}
.tag-manager-body {
  height: calc(100vh - 78px);
  padding: 0 24px 28px;
  overflow: auto;
}
::v-deep .document-detail-dialog {
  display: flex;
  flex-direction: column;
  height: 90vh;
  margin: 5vh auto !important;
}
::v-deep .document-detail-dialog .el-dialog__header {
  flex: 0 0 auto;
}
::v-deep .document-detail-dialog .el-dialog__body {
  display: flex;
  flex: 1;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}
.detail-form {
  padding-top: 8px;
}
.metadata-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 22px;
  padding: 14px 16px;
  border-radius: 9px;
  color: #65788c;
  background: #f5f8fc;
}
.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}
.ocr-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 8px 0 16px;
}
.ocr-toolbar span {
  margin-left: 10px;
  font-size: 12px;
  color: #8a98a7;
}
.ocr-error {
  margin-bottom: 14px;
}
.ocr-content {
  min-height: 360px;
  padding: 20px;
  border: 1px solid #e2eaf3;
  border-radius: 10px;
  color: #33485e;
  background: #f8fafc;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
}
.record-editor {
  display: grid;
  gap: 12px;
  margin: 8px 0 18px;
  padding: 16px;
  border: 1px solid #dce8f5;
  border-radius: 11px;
  background: #f8fbff;
}
.record-editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #27445f;
}
.record-editor-actions {
  text-align: right;
}
.record-list {
  min-height: 220px;
}
.record-item {
  margin-bottom: 12px;
  padding: 16px;
  border: 1px solid #e2eaf3;
  border-radius: 11px;
  background: #fff;
}
.record-item header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.record-avatar {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  line-height: 34px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #176fd1, #22a1da);
  text-align: center;
}
.record-item header strong,
.record-item header span {
  display: block;
}
.record-item header span,
.record-item footer {
  margin-top: 3px;
  font-size: 11px;
  color: #96a3b1;
}
.record-actions {
  margin-left: auto;
}
.inline-record-editor {
  display: grid;
  gap: 10px;
  margin-top: 13px;
  padding: 13px;
  border: 1px solid #9ecbf3;
  border-radius: 9px;
  background: #f7fbff;
}
.inline-record-editor > div:last-child {
  text-align: right;
}
.record-content {
  margin-top: 13px;
  padding: 11px 13px;
  border-radius: 8px;
  background: #f5f8fc;
}
.record-content.remark {
  border-left: 3px solid #e5a93d;
  background: #fff8eb;
}
.record-content b {
  color: #50677e;
  font-size: 12px;
}
.record-content p {
  margin: 5px 0 0;
  color: #2e4154;
  line-height: 1.65;
  white-space: pre-wrap;
}
.record-item footer {
  text-align: right;
}
.danger-text {
  color: #e0525d !important;
}
.new-tag-row {
  display: flex;
  gap: 10px;
  margin: 18px 0;
}
@media (max-width: 767px) {
  .material-page {
    padding: 12px;
  }
  .page-hero {
    align-items: flex-start;
    padding: 22px 20px;
  }
  .page-hero h1 {
    font-size: 22px;
  }
  .page-hero p {
    display: none;
  }
  .page-hero .el-button {
    padding: 10px;
    font-size: 0;
  }
  .page-hero .el-button i {
    margin: 0;
    font-size: 16px;
  }
  .stat-grid .el-col {
    margin-bottom: 10px;
  }
  .stat-card {
    min-height: 78px;
    padding: 12px;
  }
  .stat-card > i {
    width: 34px;
    height: 34px;
    font-size: 17px;
  }
  .stat-card strong {
    font-size: 19px;
  }
  .content-card {
    margin-top: 8px;
    padding: 15px;
  }
  .search-form .el-form-item {
    width: 100%;
    margin-right: 0;
  }
  .search-form .el-input,
  .search-form .el-select {
    width: 100%;
  }
  .search-form .table-actions {
    display: none;
  }
}
</style>
