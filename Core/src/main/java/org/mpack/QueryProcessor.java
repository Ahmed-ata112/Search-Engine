
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
import java.util.stream.Collectors;


public class QueryProcessor {
    String PhraseIndicator;
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

    public @NotNull List<List<Document>> ProcessQuery(List<String> Phrase) throws FileNotFoundException {
        //triming the search string
        for (int i = 0;i<Phrase.size();i++)
            Phrase.set(i,Phrase.get(i).trim());
        //intialzie data member variables
        SearchTokens = Phrase;
        if (Phrase.size()==1)
            PhraseIndicator = "";
        else
            PhraseIndicator = Phrase.stream().map(i -> String.valueOf(i)).collect(Collectors.joining(" "));
        //remove stop words
        stopWords = Indexer.constructStopWords();
        Indexer.removeStopWords(SearchTokens,stopWords, null);
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
                arr.add(StemmedWord);
                EquivalentWords.add(arr);
            }
            else {
                EquivalentWords.add(new ArrayList<String>());
            }
        }

        //get all combinations of equivalent words of the search query
        String current = "";
        List<String> QueryEquivalentWordsPermutation = new ArrayList<>();
        generatePermutations(EquivalentWords,QueryEquivalentWordsPermutation,0,current);

        //construct a hashmap that contain docs mapped to its word
        HashMap<String,Document> NameToDoc = ConstructNameToDocsHashMap(EquivalentWords);

        //create a list of list document which express the search query
        List<List<Document>> DocsList = new ArrayList<>();

        //loop on the QueryEquivalentWordsPermutation list and fill DocsList with the right documents
        for (int i=0;i<QueryEquivalentWordsPermutation.size();i++)
        {
            String[] splitedString = QueryEquivalentWordsPermutation.get(i).trim().split(" ");
            List<Document> temp = new ArrayList<>();
            for (int j = 0;j<Phrase.size();j++)
            {
                temp.add(NameToDoc.get(splitedString[j]));
            }
            DocsList.add(temp);
        }

        return DocsList;
    }


    private void generatePermutations(@NotNull List<List<String>> lists, List<String> result, int depth, String current) {
        if (depth == lists.size()) {
            System.out.println(current);
            result.add(current);
            return;
        }

        for (int i = 0; i < lists.get(depth).size(); i++) {
            generatePermutations(lists, result, depth + 1, current+" "+lists.get(depth).get(i));
        }
    }

    private HashMap<String,Document> ConstructNameToDocsHashMap(List<List<String>> EquivalentWords)
    {

        HashMap<String,Document> NameToDocsHM = new HashMap<>();
        for (int i = 0;i<EquivalentWords.size();i++)
        {
            for (int j=0;j<EquivalentWords.get(i).size();j++)
            {
                Document Doc = InvertedDocs.find(new Document("token_name",  EquivalentWords.get(i).get(j))).first();
                if (Doc != null) {
                    NameToDocsHM.putIfAbsent(EquivalentWords.get(i).get(j),Doc);
                }
                else
                {
                    NameToDocsHM.putIfAbsent(EquivalentWords.get(i).get(j),null);
                }
            }
        }
        return NameToDocsHM;
    }

    public List<String> GetSearchTokens()
    {
        return SearchTokens;
    }

    //if the search query is one word return en ampty string otherwise it return the search query as is is
    public String GetQueryPhraseIndicator(){
        return PhraseIndicator;
    }

    public static void main(String[] arg) throws FileNotFoundException {
        QueryProcessor q = new QueryProcessor();
        List<String> temp = new ArrayList<>();
        temp.add("cancelled");
        temp.add("tree");
        q.ProcessQuery(temp);

    }
}
