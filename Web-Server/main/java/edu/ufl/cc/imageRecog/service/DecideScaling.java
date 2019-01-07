package edu.ufl.cc.imageRecog.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.util.Base64;


@Service
public class DecideScaling {
	

	@Value("${cc.ec2_instance_type}")
	private String INSTANCE_TYPE;

	@Value("${cc.ec2_key_name}")
	private String KEY_NAME;

	@Value("${cc.ec2_security_group}")
	private String SECURITY_GROUP;

	@Value("${cc.ec2_worker_name}")
	private String WORKER_NAME;

	@Value("${cc.ec2_worker_ami}")
	private String WORKER_AMI;

	@Value("${cc.s3_bucket}")
	private String S3_BUCKET_NAME;

	@Value("${cc.sqs_terminate}")
	private String TERMINATE_REQUEST_QUEUE_NAME;

	@Value("${cc.worker_aws_profile}")
	private String PROFILE_NAME;
	
	

	@Value("${cc.s3_upload_enabled}")
	private boolean S3_UPLOAD_ENABLED;

	@Value("${cc.worker_pull_source}")
	private boolean WORKER_PULL_SOURCE;

	@Value("${cc.terminate_check_interval}")
	private int TERMINATE_CHECK_INTERVAL;

	@Value("${cc.terminate_request_expires_in}")
	private int TERMINATE_REQUEST_EXPIRES_IN;
	
	

	@Value("${cc.min_workers_count}")
	private int MIN_WORKERS_COUNT;

	@Value("${cc.max_workers_count}")
	private int MAX_WORKERS_COUNT;

	@Value("${cc.quality_of_service}")
	private float QUALITY_OF_SERVICE;

	@Value("${cc.scaling_factor}")
	private float SCALING_FACTOR;

	@Value("${cc.workers_kill_all_delay}")
	private int WORKERS_KILL_ALL_DELAY;
	
	private String userData = null;
	private String requestqueueUrl = null;
	
	private static final Logger logger = LoggerFactory.getLogger(DecideScaling.class);

	final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
	final AmazonSQS amazonSQS = AmazonSQSClientBuilder.defaultClient();
	
	public void decideScaling(ArrayList<Integer> metrics) {
		int numberOfMessages = metrics.get(0);
		int numberOfWorkers = metrics.get(1);
		
		int desiredWorkerCount = (int) Math.ceil(((float)numberOfMessages)/QUALITY_OF_SERVICE);
		int feasibleWorkerCount = Math.min(MAX_WORKERS_COUNT, desiredWorkerCount);
		int workerSpawnCount = (int) Math.ceil(((float)(feasibleWorkerCount - numberOfWorkers))*SCALING_FACTOR);
		
		//logic for autoscale
		 if(workerSpawnCount > 0) {
			logger.info(numberOfMessages + " messages, " + numberOfWorkers +" workers; spawning " + workerSpawnCount + " workers");
			createWorkers(workerSpawnCount);
		} else if(workerSpawnCount < 0){
			int terminateWorkerCount = -workerSpawnCount-MIN_WORKERS_COUNT;
			if(numberOfMessages > 0) {
				logger.info(numberOfMessages + " messages, " + numberOfWorkers +" workers; terminating " + terminateWorkerCount + " workers");
				shutDownWorkers(terminateWorkerCount);
			}
			else if (terminateWorkerCount > 0) {
				logger.info(numberOfMessages + " messages, " + numberOfWorkers +" workers; KILLING ALL WORKERS in " + WORKERS_KILL_ALL_DELAY + "ms");
				try {
					Thread.sleep(WORKERS_KILL_ALL_DELAY);
				} catch (InterruptedException e) {
					logger.error("Error waiting", e);
				}
				killAllWorkers();
			}
		}
	}
	
