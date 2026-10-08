import re

with open('src/main/resources/templates/customer-dashboard.html', 'r', encoding='utf-8') as f:
    dashboard_html = f.read()

with open('src/main/resources/templates/order-list.html', 'r', encoding='utf-8') as f:
    orders_html = f.read()

# Extract dashboard CSS and layout structure
head_end = dashboard_html.find('</head>')
body_start = dashboard_html.find('<body>')
main_content_start = dashboard_html.find('<main class="main-content">')
header_end = dashboard_html.find('</header>') + 9

layout_top = dashboard_html[:header_end]
layout_bottom = '''    </main>
    <script>
        // Proper Dark Mode Toggle
        function toggleDarkMode() {
            document.body.classList.toggle('dark-theme');
            localStorage.setItem('darkMode', document.body.classList.contains('dark-theme'));
        }
        
        // Load preference
        if (localStorage.getItem('darkMode') === 'true') {
            document.body.classList.add('dark-theme');
        }
    </script>
</body>
</html>'''

# Modify nav items in sidebar to highlight My Orders
layout_top = layout_top.replace('<li class="nav-item active">', '<li class="nav-item">')
layout_top = layout_top.replace('<i class="fa-solid fa-shirt"></i>\n                    My Orders', '<i class="fa-solid fa-shirt"></i>\n                    My Orders\n                </li>\n            </a>', 1)
layout_top = layout_top.replace('Dashboard\n                </li>', 'Dashboard\n                </li>')

# Use string replace carefully
layout_top = layout_top.replace('<li class="nav-item active">\n                    <i class="fa-solid fa-house"></i>', '<li class="nav-item">\n                    <i class="fa-solid fa-house"></i>')
layout_top = layout_top.replace('<li class="nav-item">\n                    <i class="fa-solid fa-shirt"></i>', '<li class="nav-item active">\n                    <i class="fa-solid fa-shirt"></i>')

# Also fix the page title
layout_top = layout_top.replace('<title>CleanTrack - Customer Dashboard</title>', '<title>CleanTrack - Order Management</title>')

# Change the header text
welcome_text_start = layout_top.find('<div class="welcome-text">')
welcome_text_end = layout_top.find('</div>', welcome_text_start) + 6
new_welcome_text = '''<div class="welcome-text">
                <h1>Order Management</h1>
                <p>Track and manage all laundry requests.</p>
            </div>'''
layout_top = layout_top[:welcome_text_start] + new_welcome_text + layout_top[welcome_text_end:]


# Now get the main table content from orders-list.html
table_start = orders_html.find('<div class="d-flex justify-content-between align-items-center mb-4">')
table_end = orders_html.find('<!-- History Modal -->')

main_table = orders_html[table_start:table_end]

# Remove the duplicated header from the original table
header_div_start = main_table.find('<div class="d-flex justify-content-between align-items-center mb-4">')
header_div_end = main_table.find('</div>', main_table.find('</div>', header_div_start) + 6) + 6
# Keep the new order button
new_order_btn = '''            <div th:if="${user.role?.name() == 'CUSTOMER' or user.role?.name() == 'COUNTER_STAFF'}" style="margin-bottom: 20px;">
                <a href="/orders/new" class="btn btn-primary" style="background: var(--primary); border: none; padding: 10px 20px; border-radius: 10px; color: white; text-decoration: none;">
                    <i class="fa-solid fa-plus me-2"></i>New Request
                </a>
            </div>'''
main_table = main_table[:header_div_start] + new_order_btn + main_table[header_div_end:]


# Modify the table slightly to fit the new aesthetic (e.g. remove glass-card specific to old css, use .card)
main_table = main_table.replace('btn-primary-custom', 'btn-primary')
main_table = main_table.replace('class="glass-card', 'class="card')
main_table = main_table.replace('table-custom', 'table')

# Also the history modal
modal_start = orders_html.find('<!-- History Modal -->')
modal_end = orders_html.find('<!-- Bootstrap JS -->')
modal = orders_html[modal_start:modal_end]

# And the scripts/styles for history
scripts_start = orders_html.find('<style>', modal_end)
scripts_end = orders_html.find('</body>')
scripts = orders_html[scripts_start:scripts_end]

# We need bootstrap css and js for the table and modal!
# Add bootstrap to head if not there
bootstrap_css = '<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">'
layout_top = layout_top.replace('</title>', '</title>\n    ' + bootstrap_css)

bootstrap_js = '<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>'
layout_bottom = bootstrap_js + '\n' + layout_bottom

new_html = layout_top + '\n<div style="padding: 30px;">\n' + main_table + '\n</div>\n' + modal + '\n' + scripts + layout_bottom

with open('src/main/resources/templates/order-list.html', 'w', encoding='utf-8') as f:
    f.write(new_html)

print('Updated order-list.html')
