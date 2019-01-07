package edu.ufl.cc.imageRecog.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

@Component
public class CachedValues {

	@Value("${cc.sqs_request}")
	private String REQUEST_QUEUE_NAME;

	AmazonSQS amazonSQS = AmazonSQSClientBuilder.defaultClient();

	@Cacheable("queue_url_request")
	public String getRequestQueueUrl() {
		return amazonSQS.getQueueUrl(REQUEST_QUEUE_NAME).getQueueUrl();
	}
}
