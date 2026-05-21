package GmailProjectForGitHubActions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

public class ScheduleVpropleInterviewHim {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static WebDriver driver;
    public static String usernameVprop = System.getenv("VPROP_USERNAME");
	public static String passwordVprop = System.getenv("VPROP_PASSWORD");
    private static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID_VPROP");
    private static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN_VPROP");
    private static final String TO_PHONE_NUMBER = System.getenv("TWILIO_TO_NUMBER_VPROP");
    private static final String FROM_PHONE_NUMBER = System.getenv("TWILIO_FROM_NUMBER_VPROP");
    private static final String TWIML_URL = "http://demo.twilio.com/docs/voice.xml";
    private static final String BASE_URL = "https://api.twilio.com/2010-04-01/Accounts/";
    
    // Utility method for timestamped logging
    private static void printLog(String level, String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        System.out.println("[" + timestamp + "] [" + level + "] " + message);
    }
    
    private static void printError(String level, String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        System.err.println("[" + timestamp + "] [" + level + "] " + message);
    }

    public static void main(String args[]) throws Exception {
        printLog("INFO", "=== VProple Interview Scheduler Started ===");
        printLog("INFO", "Application start time: " + LocalDateTime.now().format(TIME_FORMATTER));
        printLog("INFO", "Monitoring configuration:");
        printLog("INFO", "  - VProple Username: " + usernameVprop);
        printLog("INFO", "  - Twilio Account SID: " + ACCOUNT_SID);
        printLog("INFO", "  - Target Phone Number: " + TO_PHONE_NUMBER);
        printLog("INFO", "  - Check Interval: 10 seconds");
        
        int attemptCount = 0;
    	while(true) {
    	    attemptCount++;
    	    long startTime = System.currentTimeMillis();
    	    
    	    printLog("INFO", "=== Starting Check Cycle #" + attemptCount + " at " + 
    	               LocalDateTime.now().format(TIME_FORMATTER) + " ===");
    	    
			try {
				loginAndScheduleVPropleInterview();
				printLog("INFO", "Check cycle #" + attemptCount + " completed successfully");
			} catch (Exception e) {
				printError("ERROR", "CRITICAL ERROR in check cycle #" + attemptCount + ": " + e.getMessage());
				printError("ERROR", "Exception type: " + e.getClass().getSimpleName());
				printError("ERROR", "Stack trace:");
				e.printStackTrace();
			}
			
			long duration = System.currentTimeMillis() - startTime;
			printLog("INFO", "Cycle #" + attemptCount + " duration: " + duration + "ms");
			printLog("INFO", "Waiting 10 seconds before next check...");
			
			waitForFixSeconds(10);
		}
    	}
    
