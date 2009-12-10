package com.jwhois.util;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.URLEncoder;
import java.util.Arrays;

import sun.misc.BASE64Encoder;

import com.jwhois.core.Utility;

public class AlexaUtil {
	private static BASE64Encoder	encoder64	= new BASE64Encoder();
	private String					domain;

	public AlexaUtil(String domain) {
		this.domain = IDN.toASCII( domain );
	}

	public String[] getAlexaImageURI(int width) {
		String[] res = new String[3];
		Arrays.fill( res, "" );

		if (!Utility.isValidDom( domain ))
			return res;

		// Alexa Rank
		res[0] = "http://xsltcache.alexa.com/site_stats/gif/s/c/" + encoder64.encode( domain.getBytes() ) + "/s.gif";

		// Alexa Daily Reach
		try {
			res[1] = "http://traffic.alexa.com/graph?u=" + URLEncoder.encode( domain, "utf-8" ) + "&r=6m&y=r&z=1&h="
					+ Math.round( width * 2.0d / 5.0d ) + "&w=" + width;
		}
		catch (UnsupportedEncodingException e) {
			// do nothing
		}

		// Compete Rank
		try {
			res[2] = "http://grapher.compete.com/" + URLEncoder.encode( domain, "utf-8" ) + "_uv.png";
			if (!Utility.checkURL( res[2] )) {
				res[2] = "";
			}
		}
		catch (UnsupportedEncodingException e) {
			// do nothing
		}

		return res;
	}
}
