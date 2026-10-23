<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Create Account — MediTrack</title>
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
      <h2>Three roles. One platform.</h2>
      <p>Hospital staff report waste. Collectors run optimised routes. Admins monitor everything in real time.</p>
    </div>
    <div style="margin-top:32px;">
      <div class="auth-feature">
        <div class="auth-feature-icon">&#x1F3E5;</div>
        <div class="auth-feature-text"><strong style="color:#fff;">Hospital Staff</strong> — Submit waste, track pickups, see live collector location.</div>
      </div>
      <div class="auth-feature">
        <div class="auth-feature-icon">&#x1F69A;</div>
        <div class="auth-feature-text"><strong style="color:#fff;">Collector</strong> — Accept assignments, navigate to hospitals, update pickup status.</div>
      </div>
      <div class="auth-feature">
        <div class="auth-feature-icon">&#x1F4CA;</div>
        <div class="auth-feature-text"><strong style="color:#fff;">Admin</strong> — Monitor all requests, handle overdue escalations, view analytics.</div>
      </div>
    </div>
  </div>
  <div class="auth-form-area">
    <div class="auth-card">
      <h1>Create account</h1>
      <p class="subtitle">Join the MediTrack platform</p>

      <c:if test="${not empty error}">
        <div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>${error}</div>
      </c:if>

      <form method="post" action="${pageContext.request.contextPath}/register">
        <div class="grid grid-2" style="gap:12px;">
          <div class="form-group" style="margin-bottom:0;">
            <label class="form-label" for="fullName">Full name <span>*</span></label>
            <input class="form-control" type="text" id="fullName" name="fullName" required placeholder="Dr. Ravi Kumar">
          </div>
          <div class="form-group" style="margin-bottom:0;">
            <label class="form-label" for="phone">Phone</label>
            <input class="form-control" type="tel" id="phone" name="phone" placeholder="+91 98765 43210">
          </div>
        </div>
        <div class="form-group mt-2">
          <label class="form-label" for="email">Email address <span>*</span></label>
          <input class="form-control" type="email" id="email" name="email" required placeholder="you@hospital.org">
        </div>
        <div class="form-group">
          <label class="form-label" for="password">Password <span>*</span></label>
          <input class="form-control" type="password" id="password" name="password" required minlength="6" placeholder="At least 6 characters">
        </div>
        <div class="form-group">
          <label class="form-label" for="role">I am a... <span>*</span></label>
          <select class="form-control" id="role" name="role" required onchange="toggleVehicle()">
            <option value="">Select your role</option>
            <option value="HOSPITAL_STAFF">Hospital Staff</option>
            <option value="COLLECTOR">Waste Collector</option>
            <option value="ADMIN">Administrator</option>
          </select>
        </div>
        <div class="form-group" id="vehicleGroup" style="display:none;">
          <label class="form-label" for="vehicleNumber">Vehicle number</label>
          <input class="form-control" type="text" id="vehicleNumber" name="vehicleNumber" placeholder="e.g. AP-09-TC-4521">
        </div>
        <button type="submit" class="btn btn-primary btn-block btn-lg">Create account</button>
      </form>
      <p style="margin-top:20px;text-align:center;font-size:0.85rem;color:var(--ink-3);">
        Already have an account? <a href="${pageContext.request.contextPath}/login.jsp">Sign in</a>
      </p>
    </div>
  </div>
</div>
<script>
function toggleVehicle(){
  var v=document.getElementById('vehicleGroup');
  v.style.display=document.getElementById('role').value==='COLLECTOR'?'block':'none';
}
</script>
</body>
</html>
