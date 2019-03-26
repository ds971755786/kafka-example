package io.dongsheng.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.CommandListener;
import org.bson.Document;

import java.io.Closeable;
import java.util.List;

public class MongoService implements Closeable {
    public static final String MONGOS = "mongodb://10.19.140.200:30359";
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public void init(String url, String db, String collectionName) {
        ConnectionString connectionString = new ConnectionString(url);
        //CommandListener myCommandListener = ...;
        MongoClientSettings settings = MongoClientSettings.builder()
                //.addCommandListener(myCommandListener)
                .applyConnectionString(connectionString)
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase(db);
        collection = database.getCollection(collectionName).withWriteConcern(WriteConcern.W1);
    }

    public void write(List<Document> docs) {
        collection.insertMany(docs);
    }



    @Override
    public void close() {
        mongoClient.close();
    }
}
