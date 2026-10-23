<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Admin Dashboard — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin="anonymous">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left">
      <h1>Operations Overview</h1>
      <p>Real-time biomedical waste monitoring across all hospitals and collectors.</p>
    </div>
    <div class="topbar-actions">
      <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
    </div>
  </div>
  <div class="page-body">

    <c:if test="${param.reassigned=='true'}"><div class="alert alert-success"><span class="alert-icon">&#x2713;</span>Collection reassigned successfully.</div></c:if>
    <c:if test="${param.error=='select_collector'}"><div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>Please select a collector before reassigning.</div></c:if>
    <c:if test="${param.error=='not_found'}"><div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>Waste record not found.</div></c:if>
    <c:if test="${param.error=='collector_not_found'}"><div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>Selected collector not found.</div></c:if>

    <%-- Stats --%>
    <div class="stats-grid">
      <div class="stat-card"><div class="stat-icon">&#x1F3E5;</div><div class="stat-label">Hospitals</div><div class="stat-value">${totalHospitals}</div></div>
      <div class="stat-card blue"><div class="stat-icon">&#x1F69A;</div><div class="stat-label">Collectors</div><div class="stat-value">${totalCollectors}</div></div>
      <div class="stat-card amber"><div class="stat-icon">&#x23F3;</div><div class="stat-label">Pending</div><div class="stat-value amber">${pendingWaste}</div></div>
      <div class="stat-card"><div class="stat-icon">&#x1F504;</div><div class="stat-label">Active Pickups</div><div class="stat-value">${assignedWaste}</div></div>
      <div class="stat-card red"><div class="stat-icon">&#x1F6A8;</div><div class="stat-label">Emergencies</div><div class="stat-value red">${emergencyCount}</div></div>
      <div class="stat-card ${overdueCount>0?'red':''}"><div class="stat-icon">&#x1F534;</div><div class="stat-label">Overdue</div><div class="stat-value ${overdueCount>0?'red':''}">${overdueCount}</div></div>
      <div class="stat-card green"><div class="stat-icon">&#x2713;</div><div class="stat-label">Collected Today</div><div class="stat-value">${collectedToday}</div></div>
      <div class="stat-card green"><div class="stat-icon">&#x2696;</div><div class="stat-label">Kg Today</div><div class="stat-value" style="font-size:1.4rem;">${kgCollectedToday}</div></div>
    </div>

    <%-- Live map --%>
    <div class="card mb-2">
      <div class="card-header">
        <h3>Live Operations Map</h3>
        <div style="font-size:0.78rem;color:var(--ink-3);">&#x1F3E5; Hospitals &nbsp; &#x1F534; Emergencies &nbsp; &#x1F4CD; Pending &nbsp; &#x1F69A; Collectors</div>
      </div>
      <div id="adminMap" class="map-container" style="height:380px;"></div>
    </div>

    <%-- Emergencies --%>
    <c:if test="${not empty emergencyRecords}">
      <div class="section-header">
        <h2>&#x1F6A8; Active Emergencies</h2>
        <span class="section-count danger">${emergencyRecords.size()}</span>
      </div>
      <div class="card" style="border-color:var(--red);">
        <div class="table-wrap">
        <table>
          <thead><tr><th>Hospital</th><th>Type</th><th>Qty</th><th>Priority</th><th>Status</th><th>Deadline</th><th>Action</th></tr></thead>
          <tbody>
          <c:forEach var="w" items="${emergencyRecords}">
            <tr>
              <td><strong>${w.hospitalName}</strong></td>
              <td>${w.wasteType}</td>
              <td>${w.quantityKg} kg</td>
              <td class="mono">${w.priorityScore}</td>
              <td>
                <c:choose>
                  <c:when test="${w.status=='PENDING'}"><span class="badge badge-pending">Pending</span></c:when>
                  <c:when test="${w.status=='ASSIGNED'}"><span class="badge badge-assigned">Assigned</span></c:when>
                  <c:when test="${w.status=='ACCEPTED'}"><span class="badge badge-accepted">Accepted</span></c:when>
                  <c:when test="${w.status=='EN_ROUTE'}"><span class="badge badge-en-route">En Route</span></c:when>
                  <c:when test="${w.status=='ARRIVED'}"><span class="badge badge-arrived">Arrived</span></c:when>
                  <c:otherwise><span class="badge badge-pending">${w.status}</span></c:otherwise>
                </c:choose>
              </td>
              <td class="mono text-xs">${not empty w.deadline?w.deadline.substring(0,16):'-'}</td>
              <td><a href="${pageContext.request.contextPath}/waste/detail?id=${w.id}" class="btn btn-secondary btn-sm">View</a></td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
        </div>
      </div>
    </c:if>

    <%-- Overdue --%>
    <c:if test="${not empty overdueRecords}">
      <div class="section-header mt-3">
        <h2>&#x1F534; Overdue Collections</h2>
        <span class="section-count danger">${overdueRecords.size()}</span>
      </div>
      <c:forEach var="w" items="${overdueRecords}">
        <div class="card" style="border-color:var(--red);margin-bottom:10px;">
          <div class="flex-between" style="margin-bottom:10px;">
            <div>
              <strong>${w.hospitalName}</strong> &mdash; ${w.wasteType} (${w.quantityKg} kg)
              <span class="badge badge-overdue" style="margin-left:8px;">&#x1F534; OVERDUE</span>
            </div>
            <span class="mono text-xs text-muted">Deadline: ${not empty w.deadline?w.deadline.substring(0,16):'-'}</span>
          </div>
          <c:if test="${not empty w.delayReason}">
            <p style="font-size:0.82rem;color:var(--ink-2);margin-bottom:10px;">Delay: ${w.delayReason}</p>
          </c:if>
          <form method="post" action="${pageContext.request.contextPath}/admin/reassign"
                onsubmit="return validateReassign(this)"
                style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
            <input type="hidden" name="wasteId" value="${w.id}">
            <c:choose>
              <c:when test="${empty availableCollectors}">
                <span style="font-size:0.82rem;color:var(--ink-3);font-style:italic;">No available collectors right now.</span>
              </c:when>
              <c:otherwise>
                <select name="newCollectorId" class="form-control" required
                        style="flex:1;min-width:180px;font-size:0.82rem;">
                  <option value="">— Select a collector —</option>
                  <c:forEach var="c" items="${availableCollectors}">
                    <option value="${c.id}">${c.fullName} (${c.vehicleNumber}) — ${c.status}</option>
                  </c:forEach>
                </select>
                <input type="text" name="reason" class="form-control"
                       placeholder="Reason (optional)"
                       style="flex:1;min-width:140px;font-size:0.82rem;">
                <button class="btn btn-danger btn-sm"
                        onclick="return confirm('Reassign this collection? The new collector will be notified immediately.')">
                  Reassign
                </button>
              </c:otherwise>
            </c:choose>
            <a href="${pageContext.request.contextPath}/waste/detail?id=${w.id}" class="btn btn-secondary btn-sm">View</a>
          </form>
        </div>
      </c:forEach>
    </c:if>

    <%-- Re-opt log --%>
    <div class="section-header mt-3"><h2>Route Re-optimization Log</h2></div>
    <div class="card">
      <c:if test="${empty recentUpdates}">
        <div class="empty-state"><div class="empty-state-icon">&#x1F504;</div><h3>No events yet</h3><p>Route re-optimizations appear here when emergency waste triggers a live route change.</p></div>
      </c:if>
      <c:if test="${not empty recentUpdates}">
        <div class="table-wrap">
        <table>
          <thead><tr><th>Collector</th><th>Reason</th><th>Old dist.</th><th>New dist.</th><th>When</th></tr></thead>
          <tbody>
          <c:forEach var="u" items="${recentUpdates}">
            <tr>
              <td class="mono text-sm">${u.collectorId}</td>
              <td>${u.reason}</td>
              <td class="mono">${u.oldDistanceKm} km</td>
              <td class="mono">${u.newDistanceKm} km</td>
              <td class="mono text-xs text-muted">${not empty u.createdAt?u.createdAt.substring(0,16):''}</td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
        </div>
      </c:if>
    </div>
  </div>
