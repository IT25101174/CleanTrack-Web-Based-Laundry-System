import re

# Get layout pieces from invoice-view
with open('src/main/resources/templates/invoice-view.html', 'r', encoding='utf-8') as f:
    invoice_html = f.read()

layout_top_end = invoice_html.find('<div style="padding: 30px;">')
layout_top_base = invoice_html[:layout_top_end]
layout_bottom_start = invoice_html.find('<!-- Bootstrap JS -->')
layout_bottom_base = invoice_html[layout_bottom_start:]

# Customize layout_top for Complaints
layout_top = layout_top_base
layout_top = layout_top.replace('<title>CleanTrack - Billing & Invoices</title>', '<title>CleanTrack - Complaints</title>')
layout_top = layout_top.replace('<h1>Billing & Invoices</h1>', '<h1>Complaints Management</h1>')
layout_top = layout_top.replace('<p>Manage payments and view past receipts.</p>', '<p>Manage customer feedback and resolve issues.</p>')

# Update Active Tab in Sidebar
layout_top = re.sub(r'<li class="nav-item active">\s*<i class="fa-solid fa-file-invoice"></i>\s*Invoices\s*</li>', r'<li class="nav-item">\n                    <i class="fa-solid fa-file-invoice"></i>\n                    Invoices\n                </li>', layout_top)
layout_top = re.sub(r'<li class="nav-item">\s*<i class="fa-solid fa-circle-exclamation"></i>\s*Complaints\s*</li>', r'<li class="nav-item active">\n                    <i class="fa-solid fa-circle-exclamation"></i>\n                    Complaints\n                </li>', layout_top)

with open('src/main/resources/templates/complaint-form.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Remove the old <nav>
nav_start = html.find('<nav')
if nav_start != -1:
    nav_end = html.find('</nav>') + 6
    html = html[:nav_start] + html[nav_end:]

# Main content starts around <div class="container-fluid max-w-7xl mx-auto">
# Let's just find the main layout.
body_start = html.find('<body')
body_end = html.find('>', body_start) + 1
main_content = html[body_end:]

main_content = main_content.replace('</body>', '')
main_content = main_content.replace('</html>', '')

# Remove old layout wrappers
main_content = main_content.replace('<div class="dashboard-container mt-5 pt-4">', '')
main_content = main_content.replace('<div class="container-fluid max-w-7xl mx-auto">', '<div class="complaints-wrapper" style="padding: 30px;">')

# Replace the "Header Section" which duplicates our new header
# <!-- Header Section -->
header_match = re.search(r'<!-- Header Section -->.*?</div>.*?</div>', main_content, re.DOTALL)
if header_match:
    main_content = main_content.replace(header_match.group(0), '')

# Fix hardcoded text-white colors or old glass-card dark backgrounds
main_content = main_content.replace('text-white', '')
main_content = main_content.replace('bg-dark', '')
# Let's replace the custom .glass-card with Bootstrap .card + padding 0 for the table wrapper
main_content = main_content.replace('class="glass-card"', 'class="card p-0 overflow-hidden"')
main_content = main_content.replace('class="glass-card ', 'class="card ')

# Replace button colors
main_content = main_content.replace('btn-outline-custom', 'btn-outline-primary')
main_content = main_content.replace('btn-primary-custom', 'btn-primary')

# Replace the custom table classes
main_content = main_content.replace('table-dark table-hover', 'table-hover align-middle mb-0')

# Also the "New Complaint Form" might be in a glass card. We should change it to a standard card.
main_content = main_content.replace('class="card p-0 overflow-hidden mb-4"', 'class="card p-4 mb-4"')
main_content = main_content.replace('class="card p-0 overflow-hidden"', 'class="card p-0 overflow-hidden"')

new_html = layout_top + main_content + '\n    </main>\n' + layout_bottom_base

with open('src/main/resources/templates/complaint-form.html', 'w', encoding='utf-8') as f:
    f.write(new_html)

print('Updated Complaints UI!')
