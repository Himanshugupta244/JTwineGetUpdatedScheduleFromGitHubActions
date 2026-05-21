# Router

Decides the active mode based on the file being edited or task type.

## Rules
| Pattern | Mode |
|---|---|
| `src/**/*.java` | backend |
| `deploy/**/*.html`, `deploy/**/*.js` | frontend |
| `*.sql`, `**/migrations/**` | database |
| `.github/workflows/**`, `Dockerfile`, `pom.xml` | devops |

## Fallback
If no pattern matches, use **backend** mode.
