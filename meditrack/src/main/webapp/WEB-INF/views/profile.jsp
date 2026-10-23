<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>My Profile — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left">
      <h1>My Profile</h1>
      <p>View your account details and change your password.</p>
    </div>
    <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
  </div>
  <div class="page-body">

    <c:if test="${param.success=='true'}"><div class="alert alert-success"><span class="alert-icon">&#x2713;</span>Password updated successfully.</div></c:if>
    <c:if test="${param.error=='wrongpassword'}"><div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>Current password is incorrect.</div></c:if>
    <c:if test="${param.error=='mismatch'}"><div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>New passwords do not match.</div></c:if>
    <c:if test="${param.error=='tooshort'}"><div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>New password must be at least 6 characters.</div></c:if>

    <div class="grid grid-2" style="align-items:start;max-width:900px;">

      <%-- Account info card --%>
      <div class="card">
        <h3 style="margin-bottom:18px;">Account Details</h3>
        <div style="display:flex;align-items:center;gap:16px;margin-bottom:22px;">
          <div style="width:56px;height:56px;border-radius:50%;background:var(--teal);display:flex;align-items:center;justify-content:center;font-family:var(--font-display);font-weight:700;font-size:1.4rem;color:#fff;flex-shrink:0;">
            ${not empty user ? user.fullName.substring(0,1).toUpperCase() : 'U'}
          </div>
          <div>
            <div style="font-weight:700;font-size:1.1rem;">${user.fullName}</div>
            <div style="color:var(--ink-3);font-size:0.85rem;">${user.email}</div>
          </div>
        </div>
        <div class="divider"></div>
        <div style="display:grid;gap:14px;margin-top:14px;font-size:0.875rem;">
          <div><div class="stat-label">Role</div>
            <c:choose>
              <c:when test="${user.role=='ADMIN'}"><span class="badge badge-HIGH">Admin</span></c:when>
              <c:when test="${user.role=='HOSPITAL_STAFF'}"><span class="badge badge-assigned">Hospital Staff</span></c:when>
              <c:when test="${user.role=='COLLECTOR'}"><span class="badge badge-en-route">Collector</span></c:when>
            </c:choose>
          </div>
          <c:if test="${not empty user.phone}">
            <div><div class="stat-label">Phone</div><strong>${user.phone}</strong></div>
          </c:if>
          <div><div class="stat-label">Member since</div><span class="mono text-xs">${not empty user.createdAt?user.createdAt.substring(0,10):''}</span></div>
        </div>

        <%-- Collector-specific --%>
        <c:if test="${not empty collector}">
          <div class="divider"></div>
          <h4 style="margin-bottom:12px;">Collector Details</h4>
          <div style="display:grid;gap:10px;font-size:0.875rem;">
            <div><div class="stat-label">Vehicle Number</div><strong class="mono">${collector.vehicleNumber}</strong></div>
            <div><div class="stat-label">Status</div>
              <c:choose>
                <c:when test="${collector.status=='AVAILABLE'}"><span class="badge badge-disposed">Available</span></c:when>
                <c:when test="${collector.status=='ON_ROUTE'}"><span class="badge badge-en-route">&#x1F69A; On Route</span></c:when>
                <c:otherwise><span class="badge badge-pending">${collector.status}</span></c:otherwise>
              </c:choose>
            </div>
            <div><div class="stat-label">Total Collections</div><strong class="mono">${collector.totalCollections}</strong></div>
          </div>
        </c:if>

        <%-- Hospital-specific --%>
        <c:if test="${not empty hospital}">
          <div class="divider"></div>
          <h4 style="margin-bottom:12px;">Hospital Details</h4>
          <div style="display:grid;gap:10px;font-size:0.875rem;">
            <div><div class="stat-label">Hospital Name</div><strong>${hospital.name}</strong></div>
            <div><div class="stat-label">Address</div>${hospital.address}</div>
            <div><div class="stat-label">Coordinates</div><span class="mono text-xs">${hospital.latitude}, ${hospital.longitude}</span></div>
          </div>
        </c:if>
      </div>

      <%-- Change password card --%>
      <div class="card">
        <h3 style="margin-bottom:18px;">Change Password</h3>
        <form method="post" action="${pageContext.request.contextPath}/profile">
          <div class="form-group">
            <label class="form-label" for="currentPassword">Current password <span style="color:var(--red)">*</span></label>
            <input class="form-control" type="password" id="currentPassword" name="currentPassword" required placeholder="Your current password">
          </div>
          <div class="form-group">
            <label class="form-label" for="newPassword">New password <span style="color:var(--red)">*</span></label>
            <input class="form-control" type="password" id="newPassword" name="newPassword" required minlength="6" placeholder="At least 6 characters" oninput="checkMatch()">
          </div>
          <div class="form-group">
            <label class="form-label" for="confirmPassword">Confirm new password <span style="color:var(--red)">*</span></label>
            <input class="form-control" type="password" id="confirmPassword" name="confirmPassword" required placeholder="Type new password again" oninput="checkMatch()">
            <div id="matchMsg" style="font-size:0.75rem;margin-top:5px;"></div>
          </div>
          <button type="submit" class="btn btn-primary btn-block">Update Password</button>
        </form>
        <div class="divider"></div>
        <p style="font-size:0.78rem;color:var(--ink-3);">&#x1F512; Your password is stored as a salted hash. MediTrack staff cannot see it.</p>
      </div>

    </div>
  </div>
</div>
<script>
function checkMatch(){
  var np=document.getElementById('newPassword').value;
  var cp=document.getElementById('confirmPassword').value;
  var msg=document.getElementById('matchMsg');
  if(!cp)return;
  if(np===cp){msg.style.color='var(--green)';msg.textContent='Passwords match';}
  else{msg.style.color='var(--red)';msg.textContent='Passwords do not match';}
}
</script>
</body>
</html>
