// import router from './router'
// import store from './store'
// import { Message } from 'element-ui'
// import NProgress from 'nprogress'
// import 'nprogress/nprogress.css'
// import { getToken } from '@/utils/auth'
// import { isPathMatch } from '@/utils/validate'
// import { isRelogin } from '@/utils/request'
//
// NProgress.configure({ showSpinner: false })
//
// const whiteList = ['/login', '/register']
//
// const isWhiteList = (path) => {
//   return whiteList.some(pattern => isPathMatch(pattern, path))
// }
//
// router.beforeEach((to, from, next) => {
//   NProgress.start()
//   if (getToken()) {
//     to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
//     /* has token*/
//     if (to.path === '/login') {
//       next({ path: '/' })
//       NProgress.done()
//     } else if (isWhiteList(to.path)) {
//       next()
//     } else {
//       if (store.getters.roles.length === 0) {
//         isRelogin.show = true
//         // 判断当前用户是否已拉取完user_info信息
//         store.dispatch('GetInfo').then(() => {
//           isRelogin.show = false
//           store.dispatch('GenerateRoutes').then(accessRoutes => {
//             // 根据roles权限生成可访问的路由表
//             router.addRoutes(accessRoutes) // 动态添加可访问路由表
//             next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
//           })
//         }).catch(err => {
//             store.dispatch('LogOut').then(() => {
//               Message.error(err)
//               next({ path: '/' })
//             })
//           })
//       } else {
//         next()
//       }
//     }
//   } else {
//     // 没有token
//     if (isWhiteList(to.path)) {
//       // 在免登录白名单，直接进入
//       next()
//     } else {
//       next(`/login?redirect=${encodeURIComponent(to.fullPath)}`) // 否则全部重定向到登录页
//       NProgress.done()
//     }
//   }
// })
//
// router.afterEach(() => {
//   NProgress.done()
// })

// import router from './router'
// import NProgress from 'nprogress'
// import 'nprogress/nprogress.css'

// NProgress.configure({ showSpinner: false })
//
// // 无论去哪里，都直接放行，不查Token，不查后端
// router.beforeEach((to, from, next) => {
//   NProgress.start()
//   next()
// })
//
// router.afterEach(() => {
//   NProgress.done()
// })


import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      next()
    } else {
      // 判断当前用户是否已拉取完user_info信息
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        // 1. 获取用户信息
        store.dispatch('GetInfo').then(() => {
          isRelogin.show = false
          // 2. 向后端请求路由数据（这一步会加载出您的“文档管理”菜单）
          store.dispatch('GenerateRoutes').then(accessRoutes => {
            // 根据roles权限生成可访问的路由表
            router.addRoutes(accessRoutes) // 动态添加可访问路由表
            next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
          })
        }).catch(err => {
          store.dispatch('LogOut').then(() => {
            Message.error(err)
            next({ path: '/' })
          })
        })
      } else {
        next()
      }
    }
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`) // 否则全部重定向到登录页
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
