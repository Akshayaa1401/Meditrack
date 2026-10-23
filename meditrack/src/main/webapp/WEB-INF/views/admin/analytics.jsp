<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Analytics — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left"><h1>Analytics</h1><p>Performance metrics calculated from real disposal records.</p></div>
    <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
  </div>
  <div class="page-body">

    <%-- Key performance stats --%>
    <div class="stats-grid">
      <div class="stat-card green"><div class="stat-label">Total Collected</div><div class="stat-value">${totalKgCollected} kg</div></div>
      <div class="stat-card"><div class="stat-label">Total Disposals</div><div class="stat-value">${totalDisposalsCount}</div></div>
      <div class="stat-card red"><div class="stat-label">High-Risk Waste</div><div class="stat-value red">${highRiskKg} kg</div></div>
      <div class="stat-card amber"><div class="stat-label">High-Risk Count</div><div class="stat-value amber">${highRiskCount}</div></div>
      <div class="stat-card green"><div class="stat-label">On-Time Rate</div><div class="stat-value" style="font-size:1.6rem;">${onTimePct}%</div></div>
      <div class="stat-card ${overdueFinished>0?'red':''}"><div class="stat-label">Overdue Completed</div><div class="stat-value ${overdueFinished>0?'red':''}">${overdueFinished}</div></div>
      <div class="stat-card blue"><div class="stat-label">Avg Response Time</div><div class="stat-value" style="font-size:1.4rem;">${avgResponseMins}m</div></div>
      <div class="stat-card"><div class="stat-label">Records Measured</div><div class="stat-value" style="font-size:1.4rem;">${responseCount}</div></div>
    </div>

    <%-- On-time progress bar --%>
    <div class="card mt-2" style="margin-bottom:16px;">
      <div class="card-header">
        <h3>Collection Performance</h3>
        <span style="font-size:0.82rem;color:var(--ink-3);">${totalFinished} total completed collections</span>
      </div>
      <div style="margin-bottom:10px;">
        <div class="flex-between" style="margin-bottom:6px;font-size:0.85rem;">
          <span>On-time collections</span>
          <strong style="color:var(--green);">${onTimeCount} (${onTimePct}%)</strong>
        </div>
        <div style="height:12px;border-radius:999px;background:var(--border);overflow:hidden;">
          <div style="height:100%;width:${onTimePct}%;background:${onTimePct>=80?'var(--green)':(onTimePct>=50?'var(--amber)':'var(--red)')};border-radius:999px;transition:width 0.5s;"></div>
        </div>
        <div class="flex-between" style="margin-top:6px;font-size:0.78rem;color:var(--ink-3);">
          <span>&#x1F7E2; On time: ${onTimeCount}</span>
          <span>&#x1F534; Overdue: ${overdueFinished}</span>
        </div>
      </div>
      <c:if test="${avgResponseMins > 0}">
        <div style="font-size:0.85rem;color:var(--ink-2);padding-top:10px;border-top:1px solid var(--border);">
          Average time from waste reported to collector accepted: <strong class="mono">${avgResponseMins} minutes</strong>
          <span style="color:var(--ink-3);font-size:0.75rem;">(based on ${responseCount} records)</span>
        </div>
      </c:if>
    </div>

    <%-- Charts row --%>
    <div class="grid grid-2 mt-2">
      <div class="card">
        <div class="card-header"><h3>Waste Category Distribution</h3></div>
        <canvas id="catChart" height="240"></canvas>
      </div>
      <div class="card">
        <div class="card-header"><h3>Monthly Collection (kg)</h3></div>
        <canvas id="monthChart" height="240"></canvas>
      </div>
    </div>

    <%-- On-time donut --%>
    <div class="grid grid-2 mt-2">
      <div class="card">
        <div class="card-header"><h3>On-Time vs Overdue</h3></div>
        <canvas id="ontimeChart" height="240"></canvas>
      </div>
      <div class="card">
        <div class="card-header"><h3>Collector Performance</h3></div>
        <c:if test="${empty collectorPerformance}">
          <div class="empty-state"><h3>No data yet</h3></div>
        </c:if>
        <c:if test="${not empty collectorPerformance}">
          <div class="table-wrap"><table>
            <thead><tr><th>Collector</th><th>Vehicle</th><th>Disposals</th><th>Status</th></tr></thead>
            <tbody>
            <c:forEach var="c" items="${collectorPerformance}">
              <tr>
                <td><strong>${c.fullName}</strong></td>
                <td class="mono text-sm">${c.vehicleNumber}</td>
                <td class="mono">${c.totalCollections}</td>
                <td><c:choose>
                  <c:when test="${c.status=='ON_ROUTE'}"><span class="badge badge-en-route">&#x1F69A; On Route</span></c:when>
                  <c:when test="${c.status=='AVAILABLE'}"><span class="badge badge-disposed">Available</span></c:when>
                  <c:otherwise><span class="badge badge-pending">${c.status}</span></c:otherwise>
                </c:choose></td>
              </tr>
            </c:forEach>
            </tbody>
          </table></div>
        </c:if>
      </div>
    </div>

  </div>
</div>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.4/chart.umd.min.js"></script>
<script>
var catLabels=[], catVals=[];
<c:forEach var="cat" items="${categoryDistribution}">catLabels.push('${cat._id}');catVals.push(${cat.totalKg});</c:forEach>

var mLabels=[], mVals=[];
<c:forEach var="m" items="${monthlyReport}">mLabels.push('${m._id}');mVals.push(${m.totalKg});</c:forEach>

Chart.defaults.font.family = "'Inter', sans-serif";
Chart.defaults.color = '#64748B';

new Chart(document.getElementById('catChart'), {
  type: 'doughnut',
  data: { labels: catLabels, datasets: [{ data: catVals, backgroundColor: ['#0891B2','#16A34A','#DC2626','#D97706','#7C3AED','#64748B'], borderWidth: 0 }] },
  options: { plugins: { legend: { position: 'bottom', labels: { padding: 16, boxWidth: 12 } } }, cutout: '65%' }
});

new Chart(document.getElementById('monthChart'), {
  type: 'bar',
  data: { labels: mLabels, datasets: [{ label: 'kg collected', data: mVals, backgroundColor: '#0891B2', borderRadius: 6, borderSkipped: false }] },
  options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, grid: { color: '#F1F5F9' } }, x: { grid: { display: false } } } }
});

new Chart(document.getElementById('ontimeChart'), {
  type: 'doughnut',
  data: {
    labels: ['On Time', 'Overdue'],
    datasets: [{ data: [${onTimeCount}, ${overdueFinished}], backgroundColor: ['#16A34A','#DC2626'], borderWidth: 0 }]
  },
  options: {
    cutout: '65%',
    plugins: {
      legend: { position: 'bottom', labels: { padding: 16, boxWidth: 12 } },
      tooltip: { callbacks: { label: function(c){ return c.label+': '+c.raw+' ('+Math.round(c.raw/(${onTimeCount}+${overdueFinished}||1)*100)+'%)'; } } }
    }
  }
});
</script>
</body>
</html>
