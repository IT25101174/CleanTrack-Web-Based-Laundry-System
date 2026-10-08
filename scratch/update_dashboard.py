with open('src/main/resources/templates/customer-dashboard.html', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Total Spent
content = content.replace('pendingPaymentsAmount', 'totalSpentAmount')
content = content.replace('<p>Pending Payments</p>', '<p>Total Spent</p>')

# 2. Remove Promo Card
promo_card = '''        <div class="promo-card">
            <h4>Need Priority Cleaning?</h4>
            <p style="font-size: 13px; opacity: 0.9;">Upgrade to Premium for 24-hour turnaround.</p>
            <button>Upgrade Now</button>
        </div>'''
content = content.replace(promo_card, '')

# 3. Remove Dark Mode Link
dark_mode = '''            <a href="#" onclick="document.body.style.filter = document.body.style.filter === 'invert(1) hue-rotate(180deg)' ? 'invert(0)' : 'invert(1) hue-rotate(180deg)';" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-moon"></i>
                    Toggle Dark Mode
                </li>
            </a>'''
content = content.replace(dark_mode, '')

# 4. Add Edit Details icon near logout
header_old = '''<a th:href="@{/logout}" style="text-decoration:none;"><i class="fa-solid fa-right-from-bracket" style="font-size: 20px; color: var(--text-muted); cursor: pointer;"></i></a>'''
header_new = '''<a th:href="@{/profile/edit}" style="text-decoration:none; margin-right: 15px;" title="Edit Details"><i class="fa-solid fa-user-pen" style="font-size: 20px; color: var(--text-muted); cursor: pointer;"></i></a>
                <a th:href="@{/logout}" style="text-decoration:none;" title="Sign Out"><i class="fa-solid fa-right-from-bracket" style="font-size: 20px; color: var(--text-muted); cursor: pointer;"></i></a>'''
content = content.replace(header_old, header_new)

with open('src/main/resources/templates/customer-dashboard.html', 'w', encoding='utf-8') as f:
    f.write(content)

print('Done!')
