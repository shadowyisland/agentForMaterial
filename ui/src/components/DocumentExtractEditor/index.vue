<template>
  <el-form v-if="value" label-position="top" class="extract-editor">
    <section
      v-for="section in orderedSections"
      :key="section.key"
      class="editor-section"
    >
      <template v-if="section.type === 'image'">
        <div v-for="field in section.fields" :key="field.type" class="image-section">
          <div class="editor-section-title"><i class="el-icon-picture-outline" /> {{ field.title }}</div>
          <div class="image-source-actions"><el-button type="primary" plain icon="el-icon-folder-opened" :disabled="!documentId" @click="openMineruImagePicker(field)">从当前文件选择</el-button><span>或本地上传</span></div>
          <el-upload :action="uploadUrl" :headers="headers" list-type="picture-card" :file-list="imageFileList(field)" :on-success="(res, file) => handleImageSuccess(res, file, field)" :on-remove="() => removeImage(field)" :on-preview="previewImage" :limit="1" accept="image/png,image/jpeg,image/jpg"><i class="el-icon-plus" /></el-upload>
          <el-empty v-if="!imageByType(field.type)" :description="'请从当前文件选择或本地上传' + field.title + '图片'" :image-size="54" />
          <p class="section-hint">当前文件的 MinerU 解析图片已保存至 MinIO；下载 Word 时会插入对应位置。</p>
        </div>
      </template>
      <div v-else class="editor-section-title">
        <i :class="sectionIcon(section.type)" /> {{ section.key }}
        <el-button v-if="section.type === 'text'" type="text" class="danger-text" icon="el-icon-delete" title="删除此组件" @click="removeRootComponent(section.key)" />
      </div>

      <template v-if="section.type === 'text'">
        <el-input
          v-model="value[section.key]"
          :type="section.long ? 'textarea' : 'text'"
          :rows="section.long ? 5 : 1"
        />
      </template>

      <template v-else-if="section.type === 'scalar-array'">
        <el-button type="text" size="mini" icon="el-icon-plus" @click="section.value.push('')">
          新增条目
        </el-button>
        <div
          v-for="(item, index) in section.value"
          :key="index"
          class="array-item-row"
        >
          <el-input
            :value="item"
            type="textarea"
            :rows="2"
            @input="setArrayValue(section.value, index, $event)"
          />
          <el-button
            type="text"
            icon="el-icon-delete"
            class="danger-text"
            @click="section.value.splice(index, 1)"
          />
        </div>
        <el-empty
          v-if="!section.value.length"
          :description="'暂无' + section.key"
          :image-size="54"
        />
      </template>

      <template v-else-if="section.type === 'object-array'">
        <div class="nested-table-title">
          <span />
          <el-button
            type="text"
            size="mini"
            icon="el-icon-plus"
            @click="addObjectArrayItem(section.value, [section.key])"
          >
            新增行
          </el-button>
          <el-button
            v-if="isTds"
            type="text"
            size="mini"
            class="danger-text"
            @click="removeArrayParameter(value, section.key, [section.key])"
          >
            删除参数
          </el-button>
        </div>
        <el-table
          :data="section.value"
          border
          size="small"
          class="specification-table"
          :row-class-name="tableRowClass"
        >
          <el-table-column
            v-for="column in arrayColumns(section.value)"
            :key="column"
            :label="column"
            min-width="140"
          >
            <template slot-scope="scope">
              <el-input
                :value="getArrayColumnValue(scope.row, column)"
                @input="
                  setArrayColumnValue(
                    section.value,
                    scope.$index,
                    column,
                    $event
                  )
                "
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="54" align="center">
            <template slot-scope="scope">
              <el-button
                type="text"
                class="row-delete"
                title="删除本行"
                @click="section.value.splice(scope.$index, 1)"
              >×</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-if="!section.value.length"
          :description="'暂无' + section.key"
          :image-size="54"
        />
      </template>

      <template v-else-if="section.type === 'specification'">
        <template v-for="entry in specificationEntries(section.value)">
        <div
          v-if="entry.type === 'array'"
          :key="section.key + entry.key"
          class="nested-table-block"
        >
          <div class="nested-table-title">
            <strong> {{ entry.key }} </strong
            ><el-button
              type="text"
              size="mini"
              icon="el-icon-plus"
              @click="addObjectArrayItem(entry.value, [section.key, entry.key])"
              >新增行</el-button
            >
            <el-button
              v-if="isTds"
              type="text"
              size="mini"
              class="danger-text"
              @click="removeArrayParameter(section.value, entry.key, [section.key, entry.key])"
            >删除参数</el-button>
          </div>
          <el-table
            :data="entry.value"
            border
            size="small"
            class="specification-table"
            :row-class-name="tableRowClass"
          >
            <el-table-column
              v-for="column in arrayColumns(entry.value)"
              :key="column"
              :label="column"
              min-width="130"
            >
              <template slot-scope="scope">
                <el-input
                  :value="getArrayColumnValue(scope.row, column)"
                  :disabled="isArrayCellDisabled(entry.key, scope.row, column)"
                  @input="
                    setArrayColumnValue(
                      entry.value,
                      scope.$index,
                      column,
                      $event
                    )
                  "
                /> </template
            ></el-table-column>
            <el-table-column label="操作" width="54" align="center">
              <template slot-scope="scope">
                <el-button
                  type="text"
                  class="row-delete"
                  title="删除本行"
                  @click="entry.value.splice(scope.$index, 1)"
                >×</el-button> </template
            ></el-table-column>
          </el-table>
        </div>
        <el-table
          v-else
          :key="section.key + entry.key"
          :data="[entry]"
          border
          size="small"
          class="specification-table specification-single-row"
          :row-class-name="() => entryRowClass(entry)"
        >
          <el-table-column label="参数" min-width="180">
            <template slot-scope="scope">
              <span> {{ scope.row.key }} </span></template
            >
          </el-table-column>
          <el-table-column
            v-for="column in entryColumns(entry)"
            :key="column"
            :label="column"
            min-width="138"
          >
            <template slot-scope="scope">
              <el-input
                :value="getEntryValue(scope.row, column)"
                @input="setEntryValue(scope.row, column, $event)"
              /> </template
          ></el-table-column>
          <el-table-column label="操作" width="54" align="center">
            <template slot-scope="scope">
              <el-button
                type="text"
                class="row-delete"
                title="删除本行"
                @click="removeSpecificationEntry(section.key, scope.row)"
              >×</el-button>
            </template>
          </el-table-column>
        </el-table>
        </template>
      </template>

      <template v-else-if="section.type === 'object' && isMsds">
        <template v-for="item in orderedObjectItems(section.value)">
          <div v-if="item.type === 'array'" :key="item.path.join('.')" class="nested-table-block">
            <div class="nested-table-title"><strong>{{ item.path.join(' / ') }}</strong><el-button type="text" size="mini" icon="el-icon-plus" @click="addObjectArrayItem(item.value, [section.key].concat(item.path))">新增行</el-button><el-button type="text" size="mini" class="danger-text" @click="removeArrayParameter(getParentByPath(section.value, item.path), item.path[item.path.length - 1], [section.key].concat(item.path))">删除参数</el-button></div>
            <el-table :data="item.value" border size="small" class="specification-table" :row-class-name="tableRowClass">
              <el-table-column v-for="column in arrayColumns(item.value)" :key="column" :label="column" min-width="130"><template slot-scope="scope"><el-input :value="getArrayColumnValue(scope.row, column)" @input="setArrayColumnValue(item.value, scope.$index, column, $event)" /></template></el-table-column>
              <el-table-column label="操作" width="54" align="center"><template slot-scope="scope"><el-button type="text" class="row-delete" @click="item.value.splice(scope.$index, 1)">×</el-button></template></el-table-column>
            </el-table>
          </div>
          <el-form-item v-else :key="item.path.join('.')" :label="item.path.join(' / ')" class="msds-single-field">
            <el-button type="text" class="field-delete danger-text" icon="el-icon-delete" title="删除此字段" @click="removeObjectField(section.value, item.path, [section.key].concat(item.path))" />
            <template v-if="imageFieldForPath(section.key, item.path)">
              <div class="image-source-actions"><el-button type="primary" plain icon="el-icon-folder-opened" :disabled="!documentId" @click="openMineruImagePicker(imageFieldForPath(section.key, item.path))">从当前文件选择</el-button><span>或本地上传</span></div>
              <el-upload :action="uploadUrl" :headers="headers" list-type="picture-card" :file-list="imageFileList(imageFieldForPath(section.key, item.path))" :on-success="(res, file) => handleImageSuccess(res, file, imageFieldForPath(section.key, item.path))" :on-remove="(file) => removeImage(imageFieldForPath(section.key, item.path), file)" :on-preview="previewImage" accept="image/png,image/jpeg,image/jpg"><i class="el-icon-plus" /></el-upload>
            </template>
            <el-input v-else :value="item.value" :type="item.long ? 'textarea' : 'text'" :rows="item.long ? 4 : 1" @input="setByPath(section.value, item.path, $event)" />
          </el-form-item>
        </template>
      </template>

      <template v-else-if="section.type === 'object'">
        <el-row :gutter="16">
          <el-col
            v-for="leaf in objectLeaves(section.value)"
            :key="leaf.path.join('.')"
            :xs="24"
            :sm="leaf.long ? 24 : 12"
          >
            <el-form-item :label="leaf.path.join(' / ')">
              <el-button type="text" class="field-delete danger-text" icon="el-icon-delete" title="删除此字段" @click="removeObjectField(section.value, leaf.path, [section.key].concat(leaf.path))" />
              <template v-if="imageFieldForPath(section.key, leaf.path)">
                <div class="image-source-actions"><el-button type="primary" plain icon="el-icon-folder-opened" :disabled="!documentId" @click="openMineruImagePicker(imageFieldForPath(section.key, leaf.path))">从当前文件选择</el-button><span>或本地上传</span></div>
                <el-upload :action="uploadUrl" :headers="headers" list-type="picture-card" :file-list="imageFileList(imageFieldForPath(section.key, leaf.path))" :on-success="(res, file) => handleImageSuccess(res, file, imageFieldForPath(section.key, leaf.path))" :on-remove="(file) => removeImage(imageFieldForPath(section.key, leaf.path), file)" :on-preview="previewImage" accept="image/png,image/jpeg,image/jpg"><i class="el-icon-plus" /></el-upload>
              </template>
              <el-input v-else
                :value="leaf.value"
                :type="leaf.long ? 'textarea' : 'text'"
                :rows="leaf.long ? 4 : 1"
                @input="setByPath(section.value, leaf.path, $event)"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <template v-for="arrayField in objectArrays(section.value)">
          <div :key="arrayField.path.join('.')" class="nested-table-block">
            <div class="nested-table-title">
              <strong> {{ arrayField.path.join(" / ") }} </strong>
              <el-button
                type="text"
                size="mini"
                icon="el-icon-plus"
                @click="addObjectArrayItem(arrayField.value, [section.key].concat(arrayField.path))"
              >
                新增行
              </el-button>
              <el-button
                v-if="isTds"
                type="text"
                size="mini"
                class="danger-text"
                @click="removeArrayParameter(getParentByPath(section.value, arrayField.path), arrayField.path[arrayField.path.length - 1], [section.key].concat(arrayField.path))"
              >删除参数</el-button>
            </div>
            <el-table
              :data="arrayField.value"
              border
              size="small"
              class="specification-table"
              :row-class-name="tableRowClass"
            >
              <el-table-column
                v-for="column in arrayColumns(arrayField.value)"
                :key="column"
                :label="column"
                min-width="130"
              >
                <template slot-scope="scope">
                  <el-input
                    :value="getArrayColumnValue(scope.row, column)"
                    @input="
                      setArrayColumnValue(
                        arrayField.value,
                        scope.$index,
                        column,
                        $event
                      )
                    "
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="54" align="center">
                <template slot-scope="scope">
                  <el-button
                    type="text"
                    class="row-delete"
                    title="删除本行"
                    @click="arrayField.value.splice(scope.$index, 1)"
                  >×</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </template>
    </section>

    <el-dialog
      :visible.sync="mineruImagePickerOpen"
      append-to-body
      title="从当前文件选择图片"
      width="900px"
      class="mineru-image-picker"
    >
      <div class="mineru-image-context">
        <strong>当前上传文件</strong>
        <span>{{ currentDocumentName || "当前文件" }}</span>
        <small>以下图片由 MinerU 解析后保存至 MinIO</small>
      </div>
      <div v-loading="mineruImageLoading" class="mineru-image-grid">
        <button
          v-for="image in mineruImages"
          :key="image.imageId"
          type="button"
          class="mineru-image-card"
          :class="{ selected: selectedMineruImageIds.includes(image.imageId) }"
          @click="toggleMineruImage(image.imageId)"
        >
          <img :src="image.previewUrl" :alt="image.name" />
          <i v-if="selectedMineruImageIds.includes(image.imageId)" class="el-icon-check" />
          <span>{{ image.name }}</span>
        </button>
        <el-empty
          v-if="!mineruImageLoading && !mineruImages.length"
          description="当前上传文件没有可用的 MinerU 解析图片"
          :image-size="64"
        />
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="mineruImagePickerOpen = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!selectedMineruImageIds.length"
          @click="useSelectedMineruImage"
        >确认使用</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :visible.sync="previewOpen"
      append-to-body
      title="图片预览"
      width="760px"
    >
      <img :src="previewUrl" class="image-preview" />
    </el-dialog>
  </el-form>
