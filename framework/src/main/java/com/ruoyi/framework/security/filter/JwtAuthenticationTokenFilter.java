package com.ruoyi.framework.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;


import com.ruoyi.common.core.domain.entity.SysUser;
import java.util.HashSet;
import java.util.Collections;
/**
 * token过滤器 验证token有效性
 * 
 * @author ruoyi
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter
{
    @Autowired
    private TokenService tokenService;

//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
//            throws ServletException, IOException
//    {
//        LoginUser loginUser = tokenService.getLoginUser(request);
//        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication()))
//        {
//            tokenService.verifyToken(loginUser);
//            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
//            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
//        }
//        chain.doFilter(request, response);
//    }
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException
{
    LoginUser loginUser = tokenService.getLoginUser(request);

    // ================== 上帝模式开始 ==================
    // 如果没有Token，手动伪造一个超级管理员身份
    if (loginUser == null) {
        SysUser user = new SysUser();
        user.setUserId(1L);        // 1号通常是超级管理员
        user.setUserName("admin");
        user.setNickName("上帝模式");
        user.setDeptId(100L);      // 设置一个存在的部门ID

        // 赋予所有权限 (*:*:*)
        HashSet<String> perms = new HashSet<>();
        perms.add("*:*:*");

        loginUser = new LoginUser(user, perms);
    }
    // ================== 上帝模式结束 ==================

    if (StringUtils.isNotNull(loginUser) && SecurityUtils.getAuthentication() == null)
    {
        // 这里的 tokenService.verifyToken 删除或注释掉，因为伪造的用户没有真实Token
        // tokenService.verifyToken(loginUser);

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    chain.doFilter(request, response);
}
}
