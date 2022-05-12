package org.mpack;
import ca.rmen.porterstemmer.PorterStemmer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.lang.reflect.Array;
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

    private @NotNull List<List<Document>> Stem(List<String> Phrase)
    {
        //list that contain all equivalent words from data base
        List<List<String>> EquivalentWords = new ArrayList<>();
        for (int i=0;i< Phrase.size();i++)
        {
            //get the original word , its stemming , the equivalent words and put all of them in the EquWords List
            String OriginalWord = Phrase.get(i);
            PorterStemmer stem = new PorterStemmer();
            String StemmedWord = stem.stemWord(OriginalWord);
            StemmedWord = StemmedWord.toLowerCase();
            Document Doc;
            Doc = StemmingCollection.find(new Document("stem_word", StemmedWord)).projection(Document.parse("{Equivalent_words: 1 ,_id: 0}")).first();
            if (Doc != null) {
                //make an array list of all Equivalent words with original word in the beginning of it
                ArrayList<String> arr = (ArrayList<String>) Doc.get("Equivalent_words");
                arr.remove(OriginalWord);
                arr.add(0,OriginalWord);
                EquivalentWords.add(arr);
            }
            else {
                EquivalentWords.add(new ArrayList<String>());
            }
        }
        
        //get all combinations of equivalent words of the search query
        String current = "";
        List<String> result = new ArrayList<>();
        generatePermutations(EquivalentWords,result,0,current);
        
        //construct a hashmap that contain docs mapped to its word
        Hashmap <String,Document> NameToDoc = ConstructNameToDocsHashMap(EquivalentWords);
        
        //create a list of list document which express the search query
        List<List<Document>> DocsList = new ArrayList<>();
        
        //loop on the result list and fill DocsList with the right documents
        for (int i=0;i<result.size();i++)
        {
            List<Document> temp = new ArrayList<>();
            for (int j = 0;j<Phrase.size();j++)
            {
                temp.add(NameToDoc.get(result[i][j]));
            }
            DocsList.add(temp);
        }
        
        return DocsList;
    }

   
    private void generatePermutations(@NotNull List<List<String>> lists, List<String> result, int depth, String current) {
        if (depth == lists.size()) {
            result.add(current);
            return;
        }
        
        for (int i = 0; i < lists.get(depth).size(); i++) {
            generatePermutations(lists, result, depth + 1, current+lists.get(depth).get(i));
        }
    }
    
    private Hashmap<String,Document> ConstructNameToDocsHashMap(List<List<String>> EquivalentWords)
    {
        Hashmap<String,Document> NameToDocsHM = new Hashmap<>();
        for (int i = 0;i<EquivalentWords.size();i++)
        {
            for (int j=0;j<EquivalentWords.get(i).size();j++)
            {
                Document Doc = InvertedDocs.find(new Document("token_name",  EquivalentWords.get(i).get(j))).first();
                if (Doc != null) {
                    NameToDocsHM.putIfAbsent(EquivalentWords.get(i),Doc);
                }
                else
                {
                    NameToDocsHM.putIfAbsent(EquivalentWords.get(i),null);
                }
            }
        }
        return NameToDocsHM;
    }
    
    public List<String> GetSearchTokens()
    {
        return SearchTokens;
    }


     /*void generatePermutations(@NotNull List<List<String>> lists, List<List<Document>> result, int depth, List<Document> current) {
        if (depth == lists.size()) {
            List<Document> temp = new ArrayList<>();
            temp.addAll(current);
            result.add(temp);
            return;
        }

        for (int i = 0; i < lists.get(depth).size(); i++) {

            Document Doc = InvertedDocs.find(new Document("token_name",  lists.get(depth).get(i))).first();
            if (Doc != null) {
                current.add(Doc);
            }
            else
            {
                current.add(null);
            }
            generatePermutations(lists, result, depth + 1, current);
            current.remove(current.size()-1);
        }
    }*/
    
    
   /* public HashMap<Integer,ArrayList<Document>> QueryProcessingFunction (String SearchQuery) throws FileNotFoundException {
        stopWords = Indexer.constructStopWords();
        SearchTokens = List.of(SearchQuery.toLowerCase().split(" "));
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
            String StemmedWord = Stem(SearchTokens);
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
    }*/

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
        List<String> temp = new ArrayList<>();
        temp.add("cancelled");
        temp.add("mathematical");
        q.Stem(temp);
    }
}
