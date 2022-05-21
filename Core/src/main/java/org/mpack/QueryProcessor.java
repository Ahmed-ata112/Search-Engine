package org.mpack;

import ca.rmen.porterstemmer.PorterStemmer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


public class QueryProcessor {
    String moreThanOneIndicator;
    List<String> searchTokens;
    static HashMap<Character, List<String>> stopWords = new HashMap<>();
    MongoClient mongoClient;
    MongoDatabase DataBase;
    MongoCollection<org.bson.Document> InvertedDocs;
    MongoCollection<org.bson.Document> StemmingCollection;

    public QueryProcessor() {
        InitMongoDb();
    }

    private void InitMongoDb() {
        mongoClient = MongoClients.create(MongodbIndexer.CONNECTION_STRING);
        DataBase = mongoClient.getDatabase("SearchEngine");
        InvertedDocs = DataBase.getCollection("InvertedFile");
        StemmingCollection = DataBase.getCollection("StemmingCollection");
    }

    public @NotNull List<List<Document>> ProcessQuery(List<String> Phrase, boolean isPhraseSearching) throws FileNotFoundException {
        //initialize data member variables
        searchTokens = Phrase;
        if (Phrase.size() == 1)
            moreThanOneIndicator = "";
        else
            moreThanOneIndicator = Phrase.stream().map(String::valueOf).collect(Collectors.joining(" "));
        //remove stop words
        if (stopWords.isEmpty())
            stopWords = Indexer.constructStopWords();

        Indexer.removeStopWords(searchTokens, stopWords);
        //list that contain all equivalent words from database
        List<List<String>> EquivalentWords = new ArrayList<>();

        PorterStemmer stem = new PorterStemmer();
        for (String currentWord : searchTokens) {
            //get the original word , its stemming , the equivalent words and put all of them in the EquWords List


            String currentRoot = stem.stemWord(currentWord).toLowerCase();

            //TODO Replace this with a function from the database handler
            Document doc = StemmingCollection.find(new Document("stem_word", currentRoot)).projection(Document.parse("{Equivalent_words: 1 ,_id: 0}")).first();

            if (doc != null) {
                //make an array list of all Equivalent words with original word in the beginning of it
                ArrayList<String> arr = new ArrayList<>();
                if (!isPhraseSearching) {
                    arr = (ArrayList<String>) doc.get("Equivalent_words");
                    arr.add(currentRoot);
                    arr.remove(currentWord);
                }
                arr.add(0, currentWord);
                EquivalentWords.add(arr);
            }

        }
        //get all combinations of equivalent words of the search query
        String current = "";
        List<String> queryEquivalentWordsPermutation = new ArrayList<>();
        generatePermutations(EquivalentWords, queryEquivalentWordsPermutation, 0, current);

        //construct a hashmap that contain docs mapped to its word
        HashMap<String, Document> nameToDocsHashMap = constructNameToDocsHashMap(EquivalentWords);
        //create a list of list document which express the search query
        List<List<Document>> docslist = new ArrayList<>();

        //loop on the QueryEquivalentWordsPermutation list and fill DocsList with the right documents
        for (String value : queryEquivalentWordsPermutation) {

            String[] splitString = value.trim().split(" ");
            List<Document> temp = new ArrayList<>();

            for (String s : splitString) {
                var t = nameToDocsHashMap.get(s);
                if (t != null)
                    temp.add(t);
            }
            docslist.add(temp);
        }

        return docslist;
    }


    private void generatePermutations(@NotNull List<List<String>> lists, List<String> result, int depth, String current) {
        if (depth == lists.size()) {
            System.out.println(current);
            result.add(current);
            return;
        }

        for (int i = 0; i < lists.get(depth).size(); i++) {
            generatePermutations(lists, result, depth + 1, current + " " + lists.get(depth).get(i));
        }
    }

    private HashMap<String, Document> constructNameToDocsHashMap(List<List<String>> equivalentWords) {

        HashMap<String, Document> nameToDocsHM = new HashMap<>();
        Document doc;

        for (List<String> equivalentWord : equivalentWords) {
            for (String s : equivalentWord) {
                doc = InvertedDocs.find(new Document("token_name", s)).first();
                nameToDocsHM.putIfAbsent(s, doc);
            }
        }
        return nameToDocsHM;
    }

    public List<String> GetSearchTokens() {
        return searchTokens;
    }

    //if the search query is one word return en ampty string otherwise it return the search query as is is
    public String GetQueryPhraseIndicator() {
        return moreThanOneIndicator;
    }

    public static void main(String[] arg) throws FileNotFoundException {
        QueryProcessor q = new QueryProcessor();
        List<String> temp = new ArrayList<>();
        temp.add("cancelled");
        temp.add("tree");
        q.ProcessQuery(temp, false);

    }
}
