# Security Rules

## Authentication
- SHA-256 hashed credentials (no plaintext password comparison)
- Session expiry: 1 hour (`3600000ms`)
- `sessionStorage` for auth state (clears on tab close)

## API Security
- Twilio: Basic auth with Account SID + Auth Token
- CORS: Configured via PHP headers
- Input sanitization: HTML escaping via `escapeHtml()`, XML escaping via `escapeXml()`

## Secrets Management
- Environment variables for credentials (`JTWINE_USERNAME_HIM`, etc.)
- No hardcoded passwords in source (Twilio creds are an exception — consider moving to env vars)

## Best Practices
- URL-encode all HTTP POST parameters
- Use `HttpURLConnection` timeouts (connect + read)
- Validate all external input before processing
