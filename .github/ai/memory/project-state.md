# Project State

## Current Status
- JTwine scraping: **Commented out** (disabled)
- VProp scraping: **Active**
- Twilio call reminders: **Active** (Selenium-based, cron-driven)
- Schedule page: **Active** (deployed to cloud.codifixsolutions.com)

## Recent Changes
- Migrated TwilioInterviewReminder from Playwright to Selenium
- Added tight date-time coupling using section `data-date` attributes
- Added dual-call logic (15min + 5min) for Himanshu/Sudhanshu/Amit
- Added same-person-same-time deduplication
- Unassigned dropdowns default to Himanshu
- Added logo to login screen
- Fixed VProp card fade-out bug (date parsed from card text)

## Active Integrations
- Twilio voice calls for interview reminders
- JTwine interview platform (currently disabled)
- VProp interview platform
- Google Sheets API
- Dropdown state API (PHP backend)
