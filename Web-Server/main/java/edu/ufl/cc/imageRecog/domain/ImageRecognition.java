package edu.ufl.cc.imageRecog.domain;

import org.springframework.web.context.request.async.DeferredResult;

public interface ImageRecognition {
	public String getImageUrl();
	public void setImageUrl(String imageUrl);
	public void getResult(DeferredResult<String> result);
}
