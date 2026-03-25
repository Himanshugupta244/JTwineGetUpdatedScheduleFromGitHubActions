package GmailProjectForGitHubActions;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

public class JTwineScheduleForTodayFromGitHubActions {

	public static String todayDate;
	public static String tomorrowDate;
	public static String todayDateDisplay;
	public static String tomorrowDateDisplay;
	public static WebDriver driver;
	public static String username = null;
	public static String password = null;
	public static String usernameVprop = null;
	public static String passwordVprop = null;
	public static String todayDateVpropFormat = null;
	public static String tomorrowDateVpropFormat = null;
	public static List<String> outputLines = new ArrayList<>();

	public static void main(String[] args) {

		username = System.getenv("JTWINE_USERNAME_HIM");
		password = System.getenv("JTWINE_PASSWORD_HIM");
		usernameVprop = System.getenv("VPROP_USERNAME_HIM");
		passwordVprop = System.getenv("VPROP_PASSWORD_HIM");
		todayDate = getTodayDateFormatted();
		tomorrowDate = getTomorrowDateFormatted();
		todayDateDisplay = getTodayDateFormattedForDisplay();
		tomorrowDateDisplay = getTomorrowDateFormattedForDisplay();
		System.out.println("Today's date: " + todayDate);
		outputLines.add("Today's date: " + todayDate);

		try {
			System.out.println("**************** SCHEDULE FOR HIMANSHU JTWINE ACCOUNT ****************");
			outputLines.add("**************** SCHEDULE FOR HIMANSHU JTWINE ACCOUNT :- ****************");
			loginToJTwine();
			List<String> scheduleLines = fetchScheduleForToday();
			outputLines.addAll(scheduleLines);
			driver.quit();
			System.out.println("======================================================================");
			username = System.getenv("JTWINE_USERNAME_SUD");
			password = System.getenv("JTWINE_PASSWORD_SUD");
			System.out.println("**************** SCHEDULE FOR SUDHANSHU JTWINE ACCOUNT ****************");
			outputLines.add("**************** SCHEDULE FOR SUDHANSHU JTWINE ACCOUNT :- ****************");
			loginToJTwine();
			scheduleLines = fetchScheduleForToday();
			outputLines.addAll(scheduleLines);

		} catch(Exception ex) {
			ex.printStackTrace();
			outputLines.add("Exception: " + ex.getMessage());
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}

		// Separate Call for Vprop
		System.out.println("======================================================================");
		System.out.println("**************** SCHEDULE FOR Vprop ACCOUNT :- ****************");
		outputLines.add("**************** SCHEDULE FOR Vprop ACCOUNT :- ****************");
		usernameVprop = System.getenv("VPROP_USERNAME_HIM");
		passwordVprop = System.getenv("VPROP_PASSWORD_HIM");
		todayDateVpropFormat = getTodayDateAsPerVpropFormat();
		tomorrowDateVpropFormat = getTomorrowDateAsPerVpropFormat();
		loginAndFetchVPropScheduleForToday();
		System.out.println("====================== FINAL OUTPUT FOR DEBUGGING IS ================");
		System.out.println(outputLines);

		writeCodeToScheduleTxtFileForGitHub();
		writeCodeToIndexHtmlFileForGitHub();
	}

