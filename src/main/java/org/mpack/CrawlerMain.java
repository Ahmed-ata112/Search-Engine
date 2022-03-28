package org.mpack;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


class GlobalVarsWrapper {
    final HashSet<String> visitedLinks;  //Links That were already crawled -- So That You don't put one twice
    // a way to define blocked sites (Robot.txt) is just to put it in the links set without crawling it
    Integer count;

    public GlobalVarsWrapper() {
        visitedLinks = new HashSet<>();
        count = 0;
    }
}

class Crawler implements Runnable {
    // Global Data can be Static too
    GlobalVarsWrapper myWrapper;
    ArrayList<String> initialStrings;
    static String connectionString = "mongodb://localhost:27017";
    static MongoCollection<org.bson.Document> UrlsCollection;
    int neededThreads = 0;
    static MongoCollection<org.bson.Document> myDb;

    public Crawler(List<String> initialStrings, GlobalVarsWrapper varsWrapper, int neededThreads) {
        this.myWrapper = varsWrapper;
        this.initialStrings = (ArrayList<String>) initialStrings;
        this.neededThreads = neededThreads;

    }

    public Crawler(List<String> initialStrings, GlobalVarsWrapper varsWrapper) {
        this(initialStrings, varsWrapper, 0);

    }

    public static void setUrlsCollection(MongoCollection<org.bson.Document> crawledURLS) {
        UrlsCollection = crawledURLS;
    }

    @Override
    public void run() {
        saveState();
        crawl(initialStrings);
    }

    private void saveState() {

    }

    public void getPageLinks(String URL) {
        if (myWrapper.count > 1000) {
            return;
        }
        // 4. Check if you have already crawled the URLs
        // (we are intentionally not checking for duplicate content in this example)
        synchronized (myWrapper.visitedLinks) {
            if (myWrapper.visitedLinks.contains(URL))
                return;
            else if (myWrapper.visitedLinks.add(URL)) {
                System.out.println(myWrapper.count + " " + URL);
                myWrapper.count++;
            }
        }
        try {
            // 4. (i) If not add it to the index
            // 2. Fetch the HTML code
            Document document = Jsoup.connect(URL).get();
            // 3. Parse the HTML to extract links to other URLs
            Elements linksOnPage = document.select("a[href]");
            // 5. For each extracted URL... go back to Step 4.
            for (Element page : linksOnPage) {
                getPageLinks(page.attr("abs:href"));
            }
        } catch (Exception e) {
            // System.err.println("For '" + URL + "': " + e.getMessage());
            System.out.println(e.toString());
        }

    }

    Deque<String> unprocessedUrlsStack = new ArrayDeque<>();

    public void crawl(List<String> seedUrls) {

        unprocessedUrlsStack.addAll(seedUrls);

        while (!unprocessedUrlsStack.isEmpty()) {
            if (myWrapper.count > 1000) {
                return;
            }
            String url;
            //To make sure The Queue is large enough not to make it idle here
            if (neededThreads > 0 && unprocessedUrlsStack.size() > 5000) {
                url = unprocessedUrlsStack.pop();
                ArrayList<String> toSend = new ArrayList<>();
                toSend.add(url);
                new Thread(new Crawler(toSend, myWrapper)).start();
                neededThreads--;
            }
            url = unprocessedUrlsStack.pop();

            synchronized (myWrapper.visitedLinks) {
                if (myWrapper.visitedLinks.contains(url))
                    continue;
                else {
                    myWrapper.visitedLinks.add(url);
                    myWrapper.count++;
                }
            }
            try {
                Document document = Jsoup.connect(url).get();
                Elements linksOnPage = document.select("a[href]");
                for (Element page : linksOnPage) {
                    unprocessedUrlsStack.add(page.attr("abs:href"));
                }
                org.bson.Document urlEntry = new org.bson.Document("_id", new ObjectId());
                urlEntry.append("url_link", url)
                        .append("html_body", document.html());
                UrlsCollection.insertOne(urlEntry);
                //now we really processed a link

            } catch (Exception e) {
                System.err.println("For '" + url + "': " + e.getMessage());
                System.out.println(e.toString());
                synchronized (myWrapper.count){
                myWrapper.count--;
                }
            }

        }
    }
}


public class CrawlerMain {

    static GlobalVarsWrapper wr = new GlobalVarsWrapper();
    static String connectionString = "mongodb://localhost:27017";
    private static MongoClient mongoClient;
    static MongoDatabase searchEngineDb;

    //connect with the DB
    public static void initConnection() {
        try {
            //SearchEngine
            //.
            //CrawledURLS
            mongoClient = MongoClients.create(connectionString);
            searchEngineDb = mongoClient.getDatabase("SearchEngine");
            Crawler.setUrlsCollection(searchEngineDb.getCollection("CrawledURLS"));


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    public static void main(String[] args) {

        //initialize Connection with The Database
        initConnection();

        int numThreads = 10;
        System.out.printf("Number of Threads is: %d%n", numThreads);
        try {
            File file = new File(".\\attaches\\seed.txt");    //creates a new file instance
            FileReader fr = new FileReader(file);   //reads the file
            ArrayList<String> seedsArray;
            try (BufferedReader br = new BufferedReader(fr)) {
                String line;
                seedsArray = new ArrayList<>();
                while ((line = br.readLine()) != null) {
                    //Read what in The seed
                    seedsArray.add(line);
                }
            }  //creates a buffering character input stream

            /*
             *
             * now All The seeds are in the array
             * Num of threads and The number of seeds are critical
             * N_Threads > Seeds? -> That's a Good case where we Can divide them evenly
             * N_Threads < Seeds? -> give each a one seed and The remaining get them seeds from te working Ones
             *
             * */
            int ratio = seedsArray.size() / numThreads; // how many seeds per a Thread
            ArrayList<String> ss;

            if (ratio > 0) {
                //there are enough seeds For The threads
                for (int i = 0; i < numThreads; i++) {
                    if (i != numThreads - 1) {
                        ss = new ArrayList<>(seedsArray.subList(i * ratio, (i + 1) * (ratio)));
                        System.out.println(ss);
                    } else {
                        //The last threads takes all the remaining
                        ss = new ArrayList<>(seedsArray.subList(i * ratio, seedsArray.size()));
                    }
                    new Thread(new Crawler(ss, wr)).start();
                }


            } else {
                //not enough seeds for Threads
                // take one of the urls and get some from it
                int neededThreads = numThreads;

                for (int i = 0; i < seedsArray.size(); i++) {
                    if (i != seedsArray.size() - 1) {
                        // 0 1 2 3
                        ss = new ArrayList<>();
                        ss.add(seedsArray.get(i));
                        System.out.println(ss);
                        new Thread(new Crawler(ss, wr)).start();
                        neededThreads--;
                    } else {
                        //The last threads takes all the remaining
                        ss = new ArrayList<>();
                        ss.add(seedsArray.get(i));
                        new Thread(new Crawler(ss, wr, neededThreads)).start();

                        System.out.println(ss);
                    }
                }

            }

            fr.close();    //closes the stream and release the resources
            //System.out.println(sb.toString());   //returns a string that textually represents the object
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}