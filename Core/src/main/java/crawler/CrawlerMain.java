package crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static crawler.Crawler.encryptThisString;
import static java.lang.Thread.sleep;

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
                System.out.println("Created Thread num: " + i);
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
                    neededThreads--;
                    new Thread(new Crawler(ss)).start();
                } else {
                    //The last threads takes all the remaining
                    ss = new ArrayList<>();
                    ss.add(seedsArray.get(i));
                    neededThreads--;
                    new Thread(new Crawler(ss, neededThreads)).start();
                    System.out.println(ss);
                }
            }


        }

    }

    // take your seeds from the unprocessed Stack
    private static void readAndProcess(int numThreads) throws FileNotFoundException {

        File file = new File(".\\attaches\\seed.txt");    //creates a new file instance
        FileReader fr = new FileReader(file);   //reads the file
        ArrayList<String> seedsArray;
        mainMongo.setState(0); // start crawling
        Crawler.resetDatabase();
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
        mainMongo.getAllArraysBAck();
        Crawler.setCount(Crawler.visitedLinks.size());
        Crawler.setIsContinuing(true);
        System.out.printf("will continue my work with count %d%n", Crawler.atomicCount.get());
        ArrayList<String> seedsArray = (ArrayList<String>) mainMongo.getStateURLs();
        initCrawling(numThreads, seedsArray);
    }

    private static void reCrawl(int numThreads) {
        mainMongo.getAllArraysBAck();
        Crawler.setCount((int) mainMongo.getUrlCount());
        Crawler.setIsReCrawling(true);
        initCrawling(numThreads, new ArrayList<>());
    }


    private static void pagerankInit() {
        PageRank p = new PageRank();
        p.initRankMatrix(Crawler.visitedLinks, Crawler.pagesEdges);
        p.run();

    }

    private static void testMongo() throws IOException {

        Document document = Jsoup.connect("https://time.com/newsletters/?source=SI+hp+link+mid+&newsletter_name=climate").parser(Parser.xmlParser()).get();
        String hashed = encryptThisString(document.body().text().trim());
        document = Jsoup.connect("https://time.com/newsletters/?source=SI+hp+mod+mid+&newsletter_name=the_brief").parser(Parser.xmlParser()).get();
        System.out.println(Objects.equals(encryptThisString(document.body().text().trim()), hashed));


    }

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {

        //initialize Connection with The Database
        int numThreads = 20;
        Crawler.latch = new CountDownLatch(numThreads);
        System.out.printf("Number of Threads is: %d%n", numThreads);

        /*
         * state is -1 |0 | 1
         * -1 : never worked before
         * 0  : worked before but didn't finish
         * 1  : worked and finished
         * */

        int state = mainMongo.getState();
        System.out.printf("state is: %d%n", state);
        if (state == -1) {
            // never worked
            readAndProcess(numThreads);
            Crawler.latch.await();      // wait for all The Threads to finish
            pagerankInit();
        } else if (state == 0) {
            //continue what it started
            continueAndProcess(numThreads);
            Crawler.latch.await();      // wait for all The Threads to finish
            System.out.println("Finished Waiting and started Ranking");
            pagerankInit();
        }
        Crawler.finishState(); //all is done
        //Re crawl

        while (true) {

            try {
                sleep(100);
                Crawler.latch = new CountDownLatch(numThreads); // puts a Countdown for threads
                reCrawl(numThreads);
                Crawler.latch.await();      // wait for all The Threads to finish

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//        try {
//            testMongo();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }


}