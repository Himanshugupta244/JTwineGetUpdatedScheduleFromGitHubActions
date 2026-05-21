package GmailProjectForGitHubActions;

import java.io.InputStream;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class JTwineIntReminder {

    // === CONFIGURATION ===
    private static final String SPREADSHEET_ID = "1uDZiyJQPF3qfjkFOGAuiNNWeWqr3wlte-MtG9ksqOVI";
    private static final String SHEET_NAME = "Sheet1";
    private static final String SCHEDULE_URL = "https://cloud.codifixsolutions.com";
    private static final String WHATSAPP_GROUP_ID = "120363407057401283@g.us";
    private static final String GREEN_API_INSTANCE_ID = "7107607097";
    private static final String GREEN_API_TOKEN = "df0aeac76a5a40a182289b7565c51138da2c6ff289de46c091";
    private static final String SERVICE_ACCOUNT_JSON = "/jtwinereminder-c1388854430e.json";
    private static final int REMINDER_WINDOW_MIN = 30;

    // ===================== MAIN =====================

    public static void main(String[] args) {
        String now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a"));
        System.out.println("JTwine Interview Reminder - " + now);
        System.out.println("Spreadsheet: " + SPREADSHEET_ID + "\n");

        try {
            // Step 1: Scrape schedule from website
            List<String[]> scraped = scrapeSchedule();
            System.out.println("Scraped " + scraped.size() + " interviews from website.");

            if (!scraped.isEmpty()) {
                // Step 2: Sync to Google Sheet (preserve ReminderSent flags)
                syncToSheet(scraped);

                // Step 3: Check for upcoming interviews and send reminders
                checkAndSendReminders();
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== SCRAPING =====================

    private static List<String[]> scrapeSchedule() throws Exception {
        List<String[]> entries = new ArrayList<>();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(SCHEDULE_URL);
            String html = client.execute(get, response -> EntityUtils.toString(response.getEntity()));

            // Find TODAY section: look for the actual <div with section-box-today class (skip CSS)
            int todayStart = html.indexOf("<div class=\"section-box-today\"");
            if (todayStart < 0) {
                System.out.println("Could not find TODAY section div in HTML");
                return entries;
            }
            // End at the closing </div> of the section-box-today, which is before the next <div class="section">
            int todayEnd = html.indexOf("<div class=\"section\">", todayStart);
            if (todayEnd < 0) todayEnd = html.length();
            String todayHtml = html.substring(todayStart, todayEnd);
            System.out.println("TODAY section length: " + todayHtml.length());

            // Also find TOMORROW section (the website might show today's actual date as tomorrow)
            int tomorrowStart = html.indexOf("<div class=\"section-box-tomorrow\"");
            String tomorrowHtml = "";
            if (tomorrowStart > 0) {
                int tomorrowEnd = html.indexOf("<div class=\"section\">", tomorrowStart);
                if (tomorrowEnd < 0) tomorrowEnd = html.indexOf("<div class=\"footer\">", tomorrowStart);
                if (tomorrowEnd < 0) tomorrowEnd = html.length();
                tomorrowHtml = html.substring(tomorrowStart, tomorrowEnd);
                System.out.println("TOMORROW section length: " + tomorrowHtml.length());
            }

            // Extract dates from data-date attributes
            String todayDate = extractDataDate(todayHtml);
            String tomorrowDate = tomorrowHtml.isEmpty() ? "" : extractDataDate(tomorrowHtml);

            // Warn if website data is stale
            String actualToday = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
            if (!todayDate.isEmpty() && !todayDate.equals(actualToday)) {
                System.out.println("WARNING: Website TODAY section shows date " + todayDate
                        + " but actual today is " + actualToday + " - website may not be updated yet");
            }

            // Parse both sections
            parseSection(todayHtml, entries, "TODAY", todayDate);
            if (!tomorrowHtml.isEmpty()) {
                parseSection(tomorrowHtml, entries, "TOMORROW", tomorrowDate);
            }
        }
        return entries;
    }

    private static String extractDataDate(String sectionHtml) {
        Matcher m = Pattern.compile("data-date=\"([^\"]+)\"").matcher(sectionHtml);
        return m.find() ? m.group(1) : "";
    }

    private static void parseSection(String sectionHtml, List<String[]> entries, String label, String date) {
        // Find person label positions
        Pattern accPattern = Pattern.compile("acc-label\\s+(acc-him|acc-sud)");
        Matcher accMatcher = accPattern.matcher(sectionHtml);
        List<int[]> personPositions = new ArrayList<>(); // [position, 0=him/1=sud]
        while (accMatcher.find()) {
            personPositions.add(new int[]{accMatcher.start(), accMatcher.group(1).equals("acc-him") ? 0 : 1});
        }

        // Find each row and extract data
        int searchFrom = 0;
        while (true) {
            int rowStart = sectionHtml.indexOf("<div class=\"row\">", searchFrom);
            if (rowStart < 0) break;

            // Determine person based on closest preceding acc-label
            String person = "UNKNOWN";
            for (int[] pp : personPositions) {
                if (pp[0] < rowStart) {
                    person = pp[1] == 0 ? "HIMANSHU" : "SUDHANSHU";
                }
            }

            // Find row boundary
            int nextRow = sectionHtml.indexOf("<div class=\"row\">", rowStart + 1);
            int nextLabel = sectionHtml.indexOf("<div class=\"acc-label", rowStart + 1);
            int rowEnd = sectionHtml.length();
            if (nextRow > 0) rowEnd = Math.min(rowEnd, nextRow);
            if (nextLabel > 0) rowEnd = Math.min(rowEnd, nextLabel);
            String rowHtml = sectionHtml.substring(rowStart, rowEnd);

            // Extract time (strip SDET badge if present)
            String time = "";
            Matcher timeM = Pattern.compile("col-time\">(?:<span[^>]*>[^<]*</span>)?([^<]+)</div>")
                    .matcher(rowHtml);
            if (timeM.find()) {
                time = timeM.group(1).trim();
            }

            // Extract candidate name and profile (handle escaped quotes)
            String candidate = "", profile = "";
            Matcher candM = Pattern.compile("showCandidatePopup\\('([^']*(?:\\\\'[^']*)*)',\\s*'([^']*(?:\\\\'[^']*)*)'\\)")
                    .matcher(rowHtml);
            if (candM.find()) {
                candidate = decodeHtml(candM.group(1));
                profile = decodeHtml(candM.group(2));
            }

            // Extract status
            String status = "";
            Matcher statusM = Pattern.compile("col-status[^>]*>([^<]+)</div>").matcher(rowHtml);
            if (statusM.find()) {
                status = statusM.group(1).trim();
            }

            if (!time.isEmpty()) {
                entries.add(new String[]{date, person, time, candidate, profile, status});
                System.out.println("  [" + label + " " + date + "] " + person + " | " + time + " | " + candidate + " | " + status);
            }

            searchFrom = rowStart + 1;
        }
    }

    private static String decodeHtml(String text) {
        return text.replace("&amp;", "&").replace("&#39;", "'")
                   .replace("&quot;", "\"").replace("&lt;", "<")
                   .replace("&gt;", ">");
    }

    // ===================== GOOGLE SHEETS =====================

    private static Sheets getSheetsService() throws Exception {
        InputStream in = JTwineIntReminder.class.getResourceAsStream(SERVICE_ACCOUNT_JSON);
        if (in == null) {
            throw new RuntimeException("Service account JSON not found: " + SERVICE_ACCOUNT_JSON);
        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));
        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("JTwine Reminder")
                .build();
    }

    private static void syncToSheet(List<String[]> scraped) throws Exception {
        Sheets sheets = getSheetsService();
        String range = SHEET_NAME + "!A:G";

        // Read existing data to preserve ReminderSent flags
        Map<String, String> existingFlags = new HashMap<>();
        try {
            ValueRange existing = sheets.spreadsheets().values()
                    .get(SPREADSHEET_ID, range).execute();
            List<List<Object>> rows = existing.getValues();
            if (rows != null) {
                for (int i = 1; i < rows.size(); i++) { // skip header
                    List<Object> row = rows.get(i);
                    if (row.size() >= 7) {
                        // Key: Date|Person|Time|Candidate
                        String key = row.get(0) + "|" + row.get(1) + "|" + row.get(2) + "|" + row.get(3);
                        String flag = row.get(6).toString();
                        existingFlags.put(key, flag);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("No existing sheet data (first run): " + e.getMessage());
        }

        // Build new data with preserved flags
        List<List<Object>> newData = new ArrayList<>();
        newData.add(Arrays.asList("Date", "Person", "Time", "Candidate", "Profile", "Status", "ReminderSent"));
        for (String[] entry : scraped) {
            // entry: [date, person, time, candidate, profile, status]
            String key = entry[0] + "|" + entry[1] + "|" + entry[2] + "|" + entry[3];
            String flag = existingFlags.getOrDefault(key, "NO");
            newData.add(Arrays.asList(entry[0], entry[1], entry[2], entry[3], entry[4], entry[5], flag));
        }

        // Clear and write
        sheets.spreadsheets().values()
                .clear(SPREADSHEET_ID, range, new ClearValuesRequest()).execute();
        sheets.spreadsheets().values()
                .update(SPREADSHEET_ID, range, new ValueRange().setValues(newData))
                .setValueInputOption("RAW").execute();

        System.out.println("Sheet synced: " + scraped.size() + " rows written.");
    }

    private static void checkAndSendReminders() throws Exception {
        Sheets sheets = getSheetsService();
        String range = SHEET_NAME + "!A:G";

        ValueRange result = sheets.spreadsheets().values()
                .get(SPREADSHEET_ID, range).execute();
        List<List<Object>> rows = result.getValues();
        if (rows == null || rows.size() <= 1) return;

        // Current IST time in minutes since midnight
        LocalTime nowIST = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        int nowMinutes = nowIST.getHour() * 60 + nowIST.getMinute();
        System.out.println("Current IST: " + nowIST.format(DateTimeFormatter.ofPattern("hh:mm a")));

        // Today's date in same format as data-date (yyyy-MM-dd)
        String todayDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();

        int remindersChecked = 0;
        int remindersSent = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.size() < 7) continue;

            String date = row.get(0).toString();
            String person = row.get(1).toString();
            String time = row.get(2).toString();
            String candidate = row.get(3).toString();
            String profile = row.get(4).toString();
            String status = row.get(5).toString();
            String reminderSent = row.get(6).toString();

            // Only send reminders for today's interviews
            if (!date.equals(todayDate)) continue;

            // Skip non-Scheduled interviews (completed, cancelled, no-show, etc.)
            if (!"Scheduled".equalsIgnoreCase(status)) continue;

            if ("YES".equalsIgnoreCase(reminderSent)) continue;

            remindersChecked++;
            int interviewMinutes = parseTimeToMinutes(time);
            if (interviewMinutes < 0) continue;

            int minutesUntil = interviewMinutes - nowMinutes;

            if (minutesUntil > 0 && minutesUntil <= REMINDER_WINDOW_MIN) {
                System.out.println("REMINDER: " + person + " interview with " + candidate
                        + " at " + time + " (" + minutesUntil + " min away)");

                String message = "\u23f0 Reminder: " + person + "'s interview with "
                        + candidate + " at " + time
                        + "\nProfile: " + profile
                        + "\nStatus: " + status
                        + "\n" + minutesUntil + " minutes from now";

                boolean sent = sendToWhatsApp(message);
                if (sent) {
                    remindersSent++;
                    // Mark as sent in sheet (column G, row i+1 in 1-indexed)
                    String cellRange = SHEET_NAME + "!G" + (i + 1);
                    sheets.spreadsheets().values()
                            .update(SPREADSHEET_ID, cellRange,
                                    new ValueRange().setValues(Collections.singletonList(
                                            Collections.singletonList((Object) "YES"))))
                            .setValueInputOption("RAW").execute();
                    System.out.println("Reminder sent and flagged for " + candidate);
                }
            }
        }

        if (remindersChecked == 0) {
            System.out.println("No pending Scheduled interviews for today (" + todayDate + ").");
        } else if (remindersSent == 0) {
            System.out.println(remindersChecked + " Scheduled interview(s) pending but none within " + REMINDER_WINDOW_MIN + " min window.");
        } else {
            System.out.println(remindersSent + " reminder(s) sent out of " + remindersChecked + " pending.");
        }
    }

    private static int parseTimeToMinutes(String timeStr) {
        // Parse "9:30 PM IST" or "12:00 AM IST" → minutes since midnight
        Matcher m = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(AM|PM)", Pattern.CASE_INSENSITIVE)
                .matcher(timeStr);
        if (!m.find()) return -1;

        int hours = Integer.parseInt(m.group(1));
        int minutes = Integer.parseInt(m.group(2));
        String ampm = m.group(3).toUpperCase();

        if (ampm.equals("AM") && hours == 12) hours = 0;
        else if (ampm.equals("PM") && hours != 12) hours += 12;

        return hours * 60 + minutes;
    }

    // ===================== WHATSAPP =====================

    private static boolean sendToWhatsApp(String message) {
        String url = "https://7107.api.greenapi.com/waInstance" + GREEN_API_INSTANCE_ID
                + "/sendMessage/" + GREEN_API_TOKEN;

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            String json = "{\"chatId\": \"" + WHATSAPP_GROUP_ID + "\", \"message\": \""
                    + escapeJson(message) + "\"}";
            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            return client.execute(post, response -> {
                int status = response.getCode();
                String body = EntityUtils.toString(response.getEntity());
                System.out.println("Green API [" + status + "]: " + body);
                return status == 200;
            });
        } catch (Exception e) {
            System.out.println("WhatsApp send error: " + e.getMessage());
            return false;
        }
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
