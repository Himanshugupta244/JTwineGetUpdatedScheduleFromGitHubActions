# Frontend Mode

## Focus
Static HTML/CSS/JS for the schedule dashboard.

## Conventions
- Single-file HTML with inline CSS and JS
- CSS framework: Custom (Inter font, utility-first approach)
- No build tools — vanilla JS only
- Mobile-responsive with `@media (max-width:768px)`

## UI Components
- Login overlay with animated starfield background
- Collapsible sections (today/tomorrow)
- Interview cards with dropdowns, JOIN buttons, candidate popups
- Past-interview fading via `markPastInterviews()`
- PNG download via html2canvas

## API Integration
- Dropdown state persistence via `dropdown-api.php`
- Dropdown options loaded from `dropdown-options.php`
- Domain-prefix based keys (`cloud_`, `conf_`, `local_`)
