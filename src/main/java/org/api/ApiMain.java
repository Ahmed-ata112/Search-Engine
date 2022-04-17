package org.api;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import lombok.AllArgsConstructor;
import lombok.Getter;
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
    private String paragraph;
}

@RestController
class Api {

    @GetMapping(value="/Query/{Qword}")
    public List<Pojo> index(@PathVariable(value = "Qword") String SearchQ) {
        List<Pojo> objectsList =new ArrayList<>();
        //TODO: Remember to add The Word to The future Suggestions List

        Lorem lorem = LoremIpsum.getInstance();

        for (int i = 0; i < 1000; i++) {
            Pojo p1 = new Pojo(""+i ,lorem.getParagraphs(1,3));
            objectsList.add(p1);
        }

        return objectsList;
    }

    @GetMapping(value="/suggests")
    public List<String> getSuggestion(){
        ArrayList<String> arr = new ArrayList<>();

        arr.add("as");
        arr.add("asdasd");
        arr.add("fdfsd");

        return arr;
    }



}






