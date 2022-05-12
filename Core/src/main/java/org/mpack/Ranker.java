package org.mpack;

import org.bson.Document;
import org.springframework.data.util.Pair;


import java.lang.reflect.Array;
import java.util.*;
//import javafx.util.Pair;


//word1 --> doc1  doc2  doc3  doc4
//word2 --> doc2  doc4  doc6  doc8
//word3 --> doc4  doc6  doc9
//so on

//IDF --> per word

//TF --> per doc





public class Ranker {
    final MongodbIndexer mongoDB = new MongodbIndexer();


    Comparator<Pair<String, collections>> urlPriority = (url2, url1) -> {
return url1.getSecond().compare(url2.getSecond());

    };



    public PriorityQueue<Pair<String, collections>>  ranker2(String phrase, List<Document> retDoc) {

        PriorityQueue<Pair<String, collections>> rankedPages = new PriorityQueue<Pair<String, collections>>(urlPriority);
        //                       url         paragraph   header          flags         pagerank      priority   tokenCount   positions


        HashMap<String, collections> url_priority = new HashMap<>();
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
                List<Integer> positions = new  ArrayList<>();
                positions = (ArrayList<Integer>) (d.get("Positions"));

                pagRank = Double.parseDouble(d.get("pageRank").toString());
                priority = TF * IDF;


                //search in the hashmap for this url or insert it if not found
                if (url_priority.containsKey(d.getString("URL"))) {
                    //then update the priority
                    collections url = url_priority.get(d.getString("URL"));
                    url.pagerank = pagRank;
                    url.positions.add(positions);
                    double prePriority = url_priority.get(d.getString("URL")).priority;
                    int preTokenCount = url_priority.get(d.getString("URL")).token_count;
                    //then update the priority
                    url.token_count =  preTokenCount + 1;
                    url.priority = prePriority + priority;
                    url_priority.put(d.getString("URL"), url);
                } else {
                    collections url = new collections();
                    url.flags = _flags;
                    url.priority = priority;
                    url.positions = new ArrayList<>();
                    url.positions.add(positions);
                    url.token_count = 1;
                    url.url = d.getString("URL");
                    url_priority.put(d.getString("URL"), url);
                }
            }
        }


        int ifFound = 0;
        for (Map.Entry<String, collections> entry : url_priority.entrySet()) {
            Pair<String, String> paragraphTitle = getParagraph(entry.getKey(), query, entry.getValue()).getSecond();
            entry.getValue().title = paragraphTitle.getFirst();
            entry.getValue().paragraph = paragraphTitle.getSecond();
            rankedPages.add(Pair.of(entry.getKey(), entry.getValue()));
        }



        return rankedPages;
    }





    //phrase is array of query words without stop words, the whole phrase is at index 0.
    Pair<Integer, Pair<String, String>> getParagraph(String url, ArrayList<String> phrase, collections collection) {
        ArrayList<String> text = mongoDB.getTextUrl(url);
        boolean found = false;
        int index = -1;
        int i = -1, j;
        int start, end;
        StringBuilder parag = new StringBuilder();

        collection.wordNear = 0;


        for (j = 0; j < phrase.size(); j++) {
            for (i = 0; i < collection.positions.get(j).size(); i++) {

                start = Math.max(0, collection.positions.get(j).get(i) - 10);
                end = Math.min(text.size() - 1, collection.positions.get(j).get(i) + 10);

                for (int k = start; k < end; k++) {
                    parag.append(text.get(k + 1) + " ");
                }
                return Pair.of(0, Pair.of(text.get(0), parag.toString()));
            }
        }
        return Pair.of(-1, Pair.of("", ""));
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
