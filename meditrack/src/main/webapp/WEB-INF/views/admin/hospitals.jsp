<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Hospitals — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left"><h1>Manage Hospitals</h1><p>${hospitals.size()} registered hospital(s)</p></div>
    <div style="display:flex;gap:8px;align-items:center;">
      <input type="text" class="form-control" id="searchHospitals" placeholder="&#128269; Search hospitals..." oninput="filterTable('hospitalTable',this.value)" style="width:220px;padding:8px 12px;">
      <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
    </div>
  </div>
  <div class="page-body">
    <div class="card">
      <c:if test="${empty hospitals}">
        <div class="empty-state"><div class="empty-state-icon">&#x1F3E5;</div><h3>No hospitals registered</h3><p>Hospital staff register their hospitals when they set up their accounts.</p></div>
      </c:if>
      <c:if test="${not empty hospitals}">
        <div class="table-wrap">
        <table>
          <thead id="hospitalTable"><tr><th>Name</th><th>Address</th><th>Coordinates</th><th>Contact</th><th>Registered</th><th></th></tr></thead>
          <tbody>
          <c:forEach var="h" items="${hospitals}">
            <tr>
              <td><strong>${h.name}</strong></td>
              <td>${h.address}</td>
              <td class="mono text-xs">${h.latitude}, ${h.longitude}</td>
              <td>${h.contactPerson}<c:if test="${not empty h.contactPhone}"><br><span class="text-xs text-muted">${h.contactPhone}</span></c:if></td>
              <td class="text-xs text-muted mono">${not empty h.createdAt?h.createdAt.substring(0,10):''}</td>
              <td>
                <form method="post" action="${pageContext.request.contextPath}/admin/hospitals" style="display:inline;">
                  <input type="hidden" name="action" value="delete">
                  <input type="hidden" name="hospitalId" value="${h.id}">
                  <button class="btn btn-danger btn-sm" onclick="return confirm('Remove this hospital? This cannot be undone.')">Remove</button>
                </form>
              </td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
        </div>
      </c:if>
    </div>
  </div>
</div>
<script>
function filterTable(tableId,q){
  var rows=document.querySelectorAll('#'+tableId+' ~ tbody tr, table tbody tr');
  var term=q.toLowerCase();
  rows.forEach(function(r){r.style.display=r.textContent.toLowerCase().includes(term)?'':'none';});
}
</script>
</body></html>
