with open('src/main/resources/templates/customer-dashboard.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace Total Spent with Garments Cleaned
content = content.replace('totalSpentAmount', 'totalItemsCleaned')
content = content.replace('<p>Total Spent</p>', '<p>Garments Cleaned</p>')
content = content.replace('fa-wallet', 'fa-socks')
content = content.replace('Rs. ', '')

# Add dark theme CSS
css_injection = '''        }
        
        /* Proper Dark Theme */
        body.dark-theme {
            --bg-color: #121212;
            --surface: #1e1e2d;
            --text-main: #ffffff;
            --text-muted: #a0a0b0;
            --secondary: #2a2a35;
        }
        body.dark-theme .bg-shape {
            opacity: 0.1;
        }
        body.dark-theme .card, body.dark-theme .sidebar, body.dark-theme .stat-box {
            box-shadow: 0 10px 40px rgba(0,0,0,0.3);
        }
        body.dark-theme input {
            background-color: var(--secondary);
            border-color: #333;
            color: #fff;
        }
    </style>'''
content = content.replace('        }\n    </style>', css_injection)

# Add Toggle Dark Mode Nav item back
nav_injection = '''            <a th:href="@{/invoices}" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-file-invoice"></i>
                    Invoices
                </li>
            </a>
            <a href="#" onclick="toggleDarkMode()" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-moon"></i>
                    Toggle Dark Mode
                </li>
            </a>'''
content = content.replace('''            <a th:href="@{/invoices}" style="text-decoration: none;">
                <li class="nav-item">
                    <i class="fa-solid fa-file-invoice"></i>
                    Invoices
                </li>
            </a>''', nav_injection)

# Add JS at the bottom
js_injection = '''    </main>
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
</body>'''
content = content.replace('''    </main>
</body>''', js_injection)

with open('src/main/resources/templates/customer-dashboard.html', 'w', encoding='utf-8') as f:
    f.write(content)

print('Done!')
