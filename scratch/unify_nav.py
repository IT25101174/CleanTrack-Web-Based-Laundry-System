files_to_update = [
    'src/main/resources/templates/customer-dashboard.html',
    'src/main/resources/templates/order-list.html',
    'src/main/resources/templates/invoice-view.html'
]

unified_nav = '''        <ul class="nav-links">
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

for filepath in files_to_update:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    nav_start = content.find('<ul class="nav-links">')
    nav_end = content.find('</ul>') + 5
    if nav_start != -1:
        # Determine which item should be active based on filepath
        custom_nav = unified_nav
        if 'orders' in filepath:
            custom_nav = custom_nav.replace('nav-item active">\n                    <i class="fa-solid fa-house', 'nav-item">\n                    <i class="fa-solid fa-house')
            custom_nav = custom_nav.replace('nav-item">\n                    <i class="fa-solid fa-shirt', 'nav-item active">\n                    <i class="fa-solid fa-shirt')
        elif 'invoices' in filepath:
            custom_nav = custom_nav.replace('nav-item active">\n                    <i class="fa-solid fa-house', 'nav-item">\n                    <i class="fa-solid fa-house')
            custom_nav = custom_nav.replace('nav-item">\n                    <i class="fa-solid fa-file-invoice', 'nav-item active">\n                    <i class="fa-solid fa-file-invoice')
        
        content = content[:nav_start] + custom_nav + content[nav_end:]
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
print('Unified navigation injected across all pages!')
