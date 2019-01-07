package edu.ufl.cc.imageRecog.domain;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.ufl.cc.imageRecog.service.DecideScaling;
import edu.ufl.cc.imageRecog.service.Metrics;

@Component
public class ScheduleLoadBalancer {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleLoadBalancer.class);
	
	@Autowired
	Metrics metrics;
	
	@Autowired
	DecideScaling decideScaling;
	
	@Scheduled(fixedDelay = 5000)
	public void scheduleLoadBalancerTask() {
		ArrayList<Integer> metricsList = new ArrayList<Integer>();
		
		metricsList = (ArrayList<Integer>) metrics.getMetrics();
		
		try {
			decideScaling.decideScaling(metricsList);
		} catch (Exception e) {
			logger.error("Error encountered in load balancer", e);
		}
	}
}
