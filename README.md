# 🚗 RoadRescue — Vehicle Breakdown Assistance System

> A full-stack Spring Boot web application that connects stranded drivers with nearby garages in real time.

---

## 📋 Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup & Run](#setup--run)
- [Default Accounts](#default-accounts)
- [API / Routes Overview](#routes-overview)
- [Assignment Requirements Checklist](#assignment-checklist)
- [Team Module Split](#team-module-split)

---

## ✨ Features

| Feature | Details |
|---|---|
| **Authentication** | Session/cookie login, Remember-Me (7 days), CSRF protection |
| **Role-based Access** | ROLE_USER · ROLE_GARAGE_OWNER · ROLE_TECHNICIAN · ROLE_ADMIN |
| **Breakdown Requests** | GPS location picker, problem type selection, real-time status |
| **Garage Finder** | Haversine formula — finds nearest verified garages within 15 km |
| **Smart Matching** | Filters garages by service type (Flat Tyre → tyre-repair garages only) |
| **Real-time Updates** | WebSocket (STOMP over SockJS) — instant status push to driver |
| **Email Notifications** | Async emails on registration, request accepted, request completed |
| **Google Maps** | Interactive pin-drop for location, garage map view |
| **Reviews & Ratings** | 1–5 star rating system with average calculation |
| **Payments** | Payment record creation and confirmation |
| **Pagination & Search** | All list screens paginated; search on users/garages |
| **Soft Deletes** | All entities support soft delete with `is_deleted` flag |
| **Audit Trail** | `created_at`, `updated_at`, `created_by` on all entities |
| **Caching** | `@Cacheable` on garage list endpoint |
| **Error Handling** | Global `@ControllerAdvice` → custom 404/403/500 pages |
| **Bean Validation** | All forms use Jakarta Validation with friendly UI error messages |

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2, Spring Security, Spring Data JPA |
| Frontend | Thymeleaf + Bootstrap 5 + Font Awesome 6 |
| Database | MySQL 8 |
| Real-time | WebSocket (STOMP / SockJS) |
| Maps | Google Maps JavaScript API |
| Email | Spring Mail (Gmail SMTP) |
| Build | Maven |
| Java | 21 |

---

## 📁 Project Structure

```
src/main/java/com/roadrescue/
├── RoadRescueApplication.java
├── config/          WebSocketConfig
├── controller/      AuthController, DashboardController, GarageController,
│                    BreakdownRequestController, VehicleController,
│                    GarageOwnerController, AdminController,
│                    ReviewController, PaymentController, NotificationController
├── dto/             RegisterDTO, BreakdownRequestDTO, GarageDTO
├── entity/          User, Garage, Vehicle, Technician, BreakdownRequest,
│                    Assignment, Payment, Review, Notification
├── enums/           Role, RequestStatus, ServiceType, PaymentStatus
├── exception/       GlobalExceptionHandler, ResourceNotFoundException
├── repository/      (one per entity, with custom queries)
├── security/        SecurityConfig, CustomUserDetailsService
└── service/         UserService, GarageService, BreakdownRequestService,
                     VehicleService, TechnicianService, ReviewService,
                     PaymentService, NotificationService, EmailService

src/main/resources/
├── application.properties
├── data.sql                 (seed data)
├── static/css/style.css
├── static/js/main.js
└── templates/
    ├── layout.html
    ├── auth/            login.html, register.html
    ├── user/            dashboard.html
    ├── admin/           dashboard.html, users.html, garages.html
    ├── garage-owner/    requests.html
    ├── garage/          list.html, detail.html, create.html, edit.html
    ├── vehicle/         list.html, create.html, edit.html
    ├── request/         create.html, detail.html, list.html
    ├── notification/    list.html
    ├── review/          create.html
    └── error/           404.html, 403.html, 500.html
```

---

## ⚙️ Setup & Run

### Prerequisites
- Java 21+
- MySQL 8+
- Maven 3.8+
- Google Maps API Key ([get one here](https://console.cloud.google.com/))

### 1. Clone the Repository
```bash
git clone https://github.com/your-team/road-rescue.git
cd road-rescue
```

### 2. Create MySQL Database
```sql
CREATE DATABASE roadrescue_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure `application.properties`
```properties
# Update these values:
spring.datasource.username=root
spring.datasource.password=
spring.mail.username=nadeeshakalhara685@gmail.com
spring.mail.password=gzos elrn xgti arhg
google.maps.api.key=YOUR_GOOGLE_MAPS_KEY
```

### 4. Run the Application
```bash
mvn spring-boot:run
```
App starts at: **http://localhost:8080**

### 5. Load Seed Data
After first run (tables created), execute `src/main/resources/data.sql` in your MySQL client.

---

## 👥 Default Accounts

| Role | Email | Password |
|---|---|---|
| Admin | nadeeshakalhara685.com | admin123 |


---

## 🗺 Routes Overview

| Path | Role | Description |
|---|---|---|
| `GET /auth/login` | Public | Login page |
| `GET /auth/register` | Public | Register page |
| `GET /dashboard` | All | Role-based dashboard |
| `GET /requests/new` | USER | Submit breakdown request |
| `GET /requests` | USER | My requests list |
| `GET /requests/{id}` | Authenticated | Request detail + map |
| `GET /garages` | All | Browse garages (paginated) |
| `GET /garages/{id}` | All | Garage detail + reviews |
| `GET /vehicles` | USER | My vehicles |
| `POST /garage-owner/requests/{id}/accept` | GARAGE_OWNER | Accept request |
| `GET /notifications` | Authenticated | Notification inbox |
| `GET /admin/users` | ADMIN | Manage users |
| `GET /admin/garages` | ADMIN | Verify/manage garages |
| `/ws` | Authenticated | WebSocket endpoint |

---

## ✅ Assignment Checklist

- [x] Sign up / Sign in / Sign out
- [x] Session & Cookie based login (HttpOnly, SameSite)
- [x] Role-based authorization (USER / GARAGE_OWNER / ADMIN)
- [x] Protected routes with `@PreAuthorize`
- [x] At least 2 core entities with relationships (10 entities total)
- [x] Full CRUD on main entities
- [x] Pagination + sorting on list screens
- [x] Bean Validation with friendly UI messages
- [x] Centralized error handling (GlobalExceptionHandler)
- [x] HttpOnly session cookies + CSRF protection
- [x] Spring Data JPA + MySQL
- [x] Seed data
- [x] **Beyond CRUD #1** — External API (Google Maps)
- [x] **Beyond CRUD #2** — Real-time Updates (WebSocket/STOMP)
- [x] **Beyond CRUD #3** — Email Notifications (Spring Mail)
- [x] **Beyond CRUD #4** — Caching (`@Cacheable`)
- [x] **Beyond CRUD #5** — Soft Deletes / Audit Trail
- [x] API documentation (Postman collection — see `/postman`)
- [x] README with setup, run steps

---

## 👨‍💻 Team Module Split

| Member | 
|---|
| Nadeesha Kalhara - 38932 | 
| Tharuka Nimsara - 39764 | 
| Vimukthi Udara - 39313 | 
| R T Lakshan - 39058 | 
| Ishan Shalitha - 39401 | 
| Vihanga Ashan - 39245 | 
| Bhagya Ranaweera - 39333 | 
| Chamodya Diwyanjalee - 39338 | 
| Navodi Dharshika - 39227 | 
| Senanthi Hirunima - 39762 | 

---

## 📬 Postman Collection

Import `https://documenter.getpostman.com/view/54224256/2sBY4HSiDv` for API testing.

---


