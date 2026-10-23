<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html><html><head><meta charset="UTF-8"><title>403 — MediTrack</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"></head>
<body style="display:flex;align-items:center;justify-content:center;min-height:100vh;background:var(--bg);">
<div style="text-align:center;max-width:400px;padding:40px;">
<div style="font-size:3rem;margin-bottom:16px;">&#x1F512;</div>
<h1 style="margin-bottom:8px;">Access Denied</h1>
<p style="color:var(--ink-3);margin-bottom:24px;">Your account role doesn't have access to this page.</p>
<a href="${pageContext.request.contextPath}/login.jsp" class="btn btn-primary">Back to Sign In</a>
</div></body></html>
