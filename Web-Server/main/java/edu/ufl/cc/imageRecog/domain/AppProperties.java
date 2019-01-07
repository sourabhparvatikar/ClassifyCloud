package edu.ufl.cc.imageRecog.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

@Component
public class AppProperties {
	
	@Autowired
	Environment env;

	private String properties = null;
	
	@SuppressWarnings("rawtypes")
	public void loadProperties() {
		if (env instanceof ConfigurableEnvironment) {
			for (PropertySource<?> propertySource : ((ConfigurableEnvironment) env).getPropertySources()) {
				if (propertySource instanceof EnumerablePropertySource) {
					for (String key : ((EnumerablePropertySource) propertySource).getPropertyNames()) {
						if(key.startsWith("cc.")) {
							properties += String.format("%35s = %s\n", key, propertySource.getProperty(key));
						}
					}
				}
			}
		}
	}
	
	public String getProperties() {
		if(properties==null)
			loadProperties();
		return properties;
	}
}
