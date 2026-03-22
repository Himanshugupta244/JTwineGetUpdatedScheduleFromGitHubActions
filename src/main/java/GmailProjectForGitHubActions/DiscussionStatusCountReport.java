package GmailProjectForGitHubActions;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

public class DiscussionStatusCountReport {

    // The 3 statuses we track
    private static final List<String> TRACKED_STATUSES = new ArrayList<>();
    static {
        TRACKED_STATUSES.add("Is a Good Fit");
        TRACKED_STATUSES.add("Not Recommended");
        TRACKED_STATUSES.add("Strongly Recommended");
    }

    public static void main(String[] args) {
        String himUser = System.getenv("JTWINE_USERNAME_HIM");
        String himPass = System.getenv("JTWINE_PASSWORD_HIM");
        String sudUser = System.getenv("JTWINE_USERNAME_SUD");
        String sudPass = System.getenv("JTWINE_PASSWORD_SUD");

        // Compute current and last month prefixes  e.g. "Mar " and "Feb "
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        java.time.LocalDate firstOfLastMonth = today.withDayOfMonth(1).minusMonths(1);
        String currentMonthPrefix = today.format(java.time.format.DateTimeFormatter.ofPattern("MMM "));
        String lastMonthPrefix    = firstOfLastMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMM "));
        String currentMonthLabel  = today.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase();
        String lastMonthLabel     = firstOfLastMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase();

        System.out.println("Current month prefix : " + currentMonthPrefix);
        System.out.println("Last month prefix    : " + lastMonthPrefix);

        // Counts: account -> { "current" -> { status -> count }, "last" -> { status -> count } }
        Map<String, Integer> himCurrentCounts = newCountMap();
        Map<String, Integer> himLastCounts    = newCountMap();
        Map<String, Integer> sudCurrentCounts = newCountMap();
        Map<String, Integer> sudLastCounts    = newCountMap();

        // ---------- Himanshu ----------
        System.out.println("======== Scraping HIM account ========");
        WebDriver himDriver = null;
        try {
            himDriver = loginToJTwine(himUser, himPass);
            waitTillElementVisible(himDriver, By.xpath(".//span[text()='Start Meeting']"), 60);
            scrapeAllPages(himDriver, currentMonthPrefix, lastMonthPrefix, himCurrentCounts, himLastCounts);
        } catch (Exception e) {
            System.err.println("Error scraping HIM: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (himDriver != null) himDriver.quit();
        }

        // ---------- Sudhanshu ----------
        System.out.println("======== Scraping SUD account ========");
        WebDriver sudDriver = null;
        try {
            sudDriver = loginToJTwine(sudUser, sudPass);
            waitTillElementVisible(sudDriver, By.xpath(".//span[text()='Start Meeting']"), 60);
            scrapeAllPages(sudDriver, currentMonthPrefix, lastMonthPrefix, sudCurrentCounts, sudLastCounts);
        } catch (Exception e) {
            System.err.println("Error scraping SUD: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (sudDriver != null) sudDriver.quit();
        }

        printCounts("HIM - Current Month", himCurrentCounts);
        printCounts("HIM - Last Month",    himLastCounts);
        printCounts("SUD - Current Month", sudCurrentCounts);
        printCounts("SUD - Last Month",    sudLastCounts);

        writeStatsHtmlFile(
            himCurrentCounts, himLastCounts,
            sudCurrentCounts, sudLastCounts,
            currentMonthLabel, lastMonthLabel
        );
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    private static WebDriver loginToJTwine(String username, String password) {
        System.out.println("Logging into JTwine as: " + username);
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        setTimezoneToIST(driver);
        driver.get("https://www.jobtwine.com/signin");
        waitForFixTime(2000);
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("JTwine username/password env vars not set.");
        }
        waitTillElementVisible(driver, By.xpath(".//input[@formcontrolname='userName']"), 30);
        waitForFixTime(1000);
        driver.findElement(By.xpath(".//input[@formcontrolname='userName']")).sendKeys(username);
        waitForFixTime(1000);
        waitTillElementVisible(driver, By.xpath(".//button[contains(text(),'Next')]"), 30);
        waitForFixTime(1000);
        driver.findElement(By.xpath(".//button[contains(text(),'Next')]")).click();
        waitForFixTime(1000);
        waitTillElementVisible(driver, By.xpath(".//input[@formcontrolname='password']"), 30);
        waitForFixTime(1000);
        driver.findElement(By.xpath(".//input[@formcontrolname='password']")).sendKeys(password);
        waitForFixTime(1000);
        waitTillElementVisible(driver, By.xpath(".//button[contains(text(),'Sign In')]"), 30);
        waitForFixTime(1000);
        driver.findElement(By.xpath(".//button[contains(text(),'Sign In')]")).click();
        waitTillElementVisible(driver, By.xpath(".//div[contains(text(),'Candidates For Inter')]"), 30);
        waitForFixTime(1000);
        if (driver.findElements(By.xpath(".//div[contains(text(),'Candidates For Inter')]")).size() > 0) {
            System.out.println("Login successful.");
        } else {
            throw new RuntimeException("Login failed - 'Candidates For Discussion' not found.");
        }
        return driver;
    }

    private static void setTimezoneToIST(WebDriver driver) {
        Map<String, Object> tz = new HashMap<>();
        tz.put("timezoneId", "Asia/Kolkata");
        ((ChromeDriver) driver).executeCdpCommand("Emulation.setTimezoneOverride", tz);
    }

    // -------------------------------------------------------------------------
    // Pagination scraping
    // -------------------------------------------------------------------------
    private static void scrapeAllPages(WebDriver driver,
                                       String currentMonthPrefix,
                                       String lastMonthPrefix,
                                       Map<String, Integer> currentCounts,
                                       Map<String, Integer> lastCounts) throws Exception {
        // Page 1 is already visible after login
        System.out.println("Scraping page 1...");
        boolean hasRelevantData = scrapePageCounts(driver, currentMonthPrefix, lastMonthPrefix, currentCounts, lastCounts);

        for (int page = 2; page <= 20; page++) {
            // Look for the Nth page button: the pattern used in the main file
            List<WebElement> pageBtn = driver.findElements(
                By.xpath("(.//span[contains(text(),'page ')]/following-sibling::span)[" + (page - 1) + "]"));
            if (pageBtn.isEmpty()) {
                System.out.println("No page " + page + " button found. Stopping pagination.");
                break;
            }
            System.out.println("Clicking page " + page + "...");
            pageBtn.get(0).click();
            waitForFixTime(10000);

            System.out.println("Scraping page " + page + "...");
            hasRelevantData = scrapePageCounts(driver, currentMonthPrefix, lastMonthPrefix, currentCounts, lastCounts);

            // Early exit: if this page had no dates from either target month, data is exhausted
            if (!hasRelevantData) {
                System.out.println("Page " + page + " has no data for target months. Stopping early.");
                break;
            }
        }
    }

    /**
     * Scrapes the currently visible page.
     * Returns true if the page contained at least one date entry from either target month.
     */
    private static boolean scrapePageCounts(WebDriver driver,
                                             String currentMonthPrefix,
                                             String lastMonthPrefix,
                                             Map<String, Integer> currentCounts,
                                             Map<String, Integer> lastCounts) {
        boolean foundAny = false;

        // Scrape current month
        List<WebElement> currentStatuses = driver.findElements(By.xpath(
            ".//div[@class='sub-sub-heading-1'][contains(text(),'" + currentMonthPrefix + "')]" +
            "//ancestor::div[contains(@class,'candidate-details-sec')]" +
            "//div[contains(@class,'btn-chip')]/div"));
        for (WebElement el : currentStatuses) {
            String status = el.getText().trim();
            if (currentCounts.containsKey(status)) {
                currentCounts.put(status, currentCounts.get(status) + 1);
            }
            foundAny = true;
        }

        // Scrape last month
        List<WebElement> lastStatuses = driver.findElements(By.xpath(
            ".//div[@class='sub-sub-heading-1'][contains(text(),'" + lastMonthPrefix + "')]" +
            "//ancestor::div[contains(@class,'candidate-details-sec')]" +
            "//div[contains(@class,'btn-chip')]/div"));
        for (WebElement el : lastStatuses) {
            String status = el.getText().trim();
            if (lastCounts.containsKey(status)) {
                lastCounts.put(status, lastCounts.get(status) + 1);
            }
            foundAny = true;
        }

        System.out.println("  Found " + currentStatuses.size() + " entries for current month, "
            + lastStatuses.size() + " for last month on this page.");
        return foundAny;
    }

    // -------------------------------------------------------------------------
    // HTML generation
    // -------------------------------------------------------------------------

    private static void writeStatsHtmlFile(
            Map<String, Integer> himCurrent,
            Map<String, Integer> himLast,
            Map<String, Integer> sudCurrent,
            Map<String, Integer> sudLast,
            String currentMonthLabel,
            String lastMonthLabel) {

        java.time.ZonedDateTime nowIST =
            java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String updatedAt = nowIST.format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a"));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>Discussion Status Report</title>\n");
        html.append("<style>\n");
        html.append("@import url('https://fonts.googleapis.com/css2?family=Barlow:wght@700;800;900&display=swap');\n");
        html.append("* { box-sizing: border-box; font-family: 'Barlow', 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; }\n");
        html.append("body { background: #f0f0f0; padding: 12px; color: #1a1a1a; }\n");
        html.append(".container { max-width: 480px; margin: auto; width: 100%; }\n");
        // Header
        html.append(".header { border: 3px solid #1a1a1a; background: #fff; text-align: center; padding: 16px 10px; margin-bottom: 18px; }\n");
        html.append(".header h1 { font-size: 26px; font-weight: 900; letter-spacing: 5px; text-transform: uppercase; }\n");
        // Section
        html.append(".section { margin-bottom: 18px; }\n");
        html.append(".tab-label { display: inline-block; padding: 8px 20px; font-size: 15px; font-weight: 900; border-bottom: none; margin-left: 0; letter-spacing: 2px; text-transform: uppercase; }\n");
        html.append(".tab-current { background: #15803d; color: #fff; }\n");
        html.append(".tab-last    { background: #1d4ed8; color: #fff; }\n");
        html.append(".section-box-current { border: 3px solid #15803d; background: #fff; }\n");
        html.append(".section-box-last    { border: 3px solid #1d4ed8; background: #fff; }\n");
        // Account label
        html.append(".acc-label { padding: 10px 14px; font-size: 14px; font-weight: 900; letter-spacing: 1.5px; text-transform: uppercase; border-bottom: 2px solid #e5e7eb; display: flex; align-items: center; gap: 7px; }\n");
        html.append(".acc-him { background: #ede9fe; color: #4c1d95; }\n");
        html.append(".acc-sud { background: #d1fae5; color: #064e3b; }\n");
        // Rows
        html.append(".row { display: flex; align-items: stretch; border-bottom: 1px solid #e5e7eb; }\n");
        html.append(".row:last-child { border-bottom: none; }\n");
        html.append(".col-status { flex: 1; padding: 12px 14px; font-weight: 800; font-size: 15px; color: #111827; border-right: 1px solid #e5e7eb; letter-spacing: 0.3px; line-height: 1.4; }\n");
        html.append(".col-count  { flex: 0 0 60px; padding: 12px 14px; font-weight: 900; font-size: 20px; text-align: center; letter-spacing: 0.5px; line-height: 1.4; }\n");
        // Count colors per status
        html.append(".cnt-gf { color: #15803d; }\n");
        html.append(".cnt-sr { color: #1d4ed8; }\n");
        html.append(".cnt-nr { color: #b91c1c; }\n");
        // Footer
        html.append(".footer { border: 3px solid #1a1a1a; padding: 12px; font-size: 13px; font-weight: 800; letter-spacing: 1px; background: #fff; text-align: center; margin-top: 4px; }\n");
        html.append("@media (max-width: 480px) {\n");
        html.append("  body { padding: 10px; }\n");
        html.append("  .header h1 { font-size: 22px; letter-spacing: 4px; }\n");
        html.append("  .tab-label { font-size: 14px; padding: 8px 16px; letter-spacing: 1.5px; }\n");
        html.append("  .acc-label { font-size: 13px; padding: 9px 12px; }\n");
        html.append("  .col-status { font-size: 14px; padding: 11px 12px; }\n");
        html.append("  .col-count  { font-size: 18px; padding: 11px 10px; }\n");
        html.append("  .footer { font-size: 12px; }\n");
        html.append("}\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<div class=\"container\">\n");
        html.append("<div class=\"header\"><h1>&#128202; DISCUSSION STATS</h1></div>\n");

        // --- CURRENT MONTH (green) ---
        html.append("<div class=\"section\">\n");
        html.append("<div class=\"tab-label tab-current\">&#128197; ").append(currentMonthLabel).append("</div>\n");
        html.append("<div class=\"section-box-current\">\n");
        html.append("<div class=\"acc-label acc-him\">&#128100; HIMANSHU &mdash; JTwine</div>\n");
        appendStatusRows(html, himCurrent);
        html.append("<div class=\"acc-label acc-sud\">&#128101; SUDHANSHU &mdash; JTwine</div>\n");
        appendStatusRows(html, sudCurrent);
        html.append("</div>\n</div>\n");

        // --- LAST MONTH (blue) ---
        html.append("<div class=\"section\">\n");
        html.append("<div class=\"tab-label tab-last\">&#128197; ").append(lastMonthLabel).append("</div>\n");
        html.append("<div class=\"section-box-last\">\n");
        html.append("<div class=\"acc-label acc-him\">&#128100; HIMANSHU &mdash; JTwine</div>\n");
        appendStatusRows(html, himLast);
        html.append("<div class=\"acc-label acc-sud\">&#128101; SUDHANSHU &mdash; JTwine</div>\n");
        appendStatusRows(html, sudLast);
        html.append("</div>\n</div>\n");

        // --- Footer ---
        html.append("<div class=\"footer\">&#9201; Updated at (IST): ").append(updatedAt).append("</div>\n");
        html.append("</div>\n</body>\n</html>");

        try {
            Files.write(Paths.get("deploy/discussion-stats.html"),
                html.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("discussion-stats.html generated successfully.");
        } catch (java.io.IOException ioe) {
            System.err.println("Failed to write discussion-stats.html: " + ioe.getMessage());
        }
    }

    private static void appendStatusRows(StringBuilder html, Map<String, Integer> counts) {
        for (String status : TRACKED_STATUSES) {
            int count = counts.getOrDefault(status, 0);
            String countClass;
            switch (status) {
                case "Is a Good Fit":        countClass = "cnt-gf"; break;
                case "Strongly Recommended": countClass = "cnt-sr"; break;
                default:                     countClass = "cnt-nr"; break;
            }
            html.append("<div class=\"row\">")
                .append("<div class=\"col-status\">").append(status).append("</div>")
                .append("<div class=\"col-count ").append(countClass).append("\">").append(count).append("</div>")
                .append("</div>\n");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Map<String, Integer> newCountMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String s : TRACKED_STATUSES) map.put(s, 0);
        return map;
    }

    private static void printCounts(String label, Map<String, Integer> counts) {
        System.out.println("---- " + label + " ----");
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            System.out.println("  " + e.getKey() + ": " + e.getValue());
        }
    }

    private static void waitForFixTime(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private static void waitTillElementVisible(WebDriver driver, By locator, int timeoutSeconds) {
        new org.openqa.selenium.support.ui.WebDriverWait(driver,
            java.time.Duration.ofSeconds(timeoutSeconds))
            .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(locator));
    }
}
