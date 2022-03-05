/**
 * 
 */
package com.tars.kite.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.LTPQuote;

/**
 * @author user
 *
 */

@Component
public class Kitefeeds {

	@Autowired
	KiteConnect kiteconnect;

	public List<Instrument> instuments(String exchange, List<String> instuments) {
		List<Instrument> filteredinstuments = null;
		try {
			List<Instrument> allinstruments = kiteconnect.getInstruments();
			filteredinstuments = allinstruments.stream()
					.filter(instument -> instuments.contains(instument.getTradingsymbol())).collect(Collectors.toList());
			
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KiteException e) {
			e.printStackTrace();
		}
		return filteredinstuments;

	}

	public Double getLTP(String[] instuments) throws JSONException, IOException, KiteException {
		Double lastprice = null;
		Map<String, LTPQuote> ltpinstument = kiteconnect.getLTP(instuments);
		if (ltpinstument.size() > 0)
			return ltpinstument.get("NSE:NIFTY BANK").lastPrice;
		else
			return lastprice;

	}


}
