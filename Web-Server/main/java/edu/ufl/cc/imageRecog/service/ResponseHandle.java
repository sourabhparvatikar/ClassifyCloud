package edu.ufl.cc.imageRecog.service;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ResponseHandle {
	
	private static final Logger logger = LoggerFactory.getLogger(ResponseHandle.class);
	
	public String imageUrl;
	public String uuid;
	public String result;
	
	public ResponseHandle(String imageUrl, String uuid, String result) {
		this.imageUrl = imageUrl;
		this.uuid = uuid;
		this.result = result;
	}
	
	public static ResponseHandle fromJSON(String jsonStr) {
		JSONParser parser = new JSONParser();
		JSONObject json;
		try {
			json = (JSONObject) parser.parse(jsonStr);
			return new ResponseHandle((String)json.get("imageUrl"), (String)json.get("uuid"), (String)json.get("result"));
		} catch (ParseException e) {
			logger.error("JSON parser failed", e);
		}
		return null;
	}
}
