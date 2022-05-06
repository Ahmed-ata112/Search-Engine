package org.mpack;
import ca.rmen.porterstemmer.PorterStemmer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.util.*;

public class QueryProcessor {
    HashMap<Integer,ArrayList<Document>> result = new HashMap<>();
    String Phrase;
    List<String> SearchTokens;
    HashMap<Character, List<String>> stopWords = new HashMap<>();
    MongoClient mongoClient;
    MongoDatabase DataBase;
    MongoCollection<org.bson.Document> InvertedDocs;
    MongoCollection<org.bson.Document> StemmingCollection;
    public QueryProcessor()
    {
        InitMongoDb();
    }

    private void InitMongoDb()
    {
        mongoClient = MongoClients.create(MongodbIndexer.CONNECTION_STRING);
        DataBase = mongoClient.getDatabase("SearchEngine");
        InvertedDocs = DataBase.getCollection("InvertedFile");
        StemmingCollection = DataBase.getCollection("StemmingCollection");
    }

    private @NotNull String Stem(String Word)
    {
        String StemmedWord;
        PorterStemmer stem = new PorterStemmer();
        StemmedWord = stem.stemWord(Word);
        return StemmedWord.toLowerCase();
    }
    
    public List<String> GetSearchTokens()
    {
        return SearchTokens;
    }

    public HashMap<Integer,ArrayList<Document>> QueryProcessingFunction (String SearchQuery) throws FileNotFoundException {
        stopWords = Indexer.constructStopWords();
        SearchTokens = List.of(SearchQuery.split(" "));
        Indexer.removeStopWords(SearchTokens,stopWords);
        ArrayList<Document> OriginalWords = new ArrayList<>();
        ArrayList<Document> StemmedWords = new ArrayList<>();
        for (int i=0;i<SearchTokens.size();i++) {
            //1- retrieve the original word document and put it in hashmap
            Document Doc = InvertedDocs.find(new Document("token_name", SearchTokens.get(i))).first();
            if (Doc != null) {
                OriginalWords.add(Doc);
            }
            //2-stemming process
            String StemmedWord = Stem(SearchTokens.get(i));
            //3- retrieve documents for stemmed words from stemming collection
            Doc = StemmingCollection.find(new Document("stem_word", StemmedWord)).projection(Document.parse("{Equivalent_words: 1 ,_id: 0}")).first();
            if (Doc != null) {
                //4-make an array list of stemming words
                ArrayList<String> arr = (ArrayList<String>) Doc.get("Equivalent_words");
                for (String s : arr) {
                    //5- retrieve documents for stemmed words from inverted file and put it in the hashmap
                    Doc = InvertedDocs.find(new Document("token_name", s)).first();
                    StemmedWords.add(Doc);
                }
            }
            result.putIfAbsent(0,OriginalWords);
            result.putIfAbsent(1,StemmedWords);
        }
        return result;
    }

    public HashMap<Integer,ArrayList<Document>> PhraseProcessing (String SearchQuery) {
        //1-construct and remove stop words
        //2-split the phrase into array of words
        //3-search for a certain word in the database
        //4-parse retrieved URLs
        //5- if it contain the whole phrase put it into the hashmap else parse the next URL
        return result;
    }


    public static void main(String[] arg) throws FileNotFoundException {
        QueryProcessor q = new QueryProcessor();
        q.QueryProcessingFunction("salary");
    }
}