    public static void makeCall() {
        printLog("INFO", ">>> Entering makeCall() method");
        long callStartTime = System.currentTimeMillis();
        
        try {
            String fullUrl = BASE_URL + ACCOUNT_SID + "/Calls.json";
            printLog("INFO", "Initiating Twilio API call to: " + fullUrl);
            printLog("INFO", "Call parameters:");
            printLog("INFO", "  - To: " + TO_PHONE_NUMBER);
            printLog("INFO", "  - From: " + FROM_PHONE_NUMBER);
            printLog("INFO", "  - TwiML URL: " + TWIML_URL);

            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(fullUrl);
            printLog("DEBUG", "HTTP client and POST request initialized");

            // 🔐 Basic Auth Header
            String credentials = ACCOUNT_SID + ":" + AUTH_TOKEN;
            String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes());
            post.setHeader("Authorization", "Basic " + encodedAuth);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            printLog("DEBUG", "Authorization headers set");

            // 📞 Request Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("To", TO_PHONE_NUMBER));
            params.add(new BasicNameValuePair("From", FROM_PHONE_NUMBER));
            params.add(new BasicNameValuePair("Url", TWIML_URL));

            post.setEntity(new UrlEncodedFormEntity(params));
            printLog("DEBUG", "Request parameters configured");

            ClassicHttpResponse response = null;
            try {
                printLog("INFO", "Executing Twilio API call...");
                response = client.executeOpen(null, post, null);
                
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                printLog("INFO", "=== TWILIO API RESPONSE ===");
                printLog("INFO", "Response Code: " + statusCode);
                printLog("INFO", "Response Body: " + responseBody);
                
                if (statusCode >= 200 && statusCode < 300) {
                    printLog("INFO", "SUCCESS: Twilio call initiated successfully");
                } else {
                    printLog("WARN", "WARNING: Unexpected response code " + statusCode + " from Twilio API");
                }
                
            } finally {
                if (response != null) {
                    try {
                        EntityUtils.consume(response.getEntity());
                        printLog("DEBUG", "Response entity consumed");
                    } catch (Exception e) {
                        printLog("WARN", "Exception while consuming response entity: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            printError("ERROR", "CRITICAL ERROR during Twilio API call: " + e.getMessage());
            printError("ERROR", "Exception type: " + e.getClass().getSimpleName());
            printError("ERROR", "Detailed stack trace:");
            e.printStackTrace();
        }
        
        long callDuration = System.currentTimeMillis() - callStartTime;
        printLog("INFO", "makeCall() completed in " + callDuration + "ms");
        printLog("INFO", "<<< Exiting makeCall() method");
    }
    
    public static void loginAndScheduleVPropleInterview() {
        printLog("INFO", ">>> Entering loginAndScheduleVPropleInterview() method");
        long methodStartTime = System.currentTimeMillis();
        
		try {
			printLog("INFO", "=== STARTING VPROPLE LOGIN PROCESS ===");
			printLog("INFO", "Target URL: https://expert.vprople.com/login");
			printLog("INFO", "Username: " + usernameVprop);
			
			// Setup WebDriver
			printLog("INFO", "Setting up Chrome WebDriver...");
			printLog("INFO", "System OS: " + System.getProperty("os.name"));
			printLog("INFO", "System Architecture: " + System.getProperty("os.arch"));
			
			// Check for Chrome installation
			checkChromeInstallation();
			
			try {
				WebDriverManager.chromedriver().setup();
				printLog("DEBUG", "WebDriverManager setup completed");
			} catch (Exception e) {
				printError("ERROR", "Failed to setup WebDriverManager: " + e.getMessage());
				throw e;
			}
			
			ChromeOptions options = new ChromeOptions();
			
			// Enhanced Chrome options for AWS/Linux - addressing common issues
			options.addArguments("--headless=new");          // Use new headless mode
			options.addArguments("--no-sandbox");            // Required for AWS/Docker
			options.addArguments("--disable-dev-shm-usage"); // Overcome limited resource problems
			options.addArguments("--disable-gpu");           // Disable GPU acceleration
			options.addArguments("--disable-software-rasterizer");
			options.addArguments("--window-size=1920,1080"); 
			options.addArguments("--remote-debugging-port=9222");
			options.addArguments("--disable-extensions");    // Disable extensions
			options.addArguments("--disable-plugins");       // Disable plugins  
			options.addArguments("--disable-images");        // Speed up loading
			options.addArguments("--disable-background-timer-throttling");
			options.addArguments("--disable-backgrounding-occluded-windows");
			options.addArguments("--disable-renderer-backgrounding");
			options.addArguments("--disable-features=TranslateUI");
			options.addArguments("--disable-ipc-flooding-protection");
			options.addArguments("--user-data-dir=/tmp/chrome-user-data-" + System.currentTimeMillis());
			options.addArguments("--data-path=/tmp/chrome-data-" + System.currentTimeMillis());
			options.addArguments("--disk-cache-dir=/tmp/chrome-cache-" + System.currentTimeMillis());
			options.addArguments("--single-process");        // Use single process mode
			options.addArguments("--disable-web-security");  // Disable web security for testing
			
			// Try to set Chrome binary path explicitly (common AWS locations)
			String[] possibleChromePaths = {
				"/usr/bin/google-chrome",
				"/usr/bin/google-chrome-stable", 
				"/usr/bin/chromium-browser",
				"/usr/bin/chromium",
				"/opt/google/chrome/google-chrome"
			};
			
			String chromePath = findChromeExecutable(possibleChromePaths);
			if (chromePath != null) {
				options.setBinary(chromePath);
				printLog("INFO", "Using Chrome binary at: " + chromePath);
			} else {
				printLog("WARN", "Chrome binary not found in common locations - will try default");
			}
			
			printLog("DEBUG", "Chrome options configured for AWS/Linux environment");
			
			// Try to initialize Chrome with multiple strategies
			Exception lastException = null;
			boolean driverInitialized = false;
			
			// Strategy 1: Try with detected Chrome path
			if (chromePath != null && !driverInitialized) {
				try {
					printLog("INFO", "Strategy 1: Using detected Chrome binary at: " + chromePath);
					driver = new ChromeDriver(options);
					driver.manage().window().maximize();
					printLog("INFO", "✓ SUCCESS: Chrome driver initialized with detected binary");
					driverInitialized = true;
				} catch (Exception e) {
					lastException = e;
					printError("ERROR", "Strategy 1 failed: " + e.getMessage());
					if (driver != null) {
						try { driver.quit(); } catch (Exception ex) {}
						driver = null;
					}
				}
			}
			
			// Strategy 2: Try without explicit binary path (let WebDriverManager handle it)
			if (!driverInitialized) {
				try {
					printLog("INFO", "Strategy 2: Using WebDriverManager default Chrome");
					ChromeOptions defaultOptions = new ChromeOptions();
					// Use minimal options for compatibility
					defaultOptions.addArguments("--headless=new");
					defaultOptions.addArguments("--no-sandbox");
					defaultOptions.addArguments("--disable-dev-shm-usage");
					defaultOptions.addArguments("--disable-gpu");
					defaultOptions.addArguments("--window-size=1920,1080");
					
					driver = new ChromeDriver(defaultOptions);
					driver.manage().window().maximize();
					printLog("INFO", "✓ SUCCESS: Chrome driver initialized with minimal options");
					driverInitialized = true;
				} catch (Exception e) {
					lastException = e;
					printError("ERROR", "Strategy 2 failed: " + e.getMessage());
					if (driver != null) {
						try { driver.quit(); } catch (Exception ex) {}
						driver = null;
					}
				}
			}
			
			// Strategy 3: Try with alternative Chrome locations
			if (!driverInitialized) {
				String[] alternativePaths = {"google-chrome", "chromium-browser", "chromium"};
				for (String altPath : alternativePaths) {
					try {
						printLog("INFO", "Strategy 3: Trying alternative Chrome path: " + altPath);
						ChromeOptions altOptions = new ChromeOptions();
						altOptions.setBinary(altPath);
						altOptions.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
						
						driver = new ChromeDriver(altOptions);
						driver.manage().window().maximize();
						printLog("INFO", "✓ SUCCESS: Chrome driver initialized with alternative path: " + altPath);
						driverInitialized = true;
						break;
					} catch (Exception e) {
						lastException = e;
						printLog("WARN", "Alternative path failed: " + altPath + " - " + e.getMessage());
						if (driver != null) {
							try { driver.quit(); } catch (Exception ex) {}
							driver = null;
						}
					}
				}
			}
			
			if (!driverInitialized) {
				printError("ERROR", "=== ALL CHROME STRATEGIES FAILED ===");
				printError("ERROR", "Could not initialize Chrome driver with any strategy");
				if (lastException != null) {
					printError("ERROR", "Last exception: " + lastException.getMessage());
					lastException.printStackTrace();
				}
				
				// Provide detailed troubleshooting info
				printError("ERROR", "Troubleshooting steps:");
				printError("ERROR", "1. Verify Chrome installation: google-chrome --version");
				printError("ERROR", "2. Install missing libraries: sudo yum install -y libX11 libXcomposite libXcursor");
				printError("ERROR", "3. Check ChromeDriver version compatibility");
				printError("ERROR", "4. Verify /tmp directory permissions: ls -la /tmp");
				
				throw new RuntimeException("Failed to initialize Chrome WebDriver after trying all strategies", lastException);
			}
			
			// Navigate to login page
			printLog("INFO", "Navigating to VProple login page...");
			driver.get("https://expert.vprople.com/login");
			printLog("INFO", "Login page loaded");
			
			// Enter credentials
			printLog("INFO", "Entering login credentials...");
			waitTillElementVisible(By.xpath(".//input[@name='email']"), 30);
			printLog("DEBUG", "Email input field found and visible");
			driver.findElement(By.xpath(".//input[@name='email']")).sendKeys(usernameVprop);
			printLog("INFO", "Email entered: " + usernameVprop);
			
			waitForFixSeconds(1);
			
			waitTillElementVisible(By.xpath(".//input[@name='password']"), 30);
			printLog("DEBUG", "Password input field found and visible");
			driver.findElement(By.xpath(".//input[@name='password']")).sendKeys(passwordVprop);
			printLog("INFO", "Password entered (length: " + passwordVprop.length() + " characters)");
			
			waitForFixSeconds(1);
			
			// Submit login form
			printLog("INFO", "Submitting login form...");
			waitTillElementVisible(By.xpath(".//button[@type='submit']"), 30);
			driver.findElement(By.xpath(".//button[@type='submit']")).click();
			printLog("INFO", "Login form submitted");
			
			// Verify login success
			printLog("INFO", "Verifying login success...");
			waitTillElementVisible(By.xpath(".//header//span[text()='Himanshu']"), 30);
			printLog("INFO", "Login verification successful - user 'Himanshu' found in header");
			
			// Navigate to dashboard
			driver.get("https://expert.vprople.com/");
			printLog("INFO", "Navigated to dashboard");
			
			printLog("INFO", "Waiting 5 seconds for dashboard to fully load...");
			waitForFixSeconds(5);
			
			// Verify dashboard access
			if(driver.findElements(By.xpath(".//a[span[contains(text(),'Dashboard')]]")).size() > 0) {
				printLog("INFO", "SUCCESS: Login to VProple completed successfully - Dashboard accessible");
			} else {
				throw new RuntimeException("Login to VProple failed - 'Dashboard' element not found after login");
			}
			
			// Navigate to self-assign page
			printLog("INFO", "=== CHECKING SELF-ASSIGN INTERVIEWS ===");
			driver.get("https://expert.vprople.com/self-assign");
			printLog("INFO", "Navigated to self-assign page");
			
			waitTillElementVisible(By.xpath(".//h1[text()='Self Assign']"), 30);
			printLog("INFO", "Self-assign page loaded successfully");
			
			// Check for available interviews
			printLog("INFO", "Checking for available self-assign interviews...");
			if(driver.findElements(By.xpath(".//div[text()='No Self-Assign Interviews']")).size() > 0) {
				printLog("INFO", "RESULT: No self-assign interviews available at this time");
				printLog("INFO", "No action required - will check again in next cycle");
			} 
			else {
				printLog("WARN", "ALERT: Self-assign interviews are available!");
				printLog("INFO", "Proceeding to trigger phone call notification...");
				makeCall();
			}
			
		} catch (Exception e) {
			printError("ERROR", "CRITICAL ERROR during login and scheduling process: " + e.getMessage());
			printError("ERROR", "Exception type: " + e.getClass().getSimpleName());
			printError("ERROR", "Detailed stack trace:");
			e.printStackTrace();
			
			// Log additional context if available
			if (driver != null) {
			    try {
			        String currentUrl = driver.getCurrentUrl();
			        printError("ERROR", "Current page URL at error time: " + currentUrl);
			    } catch (Exception urlException) {
			        printLog("DEBUG", "Could not retrieve current URL: " + urlException.getMessage());
			    }
			}
			
		} finally {
			if (driver != null) {
				printLog("INFO", "Cleaning up WebDriver resources...");
				try {
				    driver.quit();
				    printLog("INFO", "WebDriver closed successfully");
				} catch (Exception e) {
				    printLog("WARN", "Exception while closing WebDriver: " + e.getMessage());
				}
			}
		}
		
		long methodDuration = System.currentTimeMillis() - methodStartTime;
		printLog("INFO", "loginAndScheduleVPropleInterview() completed in " + methodDuration + "ms");
		printLog("INFO", "<<< Exiting loginAndScheduleVPropleInterview() method");
    }

	private static void waitTillElementVisible(By locator, int timeoutSeconds) {
		printLog("DEBUG", "Waiting for element to be visible: " + locator.toString());
		printLog("DEBUG", "Timeout: " + timeoutSeconds + " seconds");
		long waitStartTime = System.currentTimeMillis();
		
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
			wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
			
			long waitDuration = System.currentTimeMillis() - waitStartTime;
			printLog("DEBUG", "Element became visible after " + waitDuration + "ms");
			
		} catch (Exception e) {
			long waitDuration = System.currentTimeMillis() - waitStartTime;
			printError("ERROR", "TIMEOUT: Element did not become visible within " + timeoutSeconds + " seconds");
			printError("ERROR", "Element locator: " + locator.toString());
			printError("ERROR", "Wait duration: " + waitDuration + "ms");
			printError("ERROR", "Exception details: " + e.getMessage());
			printError("ERROR", "Stack trace:");
			e.printStackTrace();
			throw e; // Re-throw to maintain original behavior
		}
	}

	private static void waitForFixSeconds(int seconds) {
		printLog("DEBUG", "Waiting for " + seconds + " seconds...");
		try {
			Thread.sleep(seconds * 1000);
			printLog("DEBUG", "Wait completed");
		} catch (InterruptedException e) {
			printLog("WARN", "Thread sleep was interrupted: " + e.getMessage());
			Thread.currentThread().interrupt(); // Restore interrupted status
		}
	}
	
	/**
	 * Checks if Chrome is properly installed and accessible
	 */
	private static void checkChromeInstallation() {
		printLog("INFO", "=== DIAGNOSING CHROME SETUP ===");
		
		// Check Java system properties
		printLog("INFO", "Java Version: " + System.getProperty("java.version"));
		printLog("INFO", "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
		
		// Try to run Chrome directly to check if it's working
		String[] possibleChromePaths = {
			"/usr/bin/google-chrome",
			"/usr/bin/google-chrome-stable", 
			"/usr/bin/chromium-browser",
			"/usr/bin/chromium",
			"/opt/google/chrome/google-chrome",
			"google-chrome",  // Try PATH
			"chromium-browser" // Try PATH
		};
		
		printLog("INFO", "Testing Chrome executable:");
		String workingChrome = null;
		for (String chromePath : possibleChromePaths) {
			try {
				printLog("INFO", "Testing: " + chromePath);
				ProcessBuilder pb = new ProcessBuilder(chromePath, "--version", "--no-sandbox");
				pb.redirectErrorStream(true);
				Process process = pb.start();
				
				// Read output
				BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
				String line;
				StringBuilder output = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
				}
				
				int exitCode = process.waitFor();
				if (exitCode == 0) {
					printLog("INFO", "✓ SUCCESS: " + chromePath + " is working");
					printLog("INFO", "Chrome version: " + output.toString().trim());
					workingChrome = chromePath;
					break;
				} else {
					printLog("WARN", "✗ FAILED: " + chromePath + " exit code: " + exitCode);
					printLog("WARN", "Output: " + output.toString().trim());
				}
			} catch (Exception e) {
				printLog("WARN", "✗ ERROR testing " + chromePath + ": " + e.getMessage());
			}
		}
		
		if (workingChrome == null) {
			printError("ERROR", "=== CHROME EXECUTION FAILED ===");
			printError("ERROR", "Chrome is installed but cannot execute properly");
			printError("ERROR", "Common fixes:");
			printError("ERROR", "1. Install missing dependencies: sudo yum install -y xorg-x11-server-Xvfb gtk3 libXcomposite libXcursor libXdamage libXext libXi libXtst cups-libs libXss libXrandr libasound2");
			printError("ERROR", "2. Or for Ubuntu: sudo apt-get install -y fonts-liberation libasound2 libatk-bridge2.0-0 libdrm2 libgtk-3-0 libnspr4 libnss3 xdg-utils");
			printError("ERROR", "3. Check permissions: sudo chmod +x /usr/bin/google-chrome*");
		} else {
			printLog("INFO", "Chrome is functional at: " + workingChrome);
		}
		
		// Check system resources
		long maxMemory = Runtime.getRuntime().maxMemory();
		long freeMemory = Runtime.getRuntime().freeMemory();
		printLog("INFO", "Available memory: " + (maxMemory / 1024 / 1024) + " MB");
		printLog("INFO", "Free memory: " + (freeMemory / 1024 / 1024) + " MB");
		
		// Check environment
		String display = System.getenv("DISPLAY");
		printLog("INFO", "DISPLAY env var: " + (display != null ? display : "not set (good for headless)"));
	}
	
	/**
	 * Finds the Chrome executable from a list of possible paths
	 */
	private static String findChromeExecutable(String[] paths) {
		for (String path : paths) {
			File chromeFile = new File(path);
			if (chromeFile.exists() && chromeFile.canExecute()) {
				return path;
			}
		}
		return null;
	}

}