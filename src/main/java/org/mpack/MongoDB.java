package org.mpack;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.types.ObjectId;

public class MongoDB {
    MongoCollection<org.bson.Document> urlsCollection;
    static final  String CONNECTION_STRING = "mongodb://localhost:27017";
    MongoDatabase searchEngineDb;
    MongoClient mongoClient;
    MongoDB() {
        initConnection();

    }

    public  void initConnection() {
        try {
            //SearchEngine
            //.
            //CrawledURLS
            mongoClient = MongoClients.create(CONNECTION_STRING);
            searchEngineDb = mongoClient.getDatabase("SearchEngine");

            urlsCollection = searchEngineDb.getCollection("CrawledURLS");
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public void insertUrl(String url, String html) {
        org.bson.Document urlEntry = new org.bson.Document("_id", new ObjectId());
        urlEntry.append("url_link", url)
                .append("html_body", html);
        urlsCollection.insertOne(urlEntry);
    }

    // state is 0 if not finished or 1 if finished
    public void initState(int state){
        // should be called on a new
        org.bson.Document urlEntry = new org.bson.Document("Name", "state");
        org.bson.Document updateEntry = new org.bson.Document("isDone",state );
        org.bson.Document setEntry = new org.bson.Document("$set", updateEntry);

        UpdateOptions options = new UpdateOptions().upsert(true);

        urlsCollection.updateOne(urlEntry, setEntry, options);

    }
}
