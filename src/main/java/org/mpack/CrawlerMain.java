package org.mpack;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.Thread.sleep;


class Crawler implements Runnable {
    // Global Data can be Static too
    static final HashSet<String> visitedLinks = new HashSet<>();
    //Links That were already crawled -- So That You don't put one twice
    // a way to define blocked sites (Robot.txt) is just to put it in the links set without crawling it
    static int count = 0;
    static final Object cLock = new Object();
    ArrayList<String> initialStrings;
    static final int MAX_PAGES = 100;
    int neededThreads;
    static final MongoDB mongoDB = new MongoDB();

    public static void setIsReCrawling(boolean isReCrawling) {
        Crawler.isReCrawling = isReCrawling;
    }

    static boolean isReCrawling = false;
    static final List<String> reCrawlingList = List.of("https://www.cnn.com/", "https://abc.com/");

    public Crawler(List<String> initialStrings, int neededThreads) {
        this.initialStrings = (ArrayList<String>) initialStrings;
        this.neededThreads = neededThreads;
    }

    public Crawler(List<String> initialStrings) {
        this(initialStrings, 0);
    }

    @Override
    public void run() {
        if (isReCrawling)
            reCrawl(reCrawlingList);
        else
            crawl(initialStrings);
    }

    public static void setCount(int oldCount) {
        count = oldCount;
    }


    public void crawl(List<String> seedUrls) {

        Deque<String> unprocessedUrlsStack = new ArrayDeque<>(seedUrls);

        while (!unprocessedUrlsStack.isEmpty()) {
            if (count > MAX_PAGES) {
                //set state to 1 as we finished all the Pages
                finishState();
                return;
            }
            String url;
            //To make sure The Queue is large enough not to make it idle here
            if (neededThreads > 0 && unprocessedUrlsStack.size() > 10) {
                url = unprocessedUrlsStack.pop();
                ArrayList<String> toSend = new ArrayList<>();
                toSend.add(url);
                new Thread(new Crawler(toSend)).start();
                neededThreads--;
            }
            url = unprocessedUrlsStack.pop();
            //delete from the unprocessed array
            mongoDB.removeFromStateArray(url);

            synchronized (visitedLinks) {
                if (visitedLinks.contains(url))
                    continue;
                else {
                    visitedLinks.add(url);
                    count++;
                }
            }
            try {
                Document document = Jsoup.connect(url).get();
                Elements linksOnPage = document.select("a[href]");
                for (Element page : linksOnPage) {
                    String uu = page.attr("abs:href");

                    unprocessedUrlsStack.add(uu);
                    // add to the unprocessed array
                    mongoDB.addToStateArray(uu);

                }

                mongoDB.insertUrl(url, document.html());
                //now we really processed a link

            } catch (Exception e) {
                System.err.println("For '" + url + "': " + e.getMessage());
                System.out.println(e.getMessage());
                synchronized (cLock) {
                    //a bad Link is detected
                    count--;
                }
            }

        }
        // Out but with links less than Count
        //state should Remain 0
    }


    public void reCrawl(List<String> seedUrls) {

        Deque<String> unprocessedUrlsStack = new ArrayDeque<>(seedUrls);

        while (!unprocessedUrlsStack.isEmpty()) {

            String url;
            //To make sure The Queue is large enough not to make it idle here
            if (neededThreads > 0 && unprocessedUrlsStack.size() > 10) {
                url = unprocessedUrlsStack.pop();
                ArrayList<String> toSend = new ArrayList<>();
                toSend.add(url);
                new Thread(new Crawler(toSend)).start();
                neededThreads--;
            }
            url = unprocessedUrlsStack.pop();
            //delete from the unprocessed array
            synchronized (visitedLinks) {

                if (!reCrawlingList.contains(url) && visitedLinks.contains(url)) {
                    continue;
                }
            }
            try {
                Document document = Jsoup.connect(url).get();
                Elements linksOnPage = document.select("a[href]");

                for (Element page : linksOnPage) {
                    String uu = page.attr("abs:href");
                    unprocessedUrlsStack.add(uu);

                }
                synchronized (visitedLinks) {
                    if (!visitedLinks.contains(url)) {
                        mongoDB.insertUrl(url, document.html());
                        visitedLinks.add(url);
                    }//now we really processed a link
                }
            } catch (Exception e) {
                System.err.println("For '" + url + "': " + e.getMessage());
                System.out.println(e.getMessage());
            }

        }
    }

    private void finishState() {
        //send isDone=1 to the state
        mongoDB.setState(1);

    }
}


public class CrawlerMain {
    static final MongoDB mainMongo = new MongoDB();

    public static void initCrawling(int numThreads, List<String> seedsArray) {

        /*
         * now All The seeds are in the array
         * Num of threads and The number of seeds are critical
         * N_Threads > Seeds? -> That's a Good case where we Can divide them evenly
         * N_Threads < Seeds? -> give each a one seed and The remaining get them seeds from te working Ones
         * */
        int ratio = seedsArray.size() / numThreads; // how many seeds per a Thread
        ArrayList<String> ss;

        if (ratio > 0) {
            //there are enough seeds For The threads
            for (int i = 0; i < numThreads; i++) {
                if (i != numThreads - 1) {
                    ss = new ArrayList<>(seedsArray.subList(i * ratio, (i + 1) * (ratio)));
                } else {
                    //The last threads takes all the remaining
                    ss = new ArrayList<>(seedsArray.subList(i * ratio, seedsArray.size()));
                }
                new Thread(new Crawler(ss)).start();
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
                    new Thread(new Crawler(ss)).start();
                    neededThreads--;
                } else {
                    //The last threads takes all the remaining
                    ss = new ArrayList<>();
                    ss.add(seedsArray.get(i));
                    new Thread(new Crawler(ss, neededThreads)).start();

                    System.out.println(ss);
                }
            }

        }

    }

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws FileNotFoundException {

        //initialize Connection with The Database
        int numThreads = 5;
        System.out.printf("Number of Threads is: %d%n", numThreads);

        //testMongo();

        /*
         *
         * state is -1 |0 | 1
         *
         * -1 : never worked before
         * 0  : worked before but didn't finish
         * 1  : worked and finished
         *
         *
         * */

        int state = mainMongo.getState();
        if (state == -1) {
            // never worked
            readAndProcess(numThreads);
        } else if (state == 0) {
            //continue what it started
            continueAndProcess(numThreads);
        }


        while (true) {
            try {
                sleep(10000);
                reCrawl(numThreads);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    // take your seeds from the unprocessed Stack
    private static void readAndProcess(int numThreads) throws FileNotFoundException {
        mainMongo.setState(0); // start crawling


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
            initCrawling(numThreads, seedsArray);
            fr.close();    //closes the stream and release the resources
        }  //creates a buffering character input stream
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void continueAndProcess(int numThreads) {
        //mainMongo.setState(0); // continue crawling

        mainMongo.getVisitedLinks(Crawler.visitedLinks);

        Crawler.setCount(Crawler.visitedLinks.size());

        //Get The previous State array
        ArrayList<String> seedsArray = (ArrayList<String>) mainMongo.getStateArray();

        initCrawling(numThreads, seedsArray);

    }

    private static void reCrawl(int numThreads) {
        mainMongo.getVisitedLinks(Crawler.visitedLinks);

        Crawler.setCount((int) mainMongo.getUrlCount());

        Crawler.setIsReCrawling(true);

        initCrawling(numThreads, new ArrayList<>());

    }


    private static void testMongo() {
        HashSet<String> arr = new HashSet<>();
        mainMongo.getVisitedLinks(arr);
        System.out.println(arr.size());
    }

}