package org.mpack;

import org.bson.Document;
import org.springframework.data.util.Pair;


import java.util.*;


class paragraphGetter implements Runnable
{
    ArrayList<String> phrase;
    ArrayList<collections> collectionsList;
    PriorityQueue<collections> rankedPages;

    int count;
    MongodbIndexer mongoDB;


    @Override
    public void run() {
        int id = Integer.parseInt(Thread.currentThread().getName());
        int start, end;
        collections current;

        start = (collectionsList.size() / count) * id;
        if(id == count - 1)
            end = collectionsList.size();
        else
            end = start + collectionsList.size() / count;
        for (int i = start; i < end; i++) {
            current = collectionsList.get(i);
            getParagraph(current);
            synchronized (this)
            {
                rankedPages.add(current);
            }
        }

    }

    void getParagraph(collections collection) {
        ArrayList<String> text = mongoDB.getTextUrl(collection.url);
        boolean found = false;
        int index = -1;
        int i = -1, j;
        int start, end;
        StringBuilder parag = new StringBuilder();

        collection.wordNear = 0;

        collection.title = text.get(0);

        collection.paragraph = "paragragh";
/*
        for (j = 0; j < phrase.size(); j++) {
            for (i = 0; i < collection.positions.get(j).size(); i++) {

                start = Math.max(0, collection.positions.get(j).get(i) - 10);
                end = Math.min(text.size() - 1, collection.positions.get(j).get(i) + 10);

                for (int k = start; k < end; k++) {
                    parag.append(text.get(k + 1) + " ");
                }
                collection.paragraph = parag.toString();
                return;
            }
        }*/
    }

}

public class Ranker {
    final MongodbIndexer mongoDB = new MongodbIndexer();


    Comparator<collections> urlPriority = (url2, url1) -> {
        return url1.compare(url2);
    };


    public PriorityQueue<collections> ranker2(String phrase, List<Document> retDoc) {

        PriorityQueue<collections> rankedPages = new PriorityQueue<>(urlPriority);
        //                       url         paragraph   header          flags         pagerank      priority   tokenCount   positions


        HashMap<String, Integer> urlPosition = new HashMap<>();
        ArrayList<collections> urlPriority = new ArrayList<>();
        //         url          flags            pagerank     priority  tokenCount

        ArrayList<String> query = new ArrayList<>();

        query.add(phrase);


        double IDF = 0;
        double TF = 0;
        double priority = 0;
        double pagRank;

        collections url;
        for (Document document : retDoc) {
            query.add(document.get("token_name").toString());
            IDF = Double.parseDouble(document.get("IDF").toString());
            List<Document> webPages = (List<Document>) document.get("documents");
            //I think there is a more efficient way to get the url of the word rather than this
            for (Document d : webPages) {
                List<Integer> _flags;
                TF = Double.parseDouble(d.get("normalizedTF").toString());  // to make sure -48 ?
                _flags = (ArrayList<Integer>) (d.get("Flags"));
                List<Integer> positions = new ArrayList<>();
                positions = (ArrayList<Integer>) (d.get("Positions"));

                pagRank = Double.parseDouble(d.get("pageRank").toString());
                priority = TF * IDF;



                //search in the hashmap for this url or insert it if not found
                if (urlPosition.containsKey(d.getString("URL"))) {
                    //then update the priority
                    url = urlPriority.get(urlPosition.get(d.getString("URL")));
                    url.pagerank = pagRank;

                    //then update the priority
                    url.token_count++;
                    url.priority += priority;


                } else {
                    url = new collections();
                    url.flags = _flags;
                    url.priority = priority;

                    url.positions = new ArrayList<>();

                    url.token_count = 1;
                    url.url = d.getString("URL");
                    urlPriority.add(url);
                    urlPosition.put(url.url, urlPriority.size() - 1);
                }


                //rufaida: make sure var in list is updated also
                for (int pos:
                        positions) {              //starts from 1 as 0 is the phrase ==> we may remove it
                    url.positions.add(Pair.of(pos, query.size() - 1));
                }
            }
        }


        int ifFound = 0;

        paragraphGetter pGet = null;
        pGet = new paragraphGetter();
        pGet.collectionsList = urlPriority;
        pGet.rankedPages = rankedPages;
        pGet.mongoDB = mongoDB;
        pGet.phrase = query;
        pGet.count = Math.max(1, urlPriority.size() / 50);

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


        return rankedPages;
    }

}
