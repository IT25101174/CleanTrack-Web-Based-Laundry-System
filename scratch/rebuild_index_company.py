new_html = """<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CleanTrack - Premium Laundry Services</title>
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
            color: var(--text-main);
            border: 2px solid #e2e8f0;
            border-radius: 8px;
            padding: 10px 24px;
            font-weight: 600;
        }

        .btn-outline-custom:hover {
            background-color: #f1f5f9;
            color: var(--text-main);
        }

        /* Hero Section */
        .hero-section {
            padding: 120px 0;
            background-color: white;
            flex-grow: 1;
        }

        .hero-title {
            font-size: 3.8rem;
            font-weight: 800;
            color: var(--text-main);
            margin-bottom: 24px;
            line-height: 1.2;
            letter-spacing: -1px;
        }

        .hero-subtitle {
            font-size: 1.25rem;
            color: #64748b;
            margin-bottom: 40px;
            line-height: 1.6;
        }

        /* Feature Cards */
        .feature-card {
            background: white;
            border-radius: 16px;
            padding: 40px 30px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
            border: 1px solid rgba(0,0,0,0.04);
            height: 100%;
            text-align: center;
            transition: transform 0.3s ease;
        }

        .feature-card:hover {
            transform: translateY(-5px);
        }

        .icon-box {
            width: 70px;
            height: 70px;
            border-radius: 50%;
            background: #f8fafc;
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
            padding: 40px 0 20px 0;
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
                    <li class="nav-item"><a class="nav-link nav-link-custom" href="#services">Services</a></li>
                    <li class="nav-item"><a class="nav-link nav-link-custom" href="#about">About Us</a></li>
                    <li class="nav-item" th:if="${session.user != null}">
                        <a href="dashboard.html" th:href="@{/dashboard}" class="btn btn-primary-custom ms-3">
                            <i class="fa-solid fa-user me-2"></i>My Account
                        </a>
                    </li>
                    <li class="nav-item" th:if="${session.user == null}">
                        <a href="login.html" th:href="@{/login}" class="nav-link nav-link-custom ms-2">Log In</a>
                    </li>
                    <li class="nav-item" th:if="${session.user == null}">
                        <a href="register.html" th:href="@{/register}" class="btn btn-primary-custom ms-2">Book a Service</a>
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
                    <h1 class="hero-title">Premium Laundry Care for Everyday Wear.</h1>
                    <p class="hero-subtitle">We provide professional washing, dry cleaning, and expert ironing services. Drop off your garments or schedule a pickup, and track your order's progress in real-time through your personal customer portal.</p>
                    <div class="d-flex justify-content-center gap-3 mt-4">
                        <a href="register.html" th:href="${session.user != null} ? @{/dashboard} : @{/register}" class="btn btn-primary-custom btn-lg">
                            <i class="fa-solid fa-calendar-check me-2"></i>Book Now
                        </a>
                        <a href="#services" class="btn btn-outline-custom btn-lg">
                            Our Services
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </header>

    <!-- Features Grid -->
    <section class="py-5" id="services">
        <div class="container my-5">
            <div class="text-center mb-5">
                <h2 class="fw-bold" style="color: var(--text-main);">Why Choose CleanTrack?</h2>
                <p class="text-muted">Experience laundry service that is transparent, fast, and high-quality.</p>
            </div>
            
            <div class="row">
                <div class="col-md-4 mb-4">
                    <div class="feature-card">
                        <div class="icon-box"><i class="fa-solid fa-shirt"></i></div>
                        <h5 class="fw-bold mb-3">Expert Garment Care</h5>
                        <p class="text-muted mb-0">From delicate silks to heavy winter coats, our trained staff uses premium, eco-friendly detergents to keep your clothes looking brand new.</p>
                    </div>
                </div>
                
                <div class="col-md-4 mb-4">
                    <div class="feature-card">
                        <div class="icon-box"><i class="fa-solid fa-mobile-screen"></i></div>
                        <h5 class="fw-bold mb-3">Live Order Tracking</h5>
                        <p class="text-muted mb-0">Log in to your customer portal to see exactly when your clothes are being washed, dried, ironed, and ready for collection.</p>
                    </div>
                </div>
                
                <div class="col-md-4 mb-4">
                    <div class="feature-card">
                        <div class="icon-box"><i class="fa-solid fa-file-invoice-dollar"></i></div>
                        <h5 class="fw-bold mb-3">Transparent Pricing</h5>
                        <p class="text-muted mb-0">No hidden fees. View detailed digital invoices directly from your dashboard and pay securely upon pickup or delivery.</p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Footer Area -->
    <footer class="footer mt-5">
        <div class="container">
            <div class="row">
                <div class="col-md-6 mb-4 mb-md-0 text-center text-md-start">
                    <div class="mb-3">
                        <i class="fa-solid fa-droplet me-2" style="color: var(--primary); font-size: 1.5rem;"></i><span class="fw-bold fs-4" style="color: var(--text-main);">CleanTrack</span>
                    </div>
                    <p class="text-muted small">Your trusted partner for professional laundry and dry cleaning services.</p>
                </div>
                <div class="col-md-6 text-center text-md-end">
                    <p class="text-muted small mb-1"><i class="fa-solid fa-location-dot me-2"></i>123 Clean Street, Laundryville</p>
                    <p class="text-muted small mb-1"><i class="fa-solid fa-phone me-2"></i>(555) 123-4567</p>
                    <p class="text-muted small mb-0"><i class="fa-solid fa-envelope me-2"></i>hello@cleantrack.com</p>
                </div>
            </div>
            <hr class="my-4" style="border-color: rgba(0,0,0,0.05);">
            <div class="text-center">
                <p class="text-muted small mb-0">&copy; 2026 CleanTrack Laundry Services. All rights reserved.</p>
            </div>
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
