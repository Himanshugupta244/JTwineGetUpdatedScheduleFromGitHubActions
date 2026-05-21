package GmailProjectForGitHubActions;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

/**
 * Reads dropdown assignments from the API, parses interview times,
 * and calls the assigned person via Twilio 5 minutes before each interview.
 *
 * Run continuously — checks every 60 seconds.
 */
public class TwilioInterviewReminder {

    // --- Twilio config (from environment variables) ---
    private static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN  = System.getenv("TWILIO_AUTH_TOKEN");
    private static final String FROM_NUMBER = System.getenv("TWILIO_FROM_NUMBER");

    // --- Dropdown API ---
    private static final String DD_API_URL = "https://cloud.codifixsolutions.com/dropdown-api.php";

    // --- Phone mapping (name → phone with country code) ---
    private static final Map<String, String> PHONE_MAP = new LinkedHashMap<>();
    static {
        PHONE_MAP.put("Dhruv",     "+918383075665");
        PHONE_MAP.put("Himanshu",  "+917838060230");
        PHONE_MAP.put("Pallvit",   "+917082182806");
        PHONE_MAP.put("Sudhanshu", "+919582192526");
        PHONE_MAP.put("Amit",      "+919958231821");
        PHONE_MAP.put("Vansh",     "+919719666500");
        PHONE_MAP.put("Abhinav",   "+918448630830");
        PHONE_MAP.put("Dhananjay", "+919136501578");
    }

    // File to track calls made today (persists across cron runs)
    private static final String CALLED_FILE = "twilio-called.txt";

    // Maps data-key → section date (from data-date attribute)
    private static final Map<String, String> keyDateMap = new LinkedHashMap<>();

    public static void main(String[] args) {
        log("=== Twilio Interview Reminder Started ===");
        try {
            checkAndCall();
        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
        }
        log("=== Twilio Interview Reminder Finished ===");
    }

