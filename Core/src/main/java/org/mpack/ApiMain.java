package org.mpack;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import crawler.MongoDB;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;

@SpringBootApplication
public class ApiMain {

    public static void main(String[] args) {
        SpringApplication.run(ApiMain.class, args);
    }

}


@Getter
@AllArgsConstructor
class Pojo {
    private String url;
    private String header;
    private List<String> tokens;
    private String paragraph;

}

@RestController
class Api {
    static final MongoDB mongoDB = new MongoDB();
    static final Ranker ranker = new Ranker();
    static final QueryProcessor queryProcessor = new QueryProcessor();

    @GetMapping(value = "/Query/{Qword}")
    public List<Pojo> queryProcessor(@PathVariable(value = "Qword") String searchQ) throws FileNotFoundException {
        List<Pojo> objectsList = new ArrayList<>();

        mongoDB.addToSuggestionsArray(searchQ);
        boolean isPhraseSearching = false;

        searchQ = searchQ.toLowerCase().trim();

        if (searchQ.charAt(0) == '"' && searchQ.charAt(searchQ.length() - 1) == '"') {
            isPhraseSearching = true;
            searchQ = searchQ.substring(1, searchQ.length() - 1);
        }

        System.out.println("query is " + searchQ);
        var ts = new ArrayList<>(List.of(searchQ.split("\\s+")));
        List<List<Document>> documents = queryProcessor.ProcessQuery(ts, isPhraseSearching);
        HashSet<String> resultsUrls = new HashSet<>();
        //System.out.println("QUERY");
        //System.out.println(documents);

        // window size
        for (List<Document> v :
                documents) {
            if (v == null || v.isEmpty() || v.get(0) == null) {
                continue;
            }

            int wordsCount = 00;


            List< collections> ret = ranker.ranker2("", v,ts,isPhraseSearching, wordsCount);
            for (var a : ret) {
                if (resultsUrls.add(a.url)) {
                    Pojo p1 = new Pojo(a.url, a.title, ts, a.paragraph);
                    objectsList.add(p1);
                }
            }

        }
        Ranker.clearAllUrls();
        System.out.println("Sending  size: " + objectsList.size());
        return objectsList;
    }

    @GetMapping(value = "/suggests")
    public List<String> getSuggestion() {
        return mongoDB.getSuggestionsArray();
    }


}






