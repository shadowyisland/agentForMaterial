<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="文档名称" prop="documentName">
            <el-input
              v-model="queryParams.documentName"
              placeholder="请输入文档名称"
              clearable
              style="width: 240px"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="标签筛选" prop="searchTag">
            <el-select
              v-model="queryParams.searchTag"
              placeholder="选择或输入标签"
              clearable
              filterable
              style="width: 200px"
              @change="handleQuery"
            >
              <el-option
                v-for="item in allUserTags"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="el-icon-plus"
              size="mini"
              @click="handleAdd"
              v-hasPermi="['system:document:add']"
            >上传文档</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              size="mini"
              :disabled="multiple"
              @click="handleDelete"
              v-hasPermi="['system:document:remove']"
            >删除</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="documentList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="文档ID" align="center" prop="documentId" />

          <el-table-column label="文档名称" align="center" prop="documentName" :show-overflow-tooltip="true">
            <template slot-scope="scope">
              <span
                class="link-type"
                @click="handleDetail(scope.row)"
              >{{ scope.row.documentName }}</span>
            </template>
          </el-table-column>

          <el-table-column label="OCR状态" align="center" prop="isRecognized">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.isRecognized === 1" type="success">已识别</el-tag>
              <el-tag v-else type="info">未识别</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="原始文件名" align="center" prop="fileOriginName" :show-overflow-tooltip="true" />
          <el-table-column label="上传人" align="center" prop="createBy" />
          <el-table-column label="文件标签" align="center" prop="tags" width="200">
          <template slot-scope="scope">
            <div v-if="scope.row.tags && scope.row.tags.length > 0">
              <el-tag
                v-for="(tag, index) in scope.row.tags"
                :key="index"
                size="mini"
                style="margin-right: 4px; margin-bottom: 4px;"
              >
                {{ tag }}
              </el-tag>
            </div>
            <span v-else style="color: #ccc; font-size: 12px;">无标签</span>
          </template>
        </el-table-column>
          <el-table-column label="上传时间" align="center" prop="createTime" width="180">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-download"
                @click="handleDownload(scope.row)"
              >下载</el-button>

              <el-button
                size="mini"
                type="text"
                icon="el-icon-aim"
                @click="handleOcr(scope.row)"
                v-hasPermi="['system:document:edit']"
              >识别</el-button>

              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['system:document:remove']"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="total>0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getList"
        />
      </el-col>
    </el-row>

    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="文档名称" prop="documentName">
          <el-input v-model="form.documentName" placeholder="请输入文档显示名称" />
        </el-form-item>

        <el-form-item label="文件上传" prop="filePath">
          <el-upload
            ref="upload"
            :limit="1"
            accept=".pdf,.png,.jpg,.jpeg"
            :action="uploadUrl"
            :headers="headers"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
            :before-upload="handleBeforeUpload"
            :file-list="fileList"
            drag
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip" slot="tip">支持 PDF/JPG/PNG，且不超过50MB</div>
          </el-upload>
        </el-form-item>

        <el-form-item label="文档标签" prop="tags">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入标签(回车确认)"
            style="width: 100%"
          >
            <el-option
              v-for="item in allUserTags"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>

          <div class="tag-recommend-area" v-if="allUserTags && allUserTags.length > 0">
            <span class="tag-title">常用标签：</span>
            <el-tag
              v-for="tag in visibleTags"
              :key="tag"
              size="small"
              class="recommend-tag"
              @click="handleQuickAddTag(tag)"
              :effect="form.tags && form.tags.includes(tag) ? 'dark' : 'plain'"
            >
              {{ tag }}
            </el-tag>

            <el-button
              v-if="!showAllTags && allUserTags.length > 10"
              type="text"
              size="mini"
              @click="showAllTags = true"
            >... (展开)</el-button>
            <el-button
              v-if="showAllTags && allUserTags.length > 10"
              type="text"
              size="mini"
              @click="showAllTags = false"
            > (收起)</el-button>
          </div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="OCR识别详情" :visible.sync="detailOpen" width="700px" append-to-body>
      <el-form ref="detailForm" :model="detailForm" label-width="100px" size="mini">
        <el-row>
          <el-col :span="24">
            <el-form-item label="文档名称：">{{ detailForm.documentName }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="识别状态：">
              <el-tag v-if="detailForm.isRecognized === 1" type="success">已识别</el-tag>
              <el-tag v-else type="info">未识别</el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="识别时间：">{{ parseTime(detailForm.ocrTime) }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="识别结果：">
              <el-input
                type="textarea"
                :rows="15"
                v-model="detailForm.ocrContent"
                readonly
                placeholder="暂无识别内容"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
// 注意：这里引入了 getTopTags
import { listDocument, addDocument, delDocument, ocrDocument, getDocument, getTopTags } from "@/api/system/document";
import { getToken } from "@/utils/auth";

export default {
  name: "Document",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 文档表格数据
      documentList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // OCR详情弹窗开关
      detailOpen: false,
      // 详情数据对象
      detailForm: {},
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        documentName: undefined
      },
      // 表单参数
      form: {
        tags: [] // 初始化标签
      },
      // 表单校验
      rules: {
        documentName: [
          { required: true, message: "文档名称不能为空", trigger: "blur" }
        ],
        filePath: [
          { required: true, message: "请上传文件", trigger: "blur" }
        ]
      },
      // 上传参数
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      headers: {
        Authorization: "Bearer " + getToken()
      },
      fileList: [],

      // --- 标签相关数据 ---
      allUserTags: [], // 所有标签（从后端获取，已排序）
      showAllTags: false // 是否展开全部标签
    };
  },
  created() {
    this.getList();
    this.getTagsList();
  },
  computed: {
    // 计算属性：决定显示哪些快捷标签（前10个 或 全部）
    visibleTags() {
      if (this.showAllTags) {
        return this.allUserTags;
      } else {
        return this.allUserTags.slice(0, 10);
      }
    }
  },
  methods: {
    /** 获取所有标签列表 */
    getTagsList() {
      getTopTags().then(res => {
        this.allUserTags = res.data || [];
      });
    },

    /** 查询文档列表 */
    getList() {
      this.loading = true;
      listDocument(this.queryParams).then(response => {
        this.documentList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        documentId: undefined,
        documentName: undefined,
        filePath: undefined,
        fileOriginName: undefined,
        fileSuffix: undefined,
        remark: undefined,
        tags: [] // 重置标签
      };
      this.fileList = [];
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.documentId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "上传文档";

      // 打开弹窗时，获取最新的常用标签
      this.showAllTags = false;
      this.getTagsList();
      // 调用我们在 API 里新增的接口
      getTopTags().then(res => {
        // 假设后端返回 ["TagA", "TagB", ...]
        this.allUserTags = res.data || [];
      });
    },
    /** 点击快捷标签 */
    handleQuickAddTag(tag) {
      if (!this.form.tags) {
        this.$set(this.form, 'tags', []);
      }
      // 如果已存在则移除，不存在则添加
      const index = this.form.tags.indexOf(tag);
      if (index > -1) {
        this.form.tags.splice(index, 1);
      } else {
        this.form.tags.push(tag);
      }
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          addDocument(this.form).then(response => {
            this.$modal.msgSuccess("上传成功");
            this.open = false;
            this.getList();
          });
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const documentIds = row.documentId || this.ids;
      this.$modal.confirm('是否确认删除文档ID为"' + documentIds + '"的数据项？').then(function() {
        return delDocument(documentIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 下载按钮操作 */
    handleDownload(row) {
      this.$download.resource(row.filePath);
    },
    /** OCR识别按钮操作 */
    handleOcr(row) {
      const documentId = row.documentId;
      const loading = this.$loading({
        lock: true,
        text: '正在请求识别服务...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      });

      ocrDocument(documentId).then(response => {
        loading.close();
        this.$modal.msgSuccess("识别成功");
        this.getList(); // 刷新列表查看状态变化
      }).catch(() => {
        loading.close();
      });
    },
    /** 查看详情操作 */
    handleDetail(row) {
      this.reset();
      const documentId = row.documentId;
      getDocument(documentId).then(response => {
        this.detailForm = response.data;
        this.detailOpen = true;
      });
    },
    // 上传前校验
    handleBeforeUpload(file) {
      const isLt50M = file.size / 1024 / 1024 < 50;
      if (!isLt50M) {
        this.$message.error("上传文件大小不能超过 50MB!");
      }
      return isLt50M;
    },
    // 上传成功回调
    handleUploadSuccess(res, file) {
      if (res.code === 200) {
        this.form.filePath = res.fileName;
        this.form.fileOriginName = res.originalFilename;
        const name = res.originalFilename;
        this.form.fileSuffix = name.substring(name.lastIndexOf(".") + 1);
        if (!this.form.documentName) {
          this.form.documentName = name.substring(0, name.lastIndexOf("."));
        }
        this.$modal.msgSuccess("文件上传成功");
      } else {
        this.$modal.msgError("文件上传失败");
        this.$refs.upload.clearFiles();
      }
    },
    // 移除文件
    handleRemove(file, fileList) {
      this.form.filePath = undefined;
      this.form.fileOriginName = undefined;
      this.form.fileSuffix = undefined;
    }
  }
};
</script>

<style scoped>
/* 标签推荐区样式 */
.tag-recommend-area {
  margin-top: 8px;
  line-height: 24px;
}
.tag-title {
  font-size: 12px;
  color: #909399;
  margin-right: 8px;
}
.recommend-tag {
  margin-right: 8px;
  margin-bottom: 5px;
  cursor: pointer;
  user-select: none;
}
.recommend-tag:hover {
  opacity: 0.8;
}
/* 调整按钮样式 */
.el-button--text {
    padding-bottom: 0;
    padding-top: 0;
}
</style>
