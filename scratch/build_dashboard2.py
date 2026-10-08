import sys

with open('src/main/resources/templates/customer-dashboard.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace Live Tracking Block
tracking_old = '''            <!-- Left Column: Active Order Tracking -->
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">Live Tracking: Order #CT-99012</h2>
                    <button style="border: none; background: var(--secondary); color: var(--primary); padding: 8px 16px; border-radius: 10px; font-weight: 600; cursor: pointer;">View Details</button>
                </div>
                
                <div style="margin-top: 30px;">
                    <div class="tracking-stage">
                        <div class="stage-icon"><i class="fa-solid fa-box"></i></div>
                        <div class="stage-info">
                            <h5>Order Placed</h5>
                            <p>Oct 7, 2026 - 10:30 AM</p>
                        </div>
                    </div>
                    
                    <div class="tracking-stage">
                        <div class="stage-icon"><i class="fa-solid fa-truck-ramp-box"></i></div>
                        <div class="stage-info">
                            <h5>Received at Branch</h5>
                            <p>Oct 7, 2026 - 01:15 PM</p>
                        </div>
                    </div>
                    
                    <div class="tracking-stage active">
                        <div class="stage-icon"><i class="fa-solid fa-soap"></i></div>
                        <div class="stage-info">
                            <h5>Washing in Progress</h5>
                            <p>Your garments are currently being cleaned.</p>
                            <span class="status-badge status-processing">Current Stage</span>
                        </div>
                    </div>
                    
                    <div class="tracking-stage" style="opacity: 0.4;">
                        <div class="stage-icon"><i class="fa-solid fa-temperature-arrow-up"></i></div>
                        <div class="stage-info">
                            <h5>Drying & Ironing</h5>
                            <p>Pending</p>
                        </div>
                    </div>
                    
                    <div class="tracking-stage" style="opacity: 0.4;">
                        <div class="stage-icon"><i class="fa-solid fa-check"></i></div>
                        <div class="stage-info">
                            <h5>Ready for Collection</h5>
                            <p>Pending</p>
                        </div>
                    </div>
                </div>
            </div>'''

tracking_new = '''            <!-- Left Column: Active Order Tracking -->
            <div class="card">
                <div class="card-header" th:if="${activeOrder != null}">
                    <h2 class="card-title">Live Tracking: Order #<span th:text="${activeOrder.trackingId}">CT-00000</span></h2>
                    <a th:href="@{/orders}" style="text-decoration:none;">
                        <button style="border: none; background: var(--secondary); color: var(--primary); padding: 8px 16px; border-radius: 10px; font-weight: 600; cursor: pointer;">View Details</button>
                    </a>
                </div>
                <div class="card-header" th:if="${activeOrder == null}">
                    <h2 class="card-title">No Active Orders</h2>
                    <a th:href="@{/orders}" style="text-decoration:none;">
                        <button style="border: none; background: var(--secondary); color: var(--primary); padding: 8px 16px; border-radius: 10px; font-weight: 600; cursor: pointer;">Place Order</button>
                    </a>
                </div>
                
                <div style="margin-top: 30px;" th:if="${activeOrder != null}">
                    <div class="tracking-stage" th:classappend="${activeOrder.status == 'Order Placed' ? 'active' : ''}" th:style="${activeOrder.status == 'Order Placed' ? '' : 'opacity: 0.7;'}">
                        <div class="stage-icon"><i class="fa-solid fa-box"></i></div>
                        <div class="stage-info">
                            <h5>Order Placed</h5>
                            <p th:text="${#temporals.format(activeOrder.createdAt, 'MMM d, yyyy - hh:mm a')}">Date</p>
                            <span class="status-badge status-processing" th:if="${activeOrder.status == 'Order Placed'}">Current Stage</span>
                        </div>
                    </div>
                    
                    <div class="tracking-stage" th:classappend="${activeOrder.status == 'WASHING' ? 'active' : ''}" th:style="${activeOrder.status == 'Order Placed' ? 'opacity: 0.4;' : 'opacity: 0.7;'}">
                        <div class="stage-icon"><i class="fa-solid fa-soap"></i></div>
                        <div class="stage-info">
                            <h5>Processing & Washing</h5>
                            <p th:text="${activeOrder.status == 'Order Placed' ? 'Pending' : 'In Progress'}">Pending</p>
                            <span class="status-badge status-processing" th:if="${activeOrder.status == 'WASHING' || activeOrder.status == 'GARMENT INSPECTION' || activeOrder.status == 'IRONING' || activeOrder.status == 'DRYING' || activeOrder.status == 'DRY CLEAN'}">Current Stage</span>
                        </div>
                    </div>
                    
                    <div class="tracking-stage" th:classappend="${activeOrder.status == 'READY FOR COLLECTION' ? 'active' : ''}" th:style="${activeOrder.status == 'READY FOR COLLECTION' ? 'opacity: 1;' : 'opacity: 0.4;'}">
                        <div class="stage-icon"><i class="fa-solid fa-check"></i></div>
                        <div class="stage-info">
                            <h5>Ready for Collection</h5>
                            <p th:text="${activeOrder.status == 'READY FOR COLLECTION' ? 'Please pickup your items' : 'Pending'}">Pending</p>
                            <span class="status-badge status-processing" th:if="${activeOrder.status == 'READY FOR COLLECTION'}">Current Stage</span>
                        </div>
                    </div>
                </div>
                
                <div style="margin-top: 30px;" th:if="${activeOrder == null}">
                    <p style="color: var(--text-muted);">You don't have any laundry orders being processed right now. Check your past orders or place a new one!</p>
                </div>
            </div>'''

content = content.replace(tracking_old, tracking_new)

# Replace Invoices Block
invoices_old = '''            <!-- Right Column: Recent Activity / Promo -->
            <div style="display: flex; flex-direction: column; gap: 30px;">
                <div class="card">
                    <h2 class="card-title" style="margin-bottom: 20px; font-size: 18px;">Recent Invoices</h2>
                    <div style="display: flex; justify-content: space-between; align-items: center; padding: 15px 0; border-bottom: 1px solid rgba(0,0,0,0.05);">
                        <div>
                            <p style="font-weight: 600;">INV-1093</p>
                            <p style="font-size: 13px; color: var(--text-muted);">Oct 5, 2026</p>
                        </div>
                        <span style="background: rgba(76, 175, 80, 0.1); color: #4CAF50; padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 600;">PAID</span>
                    </div>
                    <div style="display: flex; justify-content: space-between; align-items: center; padding: 15px 0;">
                        <div>
                            <p style="font-weight: 600;">INV-1021</p>
                            <p style="font-size: 13px; color: var(--text-muted);">Sep 28, 2026</p>
                        </div>
                        <span style="background: rgba(76, 175, 80, 0.1); color: #4CAF50; padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 600;">PAID</span>
                    </div>
                    <button style="width: 100%; border: 1px dashed var(--primary); background: transparent; color: var(--primary); padding: 10px; border-radius: 10px; font-weight: 600; margin-top: 10px; cursor: pointer;">View All History</button>
                </div>
            </div>'''

invoices_new = '''            <!-- Right Column: Recent Activity / Promo -->
            <div style="display: flex; flex-direction: column; gap: 30px;">
                <div class="card">
                    <h2 class="card-title" style="margin-bottom: 20px; font-size: 18px;">Recent Invoices</h2>
                    
                    <div th:if="${recentInvoices == null || recentInvoices.empty}">
                        <p style="color: var(--text-muted); font-size: 14px;">No recent invoices.</p>
                    </div>
                    
                    <div th:each="invoice : ${recentInvoices}" style="display: flex; justify-content: space-between; align-items: center; padding: 15px 0; border-bottom: 1px solid rgba(0,0,0,0.05);">
                        <div>
                            <p style="font-weight: 600;" th:text="'INV-' + ${invoice.id}">INV-1093</p>
                            <p style="font-size: 13px; color: var(--text-muted);" th:text="${#temporals.format(invoice.createdAt, 'MMM d, yyyy')}">Oct 5, 2026</p>
                        </div>
                        <span th:if="${invoice.status == 'PAID'}" style="background: rgba(76, 175, 80, 0.1); color: #4CAF50; padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 600;">PAID</span>
                        <span th:if="${invoice.status != 'PAID'}" style="background: rgba(255, 152, 0, 0.1); color: #FF9800; padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 600;" th:text="${invoice.status}">PENDING</span>
                    </div>
                    
                    <a th:href="@{/invoices}" style="text-decoration:none;">
                        <button style="width: 100%; border: 1px dashed var(--primary); background: transparent; color: var(--primary); padding: 10px; border-radius: 10px; font-weight: 600; margin-top: 10px; cursor: pointer;">View All History</button>
                    </a>
                </div>
            </div>'''

content = content.replace(invoices_old, invoices_new)

with open('src/main/resources/templates/customer-dashboard.html', 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