</template>

<script>
import { getToken } from "@/utils/auth";
import { listMineruImages, readMineruImage } from "@/api/system/document";
import { createExtractTemplate, DELETED_ROWS_KEY } from "./templateDefaults";

const LONG_FIELD_NAMES = [
  "产品描述",
  "组分及结构式",
  "主要特点",
  "主要特性",
  "推荐用途",
  "推荐应用及适用类型",
  "建议用量",
  "加入方法及加工指导",
  "主要应用",
  "应用",
  "使用方法",
  "包装",
  "可用包装",
  "储存及贮存期",
  "贮存及保质期",
  "贮存及运输",
  "安全及健康",
  "特别说明",
  "免责声明",
  "注意事项",
];
const SPECIFICATION_KEYS = ["产品规格", "物化参数", "典型参数"];

export default {
  name: "DocumentExtractEditor",
  props: {
    value: {
      type: Object,
      required: true,
    },
    materialCategory: {
      type: String,
      default: "",
    },
    documentKind: {
      type: String,
      default: "",
    },
    documentId: {
      type: [Number, String],
      default: null,
    },
    currentDocumentName: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      headers: {
        Authorization: "Bearer " + getToken(),
      },
      previewOpen: false,
      previewUrl: "",
      mineruImagePickerOpen: false,
      mineruImageLoading: false,
      mineruImages: [],
      selectedMineruImageIds: [],
      activeImageField: null,
      mineruPreviewUrls: {},
    };
  },
  computed: {
    formTemplate() {
      return createExtractTemplate(this.materialCategory, this.documentKind);
    },
    orderedSections() {
      const sections = Object.keys(this.value)
        .filter((key) => key !== "图片" && key !== DELETED_ROWS_KEY)
        .map((key) => ({
          key,
          value: this.value[key],
          type: this.sectionType(key, this.value[key]),
          long:
            LONG_FIELD_NAMES.includes(key) ||
            String(this.value[key] || "").length > 80,
        }));
      const result = [];
      const imageFields = this.isMsds ? [] : this.imageFields;
      sections.forEach((section) => {
        const beforeFields = imageFields.filter((field) => field.before === section.key);
        if (beforeFields.length) {
          result.push({ key: section.key + "-前图片", type: "image", fields: beforeFields });
        }
        result.push(section);
        const fields = imageFields.filter((field) => field.after === section.key);
        if (fields.length) {
          result.push({ key: section.key + "-图片", type: "image", fields });
        }
      });
      const unmatchedBeforeFields = imageFields.filter(
        (field) => field.before && !sections.some((section) => section.key === field.before)
      );
      if (unmatchedBeforeFields.length) {
        result.unshift({ key: "顶部图片", type: "image", fields: unmatchedBeforeFields });
      }
      return result;
    },
    images() {
      return Array.isArray(this.value.图片) ? this.value.图片 : [];
    },
    isEpoxyTds() {
      return this.materialCategory === "EPOXY" && this.documentKind === "TDS";
    },
    isMsds() { return this.documentKind === "MSDS"; },
    imageFields() {
      if (this.isEpoxyTds) return [{ type: "分子结构", title: "分子结构", position: "分子结构后", before: "主要特性" }];
      if (!this.isMsds) return [];
      return [
        { type: "象形图", title: "象形图", position: "第2部分标签要素", path: "第2部分 危险标识.标签要素.象形图" },
        { type: "个人防护装备总要求", title: "个人防护装备总要求", position: "第8部分个人防护装备", path: "第8部分 接触控制/个体防护.接触控制.个人防护装备.总要求" },
        { type: "运输标签", title: "运输标签", position: "第14部分标签和标记", path: "第14部分 运输信息.运输标签" },
      ];
    },
    isFillerTds() {
      return this.materialCategory === "FILLER" && this.documentKind === "TDS";
    },
    isTds() {
      return this.documentKind === "TDS";
    },
    structureImage() {
      return this.images.find((image) => image.类型 === "分子结构");
    },
    structureImageFileList() {
      return this.structureImage
        ? [this.toUploadFile(this.structureImage, 0)]
        : [];
    },
  },
  methods: {
    toUploadFile(image, index) {
      return {
        name: image.名称 || "图片" + (index + 1),
        url: this.structureImageUrl(image),
        uid: (image.图片ID || image.路径 || image.名称 || "image") + index,
      };
    },
    async openMineruImagePicker(field) {
      if (!this.documentId) return;
      this.activeImageField = field || this.imageFields[0];
      this.mineruImagePickerOpen = true;
      this.selectedMineruImageIds = this.images.filter((image) => image.类型 === this.activeImageField.type && image.图片ID).map((image) => image.图片ID);
      this.mineruImageLoading = true;
      try {
        const response = await listMineruImages(this.documentId);
        const images = response.data || [];
        this.mineruImages = await Promise.all(
          images.map(async (image) => ({
            ...image,
            previewUrl: await this.loadMineruImageUrl(image.imageId),
          }))
        );
      } catch (error) {
        this.mineruImages = [];
        this.$modal.msgError("加载当前文件解析图片失败");
      } finally {
        this.mineruImageLoading = false;
      }
    },
    async loadMineruImageUrl(imageId) {
      if (this.mineruPreviewUrls[imageId]) return this.mineruPreviewUrls[imageId];
      const blob = await readMineruImage(this.documentId, imageId);
      const url = URL.createObjectURL(blob);
      this.$set(this.mineruPreviewUrls, imageId, url);
      return url;
    },
    async ensureStructureImagePreview() {
      if (!(this.structureImage && this.structureImage.图片ID) || !this.documentId) return;
      try {
        await this.loadMineruImageUrl(this.structureImage.图片ID);
      } catch (error) {
        this.$modal.msgError("加载已选 MinerU 图片失败");
      }
    },
    async ensureSelectedImagePreviews() {
      for (const image of this.images) {
        if (image.图片ID && this.documentId) await this.loadMineruImageUrl(image.图片ID);
      }
    },
    structureImageUrl(image) {
      return image && image.图片ID
        ? this.mineruPreviewUrls[image.图片ID] || ""
        : this.imageUrl(image && image.路径);
    },
    useSelectedMineruImage() {
      const selected = this.mineruImages.filter((image) => this.selectedMineruImageIds.includes(image.imageId));
      if (!selected.length) return;
      const images = this.ensureImageList();
      for (let index = images.length - 1; index >= 0; index--) {
        if (images[index].类型 === this.activeImageField.type && images[index].图片ID) images.splice(index, 1);
      }
      selected.forEach((image) => images.push({ 名称: image.name, 类型: this.activeImageField.type, 位置: this.activeImageField.position, 来源: "MINERU", 图片ID: image.imageId }));
      this.mineruImagePickerOpen = false;
    },
    toggleMineruImage(imageId) {
      const index = this.selectedMineruImageIds.indexOf(imageId);
      if (index >= 0) this.selectedMineruImageIds.splice(index, 1);
      else this.selectedMineruImageIds.push(imageId);
    },
    releaseMineruPreviews() {
      Object.keys(this.mineruPreviewUrls).forEach((imageId) => {
        URL.revokeObjectURL(this.mineruPreviewUrls[imageId]);
      });
      this.mineruPreviewUrls = {};
      this.mineruImages = [];
    },
    removeStructureImage() {
      this.removeImage({ type: "分子结构" });
    },
    imageByType(type) { return this.images.find((image) => image.类型 === type); },
    imageFieldForPath(sectionKey, path) {
      const fullPath = [sectionKey].concat(path).join('.');
      return this.imageFields.find((field) => field.path === fullPath);
    },
    imageFileList(field) {
      return this.images.filter((image) => image.类型 === field.type).map((image, index) => this.toUploadFile(image, index));
    },
    removeImage(field, file) {
      const images = this.ensureImageList();
      for (let index = images.length - 1; index >= 0; index--) {
        const image = images[index];
        if (image.类型 === field.type && (!file || (image.图片ID || image.路径 || image.名称) + index === file.uid)) images.splice(index, 1);
      }
    },
    isObject(value) {
      return value && typeof value === "object" && !Array.isArray(value);
    },
    sectionType(key, value) {
      if (SPECIFICATION_KEYS.includes(key) && this.isObject(value))
        return "specification";
      if (Array.isArray(value))
        return value.some((item) => this.isObject(item)) ||
          (this.formTemplate[key] || []).some((item) => this.isObject(item))
          ? "object-array"
          : "scalar-array";
      return this.isObject(value) ? "object" : "text";
    },
    sectionIcon(type) {
      const icons = {
        text: "el-icon-document",
        "scalar-array": "el-icon-collection-tag",
        "object-array": "el-icon-menu",
        specification: "el-icon-tickets",
        object: "el-icon-document-copy",
      };
      return icons[type] || "el-icon-document";
    },
    isLongField(key, value) {
      return LONG_FIELD_NAMES.includes(key) || String(value || "").length > 80;
    },
    setArrayValue(array, index, value) {
      this.$set(array, index, value);
    },
    specificationEntries(specification) {
      return Object.keys(specification || {})
        .filter((key) => key !== "自定义参数")
        .map((key) => ({
          key,
          parent: specification,
          value: specification[key],
          type: Array.isArray(specification[key]) ? "array" : "value",
        }));
    },
    removeSpecificationEntry(sectionKey, entry) {
      this.$delete(entry.parent, entry.key);
      this.markDeletedRow([sectionKey, entry.key]);
    },
    removeArrayParameter(parent, key, path) {
      this.$delete(parent, key);
      this.markDeletedRow(path);
    },
    removeObjectField(parent, path, deletedPath) {
      this.$delete(this.getParentByPath(parent, path), path[path.length - 1]);
      this.markDeletedRow(deletedPath);
    },
    removeRootComponent(key) {
      this.$delete(this.value, key);
      this.markDeletedRow([key]);
    },
    markDeletedRow(path) {
      const text = path.join(".");
      if (!Array.isArray(this.value[DELETED_ROWS_KEY])) {
        this.$set(this.value, DELETED_ROWS_KEY, []);
      }
      if (!this.value[DELETED_ROWS_KEY].includes(text)) {
        this.value[DELETED_ROWS_KEY].push(text);
      }
    },
    entryRowClass(entry) {
      return this.isTableValueEmpty(entry.value) ? "missing-data-row" : "";
    },
    tableRowClass({ row }) {
      return this.isTableValueEmpty(row) ? "missing-data-row" : "";
    },
    isTableValueEmpty(value) {
      if (
        this.isFillerTds &&
        this.isObject(value) &&
        value.类别 === "种类" &&
        Object.prototype.hasOwnProperty.call(value, "种类名")
      ) {
        return !String(value.种类名 || "").trim();
      }
      const leaves = this.valueLeaves(value);
      const editable = leaves.filter(({ key }) => !this.isAssistingField(key));
      const preferred = editable.filter(({ key }) =>
        /(?:数值|输出|含量|标准值)$/.test(key)
      );
      const candidates = preferred.length ? preferred : editable;
      return candidates.length > 0 && candidates.every(({ value }) => !String(value || "").trim());
    },
    valueLeaves(value, key = "") {
      if (!this.isObject(value)) return [{ key, value }];
      return Object.keys(value).reduce(
        (result, childKey) =>
          result.concat(this.valueLeaves(value[childKey], childKey)),
        []
      );
    },
    isAssistingField(key) {
      return [
        "单位",
        "测试温度",
        "测试方法",
        "测试转速",
        "测试条件",
        "测试标准",
        "时间单位",
        "类别",
      ].includes(key);
    },
    isArrayCellDisabled(entryKey, row, column) {
      if (!this.isFillerTds || entryKey !== "溶剂/分散剂") return false;
      const key = column.split(" / ").pop();
      return (
        (row.类别 === "种类" && (key === "单位" || key === "数值")) ||
        (row.类别 === "含量" && key === "种类名")
      );
    },
    entryColumns(entry) {
      return this.isObject(entry.value)
        ? Object.keys(entry.value).filter(
            (key) =>
              !Array.isArray(entry.value[key]) &&
              !this.isObject(entry.value[key])
          )
        : ["数值"];
    },
    getEntryValue(entry, column) {
      return this.isObject(entry.value)
        ? entry.value[column] || ""
        : column === "数值"
        ? entry.value || ""
        : "";
    },
    setEntryValue(entry, column, value) {
      if (this.isObject(entry.value)) this.$set(entry.value, column, value);
      else if (column === "数值")
        this.$set(entry.parent, entry.key, value);
    },
    arrayColumns(array) {
      const columns = [];
      array.forEach((item) => {
        if (this.isObject(item)) this.collectObjectColumns(item, [], columns);
      });
      return columns.length ? columns : ["数值"];
    },
    collectObjectColumns(object, prefix, columns) {
      Object.keys(object || {}).forEach((key) => {
        const value = object[key];
        const path = prefix.concat(key);
        if (this.isObject(value))
          this.collectObjectColumns(value, path, columns);
        else if (!Array.isArray(value) && !columns.includes(path.join(" / ")))
          columns.push(path.join(" / "));
      });
    },
    getArrayColumnValue(row, column) {
      return this.isObject(row)
        ? this.getByPath(row, column.split(" / "))
        : column === "数值"
        ? row || ""
        : "";
    },
    setArrayColumnValue(array, index, column, value) {
      if (this.isObject(array[index]))
        this.setByPath(array[index], column.split(" / "), value);
      else if (column === "数值") this.$set(array, index, value);
    },
    addObjectArrayItem(array, path) {
      const template = this.getByPath(this.formTemplate, path);
      // 删除全部行后，仍可按原字段结构重新添加。
      array.push(this.emptyLike(array[0] || template[0]));
    },
    emptyLike(value) {
      if (Array.isArray(value)) return [];
      if (this.isObject(value)) {
        const result = {};
        Object.keys(value).forEach((key) => {
          result[key] = this.emptyLike(value[key]);
        });
        return result;
      }
      return "";
    },
    objectLeaves(object, prefix = []) {
      const leaves = [];
      Object.keys(object || {}).forEach((key) => {
        const current = object[key];
        const path = prefix.concat(key);
        if (this.isObject(current))
          leaves.push(...this.objectLeaves(current, path));
        else if (!Array.isArray(current))
          leaves.push({
            path,
            value: current,
            long: this.isLongField(key, current),
          });
      });
      return leaves;
    },
    orderedObjectItems(object, prefix = []) {
      const result = [];
      Object.keys(object || {}).forEach((key) => {
        const value = object[key];
        const path = prefix.concat(key);
        if (Array.isArray(value)) result.push({ type: 'array', path, value });
        else if (this.isObject(value)) result.push(...this.orderedObjectItems(value, path));
        else result.push({ type: 'leaf', path, value, long: this.isLongField(key, value) });
      });
      return result;
    },
    objectArrays(object, prefix = []) {
      const arrays = [];
      Object.keys(object || {}).forEach((key) => {
        const current = object[key];
        const path = prefix.concat(key);
        if (Array.isArray(current))
          arrays.push({
            path,
            value: current,
          });
        else if (this.isObject(current))
          arrays.push(...this.objectArrays(current, path));
      });
      return arrays;
    },
    getByPath(object, path) {
      let target = object;
      path.forEach((key) => {
        target = target == null ? "" : target[key];
      });
      return target == null ? "" : target;
    },
    getParentByPath(object, path) {
      return path.slice(0, -1).reduce(
        (target, key) => (target == null ? null : target[key]),
        object
      );
    },
    setByPath(object, path, value) {
      let target = object;
      path.slice(0, -1).forEach((key) => {
        if (!this.isObject(target[key])) this.$set(target, key, {});
        target = target[key];
      });
      this.$set(target, path[path.length - 1], value);
    },
    handleImageSuccess(res, file, field) {
      if (res.code !== 200) {
        this.$modal.msgError(res.msg || "图片上传失败");
        return;
      }
      this.ensureImageList().push({
        名称: field.title,
        类型: field.type,
        位置: field.position,
        路径: res.fileName,
      });
    },
    ensureImageList() {
      if (!Array.isArray(this.value.图片)) this.$set(this.value, "图片", []);
      return this.value.图片;
    },
    previewImage(file) {
      this.previewUrl = file.url;
      this.previewOpen = true;
    },
    imageUrl(path) {
      return !path
        ? ""
        : /^https?:\/\//.test(path)
        ? path
        : process.env.VUE_APP_BASE_API + path;
    },
  },
  mounted() {
    this.ensureSelectedImagePreviews().catch(() => {
      this.$modal.msgError("加载已选 MinerU 图片失败");
    });
  },
  beforeDestroy() {
    this.releaseMineruPreviews();
  },
};
</script>

