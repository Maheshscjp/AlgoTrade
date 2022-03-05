/**
 * 
 */
package com.tars.kite.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import com.opencsv.CSVWriter;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnDisconnect;
import com.zerodhatech.ticker.OnTicks;

/**
 * @author user
 *
 */
@Service
public class TickerService {

	@Autowired
	private KiteConnect kiteConnect;

	DateTime latestTickTime = new DateTime();
	Map<Long, DateTime> mapGlobalTimeStamps;

	@Autowired
	RedisTemplate<String, Object> redisTemplate;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TickerService.class);


	public void kite_stream(ArrayList<Long> tokens, Map<Object, Object> tokensmap) throws KiteException {

		KiteTicker tickerProvider = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());

		tickerProvider.setOnConnectedListener(new OnConnect() {
			@Override
			public void onConnected() {
				boolean isConnected = tickerProvider.isConnectionOpen();
				LOGGER.info("TickerService.kite_stream(...).new OnConnect() {...}.onConnected()  isConnected={} ",isConnected);
			}
		});

		tickerProvider.setOnDisconnectedListener(new OnDisconnect() {
			@Override
			public void onDisconnected() {
				tickerProvider.connect();
				boolean isConnected = tickerProvider.isConnectionOpen();
				LOGGER.info("TickerService.kite_stream(...).new OnDisconnect() {...}.onDisconnected()  isConnected={} ",isConnected);
				LOGGER.info("reconnecting... ");
				tickerProvider.subscribe(tokens);
				tickerProvider.setMode(tokens, KiteTicker.modeFull);
				latestTickTime = new DateTime();

			}
		});

		tickerProvider.setOnTickerArrivalListener(new OnTicks() {
			@Override
			public void onTicks(ArrayList<Tick> ticks) {

				if (ticks.size() > 0) {
					extractTicksData(ticks, tokensmap);
					latestTickTime = new DateTime();
				}
				else {
					DateTime currTime = new DateTime();
					if (currTime.isAfter(latestTickTime.plusSeconds(15))) {
						tickerProvider.disconnect();
					}

				}

			}

		});

		tickerProvider.setTryReconnection(true);
		// minimum value must be 5 for time interval for reconnection
		tickerProvider.setMaximumRetryInterval(5);
		// set number to times com.rainmatter.ticker can try reconnection, for
		// infinite retries use -1
		tickerProvider.setMaximumRetries(10);

		/**
		 * connects to com.rainmatter.com.rainmatter.ticker server for getting live
		 * quotes
		 */
		tickerProvider.connect();

		/**
		 * You can check, if websocket connection is open or not using the following
		 * method.
		 */
		boolean isConnected = tickerProvider.isConnectionOpen();

		System.out.println(isConnected);

		tickerProvider.subscribe(tokens);

		/**
		 * set mode is used to set mode in which you need tick for list of tokens.
		 * Ticker allows three modes, modeFull, modeQuote, modeLTP. For getting only
		 * last traded price, use modeLTP For getting last traded price, last traded
		 * quantity, average price, volume traded today, total sell quantity and total
		 * buy quantity, open, high, low, close, change, use modeQuote For getting all
		 * data with depth, use modeFull
		 */
		tickerProvider.setMode(tokens, KiteTicker.modeFull);

		latestTickTime = new DateTime();
	}

	private void extractTicksData(ArrayList<Tick> ticks, Map<Object, Object> tokensmap) {

		// DateTimeZone.setDefault(DateTimeZone.forID("Asia/Kolkata"));

		try {
			for (Tick tick : ticks) {
				// DateTime oldTimeStamp = mapGlobalTimeStamps.get(tokenValue);
				// long difference = newTimeStamp.getMillis() - oldTimeStamp.getMillis();

				long token = tick.getInstrumentToken();
				String streamid = getStreamID(tokensmap.get(token).toString());

				LOGGER.info("TickerServiceImpl.extractTicksData() || streamid={}  ", streamid);

				Map<String, String> streamdata = new HashMap<>();
				streamdata.put("Instrument", tokensmap.get(tick.getInstrumentToken()).toString());
				streamdata.put("InstrumentToken", String.valueOf(token));
				streamdata.put("LastPrice", Double.toString(tick.getLastTradedPrice()));
				streamdata.put("Volume", Double.toString(tick.getVolumeTradedToday()));
				streamdata.put("OI", Double.toString(tick.getOi()));
				streamdata.put("OIDayHigh", Double.toString(tick.getOpenInterestDayHigh()));
				streamdata.put("OIDayLow", Double.toString(tick.getOpenInterestDayLow()));
				streamdata.put("Open", Double.toString(tick.getOpenPrice()));
				streamdata.put("High", Double.toString(tick.getHighPrice()));
				streamdata.put("Low", Double.toString(tick.getLowPrice()));
				streamdata.put("Close", Double.toString(tick.getClosePrice()));
				streamdata.put("LastTradeQuantity", Double.toString(tick.getLastTradedQuantity()));
				streamdata.put("TotalBuyQuantity", Double.toString(tick.getTotalBuyQuantity()));
				streamdata.put("TotalSellQuantity", Double.toString(tick.getTotalSellQuantity()));
				streamdata.put("LastTradeTime", getStringFromDate(tick.getLastTradedTime()));
				streamdata.put("Timestamp", getStringFromDate(new Date()));

				LOGGER.info("TickerServiceImpl.extractTicksData() || streamdata={} " , streamdata);
				CSVWriter csvWriter = new CSVWriter(null);
				MapRecord<String, String, String> record = StreamRecords.newRecord().in(streamid).ofMap(streamdata);
				redisTemplate.opsForStream().add(record);

			}
		} catch (Exception e) {
			LOGGER.info("TickerServiceImpl.extractTicksData() || Exception={} " , e);
			e.printStackTrace();
		}

	}

	private String getStreamID(String instument) {

		String month = new SimpleDateFormat("MM").format(new Date());
		String year = new SimpleDateFormat("YYYY").format(new Date());
		String day = new SimpleDateFormat("dd").format(new Date());

		// banknifty22feb36400ce_2022_02_22
		instument = instument.toLowerCase() + "_" + year + "_" + month + "_" + day;
		return instument;

	}

	public static String getStringFromDate(Date date) throws ParseException {
		if (date == null)
			date = new Date();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return formatter.format(date);
	}
}
