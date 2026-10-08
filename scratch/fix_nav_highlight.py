import re

# order-list.html
with open('src/main/resources/templates/order-list.html', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'<li class="nav-item active">(\s*<i class="fa-solid fa-house">)', r'<li class="nav-item">\1', content)
content = re.sub(r'<li class="nav-item">(\s*<i class="fa-solid fa-shirt">)', r'<li class="nav-item active">\1', content)

with open('src/main/resources/templates/order-list.html', 'w', encoding='utf-8') as f:
    f.write(content)

# invoice-view.html
with open('src/main/resources/templates/invoice-view.html', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'<li class="nav-item active">(\s*<i class="fa-solid fa-house">)', r'<li class="nav-item">\1', content)
content = re.sub(r'<li class="nav-item">(\s*<i class="fa-solid fa-file-invoice">)', r'<li class="nav-item active">\1', content)

with open('src/main/resources/templates/invoice-view.html', 'w', encoding='utf-8') as f:
    f.write(content)
print('Fixed active nav items')
