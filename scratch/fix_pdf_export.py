import re

with open('src/main/resources/templates/reports.html', 'r', encoding='utf-8') as f:
    html = f.read()

# 1. Fix the Export PDF button color
html = html.replace('class="btn btn-outline-light btn-print px-4 py-2"', 'class="btn btn-outline-danger btn-print px-4 py-2"')

# 2. Add @media print styles
print_styles = '''
    /* Print Styles for tidy PDF export */
    @media print {
        body { background: white !important; color: black !important; }
        .sidebar { display: none !important; }
        .header { display: none !important; }
        .btn, button, form, a[href^="/"] { display: none !important; }
        .main-content { margin: 0 !important; padding: 0 !important; width: 100% !important; }
        .dashboard-container { max-width: 100% !important; margin: 0 !important; padding: 0 !important; }
        .card, .kpi-card { 
            box-shadow: none !important; 
            border: 1px solid #ddd !important; 
            break-inside: avoid;
            page-break-inside: avoid;
        }
        /* Make sure backgrounds print */
        * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
        .row { display: flex !important; flex-wrap: wrap !important; }
        .col-md-3 { width: 25% !important; flex: 0 0 25% !important; }
        .col-lg-6 { width: 50% !important; flex: 0 0 50% !important; }
        /* Give canvas a fixed size for printing so Chart.js doesn't collapse */
        canvas { max-height: 300px !important; }
    }
'''

if '@media print {' not in html:
    html = html.replace('</style>', print_styles + '\n</style>')

with open('src/main/resources/templates/reports.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('Fixed Export PDF button and added tidy print styles!')
