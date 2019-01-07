package edu.ufl.cc.imageRecog.service;

import java.util.UUID;

import com.google.gson.Gson;


public class RequestHandle {
	public String imageUrl;
	public String uuid;
	
	public RequestHandle(String imageUrl) {
		this.imageUrl = imageUrl;
		this.uuid = UUID.randomUUID().toString();
	}
	
	public String serialize() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
