/**
 * 
 */
package com.tars.kite.scheduler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tars.kite.service.TickerService;
import com.tars.kite.util.Constants;
import com.tars.kite.util.Kitefeeds;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;

/**
 * @author user
 *
 */
@Component
public class KiteScheduler {

	@Autowired
	Kitefeeds kitefeeds;

	@Autowired
	TickerService tickerServiceImpl;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(KiteScheduler.class);


	@Scheduled(cron = "${kite_stream_schedule}")
	public void stream() throws JSONException, IOException, KiteException {
		LOGGER.info("stream started... ");
		Double ltp = kitefeeds.getLTP(Constants.NIFTY_BANK);
		LOGGER.info("stream ltp = {} ",ltp);

		List<String> optionInstuments = getOptionsInstuments(Constants.BASE_BANKNIFTY_INST, ltp);
		LOGGER.info("stream optionInstuments={} ",optionInstuments);

		List<Instrument> instruments =  kitefeeds.instuments(Constants.EXCHANGE_NFO, optionInstuments);
		LOGGER.info("stream instruments={} ",instruments);

		List<Long> tokens = instruments.stream().map(i -> i.instrument_token).collect(Collectors.toList());
		LOGGER.info("stream tokens={} ",tokens);

		Map<Object, Object> instumentobject = instruments.stream().collect(Collectors.toMap(i -> i.instrument_token, i -> i.tradingsymbol));
		
		ArrayList<Long> tokenslist = new ArrayList<Long>(tokens);
		LOGGER.info("stream tokenslist={}, instumentobject={} ",tokenslist, instumentobject);
		tickerServiceImpl.kite_stream(tokenslist, instumentobject);
		
	}

	public List<String> getOptionsInstuments(String baseInst, Double ltp) throws KiteException {
		
		List<String> optionInstuments = new ArrayList<String>();

		String month = new SimpleDateFormat("MMM").format(new Date()); 
		String year = new SimpleDateFormat("YY").format(new Date()); 
		month = month.toUpperCase();
		

        int lastprice = (int)Math.ceil(ltp);
        lastprice = ((lastprice + 99) / 100) * 100;
        
        int startval = lastprice - 1000;
        int endval = lastprice + 1000;
        
        while(startval <= endval ) {
        	optionInstuments.add(baseInst + year + month + startval+ "CE");
        	optionInstuments.add(baseInst + year + month + startval+ "PE");
        	startval = startval + 100;
        }

		return optionInstuments;
	}
}