	public static void loginToJTwine() {
		System.out.println("Logging into JTwine");
		WebDriverManager.chromedriver().setup();
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new");
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--window-size=1920,1080");
		driver = new ChromeDriver(options);
		driver.manage().window().maximize();
		setTimezoneToIST(driver);
		driver.get("https://www.jobtwine.com/signin");
		waitForFixTime(2000);
		if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
			throw new IllegalArgumentException("JTWINE_USERNAME and/or JTWINE_PASSWORD environment variables are not set or empty.");
		}
		waitTillElementVisible(By.xpath(".//input[@formcontrolname='userName']"), 30);
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//input[@formcontrolname='userName']")).sendKeys(username);
		waitForFixTime(1000);
		waitTillElementVisible(By.xpath(".//button[contains(text(),'Next')]"), 30);
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//button[contains(text(),'Next')]")).click();
		waitForFixTime(1000);
		waitTillElementVisible(By.xpath(".//input[@formcontrolname='password']"), 30);
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//input[@formcontrolname='password']")).sendKeys(password);
		waitForFixTime(1000);
		waitTillElementVisible(By.xpath(".//button[contains(text(),'Sign In')]"), 30);
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//button[contains(text(),'Sign In')]")).click();
		waitTillElementVisible(By.xpath(".//div[contains(text(),'Candidates For Interview')]"), 30);
		waitForFixTime(1000);
		if(driver.findElements(By.xpath(".//div[contains(text(),'Candidates For Interview')]")).size() > 0) {
			System.out.println("Login to Jtwin is successful");
		} else {
			throw new RuntimeException("Login to Jtwin failed - 'Candidates For Interview' text not found after login.");
		}
	}

	private static void setTimezoneToIST(WebDriver driver) {
		Map<String, Object> timezone = new HashMap<>();
		timezone.put("timezoneId", "Asia/Kolkata");
		((ChromeDriver) driver).executeCdpCommand("Emulation.setTimezoneOverride", timezone);
	}

	public static List<String> fetchScheduleForToday() throws Exception {
		List<String> todayLines = new ArrayList<>();
		List<String> tomorrowLines = new ArrayList<>();
		System.out.println("Fetching schedule.....");
		System.out.println("======================================================================");
		waitTillElementVisible(By.xpath(".//span[text()='Start Meeting']"), 60);

		// Fetch from Page 1 first
		System.out.println("Fetching schedule from Page 1.....");
		Map<String, List<String>> page1Data = fetchScheduleFromCurrentPage();
		todayLines.addAll(page1Data.get("today"));
		tomorrowLines.addAll(page1Data.get("tomorrow"));

		// Check if page 2 exists and click on it
		List<WebElement> page2Button = driver.findElements(By.xpath("(.//span[contains(text(),'page ')]/following-sibling::span)[2]"));
		if (!page2Button.isEmpty()) {
			System.out.println("Page 2 found, clicking on it.....");
			page2Button.get(0).click();
			System.out.println("Waiting 10 seconds for page 2 to load.....");
			waitForFixTime(10000);

			// Fetch from Page 2
			System.out.println("Fetching schedule from Page 2.....");
			Map<String, List<String>> page2Data = fetchScheduleFromCurrentPage();
			todayLines.addAll(page2Data.get("today"));
			tomorrowLines.addAll(page2Data.get("tomorrow"));
		} else {
			System.out.println("Page 2 not found, only Page 1 results available.");
		}

		// Today's data first, then tomorrow's data
		List<String> lines = new ArrayList<>();
		if (!todayLines.isEmpty()) {
			lines.add("§TODAY§");
			lines.addAll(todayLines);
		}
		if (!tomorrowLines.isEmpty()) {
			lines.add("§TOMORROW§");
			lines.addAll(tomorrowLines);
		}

		if(lines.isEmpty()) {
			System.out.println("No discussions scheduled for today and tomorrow.");
			lines.add("No discussions scheduled for today and tomorrow.");
			System.out.println("======================================================================");
		}
		return lines;
	}

	public static Map<String, List<String>> fetchScheduleFromCurrentPage() throws Exception {
		List<String> todayLines = new ArrayList<>();
		List<String> tomorrowLines = new ArrayList<>();

		String todayLocator = ".//div[@class='sub-sub-heading-1'][contains(text(),'" + todayDate + "')]";
		String todayStatusLocator = todayLocator+"//ancestor::div[contains(@class,'candidate-details-sec')]//div[contains(@class,'btn-chip')]/div";
		List<WebElement> discussionListToday = driver.findElements(By.xpath(todayLocator));
		List<WebElement> discussionStatusListToday = driver.findElements(By.xpath(todayStatusLocator));
		
		System.out.println("Discussions found for today: " + discussionListToday.size());
		System.out.println("Discussions Status found for today: " + discussionStatusListToday.size());

		for (int index = 0; index < discussionListToday.size(); index++) {
			WebElement discussion = discussionListToday.get(index);
			String statusText = (index < discussionStatusListToday.size()) ? discussionStatusListToday.get(index).getText() : "NA";
			System.out.println(discussion.getText() + " ==> " + statusText);
			todayLines.add("✪ " + discussion.getText() + " ==> " + statusText);
		}

		String tomorrowLocator = ".//div[@class='sub-sub-heading-1'][contains(text(),'" + tomorrowDate + "')]";
		String tomorrowStatusLocator = tomorrowLocator+"//ancestor::div[contains(@class,'candidate-details-sec')]//div[contains(@class,'btn-chip')]/div";
		List<WebElement> discussionListTomorrow = driver.findElements(By.xpath(tomorrowLocator));
		List<WebElement> discussionStatusListTomorrow = driver.findElements(By.xpath(tomorrowStatusLocator));

		for (int index = 0; index < discussionListTomorrow.size(); index++) {
			WebElement discussion = discussionListTomorrow.get(index);
			String statusText = (index < discussionStatusListTomorrow.size()) ? discussionStatusListTomorrow.get(index).getText() : "NA";
			System.out.println(discussion.getText() + " ==> " + statusText);
			tomorrowLines.add("✪ " + discussion.getText() + " ==> " + statusText);
		}

		Map<String, List<String>> result = new HashMap<>();
		result.put("today", todayLines);
		result.put("tomorrow", tomorrowLines);
		return result;
	}

	public static void waitForFixTime(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static String getTodayDateFormatted() {
		java.time.LocalDate today = java.time.LocalDate.now();
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d");
		return today.format(formatter);
	}

	public static String getTomorrowDateFormatted() {
		java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
		java.time.format.DateTimeFormatter formatter =
				java.time.format.DateTimeFormatter.ofPattern("MMM d");
		return tomorrow.format(formatter);
	}

	public static String getTodayDateFormattedForDisplay() {
		java.time.LocalDate today = java.time.LocalDate.now();
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM '['EEEE']'");
		return today.format(formatter);
	}

	public static String getTomorrowDateFormattedForDisplay() {
		java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM '['EEEE']'");
		return tomorrow.format(formatter);
	}

	public static void waitTillElementVisible(By locator, int timeoutInSeconds) {
		org.openqa.selenium.support.ui.WebDriverWait wait = new org.openqa.selenium.support.ui.WebDriverWait(driver,
				java.time.Duration.ofSeconds(timeoutInSeconds));
		wait.until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(locator));
	}

	public static void writeCodeToScheduleTxtFileForGitHub() {
		try {
			java.time.ZonedDateTime nowIST =
					java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

			outputLines.add("-----------------------------------");
			outputLines.add("Updated at (IST): " +
					nowIST.format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a")));

			Files.write(Paths.get("schedule.txt"), outputLines, StandardCharsets.UTF_8);
		} catch (IOException ioe) {
			System.err.println("Failed to write schedule.txt: " + ioe.getMessage());
		}
	}

	public static void writeCodeToIndexHtmlFileForGitHub() {
		try {
			// --- Parse outputLines into per-account, per-day buckets ---
			String dateDisplay = "";
			String updatedAt = "";
			java.util.List<String> himToday     = new java.util.ArrayList<>();
			java.util.List<String> himTomorrow  = new java.util.ArrayList<>();
			java.util.List<String> sudToday     = new java.util.ArrayList<>();
			java.util.List<String> sudTomorrow  = new java.util.ArrayList<>();
			java.util.List<String> vpropLines   = new java.util.ArrayList<>();

			String currentAcc = "";
			String currentSec = "";

			for (String line : outputLines) {
				if (line.startsWith("Today's date:")) {
					dateDisplay = line.replace("Today's date:", "").trim();
				} else if (line.startsWith("Updated at (IST):")) {
					updatedAt = line.replace("Updated at (IST):", "").trim();
				} else if (line.startsWith("**************** SCHEDULE FOR HIMANSHU")) {
					currentAcc = "HIM"; currentSec = "";
				} else if (line.startsWith("**************** SCHEDULE FOR SUDHANSHU")) {
					currentAcc = "SUD"; currentSec = "";
				} else if (line.startsWith("**************** SCHEDULE FOR")) {
					currentAcc = "VPROP"; currentSec = "";
				} else if (line.equals("\u00a7TODAY\u00a7")) {
					currentSec = "TODAY";
				} else if (line.equals("\u00a7TOMORROW\u00a7")) {
					currentSec = "TOMORROW";
				} else if (line.startsWith("\u272a ")
						|| (currentAcc.equals("VPROP") && !line.trim().isEmpty()
							&& !line.equals("-----------------------------------"))) {
					if      (currentAcc.equals("HIM")   && currentSec.equals("TODAY"))    himToday.add(line);
					else if (currentAcc.equals("HIM")   && currentSec.equals("TOMORROW")) himTomorrow.add(line);
					else if (currentAcc.equals("SUD")   && currentSec.equals("TODAY"))    sudToday.add(line);
					else if (currentAcc.equals("SUD")   && currentSec.equals("TOMORROW")) sudTomorrow.add(line);
					else if (currentAcc.equals("VPROP"))                                  vpropLines.add(line);
				}
			}

			String dateUpper     = todayDateDisplay != null ? todayDateDisplay.toUpperCase() : dateDisplay.toUpperCase();
			String tomorrowUpper = tomorrowDateDisplay != null ? tomorrowDateDisplay.toUpperCase() : (tomorrowDate != null ? tomorrowDate.toUpperCase() : "");

			StringBuilder html = new StringBuilder();
			html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
			html.append("<meta charset=\"UTF-8\">\n");
			html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
			html.append("<title>Interview Schedule</title>\n");
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
			html.append(".tab-today { background: #1d4ed8; color: #fff; }\n");
			html.append(".tab-tomorrow { background: #b45309; color: #fff; }\n");
			html.append(".tab-vprop { background: #6d28d9; color: #fff; }\n");
			html.append(".section-box-today { border: 3px solid #1d4ed8; background: #fff; }\n");
			html.append(".section-box-tomorrow { border: 3px solid #b45309; background: #fff; }\n");
			html.append(".section-box-vprop { border: 3px solid #6d28d9; background: #fff; }\n");
			// Account label row
			html.append(".acc-label { padding: 10px 14px; font-size: 14px; font-weight: 900; letter-spacing: 1.5px; text-transform: uppercase; border-bottom: 2px solid #e5e7eb; display: flex; align-items: center; gap: 7px; }\n");
			html.append(".acc-him { background: #ede9fe; color: #4c1d95; }\n");
			html.append(".acc-sud { background: #d1fae5; color: #064e3b; }\n");
			html.append(".acc-vp  { background: #fef3c7; color: #78350f; }\n");
			// Rows - two columns with vertical divider
			html.append(".row { display: flex; align-items: stretch; border-bottom: 1px solid #e5e7eb; }\n");
			html.append(".row:last-child { border-bottom: none; }\n");
			html.append(".col-time { flex: 0 0 auto; white-space: nowrap; padding: 12px 14px; font-weight: 800; font-size: 15px; color: #111827; border-right: 1px solid #e5e7eb; letter-spacing: 0.5px; line-height: 1.4; }\n");
			html.append(".col-status { flex: 1; padding: 12px 14px; font-weight: 900; font-size: 15px; text-align: right; letter-spacing: 0.3px; line-height: 1.4; white-space: nowrap; }\n");
			// Status colors (dark)
			html.append(".sc { color: #14532d; }\n");
			html.append(".gf { color: #374151; }\n");
			html.append(".nr { color: #374151; }\n");
			html.append(".ns { color: #374151; }\n");
			html.append(".pd { color: #991b1b; }\n");
			html.append(".empty { padding: 12px 14px; font-size: 14px; font-weight: 700; color: #9ca3af; font-style: italic; letter-spacing: 0.5px; }\n");
			// Footer
			html.append(".footer { border: 3px solid #1a1a1a; padding: 12px; font-size: 13px; font-weight: 800; letter-spacing: 1px; background: #fff; text-align: center; margin-top: 4px; }\n");
			html.append("@media (max-width: 480px) {\n");
			html.append("  body { padding: 10px; }\n");
			html.append("  .header h1 { font-size: 22px; letter-spacing: 4px; }\n");
			html.append("  .tab-label { font-size: 14px; padding: 8px 16px; letter-spacing: 1.5px; }\n");
			html.append("  .acc-label { font-size: 13px; padding: 9px 12px; }\n");
			html.append("  .col-time { font-size: 14px; padding: 11px 12px; }\n");
			html.append("  .col-status { font-size: 14px; padding: 11px 10px; }\n");
			html.append("  .footer { font-size: 12px; }\n");
			html.append("}\n");
			html.append(".collapsible-body { display: none; }\n");
			html.append(".collapsible-body.open { display: block; }\n");
			html.append(".tab-label.clickable { cursor: pointer; display: flex; justify-content: space-between; align-items: center; user-select: none; }\n");
			html.append(".toggle-btn { display: inline-flex; align-items: center; gap: 5px; padding: 4px 14px; font-size: 11px; font-weight: 900; letter-spacing: 1px; color: #b45309; background: #fff; border: 2px solid #fff; border-radius: 20px; white-space: nowrap; transition: background 0.2s, transform 0.15s; box-shadow: 0 1px 3px rgba(0,0,0,0.15); }\n");
			html.append(".toggle-btn:hover { background: #fef3c7; transform: scale(1.05); }\n");
			html.append(".toggle-btn .arrow { font-size: 13px; line-height: 1; }\n");
			html.append("</style>\n</head>\n<body>\n");
			html.append("<div class=\"container\">\n");
			html.append("<div class=\"header\"><h1>&#128197; SCHEDULE</h1></div>\n");

			// --- TODAY block (Blue) ---
			html.append("<div class=\"section\">\n");
			html.append("<div class=\"tab-label tab-today\">&#9728;&#65039; TODAY &mdash; ").append(dateUpper).append("</div>\n");
			html.append("<div class=\"section-box-today\">\n");
			html.append("<div class=\"acc-label acc-him\">&#128100; HIMANSHU &mdash; JTwine</div>\n");
			if (himToday.isEmpty()) {
				html.append("<div class=\"empty\">No interviews today</div>\n");
			} else {
				for (String l : himToday) html.append(buildInterviewRow(l));
			}
			html.append("<div class=\"acc-label acc-sud\">&#128101; SUDHANSHU &mdash; JTwine</div>\n");
			if (sudToday.isEmpty()) {
				html.append("<div class=\"empty\">No interviews today</div>\n");
			} else {
				for (String l : sudToday) html.append(buildInterviewRow(l));
			}
			html.append("</div>\n"); // end section-box-today
			html.append("</div>\n"); // end TODAY section

			// --- VProp block (between Today and Tomorrow) ---
			if (!vpropLines.isEmpty()) {
				html.append("<div class=\"section\">\n");
				html.append("<div class=\"tab-label tab-vprop\">&#11088; VPROP</div>\n");
				html.append("<div class=\"section-box-vprop\">\n");
				html.append("<div class=\"acc-label acc-vp\">&#128100; Himanshu &mdash; VProp</div>\n");
				for (String l : vpropLines) {
					if (l.startsWith("No discussions")) {
						html.append("<div class=\"empty\">" + escapeHtml(l) + "</div>\n");
					} else {
						html.append(buildInterviewRow(l));
					}
				}
				html.append("</div>\n</div>\n");
			}

			// --- TOMORROW block (Amber) — collapsible ---
			html.append("<div class=\"section\">\n");
			html.append("<div class=\"tab-label tab-tomorrow clickable\" onclick=\"toggleSection('tomorrow-body')\">\n");
			html.append("  <span>&#127769; TOMORROW &mdash; ").append(tomorrowUpper).append("</span>\n");
			html.append("  <span class=\"toggle-btn\" id=\"tomorrow-body-icon\"><span class=\"arrow\">&#9660;</span> TAP TO EXPAND</span>\n");
			html.append("</div>\n");
			html.append("<div id=\"tomorrow-body\" class=\"collapsible-body\">\n");
			html.append("<div class=\"section-box-tomorrow\">\n");
			html.append("<div class=\"acc-label acc-him\">&#128100; HIMANSHU &mdash; JTwine</div>\n");
			if (himTomorrow.isEmpty()) {
				html.append("<div class=\"empty\">No interviews tomorrow</div>\n");
			} else {
				for (String l : himTomorrow) html.append(buildInterviewRow(l));
			}
			html.append("<div class=\"acc-label acc-sud\">&#128101; SUDHANSHU &mdash; JTwine</div>\n");
			if (sudTomorrow.isEmpty()) {
				html.append("<div class=\"empty\">No interviews tomorrow</div>\n");
			} else {
				for (String l : sudTomorrow) html.append(buildInterviewRow(l));
			}
			html.append("</div>\n"); // end section-box-tomorrow
			html.append("</div>\n"); // end collapsible-body
			html.append("</div>\n"); // end TOMORROW section

			// --- Footer ---
			if (!updatedAt.isEmpty()) {
				html.append("<div class=\"footer\">&#9201; Updated at (IST): ").append(updatedAt).append("</div>\n");
			}

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

			java.nio.file.Files.write(java.nio.file.Paths.get("deploy/index.html"),
					html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
			System.out.println("index.html generated successfully.");

		} catch (java.io.IOException ioe) {
			System.err.println("Failed to write index.html: " + ioe.getMessage());
		}
	}

	private static String buildInterviewRow(String line) {
		String content = line.startsWith("\u272a ") ? line.substring(2).trim() : line.trim();
		String[] parts = content.split("==>");
		if (parts.length == 2) {
			String disc = parts[0].trim();
			String stat = parts[1].trim();
			String bc;
			switch (stat) {
				case "Scheduled":               bc = "sc"; break;
				case "Not Recommended":         bc = "nr"; break;
				case "Is a Good Fit":           bc = "gf"; break;
				case "Candidate No Show":       bc = "ns"; break;
				case "Cancelled by Candidate":  bc = "ns"; break;
				case "Strongly Recommended":    bc = "gf"; break;
				case "Pending Feedback Review": bc = "pd"; break;
				default: bc = "ns";
			}
			String[] dtParts = disc.split(", ");
			String time = dtParts.length >= 4 ? dtParts[3] : disc;
			return "<div class=\"row\"><div class=\"col-time\">" + escapeHtml(time) + "</div><div class=\"col-status " + bc + "\">" + escapeHtml(stat) + "</div></div>\n";
		}
		return "<div class=\"row\"><div class=\"col-time\">" + escapeHtml(content) + "</div><div class=\"col-status\"></div></div>\n";
	}

	private static String escapeHtml(String text) {
		if (text == null) return "";
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	// Separate Code for VProp
	public static void loginAndFetchVPropScheduleForToday() {
		try {
			System.out.println("Logging into Vprop");
			WebDriverManager.chromedriver().setup();
			ChromeOptions options = new ChromeOptions();
			options.addArguments("--headless=new");
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-dev-shm-usage");
			options.addArguments("--window-size=1920,1080");
			driver = new ChromeDriver(options);
			driver.manage().window().maximize();
			setTimezoneToIST(driver);
			driver.get("https://expert.vprople.com/login");
			waitForFixTime(2000);
			if (usernameVprop == null || usernameVprop.isEmpty() || passwordVprop == null || passwordVprop.isEmpty()) {
				throw new IllegalArgumentException("usernameVprop and/or passwordVprop environment variables are not set or empty.");
			}
			waitTillElementVisible(By.xpath(".//input[@name='email']"), 30);
			driver.findElement(By.xpath(".//input[@name='email']")).sendKeys(usernameVprop);
			waitForFixTime(1000);
			waitTillElementVisible(By.xpath(".//input[@name='password']"), 30);
			driver.findElement(By.xpath(".//input[@name='password']")).sendKeys(passwordVprop);
			waitForFixTime(1000);
			waitTillElementVisible(By.xpath(".//button[@type='submit']"), 30);
			driver.findElement(By.xpath(".//button[@type='submit']")).click();
			waitTillElementVisible(By.xpath(".//header//span[text()='Himanshu']"), 30);
			driver.get("https://expert.vprople.com/");
			System.out.println("Waiting for 10 seconds more......");
			waitForFixTime(10000);
			if(driver.findElements(By.xpath(".//a[span[contains(text(),'Dashboard')]]")).size() > 0) {
				System.out.println("Login to Vprop is successful");
			} else {
				throw new RuntimeException("Login to Vprop is failed - 'Dashboard' text not found after login.");
			}
			driver.get("https://expert.vprople.com/interviews");
			waitTillElementVisible(By.xpath(".//input[contains(@placeholder,'enter candidate name')]"), 30);
			waitForFixTime(1000);
			List<WebElement> todayCards = driver.findElements(By.xpath(".//span[contains(text(),'"+todayDateVpropFormat+"')]/ancestor::div[.//h4 and .//span[@data-slot='badge']][1]"));
			waitForFixTime(500);
			System.out.println("Getting Vprop schedule for today....");
			if(todayCards.isEmpty()) {
				System.out.println("No discussions scheduled for today in Vprop.");
				outputLines.add("No discussions scheduled for today in Vprop.");
			} else {

				for (int index = 0; index < todayCards.size(); index++) {
					WebElement card = todayCards.get(index);
					WebElement dateSpan = card.findElement(By.xpath(".//span[contains(text(),'"+todayDateVpropFormat+"')]"));
					List<WebElement> badgeElements = card.findElements(By.xpath(".//h4/parent::div/parent::div/following-sibling::div//span[@data-slot='badge']"));
					String status = badgeElements.isEmpty() ? "Unknown" : badgeElements.get(0).getText();
					System.out.println(dateSpan.getText() + " ==> " + status);
					outputLines.add(dateSpan.getText() + " ==> " + status);
				}
			}

			// Fetch tomorrow's schedule for Vprop
			List<WebElement> tomorrowCards = driver.findElements(By.xpath(".//span[contains(text(),'"+tomorrowDateVpropFormat+"')]/ancestor::div[.//h4 and .//span[@data-slot='badge']][1]"));
			waitForFixTime(500);
			System.out.println("Getting Vprop schedule for tomorrow....");
			if(tomorrowCards.isEmpty()) {
				System.out.println("No discussions scheduled for tomorrow in Vprop.");
				outputLines.add("No discussions scheduled for tomorrow in Vprop.");
			} else {

				for (int index = 0; index < tomorrowCards.size(); index++) {
					WebElement card = tomorrowCards.get(index);
					WebElement dateSpan = card.findElement(By.xpath(".//span[contains(text(),'"+tomorrowDateVpropFormat+"')]"));
					List<WebElement> badgeElements = card.findElements(By.xpath(".//h4/parent::div/parent::div/following-sibling::div//span[@data-slot='badge']"));
					String status = badgeElements.isEmpty() ? "Unknown" : badgeElements.get(0).getText();
					System.out.println(dateSpan.getText() + " ==> " + status);
					outputLines.add(dateSpan.getText() + " ==> " + status);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			outputLines.add("Exception while fetching Vprop schedule: " + e.getMessage());
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
		System.out.println("======================================================================");
	}

	public static String getTodayDateAsPerVpropFormat() { 
		System.out.println("Getting today's date in Vprop format....");
		String[] dateSplitted = todayDate.split(" ");
		System.out.println("Today's date in Vprop format is : " + dateSplitted[1] + "-" + dateSplitted[0]);
		return dateSplitted[1] + " " + dateSplitted[0]; 
	}

	public static String getTomorrowDateAsPerVpropFormat() { 
		System.out.println("Getting tomorrow's date in Vprop format....");
		String[] dateSplitted = tomorrowDate.split(" ");
		System.out.println("Tomorrow's date in Vprop format is : " + dateSplitted[1] + "-" + dateSplitted[0]);
		return dateSplitted[1] + " " + dateSplitted[0]; 
	}

}