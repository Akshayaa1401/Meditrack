package com.meditrack.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * AuthFilter
 * -----------
 * Centralized role-based access control (RBAC).
 *  - Unauthenticated users hitting a protected path are bounced to /login.jsp
 *  - Authenticated users hitting a path that doesn't match their role get a 403
 *
 * URL conventions enforced:
 *   /admin/*      -> requires role ADMIN
 *   /hospital/*   -> requires role HOSPITAL_STAFF
 *   /collector/*  -> requires role COLLECTOR
 *
 * Public (always allowed) paths: /login.jsp, /register.jsp, /login, /register,
 * /css/*, /js/*, /images/*, /index.jsp
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (isPublic(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        String role = (session != null) ? (String) session.getAttribute("role") : null;

        if (role == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=auth_required");
            return;
        }

        if (path.startsWith("/admin") && !"ADMIN".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only");
            return;
        }
        if (path.startsWith("/hospital") && !"HOSPITAL_STAFF".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Hospital staff only");
            return;
        }
        if (path.startsWith("/collector") && !"COLLECTOR".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Collectors only");
            return;
        }

        chain.doFilter(req, res);
    }

    private boolean isPublic(String path) {
        return path.equals("/") || path.equals("/index.jsp")
                || path.equals("/login.jsp") || path.equals("/register.jsp")
                || path.equals("/login") || path.equals("/register")
                || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/");
    }

    @Override
    public void destroy() {}
}
