<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!sidebar.hide" class="sidebar-container"/>
    <div :class="{hasTagsView:needTagsView,sidebarHide:sidebar.hide}" class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar @setLayout="setLayout"/>
        <tags-view v-if="needTagsView"/>
      </div>
      <app-main/>
      <settings ref="settingRef"/>
    </div>
  </div>
</template>

<script>
import { AppMain, Navbar, Settings, Sidebar, TagsView } from './components'
import ResizeMixin from './mixin/ResizeHandler'
import { mapState } from 'vuex'
import variables from '@/assets/styles/variables.scss'
import { getPendingApprovalCount } from '@/api/system/user'

export default {
  name: 'Layout',
  components: {
    AppMain,
    Navbar,
    Settings,
    Sidebar,
    TagsView
  },
  mixins: [ResizeMixin],
  computed: {
    ...mapState({
      theme: state => state.settings.theme,
      sideTheme: state => state.settings.sideTheme,
      sidebar: state => state.app.sidebar,
      device: state => state.app.device,
      needTagsView: state => state.settings.tagsView,
      fixedHeader: state => state.settings.fixedHeader
    }),
    classObj() {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    },
    variables() {
      return variables
    }
  },
  created() {
    this.notifyPendingRegistrations()
  },
  methods: {
    notifyPendingRegistrations() {
      const roles = this.$store.getters.roles || []
      if (!roles.includes('admin') && !roles.includes('manager')) {
        return
      }
      const reminderKey = 'pendingApprovalReminderShown'
      if (sessionStorage.getItem(reminderKey)) {
        return
      }
      sessionStorage.setItem(reminderKey, '1')
      getPendingApprovalCount().then(response => {
        const count = Number(response.data || 0)
        if (count < 1) {
          return
        }
        if (roles.includes('admin')) {
          this.$confirm(`当前有 ${count} 个用户注册申请待审批，是否现在处理？`, '待审批提醒', {
            confirmButtonText: '去审批',
            cancelButtonText: '稍后处理',
            type: 'warning'
          }).then(() => {
            this.$router.push({ path: '/users', query: { approvalStatus: '0' } })
          }).catch(() => {})
        } else {
          this.$alert(`当前有 ${count} 个用户注册申请待审批，请提醒超级管理员及时处理。`, '待审批提醒', {
            confirmButtonText: '知道了',
            type: 'warning'
          }).catch(() => {})
        }
      }).catch(() => {})
    },
    handleClickOutside() {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    },
    setLayout() {
      this.$refs.settingRef.openSetting()
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "~@/assets/styles/mixin.scss";
  @import "~@/assets/styles/variables.scss";

  .app-wrapper {
    @include clearfix;
    position: relative;
    height: 100%;
    width: 100%;

    &.mobile.openSidebar {
      position: fixed;
      top: 0;
    }
  }

  .main-container:has(.fixed-header) {
    height: 100vh;
    overflow: hidden;
  }

  .drawer-bg {
    background: #000;
    opacity: 0.3;
    width: 100%;
    top: 0;
    height: 100%;
    position: absolute;
    z-index: 999;
  }

  .fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 9;
    width: calc(100% - #{$base-sidebar-width});
    transition: width 0.28s;
  }

  .hideSidebar .fixed-header {
    width: calc(100% - 54px);
  }

  .sidebarHide .fixed-header {
    width: 100%;
  }

  .mobile .fixed-header {
    width: 100%;
  }
</style>
