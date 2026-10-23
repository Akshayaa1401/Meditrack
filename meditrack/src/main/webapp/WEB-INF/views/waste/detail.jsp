<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Waste Journey — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin="anonymous">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left">
      <h1>${waste.wasteType} — ${waste.hospitalName}</h1>
      <p>Complete waste journey from submission to safe disposal.</p>
    </div>
    <div class="topbar-actions">
      <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
      <a href="javascript:history.back()" class="btn btn-secondary btn-sm">&#x2190; Back</a>
    </div>
  </div>
  <div class="page-body">

    <c:if test="${waste.emergency}">
      <div class="emergency-banner">&#x1F6A8; EMERGENCY COLLECTION — Highest operational priority</div>
    </c:if>

    <div class="grid grid-2" style="align-items:start;">

      <%-- LEFT: details + timeline --%>
      <div>
        <%-- Status + info card --%>
        <div class="card" style="margin-bottom:16px;">
          <div class="card-header">
            <h3>Collection Status</h3>
            <c:choose>
              <c:when test="${waste.status=='PENDING'}">  <span class="badge badge-pending">Pending</span></c:when>
              <c:when test="${waste.status=='ASSIGNED'}"> <span class="badge badge-assigned">Assigned</span></c:when>
              <c:when test="${waste.status=='ACCEPTED'}"> <span class="badge badge-accepted">Accepted</span></c:when>
              <c:when test="${waste.status=='EN_ROUTE'}"> <span class="badge badge-en-route">&#x1F69A; En Route</span></c:when>
              <c:when test="${waste.status=='ARRIVED'}">  <span class="badge badge-arrived">&#x1F4CD; Arrived</span></c:when>
              <c:when test="${waste.status=='COLLECTED'}"><span class="badge badge-collected">&#x1F4E6; Collected</span></c:when>
              <c:when test="${waste.status=='DISPOSED'}"> <span class="badge badge-disposed">&#x2713; Disposed</span></c:when>
              <c:when test="${waste.status=='OVERDUE'}">  <span class="badge badge-overdue">&#x1F534; Overdue</span></c:when>
              <c:otherwise><span class="badge badge-pending">${waste.status}</span></c:otherwise>
            </c:choose>
          </div>
          <div class="grid grid-2" style="gap:14px;font-size:0.875rem;">
            <div><div class="stat-label">Waste Type</div><strong>${waste.wasteType}</strong></div>
            <div><div class="stat-label">Quantity</div><strong>${waste.quantityKg} kg</strong></div>
            <div><div class="stat-label">Risk Level</div>
              <c:choose>
                <c:when test="${waste.riskLevel=='HIGH'}"><span class="badge badge-HIGH">&#x1F534; HIGH</span></c:when>
                <c:when test="${waste.riskLevel=='MEDIUM'}"><span class="badge badge-MEDIUM">&#x1F7E0; MEDIUM</span></c:when>
                <c:otherwise><span class="badge badge-LOW">&#x1F7E2; LOW</span></c:otherwise>
              </c:choose>
            </div>
            <div><div class="stat-label">Priority Score</div><span class="mono">${waste.priorityScore}</span></div>
            <c:if test="${not empty collector}">
              <div><div class="stat-label">Assigned Collector</div><strong>${collector.fullName}</strong></div>
              <div><div class="stat-label">Vehicle Number</div><span class="mono">${collector.vehicleNumber}</span></div>
              <div><div class="stat-label">Total Collections Done</div><span class="mono">${collector.totalCollections}</span></div>
            </c:if>
            <c:if test="${not empty waste.description}">
              <div style="grid-column:1/-1;"><div class="stat-label">Notes</div>${waste.description}</div>
            </c:if>
            <c:if test="${not empty waste.delayReason}">
              <div style="grid-column:1/-1;"><div class="stat-label">Delay Reason</div><span style="color:var(--red);">${waste.delayReason}</span></div>
            </c:if>
          </div>

          <%-- Deadline --%>
          <c:if test="${not empty waste.deadline}">
            <div style="margin-top:16px;padding-top:14px;border-top:1px solid var(--border);">
              <div class="flex-between" style="margin-bottom:6px;">
                <span style="font-size:0.75rem;text-transform:uppercase;letter-spacing:0.06em;color:var(--ink-3);">Deadline</span>
                <c:choose>
                  <c:when test="${waste.status=='DISPOSED'||waste.status=='COLLECTED'}">
                    <span class="deadline-chip completed">&#x2713; Completed</span>
                  </c:when>
                  <c:otherwise>
                    <span class="deadline-chip" id="dlChip" data-deadline="${waste.deadline}" data-reported="${waste.reportedAt}"></span>
                  </c:otherwise>
                </c:choose>
              </div>
              <div class="mono text-xs text-muted">${waste.deadline}</div>
            </div>
          </c:if>
        </div>

        <%-- Timeline --%>
        <div class="card">
          <h3 style="margin-bottom:20px;">&#x1F5FA; Waste Journey Timeline</h3>
          <ul class="timeline">

            <li class="timeline-item">
              <div class="timeline-dot done">&#x2713;</div>
              <div class="timeline-content">
                <div class="timeline-label done">&#x2713; Reported</div>
                <div class="timeline-time">${not empty waste.reportedAt?waste.reportedAt.substring(0,16):''} UTC</div>
              </div>
            </li>

            <li class="timeline-item">
              <c:set var="assignedDone" value="${waste.status!='PENDING'}"/>
              <div class="timeline-dot ${assignedDone?'done':''}">
                <c:choose><c:when test="${assignedDone}">&#x2713;</c:when><c:otherwise>2</c:otherwise></c:choose>
              </div>
              <div class="timeline-content">
                <div class="timeline-label ${assignedDone?'done':'pending'}">
                  <c:choose><c:when test="${assignedDone}">&#x2713; Collector Assigned</c:when><c:otherwise>Collector Assignment</c:otherwise></c:choose>
                </div>
                <c:if test="${not empty waste.assignedAt}"><div class="timeline-time">${waste.assignedAt.substring(0,16)} UTC</div></c:if>
                <c:if test="${empty waste.assignedAt and !assignedDone}"><div class="timeline-time text-muted">Waiting for assignment...</div></c:if>
              </div>
            </li>

            <li class="timeline-item">
              <c:set var="acceptedDone" value="${waste.status=='ACCEPTED'||waste.status=='EN_ROUTE'||waste.status=='ARRIVED'||waste.status=='COLLECTED'||waste.status=='DISPOSED'}"/>
              <c:set var="acceptedActive" value="${waste.status=='ASSIGNED'}"/>
              <div class="timeline-dot ${acceptedDone?'done':(acceptedActive?'active':'')}">
                <c:choose><c:when test="${acceptedDone}">&#x2713;</c:when><c:when test="${acceptedActive}">&#x25CF;</c:when><c:otherwise>3</c:otherwise></c:choose>
              </div>
              <div class="timeline-content">
                <div class="timeline-label ${acceptedDone?'done':(acceptedActive?'active':'pending')}">
                  <c:choose><c:when test="${acceptedDone}">&#x2713; Pickup Accepted</c:when><c:when test="${acceptedActive}">Waiting for Collector to Accept</c:when><c:otherwise>Pickup Acceptance</c:otherwise></c:choose>
                </div>
                <c:if test="${not empty waste.acceptedAt}"><div class="timeline-time">${waste.acceptedAt.substring(0,16)} UTC</div></c:if>
              </div>
            </li>

            <li class="timeline-item">
              <c:set var="routeDone" value="${waste.status=='EN_ROUTE'||waste.status=='ARRIVED'||waste.status=='COLLECTED'||waste.status=='DISPOSED'}"/>
              <c:set var="routeActive" value="${waste.status=='ACCEPTED'}"/>
              <div class="timeline-dot ${routeDone?'done':(routeActive?'active':'')}">
                <c:choose><c:when test="${routeDone}">&#x2713;</c:when><c:when test="${routeActive}">&#x25CF;</c:when><c:otherwise>4</c:otherwise></c:choose>
              </div>
              <div class="timeline-content">
                <div class="timeline-label ${routeDone?'done':(routeActive?'active':'pending')}">
                  <c:choose>
                    <c:when test="${waste.status=='EN_ROUTE'}">&#x1F69A; En Route — CURRENTLY TRAVELLING<span class="timeline-current-badge">CURRENT</span></c:when>
                    <c:when test="${routeDone}">&#x2713; Pickup Started</c:when>
                    <c:otherwise>Pickup Start</c:otherwise>
                  </c:choose>
                </div>
                <c:if test="${not empty waste.routeStartedAt}"><div class="timeline-time">${waste.routeStartedAt.substring(0,16)} UTC</div></c:if>
              </div>
            </li>

            <li class="timeline-item">
              <c:set var="arrivedDone" value="${waste.status=='ARRIVED'||waste.status=='COLLECTED'||waste.status=='DISPOSED'}"/>
              <c:set var="arrivedActive" value="${waste.status=='EN_ROUTE'}"/>
              <div class="timeline-dot ${arrivedDone?'done':(arrivedActive?'active':'')}">
                <c:choose><c:when test="${arrivedDone}">&#x2713;</c:when><c:when test="${arrivedActive}">&#x25CF;</c:when><c:otherwise>5</c:otherwise></c:choose>
              </div>
              <div class="timeline-content">
                <div class="timeline-label ${arrivedDone?'done':(arrivedActive?'active':'pending')}">
                  <c:choose>
                    <c:when test="${waste.status=='ARRIVED'}">&#x1F4CD; Arrived at Hospital<span class="timeline-current-badge">CURRENT</span></c:when>
                    <c:when test="${arrivedDone}">&#x2713; Arrived at Hospital</c:when>
                    <c:otherwise>Arrival at Hospital</c:otherwise>
                  </c:choose>
                </div>
                <c:if test="${not empty waste.arrivedAt}"><div class="timeline-time">${waste.arrivedAt.substring(0,16)} UTC</div></c:if>
              </div>
            </li>

            <li class="timeline-item">
              <c:set var="collectedDone" value="${waste.status=='COLLECTED'||waste.status=='DISPOSED'}"/>
              <c:set var="collectedActive" value="${waste.status=='ARRIVED'}"/>
              <div class="timeline-dot ${collectedDone?'done':(collectedActive?'active':'')}">
                <c:choose><c:when test="${collectedDone}">&#x2713;</c:when><c:when test="${collectedActive}">&#x25CF;</c:when><c:otherwise>6</c:otherwise></c:choose>
              </div>
              <div class="timeline-content">
                <div class="timeline-label ${collectedDone?'done':(collectedActive?'active':'pending')}">
                  <c:choose>
                    <c:when test="${waste.status=='COLLECTED'}">&#x1F4E6; Waste Collected<span class="timeline-current-badge">CURRENT</span></c:when>
                    <c:when test="${collectedDone}">&#x2713; Waste Collected</c:when>
                    <c:otherwise>Waste Collection</c:otherwise>
                  </c:choose>
                </div>
                <c:if test="${not empty waste.collectedAt}"><div class="timeline-time">${waste.collectedAt.substring(0,16)} UTC</div></c:if>
              </div>
            </li>

            <li class="timeline-item">
              <c:set var="disposedDone" value="${waste.status=='DISPOSED'}"/>
              <c:set var="disposedActive" value="${waste.status=='COLLECTED'}"/>
              <div class="timeline-dot ${disposedDone?'done':(disposedActive?'active':'')}">
                <c:choose><c:when test="${disposedDone}">&#x2713;</c:when><c:when test="${disposedActive}">&#x25CF;</c:when><c:otherwise>7</c:otherwise></c:choose>
              </div>
              <div class="timeline-content">
                <div class="timeline-label ${disposedDone?'done':(disposedActive?'active':'pending')}">
                  <c:choose>
                    <c:when test="${disposedDone}">&#x2705; Safely Disposed</c:when>
                    <c:when test="${disposedActive}">Awaiting Disposal Confirmation</c:when>
                    <c:otherwise>Safe Disposal</c:otherwise>
                  </c:choose>
                </div>
                <c:if test="${not empty waste.disposedAt}"><div class="timeline-time">${waste.disposedAt.substring(0,16)} UTC</div></c:if>
              </div>
            </li>

          </ul>
        </div>
      </div>

      <%-- RIGHT: map + tracking --%>
      <div>
        <div class="card">
          <div class="card-header">
            <h3>
              <c:choose>
                <c:when test="${trackingActive}">&#x1F4CD; Live Collector Tracking</c:when>
                <c:otherwise>Hospital Location</c:otherwise>
              </c:choose>
            </h3>
            <c:if test="${trackingActive}"><div class="tracking-active-chip">Tracking Active</div></c:if>
          </div>

          <c:if test="${trackingActive}">
            <div style="display:flex;gap:14px;flex-wrap:wrap;margin-bottom:14px;font-size:0.85rem;">
              <div><div class="stat-label">Collector</div><strong id="trkName"><c:if test="${not empty collector}">${collector.fullName}</c:if></strong></div>
              <div><div class="stat-label">Status</div><strong>${waste.status}</strong></div>
              <div><div class="stat-label">Distance</div><strong id="trkDist">Calculating...</strong></div>
              <div><div class="stat-label">ETA</div><strong id="trkEta">Calculating...</strong></div>
            </div>
            <p class="text-xs text-muted" style="margin-bottom:10px;">Updates every 15 seconds while collector is en route. Location provided by collector's device.</p>
          </c:if>

          <div id="detailMap" class="map-container" style="height:380px;"></div>

          <c:if test="${waste.status=='DISPOSED'}">
            <div class="alert alert-success mt-2"><span class="alert-icon">&#x2705;</span>This waste has been safely disposed of. No further tracking needed.</div>
          </c:if>
        </div>
      </div>

    </div>
  </div>
