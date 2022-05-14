package crawler;/* package whatever; // don't place package name! */

import org.apache.commons.text.WordUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static crawler.Crawler.encryptThisString;
import static org.apache.commons.text.WordUtils.initials;


public class Test {
    static String hashit(String s1) {
        if (s1.length() <= 10) {
            return s1;
        }
        String inits = WordUtils.initials(s1);
        return inits.substring(0, inits.length() / 10);
    }

    public static void main(String[] args) throws IOException {
        Document document = Jsoup.connect("https://editorial.rottentomatoes.com/guide/best-netflix-shows-and-movies-to-binge-watch-now/").parser(Parser.xmlParser()).get();


        String hashed1 = document.body().text().trim();

        document = Jsoup.connect("https://editorialadmin.rottentomatoes.com/guide/best-netflix-shows-and-movies-to-binge-watch-now/").parser(Parser.xmlParser()).get();
        String hashed2 = document.body().text().trim();


        System.out.println(encryptThisString(hashed1));


        System.out.println(encryptThisString(hashed1).equals(encryptThisString(hashed2)));
    }
}