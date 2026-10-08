import re

with open('src/main/resources/static/css/inventory-ui-clean.css', 'r', encoding='utf-8') as f:
    css = f.read()

# Fix button colors
css = css.replace('color: #b8c8ff;', 'color: var(--ct-accent);')
css = css.replace('color: #6ee7b7;', 'color: var(--ct-ok);')
css = css.replace('color: #fca5a5;', 'color: var(--ct-danger);')

# Fix tag colors
css = css.replace('color: #cbd5e1;', 'color: var(--ct-muted);')
css = css.replace('color: #fcd34d;', 'color: var(--ct-warn);')

# The button hover background on light mode should probably be a bit darker than rgba(..., 0.09)
css = css.replace('.btn-act:hover { background: rgba(255, 255, 255, 0.09); }', '.btn-act:hover { background: rgba(0, 0, 0, 0.04); }')

with open('src/main/resources/static/css/inventory-ui-clean.css', 'w', encoding='utf-8') as f:
    f.write(css)

print('Fixed tag and button text colors!')
