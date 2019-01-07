package edu.ufl.cc.imageRecog.domain;

import java.security.NoSuchAlgorithmException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.context.request.async.DeferredResult;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.ufl.cc.imageRecog.service.RequestQueue;

@Scope("prototype")
@Component
public class CloudImageRecognition implements ImageRecognition{
	
	private static final Logger logger = LoggerFactory.getLogger(CloudImageRecognition.class);
	
	private String imageUrl;

	@Autowired
	RequestQueue queue;
	
	public void setQueue(RequestQueue queue) {
		this.queue = queue;
	}

	public RequestQueue getQueue() {
		return this.queue;
	}
	
	public CloudImageRecognition() {
	}
	
	@Override
	public String getImageUrl() {
		return imageUrl;
	}

	@Override
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	@Override
	public void getResult(DeferredResult<String> result) {
		try {
			queue.send(imageUrl, result);
		} catch (NoSuchAlgorithmException | JsonProcessingException e) {
			logger.error("Error sending " + imageUrl, e);
		}
	}
}
