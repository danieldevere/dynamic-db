package com.dandevere.learn.dynamicdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "dynamic-db")
@Getter
@Setter
public class DynamicDataSource extends AbstractDataSource implements BeanClassLoaderAware {
	
	private ClassLoader classLoader;
	
	private List<String> products;
	
	private Long expireAfter;
	
	private LoadingCache<String, DataSource> dataSources;
	
	@PostConstruct
	public void init() throws ExecutionException, SQLException {
		dataSources = CacheBuilder.newBuilder()
				.maximumSize(50)
				.expireAfterAccess(expireAfter, TimeUnit.SECONDS)
				.removalListener(new RemovalListener<String, DataSource>() {

					@Override
					public void onRemoval(RemovalNotification<String, DataSource> entry) {
						DataSource dataSource = entry.getValue();
//						System.out.println("Closing datasource: " + entry.getKey());
						dataSource.close(true);
					}
				})
				.build(new CacheLoader<String, DataSource>() {
					@Override
					public DataSource load(String source) throws Exception {
//						System.out.println("Initializing datasource: " + source);
						Flyway flyway = new Flyway();
						flyway.setDataSource("jdbc:mysql://localhost:3306?useSSL=false", "test", "test");
						flyway.setSchemas(source + "_dynamic");
						flyway.migrate();
						DataSource dataSource = (DataSource)DataSourceBuilder.create(classLoader)
								.driverClassName(DatabaseDriver.MYSQL.getDriverClassName())
								.url("jdbc:mysql://localhost:3306/" + source + "_dynamic?useSSL=false")
								.username("test").password("test").build();
						dataSource.getPoolProperties().setValidationQuery("SELECT 1;");
						dataSource.getPoolProperties().setMaxActive(1);
						dataSource.getPoolProperties().setMaxIdle(1);
						dataSource.getPoolProperties().setMinIdle(0);
						dataSource.getPoolProperties().setInitialSize(1);
						dataSource.getPoolProperties().setRemoveAbandoned(true);
						dataSource.getPoolProperties().setRemoveAbandonedTimeout(1000);
						return dataSource;
					}
				});
		for(String product : products) {
			dataSources.get(product).getConnection();
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if(attributes == null) {
			// no request
			return DriverManager.getConnection("jdbc:mysql://localhost:3306/metadataDB?useSSL=false", "test", "test");
		}
		try {
			Map<String, String> params = (Map<String,String>)attributes.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
			try {
				return dataSources.get(params.get("product")).getConnection();
			} catch(SQLException e) {
				dataSources.refresh(params.get("product"));
				return dataSources.get(params.get("product")).getConnection();
			}
			
		} catch (ExecutionException e) {
//			e.printStackTrace();
			System.out.println("getting default");
			return null;
		}
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if(attributes == null) {
			// no request
			return DriverManager.getConnection("jdbc:mysql://localhost:3306/metadataDB", "test", "test");
		}
		try {
			Map<String, String> params = (Map<String,String>)attributes.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
			try {
				return dataSources.get(params.get("product")).getConnection();
			} catch(SQLException e) {
				dataSources.refresh(params.get("product"));
				return dataSources.get(params.get("product")).getConnection();
			}
			
		} catch (ExecutionException e) {
//			e.printStackTrace();
			System.out.println("getting default");
			return null;
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
}
