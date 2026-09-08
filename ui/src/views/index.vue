<template>
  <div v-loading="loading" class="app-container home">
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-header">
        <img :src="avatar" class="user-avatar" alt="用户头像">
        <div class="welcome-copy">
          <div class="welcome-title">{{ timeFix }}，{{ displayName }}，祝你工作顺利！</div>
          <div class="welcome-meta">
            <span>账号：{{ profile.userName || name }}</span>
            <span>角色：{{ roleText }}</span>
            <span>上次登录：{{ lastLoginText }}</span>
          </div>
        </div>
        <el-button type="primary" plain icon="el-icon-user" @click="openProfile">个人中心</el-button>
      </div>
    </el-card>

    <el-alert
      v-if="pendingApprovalCount > 0"
      class="approval-alert"
      type="warning"
      :closable="false"
      show-icon
    >
      <template slot="title">
        当前有 {{ pendingApprovalCount }} 个用户注册申请待审批
        <el-button v-if="isAdmin" type="text" class="alert-action" @click="openPendingUsers">立即处理</el-button>
        <span v-else>，请提醒超级管理员及时处理。</span>
      </template>
    </el-alert>

    <el-row :gutter="18" class="stats-row">
      <el-col v-for="item in statCards" :key="item.key" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div :class="['stat-icon', item.color]"><i :class="item.icon" /></div>
          <div>
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18">
      <el-col :xs="24" :sm="24" :md="8">
        <el-card shadow="never" class="panel-card profile-card">
          <div slot="header" class="card-header">
            <span><i class="el-icon-user" /> 我的信息</span>
            <el-button type="text" @click="openProfile">修改资料</el-button>
          </div>
          <div class="profile-name">{{ profile.nickName || '未填写姓名' }}</div>
          <div class="profile-account">账号：{{ profile.userName || name }}</div>
          <div class="detail-list">
            <div class="detail-item">
              <i class="el-icon-office-building" />
              <span class="label">部门</span>
              <span class="value">{{ departmentText }}</span>
            </div>
            <div class="detail-item">
              <i class="el-icon-s-custom" />
              <span class="label">岗位</span>
              <span class="value">{{ postGroup || '暂未分配' }}</span>
            </div>
            <div class="detail-item">
              <i class="el-icon-phone-outline" />
              <span class="label">手机号</span>
              <span class="value">{{ profile.phonenumber || '暂未绑定' }}</span>
            </div>
            <div class="detail-item">
              <i class="el-icon-message" />
              <span class="label">邮箱</span>
              <span class="value">{{ profile.email || '暂未绑定' }}</span>
            </div>
            <div class="detail-item">
              <i class="el-icon-date" />
              <span class="label">加入时间</span>
              <span class="value">{{ profile.createTime ? parseTime(profile.createTime) : '-' }}</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card shortcut-card">
          <div slot="header" class="card-header"><span><i class="el-icon-guide" /> 常用功能</span></div>
          <div class="shortcut-grid">
            <button v-if="canAddDocument" class="shortcut-item" type="button" @click="openDocumentUpload">
              <span class="shortcut-icon blue"><i class="el-icon-upload2" /></span>
              <span>上传文档</span>
            </button>
            <button v-if="canSearchDocument" class="shortcut-item" type="button" @click="openDocumentSearch">
              <span class="shortcut-icon cyan"><i class="el-icon-search" /></span>
              <span>文档检索</span>
            </button>
            <button class="shortcut-item" type="button" @click="openProfile">
              <span class="shortcut-icon green"><i class="el-icon-user" /></span>
              <span>个人中心</span>
            </button>
            <button v-if="isManager" class="shortcut-item" type="button" @click="openUsers">
              <span class="shortcut-icon purple"><i class="el-icon-s-custom" /></span>
              <span>用户管理</span>
            </button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="16">
        <el-card shadow="never" class="panel-card recent-card">
          <div slot="header" class="card-header">
            <span><i class="el-icon-document" /> 最近上传的文档</span>
            <el-button v-if="canListDocument" type="text" @click="openDocumentSearch">查看全部</el-button>
          </div>
          <el-table v-if="recentDocuments.length" :data="recentDocuments" class="recent-table">
            <el-table-column label="文档名称" min-width="145" show-overflow-tooltip>
              <template slot-scope="scope">
                <div class="document-name">{{ scope.row.documentName || scope.row.fileOriginName || '-' }}</div>
                <div class="document-product">{{ scope.row.productName || '未填写产品名称' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="105">
              <template slot-scope="scope">{{ documentCategory(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="资料类型" width="76" align="center">
              <template slot-scope="scope"><el-tag size="mini" effect="plain">{{ scope.row.documentKind || '-' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="识别状态" width="86" align="center">
              <template slot-scope="scope"><el-tag size="mini" :type="ocrType(scope.row)">{{ ocrLabel(scope.row) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="上传时间" width="135">
              <template slot-scope="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</template>
            </el-table-column>
            <el-table-column label="操作" width="55" align="center">
              <template slot-scope="scope"><el-button type="text" @click="openDocument(scope.row)">打开</el-button></template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无文档，上传后会在这里显示">
            <el-button v-if="canAddDocument" type="primary" size="small" @click="openDocumentUpload">上传第一份文档</el-button>
          </el-empty>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getUserProfile, getPendingApprovalCount } from '@/api/system/user'
import { getDocumentStats, listDocument } from '@/api/system/document'

const CATEGORY_PATHS = {
  RAW_MATERIAL: 'raw-material',
  ACRYLIC: 'acrylic',
  EPOXY: 'epoxy',
  OTHER_RESIN: 'other-resin',
  FILLER: 'filler',
  SILICONE: 'silicone',
  ADDITIVE: 'additive'
}

const CATEGORY_LABELS = {
  RAW_MATERIAL: '原材料',
  ACRYLIC: '丙烯酸原材料',
  EPOXY: '环氧原材料',
  OTHER_RESIN: '其它树脂原材料',
  FILLER: '填料类原材料',
  SILICONE: '有机硅原材料',
  ADDITIVE: '助剂类原材料'
}

export default {
  name: 'Index',
  data() {
    return {
      loading: true,
      profile: {},
      roleGroup: '',
      postGroup: '',
      documentStats: { total: 0, recognized: 0, pending: 0, failed: 0 },
      recentDocuments: [],
      pendingApprovalCount: 0
    }
  },
  computed: {
    ...mapGetters(['avatar', 'name', 'nickName', 'roles', 'permissions']),
    displayName() {
      return this.profile.nickName || this.nickName || this.name
    },
    roleText() {
      return this.roleGroup || (this.roles || []).join(' / ') || '暂未分配'
    },
    departmentText() {
      return this.profile.dept && this.profile.dept.deptName ? this.profile.dept.deptName : '暂未分配'
    },
    lastLoginText() {
      return this.profile.loginDate ? this.parseTime(this.profile.loginDate) : '首次登录'
    },
    timeFix() {
      const hour = new Date().getHours()
      return hour < 9 ? '早上好' : hour <= 11 ? '上午好' : hour <= 13 ? '中午好' : hour < 20 ? '下午好' : '晚上好'
    },
    isAdmin() {
      return (this.roles || []).includes('admin')
    },
    isManager() {
      return this.isAdmin || (this.roles || []).includes('manager')
    },
    canListDocument() {
      return this.hasPermission('system:document:list')
    },
    canAddDocument() {
      return this.hasPermission('system:document:add')
    },
    canSearchDocument() {
      return this.hasPermission('system:document:search')
    },
    statCards() {
      return [
        { key: 'total', label: '文档总数', value: this.documentStats.total || 0, icon: 'el-icon-document', color: 'blue' },
        { key: 'recognized', label: '识别完成', value: this.documentStats.recognized || 0, icon: 'el-icon-circle-check', color: 'green' },
        { key: 'pending', label: '等待识别', value: this.documentStats.pending || 0, icon: 'el-icon-time', color: 'orange' },
        { key: 'failed', label: '识别失败', value: this.documentStats.failed || 0, icon: 'el-icon-warning-outline', color: 'red' }
      ]
    }
  },
  created() {
    Promise.all([this.loadProfile(), this.loadDocuments(), this.loadPendingApprovals()]).finally(() => {
      this.loading = false
    })
  },
  methods: {
    hasPermission(code) {
      return (this.permissions || []).includes('*:*:*') || (this.permissions || []).includes(code)
    },
    loadProfile() {
      return getUserProfile().then(response => {
        this.profile = response.data || {}
        this.roleGroup = response.roleGroup || ''
        this.postGroup = response.postGroup || ''
      }).catch(() => {})
    },
    loadDocuments() {
      if (!this.canListDocument) return Promise.resolve()
      return Promise.all([
        getDocumentStats().then(response => { this.documentStats = response.data || this.documentStats }),
        listDocument({ pageNum: 1, pageSize: 5 }).then(response => { this.recentDocuments = response.rows || [] })
      ]).catch(() => {})
    },
    loadPendingApprovals() {
      if (!this.isManager) return Promise.resolve()
      return getPendingApprovalCount().then(response => {
        this.pendingApprovalCount = Number(response.data || 0)
      }).catch(() => {})
    },
    openProfile() {
      this.$router.push('/user/profile')
    },
    openUsers() {
      this.$router.push('/users')
    },
    openPendingUsers() {
      this.$router.push({ path: '/users', query: { approvalStatus: '0' } })
    },
    openDocumentUpload() {
      this.$router.push('/documents/internal/raw-material')
    },
    openDocumentSearch() {
      this.$router.push('/documents/search')
    },
    openDocument(document) {
      const query = { documentId: document.documentId, documentType: document.documentType }
      if (document.materialCategory) query.materialCategory = document.materialCategory
      const path = document.documentType === 'EXTERNAL'
        ? '/documents/external'
        : '/documents/internal/' + (CATEGORY_PATHS[document.materialCategory] || 'raw-material')
      this.$router.push({ path, query })
    },
    documentCategory(document) {
      return document.documentType === 'EXTERNAL' ? '外部文档' : (CATEGORY_LABELS[document.materialCategory] || '内部文档')
    },
    ocrLabel(document) {
      return { 0: '等待识别', 1: '已完成', 2: '识别失败' }[document.isRecognized] || '未知'
    },
    ocrType(document) {
      return { 0: 'warning', 1: 'success', 2: 'danger' }[document.isRecognized] || 'info'
    }
  }
}
</script>

<style scoped lang="scss">
.home { min-height: calc(100vh - 84px); padding: 20px; background: #f4f7fb; }
.welcome-card, .panel-card, .stat-card { border: 0; border-radius: 10px; }
.welcome-card { margin-bottom: 16px; }
.welcome-header { display: flex; align-items: center; min-height: 74px; }
.user-avatar { width: 62px; height: 62px; border-radius: 50%; object-fit: cover; margin-right: 18px; }
.welcome-copy { flex: 1; min-width: 0; }
.welcome-title { margin-bottom: 10px; color: #20354d; font-size: 21px; font-weight: 600; }
.welcome-meta { display: flex; flex-wrap: wrap; gap: 8px 22px; color: #7c8da2; font-size: 13px; }
.approval-alert { margin-bottom: 16px; }
.alert-action { margin-left: 10px; padding: 0; }
.stats-row { margin-bottom: 18px; }
.stat-card ::v-deep .el-card__body { display: flex; align-items: center; padding: 20px; }
.stat-icon { display: flex; align-items: center; justify-content: center; width: 48px; height: 48px; margin-right: 14px; border-radius: 12px; font-size: 23px; }
.stat-icon.blue { color: #1685d8; background: #e8f4ff; }
.stat-icon.green { color: #20a66a; background: #e9f8f1; }
.stat-icon.orange { color: #e89527; background: #fff4e5; }
.stat-icon.red { color: #e35d65; background: #ffedef; }
.stat-value { color: #20354d; font-size: 25px; font-weight: 700; line-height: 1; }
.stat-label { margin-top: 7px; color: #8493a5; font-size: 13px; }
.panel-card { margin-bottom: 18px; }
.card-header { display: flex; align-items: center; justify-content: space-between; color: #273d55; font-weight: 600; }
.card-header i { margin-right: 7px; color: #1685d8; }
.profile-name { color: #20354d; font-size: 19px; font-weight: 600; }
.profile-account { margin: 5px 0 18px; color: #91a0b2; font-size: 13px; }
.detail-item { display: grid; grid-template-columns: 20px 62px 1fr; align-items: center; min-height: 38px; color: #63758a; font-size: 13px; border-bottom: 1px solid #f0f3f7; }
.detail-item:last-child { border-bottom: 0; }
.detail-item i { color: #8ba0b6; }
.detail-item .value { color: #2c4057; text-align: right; overflow-wrap: anywhere; }
.shortcut-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.shortcut-item { display: flex; flex-direction: column; align-items: center; gap: 7px; padding: 8px 2px; color: #556a80; font-size: 12px; border: 0; background: transparent; cursor: pointer; }
.shortcut-icon { display: flex; align-items: center; justify-content: center; width: 42px; height: 42px; color: #fff; border-radius: 11px; font-size: 20px; transition: transform .2s; }
.shortcut-item:hover .shortcut-icon { transform: translateY(-2px); }
.shortcut-icon.blue { background: linear-gradient(135deg, #3b9ee9, #3374d6); }
.shortcut-icon.cyan { background: linear-gradient(135deg, #32c8c1, #1597ae); }
.shortcut-icon.green { background: linear-gradient(135deg, #38bd7b, #149b64); }
.shortcut-icon.purple { background: linear-gradient(135deg, #8c83e6, #6557c7); }
.recent-card { min-height: 455px; }
.recent-table::before { display: none; }
.document-name { color: #2b4159; font-weight: 500; }
.document-product { margin-top: 3px; color: #9aa7b6; font-size: 12px; }

@media (max-width: 991px) {
  .profile-card { margin-top: 0; }
  .welcome-header { align-items: flex-start; }
  .welcome-header > .el-button { display: none; }
  .shortcut-card { margin-bottom: 18px; }
}

@media (max-width: 600px) {
  .home { padding: 12px; }
  .welcome-header { flex-wrap: wrap; }
  .user-avatar { width: 50px; height: 50px; }
  .welcome-title { font-size: 17px; }
  .welcome-meta { display: block; }
  .welcome-meta span { display: block; margin-top: 4px; }
  .stat-card ::v-deep .el-card__body { padding: 14px 10px; }
  .stat-icon { width: 40px; height: 40px; margin-right: 9px; }
  .stat-value { font-size: 21px; }
}
</style>
