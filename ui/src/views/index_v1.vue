<template>
  <div class="app-container home">
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-header">
        <div class="avatar-box">
          <img :src="avatar" class="user-avatar">
        </div>
        <div class="info-box">
          <div class="title">{{ timeFix }}，{{ name }}，祝你工作顺利！</div>
          <div class="desc">
            <span v-if="roles && roles.length > 0">所属角色：{{ roles.join(' / ') }}</span>
            <span class="divider" v-if="roles && roles.length > 0">|</span>
            <span>上次登录时间：2025-05-29 09:12:33</span> </div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <el-card shadow="hover" class="box-card mb-20">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-user"></i> 我的信息</span>
            <el-button style="float: right; padding: 3px 0" type="text" @click="$router.push('/user/profile')">
              修改资料
            </el-button>
          </div>
          <div class="user-detail">
            <div class="detail-item">
              <span class="label"><i class="el-icon-office-building"></i> 部门：</span>
              <span class="text">研发部 / 技术组</span> </div>
            <div class="detail-item">
              <span class="label"><i class="el-icon-phone-outline"></i> 电话：</span>
              <span class="text">{{ user.phonenumber || '暂未绑定' }}</span>
            </div>
            <div class="detail-item">
              <span class="label"><i class="el-icon-message"></i> 邮箱：</span>
              <span class="text">{{ user.email || '暂未绑定' }}</span>
            </div>
            <div class="detail-item">
              <span class="label"><i class="el-icon-date"></i> 加入时间：</span>
              <span class="text">{{ user.createTime || '2023-01-01' }}</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="box-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-menu"></i> 快捷操作</span>
          </div>
          <div class="shortcut-box">
            <div class="shortcut-item" @click="$router.push('/user/profile')">
              <div class="icon-wrap bg-blue"><i class="el-icon-user-solid"></i></div>
              <span>个人中心</span>
            </div>
            <div class="shortcut-item" @click="$router.push('/user/profile?tab=resetPwd')">
              <div class="icon-wrap bg-green"><i class="el-icon-key"></i></div>
              <span>修改密码</span>
            </div>
            <div class="shortcut-item" @click="contactAdmin">
              <div class="icon-wrap bg-orange"><i class="el-icon-service"></i></div>
              <span>联系管理</span>
            </div>
            <div class="shortcut-item" @click="openCalc">
              <div class="icon-wrap bg-purple"><i class="el-icon-s-cooperation"></i></div>
              <span>办公工具</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <el-card shadow="hover" class="box-card mb-20" style="min-height: 420px;">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-bell"></i> 通知公告</span>
            <el-tag size="small" type="danger" style="float: right;">NEW</el-tag>
          </div>
          <div class="notice-list">
            <div class="notice-item" v-for="i in 4" :key="i">
              <div class="notice-icon"><i class="el-icon-message-solid"></i></div>
              <div class="notice-content">
                <div class="n-title">关于五一劳动节放假的通知文件 (2025年)</div>
                <div class="n-time">2025-04-28 10:00</div>
              </div>
              <el-button size="mini" plain round>查看</el-button>
            </div>
            <div class="notice-item">
              <div class="notice-icon info"><i class="el-icon-info"></i></div>
              <div class="notice-content">
                <div class="n-title">系统将于本周六凌晨进行例行维护，请知悉。</div>
                <div class="n-time">2025-05-20 16:30</div>
              </div>
              <el-button size="mini" plain round>查看</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'

export default {
  name: "Index",
  computed: {
    ...mapGetters([
      'avatar',
      'name',
      'roles'
    ]),
    // 获取更详细的用户信息
    ...mapState({
      user: state => state.user
    }),
    timeFix() {
      const time = new Date()
      const hour = time.getHours()
      return hour < 9 ? '早上好' : hour <= 11 ? '上午好' : hour <= 13 ? '中午好' : hour < 20 ? '下午好' : '晚上好'
    }
  },
  methods: {
    contactAdmin() {
      this.$msgbox.alert('管理员联系电话：010-88888888', '联系支持')
    },
    openCalc() {
      this.$message.success('打开办公工具箱...')
    }
  }
}
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
  background-color: #f5f7f9;
  min-height: calc(100vh - 84px);
}

.mb-20 {
  margin-bottom: 20px;
}

// 顶部欢迎
.welcome-card {
  margin-bottom: 20px;
  border: none;
  background: #fff;
  .welcome-header {
    display: flex;
    align-items: center;
    padding: 10px;
  }
  .avatar-box {
    margin-right: 20px;
    .user-avatar {
      width: 60px;
      height: 60px;
      border-radius: 50%;
    }
  }
  .info-box {
    .title {
      font-size: 20px;
      font-weight: 600;
      color: #333;
      margin-bottom: 8px;
    }
    .desc {
      font-size: 14px;
      color: #999;
      .divider {
        margin: 0 10px;
        color: #ddd;
      }
    }
  }
}

// 个人信息列表
.user-detail {
  padding: 10px 0;
  .detail-item {
    display: flex;
    margin-bottom: 18px;
    font-size: 14px;
    &:last-child { margin-bottom: 0; }

    .label {
      color: #666;
      width: 100px;
      i { margin-right: 5px; }
    }
    .text {
      color: #333;
      font-weight: 500;
      flex: 1;
    }
  }
}

// 快捷操作网格
.shortcut-box {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;

  .shortcut-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    transition: transform 0.2s;

    &:hover {
      transform: translateY(-3px);
    }

    .icon-wrap {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: #fff;
      margin-bottom: 8px;

      &.bg-blue { background: linear-gradient(135deg, #36d1dc, #5b86e5); }
      &.bg-green { background: linear-gradient(135deg, #11998e, #38ef7d); }
      &.bg-orange { background: linear-gradient(135deg, #fce38a, #f38181); }
      &.bg-purple { background: linear-gradient(135deg, #a8c0ff, #3f2b96); }
    }

    span {
      font-size: 13px;
      color: #666;
    }
  }
}

// 公告列表
.notice-list {
  .notice-item {
    display: flex;
    align-items: center;
    padding: 15px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child { border-bottom: none; }

    .notice-icon {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #e8f3ff;
      color: #1890ff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      margin-right: 15px;

      &.info {
        background: #fff7e6;
        color: #fa8c16;
      }
    }

    .notice-content {
      flex: 1;
      .n-title {
        font-size: 14px;
        color: #333;
        margin-bottom: 4px;
        &:hover { color: #1890ff; cursor: pointer; }
      }
      .n-time {
        font-size: 12px;
        color: #999;
      }
    }
  }
}
</style>
