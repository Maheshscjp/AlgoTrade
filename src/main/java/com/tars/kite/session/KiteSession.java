/**
 * 
 */
package com.tars.kite.session;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tars.kite.service.TickerService;
import com.tars.kite.util.AppProperties;

/**
 * @author user
 *
 */

@Component
public class KiteSession {

	@Autowired
	AppProperties appProperties;

	private static final Logger LOGGER = LoggerFactory.getLogger(TickerService.class);

	/**
	 * Execute the automatic web login process and retrieve the request_token.
	 *
	 * @return the request_token or null if there was an error
	 * @throws InterruptedException
	 * @throws NullPointerException
	 */
	public String autoWebLogin(String userId, String password, String twoFactorPin, String redirectUrl)
			throws IOException, InterruptedException {
		String requestToken = null;
		// We need the FireFox WebDriver for Selenium
		LOGGER.info("KiteSession.autoWebLogin() || cromedriver={}  ", appProperties.getChromedriverpath());

		System.setProperty("webdriver.chrome.driver", appProperties.getChromedriverpath());

		// Get the first log in URL.

		// Initialize the drivers
		WebDriver webDriver = new ChromeDriver(new ChromeOptions().setHeadless(true));
		WebDriverWait waitDriver = new WebDriverWait(webDriver, 20);

		// Get the Kite Login page. Use the User ID & Password to submit the form.
		LOGGER.info("KiteSession.autoWebLogin() || redirectUrl={}  ", redirectUrl);
		webDriver.get(redirectUrl);
		String curUrl = webDriver.getCurrentUrl();
		WebElement loginField = webDriver.findElement(By.id("userid"));
		WebElement pwdField = webDriver.findElement(By.id("password"));
		WebElement submitButton = webDriver
				.findElement(By.xpath("/html/body/div[1]/div/div[2]/div[1]/div/div/div[2]/form/div[4]/button"));
		loginField.sendKeys(userId);
		pwdField.sendKeys(password);
		submitButton.click();
		waitDriver.until(ExpectedConditions.presenceOfElementLocated(By.id("pin")));

		// We are now in the 2FA page. Enter the PIN and submit the form.
		curUrl = webDriver.getCurrentUrl();

		WebElement twoFaField = webDriver.findElement(By.cssSelector("#pin"));
		WebElement twoFaButton = webDriver.findElement(By.cssSelector(".button-orange"));
		// NOTE: Select by XPath was working and should work. Not sure why it stopped.
		// Have to review and figure it out.
		twoFaField.sendKeys(twoFactorPin);
		twoFaButton.submit();

		// waitDriver.until(ExpectedConditions.urlContains(redirectUrl));
		Thread.sleep(20000);
		curUrl = webDriver.getCurrentUrl();
		LOGGER.info("KiteSession.autoWebLogin() || curUrl={}", curUrl);
		URL url = new URL(curUrl);
		Map<String, String> query_pairs = new HashMap<>();
		String query = url.getQuery();
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.toString()),
					URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.toString()));
		}
		requestToken = query_pairs.get("request_token");
		LOGGER.info("KiteSession.autoWebLogin() || requestToken={}", requestToken);
		webDriver.close();
		return requestToken;
	}

}
