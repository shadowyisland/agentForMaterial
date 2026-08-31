<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}">
    <transition name="sidebarLogoFade">
      <router-link :key="collapse ? 'collapse' : 'expand'" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
      </router-link>
    </transition>
  </div>
</template>

<script>
import logoImg from '@/assets/logo/logo.png'
import variables from '@/assets/styles/variables.scss'

export default {
  name: 'SidebarLogo',
  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  },
  computed: {
    variables() {
      return variables
    },
    sideTheme() {
      return this.$store.state.settings.sideTheme
    }
  },
  data() {
    return {
      logo: logoImg
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 78px;
  line-height: 78px;
  background: linear-gradient(180deg, #12395e 0%, #0b2440 100%);
  text-align: center;
  overflow: hidden;

  & .sidebar-logo-link {
    width: calc(100% - 30px) !important;
    height: 52px;
    margin: 9px 15px 0;
    display: flex !important;
    align-items: center;
    justify-content: center;
    border: 1px solid rgba(74, 144, 205, .16);
    border-radius: 12px;
    background: rgba(255, 255, 255, .98);
    box-shadow: 0 8px 20px rgba(0, 13, 31, .2);

    & .sidebar-logo {
      width: 146px;
      max-width: none;
      height: auto;
      object-fit: contain;
      transition: width .25s ease;
    }
  }

  &.collapse {
    .sidebar-logo-link {
      width: 38px !important;
      height: 46px;
      margin: 9px auto 0;
      justify-content: flex-start;
      overflow: hidden;
    }
    .sidebar-logo {
      flex: 0 0 146px;
      width: 146px;
      max-width: none;
    }
  }
}
</style>