    private static void checkAndCall() throws Exception {
        Map<String, String> assignments = fetchFromPage();
        if (assignments.isEmpty()) {
            log("No assigned interviews found on the live page.");
            return;
        }
        log("Assignments (" + assignments.size() + "): " + assignments);

        // Current time in IST
        TimeZone ist = TimeZone.getTimeZone("Asia/Kolkata");
        Calendar now = Calendar.getInstance(ist);

        // Track calls made this run to avoid calling same person twice for same date+time
        Set<String> calledThisRun = new HashSet<>();

        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            String key = entry.getKey();       // e.g. "him_8:00_PM_IST_DEVENDER_SINGH"
            String assignee = entry.getValue(); // e.g. "Pallvit"

            // Parse date+time using section date + key time
            Date interviewTime = parseDateTime(key, ist);
            if (interviewTime == null) continue;

            // Calculate minutes until interview
            long diffMs = interviewTime.getTime() - now.getTimeInMillis();
            long diffMin = diffMs / 60_000;

            String sectionDate = keyDateMap.get(key);
            String timeStr = extractTimeStr(key);

            // Dedup key: same person + same date + same time + same window = one call
            // e.g. "Pallvit_2026-05-15_2:00_PM_IST_5m"

            // For Select/Himanshu/Sudhanshu/Amit: call at 15 min AND 5 min before
            // For others: call at 5 min before only
            boolean needsDoubleCall = assignee.equals("Himanshu") || assignee.equals("Sudhanshu")
                    || assignee.equals("Amit");

            if (needsDoubleCall) {
                // 15-minute call (window: 14-15 min before)
                String personTimeKey15 = assignee + "_" + sectionDate + "_" + timeStr + "_15m";
                String callKey15 = sectionDate + "_15m_" + key;
                if (diffMin >= 14 && diffMin <= 15 && !calledThisRun.contains(personTimeKey15) && !isAlreadyCalled(callKey15)) {
                    callAssignee(key, assignee, "15", callKey15);
                    calledThisRun.add(personTimeKey15);
                }
                // 5-minute call (window: 4-5 min before)
                String personTimeKey5 = assignee + "_" + sectionDate + "_" + timeStr + "_5m";
                String callKey5 = sectionDate + "_5m_" + key;
                if (diffMin >= 4 && diffMin <= 5 && !calledThisRun.contains(personTimeKey5) && !isAlreadyCalled(callKey5)) {
                    callAssignee(key, assignee, "5", callKey5);
                    calledThisRun.add(personTimeKey5);
                }
            } else {
                // Normal: 5-minute call only (window: 4-5 min before)
                String personTimeKey5 = assignee + "_" + sectionDate + "_" + timeStr + "_5m";
                String callKey5 = sectionDate + "_5m_" + key;
                if (diffMin >= 4 && diffMin <= 5 && !calledThisRun.contains(personTimeKey5) && !isAlreadyCalled(callKey5)) {
                    callAssignee(key, assignee, "5", callKey5);
                    calledThisRun.add(personTimeKey5);
                }
            }
        }
    }

    private static void callAssignee(String key, String assignee, String minsBefore, String callKey) throws Exception {
        String candidateName = extractCandidateName(key);
        String timeStr = extractTimeStr(key);
        String phone = PHONE_MAP.get(assignee);

        if (phone == null) {
            log("No phone number mapped for: " + assignee + " (key: " + key + ")");
            return;
        }

        String message = "You have an interview in " + minsBefore + " minutes with " + candidateName + " at " + timeStr + ".";
        log("CALLING " + assignee + " at " + phone + " — " + message);
        makeCall(phone, message);
        markCalled(callKey);
        log("Call initiated successfully for key: " + key);
    }

    /**
     * Use Selenium headless Chrome to load the live page,
     * wait for dropdowns to populate, and extract data-key + selected value.
     * Each dropdown's date comes from its parent section's data-date attribute.
     */
    private static Map<String, String> fetchFromPage() {
        Map<String, String> result = new LinkedHashMap<>();
        keyDateMap.clear();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        ChromeDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.get("https://cloud.codifixsolutions.com/");

            // Explicit wait: wait until at least one select.row-dd is present
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("select.row-dd")));

            // Additional wait for dropdown values to be loaded via JS
            Thread.sleep(3000);

            // Get all dropdowns on the page
            List<WebElement> selects = driver.findElements(By.cssSelector("select.row-dd"));
            log("Found " + selects.size() + " total dropdown(s) on page.");

            for (WebElement sel : selects) {
                String dataKey = sel.getAttribute("data-key");
                String value = sel.getAttribute("value");

                // Get date from the parent section's data-date attribute
                String sectionDate = (String) ((JavascriptExecutor) driver).executeScript(
                        "return arguments[0].closest('.section[data-date]')?.getAttribute('data-date')", sel);

                if (dataKey != null && sectionDate != null) {
                    keyDateMap.put(dataKey, sectionDate);
                    // If no one is assigned (value is "Select"/empty), default to Himanshu
                    if (value == null || value.isEmpty()) {
                        value = "Himanshu";
                    }
                    result.put(dataKey, value);
                    log("  [" + sectionDate + "] " + dataKey + " => " + value);
                }
            }
        } catch (Exception e) {
            log("Selenium error: " + e.getMessage());
        } finally {
            if (driver != null) driver.close();
        }
        return result;
    }

    /**
     * Parse date+time using the section's data-date and the time from the key.
     * Key: "him_8:00_PM_IST_DEVENDER_SINGH", date from keyDateMap: "2026-05-15"
     * Combines to exact datetime in IST.
     */
    private static Date parseDateTime(String key, TimeZone ist) {
        try {
            String dateStr = keyDateMap.get(key);
            if (dateStr == null) {
                log("No section date found for key: " + key);
                return null;
            }

            // Strip prefixes to get to time portion
            String stripped = stripPrefixes(key);
            String[] parts = stripped.split("_");
            if (parts.length < 3) return null;

            String timeString = parts[0] + " " + parts[1]; // e.g. "8:00 PM"

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm a");
            sdf.setTimeZone(ist);
            return sdf.parse(dateStr + " " + timeString);
        } catch (Exception e) {
            log("Could not parse date+time for key: " + key + " — " + e.getMessage());
            return null;
        }
    }

    /** Strip cloud_/conf_/local_ and him_/sud_ prefixes from key. */
    private static String stripPrefixes(String key) {
        String s = key;
        if (s.startsWith("cloud_") || s.startsWith("conf_") || s.startsWith("local_")) {
            s = s.substring(s.indexOf('_') + 1);
        }
        if (s.startsWith("him_") || s.startsWith("sud_")) {
            s = s.substring(s.indexOf('_') + 1);
        }
        return s;
    }

    /**
     * Extract candidate name from key.
     * "him_8:00_PM_IST_DEVENDER_SINGH" → "Devender Singh"
     */
    private static String extractCandidateName(String key) {
        String stripped = stripPrefixes(key);
        // "8:00_PM_IST_DEVENDER_SINGH"
        String[] parts = stripped.split("_");
        if (parts.length <= 3) return "Unknown";
        // parts[0]=time, [1]=AM/PM, [2]=IST, rest=name
        StringBuilder name = new StringBuilder();
        for (int i = 3; i < parts.length; i++) {
            if (name.length() > 0) name.append(" ");
            String p = parts[i];
            name.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase());
        }
        return name.toString();
    }

    /**
     * Extract time string from key.
     * "him_8:00_PM_IST_DEVENDER_SINGH" → "8:00 PM IST"
     */
    private static String extractTimeStr(String key) {
        String stripped = stripPrefixes(key);
        String[] parts = stripped.split("_");
        if (parts.length >= 3) {
            return parts[0] + " " + parts[1] + " " + parts[2];
        }
        return "";
    }

    /**
     * Make a Twilio voice call with TwiML message.
     */
    private static void makeCall(String toNumber, String message) throws Exception {
        String urlStr = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Calls.json";

        String twiml = "<Response><Say voice=\"alice\">" + escapeXml(message) + "</Say></Response>";
        String postData = "To=" + URLEncoder.encode(toNumber, "UTF-8")
                + "&From=" + URLEncoder.encode(FROM_NUMBER, "UTF-8")
                + "&Twiml=" + URLEncoder.encode(twiml, "UTF-8");

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);

        // Basic auth with SID:Token
        String auth = ACCOUNT_SID + ":" + AUTH_TOKEN;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes("UTF-8"));
        conn.setRequestProperty("Authorization", "Basic " + encoded);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        OutputStream os = conn.getOutputStream();
        os.write(postData.getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        BufferedReader br;
        if (responseCode >= 200 && responseCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder resp = new StringBuilder();
        String ln;
        while ((ln = br.readLine()) != null) resp.append(ln);
        br.close();

        if (responseCode >= 200 && responseCode < 300) {
            log("Twilio call created. Response: " + resp.toString().substring(0, Math.min(200, resp.length())));
        } else {
            log("Twilio API error (HTTP " + responseCode + "): " + resp.toString());
        }
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    /**
     * Check if a call was already made for this key today.
     * Reads from twilio-called.txt. If the file has a different date prefix, it's stale and ignored.
     */
    private static boolean isAlreadyCalled(String callKey) {
        try {
            Path path = Paths.get(CALLED_FILE);
            if (!Files.exists(path)) return false;
            List<String> lines = Files.readAllLines(path);
            return lines.contains(callKey);
        } catch (Exception e) {
            log("Error reading called file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mark a call as made by appending to twilio-called.txt.
     * Clears the file first if it contains entries from a previous day.
     */
    private static void markCalled(String callKey) {
        try {
            Path path = Paths.get(CALLED_FILE);
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            // If file exists and has old date entries, clear it
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                if (!lines.isEmpty() && !lines.get(0).startsWith(today)) {
                    Files.write(path, Collections.singletonList(callKey));
                    return;
                }
            }
            // Append
            Files.write(path, Collections.singletonList(callKey),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log("Error writing called file: " + e.getMessage());
        }
    }

    private static void log(String msg) {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("[" + ts + "] " + msg);
    }
}
