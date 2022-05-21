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
    ArrayList<String> phrase;
    ArrayList<collections> collectionsList;

    int count;
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
        for (int i = start; i < end; i++) {
            current = collectionsList.get(i);
            getParagraph(current);
            synchronized (this) {
                collectionsList.add(current);
            }
        }

    }

    void getParagraph(collections collection) {
        ArrayList<String> text = mongoDB.getTextUrl(collection.url);

        int start;
        int end;
        StringBuilder parag = new StringBuilder();

        collection.wordNear = 0;

        collection.title = text.get(0);
        //if (collection.url.equals("https://time.com/6156587/time-studios-to-create-childrens-series-based-on-the-littles-nft/"))

        collection.positions.sort(sortPositions);
        //System.out.println(collection.positions);
        Pair<Integer, Integer> window = Interval.findSmallestWindow((ArrayList<Pair<Integer, Integer>>) collection.positions, collection.token_count);
        if (window == null) {
            collection.paragraph = "";
            return;
        }
        int windowLen = window.getSecond() - window.getFirst() + 1;
        if (windowLen == collection.token_count) collection.wordNear = 100;
       /* if(windowLen < 20)
        {*/
        //windowLen = (int) Math.ceil((float)(20 - windowLen - 1) / 2);
        start = Math.max(1, window.getFirst() - 4);
        end = Math.min(text.size() - 1, window.getSecond() + 4);
      /*  }
        else
        {
            start = window.getFirst();
            end = window.getSecond();
        }*/


        for (int k = start - 1; k < end; k++) {
            parag.append(text.get(k + 1)).append(" ");
        }
        collection.paragraph = parag.toString();
    }

}

public class Ranker {
    static final MongodbIndexer mongoDB = new MongodbIndexer();
    static HashSet<String> allUrls = new HashSet<>();


    Comparator<collections> urlPriority = (url2, url1) -> url1.compare(url2);


    public List<collections> ranker2(String phrase, List<Document> retDoc) {


        HashMap<String, Integer> urlPosition = new HashMap<>();
        ArrayList<collections> rankedPages = new ArrayList<>();


        ArrayList<String> query = new ArrayList<>();


        query.add(phrase);


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
                allUrls.add(d.getString("URL"));
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
                    url.token_count++;
                    url.priority += priority;
                    url.flags.add(url.flags.get(0) + _flags.get(0));
                    url.flags.add(url.flags.get(1) + _flags.get(1));

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

                for (int pos :
                        positions) {              //starts from 1 as 0 is the phrase ==> we may remove it
                    url.positions.add(Pair.of(pos, query.size() - 1));
                }

            }
        }


        int ifFound = 0;

        paragraphGetter pGet = null;
        pGet = new paragraphGetter();
        pGet.collectionsList = rankedPages;
        pGet.mongoDB = mongoDB;
        pGet.phrase = query;
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
        System.out.println(rankedPages.size());

        rankedPages.sort(urlPriority);

        return rankedPages;
    }

}