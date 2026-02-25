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
	public static List<String> outputLines = new ArrayList<>();

	public static void main(String[] args) {

		username = System.getenv("JTWINE_USERNAME_HIM");
		password = System.getenv("JTWINE_PASSWORD_HIM");
		usernameVprop = System.getenv("VPROP_USERNAME_HIM");
		usernameVprop = System.getenv("VPROP_PASSWORD_HIM");
		todayDate = getTodayDateFormatted();
		tomorrowDate = getTomorrowDateFormatted();
		System.out.println("Today's date: " + todayDate);
		outputLines.add("Today's date: " + todayDate);

		try {
			System.out.println("**************** SCHEDULE FOR HIMANSHU JTWINE ACCOUNT ****************");
			outputLines.add("**************** SCHEDULE FOR HIMANSHU JTWINE ACCOUNT ****************");
			loginToJTwine();
			List<String> scheduleLines = fetchScheduleForToday();
			outputLines.addAll(scheduleLines);
			driver.quit();
			System.out.println("======================================================================");
			outputLines.add("======================================================================");
			username = System.getenv("JTWINE_USERNAME_SUD");
			password = System.getenv("JTWINE_PASSWORD_SUD");
			System.out.println("**************** SCHEDULE FOR SUDHANSHU JTWINE ACCOUNT ****************");
			outputLines.add("**************** SCHEDULE FOR SUDHANSHU JTWINE ACCOUNT ****************");
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
		outputLines.add("======================================================================");
		System.out.println("**************** SCHEDULE FOR Vprop ACCOUNT ****************");
		outputLines.add("**************** SCHEDULE FOR Vprop ACCOUNT ****************");
		outputLines.add("-----------------------------------");
		usernameVprop = System.getenv("VPROP_USERNAME_HIM");
		passwordVprop = System.getenv("VPROP_PASSWORD_HIM");
		todayDateVpropFormat = getTodayDateAsPerVpropFormat();
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
		List<String> lines = new ArrayList<>();
		System.out.println("Fetching schedule.....");
		System.out.println("======================================================================");
		waitTillElementVisible(By.xpath(".//span[text()='Start Meeting']"), 60);

		String todayLocator = ".//div[@class='sub-sub-heading-1'][contains(text(),'" + todayDate + "')]";
		String todayStatusLocator = todayLocator+"//ancestor::div[contains(@class,'candidate-details-sec')]//div[contains(@class,'btn-chip')]/div";
		List<WebElement> discussionListToday = driver.findElements(By.xpath(todayLocator));
		List<WebElement> discussionStatusListToday = driver.findElements(By.xpath(todayStatusLocator));

		for (int index = 0; index < discussionListToday.size(); index++) {
			WebElement discussion = discussionListToday.get(index);
			WebElement discussionStatus = discussionStatusListToday.get(index);
			System.out.println("Discussion " + (index + 1) + ": " + discussion.getText() + " ==> " + discussionStatus.getText());
			lines.add("Discussion " + (index + 1) + ": " + discussion.getText() + " ==> " + discussionStatus.getText());
		}

		String tomorrowLocator = ".//div[@class='sub-sub-heading-1'][contains(text(),'" + tomorrowDate + "')]";
		String tomorrowStatusLocator = tomorrowLocator+"//ancestor::div[contains(@class,'candidate-details-sec')]//div[contains(@class,'btn-chip')]/div";
		List<WebElement> discussionListTomorrow = driver.findElements(By.xpath(tomorrowLocator));
		List<WebElement> discussionStatusListTomorrow = driver.findElements(By.xpath(tomorrowStatusLocator));
		for (int index = 0; index < discussionListTomorrow.size(); index++) {
			WebElement discussion = discussionListTomorrow.get(index);
			WebElement discussionStatus = discussionStatusListTomorrow.get(index);
			System.out.println("Discussion " + (index + 1) + ": " + discussion.getText() + " ==> " + discussionStatus.getText());
			lines.add("Discussion " + (index + 1) + ": " + discussion.getText() + " ==> " + discussionStatus.getText());
		}
		if(discussionListToday.isEmpty() && discussionListTomorrow.isEmpty()) {
			System.out.println("No discussions scheduled for today and tomorrow.");
			lines.add("No discussions scheduled for today and tomorrow.");
			System.out.println("======================================================================");
		}
		return lines;
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
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd");
		return today.format(formatter);
	}

	public static String getTomorrowDateFormatted() {
		java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
		java.time.format.DateTimeFormatter formatter =
				java.time.format.DateTimeFormatter.ofPattern("MMM dd");
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
	        StringBuilder html = new StringBuilder();
	        html.append("<!DOCTYPE html>\n");
	        html.append("<html lang=\"en\">\n");
	        html.append("<head>\n");
	        html.append("<meta charset=\"UTF-8\">\n");
	        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
	        html.append("<title>JTwine & VProp Schedule</title>\n");
	        html.append("<style>\n");
	        html.append("body { font-family: Arial, sans-serif; line-height: 1.5; padding: 20px; background: #f9f9f9; }\n");
	        html.append("h2 { color: #2c3e50; border-bottom: 2px solid #2c3e50; padding-bottom: 5px; }\n");
	        html.append("h3 { color: #34495e; margin-top: 20px; }\n");
	        html.append("p { margin: 5px 0; }\n");
	        html.append(".separator { border-top: 1px solid #ccc; margin: 10px 0; }\n");
	        html.append("table { border-collapse: collapse; width: 100%; margin: 10px 0; }\n");
	        html.append("th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }\n");
	        html.append("th { background-color: #ecf0f1; }\n");
	        html.append(".scheduled { color: green; font-weight: bold; }\n");
	        html.append(".not-recommended { color: red; font-weight: bold; }\n");
	        html.append(".good-fit { color: orange; font-weight: bold; }\n");
	        html.append(".unknown-status { color: gray; }\n");
	        html.append("</style>\n");
	        html.append("</head>\n");
	        html.append("<body>\n");

	        html.append("<h2>Schedule for Today</h2>\n");

	        String currentAccount = "";
	        boolean tableOpen = false;

	        for (String line : outputLines) {
	            if (line.equals("-----------------------------------")) {
	                if (tableOpen) {
	                    html.append("</table>\n");
	                    tableOpen = false;
	                }
	                html.append("<div class=\"separator\"></div>\n");
	            } else if (line.startsWith("**************** SCHEDULE FOR")) {
	                if (tableOpen) {
	                    html.append("</table>\n");
	                    tableOpen = false;
	                }
	                currentAccount = line.replace("*", "").trim();
	                html.append("<h3>").append(currentAccount).append("</h3>\n");
	                html.append("<table>\n");
	                html.append("<tr><th>Discussion</th><th>Status</th></tr>\n");
	                tableOpen = true;
	            } else if (line.startsWith("Discussion")) {
	                String[] parts = line.split("==>");
	                if (parts.length == 2) {
	                    String discussion = parts[0].trim();
	                    String status = parts[1].trim();

	                    String statusClass;
	                    switch (status) {
	                        case "Scheduled" : statusClass = "scheduled";
	                        case "Not Recommended" : statusClass = "not-recommended";
	                        case "Is a Good Fit" : statusClass = "good-fit";
	                        case "Candidate No Show" : statusClass = "unknown-status";
	                        case "Strongly Recommended" : statusClass = "good-fit";
	                        default : statusClass = "unknown-status";
	                    }

	                    html.append("<tr><td>").append(discussion).append("</td>")
	                        .append("<td class=\"").append(statusClass).append("\">")
	                        .append(status).append("</td></tr>\n");
	                } else {
	                    html.append("<tr><td colspan=\"2\">").append(line).append("</td></tr>\n");
	                }
	            } else {
	                // Other messages like "Fetching schedule..."
	                html.append("<p>").append(line).append("</p>\n");
	            }
	        }

	        if (tableOpen) {
	            html.append("</table>\n");
	        }

	        html.append("</body>\n</html>");

	        // Write HTML file
	        java.nio.file.Files.write(java.nio.file.Paths.get("deploy/index.html"),
	                html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	        System.out.println("schedule.html generated successfully in deploy/index.html");

	    } catch (java.io.IOException ioe) {
	        System.err.println("Failed to write schedule.html: " + ioe.getMessage());
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
			driver.get("https://expert.vprople.com/expert-login");
			waitForFixTime(2000);
			if (usernameVprop == null || usernameVprop.isEmpty() || passwordVprop == null || passwordVprop.isEmpty()) {
				throw new IllegalArgumentException("usernameVprop and/or passwordVprop environment variables are not set or empty.");
			}
			waitTillElementVisible(By.id("yourUsername"), 30);
			driver.findElement(By.id("yourUsername")).sendKeys(usernameVprop);
			waitForFixTime(1000);
			waitTillElementVisible(By.id("yourPassword"), 30);
			driver.findElement(By.id("yourPassword")).sendKeys(passwordVprop);
			waitForFixTime(1000);
			waitTillElementVisible(By.xpath(".//button[text()='Login']"), 30);
			driver.findElement(By.xpath(".//button[text()='Login']")).click();
			System.out.println("Waiting for 10 seconds......");
			waitForFixTime(10000);
			if(driver.findElements(By.xpath(".//h5[text()='Complete your profile']")).size() > 0) {
				if(driver.findElements(By.xpath(".//h5[text()='Complete your profile']/parent::div/following-sibling::div//button[text()='Close']")).size() > 0) {
					driver.findElement(By.xpath(".//h5[text()='Complete your profile']/parent::div/following-sibling::div//button[text()='Close']")).click();
				}
			}
			driver.get("https://expert.vprople.com/expert-dashboard");
			System.out.println("Waiting for 10 seconds more......");
			waitForFixTime(10000);
			if(driver.findElements(By.xpath(".//a[span[contains(text(),'Dashboard')]]")).size() > 0) {
				System.out.println("Login to Vprop is successful");
			} else {
				throw new RuntimeException("Login to Vprop is failed - 'Dashboard' text not found after login.");
			}
			driver.get("https://expert.vprople.com/interviews");
			waitTillElementVisible(By.xpath(".//label[contains(text(),' entries per page')]"), 30);
			waitForFixTime(1000);
			List<WebElement> discussionListToday = driver.findElements(By.xpath(".//p[contains(text(),'"+todayDateVpropFormat+"')]"));
			waitForFixTime(500);
			System.out.println("Getting Vprop schedule for today....");
			if(discussionListToday.isEmpty()) {
				System.out.println("No discussions scheduled for today in Vprop.");
				outputLines.add("No discussions scheduled for today in Vprop.");
			} else {

				for (int index = 0; index < discussionListToday.size(); index++) {
					WebElement discussion = discussionListToday.get(index);
					System.out.println("Discussion " + (index + 1) + ": " + discussion.getText());
					outputLines.add("Discussion " + (index + 1) + ": " + discussion.getText());
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
		return dateSplitted[1] + "-" + dateSplitted[0]; 
	}

}