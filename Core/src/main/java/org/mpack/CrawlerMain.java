package org.mpack;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.mola.galimatias.URL;
import io.mola.galimatias.canonicalize.CombinedCanonicalizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;


class Crawler implements Runnable {
    // Global Data can be Static too
    public static final HashSet<String> visitedLinks = new HashSet<>();
    public static final HashSet<String> websites_hashes = new HashSet<>();
    //Links That were already crawled -- So That You don't put one twice
    // a way to define blocked sites (Robot.txt) is just to put it in the links set without crawling it

    static final Map<String, Set<String>> pagesEdges = new HashMap<>();
    static AtomicInteger atomicCount = new AtomicInteger(0);
    static CountDownLatch latch;

    ArrayList<String> initialStrings;
    static final int MAX_PAGES = 50;
    int neededThreads;
    static final MongoDB mongoDB = new MongoDB();

    public static void setIsReCrawling(boolean isReCrawling) {
        Crawler.isReCrawling = isReCrawling;
    }

    static boolean isReCrawling = false;

    public static void setIsContinuing(boolean isContinuing) {
        Crawler.isContinuing = isContinuing;
    }

    static boolean isContinuing = false;

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

    public static void resetDatabase() {
        mongoDB.resetStateForReCrawling();

    }

    @Override
    public void run() {

        if (isReCrawling) {
            reCrawl(reCrawlingList);
        } else {
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

            // return the HashText
            return no.toString(16);
        }
        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            System.out.println("No algorithm found");
            return "";
        }
    }

    public static void setCount(int oldCount) {
        atomicCount.set(oldCount);
    }

    /**
     * @param url url passed
     * @return empty string if unsuccessful
     * normalized url if all well
     */
    private String urlIsValid(String url) {
        try {
            url = canonicalized.canonicalize(URL.parse(url)).toString();

        } catch (Exception e) {
            //not a valid url
            System.out.println("Error in Canon");
            return "";
        }

        synchronized (visitedLinks) {
            if (visitedLinks.contains(url) || RobotHandler.isDisallowed(url))
                return "";
            else {
                visitedLinks.add(url);
            }
        }
        return url;
    }

    private Document parseDocument(String url) throws IOException {
        Document document = Jsoup.connect(url).parser(Parser.xmlParser()).get();
        if (!document.select("html").attr("lang").contains("en")) {
            // not an english website
            return null;
        }

        String hashed = encryptThisString(document.html().trim());
        synchronized (websites_hashes) {
            if (websites_hashes.contains(hashed)) {
                return null;

            } else
                websites_hashes.add(hashed);
        }
        return document;
    }

    public void crawl(List<String> seedUrls) {

        Deque<String> unprocessedUrlsStack = new ArrayDeque<>(seedUrls);

        while (!unprocessedUrlsStack.isEmpty()) {
            if (atomicCount.get() > MAX_PAGES) {
                break; //exit
            }
            //To make sure The Queue is large enough not to make it idle here
            String url = getNextUrl(unprocessedUrlsStack);

            url = urlIsValid(url);
            if (url.equals(""))
                continue; // wasn't successful
            try {
                Document document = parseDocument(url);
                if (document == null) {
                    continue; // wasn't successful
                }
                Elements linksOnPage = document.select("a[href]");
                Set<String> neighbors = new LinkedHashSet<>();
                for (Element page : linksOnPage) {
                    String uu = page.attr("abs:href");
                    neighbors.add(uu);
                    unprocessedUrlsStack.add(uu);
                    mongoDB.addToStateUrls(uu);
                }
                //now we really processed a link
                atomicCount.incrementAndGet();
                pagesEdges.put(url, neighbors);
                mongoDB.addToRelationsDB(url, neighbors);
                mongoDB.insertUrl(url, document.html().trim());
            } catch (Exception e) {
                System.err.println("For '" + url + "': " + e.getMessage());
                System.out.println(e.getMessage());
            }

        }
        // Out but with links less than Count
        //state should Remain 0
        System.out.printf("thread finished remained %d and count = %d%n", neededThreads, latch.getCount());


        latch.countDown();
        //if its responsible for creating any threads - make them finish too
        while (neededThreads > 0) {
            latch.countDown();
            neededThreads--;
        }

    }


    public void reCrawl(List<String> seedUrls) {

        Deque<String> unprocessedUrlsStack = new ArrayDeque<>(seedUrls);
        String url;
        while (!unprocessedUrlsStack.isEmpty()) {
            //To make sure The Queue is large enough not to make it idle here
            url = getNextUrl(unprocessedUrlsStack);

            try {
                url = canonicalized.canonicalize(URL.parse(url)).toString();

            } catch (Exception e) {
                //not a valid url
                System.out.println("Error in Canon");
                continue;
            }
            //delete from the unprocessed array
            synchronized (visitedLinks) {
                if (RobotHandler.isDisallowed(url))
                    continue;

                visitedLinks.add(url);
            }
            try {
                Document document = Jsoup.connect(url).get();
                if (!document.select("html").attr("lang").contains("en")) {
                    // not an english website
                    continue;
                }
                Elements linksOnPage = document.select("a[href]");
                String hashed = encryptThisString(document.html());
                synchronized (websites_hashes) {
                    if (websites_hashes.contains(hashed)) {
                        continue;
                    } else
                        websites_hashes.add(hashed);
                }
                for (Element page : linksOnPage) {
                    String uu = page.attr("abs:href");
                    unprocessedUrlsStack.add(uu);
                    mongoDB.addToStateUrls(uu);


                }
                synchronized (visitedLinks) {
                    if (!visitedLinks.contains(url)) {
                        mongoDB.updateUrl(url, document.html());
                        visitedLinks.add(url);
                    }//now we really processed a link
                }
            } catch (Exception e) {
                System.err.println("For '" + url + "': " + e.getMessage());
                System.out.println(e.getMessage());
            }

        }
        latch.countDown();
        //if its responsible for creating any threads - make them finish too
        while (neededThreads > 0) {
            latch.countDown();
            neededThreads--;
        }
    }

    private String getNextUrl(Deque<String> unprocessedUrlsStack) {
        String url;
        if (neededThreads > 0 && unprocessedUrlsStack.size() > 10) {
            url = unprocessedUrlsStack.pop();
            ArrayList<String> toSend = new ArrayList<>();
            toSend.add(url);
            new Thread(new Crawler(toSend)).start();
            neededThreads--;
        }

        url = unprocessedUrlsStack.pop();
        mongoDB.removeFromStateUrls(url);
        return url;
    }

    public static void finishState() {
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
        System.out.printf("will continue my work with count %d", Crawler.atomicCount.get());
        ArrayList<String> seedsArray = (ArrayList<String>) mainMongo.getStateURLs();
        initCrawling(numThreads, seedsArray);
    }

    private static void reCrawl(int numThreads) {
        mainMongo.getAllArraysBAck();
        Crawler.setCount((int) mainMongo.getUrlCount());
        Crawler.setIsReCrawling(true);
        initCrawling(numThreads, new ArrayList<>());
    }


    private static void testMongo() {
        String url = "https://www.google.com/doodles";

        MongoDB mm = new MongoDB();
        System.out.println(mm.getRelations());
    }

    private static void pagerankInit() {
        PageRank p = new PageRank();
        p.initRankMatrix(Crawler.visitedLinks, Crawler.pagesEdges);
        p.run();

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
            System.out.println("here");
            readAndProcess(numThreads);
            Crawler.latch.await();      // wait for all The Threads to finish
            pagerankInit();
        } else if (state == 0) {
            //continue what it started
            continueAndProcess(numThreads);
            Crawler.latch.await();      // wait for all The Threads to finish
            pagerankInit();
        }
        Crawler.finishState(); //all is done
        System.out.println("Finished Waiting");
        System.out.println("RE_CRAWLING");
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

        //testMongo();
    }


}