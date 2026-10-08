import re

with open('src/main/resources/templates/reports.html', 'r', encoding='utf-8') as f:
    html = f.read()

styles = '''
<style>
    /* KPI KPI-Cards */
    .kpi-card {
        border-radius: 16px;
        padding: 25px;
        position: relative;
        overflow: hidden;
        border: 1px solid rgba(0, 0, 0, 0.05);
    }
    .kpi-revenue { background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(5, 150, 105, 0.2)); border-color: rgba(16, 185, 129, 0.3); }
    .kpi-orders { background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(37, 99, 235, 0.2)); border-color: rgba(59, 130, 246, 0.3); }
    .kpi-complaints { background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(220, 38, 38, 0.2)); border-color: rgba(239, 68, 68, 0.3); }
    .kpi-unpaid { background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(217, 119, 6, 0.2)); border-color: rgba(245, 158, 11, 0.3); }
    .kpi-icon { position: absolute; right: -10px; bottom: -20px; font-size: 6rem; opacity: 0.1; }
    .kpi-value { font-size: 2.5rem; font-weight: 700; margin: 10px 0; letter-spacing: -1px; }
    .stat-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 15px; border-bottom: 1px solid rgba(0, 0, 0, 0.05); }
    .stat-item:last-child { border-bottom: none; }
</style>
'''

if 'kpi-card {' not in html:
    html = html.replace('<div class="reports-wrapper"', styles + '\n<div class="reports-wrapper"')
    with open('src/main/resources/templates/reports.html', 'w', encoding='utf-8') as f:
        f.write(html)
    print('Injected KPI styles')
else:
    print('Styles already exist')
