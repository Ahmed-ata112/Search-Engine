package org.mpack;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;


class global_Vars_Wrapper {
    final HashSet<String> visitedLinks;  //Links That were already crawled -- So That You don't put one twice
    // a way to define blocked sites (Robot.txt) is just to put it in the links set without crawling it
    Integer count;

    public global_Vars_Wrapper() {
        visitedLinks = new HashSet<String>();
        count = 0;
    }
}

class Crawler implements Runnable {
    // Global Data can be Static too
    global_Vars_Wrapper Mywrapper;
    ArrayList<String> initial_strings;
    int needed_threads = 0;


    public Crawler(ArrayList<String> initial_strings, global_Vars_Wrapper Mywrapper, int needed_threads) {
        this.Mywrapper = Mywrapper;
        this.initial_strings = initial_strings;
        this.needed_threads = needed_threads;

    }

    public Crawler(ArrayList<String> initial_strings, global_Vars_Wrapper Mywrapper) {
        this(initial_strings, Mywrapper, 0);

    }


    @Override
    public void run() {
        System.out.println(Thread.currentThread().getId());
        Crawl(initial_strings);
        //getPageLinks(initial_string);
    }

    public void getPageLinks(String URL) {
        if (Mywrapper.count > 1000) {
            return;
        }
        // 4. Check if you have already crawled the URLs
        // (we are intentionally not checking for duplicate content in this example)
        synchronized (Mywrapper.visitedLinks) {
            if (Mywrapper.visitedLinks.contains(URL))
                return;
            else if (Mywrapper.visitedLinks.add(URL)) {
                System.out.println(Mywrapper.count + " " + URL);
                Mywrapper.count++;
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

    Stack<String> Unprocessed_URLS_Stack = new Stack<String>();

    public void Crawl(ArrayList<String> SeedUrls) {

        Unprocessed_URLS_Stack.addAll(SeedUrls);

        while (!Unprocessed_URLS_Stack.isEmpty()) {
            if (Mywrapper.count > 1000) {
                return;
            }
            String url;
            //To make sure The Queue is large enough not to make it idle here
            if (needed_threads > 0 && Unprocessed_URLS_Stack.size() > 10) {
                url = Unprocessed_URLS_Stack.pop();
                ArrayList<String> toSend = new ArrayList<String>();
                toSend.add(url);
                new Thread(new Crawler(toSend, Mywrapper)).start();
                needed_threads--;
            }

            url = Unprocessed_URLS_Stack.pop();

            synchronized (Mywrapper.visitedLinks) {
                if (Mywrapper.visitedLinks.contains(url))
                    continue;
                else if (Mywrapper.visitedLinks.add(url)) {
                    System.out.println(Mywrapper.count + " " + url);
                    Mywrapper.count++;
                }
            }
            try {

                Document document = Jsoup.connect(url).get();
                Elements linksOnPage = document.select("a[href]");
                for (Element page : linksOnPage) {
                    Unprocessed_URLS_Stack.add(page.attr("abs:href"));
                }
            } catch (Exception e) {
                System.err.println("For '" + url + "': " + e.getMessage());
                System.out.println(e.toString());
            }

        }
    }
}


public class CrawlerMain {

    static global_Vars_Wrapper wr = new global_Vars_Wrapper();


    public CrawlerMain() {

    }


    public static void main(String[] args) {
        // TODO: Add the option to continue its job from where it left
        int numThreads = Integer.parseInt(args[0]);
        System.out.printf("Number of Threads is: %d%n", numThreads);
        try {
            File file = new File(".\\attaches\\seed.txt");    //creates a new file instance
            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            String line;
            ArrayList<String> seedsArray = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                //Read what in The seed
                seedsArray.add(line.toString());
            }




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
                        // 0 1 2 3 4 5 6 7 8 9
                        ss = new ArrayList<>(seedsArray.subList(i * ratio, (i + 1) * (ratio)));
                        System.out.println(ss);
                        new Thread(new Crawler(ss, wr)).start();
                    } else {
                        //The last threads takes all the remaining
                        ss = new ArrayList<>(seedsArray.subList(i * ratio, seedsArray.size()));
                        new Thread(new Crawler(ss, wr)).start();

                        System.out.println(ss);
                    }
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