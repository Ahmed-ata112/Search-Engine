package org.mpack;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDB {
    static MongoCollection<Document> myDb;
    static MongoCollection<org.bson.Document> urlsCollection;
    static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static MongoClient mongoClient;
    static MongoDatabase searchEngineDb;

    MongoDB(){
        mongoClient = MongoClients.create(CONNECTION_STRING);
        searchEngineDb = mongoClient.getDatabase("SearchEngine");
        urlsCollection = searchEngineDb.getCollection("CrawledURLS");

    }


}
