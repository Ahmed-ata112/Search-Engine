package org.mpack;

import org.bson.Document;
import org.springframework.data.util.Pair;


import java.util.*;
//import javafx.util.Pair;


//word1 --> doc1  doc2  doc3  doc4
//word2 --> doc2  doc4  doc6  doc8
//word3 --> doc4  doc6  doc9
//so on

//IDF --> per word

//TF --> per doc


class paragraphGetter implements Runnable {
    List<String> phrase;
    ArrayList<collections> collectionsList;

    int count;
    int wordsRemoved;
    int queryLen;
    boolean isPhraseSearch;
    MongodbIndexer mongoDB;

    Comparator<Pair<Integer, Integer>> sortPositions = Comparator.comparingInt(Pair::getFirst);

    @Override
    public void run() {
        int id = Integer.parseInt(Thread.currentThread().getName());
        int start;
        int end;
        collections current;

        start = (collectionsList.size() / count) * id;
        if (id == count - 1)
            end = collectionsList.size();
        else
            end = start + collectionsList.size() / count;
        int j = 0;
        for (int i = start; i < end; i++) {

            current = collectionsList.get(i);
            if(isPhraseSearch && queryLen > current.token_count)
            {

                current.ifDeleted = true;
                continue;
            }
            getParagraph(current);
        }

    }

    void getParagraph(collections collection) {

        ArrayList<String> text = mongoDB.getTextUrl(collection.url);

        int startParagraph;
        int endParagraph;
        int startSearch = 0;
        int endSearch = 0;
        StringBuilder parag = new StringBuilder();

        collection.wordNear = 0;

        collection.title = text.get(0);

        collection.ifDeleted = false;

        collection.positions.sort(sortPositions);

        ArrayList<Pair<Integer, Integer>> windowList = Interval.findSmallestWindow((ArrayList<Pair<Integer, Integer>>) collection.positions, collection.token_count, isPhraseSearch);

        for(int l = 0; l < windowList.size(); l++) {
            Pair<Integer, Integer> window = windowList.get(l);
            if (window == null) {
                collection.paragraph = "";
                return;
            }
            int windowLen = window.getSecond() - window.getFirst() + 1;


            //if phrase searching
            if (isPhraseSearch) {
                if (phrase.size() < windowLen) {
                    continue;
                }


                startSearch = Math.max(1, window.getFirst() - wordsRemoved);
                endSearch = Math.min(text.size(), startSearch + phrase.size());

            }

            startParagraph = Math.max(1, window.getFirst() - 20);
            endParagraph = Math.min(text.size(), window.getSecond() + 21);

            collection.wordNear = windowLen;

            collection.subQuery = (windowLen == phrase.size()) ? 1 : 0;


            int i = 1;
            if (isPhraseSearch) {

                String ptemp, ttemp;
                int k;
                for (k = startSearch + 1; k < endSearch - 1; k++) {
                    if (!phrase.get(i).equals(text.get(k).toLowerCase(Locale.ROOT))) {
                        break;
                    }
                    i++;
                }
                if(k < endSearch - 1) continue;
                ptemp = phrase.get(0).toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "");
                ttemp = text.get(startSearch).toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "");
                if (!ptemp.equals(ttemp)) {
                   continue;
                }
                ptemp = phrase.get(phrase.size() - 1).toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "");
                ttemp = text.get(endSearch - 1).toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "");
                if (!ptemp.equals(ttemp)) {
                    continue;
                }
            }
            for (int k = startParagraph; k < endParagraph; k++) {

                parag.append(text.get(k)).append(" ");

            }
            if(!isPhraseSearch) break;
        }
        if(isPhraseSearch && parag.toString().isEmpty()) {collection.ifDeleted = true; return;}
        collection.paragraph = parag.toString();
        return;
    }

}

public class Ranker {

    static final MongodbIndexer mongoDB = new MongodbIndexer();
    static HashSet<String> allUrls = new HashSet<>();
    static public void clearAllUrls(){
        allUrls = new HashSet<>();
    }

