<template>
  <div class="login">
    <div class="login-shell">
      <section class="brand-panel">
        <div class="brand-logo-plate"><img :src="logo" alt="材料智能体 Logo" /></div>
      </section>
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form" autocomplete="off">
      <img :src="logo" alt="材料智能体 Logo" class="mobile-logo" />
      <h3 class="title">登录{{title}}</h3>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          name="loginAccount"
          autocomplete="off"
          :readonly="accountReadonly"
          placeholder="账号"
          @focus="accountReadonly = false"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          name="loginPassword"
          autocomplete="new-password"
          :readonly="passwordReadonly"
          placeholder="密码"
          @focus="passwordReadonly = false"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
      </el-form>
    </div>
  </div>


</template>

<script>
// import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'
import logoImg from '@/assets/logo/logo.png'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      logo: logoImg,
      codeUrl: "",
      loginForm: {
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ]
        // 验证码校验已取消，如需恢复取消下一行注释
        // ,code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      accountReadonly: true,
      passwordReadonly: true,
      // 验证码开关
      captchaEnabled: false,
      // 注册开关
      register: true,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    // 登录验证码已取消，如需恢复取消下一行注释
    // this.getCode()
    this.getCookie()
  },
  methods: {
    // 登录验证码已取消，保留原方法方便后续恢复
    // getCode() {
    //   getCodeImg().then(res => {
    //     this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
    //     if (this.captchaEnabled) {
    //       this.codeUrl = "data:image/gif;base64," + res.img
    //       this.loginForm.uuid = res.uuid
    //     }
    //   })
    // },
    getCookie() {
      const rememberMe = Cookies.get('rememberMe') === 'true'
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      if (!rememberMe || !username || !password) {
        this.clearRememberedLogin()
        return
      }
      try {
        this.loginForm.username = username
        this.loginForm.password = decrypt(password)
        this.loginForm.rememberMe = true
      } catch (e) {
        this.clearRememberedLogin()
      }
    },
    clearRememberedLogin() {
      this.loginForm.username = ""
      this.loginForm.password = ""
      this.loginForm.rememberMe = false
      this.removeRememberedCookies()
    },
    removeRememberedCookies() {
      Cookies.remove("username")
      Cookies.remove("password")
      Cookies.remove('rememberMe')
    },
    saveRememberedLogin() {
      if (this.loginForm.rememberMe) {
        Cookies.set("username", this.loginForm.username, { expires: 30 })
        Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
        Cookies.set('rememberMe', 'true', { expires: 30 })
      } else {
        this.removeRememberedCookies()
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.saveRememberedLogin()
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{})
          }).catch(() => {
            this.loading = false
            // 登录验证码已取消，如需恢复取消下面三行注释
            // if (this.captchaEnabled) {
            //   this.getCode()
            // }
          })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
  background-position: center;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(120deg, rgba(3, 22, 43, .88), rgba(6, 69, 124, .68));
  }
}
.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(360px, 1fr);
  width: min(880px, calc(100vw - 48px));
  min-height: 500px;
  border: 1px solid rgba(255, 255, 255, .22);
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, .96);
  box-shadow: 0 28px 70px rgba(0, 18, 38, .35);
}
.brand-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 15% 15%, rgba(79, 198, 245, .35), transparent 30%),
    radial-gradient(circle at 88% 82%, rgba(44, 141, 233, .28), transparent 34%),
    linear-gradient(145deg, #072b51, #0b5fa9 68%, #168fbd);

  &::before,
  &::after {
    content: '';
    position: absolute;
    border: 1px solid rgba(255, 255, 255, .13);
    border-radius: 50%;
  }

  &::before { width: 330px; height: 330px; top: -160px; left: -120px; }
  &::after { width: 260px; height: 260px; right: -120px; bottom: -130px; }
}
.brand-logo-plate {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(330px, calc(100% - 72px));
  min-height: 84px;
  padding: 18px 24px;
  border: 1px solid rgba(255,255,255,.65);
  border-radius: 18px;
  background: rgba(255, 255, 255, .97);
  box-shadow: 0 22px 48px rgba(0, 19, 42, .26);

  img { width: 100%; height: auto; }
}
.title {
  margin: 0 0 34px;
  color: #17324d;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: .3px;
}

.login-form {
  align-self: center;
  background: #ffffff;
  width: 100%;
  padding: 58px 58px 46px;
  z-index: 1;
  .el-input {
    height: 38px;
    input {
      height: 38px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
}
.login-form ::v-deep .el-input__inner {
  height: 44px;
  border-color: #d7e4f0;
  border-radius: 8px;
}
.login-form ::v-deep .el-button--primary {
  height: 44px;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(90deg, #116bc5, #178fd3);
  box-shadow: 0 9px 18px rgba(19, 111, 194, .2);
}
.mobile-logo { display: none; width: 230px; height: auto; margin-bottom: 26px; }
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 38px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 38px;
}
@media (max-width: 820px) {
  .login-shell { display: block; width: min(430px, calc(100vw - 28px)); min-height: 0; }
  .brand-panel { display: none; }
  .login-form { padding: 42px 34px 32px; }
  .mobile-logo { display: block; }
}
@media (max-width: 420px) {
  .login-form { padding: 34px 24px 26px; }
  .mobile-logo { width: 210px; }
}
</style>
