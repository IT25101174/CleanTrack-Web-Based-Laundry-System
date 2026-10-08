import re

with open('src/main/resources/templates/invoice-view.html', 'r', encoding='utf-8') as f:
    content = f.read()

with open('src/main/resources/templates/customer-dashboard.html', 'r', encoding='utf-8') as f:
    dashboard_html = f.read()

header_end = dashboard_html.find('</header>') + 9
layout_top = dashboard_html[:header_end]
layout_bottom = '''    </main>
    <script>
        function toggleDarkMode() {
            document.body.classList.toggle('dark-theme');
            localStorage.setItem('darkMode', document.body.classList.contains('dark-theme'));
        }
        if (localStorage.getItem('darkMode') === 'true') {
            document.body.classList.add('dark-theme');
        }
    </script>
</body>
</html>'''

# Fix double </a> from previous script if not fixed perfectly in layout_top
layout_top = re.sub(r'</a>\s*</li>\s*</a>', '</a>', layout_top)

layout_top = layout_top.replace('<li class="nav-item active">', '<li class="nav-item">')
layout_top = layout_top.replace('<i class="fa-solid fa-file-invoice"></i>\n                    Invoices', '<i class="fa-solid fa-file-invoice"></i>\n                    Invoices\n                </li>\n            </a>', 1)
layout_top = layout_top.replace('Dashboard\n                </li>', 'Dashboard\n                </li>')

layout_top = layout_top.replace('<li class="nav-item active">\n                    <i class="fa-solid fa-house"></i>', '<li class="nav-item">\n                    <i class="fa-solid fa-house"></i>')
layout_top = layout_top.replace('<li class="nav-item">\n                    <i class="fa-solid fa-file-invoice"></i>', '<li class="nav-item active">\n                    <i class="fa-solid fa-file-invoice"></i>')

layout_top = layout_top.replace('<title>CleanTrack - Customer Dashboard</title>', '<title>CleanTrack - Billing & Invoices</title>')

welcome_text_start = layout_top.find('<div class="welcome-text">')
welcome_text_end = layout_top.find('</div>', welcome_text_start) + 6
new_welcome_text = '''<div class="welcome-text">
                <h1>Billing & Invoices</h1>
                <p>Manage payments and view past receipts.</p>
            </div>'''
layout_top = layout_top[:welcome_text_start] + new_welcome_text + layout_top[welcome_text_end:]

# Add bootstrap CSS
bootstrap_css = '<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">'
layout_top = layout_top.replace('</title>', '</title>\n    ' + bootstrap_css)


table_start = content.find('<div class="d-flex justify-content-between align-items-center mb-4">')
table_end = content.find('<!-- View Modal -->')

main_table = content[table_start:table_end]

# Remove the duplicated header from the original table
header_div_start = main_table.find('<div class="d-flex justify-content-between align-items-center mb-4">')
header_div_end = main_table.find('</div>', main_table.find('</div>', header_div_start) + 6) + 6

main_table = main_table[:header_div_start] + main_table[header_div_end:]

# Clean up CSS classes
main_table = main_table.replace('btn-primary-custom', 'btn-primary')
main_table = main_table.replace('class="glass-card', 'class="card')
main_table = main_table.replace('table-custom', 'table')

# Now the modals and scripts
modal_start = content.find('<!-- View Modal -->')
modal_end = content.find('<!-- Bootstrap JS -->')
modal = content[modal_start:modal_end]

scripts_start = content.find('<style>', modal_end)
scripts_end = content.find('</body>')
scripts = content[scripts_start:scripts_end]

bootstrap_js = '<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>'
layout_bottom = bootstrap_js + '\n' + layout_bottom

new_html = layout_top + '\n<div style="padding: 30px;">\n' + main_table + '\n</div>\n' + modal + '\n' + scripts + layout_bottom

with open('src/main/resources/templates/invoice-view.html', 'w', encoding='utf-8') as f:
    f.write(new_html)

print('Updated invoice-view.html')
