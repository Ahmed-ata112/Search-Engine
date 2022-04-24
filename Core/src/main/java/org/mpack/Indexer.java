package org.mpack;

import ca.rmen.porterstemmer.PorterStemmer;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

public class Indexer {

    static final MongodbIndexer mongoDB = new MongodbIndexer();

    HashMap<String, HashMap<String, wordInfo>> invertedFile;
    HashMap<String, Set<String>> equivalentStems = new HashMap<String, Set<String>>();
    long documentsCount;

    public static void main(String[] arg) throws FileNotFoundException {

        Indexer obj = new Indexer();
        obj.documentsCount = mongoDB.getDocCount();
        //get crawled docs

        HashMap<String, String> htmlDocs = mongoDB.getHTML();
        ArrayList<HashMap<String, Integer>> docFlags = new ArrayList<>(2);
        for (int i = 0; i < 2; i++)
            docFlags.add(i, new HashMap<String, Integer>());
//               url     text
        //parse the HTML
        ArrayList<String> title = new ArrayList<String>(), header = new ArrayList<String>();
        HashMap<Character, List<String>> stopWords = obj.constructStopWords();

        for (Map.Entry<String, String> set : htmlDocs.entrySet()) {
            docFlags = new ArrayList<>(2);
            for (int i = 0; i < 2; i++)
                docFlags.add(i, new HashMap<String, Integer>());
            title = new ArrayList<String>();
            header = new ArrayList<String>();


            String parsedHTML = obj.ParseHTML(set.getValue(), title, header);
            obj.extractFlags(docFlags, title, header);
            List<String> tokens = obj.ExtractWords(parsedHTML);
            obj.removeStopWords(tokens, stopWords);
            obj.stemWord(tokens);

            obj.invertedFile(set.getKey(), tokens, docFlags);

        }

        mongoDB.StoreStemming(obj.equivalentStems);
        mongoDB.insertInvertedFile(obj.invertedFile, obj.documentsCount);


    }

    public Indexer() {
        invertedFile = new HashMap<String, HashMap<String, wordInfo>>();
        // id     documents  id       fields & values <TF, POSITION, FLAG>
    }

    //read the stop words
    private @NotNull HashMap<Character, List<String>> constructStopWords() throws FileNotFoundException {
        //read the file contains stop words
        File file = new File(".\\attaches\\stopwords.txt");
        Scanner scan = new Scanner(file);

        HashMap<Character, List<String>> stopWords = new HashMap<>();
        //List<String> stopWords = new ArrayList<String>();
        while (scan.hasNextLine()) {
            //append it to the list
            String stopWord = scan.nextLine();
            Character key = stopWord.charAt(0);
            if(!stopWords.containsKey(key))
            {
                stopWords.put(key, new ArrayList<String>(Collections.singleton(stopWord)));
            }
            else
                stopWords.get(key).add(stopWord);

        }

        return stopWords;
    }

    String ParseHTML(String HTMLText, ArrayList<String> title, ArrayList<String> header) {
        org.jsoup.nodes.Document parsed;
        parsed = Jsoup.parse(HTMLText);
        title.addAll(parsed.getElementsByTag("title").eachText());
        header.addAll(parsed.getElementsByTag("header").eachText());

        return parsed.text();
    }

    List<String> ExtractWords(@NotNull String text) {
        List<String> WordList = new ArrayList<String>();
        StringBuffer word = new StringBuffer();
        char c;
        for (int i = 0; i < text.length(); i++) {
            c = text.charAt(i);
            if (c <= 'z' && c >= 'a' || c <= 'Z' && c >= 'A' || c <= '9' && c >= '0')
                word.append(c);
            else {
                if (word.isEmpty()) continue;
                if (NumberUtils.isNumber(word.toString()) || word.length() < 3) ;
                else
                    WordList.add(word.toString().toLowerCase(Locale.ROOT));
                word = new StringBuffer();
            }
        }
        return WordList;
    }


    //remove them
    public void removeStopWords(@NotNull List<String> tokens, HashMap<Character, List<String>> stopWords) {
        for (int i = 0; i < tokens.size(); i++) {

            if((tokens.get(i).charAt(0) - 48) >= 0 || (tokens.get(i).charAt(0) - 48) <= 9)
                continue;
            if(stopWords.get(tokens.get(i).charAt(0)).contains(tokens.get(i).toLowerCase(Locale.ROOT)))
            //if (stopWords.contains(tokens.get(i).toLowerCase(Locale.ROOT)))
            {
                //then remove it
                tokens.remove(i);
                i--;
            }
        }
    }


    private void stemWord(@NotNull List<String> tokens) {
        PorterStemmer stem = new PorterStemmer();
        for (String token : tokens) {
            String result = stem.stemWord(token);
            if (equivalentStems.containsKey(result)) {

                equivalentStems.get(result).add(token);
            } else {
                equivalentStems.put(result, new HashSet<String>());
                equivalentStems.get(result).add(token);
            }
        }
    }


    private void invertedFile(String DocURL, List<String> tokens, ArrayList<HashMap<String, Integer>> docFlags) {
        for (int i = 0; i < tokens.size(); i++) {

            if (invertedFile.containsKey(tokens.get(i))) {
                //then go and update the positions in for this word in this doc
                //but first check if the doc exists or not
                if (invertedFile.get(tokens.get(i)).containsKey(DocURL)) {
                    //then update
                    invertedFile.get(tokens.get(i)).get(DocURL).addPosition(i);
                    invertedFile.get(tokens.get(i)).get(DocURL).incTF();
                } else {
                    //then create it
                    wordInfo container = new wordInfo();
                    container.addPosition(i);
                    container.incTF();

                    for (short k = 0; k < docFlags.size(); k++) {
                        if (docFlags.get(k).containsKey(tokens.get(i))) {
                            container.setFlags(k, docFlags.get(k).get(tokens.get(i)));
                        }
                    }
                    invertedFile.get(tokens.get(i)).put(DocURL, container);
                }

            } else {
                HashMap<String, wordInfo> docMap = new HashMap<>();
                wordInfo container = new wordInfo();
                container.addPosition(i);
                container.incTF();
                docMap.put(DocURL, container);
                invertedFile.put(tokens.get(i), docMap);
            }

        }

    }

    private void extractFlags(ArrayList<HashMap<String, Integer>> docFlags, ArrayList<String> title, ArrayList<String> header) {
        List<String> temp;
        int k = 0;
        for (String item : title) {
            temp = ExtractWords(item);
            for (String s : temp) {
                k = 0;
                if (docFlags.get(0).containsKey(s)) {
                    k = docFlags.get(0).get(s);
                }
                k++;
                docFlags.get(0).put(s, k);

            }
        }
        for (String s : header) {
            temp = ExtractWords(s);
            for (String value : temp) {
                k = 0;
                if (docFlags.get(1).containsKey(value)) {
                    k = docFlags.get(1).get(value);
                }
                k++;
                docFlags.get(1).put(value, k);

            }
        }
    }

}

