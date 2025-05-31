# Price Notifier

Track item prices on eBay or Grailed and get notified via email or SMS when they drop below your target price.

## Features
- Track any item by name, platform, and target price
- Scrape eBay listings using JSoup
- Scheduled checks with Spring Boot
- Notifications via Email (JavaMail) or SMS (Twilio)
- Frontend built with React + TypeScript

## Tech Stack
- Java + Spring Boot
- React + TypeScript
- PostgreSQL / H2
- JSoup
- Twilio / JavaMail

## How to Run
### Backend
```bash
cd backend
./mvnw spring-boot:run
