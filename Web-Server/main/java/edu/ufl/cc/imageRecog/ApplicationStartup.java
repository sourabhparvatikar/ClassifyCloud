package edu.ufl.cc.imageRecog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import edu.ufl.cc.imageRecog.domain.AppProperties;

@Component
public class ApplicationStartup 
implements ApplicationListener<ApplicationReadyEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger("Config");

	@Autowired
	Environment env;

	@Autowired
	AppProperties appProps;
	
	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
//		logger.info("Using the following config values:\n" + appProps.getProperties());
	}
}
