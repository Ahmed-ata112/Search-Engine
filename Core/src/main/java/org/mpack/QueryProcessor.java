package org.mpack;
import ca.rmen.porterstemmer.PorterStemmer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class QueryProcessor {
    HashMap<Integer,Document> result = new HashMap<>();
    String Phrase;
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

    public HashMap<Integer,Document> QueryProcessingFunction (String SearchQuery) {
        //1- retrieve the original word document and put it in hashmap
        Document Doc = InvertedDocs.find(new Document("token_name", SearchQuery)).first();
        if (Doc != null) {
            result.putIfAbsent(0, Doc);
        }
        //2-stemming process
        String StemmedWord = Stem(SearchQuery);
        //3- retrieve documents for stemmed words from stemming collection
        Doc = StemmingCollection.find(new Document("stem_word", StemmedWord)).projection(Document.parse("{Equivalent_words: 1 ,_id: 0}")).first();
        if (Doc != null) {
            //4-make an array list of stemming words
            ArrayList<String> arr = (ArrayList<String>) Doc.get("Equivalent_words");
            for (String s : arr) {
                //5- retrieve documents for stemmed words from inverted file and put it in the hashmap
                Doc = InvertedDocs.find(new Document("token_name", s)).first();
                result.putIfAbsent(1, Doc);
            }
        }
        return result;
    }

    public static void main(String[] arg)
    {
        QueryProcessor q = new QueryProcessor();
        q.QueryProcessingFunction("cancel");
    }
}