    Comparator<collections> urlPriority = (url2, url1) -> url1.compare(url2);


    public List<collections> ranker2(String phrase, List<Document> retDoc, List<String> originalTokens, boolean isPhraseSearching, int wordsRem) {


        System.out.println(isPhraseSearching);
        HashMap<String, Integer> urlPosition = new HashMap<>();
        ArrayList<collections> rankedPages = new ArrayList<>();


        ArrayList<String> query = new ArrayList<>();




        double IDF = 0;
        double TF = 0;
        double priority = 0;
        double pagRank;


        for (Document document : retDoc) {


            collections url;
            query.add(document.get("token_name").toString());
            IDF = Double.parseDouble(document.get("IDF").toString());
            List<Document> webPages = (List<Document>) document.get("documents");
            //I think there is a more efficient way to get the url of the word rather than this
            for (Document d : webPages) {

                if(allUrls.contains(d.getString("URL"))) continue;

                List<Integer> _flags;
                /*_flags.set(0, 0);
                _flags.set(1, 0);*/
                TF = Double.parseDouble(d.get("normalizedTF").toString());  // to make sure -48 ?
                _flags = (ArrayList<Integer>) (d.get("Flags"));
                List<Integer> positions = new ArrayList<>();
                positions = (ArrayList<Integer>) (d.get("Positions"));

                pagRank = Double.parseDouble(d.get("pageRank").toString());
                priority = TF * IDF;


                //search in the hashmap for this url or insert it if not found
                if (urlPosition.containsKey(d.getString("URL"))) {
                    //then update the priority
                    url = rankedPages.get(urlPosition.get(d.getString("URL")));

                    //then update the priority
                    url.token_count = url.token_count + 1;
                    System.out.println(url.token_count);
                    url.priority += priority;
                    url.flags.add(url.flags.get(0) + _flags.get(0));
                    url.flags.add(url.flags.get(1) + _flags.get(1));
                    rankedPages.set(urlPosition.get(d.getString("URL")), url);

                    //url.pagerank = pagRank; //no need - already done at the first insertion


                } else {
                    url = new collections();
                    url.flags = _flags;
                    url.priority = priority;

                    url.positions = new ArrayList<>();

                    url.token_count = 1;
                    url.url = d.getString("URL");
                    rankedPages.add(url);
                    urlPosition.put(url.url, rankedPages.size() - 1);
                }

/*
                if(url.url.equals("https://www.avclub.com/7-new-graphic-novels-to-get-you-through-the-coronavirus-1842526466"))
                {
                    System.out.println("url is found-------------------------------------------");

                }

*/

                for (int pos :
                        positions) {              //starts from 1 as 0 is the phrase ==> we may remove it
                    url.positions.add(Pair.of(pos, query.size() - 1));
                }

            }
        }
        System.out.println(rankedPages.size());

        paragraphGetter pGet = new paragraphGetter();
        pGet.isPhraseSearch = isPhraseSearching;

        pGet.collectionsList = rankedPages;

        pGet.queryLen = retDoc.size();
        pGet.mongoDB = mongoDB;

        pGet.phrase = originalTokens;
        pGet.wordsRemoved = wordsRem;

        pGet.count = Math.max(1, rankedPages.size() / 100);

        ArrayList<Thread> threads = new ArrayList<>(pGet.count);



        try {
            for (int i = 0; i < pGet.count; i++) {
                threads.add(new Thread(pGet));
                threads.get(i).setName(Integer.toString(i));
                threads.get(i).start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        ArrayList<collections> temp = null;
        if(isPhraseSearching) {
            temp = new ArrayList<>();

            for (collections rankedPage : rankedPages) {

                if (!rankedPage.ifDeleted) temp.add(rankedPage);

            }
            rankedPages = temp;
        }
        allUrls.addAll(urlPosition.keySet());

        rankedPages.sort(urlPriority);

        return rankedPages;
    }


}