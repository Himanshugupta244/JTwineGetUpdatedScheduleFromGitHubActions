# Repository Context

Snapshot of the repo understanding.

## Project
- **Name:** GmailProjectForGitHubActions
- **Language:** Java 8 (JDK 1.8)
- **Build:** Maven
- **Dependencies:** Selenium, WebDriverManager, Google APIs, Twilio (REST)

## Key Components
- **JTwine Schedule Scraper** — Selenium-based scraper for interview schedules
- **Twilio Interview Reminder** — Automated voice call reminders before interviews
- **VProp Schedule Fetcher** — Fetches VProp interview schedules
- **Deploy** — Static HTML schedule page with login, dropdowns, and live updates

## Structure
- `src/main/java/GmailProjectForGitHubActions/` — Java source files
- `deploy/` — Static HTML/JS/extensions for deployment
- `target/` — Maven build output
