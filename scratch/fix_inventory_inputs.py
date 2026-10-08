import re

with open('src/main/resources/static/css/inventory-ui-clean.css', 'r', encoding='utf-8') as f:
    css = f.read()

# Replace .form-control dark backgrounds with adaptive ones
css = css.replace('background-color: rgba(10, 14, 24, 0.6);', 'background-color: var(--ct-surface);')
css = css.replace('background-color: rgba(10, 14, 24, 0.85);', 'background-color: var(--ct-surface);')
css = css.replace('background: #131a2b;', 'background: var(--ct-surface);')
css = css.replace('color: #6b7488;', 'color: var(--ct-muted);')
css = css.replace('background: rgba(10, 14, 24, 0.6);', 'background: var(--ct-surface);')

# The pill toggles have dark hardcoded colors too:
# .segmented { display: inline-flex; background: rgba(10, 14, 24, 0.6); ...
css = css.replace('background: rgba(10, 14, 24, 0.6);', 'background: var(--ct-surface);')
css = css.replace('background: #0b0f1a;', 'background: var(--ct-surface);')

with open('src/main/resources/static/css/inventory-ui-clean.css', 'w', encoding='utf-8') as f:
    f.write(css)

print('Cleaned input backgrounds in CSS!')
