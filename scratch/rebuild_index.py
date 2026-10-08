new_html = """<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CleanTrack - Simple Laundry Management</title>
    <!-- Bootstrap 5 CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Google Font: Inter -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <!-- FontAwesome for Icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <style>
        :root {
            --primary: #4318FF;
            --bg-color: #f4f7fe;
            --text-main: #2b3674;
            --text-muted: #a3aed1;
        }

        body {
            font-family: 'Inter', sans-serif;
            background-color: var(--bg-color);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        /* Navbar */
        .navbar-light {
            background-color: white;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
        }

        .navbar-brand-custom {
            font-weight: 700;
            font-size: 1.5rem;
            color: var(--primary) !important;
        }

        .nav-link-custom {
            color: var(--text-main) !important;
            font-weight: 500;
            margin: 0 10px;
        }

        /* Buttons */
        .btn-primary-custom {
            background-color: var(--primary);
            color: white;
            border-radius: 8px;
            padding: 10px 24px;
            font-weight: 600;
            border: none;
        }

        .btn-primary-custom:hover {
            background-color: #3311cc;
            color: white;
        }

        .btn-outline-custom {
            background-color: transparent;
            color: var(--primary);
            border: 2px solid var(--primary);
            border-radius: 8px;
            padding: 8px 22px;
            font-weight: 600;
        }

        .btn-outline-custom:hover {
            background-color: var(--primary);
            color: white;
        }

        /* Hero Section */
        .hero-section {
            padding: 100px 0;
            background-color: white;
            flex-grow: 1;
        }

        .hero-title {
            font-size: 3.5rem;
            font-weight: 800;
            color: var(--text-main);
            margin-bottom: 24px;
            line-height: 1.2;
        }

        .hero-subtitle {
            font-size: 1.2rem;
            color: #6c757d;
            margin-bottom: 40px;
        }

        /* Feature Cards */
        .feature-card {
            background: white;
            border-radius: 16px;
            padding: 30px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
            border: 1px solid rgba(0,0,0,0.05);
            height: 100%;
            text-align: center;
        }

        .icon-box {
            width: 70px;
            height: 70px;
            border-radius: 50%;
            background: #f4f7fe;
            color: var(--primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.8rem;
            margin: 0 auto 20px auto;
        }

        /* Footer */
        .footer {
            background-color: white;
            border-top: 1px solid rgba(0,0,0,0.05);
            padding: 30px 0;
            text-align: center;
            margin-top: auto;
        }
    </style>
</head>
<body>

    <!-- Navigation Header -->
    <nav class="navbar navbar-expand-lg navbar-light py-3 sticky-top">
        <div class="container">
            <a class="navbar-brand navbar-brand-custom" href="index.html" th:href="@{/}">
                <i class="fa-solid fa-droplet me-2"></i>CleanTrack
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse justify-content-end" id="navbarNav">
                <ul class="navbar-nav align-items-center">
                    <li class="nav-item"><a class="nav-link nav-link-custom" href="#features">Features</a></li>
                    <li class="nav-item" th:if="${session.user != null}">
                        <a href="dashboard.html" th:href="@{/dashboard}" class="btn btn-primary-custom ms-3">
                            <i class="fa-solid fa-gauge me-2"></i>Dashboard
                        </a>
                    </li>
                    <li class="nav-item" th:if="${session.user == null}">
                        <a href="login.html" th:href="@{/login}" class="nav-link nav-link-custom">Sign In</a>
                    </li>
                    <li class="nav-item" th:if="${session.user == null}">
                        <a href="register.html" th:href="@{/register}" class="btn btn-primary-custom ms-2">Get Started</a>
                    </li>
                </ul>
            </div>
        </div>
    </nav>

    <!-- Hero Showcase Section -->
    <header class="hero-section text-center">
        <div class="container">
            <div class="row justify-content-center">
                <div class="col-lg-8">
                    <h1 class="hero-title">Simple Laundry Management</h1>
                    <p class="hero-subtitle">A straightforward system to track orders, manage invoices, and handle customer complaints efficiently without the unnecessary fluff.</p>
                    <div class="d-flex justify-content-center gap-3">
                        <a href="register.html" th:href="${session.user != null} ? @{/dashboard} : @{/register}" class="btn btn-primary-custom btn-lg">
                            <i class="fa-solid fa-rocket me-2"></i>Access System
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </header>

    <!-- Features Grid -->
    <section class="py-5" id="features">
        <div class="container">
            <div class="text-center mb-5">
                <h2 class="fw-bold" style="color: var(--text-main);">Core Capabilities</h2>
            </div>
            
            <div class="row">
                <div class="col-md-4 mb-4">
                    <div class="feature-card">
                        <div class="icon-box"><i class="fa-solid fa-clipboard-list"></i></div>
                        <h4 class="fw-bold mb-3">Order Tracking</h4>
                        <p class="text-muted">Easily log new laundry orders, update statuses through washing and drying, and mark them ready.</p>
                    </div>
                </div>
                
                <div class="col-md-4 mb-4">
                    <div class="feature-card">
                        <div class="icon-box"><i class="fa-solid fa-file-invoice-dollar"></i></div>
                        <h4 class="fw-bold mb-3">Invoicing</h4>
                        <p class="text-muted">Generate digital invoices for customers and track unpaid balances seamlessly.</p>
                    </div>
                </div>
                
                <div class="col-md-4 mb-4">
                    <div class="feature-card">
                        <div class="icon-box"><i class="fa-solid fa-headset"></i></div>
                        <h4 class="fw-bold mb-3">Customer Support</h4>
                        <p class="text-muted">Manage customer issues through a dedicated ticket system to ensure satisfaction.</p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Footer Area -->
    <footer class="footer">
        <div class="container">
            <div class="mb-2">
                <i class="fa-solid fa-droplet me-2" style="color: var(--primary);"></i><span class="fw-bold" style="color: var(--text-main);">CleanTrack</span>
            </div>
            <p class="text-muted small mb-0">&copy; 2026 CleanTrack. Clean, simple, and effective.</p>
        </div>
    </footer>

    <!-- Bootstrap 5 Bundle with Popper -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
"""

with open('src/main/resources/templates/index.html', 'w', encoding='utf-8') as f:
    f.write(new_html)

print('Updated homepage successfully!')
