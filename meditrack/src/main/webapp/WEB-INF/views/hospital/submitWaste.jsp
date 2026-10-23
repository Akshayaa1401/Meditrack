<%@ include file="/WEB-INF/jspf/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>Report Waste — MediTrack</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/jspf/navbar.jspf" %>
<div class="main-content">
  <div class="page-header">
    <div class="page-header-left">
      <h1>Report Biomedical Waste</h1>
      <p>Submit a new collection request. Risk level and priority are auto-assigned.</p>
    </div>
    <div class="topbar-actions">
      <button class="mobile-menu-btn" onclick="toggleSidebar()">&#9776;</button>
    </div>
  </div>
  <div class="page-body">
    <div style="max-width:560px;">
      <c:if test="${not empty error}"><div class="alert alert-error"><span class="alert-icon">&#x26A0;</span>${error}</div></c:if>
      <div class="card">
        <form method="post" action="${pageContext.request.contextPath}/hospital/submitWaste">
          <div class="form-group">
            <label class="form-label" for="wasteType">Waste type <span>*</span></label>
            <select class="form-control" id="wasteType" name="wasteType" required onchange="updateRisk()">
              <option value="">Select waste type</option>
              <option value="Sharps">Sharps (needles, blades)</option>
              <option value="Infectious">Infectious waste</option>
              <option value="Pathological">Pathological waste</option>
              <option value="Pharmaceutical">Pharmaceutical waste</option>
              <option value="Chemical">Chemical waste</option>
              <option value="General">General biomedical waste</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label" for="quantityKg">Quantity (kg) <span>*</span></label>
            <input class="form-control" type="number" id="quantityKg" name="quantityKg" min="0.1" step="0.1" required placeholder="e.g. 5.0">
          </div>
          <div class="form-group">
            <label class="form-label">Risk level (auto-detected)</label>
            <div id="riskPreview" style="padding:10px 14px;border-radius:var(--r-md);background:var(--bg);border:1px solid var(--border);font-size:0.85rem;color:var(--ink-3);">
              Select a waste type to see the assigned risk level
            </div>
          </div>
          <div class="form-group">
            <label class="form-label" for="description">Notes for collector</label>
            <textarea class="form-control" id="description" name="description" placeholder="Any handling instructions, storage location, special requirements..."></textarea>
          </div>
          <div style="display:flex;gap:10px;">
            <button type="submit" class="btn btn-primary btn-lg" style="flex:1;">Submit Request</button>
            <a href="${pageContext.request.contextPath}/hospital/dashboard" class="btn btn-secondary btn-lg">Cancel</a>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>
<script>
var RISK_MAP={'Sharps':'HIGH','Infectious':'HIGH','Pathological':'HIGH','Pharmaceutical':'MEDIUM','Chemical':'MEDIUM','General':'LOW'};
var RISK_STYLES={'HIGH':{'bg':'var(--red-bg)','color':'var(--red)','label':'HIGH RISK — Emergency priority. Immediate collection.'},
                 'MEDIUM':{'bg':'var(--amber-bg)','color':'var(--amber)','label':'MEDIUM RISK — Priority collection within 8 hours.'},
                 'LOW':{'bg':'var(--green-bg)','color':'var(--green)','label':'LOW RISK — Standard collection within 24 hours.'}};
function updateRisk(){
  var t=document.getElementById('wasteType').value;
  var p=document.getElementById('riskPreview');
  var risk=RISK_MAP[t];
  if(!risk){p.style.background='var(--bg)';p.style.color='var(--ink-3)';p.innerHTML='Select a waste type to see the assigned risk level';return;}
  var s=RISK_STYLES[risk];
  p.style.background=s.bg;p.style.color=s.color;
  p.innerHTML='<strong>'+risk+'</strong> — '+s.label;
}
</script>
</body>
</html>
