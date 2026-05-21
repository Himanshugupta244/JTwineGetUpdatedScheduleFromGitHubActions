package GmailProjectForGitHubActions;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class ScheduleJtwineFinalCodeHim {
    
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String EMAIL = System.getenv("JTWINE_EMAIL_HIM");
    private static final String PASSWORD = System.getenv("JTWINE_PASSWORD_HIM");
    private static Map<String, Integer> candidateAttempts = new HashMap<>(); // Track attempts per candidate (max 4)

    // PostgreSQL DB credentials
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    private static String lastTriedForTime = null; // Stores selected time text before final schedule click

    // Shared daemon executor for fire-and-forget DB inserts — main thread never waits
    private static final java.util.concurrent.ExecutorService DB_EXECUTOR =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "db-logger");
            t.setDaemon(true); // won't block JVM shutdown
            return t;
        });
    
    public static void main(String[] args) {
        // Outermost restart loop — replaces all recursive main() recovery calls
        while (true) {
            try {
                setupDriver();
            } catch (Throwable t) {
                System.out.println("❌ setupDriver failed before main loop: " + t.getMessage());
                try { Thread.sleep(10000); } catch (Throwable ignored) {}
                continue;
            }

            try {
                // Initial login (one time only)
                performLogin();
                System.out.println("\n🚀 LOGIN SUCCESSFUL - Starting infinite monitoring loop...\n");

                // INFINITE LOOP - Never stops, runs forever!
                int cycleCount = 0;
                while (true) {
                    cycleCount++;
                    // Clear memory every 30 minutes (360 cycles × 5 seconds = 1800 seconds = 30 minutes)
                    if (cycleCount % 360 == 0) {
                        candidateAttempts.clear();
                        System.out.println("🧹 Memory cleared - Will re-process all candidates again");
                    }

                    try {
                        StringBuilder separator = new StringBuilder();
                        for (int i = 0; i < 60; i++) separator.append("=");
                        System.out.println("\n" + separator.toString());
                        System.out.println("🔄 CYCLE #" + cycleCount + " - Checking for new candidates...");
                        System.out.println(separator.toString());

                        // Refresh page and process candidates
                        processInterviewScheduling();

                        System.out.println("\n✅ CYCLE #" + cycleCount + " COMPLETED");
                        System.out.println("⏳ Waiting 5 seconds before next check...\n");

                        // Wait exactly 5 seconds before next cycle
                        Thread.sleep(5000);

                    } catch (Throwable e) {
                        System.out.println("❌ Error in cycle #" + cycleCount + ": " + e.getMessage());
                        System.out.println("🔄 Continuing to next cycle in 5 seconds...");
                        try { Thread.sleep(5000); } catch (Throwable ignored) {}
                    }
                }

            } catch (Throwable e) {
                System.out.println("💥 RECOVERABLE ERROR - will restart: " + e.getMessage());
                try { if (driver != null) driver.quit(); } catch (Throwable ignored) {}
                System.out.println("🔄 Restarting in 10 seconds...");
                try { Thread.sleep(10000); } catch (Throwable ignored) {}
                // continue → outer while(true) restarts setupDriver + performLogin cleanly
            }
        }
    }
    
    private static void setupDriver() {
        while (true) {
            try {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--disable-blink-features=AutomationControlled");
                options.addArguments("--disable-extensions");
                options.addArguments("--headless");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--remote-allow-origins=*");

                driver = new ChromeDriver(options);
                wait = new WebDriverWait(driver, Duration.ofSeconds(30));

                // Force browser timezone to IST (Asia/Kolkata, UTC+5:30)
                Map<String, Object> cdpParams = new HashMap<>();
                cdpParams.put("timezoneId", "Asia/Kolkata");
                ((ChromeDriver) driver).executeCdpCommand("Emulation.setTimezoneOverride", cdpParams);
                System.out.println("Browser timezone set to IST (Asia/Kolkata)");

                System.out.println("Driver setup completed successfully");
                return; // success — exit retry loop
            } catch (Throwable e) {
                System.out.println("❌ Error setting up driver: " + e.getMessage());
                System.out.println("🔄 Retrying driver setup in 10 seconds...");
                try { Thread.sleep(10000); } catch (Throwable ignored) {}
            }
        }
    }
    
    private static void performLogin() {
        while (true) {
            try {
                System.out.println("Step 1: Opening URL: https://app.jobtwine.com/signin");
                driver.get("https://app.jobtwine.com/signin");

                // Step 2: Enter Email ID
                System.out.println("Step 2: Entering email");
                WebElement emailInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(.//input[@placeholder='Example@gmail.com'])[1]")));
                emailInput.clear();
                emailInput.sendKeys(EMAIL);

                // Step 3: Click Next Button
                System.out.println("Step 3: Clicking Next button");
                WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Next')]")));
                nextButton.click();

                // Step 4: Wait for Password field
                System.out.println("Step 4: Waiting for password field");
                WebElement passwordInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(.//input[@placeholder='Enter your password'])[1]")));

                // Step 5: Enter Password
                System.out.println("Step 5: Entering password");
                passwordInput.clear();
                passwordInput.sendKeys(PASSWORD);

                // Step 6: Click Submit
                System.out.println("Step 6: Clicking submit button");
                WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(".//button[contains(text(),'Sign In')]")));
                submitButton.click();

                // Step 7: Wait for successful login
                System.out.println("Step 7: Waiting for successful login");
                wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("candidates"),
                    ExpectedConditions.urlContains("interviewer/candidates"),
                    ExpectedConditions.presenceOfElementLocated(By.xpath(".//span[contains(text(),'page ')]/following-sibling::span[contains(text(),'4')]"))
                ));

                System.out.println("Login successful!");
                return; // success — exit retry loop
            } catch (Throwable e) {
                System.out.println("❌ Login failed: " + e.getMessage());
                System.out.println("🔄 Retrying login in 15 seconds...");
                try { Thread.sleep(15000); } catch (Throwable ignored) {}
            }
        }
    }
    
    private static void processInterviewScheduling() {
        try {
            // Step 8: Always refresh the schedule candidates page (EVERY 5 SECONDS)
            System.out.println("🔄 REFRESHING schedule candidates page...");
            driver.get("https://app.jobtwine.com/interviewer/schedule-candidates");
            
            // Wait for page to fully load
            System.out.println("⏳ Waiting for page to load...");
            Thread.sleep(5000);
            
            // Check for interview cards
            System.out.println("🔍 SCANNING for interview cards...");
            List<WebElement> jobCards = driver.findElements(By.xpath(".//div[@class='job-item']"));
            
            if (jobCards.isEmpty()) {
                System.out.println("📭 NO interview cards found - no interviews lined up");
                
                // Clear memory when no candidates are found
                candidateAttempts.clear();
                System.out.println("🧹 Memory cleared - No candidates found, starting fresh");
                
                System.out.println("✅ Page check complete - will check again in 5 seconds");
                return;
            }
            
            System.out.println("📋 FOUND " + jobCards.size() + " interview card(s) - Processing candidates...");
            
            // Track candidates before processing for memory cleanup
            Set<String> candidatesBeforeProcessing = getCurrentCandidateNames();
            
            // Check if any candidates are new (not yet attempted)
            List<WebElement> candidateDetails = driver.findElements(By.xpath(".//div[@class='candidate-details-sec']"));
            boolean hasNewCandidates = false;
            
            for (int i = 0; i < candidateDetails.size(); i++) {
                WebElement candidateSection = candidateDetails.get(i);
                String candidateName = getCandidateName(candidateSection);
                if (candidateName != null && !candidateAttempts.containsKey(candidateName)) {
                    hasNewCandidates = true;
                    break;
                }
            }
            
            if (hasNewCandidates) {
                System.out.println("✅ NEW CANDIDATES DETECTED - Processing 1st cycle (2 attempts each)");
            } else {
                System.out.println("🔄 NO NEW CANDIDATES - Processing 2nd cycle (attempts 3+4 for existing candidates)");
            }
            
            // Process each candidate
            
            for (int i = 0; i < candidateDetails.size(); i++) {
                try {
                    System.out.println("\n--- Processing Candidate #" + (i + 1) + " ---");
                    
                    // Refresh elements to avoid stale reference
                    candidateDetails = driver.findElements(By.xpath(".//div[@class='candidate-details-sec']"));
                    if (i >= candidateDetails.size()) {
                        System.out.println("❌ No more candidates to process");
                        break;
                    }
                    
                    WebElement candidateSection = candidateDetails.get(i);
                    
                    // Step 9: Get candidate name
                    String candidateName = getCandidateName(candidateSection);
                    if (candidateName == null || candidateName.trim().isEmpty()) {
                        System.out.println("❌ Could not get candidate name, skipping...");
                        continue;
                    }
                    
                    System.out.println("👤 Processing candidate: " + candidateName);
                    
                    int currentAttempts = candidateAttempts.getOrDefault(candidateName, 0);
                    System.out.println("📊 Current attempts for " + candidateName + ": " + currentAttempts);
                    
                    // Skip if candidate has reached maximum 4 attempts
                    if (currentAttempts >= 4) {
                        System.out.println("✅ Candidate " + candidateName + " has reached maximum 4 attempts, skipping...");
                        continue;
                    }
                    
                    // FIXED SKIP LOGIC - Priority-based processing
                    if (hasNewCandidates && currentAttempts >= 2) {
                        System.out.println("⏭️ Candidate " + candidateName + " already has 2+ attempts. Processing new candidates first.");
                        continue;
                    }
                    // REMOVED PROBLEMATIC LOGIC: Candidates with <2 attempts should NOT be skipped
                    // This allows John(1) to complete his Phase 1 even when no new candidates exist
                    
                    // Step 10: Click Schedule Interview button
                    if (!clickScheduleButton(candidateSection)) {
                        System.out.println("❌ Could not find schedule button for " + candidateName);
                        continue;
                    }
                    
                    // Process candidate based on current phase
                    int newAttempts;
                    if (hasNewCandidates || currentAttempts < 2) {
                        // Phase 1: New candidates or completing first 2 attempts
                        newAttempts = scheduleTimeSlotsPhase1(candidateName, currentAttempts);
                        System.out.println("🎨 Phase 1 completed for " + candidateName + ": " + newAttempts + " new attempts");
                    } else {
                        // Phase 2: Existing candidates, try all 4 button positions
                        newAttempts = scheduleTimeSlotsPhase2(candidateName, currentAttempts);
                        System.out.println("🎨 Phase 2 completed for " + candidateName + ": " + newAttempts + " new attempts");
                    }
                    
                    // Update total attempts
                    candidateAttempts.put(candidateName, currentAttempts + newAttempts);
                    System.out.println("✅ Candidate " + candidateName + " total attempts: " + (currentAttempts + newAttempts));
                    
                    // Always refresh page after processing each candidate
                    System.out.println("🔄 Refreshing page for next candidate...");
                    driver.get("https://app.jobtwine.com/interviewer/schedule-candidates");
                    Thread.sleep(3000);
                    
                } catch (Throwable e) {
                    System.out.println("❌ Error processing candidate: " + e.getMessage());
                    // Navigate back to main page and continue with next candidate
                    System.out.println("🔄 Refreshing page after error...");
                    try { driver.get("https://app.jobtwine.com/interviewer/schedule-candidates"); } catch (Throwable ignored) {}
                    try { Thread.sleep(3000); } catch (Throwable ignored) {}
                }
            }
            
            // Clean memory for disappeared candidates (targeted cleanup)
            cleanMemoryForDisappearedCandidates(candidatesBeforeProcessing);
            
            System.out.println("✅ ALL candidates in this cycle processed");
            
        } catch (Throwable e) {
            System.out.println("❌ ERROR in interview scheduling process: " + e.getMessage());
            System.out.println("🔄 Will retry in next cycle...");
        }
    }
    
    private static String getCandidateName(WebElement candidateSection) {
        try {
            // Try the primary xpath first
            WebElement nameElement = candidateSection.findElement(By.xpath(".//a/span"));
            return nameElement.getText().trim();
        } catch (Exception e) {
            try {
                // Alternative xpath
                WebElement nameElement = candidateSection.findElement(By.xpath(".//div[@class='candidate-details-sec']/div/div/a/span"));
                return nameElement.getText().trim();
            } catch (Exception e2) {
                try {
                    // Another alternative
                    WebElement nameElement = candidateSection.findElement(By.xpath(".//*//span"));
                    return nameElement.getText().trim();
                } catch (Exception e3) {
                    System.out.println("Could not find candidate name with any xpath: " + e3.getMessage());
                    return null;
                }
            }
        }
    }
    
    private static boolean clickScheduleButton(WebElement candidateSection) {
        try {
            WebElement scheduleButton = candidateSection.findElement(By.xpath(".//button[contains(text(),' Schedule Interview ')]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", scheduleButton);
            System.out.println("Clicked Schedule Interview button");
            Thread.sleep(2000); // Wait for popup
            return true;
        } catch (Exception e) {
            System.out.println("Could not find or click schedule button: " + e.getMessage());
            return false;
        }
    }
    
    // Phase 1: Process new candidates (max 2 attempts per candidate)
    private static int scheduleTimeSlotsPhase1(String candidateName, int currentAttempts) {
        int maxAttempts = Math.min(2, 4 - currentAttempts); // Don't exceed 4 total
        int completedAttempts = 0;
        
        for (int attemptIndex = 1; attemptIndex <= maxAttempts; attemptIndex++) {
            int buttonPosition = currentAttempts + attemptIndex; // Which button to try (1,2,3,4)
            
            try {
                System.out.println("PHASE 1 - Attempting button position " + buttonPosition + " for " + candidateName + " (max 2 per round)");
                
                // Click Schedule Interview button if not first attempt
                if (attemptIndex > 1) {
                    if (!findAndClickScheduleButtonForCandidate(candidateName)) {
                        System.out.println("Could not find schedule button for button position " + buttonPosition + " for " + candidateName);
                        break;
                    }
                }
                
                // Try to click the AM/PM button at this position
                boolean buttonClicked = tryClickAMPMButton(buttonPosition, candidateName);
                completedAttempts = attemptIndex; // Count this attempt regardless of success
                
                if (!buttonClicked) {
                    System.out.println("Button position " + buttonPosition + " not available for " + candidateName + " - attempt counted");
                    continue; // Count the attempt but continue to next
                }
                
                // Click final Schedule button
                if (!clickFinalScheduleButton(buttonPosition, candidateName)) {
                    continue;
                }
                
                // Check if candidate still exists
                try { driver.get("https://app.jobtwine.com/interviewer/schedule-candidates"); } catch (Throwable ignored) {}
                try { Thread.sleep(3000); } catch (Throwable ignored) {}
                
                if (!candidateStillExists(candidateName)) {
                    System.out.println("Now Candidate " + candidateName + " no longer exists after button position " + buttonPosition + " - Moving to next candidate");
                    return completedAttempts;
                }
                
                System.out.println("Candidate " + candidateName + " still exists - Will try button position " + (buttonPosition + 1));
                
            } catch (Throwable e) {
                completedAttempts = attemptIndex; // Count the attempt even if it failed
                System.out.println("Error attempting button position " + buttonPosition + " for " + candidateName + ": " + e.getMessage());
            }
        }
        
        System.out.println("PHASE 1 COMPLETE: Candidate " + candidateName + " completed " + completedAttempts + " attempts this round");
        return completedAttempts;
    }
    
    // Phase 2: Process existing candidates (try all 4 button positions)
    private static int scheduleTimeSlotsPhase2(String candidateName, int currentAttempts) {
        int attemptsToMake = 4 - currentAttempts; // How many more attempts to reach 4 total
        int completedAttempts = 0;
        
        for (int attemptIndex = 1; attemptIndex <= attemptsToMake; attemptIndex++) {
            int buttonPosition = currentAttempts + attemptIndex; // Which button to try (1,2,3,4)
            
            try {
                System.out.println("PHASE 2 - Attempting button position " + buttonPosition + " for " + candidateName + " (completing 4 total attempts)");
                
                // Click Schedule Interview button if not first attempt in this phase
                if (attemptIndex > 1) {
                    if (!findAndClickScheduleButtonForCandidate(candidateName)) {
                        System.out.println("Could not find schedule button for button position " + buttonPosition + " for " + candidateName);
                        completedAttempts = attemptIndex; // Count the attempt
                        continue;
                    }
                }
                
                // Try to click the AM/PM button at this position
                boolean buttonClicked = tryClickAMPMButton(buttonPosition, candidateName);
                completedAttempts = attemptIndex; // Count this attempt regardless of success
                
                if (!buttonClicked) {
                    System.out.println("Button position " + buttonPosition + " not available for " + candidateName + " - attempt counted, continuing to next position");
                    continue; // Count the attempt but try next button position
                }
                
                // Click final Schedule button
                if (!clickFinalScheduleButton(buttonPosition, candidateName)) {
                    continue;
                }
                
                // Check if candidate still exists
                try { driver.get("https://app.jobtwine.com/interviewer/schedule-candidates"); } catch (Throwable ignored) {}
                try { Thread.sleep(3000); } catch (Throwable ignored) {}
                
                if (!candidateStillExists(candidateName)) {
                    System.out.println("SUCCESS: Candidate " + candidateName + " no longer exists after button position " + buttonPosition + " - Moving to next candidate");
                    return completedAttempts;
                }
                
                System.out.println("Candidate " + candidateName + " still exists - Will try button position " + (buttonPosition + 1));
                
            } catch (Throwable e) {
                completedAttempts = attemptIndex; // Count the attempt even if it failed
                System.out.println("Error attempting button position " + buttonPosition + " for " + candidateName + ": " + e.getMessage());
            }
        }
        
        System.out.println("PHASE 2 COMPLETE: Candidate " + candidateName + " completed " + completedAttempts + " attempts this round (total: " + (currentAttempts + completedAttempts) + ")");
        return completedAttempts;
    }
    
    // Helper method to try clicking AM/PM button at specific position
    private static boolean tryClickAMPMButton(int buttonPosition, String candidateName) {
        try {
            String timeSlotXpath = "(.//div[contains(text(),'AM') or contains(text(),'PM')])[" + buttonPosition + "]";
            WebElement timeSlot = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(timeSlotXpath)));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", timeSlot);
            System.out.println("Successfully clicked button position " + buttonPosition + " for " + candidateName);
            Thread.sleep(1000);
            if(driver.findElements(By.xpath(".//h5[contains(text(),'Schedule Interview')]/parent::div/following-sibling::div/p[contains(text(),'Time selected is ')]")).size()>0) {
            	lastTriedForTime = driver.findElement(By.xpath(".//h5[contains(text(),'Schedule Interview')]/parent::div/following-sibling::div/p[contains(text(),'Time selected is ')]/span")).getText();
            	System.out.println("For Candidate :: " + candidateName + " -> Date and Time going to select is :: " + lastTriedForTime);
            } else {
            	lastTriedForTime = null;
            }
            Thread.sleep(500);
            return true;
        } catch (TimeoutException e) {
            System.out.println("Button position " + buttonPosition + " not found for " + candidateName);
            return false;
        } catch (Exception e) {
            System.out.println("Error clicking button position " + buttonPosition + " for " + candidateName + ": " + e.getMessage());
            return false;
        }
    }
    
    // Helper method to click final Schedule button
    private static boolean clickFinalScheduleButton(int buttonPosition, String candidateName) {
        try {
            WebElement finalScheduleButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(".//mat-progress-bar/following-sibling::button[contains(text(),'Schedule')]")
            ));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", finalScheduleButton);
            System.out.println("Clicked final Schedule button for button position " + buttonPosition + " for " + candidateName);
            try {
				Thread.sleep(2000);
				System.out.println("The message I got after clicking on final button for candidate :: " + candidateName + " is :: ");
				String primaryMsg = null;
				String secondaryMsg = null;
				if(driver.findElements(By.xpath("(.//*[@id='swal2-content']/div/p)[1]"  )).size()>0) {
					primaryMsg = driver.findElement(By.xpath("(.//*[@id='swal2-content']/div/p)[1]")).getText();
					System.out.println(primaryMsg);
				}
				if(driver.findElements(By.xpath("(.//*[@id='swal2-content']/div/p)[2]"  )).size()>0) {
					secondaryMsg = driver.findElement(By.xpath("(.//*[@id='swal2-content']/div/p)[2]")).getText();
					System.out.println(secondaryMsg);
				}
				insertCandidateLog(candidateName, lastTriedForTime, primaryMsg, secondaryMsg, buttonPosition);
            } catch (InterruptedException e) {
			} catch (Throwable dbIgnored) {
				System.out.println("⚠️ DB call skipped (non-fatal): " + dbIgnored.getMessage());
			}
            System.out.println();
            return true;
        } catch (TimeoutException e) {
            System.out.println("Could not find final schedule button for button position " + buttonPosition);
            return false;
        } catch (Throwable e) {
            System.out.println("Error in clickFinalScheduleButton for " + candidateName + ": " + e.getMessage());
            return false;
        }
    }
    
    private static boolean candidateStillExists(String candidateName) {
        try {
            List<WebElement> nameElements = driver.findElements(By.xpath(".//div[@class='candidate-details-sec']/div/div/a/span"));
            for (WebElement element : nameElements) {
                if (candidateName.equals(element.getText().trim())) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            System.out.println("Error checking if candidate exists: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean findAndClickScheduleButtonForCandidate(String candidateName) {
        try {
            System.out.println("Looking for schedule button for candidate: " + candidateName);
            List<WebElement> candidateSections = driver.findElements(By.xpath(".//div[@class='candidate-details-sec']"));
            System.out.println("Found " + candidateSections.size() + " candidate sections");
            
            for (int i = 0; i < candidateSections.size(); i++) {
                try {
                    WebElement section = candidateSections.get(i);
                    // Fixed xpath - we're already inside candidate-details-sec, so we don't need to repeat it
                    WebElement nameElement = section.findElement(By.xpath(".//a/span"));
                    String foundName = nameElement.getText().trim();
                    System.out.println("Found candidate name: '" + foundName + "', looking for: '" + candidateName + "'");
                    
                    if (candidateName.equals(foundName)) {
                        System.out.println("Match found! Looking for schedule button...");
                        WebElement scheduleButton = section.findElement(By.xpath(".//button[contains(text(),' Schedule Interview ')]"));
                        System.out.println("Schedule button found, clicking...");
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", scheduleButton);
                        Thread.sleep(2000);
                        System.out.println("Schedule button clicked successfully for " + candidateName);
                        return true;
                    }
                } catch (Exception e) {
                    System.out.println("Error processing candidate section " + i + ": " + e.getMessage());
                    continue;
                }
            }
            System.out.println("No matching candidate found for: " + candidateName);
            return false;
        } catch (Throwable e) {
            System.out.println("Error finding schedule button for candidate: " + e.getMessage());
            return false;
        }
    }
    
    // Helper method to get current candidate names on page
    private static Set<String> getCurrentCandidateNames() {
        Set<String> candidateNames = new HashSet<>();
        try {
            List<WebElement> candidateDetails = driver.findElements(By.xpath(".//div[@class='candidate-details-sec']"));
            for (WebElement candidateSection : candidateDetails) {
                String candidateName = getCandidateName(candidateSection);
                if (candidateName != null && !candidateName.trim().isEmpty()) {
                    candidateNames.add(candidateName);
                }
            }
        } catch (Throwable e) {
            System.out.println("Error getting current candidate names: " + e.getMessage());
        }
        return candidateNames;
    }
    
    private static void insertCandidateLog(String candidateName, String triedForTime, String primaryMsg, String secondaryMsg, int attemptCounter) {
        // Fire-and-forget: submit to background daemon thread, main thread returns instantly
        try {
            DB_EXECUTOR.submit(() -> {
                try {
                    String sql = "INSERT INTO candidatelogshim (candidate_name, tried_for_time, primary_message, secondary_message, attempt_counter) VALUES (?, ?, ?, ?, ?)";
                    java.sql.DriverManager.setLoginTimeout(10);
                    try (java.sql.Connection conn = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                         java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, candidateName);
                        ps.setString(2, triedForTime);
                        ps.setString(3, primaryMsg);
                        ps.setString(4, secondaryMsg);
                        ps.setInt(5, attemptCounter);
                        ps.executeUpdate();
                        System.out.println("✅ DB LOG: Inserted record for " + candidateName + " (attempt " + attemptCounter + ")");
                    }
                } catch (Throwable t) {
                    System.out.println("❌ DB LOG: Failed to insert for " + candidateName + " (non-fatal): " + t.getMessage());
                }
            });
        } catch (Throwable t) {
            System.out.println("⚠️ DB LOG: Could not submit DB task (non-fatal): " + t.getMessage());
        }
    }

    private static java.sql.Timestamp parseTimestamp(String timeText) {
        try {
            if (timeText == null || timeText.trim().isEmpty()) return null;
            String cleaned = timeText.replaceAll("\\s+IST$", "").replaceAll("\\s+UTC.*$", "").trim();
            String[] patterns = {
                "EEE, MMM d, yyyy, hh:mm a",
                "EEE, MMM d, yyyy, h:mm a",
                "EEE MMM d, yyyy hh:mm a",
                "MMM d, yyyy hh:mm a",
                "MMM d, yyyy h:mm a"
            };
            for (String pattern : patterns) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH);
                    sdf.setLenient(false);
                    java.util.Date date = sdf.parse(cleaned);
                    return new java.sql.Timestamp(date.getTime());
                } catch (Throwable ignored) {}
            }
            System.out.println("⚠️ DB LOG: Could not parse timestamp '" + timeText + "' - storing null");
        } catch (Throwable t) {
            System.out.println("⚠️ DB LOG: parseTimestamp error (non-fatal): " + t.getMessage());
        }
        return null;
    }

    // Targeted memory cleanup - remove only disappeared candidates
    private static void cleanMemoryForDisappearedCandidates(Set<String> candidatesBeforeProcessing) {
        try {
            Set<String> candidatesAfterProcessing = getCurrentCandidateNames();
            
            // Find candidates that disappeared
            for (String candidateName : candidatesBeforeProcessing) {
                if (!candidatesAfterProcessing.contains(candidateName) && candidateAttempts.containsKey(candidateName)) {
                    candidateAttempts.remove(candidateName);
                    System.out.println("✨ TARGETED CLEANUP: " + candidateName + " disappeared - Memory cleared for fresh chance if returns");
                }
            }
        } catch (Throwable e) {
            System.out.println("Error in targeted memory cleanup: " + e.getMessage());
        }
    }
}
