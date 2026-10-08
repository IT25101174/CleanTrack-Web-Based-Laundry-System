import re

with open('src/main/resources/static/css/inventory-ui.css', 'r', encoding='utf-8') as f:
    css = f.read()

# Delete body { ... } completely to let invoice-view's body control layout
css = re.sub(r'body\s*{[^}]*}', '', css)
# Delete .ct-nav { ... }
css = re.sub(r'\.ct-nav\s*{[^}]*}', '', css)

# Redefine :root variables for light mode instead of deleting them
new_root = '''
:root {
    --ct-bg: var(--bg-color);
    --ct-surface: #ffffff;
    --ct-surface-solid: #ffffff;
    --ct-surface-2: #f8fafc;
    --ct-border: rgba(0, 0, 0, 0.08);
    --ct-border-strong: rgba(0, 0, 0, 0.15);
    --ct-text: #1e293b;
    --ct-muted: #64748b;
    --ct-accent: var(--primary);
    --ct-accent-2: #8b7cf6;
    --ct-ok: #10b981;
    --ct-warn: #f59e0b;
    --ct-danger: #ef4444;
    color-scheme: light;
}
'''
css = re.sub(r':root\s*{[^}]*}', new_root, css)

# Fix hardcoded white colors on text elements
css = css.replace('.cell-title { font-weight: 600; color: #fff; }', '.cell-title { font-weight: 600; color: var(--ct-text); }')
css = css.replace('.item-card-name { font-weight: 700; color: #fff; font-size: 1.05rem; line-height: 1.25; }', '.item-card-name { font-weight: 700; color: var(--ct-text); font-size: 1.05rem; line-height: 1.25; }')
css = css.replace('.stat-value { font-size: 1.6rem; font-weight: 700; color: #fff; line-height: 1.2; }', '.stat-value { font-size: 1.6rem; font-weight: 700; color: var(--ct-text); line-height: 1.2; }')

# Fix background colors that were relying on dark mode
css = css.replace('background: rgba(14, 19, 32, 0.85);', 'background: rgba(255, 255, 255, 0.85);')
css = css.replace('background: rgba(255, 255, 255, 0.04);', 'background: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);')
css = css.replace('background-color: rgba(255, 255, 255, 0.04);', 'background-color: #f8fafc;')

with open('src/main/resources/static/css/inventory-ui-clean.css', 'w', encoding='utf-8') as f:
    f.write(css)

# --- NOW FIX THE HTML FILES ---

# Get layout pieces from invoice-view
with open('src/main/resources/templates/invoice-view.html', 'r', encoding='utf-8') as f:
    invoice_html = f.read()

layout_top_end = invoice_html.find('<div style="padding: 30px;">')
layout_top_base = invoice_html[:layout_top_end]

layout_bottom_start = invoice_html.find('<!-- Bootstrap JS -->')
layout_bottom_base = invoice_html[layout_bottom_start:]

files = [
    ('src/main/resources/templates/inventory-list.html', 'CleanTrack - Inventory', 'Inventory Management', 'Track laundry supplies, stock levels, and low-stock alerts.'),
    ('src/main/resources/templates/supplier-list.html', 'CleanTrack - Suppliers', 'Suppliers', 'Manage supplier contacts and details.'),
    ('src/main/resources/templates/supply-order-list.html', 'CleanTrack - Supply Orders', 'Supply Orders', 'Manage purchase orders for supplies.')
]

for filepath, title, heading, subtitle in files:
    with open(filepath, 'r', encoding='utf-8') as f:
        html = f.read()
    
    # Customize layout_top
    layout_top = layout_top_base
    layout_top = layout_top.replace('<title>CleanTrack - Billing & Invoices</title>', f'<title>{title}</title>')
    layout_top = layout_top.replace('<h1>Billing & Invoices</h1>', f'<h1>{heading}</h1>')
    layout_top = layout_top.replace('<p>Manage payments and view past receipts.</p>', f'<p>{subtitle}</p>')
    
    layout_top = re.sub(r'<li class="nav-item active">\s*<i class="fa-solid fa-file-invoice"></i>\s*Invoices\s*</li>', r'<li class="nav-item">\n                    <i class="fa-solid fa-file-invoice"></i>\n                    Invoices\n                </li>', layout_top)
    layout_top = re.sub(r'<li class="nav-item">\s*<i class="fa-solid fa-boxes-stacked"></i>\s*Inventory\s*</li>', r'<li class="nav-item active">\n                    <i class="fa-solid fa-boxes-stacked"></i>\n                    Inventory\n                </li>', layout_top)
    
    # Extract main content properly without breaking div count!
    # Original starts at: <div class="container ct-container">
    # We will replace <div class="container ct-container"> with <div class="inventory-wrapper" style="padding: 30px;">
    # So we don't drop any closing </div>!
    
    # BUT first, we remove the original navbar <nav ...> ... </nav>
    nav_start = html.find('<nav')
    nav_end = html.find('</nav>') + 6
    html = html[:nav_start] + html[nav_end:]
    
    # Replace the outer container
    html = html.replace('<div class="container ct-container">', '<div class="inventory-wrapper" style="padding: 30px;">')
    
    # We remove the <head> entirely since we are prepending layout_top
    body_start = html.find('<body')
    body_end = html.find('>', body_start) + 1
    main_content = html[body_end:]
    
    # We need to drop the ending </body></html> since layout_bottom has it
    main_content = main_content.replace('</body>', '')
    main_content = main_content.replace('</html>', '')
    
    # Replace inventory-ui.css with clean version
    layout_top = layout_top.replace('</head>', '    <link rel="stylesheet" th:href="@{/css/inventory-ui-clean.css}">\n</head>')
    
    # Hide the old header so it doesn't duplicate our new injected header
    main_content = re.sub(r'<div class="ct-header">.*?</div>', '', main_content, flags=re.DOTALL)
    # wait! the Add Item button is inside ct-header.
    # Let's restore just the button.
    add_btn = re.search(r'<button[^>]*data-bs-target="#addItemModal"[^>]*>.*?</button>', html, re.DOTALL)
    if add_btn:
        main_content = f'<div class="d-flex justify-content-end mb-3">{add_btn.group(0)}</div>' + main_content
    elif 'addOrderModal' in html:
        add_btn = re.search(r'<button[^>]*data-bs-target="#addOrderModal"[^>]*>.*?</button>', html, re.DOTALL)
        if add_btn:
            main_content = f'<div class="d-flex justify-content-end mb-3">{add_btn.group(0)}</div>' + main_content
    elif 'addSupplierModal' in html:
        add_btn = re.search(r'<button[^>]*data-bs-target="#addSupplierModal"[^>]*>.*?</button>', html, re.DOTALL)
        if add_btn:
            main_content = f'<div class="d-flex justify-content-end mb-3">{add_btn.group(0)}</div>' + main_content

    # Remove text-white hardcodings
    main_content = main_content.replace('text-white', '')

    new_html = layout_top + main_content + '\n    </main>\n' + layout_bottom_base
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_html)

print('Updated Inventory templates and CSS successfully!')
