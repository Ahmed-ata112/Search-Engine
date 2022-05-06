package org.api;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.mpack.MongoDB;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
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
    private String[] tokens;
    private String paragraph;

}

@RestController
class Api {
    final static MongoDB mongoDB = new MongoDB();

    @GetMapping(value = "/Query/{Qword}")
    public List<Pojo> queryProcessor(@PathVariable(value = "Qword") String SearchQ) {
        List<Pojo> objectsList = new ArrayList<>();
        //TODO: Remember to add The Word to The future Suggestions List


        System.out.println(SearchQ);
        mongoDB.addToSuggestionsArray(SearchQ);


        Lorem lorem = LoremIpsum.getInstance();
        for (int i = 0; i < 986; i++) {
            Pojo p1 = new Pojo("www.google.com" + i, "header", new String[]{"h1", "h2"}, lorem.getParagraphs(1, 1) + "h1" + lorem.getParagraphs(1, 1));
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






