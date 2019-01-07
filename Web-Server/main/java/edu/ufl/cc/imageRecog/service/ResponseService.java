package edu.ufl.cc.imageRecog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

@Component
public class ResponseService {
	
	private static final Logger logger = LoggerFactory.getLogger(ResponseService.class);
		
	@Value("${cc.sqs_response}")
	private String RESPONSE_QUEUE_NAME;

	private ExecutorService executorService;

	final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
				 
	@PostConstruct
	public void init() {

		String queueUrl;
		try {
			//queueUrl = sqs.getQueueUrl(RESPONSE_QUEUE_NAME).getQueueUrl();
			logger.info("HZI");
			queueUrl="https://sqs.us-west-1.amazonaws.com/029304977443/response";
		} catch (Exception e) {
			logger.error("Error starting Response Queue Service", e);
			throw e;
		}

		BasicThreadFactory factory = new BasicThreadFactory.Builder()
								.namingPattern("Response-Queue-Handler-%d").build();

		executorService = Executors.newSingleThreadExecutor(factory);
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				logger.info("============ Response Queue Service Started ============");

				while(true){
					ReceiveMessageRequest rmr = new ReceiveMessageRequest(queueUrl)
							.withMaxNumberOfMessages(10)
							.withWaitTimeSeconds(20);

					List<Message> messages = sqs.receiveMessage(rmr).getMessages();
					List<DeleteMessageBatchRequestEntry> delmessages = new ArrayList<>(messages.size());
					logger.debug("Received " + messages.size() + " responses");
					for(Message message: messages) {
						RequestQueue.receive(ResponseHandle.fromJSON(message.getBody()));
						delmessages.add(new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()));
					}
					if(delmessages.size() > 0) {
						sqs.deleteMessageBatch(queueUrl, delmessages);
					}
				}
			}
		});

		executorService.shutdown();

	}
		 
	@PreDestroy
	public void beandestroy() {
		if (executorService != null) {
			executorService.shutdownNow();
		}
	}
}