</div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin="anonymous"></script>
<script src="${pageContext.request.contextPath}/js/map.js"></script>
<script>
var CTX='${pageContext.request.contextPath}';
function isValidCoord(lat,lng){return typeof lat==='number'&&typeof lng==='number'&&lat>=-90&&lat<=90&&lng>=-180&&lng<=180&&(lat!==0||lng!==0);}
(function(){
  var el=document.getElementById('adminMap');
  if(!el||typeof L==='undefined'){if(el)el.innerHTML='<div class="map-placeholder">Map unavailable.</div>';return;}
  el.innerHTML='';
  var map=L.map(el).setView([17.9784,79.5941],6);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'&copy; OSM',maxZoom:19}).addTo(map);
  fetch(CTX+'/api/mapData?adminView=true').then(function(r){return r.json();}).then(function(data){
    var bounds=[];
    (data.hospitals||[]).forEach(function(h){
      if(!isValidCoord(h.latitude,h.longitude))return;
      var icon=L.divIcon({html:'<div style="background:#0891B2;color:#fff;width:26px;height:26px;border-radius:6px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:11px;border:2px solid #fff;">H</div>',className:'',iconSize:[26,26],iconAnchor:[13,13]});
      L.marker([h.latitude,h.longitude],{icon:icon}).addTo(map).bindPopup('<strong>'+h.name+'</strong><br>'+(h.address||''));
      bounds.push([h.latitude,h.longitude]);
    });
    (data.wastePoints||[]).forEach(function(w){
      if(!isValidCoord(w.latitude,w.longitude))return;
      var color=w.emergency?'#DC2626':(w.overdue?'#DC2626':(w.riskLevel==='HIGH'?'#DC2626':(w.riskLevel==='MEDIUM'?'#D97706':'#16A34A')));
      var html='<div style="width:14px;height:14px;border-radius:50%;background:'+color+';border:2px solid #fff;box-shadow:0 0 0 2px '+color+'44;"></div>';
      var icon=L.divIcon({html:html,className:'',iconSize:[14,14],iconAnchor:[7,7]});
      L.marker([w.latitude,w.longitude],{icon:icon}).addTo(map).bindPopup('<strong>'+w.hospitalName+'</strong><br>'+w.wasteType+' ('+w.quantityKg+' kg)<br>'+w.status+(w.emergency?'<br><strong style="color:#DC2626">EMERGENCY</strong>':''));
      bounds.push([w.latitude,w.longitude]);
    });
    (data.collectorLocations||[]).forEach(function(c){
      if(!isValidCoord(c.latitude,c.longitude))return;
      var icon=L.divIcon({html:'<div style="background:#7C3AED;color:#fff;width:26px;height:26px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:13px;border:2px solid #fff;">&#x1F69A;</div>',className:'',iconSize:[26,26],iconAnchor:[13,13]});
      L.marker([c.latitude,c.longitude],{icon:icon}).addTo(map).bindPopup('&#x1F69A; <strong>'+c.name+'</strong><br>'+c.status);
      bounds.push([c.latitude,c.longitude]);
    });
    if(bounds.length)map.fitBounds(bounds,{padding:[30,30]});
  }).catch(function(){el.innerHTML='<div class="map-placeholder">Could not load map data.</div>';});
})();
</script>
<script>
function validateReassign(form) {
  var sel = form.querySelector('select[name="newCollectorId"]');
  if (!sel || !sel.value || sel.value.trim() === '') {
    alert('Please select a collector to reassign to.');
    return false;
  }
  return true;
}
</script>
</body>
</html>
