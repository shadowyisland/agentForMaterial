<template>
  <el-form v-if="value" label-position="top" class="extract-editor">
    <section v-if="isEpoxyTds" class="editor-section image-section">
      <div class="editor-section-title">
        <i class="el-icon-picture-outline" /> 分子结构
      </div>
      <el-upload
        :action="uploadUrl"
        :headers="headers"
        list-type="picture-card"
        :file-list="structureImageFileList"
        :on-success="handleStructureImageSuccess"
        :on-remove="removeImage"
        :on-preview="previewImage"
        :limit="1"
        accept="image/png,image/jpeg,image/jpg"
      >
        <i class="el-icon-plus" />
      </el-upload>
      <el-empty
        v-if="!structureImage"
        description="请上传分子结构图片"
        :image-size="54"
      />
      <p class="section-hint">
        分子结构图片不会由 AI 生成，下载 Word 时会插入“分子结构”标题后。
      </p>
    </section>

    <section
      v-for="section in orderedSections"
      :key="section.key"
      class="editor-section"
    >
      <div class="editor-section-title">
        <i :class="sectionIcon(section.type)" /> {{ section.key }}
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

      <template v-else-if="section.type === 'object'">
        <el-row :gutter="16">
          <el-col
            v-for="leaf in objectLeaves(section.value)"
            :key="leaf.path.join('.')"
            :xs="24"
            :sm="leaf.long ? 24 : 12"
          >
            <el-form-item :label="leaf.path.join(' / ')">
              <el-input
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
  },
  data() {
    return {
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      headers: {
        Authorization: "Bearer " + getToken(),
      },
      previewOpen: false,
      previewUrl: "",
    };
  },
  computed: {
    formTemplate() {
      return createExtractTemplate(this.materialCategory, this.documentKind);
    },
    orderedSections() {
      return Object.keys(this.value)
        .filter((key) => key !== "图片" && key !== DELETED_ROWS_KEY)
        .map((key) => ({
          key,
          value: this.value[key],
          type: this.sectionType(key, this.value[key]),
          long:
            LONG_FIELD_NAMES.includes(key) ||
            String(this.value[key] || "").length > 80,
        }));
    },
    images() {
      return Array.isArray(this.value.图片) ? this.value.图片 : [];
    },
    isEpoxyTds() {
      return this.materialCategory === "EPOXY" && this.documentKind === "TDS";
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
        url: this.imageUrl(image.路径),
        uid: image.路径 + index,
      };
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
    handleStructureImageSuccess(res, file) {
      if (res.code !== 200) {
        this.$modal.msgError(res.msg || "图片上传失败");
        return;
      }
      const images = this.ensureImageList();
      for (let index = images.length - 1; index >= 0; index--) {
        if (images[index].类型 === "分子结构") images.splice(index, 1);
      }
      images.push({
        名称: "分子结构",
        类型: "分子结构",
        位置: "分子结构后",
        路径: res.fileName,
      });
    },
    removeImage(file) {
      const index = this.images.findIndex(
        (image) => this.imageUrl(image.路径) === file.url
      );
      if (index >= 0) this.images.splice(index, 1);
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
