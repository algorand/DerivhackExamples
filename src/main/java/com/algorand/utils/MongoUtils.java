package com.algorand.utils;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.DB;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

public class MongoUtils{

	public static DB getDatabase(String name){

	LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
	Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
	rootLogger.setLevel(Level.OFF);
	MongoClientURI uri = new MongoClientURI(
    	"mongodb://localhost");

	MongoClient mongoClient = new MongoClient(uri);
	DB database = mongoClient.getDB(name);
	return database;
}
}
