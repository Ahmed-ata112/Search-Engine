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
    Comparator<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Pair<Integer, Integer>>>>>> urlPriority = (url2, url1) -> {

        //positions --> the whole search query with the same order in sequence
        if(url1.getSecond().getSecond().getSecond().getSecond().getSecond() > url2.getSecond().getSecond().getSecond().getSecond().getSecond())
            return 1;
        else if(url1.getSecond().getSecond().getSecond().getSecond().getSecond() < url2.getSecond().getSecond().getSecond().getSecond().getSecond())
            return -1;

        //tokenCount
        if(url1.getSecond().getSecond().getSecond().getSecond().getFirst() > url2.getSecond().getSecond().getSecond().getSecond().getFirst())
            return 1;
        else if (url1.getSecond().getSecond().getSecond().getSecond().getFirst() < url2.getSecond().getSecond().getSecond().getSecond().getFirst())
            return -1;

            //title
        else if (url1.getSecond().getFirst().get(0) > url2.getSecond().getFirst().get(0))
            return 1;
        else if (url1.getSecond().getFirst().get(0) < url2.getSecond().getFirst().get(0))
            return -1;

            //header
        else if (url1.getSecond().getFirst().get(1) > url2.getSecond().getFirst().get(1))
            return 1;
        else if (url1.getSecond().getFirst().get(1) < url2.getSecond().getFirst().get(1))
            return -1;

            //priority  IDF-TF
        else if (url1.getSecond().getSecond().getSecond().getFirst() > url2.getSecond().getSecond().getSecond().getFirst())
            return 1;
        else if (url1.getSecond().getSecond().getSecond().getFirst() < url2.getSecond().getSecond().getSecond().getFirst())
            return -1;

            //pageRank
        else if (url1.getSecond().getSecond().getFirst() > url2.getSecond().getSecond().getFirst())
            return 1;

        else if (url1.getSecond().getSecond().getFirst() < url2.getSecond().getSecond().getFirst())
            return -1;

        else
            return 0;
    };







    public PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Pair<Integer, Integer>>>>>>   ranker(String phrase, ArrayList<Document> retDoc) {

        PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Pair<Integer, Integer>>>>>> rankedPages = new PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Pair<Integer, Integer>>>>>>(urlPriority);
        //                       url         paragraph   header          flags         pagerank      priority   tokenCount   positions


        HashMap<String, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>> url_priority = new HashMap<>();
        //         url          flags            pagerank     priority  tokenCount

        ArrayList<String> query = new ArrayList<>();
        ArrayList<String> stemmed = new ArrayList<>();

        query.add(phrase);


        double IDF = 0;
        double TF = 0;
        double priority = 0;
        double pagRank;


        for (int i = 0; i < retDoc.size(); i++) {
            query.add(retDoc.get(i).get("token_name").toString());
            IDF = Double.parseDouble(retDoc.get(i).get("IDF").toString());
            List<Document> webPages = (List<Document>) retDoc.get(i).get("documents");
            //I think there is a more efficient way to get the url of the word rather than this
            for (Document d : webPages) {
                List<Integer> _flags = new ArrayList<>();
                /*_flags.set(0, 0);
                _flags.set(1, 0);*/
                TF = Double.parseDouble(d.get("normalizedTF").toString());  // to make sure -48 ?
                _flags = (ArrayList<Integer>) (d.get("Flags"));

                pagRank = Double.parseDouble(d.get("pageRank").toString());
                priority = TF * IDF;
                //search in the hashmap for this url or insert it if not found
                if (url_priority.containsKey(d.getString("URL"))) {
                    //then update the priority
                    double prePriority = url_priority.get(d.getString("URL")).getSecond().getSecond().getFirst();
                    int preTokenCount = url_priority.get(d.getString("URL")).getSecond().getSecond().getSecond();
                    //then update the priority
                    url_priority.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, Pair.of(prePriority + priority, preTokenCount + 1))));
                } else {
                    url_priority.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, Pair.of(priority, 1))));
                }
            }
        }


        int ifFound = 0;
        for (Map.Entry<String, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>> entry : url_priority.entrySet()) {
            Pair<String, String> paragraphTitle = getParagraph(entry.getKey(), query, phrase.isEmpty()).getSecond();
            rankedPages.add(Pair.of(Pair.of(entry.getKey(), paragraphTitle), Pair.of(entry.getValue().getFirst(), Pair.of(entry.getValue().getSecond().getFirst(), Pair.of(entry.getValue().getSecond().getSecond().getFirst(),  Pair.of(entry.getValue().getSecond().getSecond().getSecond(), ifFound))))));
        }



        return rankedPages;
    }






   /* public Pair<PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>>>, PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>>>>   ranker(String phrase, HashMap<Integer, ArrayList<Document>> retDoc) {

        PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>>> rankedPages = new PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>>>(urlPriority);
        //                       url         paragraph   header          flags         pagerank      priority   tokenCount


        PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>>> stemmedPages = new PriorityQueue<>(urlPriority);

        HashMap<String, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>> url_priority = new HashMap<>();
        //         url          flags            pagerank     priority  tokenCount
        HashMap<String, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>> url_priority_stem = new HashMap<>();

        ArrayList<String> query = new ArrayList<>();
        ArrayList<String> stemmed = new ArrayList<>();

        query.add(phrase);


        double IDF = 0;
        double TF = 0;
        double priority = 0;
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
                        *//*_flags.set(0, 0);
                        _flags.set(1, 0);*//*
                        TF = Double.parseDouble(d.get("normalizedTF").toString());  // to make sure -48 ?
                        _flags = (ArrayList<Integer>) (d.get("Flags"));

                        *//*if(flags[0] - 48 > 0) // to convert from char to int --> to make sure ??????
                            _flags.set(0, flags[0] - 48);

                        if(flags[1] - 48 > 0)
                            _flags.set(1, flags[1] - 48);
*//*
                        pagRank = Double.parseDouble(d.get("pageRank").toString());
                        priority = TF * IDF;
                        //search in the hashmap for this url or insert it if not found
                        if (url_priority.containsKey(d.getString("URL"))) {
                            //then update the priority
                            double prePriority = url_priority.get(d.getString("URL")).getSecond().getSecond().getFirst();
                            int preTokenCount = url_priority.get(d.getString("URL")).getSecond().getSecond().getSecond();
                            //then update the priority
                            url_priority.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, Pair.of(prePriority + priority, preTokenCount + 1))));
                        } else {
                            url_priority.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, Pair.of(priority, 1))));
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
                                double prePriority = url_priority_stem.get(d.getString("URL")).getSecond().getSecond().getFirst();
                                int preTokenCount = url_priority_stem.get(d.getString("URL")).getSecond().getSecond().getSecond();
                                //then update the priority
                                url_priority_stem.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, Pair.of(prePriority + priority, preTokenCount + 1))));
                            } else {
                                url_priority_stem.put(d.getString("URL"), Pair.of(_flags, Pair.of(pagRank, Pair.of(priority, 1))));
                            }
                        }
                    }
                }

            }
        }
        for (Map.Entry<String, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>> entry : url_priority.entrySet()) {
            Pair<String, String> paragraphTitle = getParagraph(entry.getKey(), query, phrase.isEmpty()).getSecond();
            rankedPages.add(Pair.of(Pair.of(entry.getKey(), paragraphTitle), Pair.of(entry.getValue().getFirst(), Pair.of(entry.getValue().getSecond().getFirst(), Pair.of(entry.getValue().getSecond().getSecond().getFirst(),entry.getValue().getSecond().getSecond().getSecond())    ))));
        }

        for (Map.Entry<String, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>> entry : url_priority_stem.entrySet()) {
            Pair<String, String> paragraphTitle = getParagraph(entry.getKey(), stemmed, false).getSecond();
            stemmedPages.add(Pair.of(Pair.of(entry.getKey(), paragraphTitle), Pair.of(entry.getValue().getFirst(), Pair.of(entry.getValue().getSecond().getFirst(), Pair.of(entry.getValue().getSecond().getSecond().getFirst(),entry.getValue().getSecond().getSecond().getSecond()) ))));
        }

        return Pair.of(rankedPages, stemmedPages);
    }
*/

    //phrase is array of query words without stop words, the whole phrase is at index 0.
    Pair<Integer, Pair<String, String>> getParagraph(String url, ArrayList<String> phrase, boolean ps) {
        ArrayList<ArrayList<String>> text = mongoDB.getTextUrl(url);

        boolean found = false;
        int index = -1;
        int i = -1, j;

        if(ps)
        {
            for (j = 0; j < text.size(); j++) {
                for (i = 0; i < text.get(j).size(); i++) {
                    index = text.get(j).get(i).indexOf(phrase.get(0));
                    if(index != -1)
                    {
                        if(j == 2) found = true;
                    }
                    if (found) {
                        String send = text.get(j).get(i);
                        send = text(send, phrase.get(0), index);
                        return Pair.of(i, Pair.of(text.get(0).get(0), send));
                    }
                }
            }
            if((index == -1)) return Pair.of(-2, Pair.of("", "")); //url --> remove;
        }
        else {
            for (j = 1; j < text.get(2).size(); j++) {
                for (i = 0; i < phrase.size(); i++) {
                    if(phrase.get(i).isEmpty()) continue;
                    index = text.get(2).get(j).indexOf(phrase.get(i));
                    if(index != -1)
                    {
                        char b = ' ';
                        if(index != 0) b = text.get(2).get(j).charAt(index - 1);
                        char a = ' ';
                        if(text.get(2).get(j).length() != index + phrase.get(i).length()) a = text.get(2).get(j).charAt(index + phrase.get(i).length());

                        if(!((b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z') || (a >= 'a' && a <= 'z' || a >= 'A' && a <= 'Z'))) found = true;
                    }
                    if (found)
                    {
                        String send = text.get(2).get(j);
                        send = text(send, phrase.get(i), index);
                        return Pair.of(i, Pair.of(text.get(0).get(0), send));
                    }
                }
            }
        }
        //not found --> return description
        return Pair.of(-1, Pair.of(text.get(0).get(0), "text.get(2).get(0)"));
    }


    static String text(String paragragh, String word, int index)
    {
        StringBuilder text = new StringBuilder();
        int counter = 0, i = index - 1;
        char c, t = 'a';
        int maxA = 10, maxB = 10;
        if(index == 0)
        {
            maxA = 20;
            maxB = 0;
        }
        if(index + word.length() == paragragh.length())
        {
            maxA = 0;
            if(maxB == 10) maxB = 20;
        }
        while(i > -1 && counter < maxB)
        {
            c = paragragh.charAt(i);
            if(c <= 'z' && c >= 'a' || c <= 'Z' && c >= 'A' || c <= '9' && c >= '0');
            else if(t <= 'z' && t >= 'a' || t <= 'Z' && t >= 'A' || t <= '9' && t >= '0') counter++;
            i--;
            t = c;
        }
        i++;
        text.append(paragragh.substring(i, index + word.length()));

        counter = 0;

        i = index + word.length();
        t = 'a';

        while(i < paragragh.length()  && counter < maxA)

        {

            c = paragragh.charAt(i);
            if((c <= 'z' && c >= 'a' || c <= 'Z' && c >= 'A' || c <= '9' && c >= '0'));
            else if(t <= 'z' && t >= 'a' || t <= 'Z' && t >= 'A' || t <= '9' && t >= '0') counter++;
            i++;
            t = c;
        }

        text.append(paragragh.substring(index + word.length(), i));

        return text.toString();
    }

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


