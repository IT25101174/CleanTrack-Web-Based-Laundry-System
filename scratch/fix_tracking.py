with open('src/main/resources/templates/customer-dashboard.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace stage 1
s1_old = '''<div class="tracking-stage" th:classappend="${activeOrder.status == 'Order Placed' ? 'active' : ''}" th:style="${activeOrder.status == 'Order Placed' ? '' : 'opacity: 0.7;'}">'''
s1_new = '''<div class="tracking-stage" th:classappend="${activeOrder.status == 'Order Placed' ? 'active' : ''}" style="opacity: 1;">'''

# Replace stage 2
s2_old = '''<div class="tracking-stage" th:classappend="${activeOrder.status == 'WASHING' ? 'active' : ''}" th:style="${activeOrder.status == 'Order Placed' ? 'opacity: 0.4;' : 'opacity: 0.7;'}">'''
s2_new = '''<div class="tracking-stage" th:classappend="${activeOrder.status == 'WASHING' || activeOrder.status == 'GARMENT INSPECTION' || activeOrder.status == 'IRONING' || activeOrder.status == 'DRYING' || activeOrder.status == 'DRY CLEAN' || activeOrder.status == 'PROCESSING' ? 'active' : ''}" th:style="${activeOrder.status == 'Order Placed' ? 'opacity: 0.4;' : 'opacity: 1;'}">'''

# Replace stage 3
s3_old = '''<div class="tracking-stage" th:classappend="${activeOrder.status == 'READY FOR COLLECTION' ? 'active' : ''}" th:style="${activeOrder.status == 'READY FOR COLLECTION' ? 'opacity: 1;' : 'opacity: 0.4;'}">'''
s3_new = '''<div class="tracking-stage" th:classappend="${activeOrder.status == 'READY FOR COLLECTION' ? 'active' : ''}" th:style="${activeOrder.status == 'READY FOR COLLECTION' ? 'opacity: 1;' : 'opacity: 0.4;'}">'''

# Also fix the text logic inside stage 2
p2_old = '''<p th:text="${activeOrder.status == 'Order Placed' ? 'Pending' : 'In Progress'}">Pending</p>'''
p2_new = '''<p th:text="${activeOrder.status == 'Order Placed' ? 'Pending' : (activeOrder.status == 'READY FOR COLLECTION' ? 'Completed' : 'In Progress')}">Pending</p>'''

# Fix the current stage badge condition
b2_old = '''<span class="status-badge status-processing" th:if="${activeOrder.status == 'WASHING' || activeOrder.status == 'GARMENT INSPECTION' || activeOrder.status == 'IRONING' || activeOrder.status == 'DRYING' || activeOrder.status == 'DRY CLEAN'}">Current Stage</span>'''
b2_new = '''<span class="status-badge status-processing" th:if="${activeOrder.status == 'WASHING' || activeOrder.status == 'GARMENT INSPECTION' || activeOrder.status == 'IRONING' || activeOrder.status == 'DRYING' || activeOrder.status == 'DRY CLEAN' || activeOrder.status == 'PROCESSING'}">Current Stage</span>'''

content = content.replace(s1_old, s1_new)
content = content.replace(s2_old, s2_new)
content = content.replace(s3_old, s3_new)
content = content.replace(p2_old, p2_new)
content = content.replace(b2_old, b2_new)

with open('src/main/resources/templates/customer-dashboard.html', 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
