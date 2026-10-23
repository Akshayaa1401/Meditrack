<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html><html><head><meta charset="UTF-8"><title>500 — MediTrack</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"></head>
<body style="display:flex;align-items:center;justify-content:center;min-height:100vh;background:var(--bg);">
<div style="text-align:center;max-width:400px;padding:40px;">
<div style="font-size:3rem;margin-bottom:16px;">&#x26A0;</div>
<h1 style="margin-bottom:8px;">Server Error</h1>
<p style="color:var(--ink-3);margin-bottom:24px;">Something went wrong. Please try again or contact your administrator.</p>
<a href="${pageContext.request.contextPath}/index.jsp" class="btn btn-primary">Go Home</a>
</div></body></html>
