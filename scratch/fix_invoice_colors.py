with open('src/main/resources/templates/invoice-view.html', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('class="text-white"', '')
content = content.replace('class="fw-bold text-white fs-5"', 'class="fw-bold fs-5"')
# Fix the h2 in modal if needed, but let's leave modal alone if it has dark background?
# Actually modal might be white in light mode too.
# Let's check modal content. If modal has text-white, it'll be invisible too.
content = content.replace('class="text-white mb-0"', 'class="mb-0"')

with open('src/main/resources/templates/invoice-view.html', 'w', encoding='utf-8') as f:
    f.write(content)
print('Removed text-white classes')
