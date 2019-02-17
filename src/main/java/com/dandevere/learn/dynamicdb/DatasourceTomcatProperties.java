package com.dandevere.learn.dynamicdb;

import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource.tomcat")
public abstract class DatasourceTomcatProperties implements PoolConfiguration {

}
