package com.society.leagues.conf.spring;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.society.leagues.cache.CacheUtil;
import com.society.leagues.mongo.CustomMappingMongoConverter;
import com.society.leagues.mongo.CustomRefResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
@EnableConfigurationProperties(MongoProperties.class)
@SuppressWarnings("unused")
@Profile("!test")
public class MongoConfig extends AbstractMongoConfiguration {
    @Autowired MongoProperties properties;
    @Autowired CacheUtil cacheUtil;

    @Override
    protected String getDatabaseName() {
        return properties.getDatabase();
    }

    @Override
    public Mongo mongo() throws Exception {
        return new MongoClient(properties.getHost());
    }

    @Bean
    @Primary
    @Override
	public MappingMongoConverter mappingMongoConverter() throws Exception {
        DbRefResolver dbRefResolver = new CustomRefResolver(mongoDbFactory(),cacheUtil);
		CustomMappingMongoConverter converter = new CustomMappingMongoConverter(dbRefResolver, mongoMappingContext());
        converter.setCacheUtil(cacheUtil);
        converter.setCustomConversions(customConversions());
		return converter;
	}


}
