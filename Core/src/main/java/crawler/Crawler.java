package crawler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.mola.galimatias.URL;
import io.mola.galimatias.canonicalize.CombinedCanonicalizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.mpack.RobotHandler;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Crawler implements Runnable {
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
