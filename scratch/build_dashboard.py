import re

with open('customer-dashboard-test.html', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add thymeleaf namespace
content = content.replace('<html lang="en">', '<html lang="en" xmlns:th="http://www.thymeleaf.org">')

# 2. Update Sidebar links
sidebar_old = '''        <ul class="nav-links">
            <li class="nav-item active">
                <i class="fa-solid fa-house"></i>
                Dashboard
            </li>
            <li class="nav-item">
                <i class="fa-solid fa-shirt"></i>
                My Orders
            </li>
            <li class="nav-item">
                <i class="fa-solid fa-file-invoice"></i>
                Invoices
            </li>
            <li class="nav-item">
                <i class="fa-solid fa-bell"></i>
                Notifications
            </li>
            <li class="nav-item">
                <i class="fa-solid fa-gear"></i>
                Settings
            </li>
        </ul>'''

sidebar_new = '''        <ul class="nav-links">
            <a th:href="@{/dashboard}" style="text-decoration: none;">
                <li class="nav-item active">
                    <i class="fa-solid fa-house"></i>
                    Dashboard
                </li>
            </a>
            <a th:href="@{/orders}" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-shirt"></i>
                    My Orders
                </li>
            </a>
            <a th:href="@{/invoices}" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-file-invoice"></i>
                    Invoices
                </li>
            </a>
            <a href="#" onclick="document.body.style.filter = document.body.style.filter === 'invert(1) hue-rotate(180deg)' ? 'invert(0)' : 'invert(1) hue-rotate(180deg)';" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-moon"></i>
                    Toggle Dark Mode
                </li>
            </a>
        </ul>'''

content = content.replace(sidebar_old, sidebar_new)

# 3. Update the header with Thymeleaf user data
header_old = '''        <header class="header">
            <div class="welcome-text">
                <h1>Welcome back, Alex! 👋</h1>
                <p>Here is what's happening with your laundry today.</p>
            </div>
            <div class="user-profile">
                <i class="fa-regular fa-bell" style="font-size: 20px; color: var(--text-muted); cursor: pointer;"></i>
                <div class="avatar">A</div>
                <div style="font-weight: 600;">Alex Johnson</div>
                <i class="fa-solid fa-chevron-down" style="font-size: 12px; color: var(--text-muted); cursor: pointer;"></i>
            </div>
        </header>'''

header_new = '''        <header class="header">
            <div class="welcome-text">
                <h1>Welcome back, <span th:text="${user.fullName.split(' ')[0]}">User</span>! 👋</h1>
                <p>Here is what's happening with your laundry today.</p>
            </div>
            <div class="user-profile">
                <a th:href="@{/logout}" style="text-decoration:none;"><i class="fa-solid fa-right-from-bracket" style="font-size: 20px; color: var(--text-muted); cursor: pointer;"></i></a>
                <div class="avatar" th:text="${user.fullName.substring(0,1).toUpperCase()}">A</div>
                <div style="font-weight: 600;" th:text="${user.fullName}">User Name</div>
            </div>
        </header>'''

# handle the weird character if present
content = content.replace("<h1>Welcome back, Alex! dY`<</h1>", "<h1>Welcome back, Alex! 👋</h1>")
content = content.replace(header_old, header_new)

# 4. Remove reward points and fix grid
stats_old = '''            <div class="stat-box">
                <div class="stat-icon" style="background: rgba(76, 175, 80, 0.1); color: #4CAF50;"><i class="fa-solid fa-star"></i></div>
                <div class="stat-info">
                    <h3>150</h3>
                    <p>Reward Points</p>
                </div>
            </div>'''
content = content.replace(stats_old, '')

# Change stats grid to 3 columns since one is removed
content = content.replace('grid-template-columns: 1fr 1fr;', 'grid-template-columns: repeat(3, 1fr);')


# 5. Populate stats dynamically
content = content.replace('<h3>3</h3>', '<h3 th:text="${activeOrdersCount != null ? activeOrdersCount : 3}">3</h3>')
content = content.replace('<h3>12</h3>', '<h3 th:text="${completedOrdersCount != null ? completedOrdersCount : 12}">12</h3>')
content = content.replace('<h3>Rs. 0</h3>', '<h3 th:text="${pendingPaymentsAmount != null ? pendingPaymentsAmount : \'Rs. 0\'}">Rs. 0</h3>')

# Write back
with open('src/main/resources/templates/customer-dashboard.html', 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
