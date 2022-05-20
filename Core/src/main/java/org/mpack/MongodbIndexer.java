package org.mpack;

import org.springframework.data.util.Pair;
import com.mongodb.client.*;
import org.bson.Document;

import java.util.*;
import java.util.function.Consumer;
import java.text.DecimalFormat;

public class MongodbIndexer {
    MongoCollection<org.bson.Document> crawledCollection;
    static final String CONNECTION_STRING = "mongodb://localhost:27017";
    MongoDatabase searchEngineDb;
    MongoClient mongoClient;
    String pattern = "#.###";
    DecimalFormat decimalFormat = new DecimalFormat(pattern);

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

    public long getDocCount() {
        return searchEngineDb.getCollection("CrawledURLStest").countDocuments();
    }


    public HashMap<String, Pair<Float, String>> getHTML() {
        crawledCollection = searchEngineDb.getCollection("CrawledURLStest");
        HashMap<String, Pair<Float, String>> HTMLmap = new HashMap<>(5100);

        Consumer<Document> getContent = doc -> {
            HTMLmap.put(doc.get("url_link").toString(), Pair.of(Float.parseFloat(doc.get("page_rank").toString()), doc.get("html_body").toString()));
        };

        crawledCollection.find().limit(5000).forEach(getContent);
        return HTMLmap;
    }


    //--------------------------------------

    public void insertInvertedFile(HashMap<String, HashMap<String, WordInfo>> invertedFile, long docCount) {
        String formattedDouble;
        MongoCollection<Document> invertedFileCollection;
        //drop the collection if exists to create a new one
        boolean collectionExists = mongoClient.getDatabase("SearchEngine").listCollectionNames()
                .into(new ArrayList<String>()).contains("InvertedFile");
        if (collectionExists) {
            invertedFileCollection = searchEngineDb.getCollection("InvertedFile");
            invertedFileCollection.drop();

        }

        invertedFileCollection = searchEngineDb.getCollection("InvertedFile");
        List<Document> documents = new ArrayList<>();


        int k = 0;
        double idf = docCount;
        for (Map.Entry<String, HashMap<String, WordInfo>> set1 : invertedFile.entrySet()) {

            k++;
            if (k == 1000) {
                k = 0;
                invertedFileCollection.insertMany(documents);
                documents.clear();
            }

            Document doc = new Document();
            doc.put("token_name", set1.getKey());

            List<Document> doc_per_word = new ArrayList<>();

            for (Map.Entry<String, WordInfo> set2 : set1.getValue().entrySet()) {
                Document d = new Document();

                d.append("URL", set2.getKey()).append("TF", Integer.toString(set2.getValue().getTF())).append("normalizedTF", decimalFormat.format(set2.getValue().getNormalizedTF())).append("pageRank", decimalFormat.format(set2.getValue().getPageRank())).append("Flags", set2.getValue().getFlags())
                        .append("Positions", set2.getValue().getPositions());
                doc_per_word.add(d);

            }
            doc.append("DF", Integer.toString(set1.getValue().size()));
            doc.append("IDF", decimalFormat.format(Math.round(Math.log((idf) / set1.getValue().size()))));
            doc.append("documents", doc_per_word);
            documents.add(doc);

            //set1 -- key <word>     value <Hashmap>
            //set2 -- key <URL>      value <wordInfo>
        }
        invertedFileCollection.insertMany(documents);


    }

    public void StoreStemming(Map<String, Set<String>> equivalentStems) {
        List<Document> documents = new ArrayList<>();

        for (Map.Entry<String, Set<String>> set1 : equivalentStems.entrySet()) {
            Document doc = new Document();
            doc.put("stem_word", set1.getKey());
            doc.append("Equivalent_words", set1.getValue());
            documents.add(doc);
        }


        //check if the collection exists, if so then drop it and create a new one
        MongoCollection<Document> StemmingCollection;
        boolean collectionExists = mongoClient.getDatabase("SearchEngine").listCollectionNames()
                .into(new ArrayList<String>()).contains("StemmingCollection");
        if (collectionExists) {
            StemmingCollection = searchEngineDb.getCollection("StemmingCollection");
            StemmingCollection.drop();

        }
        StemmingCollection = searchEngineDb.getCollection("StemmingCollection");
        StemmingCollection.insertMany(documents);

    }


    //index                          0          1       2     3
    //usage                          "title","header", "p", "div"
    public void storeTextUrl(List<String> text, String url) {
        MongoCollection<Document> textURLCollection;
        textURLCollection = searchEngineDb.getCollection("TextURL");
        Document document = new Document();
        document.append("_id", url).append("Text_of_URL", text);
        textURLCollection.insertOne(document);
    }


    public void removeTextUrl()
    {
        MongoCollection<Document> textURLCollection;

        boolean collectionExists = mongoClient.getDatabase("SearchEngine").listCollectionNames()
                .into(new ArrayList<String>()).contains("TextURL");
        if (collectionExists) {
            textURLCollection = searchEngineDb.getCollection("TextURL");
            textURLCollection.drop();

        }
    }

    ArrayList<String> getTextUrl(String url) {
        return (ArrayList<String>) searchEngineDb.getCollection("TextURL").find(new Document("_id", url)).first().get("Text_of_URL");
    }

//our principle is first fit --> i.e., first fit

}
