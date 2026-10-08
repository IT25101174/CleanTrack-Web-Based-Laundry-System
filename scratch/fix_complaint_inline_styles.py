import re

with open('src/main/resources/templates/complaint-form.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Fix the dark grey "Customer Description" box
html = html.replace(
    'style="background: rgba(15,23,42,0.5); border: 1px solid rgba(255,255,255,0.05);"',
    'class="bg-light border"'
)

# Fix the dark navy "Rate" dropdown
html = html.replace(
    'style="width: auto; background-color: rgba(15, 23, 42, 0.9);"',
    'style="width: auto;"'
)

# And just in case there are any other rgba(15, 23, 42) background hardcodings inside style= blocks, we can aggressively regex them out.
html = re.sub(r'background-color:\s*rgba\(\s*15\s*,\s*23\s*,\s*42\s*,\s*[\d.]+\s*\)\s*;?', '', html)
html = re.sub(r'background:\s*rgba\(\s*15\s*,\s*23\s*,\s*42\s*,\s*[\d.]+\s*\)\s*;?', '', html)

with open('src/main/resources/templates/complaint-form.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('Fixed complaint form inline styles!')
