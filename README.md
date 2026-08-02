<div align="center">

# 🔗 URL Shortener

### A Production-Ready URL Shortener built with Spring Boot, MySQL, Redis, Flyway & Docker

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge\&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?style=for-the-badge\&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8-blue?style=for-the-badge\&logo=mysql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=for-the-badge\&logo=redis)
![Flyway](https://img.shields.io/badge/Flyway-Database_Migrations-cc0000?style=for-the-badge)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge\&logo=docker)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

</div>

---

# 📖 About

A Spring Boot **URL Shortener application** with Redis caching, MySQL persistence, Flyway database migrations, rate limiting, and click analytics.

This project converts long URLs into short, shareable links. It also tracks redirects, stores click data, applies rate limiting, and uses Redis for fast access.

The goal of this project is to showcase backend engineering practices commonly used in modern Java applications.

---

# ✨ Features

* 🔗 Shorten long URLs into compact short codes
* ✍️ Support custom aliases
* ↪️ Redirect short URLs to the original destination
* 📊 Track click count and click analytics
* 🚦 Redis-based rate limiting
* ⚡ Cache original URLs in Redis for fast redirects
* 🗄️ Persist URLs and click events in MySQL
* 🔄 Manage database schema with Flyway
* 🐳 Containerized with Docker and Docker Compose

---

# 🏗️ System Architecture

> Replace this placeholder with your architecture diagram.

<p align="center">

**📷 Architecture Diagram**

```
docs/images/architecture.png
```

</p>

---

# 🔄 Application Flow

## Main flow

1. User sends a long URL to the shorten endpoint.
2. Application generates a unique short code.
3. URL data is stored in MySQL.
4. Original URL is cached in Redis.
5. User opens the short URL.
6. Application checks Redis first.
7. If found, the user is redirected immediately.
8. Click event is stored for analytics.

## URL Shortening Flow

<p align="center">

<img width="1536" height="1024" alt="ChatGPT Image Aug 1, 2026, 09_12_52 PM" src="https://github.com/user-attachments/assets/4a17a891-e7c0-434c-86bc-200df70be90a" />

</p>

---

## Redirect Flow

<p align="center">

<img width="1536" height="1024" alt="ChatGPT Image Aug 1, 2026, 09_13_08 PM" src="https://github.com/user-attachments/assets/ccf89455-d300-4bc2-b2ed-d890cdb51b9c" />

</p>

---

# 📌 Results

## Create Short URL

<p align="center">
  
<img width="1384" height="688" alt="Screenshot 2026-08-01 212920" src="https://github.com/user-attachments/assets/57898219-a05c-4922-ac5d-b51183ed6632" />

</p>

---

## Redirect

<p align="center">

<img width="1382" height="705" alt="Screenshot 2026-08-01 213011" src="https://github.com/user-attachments/assets/94688249-966c-4950-8940-848ee0936a63" />

</p>

---

## URL Statistics

<p align="center">

<img width="1381" height="617" alt="Screenshot 2026-08-01 213047" src="https://github.com/user-attachments/assets/e9a1ba61-dd42-43dd-9022-5158c9e2b4ca" />

</p>

---

## URL Analytics

<p align="center">

<img width="1382" height="929" alt="Screenshot 2026-08-01 213123" src="https://github.com/user-attachments/assets/8eecc373-d27b-4609-93f9-832debea939d" />

</p>

---

# 🛠️ Tech Stack

| Category         | Technology      |
| ---------------- | --------------- |
| Language         | Java 21         |
| Framework        | Spring Boot     |
| Build Tool       | Maven           |
| Database         | MySQL           |
| Cache            | Redis           |
| ORM              | Spring Data JPA |
| Migration        | Flyway          |
| Containerization | Docker          |
| Utilities        | Lombok          |

---

# 📂 Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.ragul.UrlShortener
│   │       ├── config
│   │       ├── controller
│   │       ├── dto
│   │       ├── model
│   │       ├── repository
│   │       ├── service
│   │       ├── exception
│   │       └── util
│   │
│   └── resources
│       ├── application.yml
│       └── db
│           └── migration
│
└── test
```

---

# 🌐 REST API

| Method | Endpoint                     | Description              |
| ------ | ---------------------------- | ------------------------ |
| POST   | `/api/shorten`               | Create a short URL       |
| GET    | `/api/{shortCode}`           | Redirect to original URL |
| GET    | `/api/stats/{shortCode}`     | URL statistics           |
| GET    | `/api/analytics/{shortCode}` | Click analytics          |
| DELETE | `/api/{shortCode}`           | Delete URL               |

---

# 📥 Sample Request

```json
{
  "originalUrl": "https://example.com/articles/spring-boot",
  "customAlias": "spring",
  "expiresAt": "2026-12-31T23:59:59"
}
```

---

# 📤 Sample Response

```json
{
  "shortUrl": "http://localhost:8080/api/spring",
  "shortCode": "spring",
  "originalUrl": "https://example.com/articles/spring-boot",
  "createdAt": "2026-08-01T10:30:00",
  "expiresAt": "2026-12-31T23:59:59"
}
```

---

# ⚙️ Configuration

Configure your environment variables:

```properties
DB_URL=jdbc:mysql://localhost:3306/url_shortener
DB_USERNAME=root
DB_PASSWORD=password
REDIS_HOST=localhost
REDIS_PORT=6379
```

> Never commit your `.env` file. Use `.env.example` instead.

---

# ⚡ application.yml Example

```yaml
server:
  port: 8080

spring:
  application:
    name: UrlShortener

  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      timeout: 2000ms

url-shortener:
  base-url: http://localhost:8080
  short-code:
    length: 6
    max-attempts: 10
  rate-limit:
    max-requests-per-minute: 2
    max-requests-per-hour: 10
    minute-window: 1m
    hour-window: 1h
  cache:
    ttl-minutes: 30
```

---

# 🚀 Running Locally

## Clone Repository

```bash
git clone https://github.com/<your-username>/UrlShortener.git
cd UrlShortener
```

## Start MySQL & Redis

Make sure both services are running.

## Run Application

```bash
./mvnw spring-boot:run
```

or

```bash
mvn spring-boot:run
```

---

# 🐳 Docker

Build and start all services

```bash
docker compose up --build
```

Run in detached mode

```bash
docker compose up -d
```

Stop containers

```bash
docker compose down
```

---

# 🗄️ Database

Flyway automatically creates and manages the database schema.

Main Tables

* `url_data`
* `click_events`

---

# 📈 Future Enhancements

* Add authentication and user-specific URL management
* QR code generation
* Add Swagger/OpenAPI documentation
* Add unit and integration tests
* Add a dashboard UI for analytics
* Add custom domain support
* Monitoring with Prometheus & Grafana

---

# 🤝 Contributing

Contributions, suggestions, and improvements are welcome.

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Open a Pull Request

---

# 📄 License

This project is licensed under the **MIT License**.

---

<div align="center">

### ⭐ If you found this project helpful, consider giving it a star!

Built with ❤️ using **Spring Boot**, **Redis**, **MySQL**, and **Docker**

</div>
