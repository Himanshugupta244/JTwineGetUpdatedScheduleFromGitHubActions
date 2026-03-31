package GmailProjectForGitHubActions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
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
	public static String sessionBase64Him = "";
	public static String sessionBase64Sud = "";

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

			// Capture Himanshu's session for auto-login feature
			sessionBase64Him = captureSessionData("Himanshu");

			driver.quit();
			System.out.println("======================================================================");
			username = System.getenv("JTWINE_USERNAME_SUD");
			password = System.getenv("JTWINE_PASSWORD_SUD");
			System.out.println("**************** SCHEDULE FOR SUDHANSHU JTWINE ACCOUNT ****************");
			outputLines.add("**************** SCHEDULE FOR SUDHANSHU JTWINE ACCOUNT :- ****************");
			loginToJTwine();
			scheduleLines = fetchScheduleForToday();
			outputLines.addAll(scheduleLines);

			// Capture Sudhanshu's session for auto-login feature
			sessionBase64Sud = captureSessionData("Sudhanshu");

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

		// Per-card approach: find each card container, then extract text+status WITHIN that card (tight coupling)
		String todayCardXpath = ".//div[@class='sub-sub-heading-1'][contains(text(),'" + todayDate + "')]//ancestor::div[contains(@class,'candidate-details-sec')]";
		List<WebElement> todayCards = driver.findElements(By.xpath(todayCardXpath));
		System.out.println("Today cards found: " + todayCards.size());

		// First pass: extract text + status + profile name from WITHIN each card (guaranteed same card)
		List<String[]> todayData = new ArrayList<>();
		for (WebElement card : todayCards) {
			String discText = "";
			String statusText = "NA";
			String profileName = "";
			List<WebElement> dateDivs = card.findElements(By.xpath(".//div[@class='sub-sub-heading-1']"));
			if (!dateDivs.isEmpty()) discText = dateDivs.get(0).getText();
			List<WebElement> statusDivs = card.findElements(By.xpath(".//div[contains(@class,'btn-chip')]/div"));
			if (!statusDivs.isEmpty()) statusText = statusDivs.get(0).getText();
			List<WebElement> profileDivs = card.findElements(By.xpath(".//div[contains(text(),'Job Description')]/following-sibling::div[1]"));
			if (!profileDivs.isEmpty()) profileName = profileDivs.get(0).getText().trim();
			System.out.println("  Today Card " + todayData.size() + ": " + discText + " | Status: " + statusText + " | Profile: " + profileName);
			todayData.add(new String[]{discText, statusText, profileName});
		}
		// Second pass: re-find each card by index to capture meeting link (avoids stale element refs)
		for (int index = 0; index < todayData.size(); index++) {
			String discText = todayData.get(index)[0];
			String statusText = todayData.get(index)[1];
			String profileName = todayData.get(index)[2];
			String meetingLink = "";
			if ("Scheduled".equals(statusText)) {
				try {
					String cardBtnXpath = "(" + todayCardXpath + ")[" + (index + 1) + "]//button[.//*[text()='Start Meeting']]";
					List<WebElement> startBtns = driver.findElements(By.xpath(cardBtnXpath));
					if (!startBtns.isEmpty() && startBtns.get(0).isEnabled()) {
						meetingLink = captureMeetingLink(startBtns.get(0));
					} else {
						meetingLink = "NA";
						System.out.println("Start Meeting button not found/disabled for today card " + index);
					}
				} catch (Exception e) {
					meetingLink = "NA";
					System.out.println("Failed to capture meeting link for today card " + index + ": " + e.getMessage());
				}
			}
			String linkPart = !meetingLink.isEmpty() ? " ===LINK=== " + meetingLink : "";
			String profilePart = !profileName.isEmpty() ? " ===PROFILE=== " + profileName : "";
			System.out.println(discText + " ==> " + statusText + profilePart + linkPart);
			todayLines.add("✪ " + discText + " ==> " + statusText + profilePart + linkPart);
		}

		// Per-card approach for tomorrow
		String tomorrowCardXpath = ".//div[@class='sub-sub-heading-1'][contains(text(),'" + tomorrowDate + "')]//ancestor::div[contains(@class,'candidate-details-sec')]";
		List<WebElement> tomorrowCards = driver.findElements(By.xpath(tomorrowCardXpath));
		System.out.println("Tomorrow cards found: " + tomorrowCards.size());

		// First pass: extract text + status + profile name from WITHIN each card
		List<String[]> tomorrowData = new ArrayList<>();
		for (WebElement card : tomorrowCards) {
			String discText = "";
			String statusText = "NA";
			String profileName = "";
			List<WebElement> dateDivs = card.findElements(By.xpath(".//div[@class='sub-sub-heading-1']"));
			if (!dateDivs.isEmpty()) discText = dateDivs.get(0).getText();
			List<WebElement> statusDivs = card.findElements(By.xpath(".//div[contains(@class,'btn-chip')]/div"));
			if (!statusDivs.isEmpty()) statusText = statusDivs.get(0).getText();
			List<WebElement> profileDivs = card.findElements(By.xpath(".//div[contains(text(),'Job Description')]/following-sibling::div[1]"));
			if (!profileDivs.isEmpty()) profileName = profileDivs.get(0).getText().trim();
			System.out.println("  Tomorrow Card " + tomorrowData.size() + ": " + discText + " | Status: " + statusText + " | Profile: " + profileName);
			tomorrowData.add(new String[]{discText, statusText, profileName});
		}
		// Second pass: re-find each card by index to capture meeting link
		for (int index = 0; index < tomorrowData.size(); index++) {
			String discText = tomorrowData.get(index)[0];
			String statusText = tomorrowData.get(index)[1];
			String profileName = tomorrowData.get(index)[2];
			String meetingLink = "";
			if ("Scheduled".equals(statusText)) {
				try {
					String cardBtnXpath = "(" + tomorrowCardXpath + ")[" + (index + 1) + "]//button[.//*[text()='Start Meeting']]";
					List<WebElement> startBtns = driver.findElements(By.xpath(cardBtnXpath));
					if (!startBtns.isEmpty() && startBtns.get(0).isEnabled()) {
						meetingLink = captureMeetingLink(startBtns.get(0));
					} else {
						meetingLink = "NA";
						System.out.println("Start Meeting button not found/disabled for tomorrow card " + index);
					}
				} catch (Exception e) {
					meetingLink = "NA";
					System.out.println("Failed to capture meeting link for tomorrow card " + index + ": " + e.getMessage());
				}
			}
			String linkPart = !meetingLink.isEmpty() ? " ===LINK=== " + meetingLink : "";
			String profilePart = !profileName.isEmpty() ? " ===PROFILE=== " + profileName : "";
			System.out.println(discText + " ==> " + statusText + profilePart + linkPart);
			tomorrowLines.add("✪ " + discText + " ==> " + statusText + profilePart + linkPart);
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
			html.append(".col-time { flex: 0 0 auto; min-width: 140px; white-space: nowrap; padding: 12px 14px; font-weight: 800; font-size: 15px; color: #111827; border-right: 1px solid #e5e7eb; letter-spacing: 0.5px; line-height: 1.4; }\n");
			html.append(".col-status { flex: 1; padding: 12px 14px; font-weight: 900; font-size: 15px; text-align: right; letter-spacing: 0.3px; line-height: 1.4; white-space: nowrap; }\n");
			html.append(".night-badge { display: inline-flex; align-items: center; gap: 1px; background: linear-gradient(135deg, #1e1b4b, #312e81, #4c1d95); color: #fbbf24; font-size: 7px; font-weight: 900; padding: 1px 4px; border-radius: 8px; margin-right: 3px; vertical-align: middle; letter-spacing: 0.2px; box-shadow: 0 0 4px rgba(139,92,246,0.4); animation: nightGlow 2s ease-in-out infinite alternate; white-space: nowrap; }\n");
			html.append(".night-badge .star { font-size: 7px; }\n");
			html.append("@keyframes nightGlow { 0% { box-shadow: 0 0 6px rgba(139,92,246,0.4), 0 0 2px rgba(251,191,36,0.2); } 100% { box-shadow: 0 0 12px rgba(139,92,246,0.7), 0 0 4px rgba(251,191,36,0.5); } }\n");
			html.append("@keyframes twinkle { 0% { opacity: 0.5; } 100% { opacity: 1; } }\n");
			// Status colors (dark)
			html.append(".sc { color: #14532d; }\n");
			html.append(".gf { color: #374151; }\n");
			html.append(".nr { color: #374151; }\n");
			html.append(".ns { color: #374151; }\n");
			html.append(".pd { color: #991b1b; }\n");
			html.append(".empty { padding: 12px 14px; font-size: 14px; font-weight: 700; color: #9ca3af; font-style: italic; letter-spacing: 0.5px; }\n");
			// Past interview styling
			html.append(".past-interview { opacity: 0.5; background: #f3f4f6; }\n");
			html.append(".past-interview .col-time, .past-interview .col-status { text-decoration: line-through; }\n");
			// Meeting JOIN link button
			html.append(".col-link { flex: 0 0 44px; min-width: 44px; max-width: 44px; padding: 4px 0; display: flex; align-items: center; justify-content: center; border-right: 1px solid #e5e7eb; text-align: center; }\n");
			html.append(".join-btn { display: inline-block; font-size: 8px; font-weight: 900; color: #fff; background: #059669; padding: 3px 6px; border-radius: 3px; text-decoration: none; letter-spacing: 0.5px; white-space: nowrap; transition: background 0.2s, transform 0.15s; border: none; cursor: pointer; }\n");
			html.append(".join-btn:hover { background: #047857; transform: scale(1.06); }\n");
			html.append(".join-na { font-size: 8px; font-weight: 700; color: #d1d5db; }\n");
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
			// Session bar styles
			html.append(".session-bar { display:flex; gap:8px; margin-bottom:18px; align-items:stretch; }\n");
			html.append(".session-bar .bm-drag { flex:0 0 auto; display:flex; align-items:center; padding:8px 14px; font-size:11px; font-weight:900; color:#fff; background:linear-gradient(135deg,#7c3aed,#6d28d9); border-radius:8px; text-decoration:none; cursor:grab; letter-spacing:0.5px; white-space:nowrap; box-shadow:0 2px 8px rgba(124,58,237,0.3); transition:transform 0.15s; }\n");
			html.append(".session-bar .bm-drag:hover { transform:scale(1.04); }\n");
			html.append(".session-bar .bm-drag:active { cursor:grabbing; }\n");
			html.append(".session-bar .session-hint { flex:1; display:flex; align-items:center; justify-content:center; font-size:10px; font-weight:900; color:#6b7280; letter-spacing:0.5px; text-align:center; }\n");
			html.append("</style>\n</head>\n<body>\n");
			html.append("<div class=\"container\">\n");
			html.append("<div class=\"header\"><h1>&#128197; SCHEDULE</h1></div>\n");

			// --- Session Bar (Bookmarklet + Copy for both accounts) ---
			String bookmarklet = "javascript:void((function(){navigator.clipboard.readText().then(function(d){if(!d){alert('Clipboard empty!');return;}try{var obj=JSON.parse(atob(d));obj.cookies.forEach(function(c){document.cookie=c.n+'='+c.v+';domain='+c.d+';path='+c.p+(c.s?';secure':'');});var ls=obj.ls;for(var k in ls){localStorage.setItem(k,ls[k]);}var ss=obj.ss;for(var k in ss){sessionStorage.setItem(k,ss[k]);}location.reload();}catch(e){alert('Error: '+e.message);}}).catch(function(e){alert('Clipboard access denied: '+e.message);});})())";
			boolean hasHim = sessionBase64Him != null && !sessionBase64Him.isEmpty();
			boolean hasSud = sessionBase64Sud != null && !sessionBase64Sud.isEmpty();
			if (hasHim || hasSud) {
				html.append("<div class=\"session-bar\">\n");
				html.append("<a class=\"bm-drag\" href=\"").append(bookmarklet.replace("&", "&amp;").replace("\"", "&quot;")).append("\">&#128275; JTwine Login<br><span style='font-size:8px;opacity:0.7'>DRAG TO BOOKMARKS</span></a>\n");
				html.append("<span class=\"session-hint\">JOIN auto-copies correct session</span>\n");
				if (hasHim) {
					html.append("<textarea id=\"sessionBlobHim\" style=\"display:none\">").append(sessionBase64Him).append("</textarea>\n");
				}
				if (hasSud) {
					html.append("<textarea id=\"sessionBlobSud\" style=\"display:none\">").append(sessionBase64Sud).append("</textarea>\n");
				}
				html.append("</div>\n");
			}

			// --- TODAY block (Blue) ---
			html.append("<div class=\"section\">\n");
			html.append("<div class=\"tab-label tab-today\">&#9728;&#65039; TODAY &mdash; ").append(dateUpper).append("</div>\n");
			html.append("<div class=\"section-box-today\">\n");
			html.append("<div class=\"acc-label acc-him\">&#128100; HIMANSHU &mdash; JTwine</div>\n");
			if (himToday.isEmpty()) {
				html.append("<div class=\"empty\">No interviews today</div>\n");
			} else {
				for (String l : himToday) html.append(buildInterviewRow(l, "him"));
			}
			html.append("<div class=\"acc-label acc-sud\">&#128101; SUDHANSHU &mdash; JTwine</div>\n");
			if (sudToday.isEmpty()) {
				html.append("<div class=\"empty\">No interviews today</div>\n");
			} else {
				for (String l : sudToday) html.append(buildInterviewRow(l, "sud"));
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
						html.append(buildInterviewRow(l, ""));
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
				for (String l : himTomorrow) html.append(buildInterviewRow(l, "him"));
			}
			html.append("<div class=\"acc-label acc-sud\">&#128101; SUDHANSHU &mdash; JTwine</div>\n");
			if (sudTomorrow.isEmpty()) {
				html.append("<div class=\"empty\">No interviews tomorrow</div>\n");
			} else {
				for (String l : sudTomorrow) html.append(buildInterviewRow(l, "sud"));
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

			html.append("function markPastInterviews() {\n");
			html.append("  var now = new Date();\n");
			html.append("  var utcMs = now.getTime() + (now.getTimezoneOffset() * 60000);\n");
			html.append("  var istMs = utcMs + (5.5 * 3600000);\n");
			html.append("  var ist = new Date(istMs);\n");
			html.append("  var istMinutes = ist.getHours() * 60 + ist.getMinutes();\n");
			html.append("  var rows = document.querySelectorAll('.section-box-today .row, .section-box-vprop .row');\n");
			html.append("  rows.forEach(function(row) {\n");
			html.append("    var timeEl = row.querySelector('.col-time');\n");
			html.append("    if (!timeEl) return;\n");
			html.append("    var text = timeEl.textContent.trim();\n");
			html.append("    var match = text.match(/(\\d{1,2}):(\\d{2})\\s*(AM|PM)/i);\n");
			html.append("    if (!match) return;\n");
			html.append("    var h = parseInt(match[1]); var m = parseInt(match[2]);\n");
			html.append("    var ampm = match[3].toUpperCase();\n");
			html.append("    if (ampm === 'AM' && h === 12) h = 0;\n");
			html.append("    else if (ampm === 'PM' && h !== 12) h += 12;\n");
			html.append("    var interviewMin = h * 60 + m;\n");
			html.append("    if (istMinutes > interviewMin) {\n");
			html.append("      row.classList.add('past-interview');\n");
			html.append("    }\n");
			html.append("  });\n");
			html.append("}\n");
			html.append("markPastInterviews();\n");
			html.append("function copyAndJoin(who,url){\n");
			html.append("  var id=who==='him'?'sessionBlobHim':'sessionBlobSud';\n");
			html.append("  var el=document.getElementById(id);\n");
			html.append("  if(!el||!el.value){window.open(url,'_blank');return;}\n");
			html.append("  navigator.clipboard.writeText(el.value).then(function(){\n");
			html.append("    window.open(url,'_blank');\n");
			html.append("  }).catch(function(){window.open(url,'_blank');});\n");
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

	private static String buildInterviewRow(String line, String account) {
		String content = line.startsWith("\u272a ") ? line.substring(2).trim() : line.trim();

		// Extract meeting link if present
		String meetingLink = "";
		if (content.contains("===LINK===")) {
			String[] linkParts = content.split("===LINK===", 2);
			content = linkParts[0].trim();
			meetingLink = linkParts[1].trim();
		}

		// Extract profile name if present
		String profileName = "";
		if (content.contains("===PROFILE===")) {
			String[] profileParts = content.split("===PROFILE===", 2);
			content = profileParts[0].trim();
			profileName = profileParts[1].trim();
		}

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
			String nightPrefix = "";
			if (isNightInterview(profileName)) {
				nightPrefix = "<span class=\"night-badge\"><span class=\"star\">&#11088;</span>NIGHT</span>";
			} else if (isGoodNightInterview(profileName)) {
				nightPrefix = "<span class=\"night-badge\"><span class=\"star\">&#11088;</span>GOOD NIGHT</span>";
			}

			// Build link column — JOIN auto-copies correct session
			String linkHtml;
			if (!meetingLink.isEmpty() && !"NA".equals(meetingLink)) {
				String safeUrl = escapeHtml(meetingLink).replace("'", "\\'");
				linkHtml = "<div class=\"col-link\"><button onclick=\"copyAndJoin('" + account + "','" + safeUrl + "')\" class=\"join-btn\">JOIN &#9654;</button></div>";
			} else if ("NA".equals(meetingLink)) {
				linkHtml = "<div class=\"col-link\"><span class=\"join-na\">NA</span></div>";
			} else {
				linkHtml = "<div class=\"col-link\"><span class=\"join-na\">&mdash;</span></div>";
			}

			return "<div class=\"row\"><div class=\"col-time\">" + nightPrefix + escapeHtml(time) + "</div>" + linkHtml + "<div class=\"col-status " + bc + "\">" + escapeHtml(stat) + "</div></div>\n";
		}
		return "<div class=\"row\"><div class=\"col-time\">" + escapeHtml(content) + "</div><div class=\"col-link\"><span class=\"join-na\">&mdash;</span></div><div class=\"col-status\"></div></div>\n";
	}

	private static boolean isNightInterview(String profileName) {
		if (profileName == null || profileName.isEmpty()) return false;
		// NIGHT badge if profile name is "SDET" optionally followed by non-letter characters only
		// Matches: SDET, SDET(1331), SDET (121), SDET 123, SDET !@#456
		// Does NOT match: SDET A, SDET (131D), QA, Senior QA, QA (8804)
		return profileName.trim().matches("(?i)^SDET[^a-zA-Z]*$");
	}

	private static boolean isGoodNightInterview(String profileName) {
		if (profileName == null || profileName.isEmpty()) return false;
		String lower = profileName.toLowerCase();
		return lower.contains("software development engineer in test")
			|| lower.matches("(?i)^sr\\.?\\s*sdet[^a-zA-Z]*$");
	}

	private static String escapeHtml(String text) {
		if (text == null) return "";
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static String captureSessionData(String accountName) {
		try {
			System.out.println("Capturing session data for " + accountName + "...");
			Set<Cookie> cookies = driver.manage().getCookies();
			String ls = (String) ((ChromeDriver) driver).executeScript(
				"var items={}; for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);items[k]=localStorage.getItem(k);} return JSON.stringify(items);"
			);
			String ss = (String) ((ChromeDriver) driver).executeScript(
				"var items={}; for(var i=0;i<sessionStorage.length;i++){var k=sessionStorage.key(i);items[k]=sessionStorage.getItem(k);} return JSON.stringify(items);"
			);
			StringBuilder allData = new StringBuilder();
			allData.append("{\"cookies\":[");
			boolean first = true;
			for (Cookie c : cookies) {
				if (!first) allData.append(",");
				first = false;
				allData.append("{\"n\":\"").append(jsonEsc(c.getName()))
					.append("\",\"v\":\"").append(jsonEsc(c.getValue()))
					.append("\",\"d\":\"").append(jsonEsc(c.getDomain() != null ? c.getDomain() : ""))
					.append("\",\"p\":\"").append(jsonEsc(c.getPath() != null ? c.getPath() : "/"))
					.append("\",\"s\":").append(c.isSecure()).append("}");
			}
			allData.append("],\"ls\":").append(ls).append(",\"ss\":").append(ss).append("}");
			String base64 = Base64.getEncoder().encodeToString(allData.toString().getBytes(StandardCharsets.UTF_8));
			System.out.println(accountName + " session data captured (" + base64.length() + " chars base64)");
			return base64;
		} catch (Exception se) {
			System.out.println("Warning: Could not capture " + accountName + " session data: " + se.getMessage());
			return "";
		}
	}

	private static String jsonEsc(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
	}

	/**
	 * Clicks "Start Meeting" on a JTwine interview card, handles the guidelines dialog,
	 * captures the meeting URL from the new tab, then closes the tab and returns the URL.
	 */
	private static String captureMeetingLink(WebElement startMeetingBtn) {
		String originalWindow = null;
		try {
			originalWindow = driver.getWindowHandle();
			java.util.Set<String> existingWindows = driver.getWindowHandles();
			System.out.println("Clicking Start Meeting button to capture link...");

			// Step 1: Click the card's Start Meeting button (opens guidelines dialog)
			startMeetingBtn.click();
			waitForFixTime(3000);

			// Step 2: Find and click 'Start Meeting' inside the guidelines dialog
			// Try multiple strategies to find the dialog button
			List<WebElement> dialogBtns = driver.findElements(By.xpath(
				".//button[.//*[contains(text(),'Start Meeting')]][not(ancestor::*[contains(@class,'candidate-details')])]"
			));
			System.out.println("  Dialog btn strategy 1 (not in candidate-details): " + dialogBtns.size());
			if (dialogBtns.isEmpty()) {
				dialogBtns = driver.findElements(By.xpath(
					".//*[@role='dialog']//button[contains(.,'Start Meeting')]"
				));
				System.out.println("  Dialog btn strategy 2 (role=dialog): " + dialogBtns.size());
			}
			if (dialogBtns.isEmpty()) {
				// Angular Material uses cdk-overlay-container outside the app root
				dialogBtns = driver.findElements(By.xpath(
					".//*[contains(@class,'cdk-overlay')]//button[contains(.,'Start Meeting')]"
				));
				System.out.println("  Dialog btn strategy 3 (cdk-overlay): " + dialogBtns.size());
			}
			if (dialogBtns.isEmpty()) {
				// Last fallback: get the last Start Meeting button on page (dialog one appears last in DOM)
				List<WebElement> allBtns = driver.findElements(By.xpath(".//button[.//*[contains(text(),'Start Meeting')]]"));
				System.out.println("  Dialog btn strategy 4 (last of all): " + allBtns.size() + " total Start Meeting buttons");
				if (allBtns.size() > 1) {
					dialogBtns = new ArrayList<>();
					dialogBtns.add(allBtns.get(allBtns.size() - 1));
				}
			}

			if (dialogBtns.isEmpty()) {
				System.out.println("WARNING: Dialog Start Meeting button not found — trying Escape to dismiss");
				dismissDialogAndStabilize();
				return "NA";
			}

			System.out.println("Clicking dialog Start Meeting button...");
			dialogBtns.get(0).click();
			waitForFixTime(4000);

			// Step 3: Capture the meeting URL from the new tab
			java.util.Set<String> allWindows = driver.getWindowHandles();
			String meetingUrl = "";

			for (String handle : allWindows) {
				if (!existingWindows.contains(handle)) {
					driver.switchTo().window(handle);
					waitForFixTime(2000);
					meetingUrl = driver.getCurrentUrl();
					if ("about:blank".equals(meetingUrl)) {
						waitForFixTime(3000);
						meetingUrl = driver.getCurrentUrl();
					}
					System.out.println("Captured meeting URL from new tab: " + meetingUrl);
					driver.close();
					driver.switchTo().window(originalWindow);
					break;
				}
			}

			// If no new tab opened, check if current URL changed (same-page redirect)
			if (meetingUrl.isEmpty()) {
				String currentUrl = driver.getCurrentUrl();
				System.out.println("No new tab — current URL: " + currentUrl);
				if (!currentUrl.contains("/interviewer/candidates")) {
					meetingUrl = currentUrl;
					System.out.println("Captured meeting URL from redirect: " + meetingUrl);
					driver.navigate().back();
					waitForFixTime(3000);
					try { waitTillElementVisible(By.xpath(".//span[text()='Start Meeting']"), 15); } catch (Exception ignored) {}
				}
			}

			// Step 4: Dismiss any remaining dialog/overlay and stabilize DOM for next card
			dismissDialogAndStabilize();

			if (meetingUrl.isEmpty() || "about:blank".equals(meetingUrl)) {
				return "NA";
			}
			return meetingUrl;
		} catch (Exception e) {
			System.out.println("Exception in captureMeetingLink: " + e.getMessage());
			e.printStackTrace();
			// Recovery: close extra tabs, dismiss dialogs, get back to candidates page
			try {
				java.util.Set<String> handles = driver.getWindowHandles();
				if (originalWindow == null) originalWindow = handles.iterator().next();
				for (String h : handles) {
					if (!h.equals(originalWindow)) {
						driver.switchTo().window(h);
						driver.close();
					}
				}
				driver.switchTo().window(originalWindow);
				dismissDialogAndStabilize();
			} catch (Exception ex) {
				System.out.println("Recovery also failed: " + ex.getMessage());
			}
			return "NA";
		}
	}

	/**
	 * Dismiss any open dialog/overlay and wait for the candidates page to stabilize.
	 * Uses Escape key (works for Angular Material dialogs), then tries X button as fallback.
	 */
	private static void dismissDialogAndStabilize() {
		try {
			// Try pressing Escape to close any Angular Material dialog/overlay
			driver.findElement(By.tagName("body")).sendKeys(org.openqa.selenium.Keys.ESCAPE);
			waitForFixTime(1000);
		} catch (Exception ignored) {}
		try {
			// Fallback: click any visible close/X button
			List<WebElement> closeBtns = driver.findElements(By.xpath(
				".//*[contains(@class,'cdk-overlay')]//button[contains(text(),'\u00d7')] | .//button[contains(@class,'close')]"
			));
			if (!closeBtns.isEmpty()) {
				closeBtns.get(0).click();
				waitForFixTime(500);
			}
		} catch (Exception ignored) {}
		// Wait briefly for DOM to settle before interacting with next card
		waitForFixTime(1500);
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