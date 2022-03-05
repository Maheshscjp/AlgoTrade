package com.tars.kite.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("tars")
public class AppProperties {

	private String userid;
	private String password;
	private String apikey;
	private String apisecret;
	private String twofactorpin;
	private String chromedriverpath;

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getApikey() {
		return apikey;
	}

	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	public String getApisecret() {
		return apisecret;
	}

	public void setApisecret(String apisecret) {
		this.apisecret = apisecret;
	}

	public String getTwofactorpin() {
		return twofactorpin;
	}

	public void setTwofactorpin(String twofactorpin) {
		this.twofactorpin = twofactorpin;
	}

	public String getChromedriverpath() {
		return chromedriverpath;
	}

	public void setChromedriverpath(String chromedriverpath) {
		this.chromedriverpath = chromedriverpath;
	}

}
