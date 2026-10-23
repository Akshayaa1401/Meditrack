<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Collectors — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left"><h1>Manage Collectors</h1><p>${collectors.size()} registered collector(s).</p></div>
    <div style="display:flex;gap:8px;align-items:center;">
      <input type="text" class="form-control" id="searchCollectors" placeholder="&#128269; Search collectors..." oninput="filterRows(this.value)" style="width:220px;padding:8px 12px;">
      <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
    </div>
  </div>
  <div class="page-body">
    <div class="card">
      <c:if test="${empty collectors}">
        <div class="empty-state"><div class="empty-state-icon">&#x1F69A;</div><h3>No collectors registered</h3><p>Collectors register via the sign-up page and select the Collector role.</p></div>
      </c:if>
      <c:if test="${not empty collectors}">
        <div class="table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Vehicle</th><th>Status</th><th>Collections</th><th>Set Depot Location</th><th></th></tr></thead>
          <tbody>
          <c:forEach var="c" items="${collectors}">
            <tr>
              <td><strong>${c.fullName}</strong></td>
              <td class="mono text-sm">${c.vehicleNumber}</td>
              <td>
                <c:choose>
                  <c:when test="${c.status=='AVAILABLE'}"><span class="badge badge-disposed">Available</span></c:when>
                  <c:when test="${c.status=='ON_ROUTE'}"><span class="badge badge-en-route">&#x1F69A; On Route</span></c:when>
                  <c:otherwise><span class="badge badge-pending">${c.status}</span></c:otherwise>
                </c:choose>
              </td>
              <td class="mono">${c.totalCollections}</td>
              <td>
                <form method="post" action="${pageContext.request.contextPath}/admin/collectors" style="display:flex;gap:6px;align-items:center;">
                  <input type="hidden" name="action" value="updateLocation">
                  <input type="hidden" name="collectorId" value="${c.id}">
                  <input type="text" name="lat" class="form-control" placeholder="Lat" value="${c.currentLat}" style="width:90px;padding:6px 8px;font-size:0.8rem;">
                  <input type="text" name="lng" class="form-control" placeholder="Lng" value="${c.currentLng}" style="width:90px;padding:6px 8px;font-size:0.8rem;">
                  <button class="btn btn-secondary btn-sm">Save</button>
                </form>
              </td>
              <td>
                <form method="post" action="${pageContext.request.contextPath}/admin/collectors" style="display:inline;">
                  <input type="hidden" name="action" value="delete">
                  <input type="hidden" name="collectorId" value="${c.id}">
                  <button class="btn btn-danger btn-sm" onclick="return confirm('Remove this collector?')">Remove</button>
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
function filterRows(q){
  var rows=document.querySelectorAll('tbody tr');
  var term=q.toLowerCase();
  rows.forEach(function(r){r.style.display=r.textContent.toLowerCase().includes(term)?'':'none';});
}
</script>
</body></html>
