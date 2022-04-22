package org.mpack;

import com.mongodb.client.*;
import org.bson.Document;

import java.util.*;
import java.util.function.Consumer;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;


public class MongodbIndexer {
    MongoCollection<org.bson.Document> crawledCollection;
    static final String CONNECTION_STRING = "mongodb://localhost:27017";
    MongoDatabase searchEngineDb;
    MongoClient mongoClient;
    private MongoCollection<Document> InvertedFileCollection;

    MongodbIndexer() {
        initConnection();
    }

    public void initConnection() {
        try {
            //SearchEngine
            //.
            //CrawledURLS
            mongoClient = MongoClients.create(CONNECTION_STRING);
            searchEngineDb = mongoClient.getDatabase("SearchEngine");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public long getDocCount()
    {
        return searchEngineDb.getCollection("CrawledURLS").countDocuments();
    }
    public HashMap<String, String> getHTML()
    {
        crawledCollection = searchEngineDb.getCollection("CrawledURLS");
        HashMap<String, String> HTMLmap = new HashMap<String, String>();

        Consumer<Document> getContent = doc -> {
            HTMLmap.put(doc.get("url_link").toString(), doc.get("html_body").toString());
        };

        crawledCollection.find().forEach(getContent);
        return HTMLmap;
    }
    public void terminateConnection() {
    }



    //--------------------------------------
    public void insertInvertedFile(HashMap<String, HashMap<String, wordInfo>>  invertedFile, long docCount)
    {
        InvertedFileCollection = searchEngineDb.getCollection("InvertedFile");
        List<Document> documents = new ArrayList<>();
        
        int k = 0;
        double idf = docCount;
        for(Map.Entry<String, HashMap<String, wordInfo>> set1 : invertedFile.entrySet())
        {

            k++;
            if(k == 1000)
            {
                k = 0;
                InvertedFileCollection.insertMany(documents);
                documents.clear();
            }

            Document doc = new Document();
            doc.put("token_name", set1.getKey());
            List<Document> doc_per_word = new ArrayList<>();
            for(Map.Entry<String, wordInfo> set2 : set1.getValue().entrySet()) {
                Document d = new Document();
                d.append("URL",set2.getKey()).append("TF", set2.getValue().getTF()).append("Flags", set2.getValue().getFlags())
                        .append("Positions", set2.getValue().getPositions());
                doc_per_word.add(d);

            }
            doc.append("DF", set1.getValue().size());
            doc.append("IDF",  Math.log((idf) / set1.getValue().size()));
            doc.append("documents", doc_per_word);
            documents.add(doc);

            //set1 -- key <word>     value <Hashmap>
            //set2 -- key <URL>      value <wordInfo>
        }
        InvertedFileCollection.insertMany(documents);


    }
    public void StoreStemming(HashMap<String, Set<String>> equivalentStems) {
        List<Document> documents = new ArrayList<>();

        for (Map.Entry<String, Set<String>> set1 : equivalentStems.entrySet()) {
            Document doc = new Document();
            doc.put("stem_word", set1.getKey());
            doc.append("Equivalent_words", set1.getValue());
            documents.add(doc);
        }
        MongoCollection<Document> StemmingCollection = searchEngineDb.getCollection("StemmingCollection");
        StemmingCollection.insertMany(documents);
    }
}
    
