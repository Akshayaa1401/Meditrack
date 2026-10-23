<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Collector Dashboard — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin="anonymous">
<style>
.nav-btn{display:inline-flex;align-items:center;gap:8px;padding:10px 18px;background:#1A73E8;color:#fff;border:none;border-radius:var(--r-md);font-weight:700;font-size:0.9rem;cursor:pointer;text-decoration:none;}
.nav-btn:hover{background:#1557B0;color:#fff;text-decoration:none;}
.nav-btn-wrap{background:#EAF2FF;border:1px solid #BFDBFE;border-radius:var(--r-md);padding:12px 16px;margin-bottom:14px;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:10px;}
.nav-btn-info{font-size:0.82rem;color:var(--ink-2);}
.nav-btn-info strong{display:block;font-size:0.9rem;color:var(--ink);margin-bottom:2px;}
</style>
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left">
      <h1>My Collections</h1>
      <p>Pickup assignments sorted by urgency. Complete each step before moving to the next.</p>
    </div>
    <div class="topbar-actions">
      <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
      <a href="${pageContext.request.contextPath}/collector/optimizeRoute" class="btn btn-secondary btn-sm">Refresh Route</a>
    </div>
  </div>
  <div class="page-body">

    <c:if test="${param.updated=='true'}"><div class="alert alert-success"><span class="alert-icon">&#x2713;</span>Status updated successfully.</div></c:if>
    <c:if test="${param.optimized=='true'}"><div class="alert alert-success"><span class="alert-icon">&#x2713;</span>Route generated from pending requests.</div></c:if>

    <div class="stats-grid">
      <div class="stat-card"><div class="stat-label">Active Now</div><div class="stat-value">${activeWasteRecords.size()}</div></div>
      <div class="stat-card red"><div class="stat-label">Overdue</div><div class="stat-value red">${overdueWasteRecords.size()}</div></div>
      <div class="stat-card green"><div class="stat-label">Completed Today</div><div class="stat-value">${completedToday.size()}</div></div>
      <div class="stat-card"><div class="stat-label">Kg Today</div><div class="stat-value">${totalKgToday}</div></div>
      <c:if test="${not empty assignment and assignment.totalDistanceKm > 0}">
        <div class="stat-card blue"><div class="stat-label">Route Distance</div><div class="stat-value" style="font-size:1.4rem;">${assignment.totalDistanceKm} km</div></div>
      </c:if>
      <div class="stat-card"><div class="stat-label">All-Time Disposals</div><div class="stat-value" style="font-size:1.4rem;">${historyTotal}</div></div>
    </div>

    <%-- OVERDUE --%>
    <c:if test="${not empty overdueWasteRecords}">
      <div class="section-header">
        <h2>&#x1F534; Overdue — Immediate Action Required</h2>
        <span class="section-count danger">${overdueWasteRecords.size()}</span>
      </div>
      <c:forEach var="w" items="${overdueWasteRecords}">
        <div class="emergency-card">
          <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:10px;">
            <div>
              <div style="font-weight:700;font-size:1rem;color:var(--red);">&#x1F534; OVERDUE — ${w.wasteType}</div>
              <div style="font-size:0.85rem;color:var(--ink-2);margin-top:2px;">${w.hospitalName} &nbsp;|&nbsp; ${w.quantityKg} kg</div>
            </div>
            <span class="badge badge-HIGH">HIGH</span>
          </div>
          <form method="post" action="${pageContext.request.contextPath}/collector/updateStatus" style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
            <input type="hidden" name="wasteId" value="${w.id}">
            <select name="delayReason" class="form-control" style="flex:1;min-width:180px;font-size:0.82rem;">
              <option value="">Select delay reason (optional)</option>
              <option>Traffic / route delay</option>
              <option>Hospital unavailable</option>
              <option>Vehicle issue</option>
              <option>Emergency reassignment</option>
              <option>Other</option>
            </select>
            <button class="btn btn-danger" name="status" value="ACCEPTED">Accept (Overdue)</button>
            <a href="${pageContext.request.contextPath}/waste/detail?id=${w.id}" class="btn btn-secondary btn-sm">View</a>
          </form>
        </div>
      </c:forEach>
    </c:if>

    <%-- ACTIVE COLLECTIONS --%>
    <div class="section-header mt-3">
      <h2>Active Collections</h2>
      <span class="section-count ${activeWasteRecords.size()>0?'active':''}">${activeWasteRecords.size()}</span>
    </div>

    <c:if test="${empty activeWasteRecords and empty overdueWasteRecords}">
      <div class="card">
        <div class="empty-state">
          <div class="empty-state-icon">&#x1F4CB;</div>
          <h3>No active collections</h3>
          <p>Click <strong>Refresh Route</strong> to pull in the latest pending requests.</p>
          <a href="${pageContext.request.contextPath}/collector/optimizeRoute" class="btn btn-primary mt-2">Refresh Route</a>
        </div>
      </div>
    </c:if>

    <c:forEach var="w" items="${activeWasteRecords}">
      <c:set var="cardClass" value="${w.riskLevel=='HIGH'?'urgent':(w.riskLevel=='MEDIUM'?'medium-p':'low-p')}"/>
      <div class="priority-card ${cardClass}">
        <div class="priority-card-header">
          <div>
            <c:choose>
              <c:when test="${w.riskLevel=='HIGH'}"><span class="badge badge-HIGH" style="margin-bottom:6px;">&#x1F534; HIGH PRIORITY</span></c:when>
              <c:when test="${w.riskLevel=='MEDIUM'}"><span class="badge badge-MEDIUM" style="margin-bottom:6px;">&#x1F7E0; MEDIUM PRIORITY</span></c:when>
              <c:otherwise><span class="badge badge-LOW" style="margin-bottom:6px;">&#x1F7E2; LOW PRIORITY</span></c:otherwise>
            </c:choose>
            <c:if test="${w.emergency}"><span class="badge badge-emergency" style="margin-left:4px;margin-bottom:6px;">&#x1F6A8; EMERGENCY</span></c:if>
            <div class="priority-card-title">${w.hospitalName}</div>
          </div>
          <c:choose>
            <c:when test="${w.status=='ASSIGNED'}"><span class="badge badge-assigned">Assigned</span></c:when>
            <c:when test="${w.status=='ACCEPTED'}"><span class="badge badge-accepted">Accepted</span></c:when>
            <c:when test="${w.status=='EN_ROUTE'}"><span class="badge badge-en-route">&#x1F69A; En Route</span></c:when>
            <c:when test="${w.status=='ARRIVED'}"><span class="badge badge-arrived">&#x1F4CD; Arrived</span></c:when>
          </c:choose>
        </div>

        <div class="priority-card-meta">
          <div class="meta-item"><div class="meta-label">Waste Type</div><div class="meta-value">${w.wasteType}</div></div>
          <div class="meta-item"><div class="meta-label">Quantity</div><div class="meta-value">${w.quantityKg} kg</div></div>
          <div class="meta-item"><div class="meta-label">Priority Score</div><div class="meta-value mono">${w.priorityScore}</div></div>
          <div class="meta-item">
            <div class="meta-label">Deadline</div>
            <div class="meta-value">
              <c:if test="${not empty w.deadline}">
                <span class="deadline-chip" data-deadline="${w.deadline}"></span>
              </c:if>
            </div>
          </div>
        </div>

        <%-- START NAVIGATION — shown for ALL statuses so collector can navigate even before accepting --%>
        <c:if test="${w.status=='ASSIGNED'||w.status=='ACCEPTED'||w.status=='EN_ROUTE'||w.status=='ARRIVED'}">
          <div class="nav-btn-wrap">
            <div class="nav-btn-info">
              <strong>&#x1F3E5; ${w.hospitalName}</strong>
              Tap to open turn-by-turn directions in Google Maps
            </div>
            <a class="nav-btn"
               id="navBtn-${w.id}"
               href="#"
               onclick="openNavigation('${w.id}', '${w.hospitalName}', this); return false;"
               target="_blank" rel="noopener noreferrer">
              &#x1F30D; Open in Google Maps
            </a>
          </div>
        </c:if>

        <%-- Mini map for EN_ROUTE/ARRIVED --%>
        <c:if test="${w.status=='EN_ROUTE'||w.status=='ARRIVED'}">
          <div id="map-${w.id}" class="map-container" style="height:200px;margin-bottom:14px;">
            <div class="map-placeholder">&#x1F5FA; Loading map...</div>
          </div>
          <c:if test="${w.status=='EN_ROUTE'}">
            <div class="tracking-active-chip" style="margin-bottom:12px;">Tracking Active — Hospital can see your location</div>
          </c:if>
        </c:if>

        <%-- Action buttons --%>
        <div class="priority-card-action">
          <c:choose>
            <c:when test="${w.status=='ASSIGNED'}">
              <form method="post" action="${pageContext.request.contextPath}/collector/updateStatus">
                <input type="hidden" name="wasteId" value="${w.id}">
                <button class="btn btn-accept btn-lg" name="status" value="ACCEPTED">&#x2713; Accept Request</button>
              </form>
            </c:when>
            <c:when test="${w.status=='ACCEPTED'}">
              <form method="post" action="${pageContext.request.contextPath}/collector/updateStatus">
                <input type="hidden" name="wasteId" value="${w.id}">
                <button class="btn btn-start btn-lg" name="status" value="EN_ROUTE">&#x1F69A; Start Pickup</button>
              </form>
            </c:when>
            <c:when test="${w.status=='EN_ROUTE'}">
              <form method="post" action="${pageContext.request.contextPath}/collector/updateStatus">
                <input type="hidden" name="wasteId" value="${w.id}">
                <button class="btn btn-arrived btn-lg" name="status" value="ARRIVED">&#x1F4CD; Mark Arrived</button>
              </form>
            </c:when>
            <c:when test="${w.status=='ARRIVED'}">
              <form method="post" action="${pageContext.request.contextPath}/collector/updateStatus">
                <input type="hidden" name="wasteId" value="${w.id}">
                <button class="btn btn-collect btn-lg" name="status" value="COLLECTED">&#x1F4E6; Collect Waste</button>
              </form>
            </c:when>
          </c:choose>
          <a href="${pageContext.request.contextPath}/waste/detail?id=${w.id}" class="btn btn-secondary">View Details</a>
        </div>
      </div>
    </c:forEach>

    <%-- COMPLETED TODAY --%>
    <c:if test="${not empty completedToday}">
      <div class="section-header mt-3">
        <h2>&#x2705; Completed Today</h2>
        <span class="section-count active">${completedToday.size()}</span>
      </div>
      <div class="card">
        <div class="table-wrap"><table>
          <thead><tr><th>Hospital</th><th>Type</th><th>Qty</th><th>Risk</th><th>Status</th><th>Time</th><th></th></tr></thead>
          <tbody>
          <c:forEach var="w" items="${completedToday}">
            <tr>
              <td><strong>${w.hospitalName}</strong></td>
              <td>${w.wasteType}</td>
              <td>${w.quantityKg} kg</td>
              <td><c:choose>
                <c:when test="${w.riskLevel=='HIGH'}"><span class="badge badge-HIGH">HIGH</span></c:when>
                <c:when test="${w.riskLevel=='MEDIUM'}"><span class="badge badge-MEDIUM">MED</span></c:when>
                <c:otherwise><span class="badge badge-LOW">LOW</span></c:otherwise>
              </c:choose></td>
              <td><c:choose>
                <c:when test="${w.status=='COLLECTED'}"><span class="badge badge-collected">&#x1F4E6; Collected</span></c:when>
                <c:otherwise><span class="badge badge-disposed">&#x2713; Disposed</span></c:otherwise>
              </c:choose></td>
              <td class="mono text-xs">${not empty w.collectedAt?w.collectedAt.substring(11,16):''}</td>
              <td>
                <c:if test="${w.status=='COLLECTED'}">
                  <form method="post" action="${pageContext.request.contextPath}/collector/updateStatus" style="display:inline;">
                    <input type="hidden" name="wasteId" value="${w.id}">
                    <button class="btn btn-dispose btn-sm" name="status" value="DISPOSED">Mark Disposed</button>
                  </form>
                </c:if>
                <a href="${pageContext.request.contextPath}/waste/detail?id=${w.id}" class="btn btn-secondary btn-sm">Details</a>
              </td>
            </tr>
          </c:forEach>
          </tbody>
        </table></div>
      </div>
    </c:if>

    <%-- HISTORY --%>
    <c:if test="${historyTotal > 0}">
      <div class="section-header mt-3">
        <h2>Collection History</h2>
        <span class="section-count">${historyTotal} total</span>
      </div>
      <div class="card">
        <div class="table-wrap"><table>
          <thead><tr><th>Hospital</th><th>Type</th><th>Qty</th><th>Risk</th><th>Status</th><th>Collected</th><th>Disposed</th></tr></thead>
          <tbody>
          <c:choose>
            <c:when test="${not empty collectionHistory}">
              <c:forEach var="w" items="${collectionHistory}">
              <tr>
                <td>${w.hospitalName}</td>
                <td>${w.wasteType}</td>
                <td>${w.quantityKg} kg</td>
                <td><c:choose>
                  <c:when test="${w.riskLevel=='HIGH'}"><span class="badge badge-HIGH">HIGH</span></c:when>
                  <c:when test="${w.riskLevel=='MEDIUM'}"><span class="badge badge-MEDIUM">MED</span></c:when>
                  <c:otherwise><span class="badge badge-LOW">LOW</span></c:otherwise>
                </c:choose></td>
                <td><c:choose>
                  <c:when test="${w.status=='DISPOSED'}"><span class="badge badge-disposed">&#x2713; Disposed</span></c:when>
                  <c:otherwise><span class="badge badge-collected">Collected</span></c:otherwise>
                </c:choose></td>
                <td class="mono text-xs">${not empty w.collectedAt?w.collectedAt.substring(0,16):''}</td>
                <td class="mono text-xs">${not empty w.disposedAt?w.disposedAt.substring(0,16):''}</td>
              </tr>
              </c:forEach>
            </c:when>
            <c:otherwise>
              <tr><td colspan="7" style="text-align:center;color:var(--ink-3);padding:20px;">No records on this page.</td></tr>
            </c:otherwise>
          </c:choose>
          </tbody>
        </table></div>
        <%-- Pagination controls --%>
        <c:if test="${historyTotalPages > 1}">
          <div style="display:flex;align-items:center;justify-content:space-between;padding-top:14px;border-top:1px solid var(--border);margin-top:8px;flex-wrap:wrap;gap:8px;">
            <span style="font-size:0.82rem;color:var(--ink-3);">
              Page ${historyPage} of ${historyTotalPages} &nbsp;&middot;&nbsp; ${historyTotal} total records
            </span>
            <div style="display:flex;gap:6px;">
              <c:if test="${historyPage > 1}">
                <a href="${pageContext.request.contextPath}/collector/dashboard?historyPage=${historyPage-1}" class="btn btn-secondary btn-sm">&#x2190; Previous</a>
              </c:if>
              <c:forEach begin="1" end="${historyTotalPages}" var="p">
                <c:choose>
                  <c:when test="${p == historyPage}">
                    <span style="padding:6px 12px;background:var(--teal);color:#fff;border-radius:var(--r-sm);font-size:0.8rem;font-weight:600;">${p}</span>
                  </c:when>
                  <c:when test="${p <= 3 or p >= historyTotalPages-2 or (p >= historyPage-1 and p <= historyPage+1)}">
                    <a href="${pageContext.request.contextPath}/collector/dashboard?historyPage=${p}" class="btn btn-secondary btn-sm">${p}</a>
                  </c:when>
                  <c:when test="${p == 4 and historyPage > 5}">
                    <span style="padding:6px 4px;color:var(--ink-3);font-size:0.8rem;">...</span>
                  </c:when>
                </c:choose>
              </c:forEach>
              <c:if test="${historyPage < historyTotalPages}">
                <a href="${pageContext.request.contextPath}/collector/dashboard?historyPage=${historyPage+1}" class="btn btn-secondary btn-sm">Next &#x2192;</a>
              </c:if>
            </div>
          </div>
        </c:if>
      </div>
    </c:if>

    <%-- Route map --%>
    <c:if test="${not empty assignment and not empty assignment.route}">
      <div class="section-header mt-3"><h2>Route Map</h2></div>
      <div class="card">
        <div id="routeMap" class="map-container" style="height:380px;">
          <div class="map-placeholder">&#x1F5FA; Loading route map...</div>
        </div>
      </div>
    </c:if>

  </div>
</div>

<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin="anonymous"></script>
<script src="${pageContext.request.contextPath}/js/map.js"></script>
<script>
var CTX = '${pageContext.request.contextPath}';
var COLLECTOR_ID = '${sessionScope.collectorId}';

/* ── Deadline countdowns ─────────────────────────────── */
document.querySelectorAll('.deadline-chip[data-deadline]').forEach(function(el){
  function upd(){
    var dl=new Date(el.dataset.deadline),now=new Date(),diff=dl-now;
    if(diff<=0){el.className='deadline-chip overdue';el.textContent='OVERDUE';return;}
    var h=Math.floor(diff/3600000),m=Math.floor((diff%3600000)/60000);
    el.textContent=(h>0?h+'h ':'')+m+'m left';
    el.className='deadline-chip '+(diff<3600000?'approaching':'ok');
  }
  upd(); setInterval(upd, 60000);
});

/* ── START NAVIGATION — opens Google Maps directions ─── */
/* Looks up hospital lat/lng from /api/mapData then opens Google Maps */
var _hospitalCoords = {}; // cache: wasteId -> {lat, lng, name}

function openNavigation(wasteId, hospitalName, btn) {
  if (_hospitalCoords[wasteId]) {
    doOpenNav(_hospitalCoords[wasteId].lat, _hospitalCoords[wasteId].lng, hospitalName);
    return;
  }
  btn.textContent = 'Loading...';
  btn.style.pointerEvents = 'none';

  fetch(CTX + '/api/collectorLocation?wasteId=' + wasteId)
    .then(function(r){ return r.json(); })
    .then(function(locData){
      /* Get hospital coords from map data */
      return fetch(CTX + '/api/mapData')
        .then(function(r){ return r.json(); })
        .then(function(mapData){
          var hospitals = mapData.hospitals || [];
          /* match by hospitalName since we don't have hospitalId easily in JSP */
          var h = hospitals.find(function(x){
            return x.name && x.name.trim().toLowerCase() === hospitalName.trim().toLowerCase();
          });
          /* fallback: take nearest if name match fails */
          if (!h && hospitals.length) h = hospitals[0];

          if (h && isValidCoord(h.latitude, h.longitude)) {
            _hospitalCoords[wasteId] = { lat: h.latitude, lng: h.longitude, name: h.name };
            btn.innerHTML = '&#x1F9ED; Start Navigation';
            btn.style.pointerEvents = '';
            doOpenNav(h.latitude, h.longitude, h.name);
          } else {
            btn.innerHTML = '&#x1F9ED; Start Navigation';
            btn.style.pointerEvents = '';
            alert('Hospital location is not set yet. Please ask the admin to update the hospital profile with correct coordinates.');
          }
        });
    })
    .catch(function(){
      btn.innerHTML = '&#x1F9ED; Start Navigation';
      btn.style.pointerEvents = '';
      alert('Could not load hospital location. Please check your internet connection.');
    });
}

function doOpenNav(lat, lng, name) {
  /* Google Maps directions — opens in browser/app on mobile */
  var url = 'https://www.google.com/maps/dir/?api=1&destination=' + lat + ',' + lng
          + '&destination_place_name=' + encodeURIComponent(name)
          + '&travelmode=driving';
  window.open(url, '_blank', 'noopener,noreferrer');
}

function isValidCoord(lat, lng) {
  return typeof lat==='number' && typeof lng==='number'
      && lat>=-90 && lat<=90 && lng>=-180 && lng<=180
      && !(lat===0 && lng===0);
}

/* ── GPS sharing ─────────────────────────────────────── */
var activeIds = [
  <c:forEach var="w" items="${activeWasteRecords}" varStatus="s">
    {id:'${w.id}',status:'${w.status}'}<c:if test="${!s.last}">,</c:if>
  </c:forEach>
];
function shareGPS(){
  if(!navigator.geolocation) return;
  var tracking = activeIds.filter(function(w){
    return w.status==='EN_ROUTE'||w.status==='ARRIVED'||w.status==='ACCEPTED';
  });
  if(!tracking.length) return;
  navigator.geolocation.getCurrentPosition(function(pos){
    tracking.forEach(function(w){
      var fd=new FormData();
      fd.append('wasteId',w.id);
      fd.append('latitude',pos.coords.latitude);
      fd.append('longitude',pos.coords.longitude);
      fetch(CTX+'/api/collectorLocation',{method:'POST',body:fd}).catch(function(){});
    });
  }, function(){}, {enableHighAccuracy:true, timeout:8000});
}
shareGPS(); setInterval(shareGPS, 15000);

/* ── Mini maps ───────────────────────────────────────── */
<c:forEach var="w" items="${activeWasteRecords}">
  <c:if test="${w.status=='EN_ROUTE'||w.status=='ARRIVED'}">
  (function(){
    var el = document.getElementById('map-${w.id}');
    if(!el || typeof L==='undefined'){
      if(el) el.innerHTML='<div class="map-placeholder">Map unavailable.</div>'; return;
    }
    Promise.all([
      fetch(CTX+'/api/collectorLocation?wasteId=${w.id}').then(function(r){return r.json();}).catch(function(){return{};}),
      fetch(CTX+'/api/mapData').then(function(r){return r.json();}).catch(function(){return{};})
    ]).then(function(res){
      var loc=res[0], mapData=res[1];
      var hospital=(mapData.hospitals||[]).find(function(h){return h.name&&h.name.trim().toLowerCase()==='${w.hospitalName.replace("'","\\'")}' .toLowerCase();});
      if(!hospital||(hospital.latitude===0&&hospital.longitude===0)){
        el.innerHTML='<div class="map-placeholder">Hospital coordinates not set.</div>'; return;
      }
      /* cache for navigation */
      _hospitalCoords['${w.id}']={lat:hospital.latitude,lng:hospital.longitude,name:hospital.name};
      el.innerHTML='';
      var map=L.map(el).setView([hospital.latitude,hospital.longitude],13);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'&copy; OSM',maxZoom:19}).addTo(map);
      var hIcon=L.divIcon({html:'<div style="background:#0891B2;color:#fff;width:28px;height:28px;border-radius:6px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12px;border:2px solid #fff;">H</div>',className:'',iconSize:[28,28],iconAnchor:[14,14]});
      L.marker([hospital.latitude,hospital.longitude],{icon:hIcon}).addTo(map).bindPopup('<strong>${w.hospitalName}</strong>');
      if(loc.latitude && loc.longitude && isValidCoord(loc.latitude,loc.longitude)){
        var cIcon=L.divIcon({html:'<div style="background:#7C3AED;color:#fff;width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:14px;border:2px solid #fff;">&#x1F69A;</div>',className:'',iconSize:[28,28],iconAnchor:[14,14]});
        L.marker([loc.latitude,loc.longitude],{icon:cIcon}).addTo(map).bindPopup('Your location');
        L.polyline([[loc.latitude,loc.longitude],[hospital.latitude,hospital.longitude]],{color:'#7C3AED',weight:3,dashArray:'6 4'}).addTo(map);
        map.fitBounds([[loc.latitude,loc.longitude],[hospital.latitude,hospital.longitude]],{padding:[30,30]});
      }
    });
  })();
  </c:if>
</c:forEach>

/* ── Main route map ──────────────────────────────────── */
<c:if test="${not empty assignment and not empty assignment.route}">
var t=setTimeout(function(){
  var m=document.getElementById('routeMap');
  if(m) m.innerHTML='<div class="map-placeholder">Map timed out — check internet connection.</div>';
},8000);
fetch(CTX+'/api/mapData?collectorId='+COLLECTOR_ID)
  .then(function(r){return r.json();})
  .then(function(data){
    clearTimeout(t);
    var m=document.getElementById('routeMap');
    if(!m||typeof L==='undefined'){if(m)m.innerHTML='<div class="map-placeholder">Map unavailable.</div>';return;}
    if(data.activeRoute&&data.activeRoute.length){
      var first=data.activeRoute.find(function(s){return isValidCoord(s.latitude,s.longitude);});
      if(!first){m.innerHTML='<div class="map-placeholder">Route stops have no valid coordinates.</div>';return;}
      m.innerHTML='';
      initRouteMap('routeMap',data.activeRoute,first.latitude,first.longitude);
    } else {
      m.innerHTML='<div class="map-placeholder">No route stops yet. Accept a request to see the route.</div>';
    }
  })
  .catch(function(){
    clearTimeout(t);
    var m=document.getElementById('routeMap');
    if(m) m.innerHTML='<div class="map-placeholder">Could not load map — check internet connection.</div>';
  });
</c:if>
</script>
</body>
</html>
