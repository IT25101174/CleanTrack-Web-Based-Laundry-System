import re

# Get layout pieces from invoice-view
with open('src/main/resources/templates/invoice-view.html', 'r', encoding='utf-8') as f:
    invoice_html = f.read()

layout_top_end = invoice_html.find('<div style="padding: 30px;">')
layout_top_base = invoice_html[:layout_top_end]
layout_bottom_start = invoice_html.find('<!-- Bootstrap JS -->')
layout_bottom_base = invoice_html[layout_bottom_start:]

# Customize layout_top for Reports
layout_top = layout_top_base
layout_top = layout_top.replace('<title>CleanTrack - Billing & Invoices</title>', '<title>CleanTrack - Reports & Analytics</title>')
layout_top = layout_top.replace('<h1>Billing & Invoices</h1>', '<h1>Reports & Analytics</h1>')
layout_top = layout_top.replace('<p>Manage payments and view past receipts.</p>', '<p>Analyze performance metrics and business growth.</p>')

# Update Active Tab in Sidebar
layout_top = re.sub(r'<li class="nav-item active">\s*<i class="fa-solid fa-file-invoice"></i>\s*Invoices\s*</li>', r'<li class="nav-item">\n                    <i class="fa-solid fa-file-invoice"></i>\n                    Invoices\n                </li>', layout_top)
layout_top = re.sub(r'<li class="nav-item">\s*<i class="fa-solid fa-chart-line"></i>\s*Reports\s*</li>', r'<li class="nav-item active">\n                    <i class="fa-solid fa-chart-line"></i>\n                    Reports\n                </li>', layout_top)

with open('src/main/resources/templates/reports.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Main content starts around <div class="container-fluid max-w-7xl mx-auto">
body_start = html.find('<body')
body_end = html.find('>', body_start) + 1
main_content = html[body_end:]

main_content = main_content.replace('</body>', '')
main_content = main_content.replace('</html>', '')

# Remove old <nav>
nav_start = main_content.find('<nav')
if nav_start != -1:
    nav_end = main_content.find('</nav>') + 6
    main_content = main_content[:nav_start] + main_content[nav_end:]

# Remove old layout wrappers
main_content = main_content.replace('<div class="dashboard-container mt-5 pt-4">', '')
main_content = main_content.replace('<div class="container-fluid max-w-7xl mx-auto">', '<div class="reports-wrapper" style="padding: 30px;">')

# Replace the "Header Section" which duplicates our new header
header_match = re.search(r'<!-- Header Section -->.*?</div>.*?</div>', main_content, re.DOTALL)
if header_match:
    main_content = main_content.replace(header_match.group(0), '')

# Fix classes
main_content = main_content.replace('text-white', '')
main_content = main_content.replace('bg-dark', '')
# Let's replace the custom .glass-card with Bootstrap .card
main_content = main_content.replace('class="glass-card"', 'class="card p-4"')
main_content = main_content.replace('class="glass-card ', 'class="card ')

# Replace button colors
main_content = main_content.replace('btn-outline-custom', 'btn-outline-primary')

# Replace text-muted which was forced !important in old template - actually this is fine in standard BS

new_html = layout_top + main_content + '\n    </main>\n' + layout_bottom_base

with open('src/main/resources/templates/reports.html', 'w', encoding='utf-8') as f:
    f.write(new_html)

print('Updated Reports UI!')
