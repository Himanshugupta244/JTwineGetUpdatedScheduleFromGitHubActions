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

public class DiscussionCount {

    public static void main(String[] args) {
        String himUser = System.getenv("JTWINE_USERNAME_HIM");
        String himPass = System.getenv("JTWINE_PASSWORD_HIM");
        String sudUser = System.getenv("JTWINE_USERNAME_SUD");
        String sudPass = System.getenv("JTWINE_PASSWORD_SUD");

        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        java.time.format.DateTimeFormatter fmtMMM_d = java.time.format.DateTimeFormatter.ofPattern("MMM d','");
        java.time.format.DateTimeFormatter fmtDisplay = java.time.format.DateTimeFormatter.ofPattern("d MMM '['EEE']'");

        // Current month: 1st to today (most recent first)
        int currentMonthDays = today.getDayOfMonth();
        String currentMonthLabel = today.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase();
        List<String> currentPrefixes = new ArrayList<>();
        List<String> currentDisplayLabels = new ArrayList<>();
        for (int i = 0; i < currentMonthDays; i++) {
            java.time.LocalDate d = today.minusDays(i);
            currentPrefixes.add(d.format(fmtMMM_d));
            currentDisplayLabels.add(d.format(fmtDisplay));
        }

        // Last month: 1st to last day (most recent first)
        java.time.LocalDate lastMonthEnd = today.withDayOfMonth(1).minusDays(1);
        java.time.LocalDate lastMonthStart = lastMonthEnd.withDayOfMonth(1);
        int lastMonthDays = lastMonthEnd.getDayOfMonth();
        String lastMonthLabel = lastMonthEnd.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase();
        List<String> lastPrefixes = new ArrayList<>();
        List<String> lastDisplayLabels = new ArrayList<>();
        for (int i = 0; i < lastMonthDays; i++) {
            java.time.LocalDate d = lastMonthEnd.minusDays(i);
            lastPrefixes.add(d.format(fmtMMM_d));
            lastDisplayLabels.add(d.format(fmtDisplay));
        }

        System.out.println("Current month: " + currentMonthLabel + " (" + currentMonthDays + " days)");
        System.out.println("Last month: " + lastMonthLabel + " (" + lastMonthDays + " days)");

        // Counts per month per user: dateIndex -> count
        Map<Integer, Integer> himCurrentCounts = initCounts(currentMonthDays);
        Map<Integer, Integer> himCurrentSuccess = initCounts(currentMonthDays);
        Map<Integer, Integer> sudCurrentCounts = initCounts(currentMonthDays);
        Map<Integer, Integer> sudCurrentSuccess = initCounts(currentMonthDays);
        Map<Integer, Integer> himLastCounts = initCounts(lastMonthDays);
        Map<Integer, Integer> himLastSuccess = initCounts(lastMonthDays);
        Map<Integer, Integer> sudLastCounts = initCounts(lastMonthDays);
        Map<Integer, Integer> sudLastSuccess = initCounts(lastMonthDays);

        // Combine all prefixes for scraping (current + last month)
        List<String> allPrefixes = new ArrayList<>();
        allPrefixes.addAll(currentPrefixes);
        allPrefixes.addAll(lastPrefixes);

        // ---------- Himanshu ----------
        System.out.println("======== Scraping HIM account ========");
        WebDriver himDriver = null;
        try {
            himDriver = loginToJTwine(himUser, himPass);
            waitTillElementVisible(himDriver, By.xpath(".//span[text()='Start Meeting']"), 60);
            scrapeAllPages(himDriver, currentPrefixes, himCurrentCounts, himCurrentSuccess,
                           lastPrefixes, himLastCounts, himLastSuccess);
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
            scrapeAllPages(sudDriver, currentPrefixes, sudCurrentCounts, sudCurrentSuccess,
                           lastPrefixes, sudLastCounts, sudLastSuccess);
        } catch (Exception e) {
            System.err.println("Error scraping SUD: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (sudDriver != null) sudDriver.quit();
        }

        // Print summary
        int[] himCurrentTotals = sumCounts(himCurrentCounts, himCurrentSuccess, currentMonthDays);
        int[] sudCurrentTotals = sumCounts(sudCurrentCounts, sudCurrentSuccess, currentMonthDays);
        int[] himLastTotals = sumCounts(himLastCounts, himLastSuccess, lastMonthDays);
        int[] sudLastTotals = sumCounts(sudLastCounts, sudLastSuccess, lastMonthDays);

        printSummary("HIMANSHU - " + currentMonthLabel, currentDisplayLabels, himCurrentCounts, himCurrentSuccess, himCurrentTotals);
        printSummary("SUDHANSHU - " + currentMonthLabel, currentDisplayLabels, sudCurrentCounts, sudCurrentSuccess, sudCurrentTotals);
        printSummary("HIMANSHU - " + lastMonthLabel, lastDisplayLabels, himLastCounts, himLastSuccess, himLastTotals);
        printSummary("SUDHANSHU - " + lastMonthLabel, lastDisplayLabels, sudLastCounts, sudLastSuccess, sudLastTotals);

        writeHtmlReport(
            himCurrentCounts, himCurrentSuccess, sudCurrentCounts, sudCurrentSuccess, currentDisplayLabels,
            himCurrentTotals, sudCurrentTotals, currentMonthLabel,
            himLastCounts, himLastSuccess, sudLastCounts, sudLastSuccess, lastDisplayLabels,
            himLastTotals, sudLastTotals, lastMonthLabel
        );
    }

    private static Map<Integer, Integer> initCounts(int days) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) map.put(i, 0);
        return map;
    }

    private static int[] sumCounts(Map<Integer, Integer> counts, Map<Integer, Integer> success, int days) {
        int total = 0, successTotal = 0;
        for (int i = 0; i < days; i++) {
            total += counts.getOrDefault(i, 0);
            successTotal += success.getOrDefault(i, 0);
        }
        return new int[]{total, successTotal};
    }

    private static void printSummary(String label, List<String> displayLabels,
                                      Map<Integer, Integer> counts, Map<Integer, Integer> success, int[] totals) {
        System.out.println("\n---- " + label + " ----");
        for (int i = 0; i < displayLabels.size(); i++) {
            System.out.println("  " + displayLabels.get(i) + " : " + counts.get(i) + " (successful: " + success.get(i) + ")");
        }
        System.out.println("  TOTAL: " + totals[0] + " | SUCCESSFUL: " + totals[1]);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    private static WebDriver loginToJTwine(String username, String password) {
        System.out.println("Logging into JTwine as: " + username);
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
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
            throw new RuntimeException("Login failed - 'Candidates For Interview' not found.");
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

    private static boolean isSuccessfulStatus(String status) {
        return "Not Recommended".equals(status)
            || "Is a Good Fit".equals(status)
            || "Strongly Recommended".equals(status);
    }

    private static void scrapeAllPages(WebDriver driver,
                                       List<String> currentPrefixes,
                                       Map<Integer, Integer> currentCounts,
                                       Map<Integer, Integer> currentSuccess,
                                       List<String> lastPrefixes,
                                       Map<Integer, Integer> lastCounts,
                                       Map<Integer, Integer> lastSuccess) throws Exception {
        System.out.println("Scraping page 1...");
        boolean hasRelevantData = scrapePageCounts(driver, currentPrefixes, currentCounts, currentSuccess,
                                                   lastPrefixes, lastCounts, lastSuccess);

        for (int page = 2; page <= 20; page++) {
            List<WebElement> nextBtn = driver.findElements(
                By.xpath(".//a[@aria-label='Next page']"));
            if (nextBtn.isEmpty() || !nextBtn.get(0).isEnabled()) {
                System.out.println("No next page button found. Stopping pagination.");
                break;
            }
            System.out.println("Clicking next page (page " + page + ")...");
            nextBtn.get(0).click();
            waitForFixTime(10000);

            System.out.println("Scraping page " + page + "...");
            hasRelevantData = scrapePageCounts(driver, currentPrefixes, currentCounts, currentSuccess,
                                               lastPrefixes, lastCounts, lastSuccess);

            if (!hasRelevantData) {
                System.out.println("Page " + page + " has no data for target dates. Stopping early.");
                break;
            }
        }
    }

    private static boolean scrapePageCounts(WebDriver driver,
                                             List<String> currentPrefixes,
                                             Map<Integer, Integer> currentCounts,
                                             Map<Integer, Integer> currentSuccess,
                                             List<String> lastPrefixes,
                                             Map<Integer, Integer> lastCounts,
                                             Map<Integer, Integer> lastSuccess) {
        boolean foundAny = false;
        foundAny |= scrapeForPrefixes(driver, currentPrefixes, currentCounts, currentSuccess);
        foundAny |= scrapeForPrefixes(driver, lastPrefixes, lastCounts, lastSuccess);
        return foundAny;
    }

    private static boolean scrapeForPrefixes(WebDriver driver,
                                              List<String> datePrefixes,
                                              Map<Integer, Integer> counts,
                                              Map<Integer, Integer> successfulCounts) {
        boolean foundAny = false;

        for (int i = 0; i < datePrefixes.size(); i++) {
            String prefix = datePrefixes.get(i);
            String cardXpath = ".//div[@class='sub-sub-heading-1'][contains(text(),'" + prefix + "')]" +
                "//ancestor::div[contains(@class,'candidate-details-sec')]";
            List<WebElement> cards = driver.findElements(By.xpath(cardXpath));
            if (!cards.isEmpty()) {
                counts.put(i, counts.get(i) + cards.size());
                foundAny = true;
                int successOnPage = 0;
                for (WebElement card : cards) {
                    try {
                        List<WebElement> statusDivs = card.findElements(By.xpath(".//div[contains(@class,'btn-chip')]/div"));
                        if (!statusDivs.isEmpty()) {
                            String status = statusDivs.get(0).getText().trim();
                            if (isSuccessfulStatus(status)) {
                                successOnPage++;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("  Could not read status for a card: " + e.getMessage());
                    }
                }
                successfulCounts.put(i, successfulCounts.get(i) + successOnPage);
                System.out.println("  " + prefix + " => " + cards.size() + " interviews (" + successOnPage + " successful) on this page");
            }
        }

        return foundAny;
    }

    // -------------------------------------------------------------------------
    // HTML report
    // -------------------------------------------------------------------------

    private static void writeHtmlReport(
            Map<Integer, Integer> himCurrentCounts, Map<Integer, Integer> himCurrentSuccess,
            Map<Integer, Integer> sudCurrentCounts, Map<Integer, Integer> sudCurrentSuccess,
            List<String> currentDisplayLabels,
            int[] himCurrentTotals, int[] sudCurrentTotals, String currentMonthLabel,
            Map<Integer, Integer> himLastCounts, Map<Integer, Integer> himLastSuccess,
            Map<Integer, Integer> sudLastCounts, Map<Integer, Integer> sudLastSuccess,
            List<String> lastDisplayLabels,
            int[] himLastTotals, int[] sudLastTotals, String lastMonthLabel) {

        java.time.ZonedDateTime nowIST =
            java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        String updatedAt = nowIST.format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a"));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>Interview Count</title>\n");
        html.append("<style>\n");
        html.append("@import url('https://fonts.googleapis.com/css2?family=Barlow:wght@700;800;900&display=swap');\n");
        html.append("* { box-sizing: border-box; font-family: 'Barlow', 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; }\n");
        html.append("body { background: #f0f0f0; padding: 12px; color: #1a1a1a; }\n");
        html.append(".container { max-width: 480px; margin: auto; width: 100%; }\n");
        html.append(".header { border: 3px solid #1a1a1a; background: #fff; text-align: center; padding: 16px 10px; margin-bottom: 18px; }\n");
        html.append(".header h1 { font-size: 22px; font-weight: 900; letter-spacing: 4px; text-transform: uppercase; }\n");
        html.append(".header p { font-size: 13px; font-weight: 700; color: #6b7280; margin-top: 4px; letter-spacing: 1px; }\n");
        html.append(".section { margin-bottom: 18px; }\n");
        html.append(".acc-label { padding: 10px 14px; font-size: 14px; font-weight: 900; letter-spacing: 1.5px; text-transform: uppercase; border-bottom: 2px solid #e5e7eb; display: flex; align-items: center; justify-content: space-between; gap: 7px; }\n");
        html.append(".acc-him { background: #ede9fe; color: #4c1d95; border: 3px solid #7c3aed; border-bottom: 2px solid #e5e7eb; }\n");
        html.append(".acc-sud { background: #d1fae5; color: #064e3b; border: 3px solid #059669; border-bottom: 2px solid #e5e7eb; }\n");
        html.append(".box-him { border: 3px solid #7c3aed; border-top: none; background: #fff; }\n");
        html.append(".box-sud { border: 3px solid #059669; border-top: none; background: #fff; }\n");
        html.append(".row { display: flex; align-items: stretch; border-bottom: 1px solid #e5e7eb; }\n");
        html.append(".row:last-child { border-bottom: none; }\n");
        html.append(".col-date { flex: 1; padding: 10px 14px; font-weight: 800; font-size: 14px; color: #111827; border-right: 1px solid #e5e7eb; letter-spacing: 0.3px; line-height: 1.4; }\n");
        html.append(".col-count { flex: 0 0 60px; padding: 10px 14px; font-weight: 900; font-size: 20px; text-align: center; line-height: 1.4; }\n");
        html.append(".cnt-zero { color: #d1d5db; }\n");
        html.append(".cnt-pos { color: #15803d; }\n");
        html.append(".row-today { background: #fef9c3; }\n");
        html.append(".row-total { border-top: 3px solid #1a1a1a; background: #f9fafb; }\n");
        html.append(".row-total .col-date { font-weight: 900; font-size: 14px; letter-spacing: 1px; text-transform: uppercase; color: #1a1a1a; }\n");
        html.append(".row-total .col-count { font-weight: 900; font-size: 22px; color: #1a1a1a; }\n");
        html.append(".row-success { background: #ecfdf5; border-top: 2px solid #059669; }\n");
        html.append(".row-success .col-date { font-weight: 900; font-size: 14px; letter-spacing: 1px; text-transform: uppercase; color: #15803d; }\n");
        html.append(".row-success .col-count { font-weight: 900; font-size: 22px; color: #15803d; }\n");
        html.append(".tab-label { display: inline-block; padding: 8px 20px; font-size: 15px; font-weight: 900; border-bottom: none; margin-left: 0; letter-spacing: 2px; text-transform: uppercase; }\n");
        html.append(".tab-current { background: #15803d; color: #fff; }\n");
        html.append(".tab-last { background: #1d4ed8; color: #fff; }\n");
        html.append(".section-box-current { border: 3px solid #15803d; background: #fff; }\n");
        html.append(".section-box-last { border: 3px solid #1d4ed8; background: #fff; }\n");
        html.append(".collapsible-body { display: none; }\n");
        html.append(".collapsible-body.open { display: block; }\n");
        html.append(".tab-label.clickable { cursor: pointer; display: flex; justify-content: space-between; align-items: center; user-select: none; }\n");
        html.append(".toggle-btn { display: inline-flex; align-items: center; gap: 5px; padding: 4px 14px; font-size: 11px; font-weight: 900; letter-spacing: 1px; color: #1d4ed8; background: #fff; border: 2px solid #fff; border-radius: 20px; white-space: nowrap; transition: background 0.2s, transform 0.15s; box-shadow: 0 1px 3px rgba(0,0,0,0.15); }\n");
        html.append(".toggle-btn:hover { background: #dbeafe; transform: scale(1.05); }\n");
        html.append(".toggle-btn .arrow { font-size: 13px; line-height: 1; }\n");
        html.append(".footer { border: 3px solid #1a1a1a; padding: 12px; font-size: 13px; font-weight: 800; letter-spacing: 1px; background: #fff; text-align: center; margin-top: 4px; }\n");
        html.append("@media (max-width: 480px) {\n");
        html.append("  body { padding: 10px; }\n");
        html.append("  .header h1 { font-size: 18px; letter-spacing: 3px; }\n");
        html.append("  .acc-label { font-size: 13px; padding: 9px 12px; }\n");
        html.append("  .col-date { font-size: 13px; padding: 9px 12px; }\n");
        html.append("  .col-count { font-size: 18px; padding: 9px 10px; }\n");
        html.append("  .footer { font-size: 12px; }\n");
        html.append("}\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<div class=\"container\">\n");
        html.append("<div class=\"header\"><h1>&#128202; INTERVIEW COUNT</h1></div>\n");

        // --- CURRENT MONTH (green) ---
        html.append("<div class=\"section\">\n");
        html.append("<div class=\"tab-label tab-current\">&#9728;&#65039; ").append(currentMonthLabel).append("</div>\n");
        html.append("<div class=\"section-box-current\">\n");
        html.append("<div class=\"acc-label acc-him\"><span>&#128100; HIMANSHU</span></div>\n");
        html.append("<div class=\"box-him\">\n");
        appendDateRows(html, himCurrentCounts, currentDisplayLabels, true);
        appendSummaryRows(html, himCurrentTotals[1], himCurrentTotals[0]);
        html.append("</div>\n");
        html.append("<div class=\"acc-label acc-sud\"><span>&#128101; SUDHANSHU</span></div>\n");
        html.append("<div class=\"box-sud\">\n");
        appendDateRows(html, sudCurrentCounts, currentDisplayLabels, true);
        appendSummaryRows(html, sudCurrentTotals[1], sudCurrentTotals[0]);
        html.append("</div>\n");
        html.append("</div>\n</div>\n");

        // --- LAST MONTH (blue, collapsible) ---
        html.append("<div class=\"section\">\n");
        html.append("<div class=\"tab-label tab-last clickable\" onclick=\"toggleSection('last-month-body')\">\n");
        html.append("  <span>&#127769; ").append(lastMonthLabel).append("</span>\n");
        html.append("  <span class=\"toggle-btn\" id=\"last-month-body-icon\"><span class=\"arrow\">&#9660;</span> TAP TO EXPAND</span>\n");
        html.append("</div>\n");
        html.append("<div id=\"last-month-body\" class=\"collapsible-body\">\n");
        html.append("<div class=\"section-box-last\">\n");
        html.append("<div class=\"acc-label acc-him\"><span>&#128100; HIMANSHU</span></div>\n");
        html.append("<div class=\"box-him\">\n");
        appendDateRows(html, himLastCounts, lastDisplayLabels, false);
        appendSummaryRows(html, himLastTotals[1], himLastTotals[0]);
        html.append("</div>\n");
        html.append("<div class=\"acc-label acc-sud\"><span>&#128101; SUDHANSHU</span></div>\n");
        html.append("<div class=\"box-sud\">\n");
        appendDateRows(html, sudLastCounts, lastDisplayLabels, false);
        appendSummaryRows(html, sudLastTotals[1], sudLastTotals[0]);
        html.append("</div>\n");
        html.append("</div>\n"); // section-box-last
        html.append("</div>\n"); // collapsible-body
        html.append("</div>\n"); // section

        // --- Footer ---
        html.append("<div class=\"footer\">&#9201; Updated at (IST): ").append(updatedAt).append("</div>\n");
        html.append("<script>\n");
        html.append("function toggleSection(id) {\n");
        html.append("  var body = document.getElementById(id);\n");
        html.append("  var icon = document.getElementById(id + '-icon');\n");
        html.append("  if (body.classList.contains('open')) {\n");
        html.append("    body.classList.remove('open');\n");
        html.append("    icon.innerHTML = \"<span class='arrow'>&#9660;</span> TAP TO EXPAND\";\n");
        html.append("  } else {\n");
        html.append("    body.classList.add('open');\n");
        html.append("    icon.innerHTML = \"<span class='arrow'>&#9650;</span> TAP TO COLLAPSE\";\n");
        html.append("  }\n");
        html.append("}\n");
        html.append("</script>\n");
        html.append("</div>\n</body>\n</html>");

        try {
            Files.write(Paths.get("deploy/discussion-count.html"),
                html.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("discussion-count.html generated successfully.");
        } catch (java.io.IOException ioe) {
            System.err.println("Failed to write discussion-count.html: " + ioe.getMessage());
        }
    }

    private static void appendDateRows(StringBuilder html,
                                        Map<Integer, Integer> counts,
                                        List<String> dateDisplayLabels,
                                        boolean highlightFirst) {
        for (int i = 0; i < dateDisplayLabels.size(); i++) {
            int count = counts.getOrDefault(i, 0);
            String rowClass = (highlightFirst && i == 0) ? "row row-today" : "row";
            String countClass = (count == 0) ? "cnt-zero" : "cnt-pos";
            html.append("<div class=\"").append(rowClass).append("\">");
            html.append("<div class=\"col-date\">").append(escapeHtml(dateDisplayLabels.get(i)));
            if (highlightFirst && i == 0) html.append(" &#128994;");
            html.append("</div>");
            html.append("<div class=\"col-count ").append(countClass).append("\">").append(count).append("</div>");
            html.append("</div>\n");
        }
    }

    private static void appendSummaryRows(StringBuilder html, int successTotal, int total) {
        html.append("<div class=\"row row-success\">")
            .append("<div class=\"col-date\">&#9989; Successful</div>")
            .append("<div class=\"col-count\">").append(successTotal).append("</div>")
            .append("</div>\n");
        html.append("<div class=\"row row-total\">")
            .append("<div class=\"col-date\">&#128202; Total</div>")
            .append("<div class=\"col-count\">").append(total).append("</div>")
            .append("</div>\n");
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void waitForFixTime(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private static void waitTillElementVisible(WebDriver driver, By locator, int timeoutSeconds) {
        new org.openqa.selenium.support.ui.WebDriverWait(driver,
            java.time.Duration.ofSeconds(timeoutSeconds))
            .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(locator));
    }
}
