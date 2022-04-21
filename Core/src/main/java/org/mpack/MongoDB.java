package org.mpack;

import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;

import java.util.ArrayList;
import java.util.Collections;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Set;

public class MongoDB {
    MongoCollection<org.bson.Document> urlsCollection;
    MongoCollection<org.bson.Document> stateCollection;

    static final String CONNECTION_STRING = "mongodb://localhost:27017";
    MongoDatabase searchEngineDb;
    MongoClient mongoClient;

    MongoDB() {
        initConnection();

    }

    public void initConnection() {
        try {
            //SearchEngine
            //.
            //CrawledURLS
            mongoClient = MongoClients.create(CONNECTION_STRING);
            searchEngineDb = mongoClient.getDatabase("SearchEngine");

            urlsCollection = searchEngineDb.getCollection("CrawledURLS");
            stateCollection = searchEngineDb.getCollection("State");
        } catch (Exception e) {
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
    public void setState(int state) {
        // should be called on a new
        org.bson.Document urlEntry = new org.bson.Document("Name", "state");
        org.bson.Document updateEntry = new org.bson.Document("isDone", state);
        org.bson.Document setEntry = new org.bson.Document("$set", updateEntry);

        UpdateOptions options = new UpdateOptions().upsert(true);

        stateCollection.updateOne(urlEntry, setEntry, options);

    }

    public int getState() {
        org.bson.Document searchEntry = new org.bson.Document("Name", "state");
        org.bson.Document op = Document.parse("{isDone: 1 ,_id: 0}");

        Document doc = stateCollection.find(searchEntry).projection(op).first();
        if (doc == null) {
            //no state document is found
            return -1;
        }
        // json
        return (int) doc.get("isDone");
    }

    public void deleteState() {
        org.bson.Document searchEntry = new org.bson.Document("Name", "unprocessed");
        stateCollection.deleteOne(searchEntry);
    }

    public long getUrlCount() {

        return urlsCollection.countDocuments();

    }

    public void addToStateArray(String url) {
        org.bson.Document urlEntry = new org.bson.Document("Name", "unprocessed");
        // push the url to the links


        UpdateOptions options = new UpdateOptions().upsert(true);
        ArrayList<String> arr = new ArrayList<String>();
        arr.add(url);
        //String json = "{ $push : {\"links\":{$each: [" + url + "],$slice: -500}}}";
        org.bson.Document updateEntry = new org.bson.Document("$push", new Document("links", new Document("$each",arr).append("$slice", -4000)));
        stateCollection.updateOne(urlEntry, updateEntry, options);
    }

    public void removeFromStateArray(String url) {
        org.bson.Document urlEntry = new org.bson.Document("Name", "unprocessed");
        // push the url to the links
        org.bson.Document updateEntry = new org.bson.Document("$pull", new org.bson.Document("links", url));

        stateCollection.updateOne(urlEntry, updateEntry);
    }

    public List<String> getStateArray() {
        org.bson.Document searchEntry = new org.bson.Document("Name", "unprocessed");
        org.bson.Document op = Document.parse("{links: 1 ,_id: 0}"); //only get the array of links

        Document doc = stateCollection.find(searchEntry).projection(op).first();
        if (doc == null) {
            //no state document is found
            return Collections.emptyList();
        }
        return doc.getList("links", String.class);

    }

    public void addToSuggestionsArray(String query) {
        org.bson.Document urlEntry = new org.bson.Document("Name", "suggestions");
        // TODO: ensure addToSet  is working
        org.bson.Document updateEntry = new org.bson.Document("$addToSet", new org.bson.Document("old_searches", query));
        UpdateOptions options = new UpdateOptions().upsert(true);

        stateCollection.updateOne(urlEntry, updateEntry, options);
    }

    public List<String> getSuggestionsArray() {
        org.bson.Document searchEntry = new org.bson.Document("Name", "suggestions");
        org.bson.Document op = Document.parse("{old_searches: 1 ,_id: 0}"); //only get the array of links

        Document doc = stateCollection.find(searchEntry).projection(op).first();
        if (doc == null) {
            //no state document is found
            return Collections.emptyList();
        }
        return doc.getList("old_searches", String.class);

    }

    public void getVisitedLinks() {
        Set<String> arr = Crawler.visitedLinks;
        arr.clear();
        for (String s : urlsCollection.distinct("url_link", String.class)) {
            arr.add(s);
            Crawler.websites_hashes.add(Crawler.encryptThisString(s));
        }
    }
}