	public void createWorkers(int instanceCount) {
		if(userData==null) {
			userData = String.format(
					"#!/bin/bash\n" +
					"echo 'PROFILE_NAME = \"%s\"' > /home/ubuntu/proj1-worker/constants.py \n" +
					"echo 'S3_UPLOAD_ENABLED = %s' >> /home/ubuntu/proj1-worker/constants.py \n" +
					"echo 'S3_BUCKET_NAME = \"%s\"' >> /home/ubuntu/proj1-worker/constants.py \n" +
					"echo 'TERMINATE_REQUEST_EXPIRES_IN = %d' >> /home/ubuntu/proj1-worker/constants.py \n" +
					"echo 'TERMINATE_CHECK_INTERVAL = %d' >> /home/ubuntu/proj1-worker/constants.py \n" +
					((WORKER_PULL_SOURCE)? "cd /home/ubuntu/proj1-worker && git pull origin master \n":"") +
					"systemctl start worker.service \n",
					PROFILE_NAME,
					((S3_UPLOAD_ENABLED)?"True":"False"),
					S3_BUCKET_NAME,
					TERMINATE_REQUEST_EXPIRES_IN,
					TERMINATE_CHECK_INTERVAL);
		}

		logger.info("Spawning " + instanceCount + " instances");

		Tag t = new Tag().withKey("Name").withValue(WORKER_NAME);
		TagSpecification ts = new TagSpecification().withTags(t).withResourceType(ResourceType.Instance);
		
		RunInstancesRequest rir = new RunInstancesRequest(WORKER_AMI, instanceCount, instanceCount)
				.withKeyName(KEY_NAME)
				.withInstanceType(INSTANCE_TYPE)
				.withTagSpecifications(ts)
				.withSecurityGroups(SECURITY_GROUP)
				.withAdditionalInfo(WORKER_NAME)
				.withUserData(new String(Base64.encode(userData.getBytes())));
				
		try {
			RunInstancesResult result = ec2.runInstances(rir);
			logger.info("Created " + result.getReservation().getInstances().size() + " instances");
		} catch (Exception e) {
			logger.error("Error creating instances", e);
		}
	}
	
	public void shutDownWorkers(int batchSize) {
		if(batchSize <= 0) {
			return;
		}
		if (requestqueueUrl == null) {
			requestqueueUrl = amazonSQS.getQueueUrl(TERMINATE_REQUEST_QUEUE_NAME).getQueueUrl();
		}

		while(batchSize > 0) {
			List<SendMessageBatchRequestEntry> entries = new ArrayList<SendMessageBatchRequestEntry>();
			for (int i = 0; i < Math.min(10, batchSize); i++) {
					entries.add(new SendMessageBatchRequestEntry()
							.withId(Integer.toString(i))
							.withMessageBody("Terminate"));
			}
			batchSize -= 10;
			logger.info("Initiating shutting down of " + entries.size() + " instances");
			SendMessageBatchRequest send_msg_request = new SendMessageBatchRequest()
					.withQueueUrl(requestqueueUrl)
					.withEntries(entries);
			try {
				amazonSQS.sendMessageBatch(send_msg_request);
			} catch (Exception e) {
				logger.error("Error sending shutdown requests", e);
			}
		}
	}

	public void killAllWorkers() {
		DescribeInstancesRequest request = new DescribeInstancesRequest()
				 .withFilters(
						 new Filter("instance-state-name").withValues("running", "pending"),
						 new Filter("image-id").withValues(WORKER_AMI)
						 );
		DescribeInstancesResult ec2Response;
		try {
			ec2Response = ec2.describeInstances(request);
		} catch (Exception e) {
			logger.error("Error getting instance info", e);
			logger.info("Kill all workers request unsuccessful");
			return;
		}

		Set<String> running_instances_set = new HashSet<>();
		Set<String> pending_instances_set = new HashSet<>();
		Set<String> all_instances_set = new HashSet<>();
		List<Reservation> reservations = ec2Response.getReservations();

		for (Reservation reservation: reservations) {
			for(Instance ins: reservation.getInstances()) {
				all_instances_set.add(ins.getInstanceId());
				if("running".equals(ins.getState().getName())) {
					running_instances_set.add(ins.getInstanceId());
				} else {
					pending_instances_set.add(ins.getInstanceId());
				}
			}
		}

		Set<String> reserved_instances_set = new HashSet<String>();
		
		for(String instance_id: running_instances_set) {
			if(reserved_instances_set.size() < MIN_WORKERS_COUNT) {
				reserved_instances_set.add(instance_id);
			} else {
				break;
			}
		}
		for(String instance_id: pending_instances_set) {
			if(reserved_instances_set.size() < MIN_WORKERS_COUNT) {
				reserved_instances_set.add(instance_id);
			} else {
				break;
			}
		}
		
		all_instances_set.removeAll(reserved_instances_set);

		if(all_instances_set.size() == 0) {
			logger.info("Not terminating reserved workers");
			return;
		}
		
		List<String> instances_list = new ArrayList<>();
		instances_list.addAll(all_instances_set);
		TerminateInstancesRequest tir = new TerminateInstancesRequest(instances_list);
		try {
			ec2.terminateInstances(tir);
			logger.info("Killed " + instances_list.size() + " instances");
		} catch (Exception e) {
			logger.error("Error terminating workers", e);
			logger.info("Kill all workers request unsuccessful");
		}
	}
}
