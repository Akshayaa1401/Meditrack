<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<%
    if (session != null && session.getAttribute("role") != null) {
        String role = (String) session.getAttribute("role");
        if ("ADMIN".equals(role)) { response.sendRedirect(request.getContextPath() + "/admin/dashboard"); return; }
        if ("HOSPITAL_STAFF".equals(role)) { response.sendRedirect(request.getContextPath() + "/hospital/dashboard"); return; }
        if ("COLLECTOR".equals(role)) { response.sendRedirect(request.getContextPath() + "/collector/dashboard"); return; }
    }
    response.sendRedirect(request.getContextPath() + "/login.jsp");
%>
