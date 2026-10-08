import re

# Get the perfectly styled layout from invoice-view
with open('src/main/resources/templates/invoice-view.html', 'r', encoding='utf-8') as f:
    invoice_html = f.read()

# Get the original content from work-queue (since user reverted it)
with open('src/main/resources/templates/work-queue.html', 'r', encoding='utf-8') as f:
    queue_html = f.read()

# 1. Grab Layout Top (Everything up to the start of the padding div)
layout_top_end = invoice_html.find('<div style="padding: 30px;">')
if layout_top_end == -1:
    layout_top_end = invoice_html.find('<!-- Alerts -->')

layout_top = invoice_html[:layout_top_end]

# Modify the navigation to highlight Queue
layout_top = re.sub(r'<li class="nav-item active">\s*<i class="fa-solid fa-file-invoice"></i>\s*Invoices\s*</li>', r'<li class="nav-item">\n                    <i class="fa-solid fa-file-invoice"></i>\n                    Invoices\n                </li>', layout_top)
layout_top = re.sub(r'<li class="nav-item">\s*<i class="fa-solid fa-list-check"></i>\s*Queue\s*</li>', r'<li class="nav-item active">\n                    <i class="fa-solid fa-list-check"></i>\n                    Queue\n                </li>', layout_top)

# Update page titles
layout_top = layout_top.replace('<title>CleanTrack - Invoices</title>', '<title>CleanTrack - Workflow Queue</title>')
layout_top = layout_top.replace('<h1>Billing & Invoices</h1>', '<h1>Workflow Queue</h1>')
layout_top = layout_top.replace('<p>Manage payments and view past receipts.</p>', '<p>Track and advance garments through processing stages.</p>')

# Add the specific badge-stage CSS back into the <head>
badge_css = '''
        .badge-stage {
            background: rgba(67, 24, 255, 0.1);
            color: var(--primary) !important;
            border: 1px solid var(--primary);
            padding: 8px 12px;
        }
        .text-white {
            color: var(--text-main) !important;
        }
'''
layout_top = layout_top.replace('</style>', badge_css + '\n    </style>')

# 2. Extract Main Content from work-queue.html
filters_start = queue_html.find('<div class="d-flex justify-content-between align-items-center mb-4">')
if filters_start == -1:
    filters_start = queue_html.find('<div class="d-flex align-items-center gap-2">') - 100
table_end = queue_html.find('<!-- Modals -->')
if table_end == -1:
    table_end = queue_html.find('<!-- Bootstrap JS -->')

main_content = queue_html[filters_start:table_end]

# Fix the search controls that had inline hardcoded white color
main_content = re.sub(r'style="background: rgba\(255,255,255,0\.05\); color: #fff; border-color: rgba\(255,255,255,0\.1\);(?: min-width: 200px;)?"', '', main_content)
main_content = re.sub(r'style="background: #0f172a; border-color: rgba\(255,255,255,0\.1\);"', '', main_content)
main_content = re.sub(r'style="border-color: rgba\(255,255,255,0\.1\);"', '', main_content)
main_content = main_content.replace('dropdown-menu dropdown-menu-dark', 'dropdown-menu')
main_content = main_content.replace('btn btn-outline-secondary', 'btn btn-outline-primary')

# Wrap the table exactly like invoice-view
# We need to replace the old glass-card or raw table div with card p-0 overflow-hidden
# But wait, work-queue might just have the raw table. Let's find <div class="table-responsive"> or similar
main_content = main_content.replace('class="glass-card p-4"', 'class="card p-0 overflow-hidden"')
main_content = main_content.replace('table-custom', 'table mb-0')
main_content = main_content.replace('table-dark', '')

# Also remove hardcoded text-white
main_content = main_content.replace('class="text-white"', '')
main_content = main_content.replace('fw-bold text-white', 'fw-bold')

# Extract Layout Bottom
layout_bottom_start = invoice_html.find('<!-- Bootstrap JS -->')
layout_bottom = invoice_html[layout_bottom_start:]

# Inject Modals if any
modals_html = ''
if '<!-- Modals -->' in queue_html:
    modals_start = queue_html.find('<!-- Modals -->')
    modals_end = queue_html.find('<!-- Bootstrap JS -->')
    modals_html = queue_html[modals_start:modals_end]

# Reassemble
new_queue_html = layout_top + '\n<div style="padding: 30px;">\n' + main_content + '\n' + modals_html + '\n</div>\n    </main>\n' + layout_bottom

with open('src/main/resources/templates/work-queue.html', 'w', encoding='utf-8') as f:
    f.write(new_queue_html)

print('Work Queue rebuilt based on invoice-view!')
