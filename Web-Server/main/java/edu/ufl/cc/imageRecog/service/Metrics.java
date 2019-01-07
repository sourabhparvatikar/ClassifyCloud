package edu.ufl.cc.imageRecog.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;


@Service
public class Metrics {
	
	@Value("${cc.sqs_request}")
	private String REQUEST_QUEUE_NAME;

	@Value("${cc.ec2_worker_ami}")
	private String WORKER_AMI;

	final AmazonSQS amazonSQS = AmazonSQSClientBuilder.defaultClient();
	final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();	
	
	private String queueUrl = null;
		
	public Metrics() {
	}
	
	public List<Integer> getMetrics() {
		ArrayList<Integer> metrics = new ArrayList<Integer>();
		
		metrics.add(getMessagesInTheQueue());
		metrics.add(getNumberOfWorkers());
		
		return metrics;
	}
	
	private int getMessagesInTheQueue() {
		if(queueUrl == null) {
			queueUrl = amazonSQS.getQueueUrl(REQUEST_QUEUE_NAME).getQueueUrl();
		}
		GetQueueAttributesRequest request = new GetQueueAttributesRequest()
				.withAttributeNames("ApproximateNumberOfMessages")
        		.withQueueUrl(queueUrl);
        
        Map<String, String> attrs = amazonSQS.getQueueAttributes(request).getAttributes();
        
        int messages = Integer.parseInt(attrs.get("ApproximateNumberOfMessages"));
        
        return messages;
	}
	
	private int getNumberOfWorkers() {
		 DescribeInstancesRequest request = new DescribeInstancesRequest()
				 .withFilters(
						 new Filter("instance-state-name").withValues("running", "pending"),
						 new Filter("image-id").withValues(WORKER_AMI)
						 );
		DescribeInstancesResult ec2Response = ec2.describeInstances(request);
		Set<Instance> instances_set = new HashSet<Instance>();
		List<Reservation> reservations = ec2Response.getReservations();
		for (Reservation reservation: reservations) {
			instances_set.addAll(reservation.getInstances());
		}
		return instances_set.size();
	}
}
