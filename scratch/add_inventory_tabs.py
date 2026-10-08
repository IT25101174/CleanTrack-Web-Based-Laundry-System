import re

tabs_html = '''
        <!-- Inventory Module Navigation Tabs -->
        <ul class="nav nav-pills mb-4" style="background: rgba(109, 141, 255, 0.05); padding: 5px; border-radius: 12px; display: inline-flex;">
            <li class="nav-item">
                <a class="nav-link {active_inventory}" href="/inventory" style="border-radius: 8px; font-weight: 500;"><i class="fa-solid fa-boxes-stacked me-2"></i>Inventory List</a>
            </li>
            <li class="nav-item">
                <a class="nav-link {active_suppliers}" href="/suppliers" style="border-radius: 8px; font-weight: 500;"><i class="fa-solid fa-truck-field me-2"></i>Suppliers</a>
            </li>
            <li class="nav-item">
                <a class="nav-link {active_orders}" href="/supply-orders" style="border-radius: 8px; font-weight: 500;"><i class="fa-solid fa-clipboard-list me-2"></i>Supply Orders</a>
            </li>
        </ul>
'''

files = [
    ('src/main/resources/templates/inventory-list.html', 'inventory'),
    ('src/main/resources/templates/supplier-list.html', 'suppliers'),
    ('src/main/resources/templates/supply-order-list.html', 'orders')
]

for filepath, active in files:
    with open(filepath, 'r', encoding='utf-8') as f:
        html = f.read()
    
    if '<!-- Inventory Module Navigation Tabs -->' in html:
        continue # Already added
        
    current_tabs = tabs_html.format(
        active_inventory='active' if active == 'inventory' else 'text-muted',
        active_suppliers='active' if active == 'suppliers' else 'text-muted',
        active_orders='active' if active == 'orders' else 'text-muted'
    )
    
    # Inject right after <div class="inventory-wrapper" style="padding: 30px;">
    wrapper_tag = '<div class="inventory-wrapper" style="padding: 30px;">'
    if wrapper_tag in html:
        html = html.replace(wrapper_tag, wrapper_tag + '\n' + current_tabs)
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(html)

print('Added tabs to all inventory templates!')
