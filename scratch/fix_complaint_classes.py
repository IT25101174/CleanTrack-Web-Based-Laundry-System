import re

with open('src/main/resources/templates/complaint-form.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Replace the custom status classes with standard Bootstrap badge classes
html = html.replace("complaint.status == 'OPEN' ? 'status-open'", "complaint.status == 'OPEN' ? 'bg-warning'")
html = html.replace("complaint.status == 'IN_PROGRESS' ? 'status-progress'", "complaint.status == 'IN_PROGRESS' ? 'bg-primary'")
html = html.replace("complaint.status == 'RESOLVED' ? 'status-resolved'", "complaint.status == 'RESOLVED' ? 'bg-success'")
html = html.replace("'status-archived'", "'bg-secondary'")

# Also fix the form controls since it probably had `text-white` or `bg-dark` inside the modal form
html = html.replace('bg-dark text-white', '')

with open('src/main/resources/templates/complaint-form.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('Fixed complaint form classes!')
