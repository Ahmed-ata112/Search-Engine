package org.mpack;

import org.bson.Document;
import org.springframework.data.util.Pair;


import java.util.*;
import java.util.function.Consumer;
//import javafx.util.Pair;



//word1 --> doc1  doc2  doc3  doc4
//word2 --> doc2  doc4  doc6  doc8
//word3 --> doc4  doc6  doc9
//so on

//IDF --> per word

//TF --> per doc


public class Ranker {
    static final MongodbIndexer mongoDB = new MongodbIndexer();
    Comparator<Pair<String, Pair<List<Integer>, Pair<Double,Double>>>> urlPriority = new Comparator<Pair<String,Pair<List<Integer>, Pair<Double,Double>>>>() {
        public int compare(Pair<String, Pair<List<Integer>, Pair<Double,Double>>> url1, Pair<String, Pair<List<Integer>, Pair<Double,Double>>> url2) {
            //title
            if(url1.getSecond().getFirst().get(0) > url2.getSecond().getFirst().get(0))
                return 1;
            //header
            else if(url1.getSecond().getFirst().get(1) > url2.getSecond().getFirst().get(1))
                return 1;
            //priority  IDF-TF
            else if (url1.getSecond().getSecond().getSecond() > url2.getSecond().getSecond().getSecond())
                return 1;

            else if (url1.getSecond().getSecond().getSecond() < url2.getSecond().getSecond().getSecond())
                return -1;

            //pageRank
            else if (url1.getSecond().getSecond().getFirst() > url2.getSecond().getSecond().getFirst())
                return 1;

            else if (url1.getSecond().getSecond().getFirst() > url2.getSecond().getSecond().getFirst())
                return -1;

            else
                return 0;
        }
    };


    PriorityQueue<Pair<String, Pair<List<Integer>,Pair<Double,Double>>>> ranker(HashMap<Integer, ArrayList<Document>> retDoc,
                                                  PriorityQueue<Pair<String, Pair<List<Integer>,Pair<Double,Double>>>> stemmedPages)
    {
        PriorityQueue<Pair<String, Pair<List<Integer>,Pair<Double,Double>>>> rankedPages = new PriorityQueue<Pair<String, Pair<List<Integer>, Pair<Double,Double>>>>(urlPriority);
        //                  url           flags           pagerank  priority
        stemmedPages = new PriorityQueue<Pair<String, Pair<List<Integer>, Pair<Double,Double>>>>(urlPriority);

        HashMap<String, Pair<List<Integer>, Pair<Double,Double>>> url_priority = new HashMap<>();
        HashMap<String, Pair<List<Integer>, Pair<Double,Double>>> url_priority_stem = new HashMap<>();


        double IDF = Double.valueOf(0);
        double TF = Double.valueOf(0);
        double priority = Double.valueOf(0);
        double pagRank;
        for(int i = 0; i < 2; i++)
        {
            //actual words
            if(i == 0)
            {
                for(int j = 0; j < retDoc.get(i).size(); j++)
                {

                    //mongoDB.CalcTF_IDF(retDoc.get(i).get(j), url_priority);
                    IDF = Double.parseDouble(retDoc.get(i).get(j).get("IDF").toString());
                    List<Document> webPages = (List<Document>) retDoc.get(i).get(j).get("documents");
                    //I think there is a more efficient way to get the url of the word rather than this
                    for (Document d: webPages){
                        List<Integer> _flags = new ArrayList<>(2);
                        _flags.set(0, 0);
                        _flags.set(1, 0);
                        TF = Double.parseDouble(d.get("TF").toString());  // to make sure -48 ?
                        char[] flags  = d.get("Flags").toString().toCharArray();
                        if(flags[0] - 48 > 0) // to convert from char to int --> to make sure ??????
                            _flags.set(0, flags[0] - 48);

                        if(flags[1] - 48 > 0)
                            _flags.set(1, flags[1] - 48);

                        pagRank = Double.parseDouble(d.get("pageRank").toString());
                        priority = TF * IDF;
                        //search in the hashmap for this url or insert it if not found
                        if (url_priority.containsKey(d.getString("URL"))) {
                            //then update the priority
                            url_priority.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, url_priority.get(d.getString("URL")).getSecond().getSecond() + priority)) );
                        } else {
                            url_priority.put(d.getString("URL"), Pair.of(_flags,Pair.of(pagRank, priority)));
                        }
                    }
                }
            }
            //stemming words
            else
            {
                for(int j = 0; j < retDoc.get(i).size(); j++)
                {
                    //mongoDB.CalcTF_IDF(retDoc.get(i).get(j), url_priority);
                    IDF = Double.parseDouble(retDoc.get(i).get(j).get("IDF").toString());
                    List<Document> webPages = (List<Document>) retDoc.get(i).get(j).get("documents");
                    //I think there is a more efficient way to get the url of the word rather than this
                    for (Document d: webPages){
                        List<Integer> _flags = new ArrayList<>(2);
                        _flags.set(0, 0);
                        _flags.set(1, 0);
                        TF = Double.parseDouble(d.get("TF").toString());  // to make sure -48 ?
                        char[] flags  = d.get("Flags").toString().toCharArray();
                        if(flags[0] - 48 > 0) // to convert from char to int --> to make sure ??????
                            _flags.set(0, flags[0] - 48);

                        if(flags[1] - 48 > 0)
                            _flags.set(1, flags[1] - 48);

                        pagRank = Double.parseDouble(d.get("pageRank").toString());
                        priority = TF * IDF;
                        //search in the hashmap for this url or insert it if not found
                        if (!url_priority.containsKey(d.getString("URL"))) {
                            if (url_priority_stem.containsKey(d.getString("URL"))) {
                                //then update the priority
                                url_priority_stem.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, url_priority.get(d.getString("URL")).getSecond().getSecond() + priority)) );
                            } else {
                                url_priority_stem.put(d.getString("URL"), Pair.of(_flags,Pair.of(pagRank, priority)));
                            }
                        }
                    }
                }

            }
        }
        for(Map.Entry<String, Pair<List<Integer>,Pair<Double,Double>>> entry : url_priority.entrySet()) {
            rankedPages.add(Pair.of(entry.getKey(), Pair.of(entry.getValue().getFirst(), entry.getValue().getSecond())));
        }

        for(Map.Entry<String, Pair<List<Integer>,Pair<Double,Double>>> entry : url_priority_stem.entrySet()) {
            stemmedPages.add(Pair.of(entry.getKey(), Pair.of(entry.getValue().getFirst(), entry.getValue().getSecond())));
        }
        return rankedPages;
    }







