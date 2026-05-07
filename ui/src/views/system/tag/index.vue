<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="标签名" prop="tagName">
        <el-input
          v-model="queryParams.tagName"
          placeholder="请输入标签名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="tagList">
      <el-table-column label="标签ID" align="center" prop="tagId" width="90" />
      <el-table-column label="标签名" align="center" prop="tagName" :show-overflow-tooltip="true" />
      <el-table-column label="关联文档数量" align="center" prop="documentCount" width="130" />
      <el-table-column label="关联文档名" align="center" min-width="260">
        <template slot-scope="scope">
          <el-tooltip placement="top" effect="dark" :disabled="!hasDocuments(scope.row)">
            <div slot="content">
              <div v-for="(name, index) in scope.row.documentNames" :key="index">{{ name }}</div>
            </div>
            <div class="document-names">
              <template v-if="hasDocuments(scope.row)">
                <div
                  v-for="(name, index) in visibleDocumentNames(scope.row)"
                  :key="index"
                  class="document-name"
                >{{ name }}</div>
              </template>
              <span v-else>-</span>
            </div>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="createBy" width="120" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            active-text="开启"
            inactive-text="关闭"
            @change="handleStatusChange(scope.row)"
          />
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
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
  </div>
</template>

<script>
import { listTag, delTag, changeTagStatus } from '@/api/system/tag'

export default {
  name: 'Tag',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      tagList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        tagName: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listTag(this.queryParams).then(response => {
        this.tagList = response.rows || []
        this.total = response.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    hasDocuments(row) {
      return row.documentNames && row.documentNames.length > 0
    },
    visibleDocumentNames(row) {
      return this.hasDocuments(row) ? row.documentNames.slice(0, 3) : []
    },
    refreshRoutes() {
      this.$store.dispatch('GenerateRoutes', true)
    },
    handleStatusChange(row) {
      changeTagStatus(row.tagId, row.status).then(() => {
        this.$modal.msgSuccess(row.status === '0' ? '已开启' : '已关闭')
        this.refreshRoutes()
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除标签"' + row.tagName + '"？').then(() => {
        return delTag(row.tagId)
      }).then(() => {
        this.getList()
        this.refreshRoutes()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.document-names {
  line-height: 22px;
}

.document-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