</div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin="anonymous"></script>
<script src="${pageContext.request.contextPath}/js/map.js"></script>
<script>
var CTX='${pageContext.request.contextPath}';
var WASTE_ID='${waste.id}';
var H_LAT=${not empty hospital?hospital.latitude:0};
var H_LNG=${not empty hospital?hospital.longitude:0};
var TRACKING=${trackingActive};
var STATUS='${waste.status}';

function isValidCoord(lat,lng){return lat>=-90&&lat<=90&&lng>=-180&&lng<=180&&!(lat===0&&lng===0);}

/* Deadline chip — stop countdown if disposed/collected */
(function(){
  var chip=document.getElementById('dlChip');
  if(!chip)return;
  var dl=chip.dataset.deadline;
  if(!dl)return;
  if(STATUS==='DISPOSED'||STATUS==='COLLECTED'){chip.className='deadline-chip completed';chip.textContent='Completed';return;}
  function upd(){
    var now=new Date(),diff=new Date(dl)-now;
    if(diff<=0){chip.className='deadline-chip overdue';chip.textContent='OVERDUE';return;}
    var h=Math.floor(diff/3600000),m=Math.floor((diff%3600000)/60000);
    chip.textContent=(h>0?h+'h ':'')+m+'m remaining';
    chip.className='deadline-chip '+(diff<3600000?'approaching':'ok');
  }
  upd();setInterval(upd,30000);
})();

