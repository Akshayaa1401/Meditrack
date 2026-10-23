<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Hospital Profile — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin="anonymous">
<style>
.loc-btn-group{display:flex;gap:10px;margin-bottom:14px;flex-wrap:wrap;}
.loc-status{font-size:0.82rem;padding:10px 14px;border-radius:var(--r-md);margin-bottom:10px;display:none;}
.loc-status.show{display:block;}
.loc-status.ok{background:var(--green-bg);color:#14532D;}
.loc-status.err{background:var(--red-bg);color:#7F1D1D;}
.loc-status.loading{background:var(--teal-light);color:#164E63;}
#coordDisplay{font-family:var(--font-mono);font-size:0.78rem;color:var(--ink-3);margin-top:6px;min-height:18px;}
</style>
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left">
      <h1>Hospital Profile</h1>
      <p>Set your hospital's name, address and location on the map.</p>
    </div>
    <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
  </div>
  <div class="page-body">
    <c:if test="${not empty error}">
      <div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>${error}</div>
    </c:if>
    <div class="grid grid-2" style="align-items:start;">
      <%-- Form --%>
      <div class="card">
        <h3 style="margin-bottom:18px;">Hospital Details</h3>
        <form id="hospitalForm" method="post" action="${pageContext.request.contextPath}/hospital/register">
          <div class="form-group">
            <label class="form-label" for="name">Hospital name <span style="color:var(--red)">*</span></label>
            <input class="form-control" type="text" id="name" name="name" required placeholder="e.g. Apollo Hospital Hyderabad">
          </div>
          <div class="form-group">
            <label class="form-label" for="address">Full address <span style="color:var(--red)">*</span></label>
            <textarea class="form-control" id="address" name="address" required placeholder="Street, City, State, PIN"></textarea>
          </div>
          <div class="grid grid-2" style="gap:12px;">
            <div class="form-group">
              <label class="form-label" for="contactPerson">Contact person</label>
              <input class="form-control" type="text" id="contactPerson" name="contactPerson" placeholder="Dr. Name">
            </div>
            <div class="form-group">
              <label class="form-label" for="contactPhone">Contact phone</label>
              <input class="form-control" type="tel" id="contactPhone" name="contactPhone" placeholder="+91 98765 43210">
            </div>
          </div>

          <%-- Hidden lat/lng — filled by map or GPS --%>
          <input type="hidden" id="latitude"  name="latitude">
          <input type="hidden" id="longitude" name="longitude">

          <div class="form-group">
            <label class="form-label">Hospital Location <span style="color:var(--red)">*</span></label>
            <div class="loc-btn-group">
              <button type="button" class="btn btn-primary" id="gpsBtn" onclick="useCurrentLocation()">
                &#x1F4CD; Use My Current Location
              </button>
              <button type="button" class="btn btn-secondary" onclick="scrollToMap()">
                &#x1F5FA; Pick on Map Below
              </button>
            </div>
            <div class="loc-status" id="locStatus"></div>
            <div id="coordDisplay">No location selected yet. Use one of the buttons above.</div>
          </div>

          <button type="submit" class="btn btn-primary btn-block btn-lg" id="submitBtn" disabled>Save Hospital Profile</button>
          <p class="form-help" style="text-align:center;margin-top:8px;">Location must be set before saving.</p>
        </form>
      </div>

      <%-- Map --%>
      <div class="card" id="mapCard">
        <h3 style="margin-bottom:10px;">&#x1F5FA; Click map to set location</h3>
        <p class="form-help" style="margin-bottom:12px;">Zoom in to your hospital and click the exact building. Or use "Use My Current Location" on the left — it's faster.</p>
        <div id="pickerMap" class="map-container" style="height:420px;"></div>
        <p class="form-help" style="margin-top:8px;">&#x1F4A1; Tip: You can drag the pin after placing it to fine-tune the position.</p>
      </div>
    </div>
  </div>
</div>

<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin="anonymous"></script>
<script>
var map = null, marker = null;

/* ── Init map ─────────────────────────────────────────── */
window.addEventListener('load', function() {
  var mapEl = document.getElementById('pickerMap');
  if (typeof L === 'undefined') {
    mapEl.innerHTML = '<div class="map-placeholder">Map unavailable (no internet). Use "Use My Current Location" instead.</div>';
    return;
  }
  /* Default centre: Hyderabad — change if your users are elsewhere */
  map = L.map(mapEl).setView([17.3850, 78.4867], 12);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors', maxZoom: 19
  }).addTo(map);

  map.on('click', function(e) { placePin(e.latlng.lat, e.latlng.lng, 'map'); });
});

/* ── Place / move pin ────────────────────────────────── */
function placePin(lat, lng, source) {
  if (marker) map.removeLayer(marker);
  marker = L.marker([lat, lng], { draggable: true }).addTo(map)
    .bindPopup('Hospital location — drag to adjust').openPopup();
  marker.on('dragend', function() {
    var p = marker.getLatLng();
    setCoords(p.lat, p.lng, 'dragged');
  });
  setCoords(lat, lng, source);
  map.setView([lat, lng], Math.max(map.getZoom(), 15));
}

/* ── Store coords + update UI ────────────────────────── */
function setCoords(lat, lng, source) {
  var la = parseFloat(lat.toFixed(6));
  var ln = parseFloat(lng.toFixed(6));
  document.getElementById('latitude').value  = la;
  document.getElementById('longitude').value = ln;
  document.getElementById('submitBtn').disabled = false;
  var src = source === 'gps' ? '&#x1F4CD; From your device GPS'
          : source === 'map' ? '&#x1F5FA; Selected on map'
          : '&#x270E; Dragged to position';
  document.getElementById('coordDisplay').innerHTML =
    src + ' &nbsp;|&nbsp; <strong>' + la + ', ' + ln + '</strong>';
  showStatus('Location set: ' + la + ', ' + ln + '. You can drag the map pin to adjust.', 'ok');
}

/* ── Use current GPS location ────────────────────────── */
function useCurrentLocation() {
  if (!navigator.geolocation) {
    showStatus('Your browser does not support location access. Please click on the map instead.', 'err');
    return;
  }
  var btn = document.getElementById('gpsBtn');
  btn.disabled = true;
  btn.textContent = 'Getting location...';
  showStatus('Requesting your device location... please allow when the browser asks.', 'loading');

  navigator.geolocation.getCurrentPosition(
    function(pos) {
      btn.disabled = false;
      btn.innerHTML = '&#x1F4CD; Use My Current Location';
      var lat = pos.coords.latitude, lng = pos.coords.longitude;
      if (map) placePin(lat, lng, 'gps');
      else setCoords(lat, lng, 'gps');
      scrollToMap();
    },
    function(err) {
      btn.disabled = false;
      btn.innerHTML = '&#x1F4CD; Use My Current Location';
      var msg = 'Location access was denied. ';
      if (err.code === 1) msg += 'Please allow location access in your browser settings, or click on the map to set the location manually.';
      else if (err.code === 2) msg += 'Location unavailable right now. Please click on the map instead.';
      else msg += 'Request timed out. Please click on the map instead.';
      showStatus(msg, 'err');
    },
    { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 }
  );
}

function scrollToMap() {
  document.getElementById('mapCard').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function showStatus(msg, type) {
  var el = document.getElementById('locStatus');
  el.innerHTML = msg;
  el.className = 'loc-status show ' + type;
}

/* ── Prevent submit without location ────────────────── */
document.getElementById('hospitalForm').addEventListener('submit', function(e) {
  var lat = document.getElementById('latitude').value;
  var lng = document.getElementById('longitude').value;
  if (!lat || !lng) {
    e.preventDefault();
    showStatus('Please set the hospital location first — use "Use My Current Location" or click on the map.', 'err');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
});
</script>
</body>
</html>
