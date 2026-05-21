# API Design

## Patterns
- RESTful endpoints via PHP on the server side
- Java `HttpURLConnection` for outbound REST calls
- Basic auth for Twilio API (`ACCOUNT_SID:AUTH_TOKEN`)
- JSON request/response format
- CORS headers via PHP (`Access-Control-Allow-Origin: *`)

## Dropdown API
- `GET dropdown-api.php` — Retrieve all saved dropdown values
- `POST dropdown-api.php` — Save a dropdown value (`{key, value}`)
- Keys are domain-prefixed: `cloud_`, `conf_`, `local_`