/* Map init */
var detailMap=null,collectorMarker=null,routeLine=null;
function initDetailMap(cLat,cLng){
  var el=document.getElementById('detailMap');
  if(!el||typeof L==='undefined'){if(el)el.innerHTML='<div class="map-placeholder">Map unavailable.</div>';return;}
  if(!isValidCoord(H_LAT,H_LNG)){el.innerHTML='<div class="map-placeholder">&#x26A0; Hospital coordinates are not set or invalid. Please update the hospital profile.</div>';return;}
  el.innerHTML='';
  var center=(cLat&&isValidCoord(cLat,cLng))?[(H_LAT+cLat)/2,(H_LNG+cLng)/2]:[H_LAT,H_LNG];
  detailMap=L.map(el).setView(center,13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'&copy; OSM',maxZoom:19}).addTo(detailMap);
  var hIcon=L.divIcon({html:'<div style="background:#0891B2;color:#fff;width:32px;height:32px;border-radius:8px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:14px;border:2px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.25);">H</div>',className:'',iconSize:[32,32],iconAnchor:[16,16]});
  L.marker([H_LAT,H_LNG],{icon:hIcon}).addTo(detailMap).bindPopup('<strong>${waste.hospitalName}</strong><br>${waste.wasteType} — ${waste.quantityKg} kg');
  if(cLat&&isValidCoord(cLat,cLng))updateCollector(cLat,cLng);
  else if(TRACKING)detailMap.setView([H_LAT,H_LNG],13);
}
function updateCollector(lat,lng){
  if(!detailMap||!isValidCoord(lat,lng))return;
  var cIcon=L.divIcon({html:'<div style="background:#7C3AED;color:#fff;width:32px;height:32px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:16px;border:2px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.25);">&#x1F69A;</div>',className:'',iconSize:[32,32],iconAnchor:[16,16]});
  if(collectorMarker)detailMap.removeLayer(collectorMarker);
  if(routeLine)detailMap.removeLayer(routeLine);
  collectorMarker=L.marker([lat,lng],{icon:cIcon}).addTo(detailMap).bindPopup('Collector location');
  if(isValidCoord(H_LAT,H_LNG)){
    routeLine=L.polyline([[lat,lng],[H_LAT,H_LNG]],{color:'#7C3AED',weight:3,dashArray:'8 5',opacity:0.8}).addTo(detailMap);
    detailMap.fitBounds([[lat,lng],[H_LAT,H_LNG]],{padding:[40,40]});
  }
}

