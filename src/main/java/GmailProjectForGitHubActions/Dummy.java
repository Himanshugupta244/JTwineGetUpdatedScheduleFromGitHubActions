package GmailProjectForGitHubActions;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

public class Dummy {

	public static WebDriver driver;

	public static void main(String[] args) {
		try {
			// STEP 1: Headless login
			System.out.println("[1/2] Logging in (background)...");
			WebDriverManager.chromedriver().setup();
			ChromeOptions headlessOpts = new ChromeOptions();
			headlessOpts.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
			driver = new ChromeDriver(headlessOpts);
			setTimezoneToIST(driver);
			loginToJTwine();

			// STEP 2: Capture session & build single Base64 blob
			System.out.println("[2/2] Capturing session & generating HTML...");
			Set<Cookie> cookies = driver.manage().getCookies();
			String localStorage = (String) ((ChromeDriver) driver).executeScript(
				"var items={}; for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);items[k]=localStorage.getItem(k);} return JSON.stringify(items);"
			);
			String sessionStorage = (String) ((ChromeDriver) driver).executeScript(
				"var items={}; for(var i=0;i<sessionStorage.length;i++){var k=sessionStorage.key(i);items[k]=sessionStorage.getItem(k);} return JSON.stringify(items);"
			);
			driver.quit();
			driver = null;

			// Build a JSON object with all session data
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
			allData.append("],\"ls\":").append(localStorage)
				.append(",\"ss\":").append(sessionStorage).append("}");

			String base64Blob = Base64.getEncoder().encodeToString(allData.toString().getBytes(StandardCharsets.UTF_8));

			// Generate dummy.html
			generateHtml(base64Blob);

			System.out.println("\n==========================================");
			System.out.println("  deploy/dummy.html generated!");
			System.out.println("==========================================");
			System.out.println("\nKya karna hai:");
			System.out.println("1. dummy.html kholo browser mein");
			System.out.println("2. 'COPY SESSION DATA' button click karo");
			System.out.println("3. jobtwine.com kholo");
			System.out.println("4. Address bar mein bookmarklet click karo");
			System.out.println("5. Prompt aayega -> Ctrl+V -> OK");
			System.out.println("6. LOGGED IN!");

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (driver != null) driver.quit();
		}
	}

