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

		for (int index = 0; index < discussionListToday.size(); index++) {
			WebElement discussion = discussionListToday.get(index);
			WebElement discussionStatus = discussionStatusListToday.get(index);
			System.out.println(discussion.getText() + " ==> " + discussionStatus.getText());
			todayLines.add("✪ " + discussion.getText() + " ==> " + discussionStatus.getText());
		}

		String tomorrowLocator = ".//div[@class='sub-sub-heading-1'][contains(text(),'" + tomorrowDate + "')]";
		String tomorrowStatusLocator = tomorrowLocator+"//ancestor::div[contains(@class,'candidate-details-sec')]//div[contains(@class,'btn-chip')]/div";
		List<WebElement> discussionListTomorrow = driver.findElements(By.xpath(tomorrowLocator));
		List<WebElement> discussionStatusListTomorrow = driver.findElements(By.xpath(tomorrowStatusLocator));

		for (int index = 0; index < discussionListTomorrow.size(); index++) {
			WebElement discussion = discussionListTomorrow.get(index);
			WebElement discussionStatus = discussionStatusListTomorrow.get(index);
			System.out.println(discussion.getText() + " ==> " + discussionStatus.getText());
			tomorrowLines.add("✪ " + discussion.getText() + " ==> " + discussionStatus.getText());
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

			String dateUpper     = dateDisplay.toUpperCase();
			String tomorrowUpper = tomorrowDate != null ? tomorrowDate.toUpperCase() : "";

			StringBuilder html = new StringBuilder();
			html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
			html.append("<meta charset=\"UTF-8\">\n");
			html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
			html.append("<title>Interview Schedule</title>\n");
			html.append("<style>\n");
			html.append("* { box-sizing: border-box; font-family: 'Segoe UI', Arial, sans-serif; }\n");
			html.append("body { background: #f0f4f8; margin: 0; padding: 14px; color: #1a1a2e; }\n");
			html.append(".container { max-width: 480px; margin: auto; }\n");
			// Header
			html.append(".header { background: linear-gradient(135deg, #1a1a2e, #16213e); color: #fff; text-align: center; padding: 18px 10px; margin-bottom: 22px; border-radius: 14px; box-shadow: 0 4px 18px rgba(0,0,0,0.22); }\n");
			html.append(".header h1 { font-size: 20px; font-weight: 800; margin: 0; letter-spacing: 2px; }\n");
			html.append(".header p { margin: 5px 0 0; font-size: 12px; color: #93c5fd; }\n");
			// Section
			html.append(".section { margin-bottom: 22px; }\n");
			html.append(".tab-label { display: inline-block; padding: 8px 22px; font-size: 14px; font-weight: 800; border-radius: 10px 10px 0 0; letter-spacing: 1px; }\n");
			html.append(".tab-today { background: #2563eb; color: #fff; }\n");
			html.append(".tab-tomorrow { background: #d97706; color: #fff; }\n");
			html.append(".section-box-today { background: #eff6ff; border: 2px solid #2563eb; border-top: none; border-radius: 0 12px 12px 12px; padding: 12px; }\n");
			html.append(".section-box-tomorrow { background: #fffbeb; border: 2px solid #d97706; border-top: none; border-radius: 0 12px 12px 12px; padding: 12px; }\n");
			// Cards
			html.append(".card { background: #fff; border-radius: 10px; margin-bottom: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }\n");
			html.append(".card:last-child { margin-bottom: 0; }\n");
			html.append(".card-title { padding: 10px 14px; font-weight: 800; font-size: 13px; color: #fff; display: flex; align-items: center; gap: 6px; }\n");
			html.append(".him { background: linear-gradient(135deg, #667eea, #764ba2); }\n");
			html.append(".sud { background: linear-gradient(135deg, #11998e, #38ef7d); color: #065f46; }\n");
			html.append(".vp { background: linear-gradient(135deg, #f7971e, #ffd200); color: #78350f; }\n");
			// Rows
			html.append(".row { display: flex; justify-content: space-between; align-items: center; padding: 9px 14px; border-top: 1px solid #f1f5f9; font-size: 13px; }\n");
			html.append(".time-text { font-weight: 600; color: #374151; }\n");
			html.append(".badge { padding: 3px 11px; border-radius: 20px; font-size: 11px; font-weight: 800; white-space: nowrap; }\n");
			// Status badge colors
			html.append(".sc  { background: #dbeafe; color: #1e40af; }\n");
			html.append(".gf  { background: #dcfce7; color: #166534; }\n");
			html.append(".nr  { background: #fee2e2; color: #991b1b; }\n");
			html.append(".ns  { background: #f3f4f6; color: #6b7280; }\n");
			html.append(".pd  { background: #fef3c7; color: #92400e; }\n");
			html.append(".empty { padding: 12px 14px; border-top: 1px solid #f1f5f9; font-size: 13px; color: #9ca3af; font-style: italic; }\n");
			// Footer
			html.append(".footer { background: #1a1a2e; color: #94a3b8; padding: 12px; font-size: 12px; font-weight: 600; text-align: center; border-radius: 10px; margin-top: 6px; }\n");
			html.append("@media (max-width: 400px) { .header h1 { font-size: 17px; } .row { font-size: 12px; } }\n");
			html.append("</style>\n</head>\n<body>\n");
			html.append("<div class=\"container\">\n");
			html.append("<div class=\"header\"><h1>&#128197; INTERVIEW SCHEDULE</h1><p>" + (dateDisplay.isEmpty() ? "" : dateDisplay) + "</p></div>\n");

			// --- TODAY block (Blue) ---
			html.append("<div class=\"section\">\n");
			html.append("<div class=\"tab-label tab-today\">&#9728;&#65039; TODAY &mdash; ").append(dateUpper).append("</div>\n");
			html.append("<div class=\"section-box-today\">\n");

			html.append("<div class=\"card\"><div class=\"card-title him\">&#128100; Himanshu &mdash; JTwine</div>\n");
			if (himToday.isEmpty()) {
				html.append("<div class=\"empty\">No interviews today</div>\n");
			} else {
				for (String l : himToday) html.append(buildInterviewRow(l));
			}
			html.append("</div>\n");

			html.append("<div class=\"card\"><div class=\"card-title sud\">&#128101; Sudhanshu &mdash; JTwine</div>\n");
			if (sudToday.isEmpty()) {
				html.append("<div class=\"empty\">No interviews today</div>\n");
			} else {
				for (String l : sudToday) html.append(buildInterviewRow(l));
			}
			html.append("</div>\n");

			html.append("</div>\n"); // end section-box-today
			html.append("</div>\n"); // end TODAY section

			// --- TOMORROW block (Amber) ---
			html.append("<div class=\"section\">\n");
			html.append("<div class=\"tab-label tab-tomorrow\">&#127769; TOMORROW &mdash; ").append(tomorrowUpper).append("</div>\n");
			html.append("<div class=\"section-box-tomorrow\">\n");

			html.append("<div class=\"card\"><div class=\"card-title him\">&#128100; Himanshu &mdash; JTwine</div>\n");
			if (himTomorrow.isEmpty()) {
				html.append("<div class=\"empty\">No interviews tomorrow</div>\n");
			} else {
				for (String l : himTomorrow) html.append(buildInterviewRow(l));
			}
			html.append("</div>\n");

			html.append("<div class=\"card\"><div class=\"card-title sud\">&#128101; Sudhanshu &mdash; JTwine</div>\n");
			if (sudTomorrow.isEmpty()) {
				html.append("<div class=\"empty\">No interviews tomorrow</div>\n");
			} else {
				for (String l : sudTomorrow) html.append(buildInterviewRow(l));
			}
			html.append("</div>\n");

			html.append("</div>\n"); // end section-box-tomorrow
			html.append("</div>\n"); // end TOMORROW section

			// --- VProp block (only if data exists) ---
			if (!vpropLines.isEmpty()) {
				html.append("<div class=\"section\">\n");
				html.append("<div class=\"card\"><div class=\"card-title vp\">&#11088; VProp</div>\n");
				for (String l : vpropLines) html.append(buildInterviewRow(l));
				html.append("</div>\n</div>\n");
			}

			// --- Footer ---
			if (!updatedAt.isEmpty()) {
				html.append("<div class=\"footer\">&#9201; Updated at (IST): ").append(updatedAt).append("</div>\n");
			}

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
				case "Strongly Recommended":    bc = "gf"; break;
				case "Pending Feedback Review": bc = "pd"; break;
				default: bc = "ns";
			}
			String[] dtParts = disc.split(", ");
			String time = dtParts.length >= 4 ? dtParts[3] : disc;
			return "<div class=\"row\"><span class=\"time-text\">" + time + "</span><span class=\"badge " + bc + "\">" + stat + "</span></div>\n";
		}
		return "<div class=\"row\"><span class=\"time-text\">" + content + "</span></div>\n";
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
				outputLines.add("\n");
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