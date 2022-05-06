package org.mpack;

import org.bson.Document;
import org.jsoup.Jsoup;
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
    final MongodbIndexer mongoDB = new MongodbIndexer();
    Comparator<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Double>>>> urlPriority = (url1, url2) -> {
        //title
        if (url1.getSecond().getFirst().get(0) > url2.getSecond().getFirst().get(0))
            return 1;
            //header
        else if (url1.getSecond().getFirst().get(1) > url2.getSecond().getFirst().get(1))
            return 1;
            //priority  IDF-TF
        else if (url1.getSecond().getSecond().getSecond() > url2.getSecond().getSecond().getSecond())
            return 1;

        else if (url1.getSecond().getSecond().getSecond() < url2.getSecond().getSecond().getSecond())
            return -1;

            //pageRank
        else if (url1.getSecond().getSecond().getFirst() > url2.getSecond().getSecond().getFirst())
            return 1;

        else if (url1.getSecond().getSecond().getFirst() < url2.getSecond().getSecond().getFirst())
            return -1;

        else
            return 0;
    };


    public PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Double>>>> ranker(String phrase, HashMap<Integer, ArrayList<Document>> retDoc,
                                                                                                                     PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Double>>>> stemmedPages) {
        PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Double>>>> rankedPages = new PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Double>>>>(urlPriority);
        //                  url           flags           pagerank  priority
        stemmedPages = new PriorityQueue<>(urlPriority);

        HashMap<String, Pair<List<Integer>, Pair<Double, Double>>> url_priority = new HashMap<>();
        HashMap<String, Pair<List<Integer>, Pair<Double, Double>>> url_priority_stem = new HashMap<>();

        ArrayList<String> query = new ArrayList<>();
        ArrayList<String> stemmed = new ArrayList<>();

        query.add(phrase);


        double IDF = Double.valueOf(0);
        double TF = Double.valueOf(0);
        double priority = Double.valueOf(0);
        double pagRank;
        for (int i = 0; i < 2; i++) {
            //actual words
            if (i == 0) {
                for (int j = 0; j < retDoc.get(i).size(); j++) {
                    query.add(retDoc.get(i).get(j).get("token_name").toString());
                    //mongoDB.CalcTF_IDF(retDoc.get(i).get(j), url_priority);
                    IDF = Double.parseDouble(retDoc.get(i).get(j).get("IDF").toString());
                    List<Document> webPages = (List<Document>) retDoc.get(i).get(j).get("documents");
                    //I think there is a more efficient way to get the url of the word rather than this
                    for (Document d : webPages) {
                        List<Integer> _flags = new ArrayList<>();
                        /*_flags.set(0, 0);
                        _flags.set(1, 0);*/
                        TF = Double.parseDouble(d.get("normalizedTF").toString());  // to make sure -48 ?
                        _flags = (ArrayList<Integer>) (d.get("Flags"));

                        /*if(flags[0] - 48 > 0) // to convert from char to int --> to make sure ??????
                            _flags.set(0, flags[0] - 48);

                        if(flags[1] - 48 > 0)
                            _flags.set(1, flags[1] - 48);
*/
                        pagRank = Double.parseDouble(d.get("pageRank").toString());
                        priority = TF * IDF;
                        //search in the hashmap for this url or insert it if not found
                        if (url_priority.containsKey(d.getString("URL"))) {
                            //then update the priority
                            double prePriority = url_priority.get(d.getString("URL")).getSecond().getSecond();
                            //then update the priority
                            url_priority.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, prePriority + priority)));
                        } else {
                            url_priority.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, priority)));
                        }
                    }
                }
            }
            //stemming words
            else {
                for (int j = 0; j < retDoc.get(i).size(); j++) {
                    stemmed.add(retDoc.get(i).get(j).get("token_name").toString());
                    //mongoDB.CalcTF_IDF(retDoc.get(i).get(j), url_priority);
                    IDF = Double.parseDouble(retDoc.get(i).get(j).get("IDF").toString());
                    List<Document> webPages = (List<Document>) retDoc.get(i).get(j).get("documents");
                    //I think there is a more efficient way to get the url of the word rather than this
                    for (Document d : webPages) {
                        List<Integer> _flags;

                        TF = Double.parseDouble(d.get("normalizedTF").toString());  // to make sure -48 ?
                        _flags = (ArrayList<Integer>) (d.get("Flags"));

                        pagRank = Double.parseDouble(d.get("pageRank").toString());
                        priority = TF * IDF;
                        //search in the hashmap for this url or insert it if not found
                        if (!url_priority.containsKey(d.getString("URL"))) {
                            if (url_priority_stem.containsKey(d.getString("URL"))) {
                                //then update the priority
                                double prePriority = url_priority_stem.get(d.getString("URL")).getSecond().getSecond();
                                //then update the priority
                                url_priority_stem.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, prePriority + priority)));
                            } else {
                                url_priority_stem.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, priority)));
                            }
                        }
                    }
                }

            }
        }
        for (Map.Entry<String, Pair<List<Integer>, Pair<Double, Double>>> entry : url_priority.entrySet()) {
            Pair<String, String> paragraphTitle = getParagraph(entry.getKey(), query, phrase.isEmpty()).getSecond();
            rankedPages.add(Pair.of(Pair.of(entry.getKey(), paragraphTitle), Pair.of(entry.getValue().getFirst(), entry.getValue().getSecond())));
        }

        for (Map.Entry<String, Pair<List<Integer>, Pair<Double, Double>>> entry : url_priority_stem.entrySet()) {
            Pair<String, String> paragraphTitle = getParagraph(entry.getKey(), stemmed, false).getSecond();
            stemmedPages.add(Pair.of(Pair.of(entry.getKey(), paragraphTitle), Pair.of(entry.getValue().getFirst(), entry.getValue().getSecond())));
        }
        return rankedPages;
    }


    //phrase is array of query words without stop words, the whole phrase is at index 0.
    Pair<Integer, Pair<String, String>> getParagraph(String url, ArrayList<String> phrase, boolean ps) {
        ArrayList<String> text = mongoDB.getTextUrl(url);
        boolean found = false;
        int i = -1, j;
        for (j = 2; j < text.size(); j++) {
            for (i = 0; i < phrase.size(); i++) {
                found = text.contains(phrase.get(i));
                if (found)
                    return Pair.of(i, Pair.of(text.get(0), text.get(j)));
            }
        }
        //not found --> return description
        return Pair.of(-1, Pair.of(text.get(0), text.get(1)));
    }

    // --> the whole phrase is at index 0.
    //phrase array is sorted according to importance of the word.
/*    Pair<ArrayList<String>, String> getParagraph(String url, ArrayList<String> phrase, boolean ps)
    {
       String text = mongoDB.getTextUrl(url);
       ArrayList<ArrayList<Integer>> indecies = new ArrayList<ArrayList<Integer>>();
       int index;
    *//*   if(ps) {
           ArrayList<Integer> list = new ArrayList<>();
           list.add(0, text.indexOf(phrase.get(0)));
           indecies.add(0, list);
           return new String(text.substring(indecies.get(0).get(0) - 50, indecies.get(0).get(0) + 50)); //TODO: change limits
       }
       else
       {*//*
        for (String s : phrase) {
            index = 0;
            ArrayList<Integer> list = new ArrayList<>();
            do {
                index = text.indexOf(s, index);
                list.add(index);
            } while (index != -1);
            indecies.add(list);
        }
       *//*}*//*

        for(int i = 0; i < indecies.size(); i++)
        {

        }
       return text;
    }*/



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
