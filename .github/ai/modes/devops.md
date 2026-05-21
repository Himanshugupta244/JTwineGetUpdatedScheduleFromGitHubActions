# DevOps Mode

## Focus
CI/CD, build configuration, and deployment.

## Conventions
- GitHub Actions for CI/CD
- Maven for build and dependency management
- Fat JAR packaging via maven-shade-plugin
- Ubuntu runners for GitHub Actions
- Chrome/Chromium headless for Selenium on CI

## Deployment
- Static files deployed to `deploy/` directory
- Two domains: `cloud.codifixsolutions.com` (no auth) and `confidential.codifixsolutions.com` (auth required)
- Cron-based execution for scheduled tasks
