package org.api;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

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
        ArrayList<Document> documents = Q.QueryProcessingFunction(SearchQ);
        System.out.println("QUERY");
        System.out.println(documents);

        Ranker R = new Ranker();
         PriorityQueue<Pair<String, collections>> KP = R.ranker2("", documents);
        PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>>> K = KP.getFirst();
        PriorityQueue<Pair<Pair<String, Pair<String, String>>, Pair<List<Integer>, Pair<Double, Pair<Double, Integer>>>>> P = KP.getSecond();
        System.out.println("PRIORITY0");
        System.out.println(K);
        System.out.println("PRIORITY1");
        System.out.println(P);
        var ts = Q.GetSearchTokens();
        for (var p : K) {
            // p (   <url,pair<para,header>>     |                           )
            var f = p.getFirst();
            String url = f.getFirst();

            var ss = f.getSecond();
            String header = ss.getFirst();
            String para = ss.getSecond();
            Pojo p1 = new Pojo(url, header, ts, para);
            objectsList.add(p1);
        }
        for (var p : P) {

            // p (   <url,pair<para,header>>     |                           )
            var f = p.getFirst();
            String url = f.getFirst();
            var ss = f.getSecond();
            String header = ss.getFirst();
            String para = ss.getSecond();
            Pojo p1 = new Pojo(url, header, ts, para);
            objectsList.add(p1);
        }

        return objectsList;
    }

    @GetMapping(value = "/suggests")
    public List<String> getSuggestion() {
        System.out.println("in sugg");
        return mongoDB.getSuggestionsArray();
    }


}






