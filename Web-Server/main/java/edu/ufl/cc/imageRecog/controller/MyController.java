package edu.ufl.cc.imageRecog.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import edu.ufl.cc.imageRecog.domain.AppProperties;
import edu.ufl.cc.imageRecog.domain.CloudImageRecognition;
import edu.ufl.cc.imageRecog.service.Utils;


@RestController
public class MyController {
	
	private static final Logger logger = LoggerFactory.getLogger(MyController.class);

	@Autowired
	CloudImageRecognition cir;

	@Autowired
	AppProperties appProp;
	
	@Value("${cc.http_request_timeout}")
	private long REQUEST_TIMEOUT; 

	@Value("${cc.http_request_timeout_result}")
	private String REQUEST_TIMEOUT_RESULT; 
	
	public void setCIR(CloudImageRecognition cir) {
		this.cir = cir;
	}
	
	public CloudImageRecognition getCIR() {
		return cir;
	}

	@RequestMapping("/")
	public String welcomePage() {
		return appProp.getProperties();

	}
	
	@RequestMapping("cloudimagerecognition.php")
	public DeferredResult<String> cloudImageRecognition(@RequestParam("input") String imageUrl) {
		logger.info("Received " + imageUrl);
		DeferredResult<String> result = new DeferredResult<>(
				REQUEST_TIMEOUT,
				Utils.formatResultString(imageUrl, REQUEST_TIMEOUT_RESULT)
				);
		cir.setImageUrl(imageUrl);
		cir.getResult(result);
		return result;
	}
}
