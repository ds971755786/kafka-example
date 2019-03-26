package io.dongsheng.mongo;

import org.bson.Document;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MongoServiceTest {
    private AtomicInteger num = new AtomicInteger(1);
    private Random rand = new Random();

    public String randomLetter(int num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < num; i++) {
            char c = (char) ('a' + rand.nextInt(26));
            sb.append(c);
        }
        return sb.toString();
    }

    public String randomNumbers(int num) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < num; i++) {
            sb.append(rand.nextInt(10));
        }
        return sb.toString();
    }

    public String name() {
        if (rand.nextInt(2) == 0) {
            return randomLetter(7) + " Pizzeria";
        } else {
            return randomLetter(7) + " Coffer Bar";
        }
    }

    public String cell() {
        return randomNumbers(3) + "-" + randomNumbers(3) + "-" + randomNumbers(4);
    }

    public String email() {
        return randomLetter(9) + "@maestro.com";
    }

    public List<Double> location() {
        if (rand.nextInt(2) == 0) {
            return Arrays.asList(-73.88502, 40.749556);
        } else {
            return Arrays.asList(-73.97902, 40.8479556);
        }
    }

    public List<String> category() {
        String[] categories = {"Pizzeria", "Italian", "Pasta", "Coffee", "Pastries"};
        List<String> c = new ArrayList<>();
        for (int i = 0; i < categories.length; i++) {
            if (rand.nextInt(2) == 0) {
                c.add(categories[i]);
            }
        }
        return c;
    }

    public Document document() {
        int n = num.getAndIncrement();
        Document doc = new Document("name", name())
                .append("contact", new Document("phone", cell()))
                .append("email", email())
                .append("location", location())
                .append("stars", rand.nextInt(6))
                .append("categories", category())
                .append("enen", n);
        return doc;
    }

    @Test
    public void insert() throws InterruptedException {
        MongoService mongoService = new MongoService();
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        mongoService.init(MongoService.MONGOS, "test", "daddario");

        CountDownLatch latch = new CountDownLatch(1);

        ScheduledFuture scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(()-> {
            List<Document> docs = new ArrayList<>();
                    try {
                        for (int i = 0; i < 2; i++) {
                            docs.add(document());
                        }
                        mongoService.write( docs);
                    } catch (Exception e) {
                        System.out.println("" + docs.get(0).get("enen") + docs.get(1).get("enen"));
                        e.printStackTrace();

                    }
                }, 0, 20, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(()->{System.out.println("shutting down");scheduledFuture.cancel(false); latch.countDown();}));
        latch.await();
    }

    public void documents() {
        Document doc1 = new Document("name", "Amarcord Pizzeria")
                .append("contact", new Document("phone", "264-555-0193")
                        .append("email", "amarcord.pizzeria@example.net")
                        .append("location", Arrays.asList(-73.88502, 40.749556)))
                .append("stars", 2)
                .append("categories", Arrays.asList("Pizzeria", "Italian", "Pasta"));


        Document doc2 = new Document("name", "Blue Coffee Bar")
                .append("contact", new Document("phone", "604-555-0102")
                        .append("email", "bluecoffeebar@example.com")
                        .append("location",Arrays.asList(-73.97902, 40.8479556)))
                .append("stars", 5)
                .append("categories", Arrays.asList("Coffee", "Pastries"));

        List<Document> documents = new ArrayList<Document>();
        documents.add(doc1);
        documents.add(doc2);
    }
}
