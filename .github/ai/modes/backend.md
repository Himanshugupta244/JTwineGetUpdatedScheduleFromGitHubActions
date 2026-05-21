# Backend Mode

## Focus
Java 8 backend development with Maven.

## Conventions
- Package: `GmailProjectForGitHubActions`
- Source/target compatibility: Java 1.8
- Use `LinkedHashMap` for ordered maps
- Use `SimpleDateFormat` with explicit timezone for date parsing
- IST timezone: `TimeZone.getTimeZone("Asia/Kolkata")`

## Libraries
- Selenium WebDriver for browser automation
- WebDriverManager for driver setup
- Google API Client for Sheets/Gmail
- Standard `HttpURLConnection` for REST calls (no extra HTTP libs)

## Patterns
- Static utility methods
- File-based deduplication (e.g., `twilio-called.txt`)
- Single-run execution designed for cron jobs
