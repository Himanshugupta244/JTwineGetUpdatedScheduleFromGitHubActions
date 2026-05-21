# Decisions Log

## Architecture Decisions

### Selenium over Playwright
- **Decision:** Use Selenium WebDriver instead of Playwright
- **Reason:** Better compatibility, existing dependency, no separate browser install needed
- **Date:** May 2026

### File-based Deduplication
- **Decision:** Use `twilio-called.txt` for tracking calls made
- **Reason:** Simple, no database needed, auto-clears on new day
- **Date:** May 2026

### Tight Date-Time Coupling
- **Decision:** Extract date from section's `data-date` attribute instead of assuming "today"
- **Reason:** Prevents calling for tomorrow's interviews; supports multi-day pages
- **Date:** May 2026

### Dual Call Windows
- **Decision:** Call Himanshu/Sudhanshu/Amit at 15min AND 5min before; others at 5min only
- **Reason:** These assignees need earlier warning
- **Date:** May 2026

### Domain-based Auth
- **Decision:** `cloud.codifixsolutions.com` has no login; `confidential.codifixsolutions.com` requires auth
- **Reason:** Cloud is internal-facing; confidential needs access control
