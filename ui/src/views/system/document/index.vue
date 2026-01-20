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
          <el-table-column label="文档名称" align="center" prop="documentName" :show-overflow-tooltip="true" />
          <el-table-column label="原始文件名" align="center" prop="fileOriginName" :show-overflow-tooltip="true" />
          <el-table-column label="上传人" align="center" prop="createBy" />
          <el-table-column label="上传时间" align="center" prop="createTime" width="180">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
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
            accept=".pdf"
            :action="uploadUrl"
            :headers="headers"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
            :before-upload="handleBeforeUpload"
            :file-list="fileList"
            drag
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将PDF文件拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip" slot="tip">只能上传pdf文件，且不超过50MB</div>
          </el-upload>
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
  </div>
</template>

<script>
import { listDocument, addDocument, delDocument } from "@/api/system/document";
import { getToken } from "@/utils/auth";

export default {
  name: "Document",
  //dicts: ['sys_normal_disable'],
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
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        documentName: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        documentName: [
          { required: true, message: "文档名称不能为空", trigger: "blur" }
        ],
        filePath: [
          { required: true, message: "请上传文件", trigger: "blur" }
        ]
      },
      // 上传参数   oss上传改为：/common/upload/oss
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      headers: {
        Authorization: "Bearer " + getToken()
      },
      fileList: []
    };
  },
  created() {
    this.getList();
  },
  methods: {
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
        remark: undefined
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
    // /** 下载按钮操作 */
    // handleDownload(row) {
    //   this.download(row.filePath, row.documentName + "." + row.fileSuffix);
    // },
    /** 下载按钮操作 */
    handleDownload(row) {
      // 使用 $download.resource (GET请求) 匹配后端接口
      this.$download.resource(row.filePath);
    },
    // 上传前校验
    handleBeforeUpload(file) {
      const isPDF = file.type === "application/pdf";
      const isLt50M = file.size / 1024 / 1024 < 50;

      if (!isPDF) {
        this.$message.error("上传文档只能是 PDF 格式!");
      }
      if (!isLt50M) {
        this.$message.error("上传文档大小不能超过 50MB!");
      }
      return isPDF && isLt50M;
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
