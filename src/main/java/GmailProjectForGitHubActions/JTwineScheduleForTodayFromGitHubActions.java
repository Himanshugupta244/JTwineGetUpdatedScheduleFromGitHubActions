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
			System.out.println("Exception occurred: " + ex.getMessage());
			ex.printStackTrace();
			if (driver != null) {
				System.out.println("Current URL: " + driver.getCurrentUrl());
				System.out.println("Page title: " + driver.getTitle());
				System.out.println("Keeping browser open for 15 seconds to inspect...");
				waitForFixTime(15000);
			}
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
		driver.get("https://app.jobtwine.com/signin");
		waitForFixTime(5000); // Increased wait to see the page load
		if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
			throw new IllegalArgumentException("JTWINE_USERNAME and/or JTWINE_PASSWORD environment variables are not set or empty.");
		}
		
		System.out.println("Looking for username field...");
		waitTillElementVisible(By.xpath(".//input[@formcontrolname='userName']"), 30);
		waitForFixTime(1000);
		System.out.println("Username field found, entering username...");
		driver.findElement(By.xpath(".//input[@formcontrolname='userName']")).sendKeys(username);
		waitForFixTime(1000);
		System.out.println("Looking for Next button...");
		waitTillElementVisible(By.xpath(".//button[contains(text(),'Next')]"), 30);
		waitForFixTime(1000);
		System.out.println("Next button found, clicking...");
		driver.findElement(By.xpath(".//button[contains(text(),'Next')]")).click();
		waitForFixTime(1000);
		System.out.println("Looking for password field...");
		waitTillElementVisible(By.xpath(".//input[@formcontrolname='password']"), 30);
		waitForFixTime(1000);
		System.out.println("Password field found, entering password...");
		driver.findElement(By.xpath(".//input[@formcontrolname='password']")).sendKeys(password);
		waitForFixTime(1000);
		System.out.println("Looking for Sign In button...");
		waitTillElementVisible(By.xpath(".//button[contains(text(),'Sign In')]"), 30);
		waitForFixTime(1000);
		System.out.println("Sign In button found, clicking...");
		driver.findElement(By.xpath(".//button[contains(text(),'Sign In')]")).click();
		waitTillElementVisible(By.xpath(".//div[contains(text(),'Candidates For Interview')]"), 30);
		waitForFixTime(1000);
		if(driver.findElements(By.xpath(".//div[contains(text(),'Candidates For Interview')]")).size() > 0) {
			System.out.println("Login to Jtwin is successful");
		} else {
			System.out.println("Login verification failed - keeping browser open for inspection...");
			System.out.println("Page title: " + driver.getTitle());
			System.out.println("Current URL: " + driver.getCurrentUrl());
			waitForFixTime(10000); // Keep browser open for 10 seconds to inspect
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

		// Check pages 2 through 4
		for (int pageNum = 2; pageNum <= 4; pageNum++) {
			List<WebElement> pageButton = driver.findElements(By.xpath("(.//span[contains(text(),'page ')]/following-sibling::span)[" + pageNum + "]"));
			if (!pageButton.isEmpty()) {
				System.out.println("Page " + pageNum + " found, clicking on it.....");
				pageButton.get(0).click();
				System.out.println("Waiting 10 seconds for page " + pageNum + " to load.....");
				waitForFixTime(10000);

				System.out.println("Fetching schedule from Page " + pageNum + ".....");
				Map<String, List<String>> pageData = fetchScheduleFromCurrentPage();
				todayLines.addAll(pageData.get("today"));
				tomorrowLines.addAll(pageData.get("tomorrow"));
			} else {
				System.out.println("Page " + pageNum + " not found, stopping pagination.");
				break;
			}
		}

		// Deduplicate — same card can appear on multiple pages
		todayLines = new ArrayList<>(new java.util.LinkedHashSet<>(todayLines));
		tomorrowLines = new ArrayList<>(new java.util.LinkedHashSet<>(tomorrowLines));

		// Remove cancelled interviews
		todayLines.removeIf(line -> line.toLowerCase().contains("cancelled"));
		tomorrowLines.removeIf(line -> line.toLowerCase().contains("cancelled"));

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
			String discText = "NA";
			String statusText = "NA";
			String profileName = "NA";
			String candidateName = "NA";
			try {
				List<WebElement> dateDivs = card.findElements(By.xpath(".//div[@class='sub-sub-heading-1']"));
				if (!dateDivs.isEmpty()) { String v = dateDivs.get(0).getText().trim(); if (!v.isEmpty()) discText = v; }
			} catch (Exception e) { System.out.println("  Could not fetch discText (today): " + e.getMessage()); }
			try {
				List<WebElement> statusDivs = card.findElements(By.xpath(".//div[contains(@class,'btn-chip')]/div"));
				if (!statusDivs.isEmpty()) { String v = statusDivs.get(0).getText().trim(); if (!v.isEmpty()) statusText = v; }
			} catch (Exception e) { System.out.println("  Could not fetch statusText (today): " + e.getMessage()); }
			try {
				List<WebElement> profileDivs = card.findElements(By.xpath(".//div[contains(text(),'Job Description')]/following-sibling::div[1]"));
				if (!profileDivs.isEmpty()) { String v = profileDivs.get(0).getText().trim(); if (!v.isEmpty()) profileName = v; }
			} catch (Exception e) { System.out.println("  Could not fetch profileName (today): " + e.getMessage()); }
			try {
				// Try within card scope first; fallback to parent scope (element may sit outside candidate-details-sec)
				List<WebElement> candidateDivs = card.findElements(By.xpath(".//*[contains(@class,'f-18')]"));
				if (candidateDivs.isEmpty()) {
					candidateDivs = card.findElements(By.xpath("./..//*[contains(@class,'f-18')]"));
				}
				if (!candidateDivs.isEmpty()) {
					// Use JS textContent — works regardless of CSS visibility (getText() fails on hidden elements in headless)
					String v = (String) ((ChromeDriver) driver).executeScript("return arguments[0].textContent", candidateDivs.get(0));
					if (v != null) v = v.trim();
					if (v != null && !v.isEmpty()) candidateName = v;
				}
			} catch (Exception e) { System.out.println("  Could not fetch candidateName (today): " + e.getMessage()); }
			System.out.println("  Today Card " + todayData.size() + ": " + discText + " | Status: " + statusText + " | Profile: " + profileName + " | Candidate: " + candidateName);
			todayData.add(new String[]{discText, statusText, profileName, candidateName});
		}
		// Second pass: re-find each card by index to capture meeting link (avoids stale element refs)
		for (int index = 0; index < todayData.size(); index++) {
			String discText = todayData.get(index)[0];
			String statusText = todayData.get(index)[1];
			String profileName = todayData.get(index)[2];
			String candidateName = todayData.get(index).length > 3 ? todayData.get(index)[3] : "";
			String meetingLink = "";
			String feedbackLink = "";
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
			} else if ("Pending Feedback Review".equals(statusText)) {
				try {
					String feedbackXpath = "(" + todayCardXpath + ")[" + (index + 1) + "]//*[@id='addFeedbackCtaId']";
					List<WebElement> feedbackBtns = driver.findElements(By.xpath(feedbackXpath));
					if (!feedbackBtns.isEmpty() && feedbackBtns.get(0).isDisplayed()) {
						feedbackLink = captureFeedbackLink(feedbackBtns.get(0));
						System.out.println("feedbackLink returned: '" + feedbackLink + "'");
					} else {
						feedbackLink = "NA";
						System.out.println("Add Feedback element not found/hidden for today card " + index);
					}
				} catch (Exception e) {
					feedbackLink = "NA";
					System.out.println("Failed to capture feedback link for today card " + index + ": " + e.getMessage());
				}
			}
			if (meetingLink.isEmpty()) meetingLink = "NA";
			if (feedbackLink.isEmpty()) feedbackLink = "NA";
			String linkPart = " ===LINK=== " + meetingLink;
			String feedbackPart = " ===FEEDBACK=== " + feedbackLink;
			String profilePart = " ===PROFILE=== " + profileName;
			String candidatePart = " ===CANDIDATE=== " + candidateName;
			System.out.println(discText + " ==> " + statusText + profilePart + candidatePart + linkPart + feedbackPart);
			if (!feedbackLink.isEmpty() && !"NA".equals(feedbackLink)) {
				System.out.println("DEBUG: Feedback link included in output: " + feedbackLink);
			}
			todayLines.add("✪ " + discText + " ==> " + statusText + profilePart + candidatePart + linkPart + feedbackPart);
		}

		// Per-card approach for tomorrow
		String tomorrowCardXpath = ".//div[@class='sub-sub-heading-1'][contains(text(),'" + tomorrowDate + "')]//ancestor::div[contains(@class,'candidate-details-sec')]";
		List<WebElement> tomorrowCards = driver.findElements(By.xpath(tomorrowCardXpath));
		System.out.println("Tomorrow cards found: " + tomorrowCards.size());

		// First pass: extract text + status + profile name from WITHIN each card
		List<String[]> tomorrowData = new ArrayList<>();
		for (WebElement card : tomorrowCards) {
			String discText = "NA";
			String statusText = "NA";
			String profileName = "NA";
			String candidateName = "NA";
			try {
				List<WebElement> dateDivs = card.findElements(By.xpath(".//div[@class='sub-sub-heading-1']"));
				if (!dateDivs.isEmpty()) { String v = dateDivs.get(0).getText().trim(); if (!v.isEmpty()) discText = v; }
			} catch (Exception e) { System.out.println("  Could not fetch discText (tomorrow): " + e.getMessage()); }
			try {
				List<WebElement> statusDivs = card.findElements(By.xpath(".//div[contains(@class,'btn-chip')]/div"));
				if (!statusDivs.isEmpty()) { String v = statusDivs.get(0).getText().trim(); if (!v.isEmpty()) statusText = v; }
			} catch (Exception e) { System.out.println("  Could not fetch statusText (tomorrow): " + e.getMessage()); }
			try {
				List<WebElement> profileDivs = card.findElements(By.xpath(".//div[contains(text(),'Job Description')]/following-sibling::div[1]"));
				if (!profileDivs.isEmpty()) { String v = profileDivs.get(0).getText().trim(); if (!v.isEmpty()) profileName = v; }
			} catch (Exception e) { System.out.println("  Could not fetch profileName (tomorrow): " + e.getMessage()); }
			try {
				// Try within card scope first; fallback to parent scope
				List<WebElement> candidateDivs = card.findElements(By.xpath(".//*[contains(@class,'f-18')]"));
				if (candidateDivs.isEmpty()) {
					candidateDivs = card.findElements(By.xpath("./..//*[contains(@class,'f-18')]"));
				}
				if (!candidateDivs.isEmpty()) {
					// Use JS textContent — works regardless of CSS visibility (getText() fails on hidden elements in headless)
					String v = (String) ((ChromeDriver) driver).executeScript("return arguments[0].textContent", candidateDivs.get(0));
					if (v != null) v = v.trim();
					if (v != null && !v.isEmpty()) candidateName = v;
				}
			} catch (Exception e) { System.out.println("  Could not fetch candidateName (tomorrow): " + e.getMessage()); }
			System.out.println("  Tomorrow Card " + tomorrowData.size() + ": " + discText + " | Status: " + statusText + " | Profile: " + profileName + " | Candidate: " + candidateName);
			tomorrowData.add(new String[]{discText, statusText, profileName, candidateName});
		}
		// Second pass: re-find each card by index to capture meeting link
		for (int index = 0; index < tomorrowData.size(); index++) {
			String discText = tomorrowData.get(index)[0];
			String statusText = tomorrowData.get(index)[1];
			String profileName = tomorrowData.get(index)[2];
			String candidateName = tomorrowData.get(index).length > 3 ? tomorrowData.get(index)[3] : "";
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
			if (meetingLink.isEmpty()) meetingLink = "NA";
			String linkPart = " ===LINK=== " + meetingLink;
			String profilePart = " ===PROFILE=== " + profileName;
			String candidatePart = " ===CANDIDATE=== " + candidateName;
			System.out.println(discText + " ==> " + statusText + profilePart + candidatePart + linkPart);
			tomorrowLines.add("✪ " + discText + " ==> " + statusText + profilePart + candidatePart + linkPart);
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
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy");
		return today.format(formatter);
	}

	public static String getTomorrowDateFormattedForDisplay() {
		java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy");
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

			String dateFormatted     = todayDateDisplay != null ? todayDateDisplay : dateDisplay;
			String tomorrowFormatted = tomorrowDateDisplay != null ? tomorrowDateDisplay : (tomorrowDate != null ? tomorrowDate : "");
			String todayISO = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString();
			String tomorrowISO = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).plusDays(1).toString();

			StringBuilder html = new StringBuilder();
			html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
			html.append("<meta charset=\"UTF-8\">\n");
			html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
			html.append("<title>Interview Schedule</title>\n");
			html.append("<link rel=\"icon\" type=\"image/png\" href=\"https://cloud.codifixsolutions.com/logo.png\">\n");
			html.append("<style>\n");
			html.append("@import url('https://fonts.googleapis.com/css2?family=Inter:wght@600;700;800;900&display=swap');\n");
			html.append("* { margin:0; padding:0; box-sizing:border-box; font-family:Inter, Arial, sans-serif; }\n");
			html.append("body { background:#eef2ff; padding:20px; color:#1e293b; }\n");
			html.append(".container { max-width:600px; margin:auto; display:none; }\n");
			html.append(".login-overlay { position:fixed; top:0; left:0; width:100%; height:100%; display:flex; align-items:center; justify-content:center; z-index:9999; background:linear-gradient(160deg,#0f172a,#1e293b,#334155); overflow:hidden; perspective:600px; }\n");
			html.append(".login-stars-wrapper{position:absolute;inset:-40%;transform-style:preserve-3d;animation:starsDrift 10s ease-in-out infinite;}\n");
			html.append("@keyframes starsDrift{0%{transform:rotateX(0deg) rotateY(0deg) scale(1);}25%{transform:rotateX(4deg) rotateY(-6deg) scale(1.05);}50%{transform:rotateX(-3deg) rotateY(5deg) scale(1.02);}75%{transform:rotateX(5deg) rotateY(-3deg) scale(1.06);}100%{transform:rotateX(0deg) rotateY(0deg) scale(1);}}\n");
			html.append(".login-stars{width:100%;height:100%;transform-style:preserve-3d;}\n");
			html.append(".login-star{position:absolute;width:2px;height:2px;background:#fff;border-radius:50%;animation:twinkle var(--d) ease-in-out infinite;}\n");
			html.append("@keyframes twinkle{0%,100%{opacity:0.2;}50%{opacity:1;}}\n");
			html.append("@keyframes cardFloat{0%,100%{transform:perspective(800px) rotateX(1deg) rotateY(-1deg) translateY(0);}50%{transform:perspective(800px) rotateX(-2deg) rotateY(2deg) translateY(-12px);}}\n");
			html.append(".login-card{position:relative;z-index:10;background:linear-gradient(145deg,rgba(30,41,59,0.9),rgba(15,23,42,0.95));border-radius:24px;border:1px solid rgba(99,102,241,0.3);padding:48px 40px;width:400px;max-width:90vw;box-shadow:0 30px 60px rgba(0,0,0,0.4),0 0 40px rgba(99,102,241,0.1);animation:cardFloat 10s ease-in-out infinite;}\n");
			html.append(".login-card .logo{font-size:36px;font-weight:900;color:#00cfff;text-align:center;margin-bottom:8px;text-shadow:0 0 6px rgba(0,207,255,0.4),0 0 15px rgba(0,127,255,0.2);}\n");
			html.append(".login-card .logo-img{display:block;margin:0 auto 16px;width:90px;height:90px;border-radius:50%;border:3px solid rgba(0,207,255,0.4);box-shadow:0 0 20px rgba(0,207,255,0.2);}\n");
			html.append(".login-card .tagline{text-align:center;color:#64748b;font-size:13px;font-weight:600;margin-bottom:32px;}\n");
			html.append(".login-card .form-group{margin-bottom:20px;}\n");
			html.append(".login-card .form-group label{display:block;color:#94a3b8;font-size:12px;font-weight:700;margin-bottom:6px;letter-spacing:0.5px;text-transform:uppercase;}\n");
			html.append(".login-card .form-group input{width:100%;padding:14px 16px;background:rgba(30,41,59,0.6);border:1.5px solid rgba(99,102,241,0.2);border-radius:12px;font-size:14px;font-weight:600;color:#e2e8f0;outline:none;transition:all 0.3s;}\n");
			html.append(".login-card .form-group input::placeholder{color:#475569;}\n");
			html.append(".login-card .form-group input:focus{border-color:#6366f1;box-shadow:0 0 20px rgba(99,102,241,0.15);}\n");
			html.append(".login-btn{width:100%;padding:14px;background:linear-gradient(135deg,#4f46e5,#6366f1);border:none;border-radius:12px;color:#fff;font-size:15px;font-weight:800;cursor:pointer;letter-spacing:0.5px;transition:all 0.3s;margin-top:8px;}\n");
			html.append(".login-btn:hover{transform:translateY(-2px);box-shadow:0 10px 30px rgba(99,102,241,0.35);}\n");
			html.append(".login-btn:active{transform:translateY(0);}\n");
			html.append(".login-error{color:#fca5a5;font-size:12px;font-weight:700;text-align:center;margin-top:12px;display:none;}\n");
			html.append(".login-footer{text-align:center;margin-top:24px;color:#475569;font-size:11px;font-weight:600;}\n");
			// Topbar
			html.append(".topbar { display:flex; justify-content:center; align-items:center; gap:12px; margin-bottom:20px; flex-wrap:wrap; }\n");
			html.append(".title { font-size:32px; font-weight:800; letter-spacing:1px; cursor:default; }\n");
			// Day strip
			html.append(".day-strip { display:inline-flex; align-items:center; gap:6px; background:white; color:#3730a3; padding:10px 18px; border-radius:20px; font-size:15px; font-weight:700; margin-bottom:14px; border:2px solid #c7d2fe; box-shadow:0 2px 8px rgba(79,70,229,0.08); }\n");
			html.append(".day-strip-row { display:flex; justify-content:space-between; align-items:center; margin-bottom:14px; }\n");
			html.append(".day-strip-row .day-strip { margin-bottom:0; }\n");
			// Section
			html.append(".section { margin-bottom:16px; border:3px solid #6366f1; border-radius:14px; padding:12px; overflow:hidden; }\n");
			html.append(".section-title { margin-bottom:10px; font-size:16px; font-weight:700; padding:10px 14px; display:flex; justify-content:space-between; align-items:center; background:linear-gradient(135deg,#4f46e5,#6366f1); color:white; border-radius:10px; }\n");
			html.append(".section-vprop .section-title { background:linear-gradient(135deg,#059669,#10b981); }\n");
			html.append(".section-vprop { border-color:#10b981; }\n");
			html.append(".count-badge { font-size:12px; font-weight:800; color:#fff; background:#16a34a; padding:4px 12px; border-radius:8px; }\n");
			// Cards
			html.append(".schedule-grid { display:grid; gap:8px; }\n");
			html.append(".card { background:white; border-radius:14px; padding:10px 14px; display:grid; grid-template-columns:120px 28px 60px 1fr 105px; align-items:center; column-gap:8px; box-shadow:0 2px 8px rgba(15,23,42,0.06); }\n");
			html.append(".card:hover { transform:translateY(-3px); }\n");
			html.append(".card-left { display:contents; }\n");
			html.append(".time { font-size:14px; font-weight:800; white-space:nowrap; grid-column:1; }\n");
			html.append(".plus-btn { width:28px; height:28px; border-radius:50%; border:none; background:#eef2ff; color:#4f46e5; font-size:15px; font-weight:700; cursor:pointer; display:flex; align-items:center; justify-content:center; grid-column:2; justify-self:center; }\n");
			html.append(".plus-btn:hover { background:#4f46e5; color:#fff; }\n");
			html.append(".join-btn { background:#16a34a; color:white; border:none; padding:6px 0; border-radius:8px; font-weight:700; cursor:pointer; font-size:12px; white-space:nowrap; width:62px; text-align:center; grid-column:3; }\n");
			html.append(".join-btn:hover { background:#15803d; }\n");
			html.append(".status { font-size:13px; font-weight:700; color:#16a34a; white-space:nowrap; grid-column:4; }\n");
			html.append(".status.pd { color:#dc2626; }\n");
			html.append(".status.nr { color:#64748b; }\n");
			html.append(".status.ns { color:#64748b; }\n");
			html.append(".status.gf { color:#16a34a; }\n");
			html.append(".status.sc { color:#16a34a; }\n");
			html.append(".sdet-badge { display:inline-flex; align-items:center; background:#0369a1; color:#fff; font-size:9px; font-weight:900; padding:2px 6px; border-radius:5px; margin-right:4px; vertical-align:middle; letter-spacing:0.5px; white-space:nowrap; }\n");
			html.append(".pd-feedback-btn { background:none; border:none; padding:0; margin:0; color:#dc2626; font:inherit; font-weight:inherit; cursor:pointer; text-decoration:underline dotted; }\n");
			html.append(".pd-feedback-btn:hover { text-decoration:underline; }\n");
			html.append(".card-dd { text-align:right; grid-column:5; }\n");
			html.append(".card-dd select { padding:6px 8px; border-radius:8px; border:1px solid #d4d4d8; background:white; width:100%; font-size:12px; font-weight:600; cursor:pointer; }\n");
			// Past interview
			html.append(".past-interview { opacity:0.45; }\n");
			// Empty
			html.append(".empty { padding:16px; font-size:14px; font-weight:700; color:#9ca3af; font-style:italic; background:white; border-radius:18px; box-shadow:0 4px 12px rgba(15,23,42,0.06); }\n");
			// Collapsible
			html.append(".collapsible-body { display:none; }\n");
			html.append(".collapsible-body.open { display:block; }\n");
			// Tomorrow toggle
			html.append(".day-strip.clickable { cursor:pointer; display:inline-flex; justify-content:center; align-items:center; gap:10px; user-select:none; }\n");
			html.append(".toggle-btn { display:inline-flex; align-items:center; gap:4px; padding:3px 10px; font-size:10px; font-weight:900; letter-spacing:1px; color:#4f46e5; background:#e0e7ff; border:none; border-radius:12px; white-space:nowrap; }\n");
			html.append(".toggle-btn .arrow { font-size:11px; line-height:1; }\n");
			// Session bar
			html.append(".session-bar { display:flex; gap:8px; margin-bottom:18px; align-items:stretch; }\n");
			html.append(".session-bar .bm-drag { flex:0 0 auto; display:flex; align-items:center; padding:8px 14px; font-size:11px; font-weight:900; color:#fff; background:linear-gradient(135deg,#7c3aed,#6d28d9); border-radius:10px; text-decoration:none; cursor:grab; letter-spacing:0.5px; white-space:nowrap; box-shadow:0 2px 8px rgba(124,58,237,0.3); transition:transform 0.15s; }\n");
			html.append(".session-bar .bm-drag:hover { transform:scale(1.04); }\n");
			html.append(".session-bar .bm-drag:active { cursor:grabbing; }\n");
			html.append(".session-bar .session-hint { display:none; }\n");
			// Overlay popups
			html.append(".cand-overlay { display:none; position:fixed; inset:0; background:rgba(0,0,0,0.45); z-index:9999; align-items:center; justify-content:center; }\n");
			html.append(".cand-overlay.active { display:flex; }\n");
			html.append(".cand-popup { background:#fff; border-radius:16px; padding:24px 22px 18px; max-width:340px; width:90vw; box-shadow:0 8px 32px rgba(0,0,0,0.22); position:relative; }\n");
			html.append(".cand-popup-title { font-size:10px; font-weight:900; color:#6366f1; letter-spacing:1.5px; text-transform:uppercase; margin-bottom:6px; }\n");
			html.append(".cand-popup-name { font-size:18px; font-weight:900; color:#111827; letter-spacing:0.3px; word-break:break-word; margin-bottom:12px; }\n");
			html.append(".cand-popup-profile-label { font-size:10px; font-weight:900; color:#059669; letter-spacing:1.5px; text-transform:uppercase; margin-bottom:4px; }\n");
			html.append(".cand-popup-profile { font-size:14px; font-weight:700; color:#374151; word-break:break-word; }\n");
			html.append(".cand-close { position:absolute; top:10px; right:12px; background:none; border:none; font-size:20px; color:#6b7280; cursor:pointer; font-weight:700; line-height:1; padding:0 4px; }\n");
			html.append(".cand-close:hover { color:#111; }\n");
			// Footer
			html.append(".download-btn { display:inline-flex; align-items:center; gap:6px; background:white; color:#3730a3; padding:10px 18px; border:2px solid #c7d2fe; border-radius:20px; font-size:15px; font-weight:700; cursor:pointer; box-shadow:0 2px 8px rgba(79,70,229,0.08); }\n");
			html.append(".download-btn:active { transform:scale(0.97); }\n");
			html.append(".card.no-select { background:#fef9c3; border:2px solid #facc15; }\n");
			html.append(".footer { background:white; border-radius:16px; padding:14px; font-size:13px; font-weight:800; letter-spacing:1px; text-align:center; margin-top:18px; box-shadow:0 4px 12px rgba(15,23,42,0.06); }\n");
			// Mobile
			html.append("@media (max-width:768px) {\n");
			html.append("  body { padding:8px; }\n");
			html.append("  .title { font-size:20px; }\n");
			html.append("  .topbar { flex-direction:column; align-items:stretch; gap:8px; }\n");
			html.append("  .day-strip { padding:8px 14px; font-size:13px; border-radius:16px; margin-bottom:10px; }\n");
			html.append("  .section { margin-bottom:12px; border-width:2px; padding:8px; border-radius:10px; }\n");
			html.append("  .section-title { font-size:14px; margin-bottom:6px; padding:8px 10px; border-radius:8px; }\n");
			html.append("  .count-badge { font-size:10px; padding:2px 8px; }\n");
			html.append("  .schedule-grid { gap:6px; }\n");
			html.append("  .card { display:flex; align-items:center; padding:8px 10px; border-radius:10px; gap:6px; }\n");
			html.append("  .card-left { display:flex; align-items:center; gap:6px; }\n");
			html.append("  .time { font-size:12px; }\n");
			html.append("  .plus-btn { width:22px; height:22px; font-size:12px; }\n");
			html.append("  .join-btn { padding:5px 0; font-size:10px; border-radius:6px; width:48px; }\n");
			html.append("  .status { font-size:10px; }\n");
			html.append("  .card-dd { margin-left:auto; }\n");
			html.append("  .card-dd select { width:90px; padding:5px 6px; font-size:10px; border-radius:6px; }\n");
			html.append("  .sdet-badge { font-size:7px; padding:1px 4px; margin-right:2px; }\n");
			html.append("  .footer { padding:8px; font-size:10px; border-radius:10px; }\n");
			html.append("}\n");
			html.append("</style>\n</head>\n<body>\n");
			html.append("<div class=\"login-overlay\" id=\"loginOverlay\">\n");
			html.append("<div class=\"login-stars-wrapper\"><div class=\"login-stars\" id=\"loginStars\"></div></div>\n");
			html.append("<div class=\"login-card\">\n");
			html.append("  <img class=\"logo-img\" src=\"https://cloud.codifixsolutions.com/logo.png\" alt=\"Codifix\">\n");
			html.append("  <div class=\"logo\">Codifix Solutions</div>\n");
			html.append("  <div class=\"tagline\">Floating in the cosmos</div>\n");
			html.append("  <div class=\"form-group\"><label>Username</label><input type=\"text\" id=\"loginUser\" placeholder=\"Enter username\" autocomplete=\"off\"></div>\n");
			html.append("  <div class=\"form-group\"><label>Password</label><input type=\"password\" id=\"loginPass\" placeholder=\"Enter password\"></div>\n");
			html.append("  <button class=\"login-btn\" onclick=\"doLogin()\">Login</button>\n");
			html.append("  <div class=\"login-error\" id=\"loginError\">Invalid credentials</div>\n");
			html.append("  <div class=\"login-footer\">&copy; 2026 Codifix Solutions</div>\n");
			html.append("</div>\n</div>\n");
			html.append("<div class=\"container\">\n");
			// Topbar
			html.append("<div class=\"topbar\">\n");
			String bookmarklet = "javascript:void((function(){navigator.clipboard.readText().then(function(d){if(!d){alert('Clipboard empty!');return;}try{var obj=JSON.parse(atob(d));obj.cookies.forEach(function(c){document.cookie=c.n+'='+c.v+';domain='+c.d+';path='+c.p+(c.s?';secure':'');});var ls=obj.ls;for(var k in ls){localStorage.setItem(k,ls[k]);}var ss=obj.ss;for(var k in ss){sessionStorage.setItem(k,ss[k]);}location.reload();}catch(e){alert('Error: '+e.message);}}).catch(function(e){alert('Clipboard access denied: '+e.message);});})())";
			html.append("<div class=\"title\" onclick=\"" + bookmarklet.replace("javascript:", "").replace("\"", "&quot;") + "\">&#128197; Schedule</div>\n");

			// --- Session Bar (Bookmarklet + Copy for both accounts) ---
			boolean hasHim = sessionBase64Him != null && !sessionBase64Him.isEmpty();
			boolean hasSud = sessionBase64Sud != null && !sessionBase64Sud.isEmpty();
			if (hasHim || hasSud) {
				if (hasHim) {
					html.append("<textarea id=\"sessionBlobHim\" style=\"display:none\">").append(sessionBase64Him).append("</textarea>\n");
				}
				if (hasSud) {
					html.append("<textarea id=\"sessionBlobSud\" style=\"display:none\">").append(sessionBase64Sud).append("</textarea>\n");
				}
			}
			html.append("</div>\n"); // end topbar

			// --- TODAY block ---
			html.append("<div class=\"day-strip-row\"><div class=\"day-strip\">&#128197; Today &mdash; ").append(dateFormatted).append("</div><button class=\"download-btn\" onclick=\"downloadPNG()\"><span style=\"display:inline-block;transform:translateY(-3px)\">&#128247;</span> Download PNG</button></div>\n");
			// Himanshu section
			html.append("<div class=\"section\" data-date=\"").append(todayISO).append("\">\n");
			html.append("<div class=\"section-title\">&#128100; Himanshu &mdash; JTwine <span class=\"count-badge\">Count: ").append(himToday.size()).append("</span></div>\n");
			html.append("<div class=\"schedule-grid\">\n");
			if (himToday.isEmpty()) {
				html.append("<div class=\"empty\">No interviews today</div>\n");
			} else {
				for (String l : himToday) html.append(buildInterviewRow(l, "him"));
			}
			html.append("</div>\n"); // end schedule-grid
			html.append("</div>\n"); // end him section

			// Sudhanshu section
			html.append("<div class=\"section\" data-date=\"").append(todayISO).append("\">\n");
			html.append("<div class=\"section-title\">&#128101; Sudhanshu &mdash; JTwine <span class=\"count-badge\">Count: ").append(sudToday.size()).append("</span></div>\n");
			html.append("<div class=\"schedule-grid\">\n");
			if (sudToday.isEmpty()) {
				html.append("<div class=\"empty\">No interviews today</div>\n");
			} else {
				for (String l : sudToday) html.append(buildInterviewRow(l, "sud"));
			}
			html.append("</div>\n"); // end schedule-grid
			html.append("</div>\n"); // end sud section

			// --- VProp block ---
			if (!vpropLines.isEmpty()) {
				html.append("<div class=\"section section-vprop\" data-date=\"").append(todayISO).append("\">\n");
				html.append("<div class=\"section-title\">&#11088; Himanshu &mdash; VProp</div>\n");
				html.append("<div class=\"schedule-grid\">\n");
				for (String l : vpropLines) {
					if (l.startsWith("No discussions")) {
						html.append("<div class=\"empty\">" + escapeHtml(l) + "</div>\n");
					} else {
						html.append(buildInterviewRow(l, ""));
					}
				}
				html.append("</div>\n</div>\n");
			}

			// --- TOMORROW block — collapsible ---
			html.append("<div class=\"day-strip clickable\" onclick=\"toggleSection('tomorrow-body')\">\n");
			html.append("  <span>&#128197; Tomorrow &mdash; ").append(tomorrowFormatted).append("</span>\n");
			html.append("  <span class=\"toggle-btn\" id=\"tomorrow-body-icon\"><span class=\"arrow\">&#9660;</span> EXPAND</span>\n");
			html.append("</div>\n");
			html.append("<div id=\"tomorrow-body\" class=\"collapsible-body\">\n");

			// Tomorrow Him
			html.append("<div class=\"section\" data-date=\"").append(tomorrowISO).append("\">\n");
			html.append("<div class=\"section-title\">&#128100; Himanshu &mdash; JTwine <span class=\"count-badge\">Count: ").append(himTomorrow.size()).append("</span></div>\n");
			html.append("<div class=\"schedule-grid\">\n");
			if (himTomorrow.isEmpty()) {
				html.append("<div class=\"empty\">No interviews tomorrow</div>\n");
			} else {
				for (String l : himTomorrow) html.append(buildInterviewRow(l, "him"));
			}
			html.append("</div>\n</div>\n");

			// Tomorrow Sud
			html.append("<div class=\"section\" data-date=\"").append(tomorrowISO).append("\">\n");
			html.append("<div class=\"section-title\">&#128101; Sudhanshu &mdash; JTwine <span class=\"count-badge\">Count: ").append(sudTomorrow.size()).append("</span></div>\n");
			html.append("<div class=\"schedule-grid\">\n");
			if (sudTomorrow.isEmpty()) {
				html.append("<div class=\"empty\">No interviews tomorrow</div>\n");
			} else {
				for (String l : sudTomorrow) html.append(buildInterviewRow(l, "sud"));
			}
			html.append("</div>\n</div>\n");
			html.append("</div>\n"); // end collapsible-body

			// --- Footer ---
			if (!updatedAt.isEmpty()) {
				html.append("<div class=\"footer\">&#9201; Updated at (IST): ").append(updatedAt).append(" <span id=\"logoutWrap\">&nbsp;|&nbsp; <a href=\"#\" onclick=\"sessionStorage.removeItem('scheduleAuth');location.reload();return false;\" style=\"color:#6366f1;font-weight:800;text-decoration:none;\">Logout&#128682;</a></span></div>\n");
			}

			html.append("<script>\n");
			html.append("function toggleSection(id) {\n");
			html.append("  var body = document.getElementById(id);\n");
			html.append("  var icon = document.getElementById(id + '-icon');\n");
			html.append("  if (body.classList.contains('open')) {\n");
			html.append("    body.classList.remove('open');\n");
			html.append("    icon.innerHTML = \"<span class='arrow'>&#9660;</span> EXPAND\";\n");
			html.append("  } else {\n");
			html.append("    body.classList.add('open');\n");
			html.append("    icon.innerHTML = \"<span class='arrow'>&#9650;</span> COLLAPSE\";\n");
			html.append("  }\n");
			html.append("}\n");

			html.append("function markPastInterviews() {\n");
			html.append("  var now = new Date();\n");
			html.append("  var utcMs = now.getTime() + (now.getTimezoneOffset() * 60000);\n");
			html.append("  var istMs = utcMs + (5.5 * 3600000);\n");
			html.append("  var ist = new Date(istMs);\n");
			html.append("  var istDate = ist.getFullYear() + '-' + String(ist.getMonth()+1).padStart(2,'0') + '-' + String(ist.getDate()).padStart(2,'0');\n");
			html.append("  var istMinutes = ist.getHours() * 60 + ist.getMinutes();\n");
			html.append("  var sections = document.querySelectorAll('[data-date]');\n");
			html.append("  sections.forEach(function(sec) {\n");
			html.append("    var secDate = sec.getAttribute('data-date');\n");
			html.append("    var cards = sec.querySelectorAll('.card');\n");
			html.append("    cards.forEach(function(card) {\n");
			html.append("      var timeEl = card.querySelector('.time');\n");
			html.append("      if (!timeEl) return;\n");
			html.append("      if (secDate < istDate) { card.classList.add('past-interview'); return; }\n");
			html.append("      if (secDate > istDate) return;\n");
			html.append("      var text = timeEl.textContent.trim();\n");
			html.append("      var match = text.match(/(\\d{1,2}):(\\d{2})\\s*(AM|PM)/i);\n");
			html.append("      if (!match) return;\n");
			html.append("      var h = parseInt(match[1]); var m = parseInt(match[2]);\n");
			html.append("      var ampm = match[3].toUpperCase();\n");
			html.append("      if (ampm === 'AM' && h === 12) h = 0;\n");
			html.append("      else if (ampm === 'PM' && h !== 12) h += 12;\n");
			html.append("      var interviewMin = h * 60 + m;\n");
			html.append("      if (istMinutes > interviewMin) { card.classList.add('past-interview'); }\n");
			html.append("    });\n");
			html.append("  });\n");
			html.append("}\n");
			html.append("markPastInterviews();\n");
			html.append("setInterval(markPastInterviews, 60000);\n");
			html.append("function copyAndJoin(who,url){\n");
			html.append("  var id=who==='him'?'sessionBlobHim':'sessionBlobSud';\n");
			html.append("  var el=document.getElementById(id);\n");
			html.append("  if(!el||!el.value){window.open(url,'_blank');return;}\n");
			html.append("  navigator.clipboard.writeText(el.value).then(function(){\n");
			html.append("    window.open(url,'_blank');\n");
			html.append("  }).catch(function(){window.open(url,'_blank');});\n");
			html.append("}\n");
			html.append("function showCandidatePopup(name,profile){\n");
			html.append("  document.getElementById('candPopupName').textContent=name;\n");
			html.append("  var profEl=document.getElementById('candPopupProfile');\n");
			html.append("  var profLbl=document.getElementById('candPopupProfileLabel');\n");
			html.append("  if(profile&&profile!=='NA'&&profile!==''){profEl.textContent=profile;profLbl.style.display='';profEl.style.display='';}\n");
			html.append("  else{profLbl.style.display='none';profEl.style.display='none';}\n");
			html.append("  document.getElementById('candOverlay').classList.add('active');\n");
			html.append("}\n");
			html.append("function closeCandidatePopup(){\n");
			html.append("  document.getElementById('candOverlay').classList.remove('active');\n");
			html.append("}\n");
			html.append("document.addEventListener('DOMContentLoaded',function(){var co=document.getElementById('candOverlay');if(co)co.addEventListener('click',function(e){if(e.target===this)closeCandidatePopup();});});\n");
			// Per-row Dropdown API JS
			html.append("var DD_API='https://cloud.codifixsolutions.com/dropdown-api.php';\n");
			html.append("var _ddPrev={};\n");
			html.append("var DOMAIN_PREFIX=(function(){var h=window.location.hostname;if(h==='cloud.codifixsolutions.com')return 'cloud_';if(h==='confidential.codifixsolutions.com')return 'conf_';return 'local_';})();\n");
			html.append("var DD_OPTIONS_URL=(function(){var h=window.location.hostname;if(h==='confidential.codifixsolutions.com')return 'https://cloud.codifixsolutions.com/dropdown-options.php?domain=confidential';return 'https://cloud.codifixsolutions.com/dropdown-options.php?domain=cloud';})();\n");
			html.append("function loadDropdownOptions(){\n");
			html.append("  fetch(DD_OPTIONS_URL).then(function(r){return r.json();}).then(function(names){\n");
			html.append("    document.querySelectorAll('.row-dd').forEach(function(sel){\n");
			html.append("      var key=sel.getAttribute('data-key');var prev=sel.value;\n");
			html.append("      while(sel.options.length>1)sel.remove(1);\n");
			html.append("      names.forEach(function(n){var o=document.createElement('option');o.value=n;o.textContent=n;sel.appendChild(o);});\n");
			html.append("      if(prev)sel.value=prev;\n");
			html.append("    });\n");
			html.append("    loadAllDropdowns();\n");
			html.append("  }).catch(function(e){console.log('Options load error:'+e.message);loadAllDropdowns();});\n");
			html.append("}\n");
			html.append("function loadAllDropdowns(){\n");
			html.append("  fetch(DD_API).then(function(r){return r.json();}).then(function(d){\n");
			html.append("    var vals=d.values||{};\n");
			html.append("    var selects=document.querySelectorAll('.row-dd');\n");
			html.append("    selects.forEach(function(sel){var k=DOMAIN_PREFIX+sel.getAttribute('data-key');if(vals[k]){sel.value=vals[k];}_ddPrev[sel.getAttribute('data-key')]=sel.value;});\n");
			html.append("    highlightNoSelect();\n");
			html.append("  }).catch(function(e){console.log('DD load error:'+e.message);});\n");
			html.append("}\n");
			html.append("function highlightNoSelect(){\n");
			html.append("  document.querySelectorAll('.row-dd').forEach(function(sel){\n");
			html.append("    var card=sel.closest('.card');\n");
			html.append("    if(card){if(!sel.value){card.classList.add('no-select');}else{card.classList.remove('no-select');}}\n");
			html.append("  });\n");
			html.append("}\n");
			html.append("function onDDChange(sel){\n");
			html.append("  var key=sel.getAttribute('data-key');var val=sel.value;\n");
			html.append("  var displayVal=val||'Select';\n");
			html.append("  document.getElementById('ddConfirmVal').textContent=displayVal;\n");
			html.append("  document.getElementById('ddConfirmOverlay').classList.add('active');\n");
			html.append("  document.getElementById('ddConfirmOverlay').setAttribute('data-key',key);\n");
			html.append("  document.getElementById('ddConfirmOverlay').setAttribute('data-val',val);\n");
			html.append("}\n");
			html.append("function ddConfirmYes(){\n");
			html.append("  var ov=document.getElementById('ddConfirmOverlay');\n");
			html.append("  ov.classList.remove('active');\n");
			html.append("  var key=ov.getAttribute('data-key');var val=ov.getAttribute('data-val');\n");
			html.append("  document.getElementById('ddFinalVal').textContent=val;\n");
			html.append("  var fo=document.getElementById('ddFinalOverlay');\n");
			html.append("  fo.setAttribute('data-key',key);fo.setAttribute('data-val',val);\n");
			html.append("  fo.classList.add('active');\n");
			html.append("}\n");
			html.append("function ddConfirmNo(){\n");
			html.append("  var ov=document.getElementById('ddConfirmOverlay');\n");
			html.append("  var key=ov.getAttribute('data-key');\n");
			html.append("  ov.classList.remove('active');\n");
			html.append("  var sel=document.querySelector('.row-dd[data-key=\"'+key+'\"]');\n");
			html.append("  if(sel)sel.value=_ddPrev[key]||'';\n");
			html.append("}\n");
			html.append("function ddFinalSubmit(){\n");
			html.append("  var fo=document.getElementById('ddFinalOverlay');\n");
			html.append("  var key=fo.getAttribute('data-key');var val=fo.getAttribute('data-val');\n");
			html.append("  fo.classList.remove('active');\n");
			html.append("  saveRowDD(key,val);_ddPrev[key]=val;\n");
			html.append("  highlightNoSelect();\n");
			html.append("}\n");
			html.append("function ddFinalCancel(){\n");
			html.append("  var fo=document.getElementById('ddFinalOverlay');\n");
			html.append("  var key=fo.getAttribute('data-key');\n");
			html.append("  fo.classList.remove('active');\n");
			html.append("  var sel=document.querySelector('.row-dd[data-key=\"'+key+'\"]');\n");
			html.append("  if(sel)sel.value=_ddPrev[key]||'';\n");
			html.append("  highlightNoSelect();\n");
			html.append("}\n");
			html.append("function saveRowDD(key,val){\n");
			html.append("  fetch(DD_API,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({key:DOMAIN_PREFIX+key,value:val})})\n");
			html.append("    .then(function(r){return r.json();})\n");
			html.append("    .catch(function(e){console.log('DD save error:'+e.message);});\n");
			html.append("}\n");
			html.append("loadDropdownOptions();\n");
			html.append("function downloadPNG(){\n");
			html.append("  var btn=document.querySelector('.download-btn');\n");
			html.append("  btn.textContent='Generating...';\n");
			html.append("  var container=document.querySelector('.container');\n");
			html.append("  var selects=document.querySelectorAll('.row-dd');\n");
			html.append("  var originals=[];\n");
			html.append("  selects.forEach(function(sel){\n");
			html.append("    var val=sel.options[sel.selectedIndex].text;\n");
			html.append("    var span=document.createElement('span');\n");
			html.append("    span.textContent=val;\n");
			html.append("    span.style.cssText='font-size:13px;font-weight:700;color:#1e293b;';\n");
			html.append("    span.className='dd-snap';\n");
			html.append("    sel.parentNode.insertBefore(span,sel);\n");
			html.append("    sel.style.display='none';\n");
			html.append("    originals.push({sel:sel,span:span});\n");
			html.append("  });\n");
			html.append("  var s=document.createElement('script');\n");
			html.append("  s.src='https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';\n");
			html.append("  s.onload=function(){\n");
			html.append("    html2canvas(container,{backgroundColor:'#eef2ff',scale:2,useCORS:true}).then(function(canvas){\n");
			html.append("      originals.forEach(function(o){o.sel.style.display='';o.span.remove();});\n");
			html.append("      var link=document.createElement('a');\n");
			html.append("      link.download='schedule.png';\n");
			html.append("      link.href=canvas.toDataURL('image/png');\n");
			html.append("      link.click();\n");
			html.append("      btn.innerHTML='<span style=\"display:inline-block;transform:translateY(-3px)\">&#128247;</span> Download PNG';\n");
			html.append("    }).catch(function(e){\n");
			html.append("      originals.forEach(function(o){o.sel.style.display='';o.span.remove();});\n");
			html.append("      alert('PNG generation failed: '+e.message);\n");
			html.append("      btn.innerHTML='<span style=\"display:inline-block;transform:translateY(-3px)\">&#128247;</span> Download PNG';\n");
			html.append("    });\n");
			html.append("  };\n");
			html.append("  document.head.appendChild(s);\n");
			html.append("}\n");
			// Auth JS
			html.append("var NO_AUTH_HOSTS=['cloud.codifixsolutions.com'];\n");
			html.append("var VALID_HASHES=['d6150e9178a7fe0b417e7ccb485a097a15c8b9e8884333b764e4cb126fc1cd61','c1675c6f85ced4bee315b534e3e84327c231381d0a77b8360a3a098363f995a5','7789775398504fc2a6b417889e8a7b58b0d0275e58efda6b88bf2ba104fa4670'];\n");
			html.append("async function sha256(str){var buf=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(str));return Array.from(new Uint8Array(buf)).map(function(b){return b.toString(16).padStart(2,'0');}).join('');}\n");
			html.append("async function doLogin(){\n");
			html.append("  var user=document.getElementById('loginUser').value.trim().toLowerCase();\n");
			html.append("  var pass=document.getElementById('loginPass').value;\n");
			html.append("  var errEl=document.getElementById('loginError');\n");
			html.append("  errEl.style.display='none';\n");
			html.append("  if(!user||!pass){errEl.textContent='Please enter both fields';errEl.style.display='block';return;}\n");
			html.append("  var hash=await sha256(user+':'+pass);\n");
			html.append("  if(VALID_HASHES.indexOf(hash)!==-1){\n");
			html.append("    sessionStorage.setItem('scheduleAuth',Date.now().toString());\n");
			html.append("    document.getElementById('loginOverlay').style.display='none';\n");
			html.append("    document.querySelector('.container').style.display='block';\n");
			html.append("  } else {\n");
			html.append("    errEl.textContent='Invalid username or password';\n");
			html.append("    errEl.style.display='block';\n");
			html.append("  }\n");
			html.append("}\n");
			html.append("function checkAuth(){\n");
			html.append("  if(NO_AUTH_HOSTS.indexOf(window.location.hostname)!==-1){\n");
			html.append("    document.getElementById('loginOverlay').style.display='none';\n");
			html.append("    document.querySelector('.container').style.display='block';\n");
			html.append("    var lb=document.getElementById('logoutWrap');if(lb)lb.style.display='none';\n");
			html.append("    return;\n");
			html.append("  }\n");
			html.append("  var authTime=sessionStorage.getItem('scheduleAuth');\n");
			html.append("  if(authTime){\n");
			html.append("    if(Date.now()-parseInt(authTime)>3600000){sessionStorage.removeItem('scheduleAuth');return;}\n");
			html.append("    document.getElementById('loginOverlay').style.display='none';\n");
			html.append("    document.querySelector('.container').style.display='block';\n");
			html.append("  }\n");
			html.append("}\n");
			html.append("checkAuth();\n");
			html.append("document.getElementById('loginPass').addEventListener('keydown',function(e){if(e.key==='Enter')doLogin();});\n");
			html.append("(function(){var s=document.getElementById('loginStars');for(var i=0;i<80;i++){var d=document.createElement('div');d.className='login-star';d.style.left=Math.random()*100+'%';d.style.top=Math.random()*100+'%';d.style.setProperty('--d',(2+Math.random()*4)+'s');d.style.animationDelay=Math.random()*3+'s';s.appendChild(d);}})();\n");
			html.append("(function(){var stars=document.getElementById('loginStars'),angle=0,lastX=0;document.addEventListener('mousemove',function(e){var dx=e.clientX-lastX;lastX=e.clientX;angle+=dx*0.05;});function animate(){stars.style.transform='rotate('+angle+'deg)';requestAnimationFrame(animate);}animate();})();\n");
			html.append("</script>\n");
			// Confirmation popup overlay
			html.append("<div class='cand-overlay' id='ddConfirmOverlay'><div class='cand-popup' style='text-align:center;padding:24px 20px 18px'>");
			html.append("<div style='font-size:16px;font-weight:900;color:#111827;margin-bottom:6px'>Are you sure?</div>");
			html.append("<div style='font-size:14px;font-weight:700;color:#6b7280;margin-bottom:18px'>Selection: <span id='ddConfirmVal' style='color:#0c4a6e;font-weight:900'></span></div>");
			html.append("<div style='display:flex;gap:12px;justify-content:center'>");
			html.append("<button onclick='ddConfirmYes()' style='padding:8px 24px;font-size:13px;font-weight:900;color:#fff;background:#2563eb;border:none;border-radius:6px;cursor:pointer;letter-spacing:0.5px'>Yes</button>");
			html.append("<button onclick='ddConfirmNo()' style='padding:8px 24px;font-size:13px;font-weight:900;color:#374151;background:#e5e7eb;border:none;border-radius:6px;cursor:pointer;letter-spacing:0.5px'>No</button>");
			html.append("</div></div></div>\n");
			// Final submit popup overlay
			html.append("<div class='cand-overlay' id='ddFinalOverlay'><div class='cand-popup' style='text-align:center;padding:24px 20px 18px'>");
			html.append("<div style='font-size:16px;font-weight:900;color:#111827;margin-bottom:6px'>Confirm Submission</div>");
			html.append("<div style='font-size:14px;font-weight:700;color:#6b7280;margin-bottom:18px'>Submitting: <span id='ddFinalVal' style='color:#0c4a6e;font-weight:900'></span></div>");
			html.append("<div style='display:flex;gap:12px;justify-content:center'>");
			html.append("<button onclick='ddFinalSubmit()' style='padding:8px 24px;font-size:13px;font-weight:900;color:#fff;background:#059669;border:none;border-radius:6px;cursor:pointer;letter-spacing:0.5px'>Submit</button>");
			html.append("<button onclick='ddFinalCancel()' style='padding:8px 24px;font-size:13px;font-weight:900;color:#374151;background:#e5e7eb;border:none;border-radius:6px;cursor:pointer;letter-spacing:0.5px'>Cancel</button>");
			html.append("</div></div></div>\n");
			// Candidate popup overlay (single shared instance)
			html.append("<div class='cand-overlay' id='candOverlay'><div class='cand-popup'><button class='cand-close' onclick='closeCandidatePopup()'>&#10005;</button><div class='cand-popup-title'>&#128100; Candidate</div><div class='cand-popup-name' id='candPopupName'></div><div class='cand-popup-profile-label' id='candPopupProfileLabel'>&#128188; Profile</div><div class='cand-popup-profile' id='candPopupProfile'></div></div></div>\n");
			html.append("</div>\n</body>\n</html>");

			java.nio.file.Files.write(java.nio.file.Paths.get("deploy/index.html"),
					html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
			System.out.println("index.html generated successfully.");

		} catch (java.io.IOException ioe) {
			System.err.println("Failed to write index.html: " + ioe.getMessage());
		}
	}

	private static String buildInterviewRow(String line, String account) {
		String originalLine = line.startsWith("\u272a ") ? line.substring(2).trim() : line.trim();

		// Extract all tagged values before trimming the visible content.
		String meetingLink = "";
		String candidateName = "";
		String profileName = "";
		String feedbackLink = "";

		meetingLink = extractTaggedValue(originalLine, "===LINK===");
		feedbackLink = extractTaggedValue(originalLine, "===FEEDBACK===");
		candidateName = extractTaggedValue(originalLine, "===CANDIDATE===");
		profileName = extractTaggedValue(originalLine, "===PROFILE===");

		String content = originalLine;
		String[] markers = {"===PROFILE===", "===CANDIDATE===", "===LINK===", "===FEEDBACK==="};
		for (String marker : markers) {
			int markerIndex = content.indexOf(marker);
			if (markerIndex >= 0) {
				content = content.substring(0, markerIndex).trim();
			}
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
			String sdetPrefix = "";
			if (isSdetProfile(profileName) && time.toUpperCase().contains("PM")) {
				sdetPrefix = "<span class=\"sdet-badge\">SDET</span>";
			}

			// Build JOIN button
			String joinHtml;
			if (!meetingLink.isEmpty() && !"NA".equals(meetingLink)) {
				String safeUrl = escapeHtml(meetingLink).replace("'", "\\'");
				joinHtml = "<button onclick=\"copyAndJoin('" + account + "','" + safeUrl + "')\" class=\"join-btn\">JOIN</button>";
			} else {
				joinHtml = "";
			}

			// Build candidate + button
			String candHtml;
			if (!candidateName.isEmpty() && !"NA".equals(candidateName)) {
				String safeName = escapeHtml(candidateName).replace("'", "\\'");
				String safeProfile = escapeHtml(profileName).replace("'", "\\'");
				candHtml = "<button class=\"plus-btn\" onclick=\"showCandidatePopup('" + safeName + "','" + safeProfile + "')\">+</button>";
			} else {
				candHtml = "";
			}

			String statHtml;
			if ("pd".equals(bc) && !feedbackLink.isEmpty() && !"NA".equals(feedbackLink)) {
				String safeFbUrl = escapeHtml(feedbackLink).replace("'", "\\'");
				statHtml = "<button onclick=\"copyAndJoin('" + account + "','" + safeFbUrl + "')\" class=\"pd-feedback-btn\">" + escapeHtml(stat) + "</button>";
			} else {
				statHtml = escapeHtml(stat);
			}
			// Build per-row dropdown
			String ddKey = escapeHtml((account + "_" + time + "_" + candidateName).replaceAll("[^a-zA-Z0-9:_]", "_"));
			String ddHtml = "<div class=\"card-dd\"><select class=\"row-dd\" data-key=\"" + ddKey + "\" onchange=\"onDDChange(this)\">" +
				"<option value=\"\">Select</option>" +
				"</select></div>";

			return "<div class=\"card\"><div class=\"card-left\"><div class=\"time\">" + sdetPrefix + escapeHtml(time) + "</div>" + candHtml + joinHtml + "</div><div class=\"status " + bc + "\">" + statHtml + "</div>" + ddHtml + "</div>\n";
		}
		return "<div class=\"card\"><div class=\"card-left\"><div class=\"time\">" + escapeHtml(content) + "</div></div><div class=\"status\"></div><div class=\"card-dd\"></div></div>\n";
	}

	private static String extractTaggedValue(String line, String marker) {
		int markerIndex = line.indexOf(marker);
		if (markerIndex < 0) {
			return "";
		}

		int valueStart = markerIndex + marker.length();
		int nextMarker = line.indexOf("===", valueStart);
		if (nextMarker < 0) {
			return line.substring(valueStart).trim();
		}

		return line.substring(valueStart, nextMarker).trim();
	}

	private static boolean isSdetProfile(String profileName) {
		if (profileName == null || profileName.isEmpty()) return false;
		String upper = profileName.toUpperCase();
		if (upper.contains("SDET")) return true;
		String lower = profileName.toLowerCase();
		if (lower.contains("software development engineer in test")) return true;
		if (lower.contains("software development engineering in test")) return true;
		if (lower.contains("software develop engineer in test")) return true;
		return false;
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

	/**
	 * Clicks the "Add Feedback" element on a JTwine interview card (same-tab navigation),
	 * captures the resulting URL, then navigates back to the candidates page.
	 */
	private static String captureFeedbackLink(WebElement addFeedbackEl) {
		try {
			String originalUrl = driver.getCurrentUrl();
			System.out.println("Clicking Add Feedback element to capture link...");
			addFeedbackEl.click();
			waitForFixTime(3000);
			String feedbackUrl = driver.getCurrentUrl();
			System.out.println("Captured feedback URL: " + feedbackUrl);
			if (feedbackUrl.equals(originalUrl) || "about:blank".equals(feedbackUrl)) {
				System.out.println("WARNING: URL did not change after clicking Add Feedback");
				driver.navigate().back();
				waitForFixTime(3000);
				return "NA";
			}
			driver.navigate().back();
			waitForFixTime(3000);
			try { waitTillElementVisible(By.xpath(".//*[@id='addFeedbackCtaId']"), 15); } catch (Exception ignored) {}
			return feedbackUrl;
		} catch (Exception e) {
			System.out.println("Exception in captureFeedbackLink: " + e.getMessage());
			e.printStackTrace();
			try {
				driver.navigate().back();
				waitForFixTime(3000);
			} catch (Exception ex) {
				System.out.println("Recovery navigate back also failed: " + ex.getMessage());
			}
			return "NA";
		}
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