	private static void generateHtml(String base64Blob) throws Exception {
		String timestamp = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))
			.format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a"));

		// Bookmarklet JS: prompt for paste -> decode -> inject -> reload
		String bookmarklet = "javascript:void((function(){var d=prompt('Session data paste karo:');"
			+ "if(!d)return;try{var obj=JSON.parse(atob(d));"
			+ "obj.cookies.forEach(function(c){document.cookie=c.n+'='+c.v+';domain='+c.d+';path='+c.p+(c.s?';secure':'');});"
			+ "var ls=obj.ls;for(var k in ls){localStorage.setItem(k,ls[k]);}"
			+ "var ss=obj.ss;for(var k in ss){sessionStorage.setItem(k,ss[k]);}"
			+ "location.reload();}catch(e){alert('Error: '+e.message);}})())";

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>\n");
		html.append("<meta name='viewport' content='width=device-width,initial-scale=1.0'>\n");
		html.append("<title>JTwine Auto-Login</title>\n");
		html.append("<style>\n");
		html.append("@import url('https://fonts.googleapis.com/css2?family=Barlow:wght@600;700;800;900&display=swap');\n");
		html.append("*{box-sizing:border-box;font-family:'Barlow','Segoe UI',sans-serif;margin:0;padding:0}\n");
		html.append("body{background:#0f172a;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}\n");
		html.append(".card{background:#fff;border-radius:16px;max-width:480px;width:100%;overflow:hidden;box-shadow:0 20px 60px rgba(0,0,0,0.3)}\n");
		html.append(".header{background:linear-gradient(135deg,#1d4ed8,#7c3aed);padding:24px 28px;color:#fff}\n");
		html.append(".header h1{font-size:22px;font-weight:900;letter-spacing:1px}\n");
		html.append(".header p{font-size:13px;opacity:0.8;margin-top:4px;font-weight:600}\n");
		html.append(".body{padding:28px}\n");
		html.append(".step{display:flex;align-items:flex-start;margin-bottom:20px}\n");
		html.append(".num{min-width:32px;height:32px;line-height:32px;text-align:center;background:#1d4ed8;color:#fff;font-weight:900;font-size:14px;border-radius:50%;margin-right:12px}\n");
		html.append(".step-text{font-size:15px;font-weight:700;line-height:1.5;color:#1e293b}\n");
		html.append(".step-text small{display:block;font-size:12px;color:#94a3b8;font-weight:600;margin-top:2px}\n");
		html.append(".copy-btn{display:block;width:100%;padding:16px;font-size:17px;font-weight:900;letter-spacing:1px;color:#fff;background:linear-gradient(135deg,#059669,#047857);border:none;border-radius:10px;cursor:pointer;margin:16px 0;transition:transform 0.15s,box-shadow 0.15s;box-shadow:0 4px 15px rgba(5,150,105,0.4)}\n");
		html.append(".copy-btn:hover{transform:translateY(-2px);box-shadow:0 6px 20px rgba(5,150,105,0.5)}\n");
		html.append(".copy-btn:active{transform:translateY(0)}\n");
		html.append(".copy-btn.copied{background:linear-gradient(135deg,#16a34a,#15803d)}\n");
		html.append(".bm-link{display:block;text-align:center;padding:14px;font-size:15px;font-weight:900;letter-spacing:1px;color:#fff;background:linear-gradient(135deg,#7c3aed,#6d28d9);border-radius:10px;text-decoration:none;cursor:grab;margin:16px 0;box-shadow:0 4px 15px rgba(124,58,237,0.4);transition:transform 0.15s}\n");
		html.append(".bm-link:hover{transform:translateY(-2px)}\n");
		html.append(".bm-link:active{cursor:grabbing}\n");
		html.append(".hint{font-size:12px;color:#94a3b8;font-weight:600;text-align:center;font-style:italic}\n");
		html.append(".divider{border:none;border-top:2px dashed #e2e8f0;margin:20px 0}\n");
		html.append(".footer{text-align:center;padding:12px;font-size:11px;color:#64748b;font-weight:700;background:#f8fafc;border-top:1px solid #e2e8f0}\n");
		html.append("</style></head><body>\n");
		html.append("<div class='card'>\n");
		html.append("<div class='header'><h1>JTwine Auto-Login</h1><p>Session captured: ").append(timestamp).append("</p></div>\n");
		html.append("<div class='body'>\n");

		// ONE-TIME SETUP section
		html.append("<div class='step'><div class='num'>!</div><div class='step-text'>ONE TIME: Is button ko bookmarks bar mein DRAG karo:<small>Ye sirf ek baar karna hai</small></div></div>\n");
		html.append("<a class='bm-link' href=\"").append(bookmarklet.replace("\"", "&quot;")).append("\">&#128275; JTwine Login</a>\n");
		html.append("<p class='hint'>&#8593; Isko DRAG karke Bookmarks Bar mein daalo</p>\n");

		html.append("<hr class='divider'>\n");

		// DAILY USE section
		html.append("<div class='step'><div class='num'>1</div><div class='step-text'>Ye button click karo — data copy ho jaayega</div></div>\n");
		html.append("<button class='copy-btn' id='copyBtn' onclick='copyData()'>&#128203; COPY SESSION DATA</button>\n");

		html.append("<div class='step'><div class='num'>2</div><div class='step-text'>jobtwine.com kholo (ya koi bhi meeting link)</div></div>\n");
		html.append("<div style='text-align:center'><a href='https://www.jobtwine.com' target='_blank' rel='noopener' style='color:#1d4ed8;font-weight:800;font-size:14px'>jobtwine.com kholo &#8599;</a></div>\n");

		html.append("<div class='step' style='margin-top:18px'><div class='num'>3</div><div class='step-text'>Bookmarks bar mein <b>\"JTwine Login\"</b> click karo<small>Prompt aayega &#8594; Ctrl+V &#8594; OK &#8594; DONE!</small></div></div>\n");
		html.append("</div>\n");

		html.append("<div class='footer'>Browser band mat karo jab tak kaam na ho jaye</div>\n");
		html.append("</div>\n");

		// Hidden data + copy script
		html.append("<textarea id='sessionData' style='display:none'>").append(base64Blob).append("</textarea>\n");
		html.append("<script>\n");
		html.append("function copyData(){\n");
		html.append("  var el=document.getElementById('sessionData');\n");
		html.append("  navigator.clipboard.writeText(el.value).then(function(){\n");
		html.append("    var btn=document.getElementById('copyBtn');\n");
		html.append("    btn.textContent='\\u2705 COPIED!';\n");
		html.append("    btn.classList.add('copied');\n");
		html.append("    setTimeout(function(){btn.textContent='\\uD83D\\uDCCB COPY SESSION DATA';btn.classList.remove('copied');},3000);\n");
		html.append("  });\n");
		html.append("}\n");
		html.append("</script>\n");
		html.append("</body></html>");

		Files.write(Paths.get("deploy/dummy.html"), html.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static String jsonEsc(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
	}

	public static void loginToJTwine() {
		driver.get("https://www.jobtwine.com/signin");
		waitForFixTime(2000);
		waitTillElementVisible(By.xpath(".//input[@formcontrolname='userName']"), 30);
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//input[@formcontrolname='userName']")).sendKeys("himanshutester01@gmail.com");
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//button[contains(text(),'Next')]")).click();
		waitForFixTime(1000);
		waitTillElementVisible(By.xpath(".//input[@formcontrolname='password']"), 30);
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//input[@formcontrolname='password']")).sendKeys("Infoedge12!@");
		waitForFixTime(1000);
		driver.findElement(By.xpath(".//button[contains(text(),'Sign In')]")).click();
		waitTillElementVisible(By.xpath(".//div[contains(text(),'Candidates For Interview')]"), 30);
		System.out.println("    Login successful!");
	}

	private static void setTimezoneToIST(WebDriver d) {
		Map<String, Object> tz = new HashMap<>();
		tz.put("timezoneId", "Asia/Kolkata");
		((ChromeDriver) d).executeCdpCommand("Emulation.setTimezoneOverride", tz);
	}

	public static void waitForFixTime(int ms) {
		try { Thread.sleep(ms); } catch (InterruptedException e) {}
	}

	public static void waitTillElementVisible(By locator, int sec) {
		new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(sec))
			.until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(locator));
	}
}