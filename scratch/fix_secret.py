with open('src/main/resources/templates/secret-admin-register.html', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('th:action="@{/register}"', 'th:action="@{/secret-admin-setup-99}"')
content = content.replace('Create Account', 'Create Secret Admin Account')

with open('src/main/resources/templates/secret-admin-register.html', 'w', encoding='utf-8') as f:
    f.write(content)
print('Done')
