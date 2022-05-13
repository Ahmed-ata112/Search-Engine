package org.mpack;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.mpack.MongoDB;
import org.mpack.QueryProcessor;
import org.mpack.Ranker;
import org.mpack.collections;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.io.FileNotFoundException;
import java.net.URI;
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
    final static MongoDB mongoDB = new MongoDB();

    @GetMapping(value = "/Query/{Qword}")
    public List<Pojo> queryProcessor(@PathVariable(value = "Qword") String SearchQ) throws FileNotFoundException {
        List<Pojo> objectsList = new ArrayList<>();
        //TODO: Remember to add The Word to The future Suggestions List


        System.out.println(SearchQ);
        mongoDB.addToSuggestionsArray(SearchQ);

        QueryProcessor Q = new QueryProcessor();

        List<List<Document>> documents = Q.Stem(List.of(SearchQ.toLowerCase().trim().split(" ")));

        System.out.println("QUERY");
        System.out.println(documents);

        Ranker R = new Ranker();
        LinkedHashSet<collections> finalResults = new LinkedHashSet<>();
        for (List<Document> v :
                documents) {
            if (v == null || v.isEmpty() || v.get(0) == null) {
                continue;
            }

            PriorityQueue<Pair<String, collections>> ret = R.ranker2("", v);
            for (var a :
                    ret) {
                finalResults.add(a.getSecond());
            }
        }

        System.out.println("PRIORITY0");
        System.out.println(finalResults);

        var ts = List.of(SearchQ.trim().split(" "));
        for (var p : finalResults) {

            // p (   <url,pair<para,header>>     |                           )

            Pojo p1 = new Pojo(p.url, p.title, ts, p.paragraph);
            objectsList.add(p1);
        }
        System.out.println("Sending with size: " + objectsList.size());
        return objectsList;
    }

    @GetMapping(value = "/suggests")
    public List<String> getSuggestion() {
        System.out.println("in sugg");
        return mongoDB.getSuggestionsArray();
    }


}