<style scoped lang="scss">
.extract-editor {
  padding: 2px 4px 20px;
}
.editor-section {
  margin-bottom: 24px;
  padding: 18px;
  border: 1px solid #e3ebf4;
  border-radius: 8px;
  background: #fff;
}
.editor-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  color: #243b53;
  font-weight: 600;
}
.editor-section-title i {
  color: #2585db;
  font-size: 17px;
}
.editor-section-title .el-button,
.nested-table-title > .el-button:first-of-type {
  margin-left: auto;
}
.array-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 9px;
}
.array-item-row .el-input {
  flex: 1;
}
.specification-table {
  width: 100%;
}
.specification-table ::v-deep .missing-data-row > td,
.specification-table ::v-deep .missing-data-row .el-input__inner {
  background: #fff3c4 !important;
}
.specification-table ::v-deep .missing-data-row .el-input__inner {
  border-color: #efcb76;
}
.row-delete {
  min-width: 22px;
  padding: 0;
  color: #e0525d !important;
  font-size: 20px;
  line-height: 1;
}
.specification-single-row {
  margin-bottom: 10px;
}
.nested-table-block {
  margin: 14px 0;
  padding: 13px;
  border: 1px solid #e8eef5;
  border-radius: 6px;
  background: #fbfdff;
}
.nested-table-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #486078;
}
.image-section :deep(.el-upload-list--picture-card .el-upload-list__item),
.image-section :deep(.el-upload--picture-card) {
  width: 100px;
  height: 100px;
  line-height: 100px;
}
.image-meta-row {
  display: flex;
  align-items: center;
  margin-top: 12px;
}
.image-source-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: #8492a6;
  font-size: 13px;
}
.mineru-image-context {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;
  color: #4c6178;
}
.mineru-image-context strong {
  color: #243b53;
}
.mineru-image-context small {
  flex-basis: 100%;
  color: #8492a6;
}
.mineru-image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-height: 180px;
  max-height: 60vh;
  padding-right: 8px;
  overflow-y: auto;
}
.mineru-image-card {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  padding: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #dce6f0;
  border-radius: 6px;
  background: #fff;
  color: #486078;
  text-align: left;
}
.mineru-image-card:hover,
.mineru-image-card.selected {
  border-color: #2585db;
  box-shadow: 0 0 0 2px rgba(37, 133, 219, 0.12);
}
.mineru-image-card img {
  width: 100%;
  height: 155px;
  object-fit: contain;
  background: #f7f9fc;
}
.mineru-image-card span {
  display: block;
  margin-top: 8px;
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mineru-image-card i {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #2585db;
  color: #fff;
}
.mineru-image-grid :deep(.el-empty) {
  grid-column: 1 / -1;
}
.section-hint {
  margin: 14px 0 0;
  color: #8a98a7;
  font-size: 12px;
  line-height: 1.6;
}
.image-preview {
  display: block;
  max-width: 100%;
  max-height: 72vh;
  margin: 0 auto;
}
.danger-text {
  color: #e0525d !important;
}
@media (max-width: 767px) {
  .editor-section {
    padding: 14px;
  }
  .image-meta-row {
    display: block;
  }
  .image-meta-row .el-col {
    margin-bottom: 8px;
  }
}
</style>
