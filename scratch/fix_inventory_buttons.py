import re

files = [
    'src/main/resources/templates/inventory-list.html',
    'src/main/resources/templates/supplier-list.html',
    'src/main/resources/templates/supply-order-list.html'
]

for filepath in files:
    with open(filepath, 'r', encoding='utf-8') as f:
        html = f.read()

    # Find the tabs
    tabs_match = re.search(r'<!-- Inventory Module Navigation Tabs -->.*?</ul>', html, re.DOTALL)
    if not tabs_match:
        continue
    tabs_html = tabs_match.group(0)

    # Find the injected button wrapper (the one I added with <div class="d-flex justify-content-end mb-3">)
    injected_btn_match = re.search(r'<div class="d-flex justify-content-end mb-3">(<button.*?</button>)</div>', html, re.DOTALL)
    btn_html = injected_btn_match.group(1) if injected_btn_match else ''
    
    if not btn_html:
        # Fallback if I didn't inject it like that (e.g., supplier list)
        btn_match = re.search(r'<button[^>]*data-bs-target="#(addSupplierModal|addItemModal|addOrderModal)"[^>]*>.*?</button>', html, re.DOTALL)
        if btn_match:
            btn_html = btn_match.group(0)

    # 1. Remove tabs
    html = html.replace(tabs_html, '')
    
    # 2. Remove injected button
    if injected_btn_match:
        html = html.replace(injected_btn_match.group(0), '')
        
    # 3. Remove leftover button from ct-header
    html = re.sub(r'<button[^>]*data-bs-target="#(addSupplierModal|addItemModal|addOrderModal)"[^>]*>.*?</button>', '', html, flags=re.DOTALL)
    
    # 4. Construct new flex container right after wrapper
    tabs_html_clean = tabs_html.replace(' mb-4', ' mb-0') # remove margin bottom from tabs
    
    new_header_row = f'''
    <div class="d-flex justify-content-between align-items-center mb-4">
        {tabs_html_clean}
        {btn_html}
    </div>
    '''
    
    wrapper_tag = '<div class="inventory-wrapper" style="padding: 30px;">'
    html = html.replace(wrapper_tag, wrapper_tag + new_header_row)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(html)

print('Fixed layout and duplicate buttons!')
