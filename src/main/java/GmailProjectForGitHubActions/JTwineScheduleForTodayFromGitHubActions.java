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
			// Extract today's date for the header
			String dateDisplay = "";
			for (String line : outputLines) {
				if (line.startsWith("Today's date:")) {
					dateDisplay = line.replace("Today's date:", "").trim();
					break;
				}
			}

			String dateUpper = dateDisplay.toUpperCase();
			String tomorrowUpper = tomorrowDate != null ? tomorrowDate.toUpperCase() : "";

			StringBuilder html = new StringBuilder();
			html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
			html.append("<meta charset=\"UTF-8\">\n");
			html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
			html.append("<title>Interview Schedule</title>\n");
			html.append("<style>\n");
			html.append("* { box-sizing: border-box; font-family: Arial, sans-serif; }\n");
			html.append("body { background: #f2f2f2; margin: 0; padding: 10px; color: #000; }\n");
			html.append(".container { max-width: 500px; margin: auto; }\n");
			html.append(".header { border: 3px solid #000; background: #fff; text-align: center; padding: 14px 8px; margin-bottom: 12px; }\n");
			html.append(".header h1 { font-size: 20px; font-weight: 900; margin: 0; }\n");
			html.append(".header .date { margin-top: 6px; font-size: 15px; font-weight: 900; }\n");
			html.append(".card { border: 3px solid #000; margin-bottom: 14px; background: #fff; }\n");
			html.append(".card-title { padding: 10px; font-weight: 900; font-size: 14px; border-bottom: 3px solid #000; }\n");
			html.append(".him { background: linear-gradient(to right, #3b82f6, #6366f1); }\n");
			html.append(".sud { background: linear-gradient(to right, #10b981, #34d399); }\n");
			html.append(".vp { background: #eee; }\n");
			html.append(".section { border-top: 3px solid #000; }\n");
			html.append(".section-title { padding: 7px 10px; font-weight: 900; font-size: 13px; }\n");
			html.append(".today { background: #000; color: #fff; }\n");
			html.append(".tomorrow { background: #ddd; }\n");
			html.append(".row { display: flex; justify-content: space-between; padding: 8px 10px; border-top: 2px solid #000; font-size: 13px; font-weight: 800; }\n");
			html.append(".status { font-weight: 900; }\n");
			html.append(".sc { color: #b8860b; }\n");
			html.append(".gf { color: #777; }\n");
			html.append(".nr { color: #777; }\n");
			html.append(".ns { color: #777; }\n");
			html.append(".pd { color: #8b0000; }\n");
			html.append(".card-him .row { background: #eef4ff; }\n");
			html.append(".empty { padding: 10px; border-top: 2px solid #000; font-weight: 700; font-size: 13px; }\n");
			html.append(".footer { border: 3px solid #000; padding: 10px; font-size: 12px; font-weight: 800; background: #fff; text-align: center; }\n");
			html.append("@media (max-width: 400px) { .header h1 { font-size: 18px; } .header .date { font-size: 14px; } .row { font-size: 12px; } }\n");
			html.append("</style>\n</head>\n<body>\n");
			html.append("<div class=\"container\">\n");
			html.append("<div class=\"header\"><h1>INTERVIEW SCHEDULE</h1><div class=\"date\">TODAY - ").append(dateUpper).append("</div></div>\n");

			boolean cardOpen = false;
			boolean sectionOpen = false;

			for (String line : outputLines) {
				if (line.startsWith("Today's date:") || line.equals("-----------------------------------") || line.trim().isEmpty()) {
					continue;
				} else if (line.startsWith("Updated at (IST):")) {
					if (sectionOpen) { html.append("</div>\n"); sectionOpen = false; }
					if (cardOpen) { html.append("</div>\n"); cardOpen = false; }
					html.append("<div class=\"footer\">&#9201; Updated at (IST): ")
						.append(line.replace("Updated at (IST):", "").trim()).append("</div>\n");
				} else if (line.startsWith("**************** SCHEDULE FOR")) {
					if (sectionOpen) { html.append("</div>\n"); sectionOpen = false; }
					if (cardOpen) { html.append("</div>\n"); cardOpen = false; }
					String acc = line.replace("*", "").trim();
					String cls = acc.contains("HIMANSHU") ? "him" : acc.contains("SUDHANSHU") ? "sud" : "vp";
					String ico = acc.contains("HIMANSHU") ? "&#128100;" : acc.contains("SUDHANSHU") ? "&#128101;" : "&#11088;";
					String lbl = acc.contains("HIMANSHU") ? "Himanshu &mdash; JTwine" : acc.contains("SUDHANSHU") ? "Sudhanshu &mdash; JTwine" : "VProp";
					String cardCls = acc.contains("HIMANSHU") ? "card card-him" : "card";
					html.append("<div class=\"").append(cardCls).append("\"><div class=\"card-title ").append(cls).append("\">").append(ico).append(" ").append(lbl).append("</div>\n");
					cardOpen = true;
				} else if (line.equals("\u00a7TODAY\u00a7")) {
					if (sectionOpen) { html.append("</div>\n"); sectionOpen = false; }
					html.append("<div class=\"section\"><div class=\"section-title today\">TODAY &mdash; ").append(dateUpper).append("</div>\n");
					sectionOpen = true;
				} else if (line.equals("\u00a7TOMORROW\u00a7")) {
					if (sectionOpen) { html.append("</div>\n"); sectionOpen = false; }
					html.append("<div class=\"section\"><div class=\"section-title tomorrow\">TOMORROW &mdash; ").append(tomorrowUpper).append("</div>\n");
					sectionOpen = true;
				} else if (line.startsWith("\u272a ")) {
					String content = line.substring(2).trim();
					String[] parts = content.split("==>");
					if (parts.length == 2) {
						String disc = parts[0].trim();
						String stat = parts[1].trim();
						String bc;
						switch (stat) {
							case "Scheduled": bc = "sc"; break;
							case "Not Recommended": bc = "nr"; break;
							case "Is a Good Fit": bc = "gf"; break;
							case "Candidate No Show": bc = "ns"; break;
							case "Strongly Recommended": bc = "gf"; break;
							case "Pending Feedback Review": bc = "pd"; break;
							default: bc = "nr";
						}
						String[] dtParts = disc.split(", ");
						String time = dtParts.length >= 4 ? dtParts[3] : disc;
						html.append("<div class=\"row\"><div>").append(time).append("</div>")
							.append("<div class=\"status ").append(bc).append("\">").append(stat).append("</div></div>\n");
					} else {
						html.append("<div class=\"row\"><div>").append(content).append("</div></div>\n");
					}
				} else if (!line.trim().isEmpty()) {
					if (cardOpen) {
						html.append("<div class=\"empty\">").append(line.trim()).append("</div>\n");
					}
				}
			}

			if (sectionOpen) { html.append("</div>\n"); }
			if (cardOpen) { html.append("</div>\n"); }

			html.append("</div>\n</body>\n</html>");

			java.nio.file.Files.write(java.nio.file.Paths.get("deploy/index.html"),
					html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
			System.out.println("index.html generated successfully.");

		} catch (java.io.IOException ioe) {
			System.err.println("Failed to write index.html: " + ioe.getMessage());
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