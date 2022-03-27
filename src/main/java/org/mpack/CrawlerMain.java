package org.mpack;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Sorts.descending;
import static java.util.Arrays.asList;

class Create {
    public static void main(String[] args) {
        try (MongoClient mongoClient = MongoClients.create(System.getProperty("mongodb.uri"))) {
            MongoDatabase sampleTrainingDB = mongoClient.getDatabase("sample_training");
            MongoCollection<Document> gradesCollection = sampleTrainingDB.getCollection("grades");
        }
    }}

public class CrawlerMain {
    public static void main(String[] args) {
        String connectionString = "mongodb://localhost:27017";
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            List<Document> databases = mongoClient.listDatabases().into(new ArrayList<>());
            databases.forEach(db -> System.out.println(db.toJson()));
            MongoDatabase AcmeDB = mongoClient.getDatabase("acme");
            MongoCollection<Document> gradesCollection = AcmeDB.getCollection("posts");


            Random rand = new Random();
            Document student = new Document("_id", new ObjectId());
            student.append("student_id", 10000d)
                    .append("class_id", 1d)
                    .append("scores", asList(new Document("type", "exam").append("score", rand.nextDouble() * 100),
                            new Document("type", "quiz").append("score", rand.nextDouble() * 100),
                            new Document("type", "homework").append("score", rand.nextDouble() * 100),
                            new Document("type", "homework").append("score", rand.nextDouble() * 100)));
            gradesCollection.insertOne(student);
            List<Integer> books = Arrays.asList(27464, 747854);
            DBObject person = new BasicDBObject("_id", "jo")
                    .append("name", "Jo Bloggs")
                    .append("address", new BasicDBObject("street", "123 Fake St")
                            .append("city", "Faketon")
                            .append("state", "MA")
                            .append("zip", 12345))
                    .append("books", books);
            gradesCollection.insertOne((Document) person);

            List<Document> docs = gradesCollection.find(and(eq("student_id", 10001), lte("class_id", 5)))
                    .projection(fields(excludeId(),
                            include("class_id",
                                    "student_id")))
                    .sort(descending("class_id"))
                    .skip(2)
                    .limit(2)
                    .into(new ArrayList<>());

        }


    }
}
