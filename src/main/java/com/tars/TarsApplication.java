package com.tars;

import java.io.IOException;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.tars.kite.session.KiteSession;
import com.tars.kite.util.AppProperties;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;

@SpringBootApplication
@EnableScheduling
public class TarsApplication {
	
	@Autowired
	AppProperties appProperties;
	@Autowired
	KiteSession kiteSession;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TarsApplication.class);


	public static void main(String[] args) throws JSONException, IOException, KiteException {
		SpringApplication.run(TarsApplication.class, args);
	}
	
	
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
	    RedisTemplate<String, Object> template = new RedisTemplate<>();
	    template.setConnectionFactory(connectionFactory);
	    // Add some specific configuration here. Key serializers, etc.
	    template.setHashValueSerializer(new StringRedisSerializer());
	    template.setKeySerializer(new StringRedisSerializer());
	    template.setValueSerializer(new StringRedisSerializer());
	    template.setHashKeySerializer(new StringRedisSerializer());
	    template.afterPropertiesSet();
	    return template;
	}

	@Bean
	public KiteConnect kiteConnect() throws JSONException, IOException, KiteException {
		KiteConnect kiteConnect = new KiteConnect(appProperties.getApikey());
		try {
			kiteConnect.setUserId(appProperties.getUserid());

			// Get login url
			String url = kiteConnect.getLoginURL();
			LOGGER.info("TarsApplication.kiteSdk() userid={}, password={}, twofactorpin={} " , appProperties.getUserid(), appProperties.getPassword(), appProperties.getTwofactorpin());
			String request_token = kiteSession.autoWebLogin(appProperties.getUserid(), appProperties.getPassword(), appProperties.getTwofactorpin(), url);

			// Set session expiry callback.
			kiteConnect.setSessionExpiryHook(new SessionExpiryHook() {
				@Override
				public void sessionExpired() {
					LOGGER.info("kiteConnect  session expired");
				}
			});

			LOGGER.info("TarsApplication.kiteSdk() request_token={}  " , request_token);
			User user = kiteConnect.generateSession(request_token, appProperties.getApisecret());
			kiteConnect.setAccessToken(user.accessToken);
			kiteConnect.setPublicToken(user.publicToken);

		} catch (Exception e) {
			LOGGER.info(" kiteConnect Exception occured={} ",e);
			e.printStackTrace();
		}

		return kiteConnect;
	}

}
