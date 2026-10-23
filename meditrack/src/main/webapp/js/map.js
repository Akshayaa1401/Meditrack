/**
 * map.js
 * -------
 * Leaflet-based map integration (OpenStreetMap tiles - no API key needed).
 * Provides three reusable initializers:
 *
 *   initLocationPicker(elementId, latInputId, lngInputId, [initialLat, initialLng])
 *     -> used on Hospital Registration page: click anywhere to drop a pin
 *        and auto-fill the latitude/longitude hidden form fields.
 *
 *   initOverviewMap(elementId, options)
 *     -> used on Admin dashboard / hospital map view: shows ALL hospitals
 *        and ALL pending/assigned waste points, colored by risk level.
 *        Clicking a marker opens a popup with hospital + waste details.
 *
 *   initRouteMap(elementId, routeStops, startLat, startLng, [options])
 *     -> used on Collector dashboard: draws the optimized route as a
 *        polyline connecting ordered stops, numbered markers, distinct
 *        styling for emergency-inserted stops.
 */

const RISK_COLORS = { HIGH: '#C0392B', MEDIUM: '#B5760A', LOW: '#2E8B57' };

/**
 * Shows a clear, friendly message inside a map container when the map
 * itself can't be used (Leaflet failed to load from the CDN, no internet,
 * etc). Used instead of leaving a blank/stuck "Loading..." box forever.
 */
function showMapFallback(elementId, message) {
    const el = document.getElementById(elementId);
    if (el) {
        el.innerHTML = `<div class="empty-state"><strong>Map unavailable</strong><p class="text-muted mt-1">${message}</p></div>`;
    }
}

/** True if the Leaflet library actually loaded. Call this before any L.* usage. */
function leafletAvailable() {
    return typeof L !== 'undefined';
}

function initLocationPicker(elementId, latInputId, lngInputId, initialLat, initialLng) {
    if (!leafletAvailable()) {
        showMapFallback(elementId, 'The map library could not load (likely no internet connection right now). You can still type the latitude and longitude directly into the fields above.');
        return null;
    }

    const lat = initialLat || 17.9784; // sensible default center (Andhra Pradesh region)
    const lng = initialLng || 79.5941;

    const map = L.map(elementId).setView([lat, lng], 6);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(map);

    let marker = null;
    if (initialLat && initialLng) {
        marker = L.marker([lat, lng]).addTo(map);
    }

    function setPoint(latlng) {
        if (marker) map.removeLayer(marker);
        marker = L.marker(latlng).addTo(map);
        document.getElementById(latInputId).value = latlng.lat.toFixed(6);
        document.getElementById(lngInputId).value = latlng.lng.toFixed(6);
    }

    map.on('click', (e) => setPoint(e.latlng));

    return map;
}

function riskIcon(riskLevel, isEmergency) {
    const color = RISK_COLORS[riskLevel] || '#178A72';
    const pulse = isEmergency ? 'animation: pulse 1.2s infinite;' : '';
    const html = `<div style="
        width: 18px; height: 18px; border-radius: 50%;
        background: ${color}; border: 2px solid white;
        box-shadow: 0 0 0 2px ${color}55; ${pulse}
    "></div>`;
    return L.divIcon({ html, className: '', iconSize: [18, 18], iconAnchor: [9, 9] });
}

function hospitalIcon() {
    const html = `<div style="
        width: 22px; height: 22px; border-radius: 6px;
        background: #0F5C4C; border: 2px solid white;
        display:flex; align-items:center; justify-content:center;
        color:white; font-size:12px; font-weight:700;
    ">H</div>`;
    return L.divIcon({ html, className: '', iconSize: [22, 22], iconAnchor: [11, 11] });
}

function initOverviewMap(elementId, centerLat, centerLng) {
    if (!leafletAvailable()) {
        showMapFallback(elementId, 'The map library could not load (likely no internet connection right now). Use the tables below for the same information.');
        return null;
    }

    const map = L.map(elementId).setView([centerLat || 17.9784, centerLng || 79.5941], 6);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(map);

    fetch(`${window.MEDITRACK_CTX || ''}/api/mapData`)
        .then(r => r.json())
        .then(data => {
            const bounds = [];

            (data.hospitals || []).forEach(h => {
                const m = L.marker([h.latitude, h.longitude], { icon: hospitalIcon() }).addTo(map);
                m.bindPopup(`<strong>${escapeHtml(h.name)}</strong><br>${escapeHtml(h.address || '')}`);
                bounds.push([h.latitude, h.longitude]);
            });

            (data.wastePoints || []).forEach(w => {
                const m = L.marker([w.latitude, w.longitude], { icon: riskIcon(w.riskLevel, w.emergency) }).addTo(map);
                m.bindPopup(`
                    <strong>${escapeHtml(w.hospitalName)}</strong><br>
                    Waste: ${escapeHtml(w.wasteType)} (${w.quantityKg} kg)<br>
                    Risk: <span style="color:${RISK_COLORS[w.riskLevel]}">${w.riskLevel}</span><br>
                    Status: ${w.status}<br>
                    Priority score: ${w.priorityScore}
                    ${w.emergency ? '<br><strong style="color:#E0241B">⚠ EMERGENCY</strong>' : ''}
                `);
                bounds.push([w.latitude, w.longitude]);
            });

            if (bounds.length > 0) map.fitBounds(bounds, { padding: [30, 30] });
        })
        .catch(err => console.error('Failed to load map data', err));

    return map;
}

/**
 * Draws an ordered route on a Leaflet map.
 * routeStops: array of {wasteId, hospitalName, latitude, longitude, priorityScore, emergency}
 */
function initRouteMap(elementId, routeStops, startLat, startLng) {
    if (!leafletAvailable()) {
        showMapFallback(elementId, 'The map library could not load (likely no internet connection right now). Use the ordered stop list to the right — it has everything you need: hospital names, priority, and the Mark Collected/Disposed buttons.');
        return null;
    }

    const center = routeStops.length ? [routeStops[0].latitude, routeStops[0].longitude] : [startLat, startLng];
    const map = L.map(elementId).setView(center, 7);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(map);

    const points = [[startLat, startLng]];
    L.marker([startLat, startLng]).addTo(map).bindPopup('Collector start location');

    routeStops.forEach((stop, idx) => {
        points.push([stop.latitude, stop.longitude]);
        const color = stop.emergency ? '#E0241B' : '#0F5C4C';
        const html = `<div style="
            width: 26px; height: 26px; border-radius: 50%;
            background: ${color}; color: white; font-weight: 700; font-size: 12px;
            display:flex; align-items:center; justify-content:center;
            border: 2px solid white; box-shadow: 0 1px 4px rgba(0,0,0,0.3);
        ">${idx + 1}</div>`;
        const icon = L.divIcon({ html, className: '', iconSize: [26, 26], iconAnchor: [13, 13] });
        const m = L.marker([stop.latitude, stop.longitude], { icon }).addTo(map);
        m.bindPopup(`
            <strong>Stop ${idx + 1}: ${escapeHtml(stop.hospitalName)}</strong><br>
            Priority score: ${stop.priorityScore}
            ${stop.emergency ? '<br><strong style="color:#E0241B">⚠ EMERGENCY INSERTION</strong>' : ''}
        `);
    });

    if (points.length > 1) {
        L.polyline(points, { color: '#178A72', weight: 4, opacity: 0.75, dashArray: '6 6' }).addTo(map);
        map.fitBounds(points, { padding: [30, 30] });
    }

    return map;
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
