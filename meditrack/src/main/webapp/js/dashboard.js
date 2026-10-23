/**
 * dashboard.js
 * -------------
 * Small UX helpers shared across dashboards: confirm-before-destructive-
 * action prompts, and a periodic poll that checks whether a NEW route
 * re-optimization event has landed for the current collector (so the
 * "old vs new route" panel updates without a full page reload).
 */

document.addEventListener('DOMContentLoaded', () => {
    // Confirm before deleting hospitals/collectors (admin) or marking disposed
    document.querySelectorAll('[data-confirm]').forEach(el => {
        el.addEventListener('click', (e) => {
            if (!confirm(el.getAttribute('data-confirm'))) {
                e.preventDefault();
            }
        });
    });

    // Auto-collapse the emergency comparison panel toggle
    const compareToggle = document.getElementById('toggleRouteCompare');
    const comparePanel = document.getElementById('routeComparePanel');
    if (compareToggle && comparePanel) {
        compareToggle.addEventListener('click', () => {
            comparePanel.style.display = comparePanel.style.display === 'none' ? 'block' : 'none';
        });
    }
});

/**
 * Polls /collector/dashboard's underlying data indirectly by checking the
 * map API for a new active route distance. If it differs from what's
 * currently rendered, prompts the collector to refresh. Called only on the
 * collector dashboard page (see dashboard.jsp).
 */
function watchForRouteChanges(collectorId, currentDistanceKm) {
    if (!collectorId) return;
    setInterval(() => {
        fetch(`${window.MEDITRACK_CTX || ''}/api/mapData?collectorId=${collectorId}`)
            .then(r => r.json())
            .then(data => {
                if (data.totalDistanceKm !== undefined &&
                    Math.abs(data.totalDistanceKm - currentDistanceKm) > 0.01) {
                    const banner = document.getElementById('routeChangedBanner');
                    if (banner) banner.style.display = 'flex';
                }
            })
            .catch(() => {});
    }, 15000); // poll every 15s
}
