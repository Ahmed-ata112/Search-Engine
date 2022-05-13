package org.mpack;

import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

public class MongoDB {
    MongoCollection<org.bson.Document> urlsCollection;


    MongoCollection<org.bson.Document> stateCollection;
    MongoCollection<org.bson.Document> stateUrlsCollection;
    MongoCollection<org.bson.Document> relationsCollection;

    static final String CONNECTION_STRING = "mongodb://localhost:27017";
    MongoDatabase searchEngineDb;
    MongoClient mongoClient;

    public MongoDB() {
        initConnection();

    }

    public void initConnection() {
        try {
            //SearchEngine
            //.
            //CrawledURLS
            mongoClient = MongoClients.create(CONNECTION_STRING);
            searchEngineDb = mongoClient.getDatabase("SearchEngine");

            //urlsCollection = searchEngineDb.getCollection("CrawledURLS");
            urlsCollection = searchEngineDb.getCollection("CrawledURLStest");
            relationsCollection = searchEngineDb.getCollection("PagesRelations");
            stateCollection = searchEngineDb.getCollection("State");
            stateUrlsCollection = searchEngineDb.getCollection("StateURLS");
            stateUrlsCollection.createIndex(new Document(new org.bson.Document("url_link", -1)));

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertUrl(String url, String html) {
        org.bson.Document urlEntry = new org.bson.Document("_id", new ObjectId());
        urlEntry.append("url_link", url)
                .append("page_rank", 0.0)
                .append("html_body", html);
        urlsCollection.insertOne(urlEntry);
    }

    public void updateUrl(String url, String html) {
        org.bson.Document urlEntry = new org.bson.Document("url_link", url);
        org.bson.Document updateEntry = new org.bson.Document("$set", new org.bson.Document("url_link", url)
                .append("html_body", html));
        UpdateOptions options = new UpdateOptions().upsert(true);

        urlsCollection.updateOne(urlEntry, updateEntry, options);
    }

    public void setPageRank(String url, double rank) {

        org.bson.Document urlEntry = new org.bson.Document("url_link", url);
        org.bson.Document updateEntry = new org.bson.Document("$set", new org.bson.Document("page_rank", rank));
        //UpdateOptions options = new UpdateOptions().upsert(true);

        urlsCollection.updateOne(urlEntry, updateEntry);

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

    public void resetStateForReCrawling() {
        stateUrlsCollection.drop();
        urlsCollection.drop();
        relationsCollection.drop();
    }

    public long getUrlCount() {

        return urlsCollection.countDocuments();

    }

  /*  public void addToStateArray(String url) {
        org.bson.Document urlEntry = new org.bson.Document("Name", "unprocessed");
        // push the url to the links


        UpdateOptions options = new UpdateOptions().upsert(true);
        ArrayList<String> arr = new ArrayList<String>();
        arr.add(url);
        //String json = "{ $push : {\"links\":{$each: [" + url + "],$slice: -500}}}";
        org.bson.Document updateEntry = new org.bson.Document("$push", new Document("links", new Document("$each", arr).append("$slice", -4000)));
        stateCollection.updateOne(urlEntry, updateEntry, options);
    }*/

    public void addToStateUrls(String url) {
        org.bson.Document urlEntry = new org.bson.Document("_id", new ObjectId());
        urlEntry.append("url_link", url);
        stateUrlsCollection.insertOne(urlEntry);
    }

    public void removeFromStateUrls(String url) {
        org.bson.Document urlEntry = new org.bson.Document("url_link", url);
        stateUrlsCollection.deleteOne(urlEntry);
    }

    public List<String> getStateURLs() {
        ArrayList<String> arr = new ArrayList<>();
        for (String s : stateUrlsCollection.distinct("url_link", String.class)) {
            arr.add(s);
        }
        return arr;
    }

  /*  public void removeFromStateArray(String url) {
        org.bson.Document urlEntry = new org.bson.Document("Name", "unprocessed");
        // push the url to the links
        org.bson.Document updateEntry = new org.bson.Document("$pull", new org.bson.Document("links", url));

        stateCollection.updateOne(urlEntry, updateEntry);
    }*/

  /*  public List<String> getStateArray() {
        org.bson.Document searchEntry = new org.bson.Document("Name", "unprocessed");
        org.bson.Document op = Document.parse("{links: 1 ,_id: 0}"); //only get the array of links

        Document doc = stateCollection.find(searchEntry).projection(op).first();
        if (doc == null) {
            //no state document is found
            return Collections.emptyList();
        }
        return doc.getList("links", String.class);

    }*/

    public void addToRelationsDB(String root, Set<String> related) {
        org.bson.Document urlEntry = new org.bson.Document("_id", root);
        urlEntry.append("relations", related);

        relationsCollection.insertOne(urlEntry);
    }

    public List<Document> getRelations() {
        List<Document> items = new ArrayList<>();
        try (MongoCursor<Document> cursor = relationsCollection.find().iterator()) {
            while (cursor.hasNext()) {
                items.add(cursor.next());
            }
        }
        return items;
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


    public void getAllArraysBAck() {
        Crawler.visitedLinks.clear();
        for (String s : urlsCollection.distinct("url_link", String.class)) {
            Crawler.visitedLinks.add(s);
            Crawler.websites_hashes.add(Crawler.encryptThisString(s));
        }

        List<Document> r = getRelations();
        Crawler.pagesEdges.clear();
        
        for (Document d : r) {
            String root = (String) d.get("_id");
            List<String> others = d.getList("relations", String.class);
            Crawler.pagesEdges.put(root, new HashSet<String>(others));
        }
        System.out.println(Crawler.pagesEdges);
    }


}
