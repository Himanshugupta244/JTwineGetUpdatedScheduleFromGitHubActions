package GmailProjectForGitHubActions;

import java.util.List;
import java.util.ArrayList;
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
	public static WebDriver driver;

	public static void main(String[] args) {
		List<String> outputLines = new ArrayList<>();
		try {
			todayDate = getTodayDateFormatted();
			System.out.println("Today's date: " + todayDate);
			outputLines.add("Today's date: " + todayDate);
			loginToJTwine();
			List<String> scheduleLines = fetchScheduleForToday(todayDate);
			outputLines.addAll(scheduleLines);
		} catch(Exception ex) {
			ex.printStackTrace();
			outputLines.add("Exception: " + ex.getMessage());
		} finally {
			if (driver != null) {
				driver.quit();
			}
			try {
				Files.write(Paths.get("schedule.txt"), outputLines, StandardCharsets.UTF_8);
			} catch (IOException ioe) {
				System.err.println("Failed to write schedule.txt: " + ioe.getMessage());
			}
		}
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
		driver.get("https://www.jobtwine.com/signin");
		waitForFixTime(2000);
		String username = System.getenv("JTWINE_USERNAME");
		String password = System.getenv("JTWINE_PASSWORD");
		if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
			throw new IllegalArgumentException("JTWINE_USERNAME and/or JTWINE_PASSWORD environment variables are not set or empty.");
		}
		else {
			System.out.println("Current URL after login: " + driver.getCurrentUrl());
			System.out.println("Page Title: " + driver.getTitle());
		}
		driver.findElement(By.xpath(".//input[@formcontrolname='userName']")).sendKeys(username);
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//button[contains(text(),'Next')]"))
				.click();
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//input[@formcontrolname='password']")).sendKeys(password);
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//button[contains(text(),'Sign In')]"))
				.click();
		waitForFixTime(10000);
		if(driver.findElements(By.xpath(".//div[contains(text(),'Candidates For Interview')]")).size() > 0) {
			System.out.println("Login successful");
		} else {
			throw new RuntimeException("Login failed - 'Candidates For Interview' text not found after login.");
		}
	}
	
	public static List<String> fetchScheduleForToday(String todayDate) throws Exception {
		List<String> lines = new ArrayList<>();
		System.out.println("Fetching schedule for today");
		lines.add("Fetching schedule for today");
		waitTillElementVisible(By.xpath(".//span[text()='Start Meeting']"), 30);
		List<WebElement> discussionList = driver.findElements(By.xpath(
				".//div[@class='sub-sub-heading-1'][contains(text(),'" + todayDate + "')]"));
		System.out.println("Total discussions for today: " + discussionList.size());
		lines.add("Total discussions for today: " + discussionList.size());
		for (int index = 0; index < discussionList.size(); index++) {
			WebElement discussion = discussionList.get(index);
			System.out.println("Discussion " + (index + 1) + ": " + discussion.getText());
			lines.add("Discussion " + (index + 1) + ": " + discussion.getText());
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

	public static void waitTillElementVisible(By locator, int timeoutInSeconds) {
		org.openqa.selenium.support.ui.WebDriverWait wait = new org.openqa.selenium.support.ui.WebDriverWait(driver,
				java.time.Duration.ofSeconds(timeoutInSeconds));
		wait.until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(locator));
	}

}