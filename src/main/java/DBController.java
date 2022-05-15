import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import org.bson.conversions.Bson;
import org.json.JSONObject;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.regex.Pattern;

public class DBController {
    private static DBController dbController;
    private static final String MONGO_URI = "";
    private static MongoClient client;
    private static MongoDatabase database;
    private static MongoCollection collection;


    public DBController() {
    }

    public static synchronized DBController getInstance() {
        if (dbController == null) {
            dbController = new DBController();
            client = new MongoClient(new MongoClientURI(MONGO_URI));
            database = client.getDatabase("data");
            collection = database.getCollection("422116466");
        }
        return dbController;
    }

    public static MongoDatabase getDatabase() {
        return database;
    }

    public void createCollection(String cl) {
        database.createCollection(cl);
    }

    public void removeRecipe(String title) {
        collection.deleteOne(Filters.eq("title", title));
    }

    public void updateField(String docTitle, String field, String value) {
        collection.updateOne(Filters.eq("title", docTitle), Updates.set(field, value));
    }

    public void updateField(String docTitle, String field, String[] components) {
        collection.updateOne(Filters.eq("title", docTitle), Updates.set(field, Arrays.asList(components)));
    }


    public static void updateField(String docTitle, String field, byte[] imageBytes) {
        Document doc = (Document) collection.find(Filters.eq("title", docTitle)).first();
        String encodedString = Base64
                .getEncoder()
                .encodeToString(imageBytes);
        collection.updateOne(Filters.eq("title", docTitle), Updates.set(field, encodedString));
    }

    public void addRecipe(String userID, String title, String[] components, String preparation, byte[] imageBytes,String user) {
        Document document = new Document("title", title);
        document.put("Components", Arrays.asList(components));
        document.put("Cooking", preparation);
        document.put("User",user);
        if (imageBytes != null) {
            String encodedString = Base64
                    .getEncoder()
                    .encodeToString(imageBytes);
            document.append("photo", encodedString);
        }

        MongoCollection mc = database.getCollection(userID);
        mc.insertOne(document);

    }

    public static byte[] loadImage(String filePath) throws Exception {
        File file = new File(filePath);
        int size = (int) file.length();
        byte[] buffer = new byte[size];
        FileInputStream in = new FileInputStream(file);
        in.read(buffer);
        in.close();
        return buffer;
    }

    public ArrayList recipeList(String userID) {
        ArrayList recipeTitles = new ArrayList();
        collection = database.getCollection(userID);
        FindIterable<Document> iterDoc = collection.find();
        Iterator it = iterDoc.iterator();
        while (it.hasNext()) {
            recipeTitles.add(new JSONObject(new Document((Document) it.next())).get("title"));
        }
        return recipeTitles;
    }

    public ArrayList<JSONObject> findRecipeByTitle(String title) {
        ArrayList <JSONObject>filteredRecipes = new ArrayList();
        String patternStr = "";
        if (title.matches(".*[(]\\d{1,2}[)]$")) {
            title = title.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
            patternStr = ".*" + title + ".*";
        } else   patternStr = title + "$";

            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Bson filter = Filters.regex("title", pattern);

            FindIterable<Document> iterDoc = collection.find(filter);
            Iterator it = iterDoc.iterator();
            while (it.hasNext()) {
                filteredRecipes.add(new JSONObject(new Document((Document) it.next())));
            }
            return filteredRecipes;
    }

    public ArrayList<JSONObject> findRecipeByTitle(String title, Boolean isSearch) {
        ArrayList <JSONObject>filteredRecipes = new ArrayList();
        String patternStr = "";
        if (title.matches(".*[(]\\d{1,2}[)]$")) {
            title = title.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
        }
        patternStr = ".*" + title + ".*";
        Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
        Bson filter = Filters.regex("title", pattern);

        FindIterable<Document> iterDoc = collection.find(filter);
        Iterator it = iterDoc.iterator();
        while (it.hasNext()) {
            filteredRecipes.add(new JSONObject(new Document((Document) it.next())));
        }
        return filteredRecipes;
    }
}
