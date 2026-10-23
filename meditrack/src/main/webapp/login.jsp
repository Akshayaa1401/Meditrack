<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Sign In — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-shell">
  <div class="auth-sidebar">
    <div class="auth-brand">
      <div class="auth-brand-logo">
        <div class="auth-brand-mark">M</div>
        <div>
          <div class="auth-brand-name">MediTrack</div>
          <div class="auth-brand-sub">Biomedical Waste Management</div>
        </div>
      </div>
      <h2>Smart waste. Safe cities.</h2>
      <p>A real-time biomedical waste collection and monitoring platform for hospitals, collectors, and administrators.</p>
    </div>
    <div style="margin-top:32px;">
      <div class="auth-feature">
        <div class="auth-feature-icon">&#x1F3E5;</div>
        <div class="auth-feature-text">Hospitals report waste with one click. Risk and priority are auto-assigned.</div>
      </div>
      <div class="auth-feature">
        <div class="auth-feature-icon">&#x1F69A;</div>
        <div class="auth-feature-text">Collectors get an optimized route, sorted by urgency and deadline.</div>
      </div>
      <div class="auth-feature">
        <div class="auth-feature-icon">&#x1F4CD;</div>
        <div class="auth-feature-text">Hospitals track their collector live until waste is safely disposed.</div>
      </div>
    </div>
  </div>
  <div class="auth-form-area">
    <div class="auth-card">
      <h1>Welcome back</h1>
      <p class="subtitle">Sign in to your MediTrack account</p>

      <c:if test="${not empty error}">
        <div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>${error}</div>
      </c:if>
      <c:if test="${param.registered == 'true'}">
        <div class="alert alert-success"><span class="alert-icon">&#x2713;</span>Account created. Please sign in.</div>
      </c:if>
      <c:if test="${param.loggedout == 'true'}">
        <div class="alert alert-info"><span class="alert-icon">&#x2139;</span>You have been signed out.</div>
      </c:if>
      <c:if test="${param.error == 'auth_required'}">
        <div class="alert alert-warning"><span class="alert-icon">&#x1F512;</span>Please sign in to continue.</div>
      </c:if>

      <form method="post" action="${pageContext.request.contextPath}/login">
        <div class="form-group">
          <label class="form-label" for="email">Email address</label>
          <input class="form-control" type="email" id="email" name="email" required placeholder="you@hospital.org" autocomplete="email">
        </div>
        <div class="form-group">
          <label class="form-label" for="password">Password</label>
          <input class="form-control" type="password" id="password" name="password" required placeholder="••••••••" autocomplete="current-password">
        </div>
        <button type="submit" class="btn btn-primary btn-block btn-lg">Sign in</button>
      </form>
      <p style="margin-top:20px;text-align:center;font-size:0.85rem;color:var(--ink-3);">
        No account? <a href="${pageContext.request.contextPath}/register.jsp">Create one here</a>
      </p>
    </div>
  </div>
</div>
</body>
</html>
