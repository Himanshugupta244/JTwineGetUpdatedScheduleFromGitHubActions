# Code Style Rules

## Java
- Java 8 compatible (no var, no streams with complex lambdas)
- Static methods for utility classes
- `LinkedHashMap` for insertion-ordered maps
- Explicit `TimeZone` on all date operations
- Log with timestamp: `[yyyy-MM-dd HH:mm:ss] message`
- XML escaping for Twilio TwiML content
- URL encoding for HTTP POST parameters

## HTML/CSS/JS
- Inline styles and scripts (single-file HTML)
- CSS class naming: lowercase with hyphens (e.g., `card-dd`, `section-vprop`)
- JS: vanilla ES5-compatible (no arrow functions, no let/const in generated HTML)
- Event handlers via `onclick` attributes
- Mobile-first responsive design