/*
    PriorityQueue<Pair<String, Double>> ranker(HashMap<Integer, ArrayList<String>> retDoc)
    {
        PriorityQueue<Pair<String, Double>> rankedPages = new PriorityQueue<>(urlPriority);
        HashMap<String, Double> url_priority = new HashMap<>();
        for(int i = 0; i < 2; i++)
        {
            for(int j = 0; j < retDoc.get(i).size(); j++)
            {
                mongoDB.CalcTF_IDF(retDoc.get(i).get(j), url_priority);
            }
        }

        for(Map.Entry<String, Double> entry : url_priority.entrySet()) {
            rankedPages.add(Pair.of(entry.getKey(), entry.getValue()));
        }
        return rankedPages;
    }
*/

/*

    PriorityQueue<Pair<String, Float>> ranker(HashMap<Integer, ArrayList<Pair<String, ArrayList<String>>>> retDoc)
    {
        PriorityQueue<Pair<String, Float>> rankedPages = new PriorityQueue<>(urlPriority);
        HashMap<String, Float> url_priority = new HashMap<>();
        for(int i = 0; i < 2; i++)
        {
            for(int j = 0; j < retDoc.get(i).size(); j++)
            {
                mongoDB.CalcTF_IDF(retDoc.get(i).get(j).getFirst(), url_priority);
            }
        }

        for(Map.Entry<String, Float> entry : url_priority.entrySet()) {
            rankedPages.add(Pair.of(entry.getKey(), entry.getValue()));
        }
        return rankedPages;
    }*/














/*
    PriorityQueue<Pair<String, Float>> ranker2(List<String> query, List<String> urls)
    {
        PriorityQueue<Pair<String, Float>> rankedPages = new PriorityQueue<>(urlPriority);
                        // url     priority
        float priority;
        for(int i = 0; i < urls.size(); i++)
        {
            priority = 0;
            for(int j = 0; j < query.size(); j++)
            {
                //get the TF*IDF of this word in this url and add them to the priority
                priority += mongoDB.getTF_IDF(query.get(j), urls.get(j));

                //todo:  get the flags of this word in this url and the positions --> in the previous call


            }
            //make the pair of the url and the priority
            //Pair<String, Float> entry = new Pair<>(urls.get(i), priority);  //gives an error

            //now push enqueue this url in the queue
            rankedPages.add(Pair.of(urls.get(i), priority));

        }
        return rankedPages;
    }
*/

}
