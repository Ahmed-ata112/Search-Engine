package org.mpack;
import io.mola.galimatias.URL;
import io.mola.galimatias.canonicalize.CombinedCanonicalizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.util.concurrent.CountDownLatch;
import java.io.*;
import java.util.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.lang.Thread.sleep;


class Crawler implements Runnable {
    // Global Data can be Static too
    public static final HashSet<String> visitedLinks = new HashSet<>();
    public static final HashSet<String> websites_hashes = new HashSet<>();
    //Links That were already crawled -- So That You don't put one twice
    // a way to define blocked sites (Robot.txt) is just to put it in the links set without crawling it
    static int count = 0;
    static CountDownLatch latch;
    static final Object cLock = new Object();
    ArrayList<String> initialStrings;
    static final int MAX_PAGES = 1000;
    int neededThreads;
    static final MongoDB mongoDB = new MongoDB();

    public static void setIsReCrawling(boolean isReCrawling) {
        Crawler.isReCrawling = isReCrawling;
    }

    static boolean isReCrawling = false;
    static final List<String> reCrawlingList = List.of("https://www.cnn.com/", "https://abc.com/");
    static CombinedCanonicalizer canonicalized = new CombinedCanonicalizer();

    static Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    static {
        root.setLevel(Level.OFF);
    }

    public Crawler(List<String> initialStrings, int neededThreads) {
        this.initialStrings = (ArrayList<String>) initialStrings;
        this.neededThreads = neededThreads;
    }

    public Crawler(List<String> initialStrings) {
        this(initialStrings, 0);
    }

    @Override
    public void run() {

        if (isReCrawling) {
            reCrawl(reCrawlingList);
        } else {
            mongoDB.deleteState();
            crawl(initialStrings);
        }
    }

    public static String encryptThisString(String input) {
        try {
            // getInstance() method is called with algorithm SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

//            // Add preceding 0s to make it 32 bit
//            while (hashtext.length() < 32) {
//                hashtext = "0" + hashtext;
//            }

            // return the HashText
            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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

            try {
                url = canonicalized.canonicalize(URL.parse(url)).toString();

            } catch (Exception e) {
                //not a valid url
                System.out.println("Error in Canon");
                continue;
            }

            synchronized (visitedLinks) {
                if (visitedLinks.contains(url)) continue;
                else {
                    visitedLinks.add(url);
                    count++;
                }
            }
            try {
                Document document = Jsoup.connect(url).get();
                String hashed = encryptThisString(document.html());
                synchronized (websites_hashes){
                    if (websites_hashes.contains(hashed)) {
                        continue;
                    } else
                        websites_hashes.add(hashed);
                }
                Elements linksOnPage = document.select("a[href]");
                for (Element page : linksOnPage) {
                    String uu = page.attr("abs:href");
                    unprocessedUrlsStack.add(uu);
                    // add to the unprocessed array
                    mongoDB.addToStateArray(uu);

                }

                System.out.println(count);

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
        latch.countDown();

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

            try {
                url = canonicalized.canonicalize(URL.parse(url)).toString();

            } catch (Exception e) {
                //not a valid url
                System.out.println("Error in Canon");
                continue;
            }
            //delete from the unprocessed array
            synchronized (visitedLinks) {

                if (!reCrawlingList.contains(url) && visitedLinks.contains(url)) {
                    continue;
                }
            }
            try {
                Document document = Jsoup.connect(url).get();
                Elements linksOnPage = document.select("a[href]");
                String hashed = encryptThisString(document.html());
                synchronized (websites_hashes){
                    if (websites_hashes.contains(hashed)) {
                        continue;
                    } else
                        websites_hashes.add(hashed);
                }
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
        latch.countDown();

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
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {

        //initialize Connection with The Database
        int numThreads = 20;
        Crawler.latch = new CountDownLatch(numThreads);
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

        if (true || state == -1) {
            // never worked
            System.out.println("here");
            readAndProcess(numThreads);
        } else if (state == 0) {
            //continue what it started
            continueAndProcess(numThreads);
        }

        Crawler.latch.await();      // wait for all The Threads to finish
        System.out.println("Finished Waiting");
        while (true) {
            try {
                sleep(100);
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

        mainMongo.getVisitedLinks();

        Crawler.setCount(Crawler.visitedLinks.size());

        //Get The previous State array
        ArrayList<String> seedsArray = (ArrayList<String>) mainMongo.getStateArray();

        initCrawling(numThreads, seedsArray);

    }

    private static void reCrawl(int numThreads) {
        mainMongo.getVisitedLinks();

        Crawler.setCount((int) mainMongo.getUrlCount());

        Crawler.setIsReCrawling(true);

        initCrawling(numThreads, new ArrayList<>());

    }


    private static void testMongo() {

   /*
        URL u = null;
        u = URL.parse("https://www.PROgramiz.com/java-programming/online-compiler/");
        CombinedCanonicalizer CC = new CombinedCanonicalizer();
        String u2 = CC.canonicalize(u).toString();

        System.out.println(u2.toString());
    */

    }
}