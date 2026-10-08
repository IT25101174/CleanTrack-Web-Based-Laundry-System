import re

# Read current dashboard to extract the grid items
with open('src/main/resources/templates/dashboard.html', 'r', encoding='utf-8') as f:
    old_dashboard = f.read()

# Extract the row containing the grid
grid_start = old_dashboard.find('<div class="row">')
grid_end = old_dashboard.find('<!-- Footer -->')
grid_html = old_dashboard[grid_start:grid_end]

# Modify the cards in the grid to match the new aesthetic
grid_html = grid_html.replace('glass-card', 'card border-0 shadow-sm')
grid_html = grid_html.replace('btn-primary-custom', 'btn btn-primary rounded-pill px-4')
# Remove text-muted if we want adaptive text, but text-muted is fine

# Read customer dashboard for the base layout
with open('src/main/resources/templates/customer-dashboard.html', 'r', encoding='utf-8') as f:
    customer_dash = f.read()

header_end = customer_dash.find('</header>') + 9
layout_top = customer_dash[:header_end]
layout_bottom = '''
    </div>
    </main>
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
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

# Ensure bootstrap is in the head
bootstrap_css = '<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">'
if bootstrap_css not in layout_top:
    layout_top = layout_top.replace('</title>', '</title>\n    ' + bootstrap_css)

# Update page title and header
layout_top = layout_top.replace('<title>CleanTrack - Customer Dashboard</title>', '<title>CleanTrack - Admin Dashboard</title>')
welcome_start = layout_top.find('<div class="welcome-text">')
welcome_end = layout_top.find('</div>', welcome_start) + 6
new_welcome = '''<div class="welcome-text">
                <h1>Control Panel</h1>
                <p>Welcome to the CleanTrack Administrative Workspace.</p>
            </div>'''
layout_top = layout_top[:welcome_start] + new_welcome + layout_top[welcome_end:]

# Build the unified sidebar nav
nav_links = '''        <ul class="nav-links">
            <a th:href="@{/dashboard}" style="text-decoration: none;">
                <li class="nav-item active">
                    <i class="fa-solid fa-house"></i>
                    Dashboard
                </li>
            </a>
            <a th:href="@{/orders}" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-shirt"></i>
                    Orders
                </li>
            </a>
            <a th:href="@{/invoices}" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-file-invoice"></i>
                    Invoices
                </li>
            </a>
            <a th:href="@{/queue}" style="text-decoration: none;" th:if="${user.role?.name() != 'CUSTOMER'}">
                <li class="nav-item">
                    <i class="fa-solid fa-list-check"></i>
                    Queue
                </li>
            </a>
            <a th:href="@{/inventory}" style="text-decoration: none;" th:if="${user.role?.name() == 'COUNTER_STAFF' || user.role?.name() == 'BRANCH_SUPERVISOR' || user.role?.name() == 'ADMIN'}">
                <li class="nav-item">
                    <i class="fa-solid fa-boxes-stacked"></i>
                    Inventory
                </li>
            </a>
            <a th:href="@{/complaints}" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-circle-exclamation"></i>
                    Complaints
                </li>
            </a>
            <a th:href="@{/reports}" style="text-decoration: none;" th:if="${user.role?.name() == 'BRANCH_SUPERVISOR' || user.role?.name() == 'ADMIN'}">
                <li class="nav-item">
                    <i class="fa-solid fa-chart-line"></i>
                    Reports
                </li>
            </a>
            <a th:href="@{/admin/users/new}" style="text-decoration: none;" th:if="${user.role?.name() == 'ADMIN'}">
                <li class="nav-item">
                    <i class="fa-solid fa-user-plus"></i>
                    Employees
                </li>
            </a>
            <a href="#" onclick="toggleDarkMode()" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-moon"></i>
                    Dark Mode
                </li>
            </a>
        </ul>'''

nav_start = layout_top.find('<ul class="nav-links">')
nav_end = layout_top.find('</ul>') + 5
layout_top = layout_top[:nav_start] + nav_links + layout_top[nav_end:]


new_dashboard = layout_top + '\n<div style="padding: 30px;">\n' + grid_html + layout_bottom

with open('src/main/resources/templates/dashboard.html', 'w', encoding='utf-8') as f:
    f.write(new_dashboard)

print("Dashboard rebuilt with new layout!")
