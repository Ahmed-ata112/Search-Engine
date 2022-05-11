package org.mpack;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;
import com.mongodb.client.*;
import org.bson.Document;

import javax.print.Doc;
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
        /*MongoCollection<Document> textURLCollection;
        textURLCollection = searchEngineDb.getCollection("TextURL");
        textURLCollection.drop();*/
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
        return searchEngineDb.getCollection("CrawledURLS").countDocuments();
    }


    public HashMap<String, Pair<Float, String>> getHTML() {
        crawledCollection = searchEngineDb.getCollection("CrawledURLS");
        HashMap<String, Pair<Float, String>> HTMLmap = new HashMap<>(5100);

        Consumer<Document> getContent = doc -> {
            HTMLmap.put(doc.get("url_link").toString(), Pair.of(Float.parseFloat(doc.get("page_rank").toString()), doc.get("html_body").toString()));
        };
        crawledCollection.find().limit(3000).forEach(getContent);
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

    public void StoreTextUrl(List<String> text, String url) {
        MongoCollection<Document> textURLCollection;
        textURLCollection = searchEngineDb.getCollection("TextURL");
        Document document = new Document();
        document.append("_id", url).append("Text_of_URL", text);
        textURLCollection.insertOne(document);
    }

    ArrayList<String> getTextUrl(String url) {
        return (ArrayList<String>) searchEngineDb.getCollection("TextURL").find(new Document("_id", url)).first().get("Text_of_URL");
    }

//our principle is first fit --> i.e., first fit
/*
    void paragraphToShow(String url, List<String> words)
    {
        MongoCollection<Document> textURLCollection;
        textURLCollection = searchEngineDb.getCollection("TextURL");
        Document found = (Document) textURLCollection.find(new Document("_id", url)).first();
        if(found != null)
        {
            String text = found.get("Text_of_URL").toString();
            StringBuilder word = new StringBuilder();
            char c;
            for (int i = 0; i < text.length(); i++) {
                c = text.charAt(i);
                if (c <= 'z' && c >= 'a' || c <= 'Z' && c >= 'A' || c <= '9' && c >= '0')
                    word.append(c);
                else {
                    if (word.isEmpty()) continue;
                    if (!StringUtils.isNumeric(word.toString()))
                    {
                        //wordList.add(word.toString().toLowerCase(Locale.ROOT));
                        //compare
                        boolean picked = false;
                        for(int k = 0; k < words.size(); k++)
                        {
                            if(word.equals(words.get(k)))
                            {
                                picked = true;
                                break;
                            }
                        }

                        if(picked)
                        {
                            //then bring 15 words before and 15 words after it then return this paragraph
                            if(i <= 15)
                            {
                                //from the beginning until we find a space after 30 words
                            }

                            else
                            {

                            }
                        }
                    }

                    word = new StringBuilder();
                }
            }
        }
    }
*/














/*

    public void CalcTF_IDF(String word, HashMap<String, Double> url_priority)
    {
        MongoCollection<org.bson.Document> indexerCollection = searchEngineDb.getCollection("InvertedFile");
        double IDF = Double.valueOf(0);;
        double TF = Double.valueOf(0);
        double priority = Double.valueOf(0);

        Document found = (Document) indexerCollection.find(new Document("token_name", word)).first();
        if(found != null)
        {
            IDF = Double.parseDouble(found.get("IDF").toString());


            //Float TF = 0.0;
            List<Document> webPages = (List<Document>) found.get("documents");

            //I think there is a more efficient way to get the url of the word rather than this
            for (Document d: webPages) {
                if(Float.parseFloat(d.get("normalizedTF").toString()) >= 0.5) continue;
                TF = Double.parseDouble(d.get("TF").toString());  // to make sure -48
                priority = TF*IDF;
                //search in the hashmap for this url or insert it if not found
                if(url_priority.containsKey(d.getString("URL")))
                {
                    //then update the priority
                    url_priority.put(d.getString("URL"), url_priority.get(d.getString("URL") + priority));
                }
                else
                {
                    url_priority.put(d.getString("URL"), priority);
                }
            }

        }

    }
*/






    /*

    public List<Pair<Float, Float>> getIDF_TF(String url)
    {
        List<Pair<Float, Float>> IDF_TF = new ArrayList<Pair<Float, Float>>();
        HashMap<String, String> HTMLmap = new HashMap<String, String>();
        MongoCollection<org.bson.Document> indexerCollection = searchEngineDb.getCollection("InvertedFile");


        Consumer<Document> getContent = doc -> {
            HTMLmap.put(doc.get("url_link").toString(), doc.get("html_body").toString());
        };

        crawledCollection.find().forEach(getContent);
        return IDF_TF;  //return get ???
    }




    public Float getTF_IDF(String word, String url)
    {
        MongoCollection<org.bson.Document> indexerCollection = searchEngineDb.getCollection("InvertedFile");
        Float IDF;

        Document found = (Document) indexerCollection.find(new Document("token_name", word));
        IDF = (Float)found.get("IDF");

        Float TF = Float.valueOf(0);
        //Float TF = 0.0;
        List<Document> webPages = (List<Document>) found.get("documents");

        //I think there is a more efficient way to get the url of the word rather than this
        for (Document d: webPages) {

            if(d.get("URL").equals(url))
            {

                TF = Float.parseFloat(d.getString("TF"));  // to make sure -48
                break;
            }
        }

        //Document urls = (Document) webPages  //find(new Document("URL", word));

        //searchEngineDb
        return TF*IDF;
    }
*/
}

