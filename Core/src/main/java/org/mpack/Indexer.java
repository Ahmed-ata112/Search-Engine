package org.mpack;

import ca.rmen.porterstemmer.PorterStemmer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.safety.Whitelist;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Indexer {

    static final MongodbIndexer mongoDB = new MongodbIndexer();

    HashMap<String, HashMap<String, WordInfo>> invertedFile;

    HashMap<String, Set<String>> equivalentStems = new HashMap<>();

    long documentsCount;
    static Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    static {
        root.setLevel(Level.OFF);
    }

    public static void main(String[] arg) throws FileNotFoundException {

        Indexer obj = new Indexer();
        obj.documentsCount = mongoDB.getDocCount();
        //get crawled docs
        HashMap<String, Pair<Float, String>> htmlDocs = mongoDB.getHTML();
        ///      url     body

        ArrayList<HashMap<String, Integer>> docFlags;
        ArrayList<String> title;
        ArrayList<String> header;
        HashMap<Character, List<String>> stopWords = obj.constructStopWords();

       
        for (Map.Entry<String, Pair<Float, String>> set : htmlDocs.entrySet()) {
            docFlags = new ArrayList<>(2);
            for (int i = 0; i < 2; i++)
                docFlags.add(i, new HashMap<>());

            title = new ArrayList<>();
            header = new ArrayList<>();

            String parsedHTML = obj.parseHTML(set.getValue().getSecond(), title, header);

            obj.extractFlags(docFlags, title, header);
            Pair<List<List<String>>, List<Integer>> tokens = obj.extractWords(parsedHTML);
            tokens.getFirst().get(1).add(0, title.get(0));
            mongoDB.storeTextUrl((ArrayList<String>) tokens.getFirst().get(1), set.getKey());
            obj.removeStopWords(tokens.getFirst().get(0), stopWords, tokens.getSecond());
            obj.stemWord(tokens.getFirst().get(0));

            obj.invertedFile(set.getKey(), tokens, docFlags, set.getValue().getFirst());

        }

        mongoDB.StoreStemming(obj.equivalentStems);
        mongoDB.insertInvertedFile(obj.invertedFile, obj.documentsCount);


    }

    public Indexer() {

        invertedFile = new HashMap<>();
    }

    //read the stop words
    public static @NotNull HashMap<Character, List<String>> constructStopWords() throws FileNotFoundException {
        //read the file contains stop words

        File file = new File(".\\attaches\\stopwords.txt");

        Scanner scan = new Scanner(file);


        HashMap<Character, List<String>> stopWords = new HashMap<>();
        //List<String> stopWords = new ArrayList<String>();

        while (scan.hasNextLine()) {
            //append it to the list
            String stopWord = scan.nextLine();
            Character key = stopWord.charAt(0);
            if (!stopWords.containsKey(key)) {
                stopWords.put(key, new ArrayList<String>(Collections.singleton(stopWord)));
            } else
                stopWords.get(key).add(stopWord);

        }

        return stopWords;
    }

    String parseHTML(String HTMLText, ArrayList<String> title, ArrayList<String> header) {


        String[] toRemove = {"img", "meta", "iframe", "button", "input", "style", "script", "dfn", "span", "svg", "code", "samp", "kbd", "var", "pre"};

        org.jsoup.nodes.Document parsed;
        parsed = Jsoup.parse(HTMLText);

        title.add(parsed.title());

        if (!parsed.getElementsByTag("main").isEmpty())
            parsed = Jsoup.parse(Objects.requireNonNull(parsed.getElementsByTag("main").first()).toString());

        for (String s : toRemove)
            parsed.select(s).remove();

        header.addAll(parsed.getElementsByTag("header").eachText());
        header.addAll(parsed.getElementsByTag("h1").eachText());

        //System.out.println(Jsoup.clean(parsed.text(), Safelist.none()));
        return Jsoup.clean(parsed.text(), Safelist.none());
    }

    Pair<List<List<String>>, List<Integer>> extractWords(@NotNull String text) {

        Pair<List<List<String>>, List<Integer>> wordList;
        wordList = Pair.of(new ArrayList<>(), new ArrayList<>());
        StringBuilder original = new StringBuilder();

        StringBuilder word = new StringBuilder();
        wordList.getFirst().add(new ArrayList<>());
        wordList.getFirst().add(new ArrayList<>());
        int position = -1;
        char c;
        for (int i = 0; i < text.length(); i++) {
            c = text.charAt(i);
            if (c <= 'z' && c >= 'a' || c <= 'Z' && c >= 'A' || c <= '9' && c >= '0' || c == '+' || c == '-') {
                word.append(c);
                original.append(c);
            } else if (c == ' ') {
                if (original.isEmpty()) continue;
                position++;

                wordList.getFirst().get(1).add(original.toString());


                if (!StringUtils.isNumeric(word.toString()) && !(word.equals('+') || word.equals('-')) && !word.isEmpty()) {
                    wordList.getFirst().get(0).add(word.toString().toLowerCase(Locale.ROOT));
                    wordList.getSecond().add(position);
                }
                original = new StringBuilder();
                if (!word.isEmpty())
                    word = new StringBuilder();
            } else original.append(c);
        }
        /*System.out.println(position);
        System.out.println(wordList.getFirst().size());
        System.out.println(wordList.getFirst().get(0).size());
        System.out.println(wordList.getFirst().get(1).size());
        System.out.println("-----------------------------------------------------------------------------");*/
        return wordList;
    }


    //remove them
    public static void removeStopWords(List<String> tokens, HashMap<Character, List<String>> stopWords, List<Integer> positions) {
        for (int i = 0; i < tokens.size(); i++) {

            //if ((tokens.get(i).charAt(0) - 48) >= 0 || (tokens.get(i).charAt(0) - 48) <= 9)
            if (stopWords.get(tokens.get(i).charAt(0)) == null)
                continue;
            if (stopWords.get(tokens.get(i).charAt(0)).contains(tokens.get(i).toLowerCase(Locale.ROOT)))
            //if (stopWords.contains(tokens.get(i).toLowerCase(Locale.ROOT)))
            {
                //then remove it
                tokens.remove(i);
                positions.remove(i);
                i--;
            }
        }
    }


    private void stemWord(List<String> tokens) {
        PorterStemmer stem = new PorterStemmer();
        for (String token : tokens) {
            String result = stem.stemWord(token);
            if (equivalentStems.containsKey(result)) {

                equivalentStems.get(result).add(token);
            } else {
                equivalentStems.put(result, new HashSet<>());
                equivalentStems.get(result).add(token);
            }
        }
    }


    private void invertedFile(String docURL, Pair<List<List<String>>, List<Integer>> tokens, ArrayList<HashMap<String, Integer>> docFlags, float pageRank) {
        for (int i = 0; i < tokens.getFirst().get(0).size(); i++) {

            if (invertedFile.containsKey(tokens.getFirst().get(0).get(i))) {
                //then go and update the positions in for this word in this doc
                //but first check if the doc exists or not
                if (invertedFile.get(tokens.getFirst().get(0).get(i)).containsKey(docURL)) {
                    //then update
                    invertedFile.get(tokens.getFirst().get(0).get(i)).get(docURL).addPosition(tokens.getSecond().get(i));
                    invertedFile.get(tokens.getFirst().get(0).get(i)).get(docURL).incTF();
                } else {
                    //then create it
                    WordInfo container = new WordInfo();
                    container.addPosition(tokens.getSecond().get(i));
                    container.incTF();
                    container.setPageRank(pageRank);
                    for (short k = 0; k < docFlags.size(); k++) {
                        container.setFlags(k, docFlags.get(k).getOrDefault(tokens.getFirst().get(0).get(i), 0));
                    }
                    invertedFile.get(tokens.getFirst().get(0).get(i)).put(docURL, container);
                }

            } else {
                HashMap<String, WordInfo> docMap = new HashMap<>();
                WordInfo container = new WordInfo();
                container.addPosition(tokens.getSecond().get(i));
                container.incTF();
                container.setPageRank(pageRank);
                docMap.put(docURL, container);

                for (short k = 0; k < docFlags.size(); k++) {
                    container.setFlags(k, docFlags.get(k).getOrDefault(tokens.getFirst().get(0).get(i), 0));
                }
                invertedFile.put(tokens.getFirst().get(0).get(i), docMap);
            }

        }

    }

    private void extractFlags(ArrayList<HashMap<String, Integer>> docFlags, ArrayList<String> title, ArrayList<String> header) {
        List<String> temp;
        int k;
        for (String item : title) {
            temp = extractWords(item).getFirst().get(0);
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
            temp = extractWords(s).getFirst().get(0);
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