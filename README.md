# 📧 Email + 📱 SMS OTP Verification (Spring Boot)

This project provides **OTP verification** via:
- Email (JavaMail)
- SMS (Twilio)

It also includes:
- OTP expiry (5 minutes)
- Attempt limit for security

---

## 🚀 Requirements

- **Java 21** (LTS) → Do NOT use JDK 24 (not fully supported by Spring Boot yet)
- Maven 3.9+
- A Gmail account (for email OTPs)
- A Twilio account (for SMS OTPs)

---

## ⚙️ Setup

### 1. Configure Email (Gmail SMTP)
Edit `src/main/resources/application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
