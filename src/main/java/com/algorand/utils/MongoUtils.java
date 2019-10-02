package com.algorand.utils;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.DB;

public class MongoUtils{

	public static DB getDatabase(String name){

	MongoClientURI uri = new MongoClientURI(
    	"mongodb+srv://admin:GGbciCwuoPdiXmW1@cluster0-fsxky.mongodb.net/test?retryWrites=true&w=majority");

	MongoClient mongoClient = new MongoClient(uri);
	DB database = mongoClient.getDB(name);
	return database;
}
}