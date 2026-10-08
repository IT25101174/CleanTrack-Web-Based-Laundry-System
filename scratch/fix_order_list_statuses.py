import re

with open('src/main/resources/templates/order-list.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Replace the specific hardcoded badge block with a dynamic one
old_block = """                                <span class="badge badge-pending px-3 py-2 rounded-pill" th:if="${order.status == 'Order Placed' or order.status == 'PENDING'}">Order Placed</span>
                                <span class="badge badge-processing px-3 py-2 rounded-pill" th:if="${order.status == 'WASHING' or order.status == 'DRYING' or order.status == 'IRONING'}"><span th:text="${order.status}">PROCESSING</span></span>
                                <span class="badge badge-completed px-3 py-2 rounded-pill" th:if="${order.status == 'COMPLETED' or order.status == 'READY FOR COLLECTION'}"><span th:text="${order.status}">COMPLETED</span></span>
                                <span class="badge badge-cancelled px-3 py-2 rounded-pill" th:if="${order.status == 'Cancelled'}">Cancelled</span>"""

new_block = """                                <!-- Dynamic Status Badge -->
                                <span class="badge px-3 py-2 rounded-pill" 
                                      th:classappend="${
                                          (order.status == 'Order Placed' or order.status == 'PENDING' ? 'bg-warning text-dark' : '') +
                                          (order.status == 'RECEIVED' or order.status == 'WASHING' or order.status == 'DRYING' or order.status == 'IRONING' or order.status == 'GARMENT INSPECTION' or order.status == 'DRY CLEAN' or order.status == 'PROCESSING' ? 'bg-info text-white' : '') +
                                          (order.status == 'READY FOR COLLECTION' or order.status == 'Order Completed' or order.status == 'COMPLETED' ? 'bg-success text-white' : '') +
                                          (order.status == 'Cancelled' or order.status == 'CANCELLED' ? 'bg-danger text-white' : '')
                                      }"
                                      th:text="${order.status}">Status</span>"""

html = html.replace(old_block, new_block)

with open('src/main/resources/templates/order-list.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('Updated order-list.html statuses successfully!')
