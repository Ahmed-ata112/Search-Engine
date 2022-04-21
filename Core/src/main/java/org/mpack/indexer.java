package org.mpack;
import ca.rmen.porterstemmer.PorterStemmer;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.*;

import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;

public class indexer {

    static final MongoDB_indexer mongoDB = new MongoDB_indexer();

    HashMap<String, HashMap<String, wordInfo>>  invertedFile;
    HashMap<String, Set<String>> equivalentStems = new HashMap<String, Set<String>>();
    long documentsCount;

    public static void main(String[] arg) throws FileNotFoundException {

        //MongoDB_indexer mongoDB = new MongoDB_indexer();
        indexer obj = new indexer();
        obj.documentsCount = mongoDB.getDocCount();
        //get crawled docs

        HashMap<String, String> htmlDocs = mongoDB.getHTML();
        ArrayList<HashMap<String, Integer>> docFlags = new ArrayList<>(2);
        for(int i = 0; i < 2; i++)
            docFlags.add(i, new HashMap<String, Integer>());
//               url     text
        //parse the HTML
        ArrayList<String> title = new ArrayList<String>(), header = new ArrayList<String>();
        List<String> stopWords = obj.constructStopWords();

        for(Map.Entry<String, String> set : htmlDocs.entrySet())
        {
            docFlags = new ArrayList<>(2);
            for(int i = 0; i < 2; i++)
                docFlags.add(i, new HashMap<String, Integer>());
            title = new ArrayList<String>();
            header = new ArrayList<String>();


            String parsedHTML = obj.ParseHTML(set.getValue(), title, header);
            obj.extractFlags(docFlags, title, header);
            List<String> tokens = obj.ExtractWords(parsedHTML);
            obj.removeStopWords(tokens, stopWords);
            obj.stemWord(tokens);
            mongoDB.StoreStemming(obj.equivalentStems);
            obj.invertedFile(set.getKey(), tokens, docFlags);

        }


        mongoDB.insertInvertedFile(obj.invertedFile, obj.documentsCount);


    }

    public indexer()
    {
        invertedFile = new HashMap<String, HashMap<String, wordInfo>>();
                                // id     documents  id       fields & values <TF, POSITION, FLAG>
    }

    //read the stop words
    private  @NotNull List<String> constructStopWords() throws FileNotFoundException{
        //read the file contains stop words
        File file = new File(".\\attaches\\stopwords.txt");
        Scanner scan = new Scanner(file);

        List<String> stopWords = new ArrayList<String>();
        while(scan.hasNextLine())
        {
            //append it to the list
            stopWords.add(scan.nextLine());
        }

        return stopWords;
    }

    String ParseHTML(String HTMLText, ArrayList<String> title, ArrayList<String> header)
    {
        org.jsoup.nodes.Document parsed;
        parsed = Jsoup.parse(HTMLText);
        title.addAll(parsed.getElementsByTag("title").eachText());
        header.addAll(parsed.getElementsByTag("header").eachText());

        return parsed.text();
    }

    List<String> ExtractWords(@NotNull String text)
    {
        List<String> WordList = new ArrayList<String>();
        StringBuffer word = new StringBuffer();
        char c;
        for(int i = 0; i < text.length(); i++)
        {
            c = text.charAt(i);
            if(c <=  'z' && c  >= 'a' || c <=  'Z' && c  >= 'A' || c <=  '9' && c  >= '0')
                word.append(c);
            else {
                if(word.isEmpty()) continue;
                if(NumberUtils.isNumber(word.toString()) || word.length() < 3);
                else
                    WordList.add(word.toString().toLowerCase(Locale.ROOT));
                word = new StringBuffer();
            }
        }
        return WordList;
    }


    //remove them
    public void removeStopWords(@NotNull List<String> tokens, List<String> stopWords)
    {
        for(int i = 0; i < tokens.size(); i++)
        {

            if(stopWords.contains(tokens.get(i).toLowerCase(Locale.ROOT)))
            {
                //then remove it
                tokens.remove(i);
                i--;
            }
        }
    }


    private void stemWord(@NotNull List<String> tokens)
    {
        PorterStemmer stem = new PorterStemmer();
        for(int i = 0; i < tokens.size(); i++)
        {
            String result = stem.stemWord(tokens.get(i));
            if(equivalentStems.containsKey(result))
            {

                equivalentStems.get(result).add(tokens.get(i));
            }
            else
            {
                equivalentStems.put(result,new HashSet<String>());
                equivalentStems.get(result).add(tokens.get(i));
            }
        }
    }


    private void invertedFile(String DocURL, List<String> tokens, ArrayList<HashMap<String, Integer>> docFlags)
    {
        for(int i = 0; i < tokens.size(); i++)
        {

            if(invertedFile.containsKey(tokens.get(i)))
            {
                //then go and update the positions in for this word in this doc
                //but first check if the doc exists or not
                if(invertedFile.get(tokens.get(i)).containsKey(DocURL))
                {
                    //then update
                    invertedFile.get(tokens.get(i)).get(DocURL).addPosition(i);
                    invertedFile.get(tokens.get(i)).get(DocURL).incTF();
                }
                else{
                    //then create it
                    wordInfo container = new wordInfo();
                    container.addPosition(i);
                    container.incTF();

                    for(short k = 0; k < docFlags.size(); k++)
                    {
                        if(docFlags.get(k).containsKey(tokens.get(i)))
                        {
                            container.setFlags(k, docFlags.get(k).get(tokens.get(i)));
                        }
                    }
                    invertedFile.get(tokens.get(i)).put(DocURL, container);
                }

            }
            else
            {
                HashMap<String, wordInfo> docMap = new HashMap<String, wordInfo>();
                wordInfo container = new wordInfo();
                container.addPosition(i);
                container.incTF();
                docMap.put(DocURL, container);
                invertedFile.put(tokens.get(i), docMap);
            }

        }

    }

    private void extractFlags(ArrayList<HashMap<String, Integer>> docFlags, ArrayList<String> title, ArrayList<String> header)
    {
        List<String> temp;
        int k = 0;
        for(int i = 0; i < title.size(); i++)
        {
            temp = ExtractWords(title.get(i));
            for(int j = 0; j < temp.size(); j++)
            {
                k = 0;
                if(docFlags.get(0).containsKey(temp.get(j))) {
                    k = docFlags.get(0).get(temp.get(j));
                }
                k++;
                docFlags.get(0).put(temp.get(j), k);

            }
        }
        for(int i = 0; i < header.size(); i++)
        {
            temp = ExtractWords(header.get(i));
            for(int j = 0; j < temp.size(); j++)
            {
                k = 0;
                if(docFlags.get(1).containsKey(temp.get(j))) {
                    k = docFlags.get(1).get(temp.get(j));
                }
                k++;
                docFlags.get(1).put(temp.get(j), k);

            }
        }
    }

}

