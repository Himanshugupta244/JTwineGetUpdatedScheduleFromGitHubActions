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

			StringBuilder html = new StringBuilder();
			html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
			html.append("<meta charset=\"UTF-8\">\n");
			html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
			html.append("<title>Interview Schedule</title>\n");
			html.append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n");
			html.append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap\" rel=\"stylesheet\">\n");
			html.append("<style>\n");
			html.append("*{box-sizing:border-box;margin:0;padding:0}\n");
			html.append("body{font-family:'Inter',Arial,sans-serif;background:linear-gradient(135deg,#e8eaf6 0%,#f0f4ff 55%,#e8f5e9 100%);min-height:100vh;padding:26px 16px}\n");
			html.append(".wrap{max-width:780px;margin:0 auto}\n");
			html.append(".hdr{text-align:center;padding:20px;margin-bottom:20px;background:rgba(255,255,255,0.75);border-radius:14px;border:1px solid rgba(99,102,241,0.18);box-shadow:0 2px 12px rgba(99,102,241,0.08)}\n");
			html.append(".hdr h1{font-size:22px;font-weight:700;background:linear-gradient(90deg,#4f46e5,#0ea5e9,#059669);-webkit-background-clip:text;-webkit-text-fill-color:transparent;margin-bottom:8px}\n");
			html.append(".dbadge{display:inline-block;background:rgba(79,70,229,0.1);border:1px solid rgba(79,70,229,0.3);color:#4338ca;padding:3px 14px;border-radius:20px;font-size:13px;font-weight:500}\n");
			html.append(".card{background:#ffffff;border-radius:12px;border:1px solid rgba(0,0,0,0.08);margin-bottom:16px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.06)}.card-him{border:2.5px solid #1a1a2e}.card-sud{border:2.5px solid #1a1a2e}\n");
			html.append(".ch{padding:10px 16px;font-size:17px;font-weight:600;letter-spacing:0.7px;text-transform:uppercase}\n");
			html.append(".ch.him{background:linear-gradient(90deg,rgba(99,102,241,0.18),rgba(99,102,241,0.02));border-bottom:1px solid rgba(99,102,241,0.15);color:#3730a3}\n");
			html.append(".ch.sud{background:linear-gradient(90deg,rgba(5,150,105,0.15),rgba(5,150,105,0.02));border-bottom:1px solid rgba(5,150,105,0.15);color:#065f46}\n");
			html.append(".ch.vp{background:linear-gradient(90deg,rgba(217,119,6,0.15),rgba(217,119,6,0.02));border-bottom:1px solid rgba(217,119,6,0.15);color:#92400e}\n");
			html.append("table{width:100%;border-collapse:collapse}\n");
			html.append("tr.dr:hover td{background:rgba(99,102,241,0.04)}\n");
			html.append("td{padding:8px 16px;font-size:14px;border-bottom:1px solid rgba(0,0,0,0.06);color:#1e293b;vertical-align:middle}\n");
			html.append("tr:last-child td{border-bottom:none}\n");
			html.append(".dc{display:none}.tc{width:42%;text-align:left;font-weight:600;color:#1e293b;padding-left:12px}.st{width:58%;text-align:right;padding-right:14px}\n");
			html.append(".b{display:inline-block;padding:2px 10px;border-radius:12px;font-size:12.5px;font-weight:600;white-space:nowrap}\n");
			html.append(".sc{background:rgba(59,130,246,0.1);color:#1d4ed8;border:1px solid rgba(59,130,246,0.3)}\n");
			html.append(".pd{background:rgba(220,38,38,0.1);color:#b91c1c;border:1px solid rgba(220,38,38,0.25)}\n");
			html.append(".gf{background:rgba(5,150,105,0.1);color:#065f46;border:1px solid rgba(5,150,105,0.25)}\n");
			html.append(".nr{background:rgba(107,114,128,0.1);color:#374151;border:1px solid rgba(107,114,128,0.25)}\n");
			html.append(".unk{background:rgba(107,114,128,0.07);color:#4b5563;border:1px solid rgba(107,114,128,0.18)}\n");
			html.append(".sep td{padding:7px 16px;font-size:16px;font-weight:700;letter-spacing:0.6px;text-transform:uppercase;border-bottom:1px solid rgba(0,0,0,0.06)}\n");
			html.append(".sep.tod td{background:rgba(99,102,241,0.07);color:#4338ca}\n");
			html.append(".sep.tom td{background:rgba(5,150,105,0.06);color:#065f46}\n");
			html.append(".emr td{padding:11px 16px;font-size:13px;color:#94a3b8;font-style:italic;text-align:center;border-bottom:none}\n");
			html.append(".footer{text-align:center;margin-top:16px;font-size:12px;color:#64748b}\n");
			html.append("@media(max-width:600px){body{padding:14px 8px}.hdr{padding:14px 10px;margin-bottom:12px}.hdr h1{font-size:18px}.dbadge{font-size:12px;padding:3px 10px}.ch{font-size:15px;padding:8px 12px}.card{margin-bottom:12px;border-radius:10px}td{padding:8px 10px;font-size:13px}.st{width:58%;padding-right:8px}.b{font-size:12px;padding:2px 9px}.sep td{padding:7px 12px;font-size:14px}.emr td{font-size:12px;padding:10px 12px}.footer{font-size:11px;margin-top:12px}}\n");
			html.append("</style>\n</head>\n<body>\n<div class=\"wrap\">\n");
			html.append("<div class=\"hdr\"><h1>&#128197; Interview Schedule</h1>");
			html.append("<span class=\"dbadge\">Today &nbsp;&#8212;&nbsp; ").append(dateDisplay).append("</span></div>\n");

			boolean tableOpen = false;

			for (String line : outputLines) {
				if (line.startsWith("Today's date:") || line.equals("-----------------------------------")) {
					continue;
				} else if (line.startsWith("Updated at (IST):")) {
					if (tableOpen) { html.append("</table></div>\n"); tableOpen = false; }
					html.append("<div class=\"footer\">&#128337;&nbsp; Updated at (IST): ")
						.append(line.replace("Updated at (IST):", "").trim()).append("</div>\n");
				} else if (line.startsWith("**************** SCHEDULE FOR")) {
					if (tableOpen) { html.append("</table></div>\n"); tableOpen = false; }
					String acc = line.replace("*", "").trim();
					String cls = acc.contains("HIMANSHU") ? "him" : acc.contains("SUDHANSHU") ? "sud" : "vp";
					String ico = acc.contains("HIMANSHU") ? "&#128100;" : acc.contains("SUDHANSHU") ? "&#128101;" : "&#127775;";
					String lbl = acc.contains("HIMANSHU") ? "Himanshu &mdash; JTwine" : acc.contains("SUDHANSHU") ? "Sudhanshu &mdash; JTwine" : "VProp";
				String cardExtra = acc.contains("HIMANSHU") ? " card-him" : acc.contains("SUDHANSHU") ? " card-sud" : "";
				html.append("<div class=\"card").append(cardExtra).append("\"><div class=\"ch ").append(cls).append("\">").append(ico).append("&nbsp;").append(lbl).append("</div><table>\n");
					tableOpen = true;
				} else if (line.equals("\u00a7TODAY\u00a7")) {
					html.append("<tr class=\"sep tod\"><td colspan=\"3\">&#9719; Today &mdash; ").append(dateDisplay).append("</td></tr>\n");
				} else if (line.equals("\u00a7TOMORROW\u00a7")) {
					html.append("<tr class=\"sep tom\"><td colspan=\"3\">&#9719; Tomorrow &mdash; ").append(tomorrowDate).append("</td></tr>\n");
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
							case "Candidate No Show": bc = "nr"; break;
							case "Strongly Recommended": bc = "gf"; break;
							case "Pending Feedback Review": bc = "pd"; break;
							default: bc = "unk";
						}
						String[] dtParts = disc.split(", ");
						String date = dtParts.length >= 4 ? dtParts[0] + ", " + dtParts[1] + ", " + dtParts[2] : disc;
						String time = dtParts.length >= 4 ? dtParts[3] : "";
						html.append("<tr class=\"dr\"><td class=\"dc\">").append(date).append("</td>")
							.append("<td class=\"tc\">").append(time).append("</td>")
							.append("<td class=\"st\"><span class=\"b ").append(bc).append("\">")
							.append(stat).append("</span></td></tr>\n");
					} else {
						html.append("<tr class=\"dr\"><td class=\"dc\" colspan=\"3\">").append(content).append("</td></tr>\n");
					}
				} else if (!line.trim().isEmpty()) {
					if (tableOpen) {
						html.append("<tr class=\"emr\"><td colspan=\"3\">").append(line.trim()).append("</td></tr>\n");
					}
				}
			}

			if (tableOpen) {
				html.append("</table></div>\n");
			}

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