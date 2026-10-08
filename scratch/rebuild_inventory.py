import re

# Get layout pieces from invoice-view
with open('src/main/resources/templates/invoice-view.html', 'r', encoding='utf-8') as f:
    invoice_html = f.read()

layout_top_end = invoice_html.find('<div style="padding: 30px;">')
layout_top_base = invoice_html[:layout_top_end]

layout_bottom_start = invoice_html.find('<!-- Bootstrap JS -->')
layout_bottom_base = invoice_html[layout_bottom_start:]

# We need to process 3 files
files = [
    ('src/main/resources/templates/inventory-list.html', 'CleanTrack - Inventory', 'Inventory Management', 'Track laundry supplies, stock levels, and low-stock alerts.', 'fa-boxes-stacked', 'Inventory'),
    ('src/main/resources/templates/supplier-list.html', 'CleanTrack - Suppliers', 'Suppliers', 'Manage supplier contacts and details.', 'fa-truck-field', 'Inventory'), # We'll keep Inventory tab active
    ('src/main/resources/templates/supply-order-list.html', 'CleanTrack - Supply Orders', 'Supply Orders', 'Manage purchase orders for supplies.', 'fa-clipboard-list', 'Inventory')
]

for filepath, title, heading, subtitle, icon, active_tab in files:
    with open(filepath, 'r', encoding='utf-8') as f:
        html = f.read()
    
    # Customize layout_top for this specific file
    layout_top = layout_top_base
    
    # 1. Update Title, Heading, Subtitle
    layout_top = layout_top.replace('<title>CleanTrack - Invoices</title>', f'<title>{title}</title>')
    layout_top = layout_top.replace('<h1>Billing & Invoices</h1>', f'<h1>{heading}</h1>')
    layout_top = layout_top.replace('<p>Manage payments and view past receipts.</p>', f'<p>{subtitle}</p>')
    
    # 2. Update Active Tab in Sidebar
    # First, deactivate Invoices
    layout_top = re.sub(r'<li class="nav-item active">\s*<i class="fa-solid fa-file-invoice"></i>\s*Invoices\s*</li>', r'<li class="nav-item">\n                    <i class="fa-solid fa-file-invoice"></i>\n                    Invoices\n                </li>', layout_top)
    
    # Then activate Inventory
    layout_top = re.sub(r'<li class="nav-item">\s*<i class="fa-solid fa-boxes-stacked"></i>\s*Inventory\s*</li>', r'<li class="nav-item active">\n                    <i class="fa-solid fa-boxes-stacked"></i>\n                    Inventory\n                </li>', layout_top)
    
    # Keep the inventory-ui CSS
    if 'inventory-ui.css' in html:
        layout_top = layout_top.replace('</head>', '    <link rel="stylesheet" th:href="@{/css/inventory-ui.css}">\n</head>')

    # 3. Extract Main Content
    # Main content starts after <div class="container ct-container">
    main_start = html.find('<div class="container ct-container">')
    # Actually, we can just grab everything from <div class="container ct-container"> to the end, then trim </body></html>
    if main_start != -1:
        main_end = html.find('<!-- Bootstrap Bundle')
        if main_end == -1:
            main_end = html.rfind('</body>')
            
        main_content = html[main_start:main_end]
        
        # Replace the ct-container class so it uses our new padding instead
        main_content = main_content.replace('<div class="container ct-container">', '')
        
        # We need to remove the ct-header since we put the heading in the new top header layout
        # <div class="ct-header">...</div>
        ct_header_start = main_content.find('<div class="ct-header">')
        if ct_header_start != -1:
            ct_header_end = main_content.find('</div>', main_content.find('</div>', ct_header_start) + 6) + 6 # very fragile but let's just regex it
            
            # Use regex to remove ct-header div entirely
            # Actually, the Add button is in ct-header! We can't just delete it.
            # We'll just change ct-header to standard flex div
            main_content = main_content.replace('<div class="ct-header">', '<div class="d-flex justify-content-between align-items-center mb-4">')
            # But wait, ct-header contains the heading <h2> and <p>. We should remove them so they don't duplicate.
            main_content = re.sub(r'<div>\s*<h2>.*?</h2>\s*<p.*?</p>\s*</div>', '<div></div>', main_content, flags=re.DOTALL)
            
        # 4. Remove dark hardcodings if any exist in the HTML (inventory-ui.css handles most)
        main_content = main_content.replace('text-white', '')
        
        # 5. Assemble
        new_html = layout_top + '\n<div style="padding: 30px;">\n' + main_content + '\n</div>\n    </main>\n' + layout_bottom_base
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_html)

print('Updated Inventory templates!')
