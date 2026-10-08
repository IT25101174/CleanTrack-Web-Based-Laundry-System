import re

with open('src/main/resources/templates/reports.html', 'r', encoding='utf-8') as f:
    html = f.read()

# 1. Inject KPI styles at the end of the existing <style> block
styles = '''
    /* KPI KPI-Cards */
    .kpi-card {
        border-radius: 16px;
        padding: 25px;
        position: relative;
        overflow: hidden;
        border: 1px solid rgba(0, 0, 0, 0.05);
        height: 100%;
    }
    .kpi-revenue { background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(5, 150, 105, 0.2)); border-color: rgba(16, 185, 129, 0.3); }
    .kpi-orders { background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(37, 99, 235, 0.2)); border-color: rgba(59, 130, 246, 0.3); }
    .kpi-complaints { background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(220, 38, 38, 0.2)); border-color: rgba(239, 68, 68, 0.3); }
    .kpi-unpaid { background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(217, 119, 6, 0.2)); border-color: rgba(245, 158, 11, 0.3); }
    .kpi-icon { position: absolute; right: -10px; bottom: -20px; font-size: 6rem; opacity: 0.1; }
    .kpi-value { font-size: 2.2rem; font-weight: 700; margin: 10px 0; letter-spacing: -1px; }
    .stat-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 15px; border-bottom: 1px solid rgba(0, 0, 0, 0.05); }
    .stat-item:last-child { border-bottom: none; }
'''
if '.kpi-card {' not in html:
    html = html.replace('</style>', styles + '\n</style>')

# 2. Fix the grid for KPI cards (5 columns)
# The row containing KPI cards starts after <!-- KPI Row -->
html = html.replace('<div class="row mb-4">', '<div class="row row-cols-1 row-cols-md-5 mb-4">', 1)
# Replace the col-md-3 mb-3 with col mb-3 for the KPI cards
html = html.replace('<div class="col-md-3 mb-3">', '<div class="col mb-3">')

# 3. Fix Chart.js hardcoded dark colors
html = html.replace("labels: { color: '#f3f4f6' }", "labels: { color: '#64748b' }")
html = html.replace("ticks: { color: '#9ca3af' }", "ticks: { color: '#64748b' }")
html = html.replace("grid: { color: 'rgba(255,255,255,0.05)' }", "grid: { color: 'rgba(0,0,0,0.05)' }")

# Also, the audit log table has `text-white` or something maybe? Let's aggressively remove any text-white.
html = html.replace('text-white', '')

with open('src/main/resources/templates/reports.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('Reports UI fully fixed!')