/* Load initial location */
fetch(CTX+'/api/collectorLocation?wasteId='+WASTE_ID).then(function(r){return r.json();}).then(function(d){
  initDetailMap(d.latitude||0,d.longitude||0);
  if(d.distanceKm!=null){var el=document.getElementById('trkDist');if(el)el.textContent=d.distanceKm+' km';}
  if(d.etaMinutes!=null){var el=document.getElementById('trkEta');if(el)el.textContent='~'+d.etaMinutes+' min';}
  if(d.collectorName){var el=document.getElementById('trkName');if(el)el.textContent=d.collectorName;}
}).catch(function(){initDetailMap(0,0);});

/* Poll for live updates */
if(TRACKING){
  setInterval(function(){
    fetch(CTX+'/api/collectorLocation?wasteId='+WASTE_ID).then(function(r){return r.json();}).then(function(d){
      if(!d.tracking)return;
      if(d.latitude&&d.longitude)updateCollector(d.latitude,d.longitude);
      if(d.distanceKm!=null){var el=document.getElementById('trkDist');if(el)el.textContent=d.distanceKm+' km';}
      if(d.etaMinutes!=null){var el=document.getElementById('trkEta');if(el)el.textContent='~'+d.etaMinutes+' min';}
    }).catch(function(){});
  },15000);
}
</script>
</body>
</html>
