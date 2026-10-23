<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Hospital Dashboard — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<style>
.overdue-card{border:2px solid var(--red);border-radius:var(--r-lg);padding:18px 20px;background:var(--red-bg);margin-bottom:12px;}
.overdue-card-title{font-weight:700;color:var(--red);font-size:1rem;margin-bottom:8px;}
.reassign-btn{display:inline-flex;align-items:center;gap:6px;padding:8px 16px;background:var(--red);color:#fff;border:none;border-radius:var(--r-md);font-weight:600;font-size:0.85rem;cursor:pointer;}
.reassign-btn:disabled{opacity:0.6;cursor:not-allowed;}
.reassign-requested{display:inline-flex;align-items:center;gap:6px;padding:6px 12px;background:var(--amber-bg);color:var(--amber);border-radius:var(--r-md);font-size:0.82rem;font-weight:600;}
</style>
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left">
      <h1>${not empty hospital ? hospital.name : 'Hospital Dashboard'}</h1>
      <p>Track your waste requests from submission to safe disposal.</p>
    </div>
    <div class="topbar-actions">
      <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
      <a href="${pageContext.request.contextPath}/hospital/submitWaste" class="btn btn-primary">+ Report Waste</a>
    </div>
  </div>
  <div class="page-body">

    <c:if test="${param.submitted=='true'}"><div class="alert alert-success"><span class="alert-icon">&#x2713;</span>Waste request submitted. Collectors have been notified.</div></c:if>
    <c:if test="${param.registered=='true'}"><div class="alert alert-success"><span class="alert-icon">&#x2713;</span>Hospital profile saved.</div></c:if>
    <c:if test="${param.reassignRequested=='true'}"><div class="alert alert-warning"><span class="alert-icon">&#x26A0;</span>Reassignment request sent to admin. An available collector will be assigned shortly.</div></c:if>

    <%-- Notifications --%>
    <c:if test="${not empty notifications}">
      <c:forEach var="n" items="${notifications}">
        <div class="alert alert-info"><span class="alert-icon">&#x1F514;</span>${n.message}</div>
      </c:forEach>
    </c:if>

    <c:if test="${empty hospital}">
      <div class="card"><div class="empty-state">
        <div class="empty-state-icon">&#x1F3E5;</div>
        <h3>No hospital profile yet</h3>
        <p>Register your hospital's name and location to start reporting waste.</p>
        <a href="${pageContext.request.contextPath}/hospital/register" class="btn btn-primary mt-2">Register Hospital</a>
      </div></div>
    </c:if>

    <c:if test="${not empty hospital}">
      <%-- Counts --%>
      <c:set var="pCount" value="0"/><c:set var="aCount" value="0"/>
      <c:set var="dCount" value="0"/><c:set var="oCount" value="0"/>
      <c:forEach var="w" items="${wasteRecords}">
        <c:if test="${w.status=='PENDING'}"><c:set var="pCount" value="${pCount+1}"/></c:if>
        <c:if test="${w.status=='ASSIGNED'||w.status=='ACCEPTED'||w.status=='EN_ROUTE'||w.status=='ARRIVED'||w.status=='COLLECTED'}"><c:set var="aCount" value="${aCount+1}"/></c:if>
        <c:if test="${w.status=='DISPOSED'}"><c:set var="dCount" value="${dCount+1}"/></c:if>
        <c:if test="${w.status=='OVERDUE'}"><c:set var="oCount" value="${oCount+1}"/></c:if>
      </c:forEach>

      <div class="stats-grid">
        <div class="stat-card blue"><div class="stat-label">Pending Pickup</div><div class="stat-value">${pCount}</div></div>
        <div class="stat-card"><div class="stat-label">In Progress</div><div class="stat-value">${aCount}</div></div>
        <div class="stat-card green"><div class="stat-label">Safely Disposed</div><div class="stat-value">${dCount}</div></div>
        <div class="stat-card ${oCount>0?'red':''}"><div class="stat-label">Overdue</div><div class="stat-value ${oCount>0?'red':''}">${oCount}</div></div>
      </div>

      <%-- OVERDUE section with Reassignment button --%>
      <c:set var="hasOverdue" value="false"/>
      <c:forEach var="w" items="${wasteRecords}">
        <c:if test="${w.status=='OVERDUE'}"><c:set var="hasOverdue" value="true"/></c:if>
        <c:if test="${w.emergency and (w.status=='PENDING' or w.status=='ASSIGNED')}"><c:set var="hasOverdue" value="true"/></c:if>
      </c:forEach>
      <c:if test="${hasOverdue}">
        <div class="section-header mt-3">
          <h2>&#x1F534; Needs Attention</h2>
          <span class="section-count danger">${oCount}</span>
        </div>
        <c:forEach var="w" items="${wasteRecords}">
          <c:if test="${w.status=='OVERDUE' or (w.emergency and (w.status=='PENDING' or w.status=='ASSIGNED'))}">
          <div class="overdue-card">
            <div class="overdue-card-title">
              <c:choose>
                <c:when test="${w.status=='OVERDUE'}">&#x1F534; OVERDUE — ${w.wasteType}</c:when>
                <c:otherwise>&#x1F6A8; EMERGENCY NOT ACCEPTED — ${w.wasteType}</c:otherwise>
              </c:choose>
            </div>
            <div style="display:flex;gap:16px;flex-wrap:wrap;font-size:0.85rem;margin-bottom:12px;">
              <div><span style="color:var(--ink-3);">Quantity:</span> <strong>${w.quantityKg} kg</strong></div>
              <div><span style="color:var(--ink-3);">Risk:</span>
                <c:choose>
                  <c:when test="${w.riskLevel=='HIGH'}"><span class="badge badge-HIGH">HIGH</span></c:when>
                  <c:when test="${w.riskLevel=='MEDIUM'}"><span class="badge badge-MEDIUM">MEDIUM</span></c:when>
                  <c:otherwise><span class="badge badge-LOW">LOW</span></c:otherwise>
                </c:choose>
              </div>
              <div><span style="color:var(--ink-3);">Status:</span>
                <c:choose>
                  <c:when test="${w.status=='OVERDUE'}"><span class="badge badge-overdue">OVERDUE</span></c:when>
                  <c:when test="${w.status=='PENDING'}"><span class="badge badge-pending">Pending — no collector yet</span></c:when>
                  <c:when test="${w.status=='ASSIGNED'}"><span class="badge badge-assigned">Assigned — awaiting acceptance</span></c:when>
                </c:choose>
              </div>
              <div><span style="color:var(--ink-3);">Reported:</span> <span class="mono">${not empty w.reportedAt?w.reportedAt.substring(0,16):''}</span></div>
              <c:if test="${not empty w.deadline}">
                <div><span style="color:var(--ink-3);">Deadline:</span> <span class="mono" style="color:var(--red);">${w.deadline.substring(0,16)}</span></div>
              </c:if>
              <c:if test="${not empty collectorNames[w.assignedCollectorId]}">
                <div><span style="color:var(--ink-3);">Assigned to:</span> <strong>${collectorNames[w.assignedCollectorId]}</strong></div>
              </c:if>
            </div>
            <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;">
              <%-- Check if reassignment already requested --%>
              <c:choose>
                <c:when test="${w.reassignmentRequested}">
                  <span class="reassign-requested">&#x23F3; Reassignment requested — waiting for admin</span>
                </c:when>
                <c:otherwise>
                  <form method="post" action="${pageContext.request.contextPath}/hospital/requestReassignment" style="display:inline;">
                    <input type="hidden" name="wasteId" value="${w.id}">
                    <button type="submit" class="reassign-btn"
                            onclick="return confirm('Request a new/different collector? Admin will be notified immediately.')">
                      <c:choose>
                        <c:when test="${w.status=='OVERDUE'}">&#x1F504; Request Reassignment</c:when>
                        <c:otherwise>&#x1F6A8; Escalate — Assign Collector Now</c:otherwise>
                      </c:choose>
                    </button>
                  </form>
                </c:otherwise>
              </c:choose>
              <a href="${pageContext.request.contextPath}/waste/detail?id=${w.id}" class="btn btn-secondary btn-sm">View Details</a>
            </div>
            <p style="font-size:0.75rem;color:var(--ink-3);margin-top:8px;">
              <c:choose>
                <c:when test="${w.status=='OVERDUE'}">This waste was not collected within its deadline. Requesting reassignment alerts your admin to assign another available collector. Original history is preserved.</c:when>
                <c:otherwise>This emergency request has not been accepted by a collector. Escalating alerts your admin to immediately assign an available collector.</c:otherwise>
              </c:choose>
            </p>
          </div>
          </c:if>
        </c:forEach>
      </c:if>

      <%-- Active collections live status --%>
      <c:set var="hasActive" value="false"/>
      <c:forEach var="w" items="${wasteRecords}">
        <c:if test="${w.status=='ASSIGNED'||w.status=='ACCEPTED'||w.status=='EN_ROUTE'||w.status=='ARRIVED'||w.status=='COLLECTED'}">
          <c:set var="hasActive" value="true"/>
        </c:if>
      </c:forEach>
      <c:if test="${hasActive}">
        <div class="section-header mt-3">
          <h2>Live Pickup Status</h2>
          <span class="section-count active">${aCount}</span>
        </div>
        <c:forEach var="w" items="${wasteRecords}">
          <c:if test="${w.status=='ASSIGNED'||w.status=='ACCEPTED'||w.status=='EN_ROUTE'||w.status=='ARRIVED'||w.status=='COLLECTED'}">
          <div class="card ${w.emergency?'emergency-card':''}" style="margin-bottom:14px;">
            <div class="flex-between" style="margin-bottom:14px;">
              <div>
                <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
                  <h3 style="margin:0;">${w.wasteType}</h3>
                  <c:choose>
                    <c:when test="${w.riskLevel=='HIGH'}"><span class="badge badge-HIGH">&#x1F534; HIGH</span></c:when>
                    <c:when test="${w.riskLevel=='MEDIUM'}"><span class="badge badge-MEDIUM">&#x1F7E0; MEDIUM</span></c:when>
                    <c:otherwise><span class="badge badge-LOW">&#x1F7E2; LOW</span></c:otherwise>
                  </c:choose>
                  <c:if test="${w.emergency}"><span class="badge badge-emergency">&#x1F6A8; EMERGENCY</span></c:if>
                </div>
                <div style="font-size:0.82rem;color:var(--ink-3);margin-top:4px;">
                  ${w.quantityKg} kg
                  <c:if test="${not empty collectorNames[w.assignedCollectorId]}"> &nbsp;|&nbsp; Collector: <strong>${collectorNames[w.assignedCollectorId]}</strong></c:if>
                </div>
              </div>
              <a href="${pageContext.request.contextPath}/waste/detail?id=${w.id}" class="btn btn-secondary btn-sm">
                <c:choose>
                  <c:when test="${w.status=='EN_ROUTE'||w.status=='ARRIVED'}">&#x1F4CD; Track Collector</c:when>
                  <c:otherwise>View Journey</c:otherwise>
                </c:choose>
              </a>
            </div>
            <%-- Progress steps --%>
            <div class="status-progress">
              <div class="status-step done"><div class="step-check done">&#x2713;</div><div class="step-label done">Reported</div><div class="step-time">${not empty w.reportedAt?w.reportedAt.substring(0,16):''}</div></div>
              <div class="status-step ${w.status!='PENDING'?'done':'pending'}">
                <div class="step-check ${w.status!='PENDING'?'done':''}"><c:choose><c:when test="${w.status!='PENDING'}">&#x2713;</c:when><c:otherwise>2</c:otherwise></c:choose></div>
                <div class="step-label ${w.status!='PENDING'?'done':'pending'}">Collector Assigned</div>
              </div>
              <div class="status-step ${w.status=='ACCEPTED'||w.status=='EN_ROUTE'||w.status=='ARRIVED'||w.status=='COLLECTED'?'done':(w.status=='ASSIGNED'?'current':'pending')}">
                <div class="step-check ${w.status=='ACCEPTED'||w.status=='EN_ROUTE'||w.status=='ARRIVED'||w.status=='COLLECTED'?'done':(w.status=='ASSIGNED'?'current':'')}">
                  <c:choose><c:when test="${w.status=='ACCEPTED'||w.status=='EN_ROUTE'||w.status=='ARRIVED'||w.status=='COLLECTED'}">&#x2713;</c:when><c:when test="${w.status=='ASSIGNED'}">&#x25CF;</c:when><c:otherwise>3</c:otherwise></c:choose>
                </div>
                <div class="step-label ${w.status=='ACCEPTED'||w.status=='EN_ROUTE'||w.status=='ARRIVED'||w.status=='COLLECTED'?'done':(w.status=='ASSIGNED'?'current':'pending')}">Pickup Accepted</div>
              </div>
              <div class="status-step ${w.status=='EN_ROUTE'?'current':(w.status=='ARRIVED'||w.status=='COLLECTED'?'done':'pending')}">
                <div class="step-check ${w.status=='EN_ROUTE'?'current':(w.status=='ARRIVED'||w.status=='COLLECTED'?'done':'')}">
                  <c:choose><c:when test="${w.status=='ARRIVED'||w.status=='COLLECTED'}">&#x2713;</c:when><c:when test="${w.status=='EN_ROUTE'}">&#x1F69A;</c:when><c:otherwise>4</c:otherwise></c:choose>
                </div>
                <div class="step-label ${w.status=='EN_ROUTE'?'current':(w.status=='ARRIVED'||w.status=='COLLECTED'?'done':'pending')}">
                  <c:choose><c:when test="${w.status=='EN_ROUTE'}">&#x1F7E2; EN ROUTE</c:when><c:otherwise>En Route</c:otherwise></c:choose>
                </div>
              </div>
              <div class="status-step ${w.status=='ARRIVED'?'current':(w.status=='COLLECTED'?'done':'pending')}">
                <div class="step-check ${w.status=='ARRIVED'?'current':(w.status=='COLLECTED'?'done':'')}">
                  <c:choose><c:when test="${w.status=='COLLECTED'}">&#x2713;</c:when><c:when test="${w.status=='ARRIVED'}">&#x1F4CD;</c:when><c:otherwise>5</c:otherwise></c:choose>
                </div>
                <div class="step-label ${w.status=='ARRIVED'?'current':(w.status=='COLLECTED'?'done':'pending')}">
                  <c:choose><c:when test="${w.status=='ARRIVED'}">&#x1F7E2; ARRIVED</c:when><c:otherwise>Arrived at Hospital</c:otherwise></c:choose>
                </div>
              </div>
              <div class="status-step ${w.status=='COLLECTED'?'current':'pending'}">
                <div class="step-check ${w.status=='COLLECTED'?'current':''}"><c:choose><c:when test="${w.status=='COLLECTED'}">&#x1F4E6;</c:when><c:otherwise>6</c:otherwise></c:choose></div>
                <div class="step-label ${w.status=='COLLECTED'?'current':'pending'}"><c:choose><c:when test="${w.status=='COLLECTED'}">&#x1F7E2; WASTE COLLECTED</c:when><c:otherwise>Waste Collected</c:otherwise></c:choose></div>
              </div>
              <div class="status-step pending"><div class="step-check">7</div><div class="step-label pending">Safely Disposed</div></div>
            </div>
          </div>
          </c:if>
        </c:forEach>
      </c:if>

      <%-- All records table --%>
      <div class="section-header mt-3">
        <h2>All Waste Records</h2>
        <span class="section-count">${wasteRecords.size()}</span>
      </div>
      <div class="card">
        <c:if test="${empty wasteRecords}">
          <div class="empty-state"><div class="empty-state-icon">&#x1F4CB;</div><h3>No records yet</h3><p>Click "+ Report Waste" to submit your first collection request.</p></div>
        </c:if>
        <c:if test="${not empty wasteRecords}">
          <div class="table-wrap"><table>
            <thead><tr><th>Type</th><th>Qty</th><th>Risk</th><th>Status</th><th>Collector</th><th>Deadline</th><th>Reported</th><th></th></tr></thead>
            <tbody>
            <c:forEach var="w" items="${wasteRecords}">
              <tr>
                <td><strong>${w.wasteType}</strong><c:if test="${w.emergency}"> <span class="badge badge-emergency" style="font-size:0.62rem;">&#x1F6A8;</span></c:if></td>
                <td>${w.quantityKg} kg</td>
                <td><c:choose>
                  <c:when test="${w.riskLevel=='HIGH'}"><span class="badge badge-HIGH">HIGH</span></c:when>
                  <c:when test="${w.riskLevel=='MEDIUM'}"><span class="badge badge-MEDIUM">MED</span></c:when>
                  <c:otherwise><span class="badge badge-LOW">LOW</span></c:otherwise>
                </c:choose></td>
                <td><c:choose>
                  <c:when test="${w.status=='PENDING'}"><span class="badge badge-pending">Pending</span></c:when>
                  <c:when test="${w.status=='ASSIGNED'}"><span class="badge badge-assigned">Assigned</span></c:when>
                  <c:when test="${w.status=='ACCEPTED'}"><span class="badge badge-accepted">Accepted</span></c:when>
                  <c:when test="${w.status=='EN_ROUTE'}"><span class="badge badge-en-route">&#x1F69A; En Route</span></c:when>
                  <c:when test="${w.status=='ARRIVED'}"><span class="badge badge-arrived">&#x1F4CD; Arrived</span></c:when>
                  <c:when test="${w.status=='COLLECTED'}"><span class="badge badge-collected">&#x1F4E6; Collected</span></c:when>
                  <c:when test="${w.status=='DISPOSED'}"><span class="badge badge-disposed">&#x2713; Disposed</span></c:when>
                  <c:when test="${w.status=='OVERDUE'}"><span class="badge badge-overdue">&#x1F534; Overdue</span></c:when>
                  <c:otherwise><span class="badge badge-pending">${w.status}</span></c:otherwise>
                </c:choose></td>
                <td class="text-sm">${collectorNames[w.assignedCollectorId]}</td>
                <td>
                  <c:choose>
                    <c:when test="${w.status=='DISPOSED'||w.status=='COLLECTED'}"><span class="deadline-chip completed">&#x2713; Done</span></c:when>
                    <c:when test="${not empty w.deadline}"><span class="deadline-chip" data-deadline="${w.deadline}"></span></c:when>
                  </c:choose>
                </td>
                <td class="mono text-xs text-muted">${not empty w.reportedAt?w.reportedAt.substring(0,16):''}</td>
                <td><a href="${pageContext.request.contextPath}/waste/detail?id=${w.id}" class="btn btn-secondary btn-sm">View</a></td>
              </tr>
            </c:forEach>
            </tbody>
          </table></div>
        </c:if>
      </div>
    </c:if>
  </div>
</div>
<script>
document.querySelectorAll('.deadline-chip[data-deadline]').forEach(function(el){
  function upd(){
    var dl=new Date(el.dataset.deadline),now=new Date(),diff=dl-now;
    if(diff<=0){el.className='deadline-chip overdue';el.textContent='OVERDUE';return;}
    var h=Math.floor(diff/3600000),m=Math.floor((diff%3600000)/60000);
    el.textContent=(h>0?h+'h ':'')+m+'m left';
    el.className='deadline-chip '+(diff<3600000?'approaching':'ok');
  }
  upd();setInterval(upd,60000);
});
</script>
</body>
</html>
