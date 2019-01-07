package edu.ufl.cc.imageRecog.service;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;

import edu.ufl.cc.imageRecog.domain.CachedValues;

@Scope("prototype")
@Component
public class RequestQueue {

	private static final Logger logger = LoggerFactory.getLogger(RequestQueue.class);
	private static ConcurrentHashMap<String, DeferredResult<String>> requests = new ConcurrentHashMap<>();
	// private static ConcurrentHashMap<String, String> cachedResults = new ConcurrentHashMap<>();
	
	
	@Value("${cc.sqs_request}")
	private String REQUEST_QUEUE_NAME;

	@Value("${cc.request_send_retry_delay}")
	private int REQUEST_SEND_RETRY_DELAY;

	@Value("${cc.request_send_retry_attempts}")
	private int REQUEST_SEND_ATTEMPTS;
	
	@Autowired
	CachedValues cachedValues;
	
	AmazonSQS amazonSQS = AmazonSQSClientBuilder.defaultClient();

	public  RequestHandle send(String imageUrl, DeferredResult<String> result) throws NoSuchAlgorithmException, JsonProcessingException {
		RequestHandle handle = new RequestHandle(imageUrl);

//		String resultString = cachedResults.get(imageUrl);
//		if(resultString != null) {
//			result.setResult(resultString);
//			return handle;
//		}

		
		int sendAttempt = 0;
		do {
			sendAttempt++;
			try {

				String queueUrl = cachedValues.getRequestQueueUrl();
				SendMessageRequest send_msg_request = new SendMessageRequest()
						.withQueueUrl(queueUrl)
						.withMessageBody(handle.serialize());
				amazonSQS.sendMessage(send_msg_request);
				requests.put(handle.uuid, result);
				logger.info("Enqueued request[uuid=" + handle.uuid + ", image=" + handle.imageUrl + "]");

				return handle;

			} catch (Exception e) {
				logger.info("Error sending request[uuid=" + handle.uuid + ", image=" + handle.imageUrl + "]. "
						  + "Retrying in " + REQUEST_SEND_RETRY_DELAY + "ms...");
				try {
					Thread.sleep(REQUEST_SEND_RETRY_DELAY);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		} while (sendAttempt < REQUEST_SEND_ATTEMPTS);

		result.setResult(Utils.formatResultString(imageUrl, "ERROR sending request"));
		return handle;
	}
	
	public static void receive(ResponseHandle response) {
		DeferredResult<String> result = RequestQueue.requests.get(response.uuid);
		if (result == null) {
			logger.error("Unaccounted response[uuid=" + response.uuid + ", image=" + response.imageUrl + "]. Ignored...");
			return;
		}

		result.setResult(Utils.formatResultString(response.imageUrl, response.result));
		logger.info("Received response[uuid=" + response.uuid + ", image=" + response.imageUrl + ", result='" + response.result + "']");

//		cachedResults.put(response.imageUrl, resultString);

	}
}
