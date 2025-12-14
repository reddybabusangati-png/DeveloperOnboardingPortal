import com.mongodb.client.*;
import org.bson.Document;

import java.time.Instant;
import java.util.*;
import static com.mongodb.client.model.Filters.eq;

/**
 * Developer Onboarding Portal
 * Java + MongoDB mini project
 */
public class DeveloperOnboardingPortal {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "developerOnboardingDB";

    public static void main(String[] args) {

        try (MongoClient mongoClient = MongoClients.create(MONGO_URI)) {

            MongoDatabase db = mongoClient.getDatabase(DB_NAME);
            MongoCollection<Document> devs = db.getCollection("developers");
            MongoCollection<Document> tasks = db.getCollection("tasks");

            Scanner sc = new Scanner(System.in);
            boolean running = true;

            System.out.println(" Developer Onboarding Portal");

            while (running) {
                System.out.println("\nMenu:");
                System.out.println("1. Add Developer");
                System.out.println("2. Add Onboarding Task");
                System.out.println("3. Assign Task to Developer");
                System.out.println("4. Mark Task as Completed");
                System.out.println("5. Show Recommended Tasks");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");
                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {
                    case 1 -> addDeveloper(sc, devs);
                    case 2 -> addTask(sc, tasks);
                    case 3 -> assignTask(sc, devs, tasks);
                    case 4 -> markTaskCompleted(sc, devs);
                    case 5 -> showRecommendations(sc, devs, tasks);
                    case 6 -> {
                        System.out.println(" Exiting Portal...");
                        running = false;
                    }
                    default -> System.out.println("Invalid choice!");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addDeveloper(Scanner sc, MongoCollection<Document> devs) {
        System.out.print("Enter developer name: ");
        String name = sc.nextLine();
        System.out.print("Enter email: ");
        String email = sc.nextLine();
        System.out.print("Enter role (e.g., Backend, Frontend, DevOps): ");
        String role = sc.nextLine();

        Document dev = new Document("name", name)
                .append("email", email)
                .append("role", role)
                .append("joinedAt", Instant.now())
                .append("assignedTasks", new ArrayList<String>())
                .append("completedTasks", new ArrayList<String>());

        devs.insertOne(dev);
        System.out.println(" Developer added successfully!");
    }

    private static void addTask(Scanner sc, MongoCollection<Document> tasks) {
        System.out.print("Enter task ID: ");
        String id = sc.nextLine();
        System.out.print("Enter task title: ");
        String title = sc.nextLine();
        System.out.print("Enter description: ");
        String desc = sc.nextLine();
        System.out.print("Enter role (or leave blank for all): ");
        String role = sc.nextLine();
        System.out.print("Enter priority (1–10): ");
        int priority = Integer.parseInt(sc.nextLine());

        Document task = new Document("_id", id)
                .append("title", title)
                .append("description", desc)
                .append("role", role.isBlank() ? "All" : role)
                .append("priority", priority)
                .append("createdAt", Instant.now());

        tasks.insertOne(task);
        System.out.println("Task added successfully!");
    }

    private static void assignTask(Scanner sc, MongoCollection<Document> devs, MongoCollection<Document> tasks) {
        System.out.print("Enter developer email: ");
        String email = sc.nextLine();
        System.out.print("Enter task ID to assign: ");
        String taskId = sc.nextLine();

        Document dev = devs.find(eq("email", email)).first();
        Document task = tasks.find(eq("_id", taskId)).first();

        if (dev == null) {
            System.out.println(" Developer not found!");
            return;
        }
        if (task == null) {
            System.out.println(" Task not found!");
            return;
        }

        List<String> assigned = dev.getList("assignedTasks", String.class, new ArrayList<>());
        if (!assigned.contains(taskId)) {
            assigned.add(taskId);
            devs.updateOne(eq("email", email), new Document("$set", new Document("assignedTasks", assigned)));
            System.out.println("Task assigned successfully!");
        } else {
            System.out.println("Task already assigned.");
        }
    }

    private static void markTaskCompleted(Scanner sc, MongoCollection<Document> devs) {
        System.out.print("Enter developer email: ");
        String email = sc.nextLine();
        System.out.print("Enter completed task ID: ");
        String taskId = sc.nextLine();

        Document dev = devs.find(eq("email", email)).first();
        if (dev == null) {
            System.out.println(" Developer not found!");
            return;
        }

        List<String> completed = dev.getList("completedTasks", String.class, new ArrayList<>());
        List<String> assigned = dev.getList("assignedTasks", String.class, new ArrayList<>());

        if (assigned.contains(taskId) && !completed.contains(taskId)) {
            completed.add(taskId);
            devs.updateOne(eq("email", email), new Document("$set", new Document("completedTasks", completed)));
            System.out.println(" Task marked as completed!");
        } else {
            System.out.println(" Task not assigned or already completed.");
        }
    }

    private static void showRecommendations(Scanner sc,
                                            MongoCollection<Document> devs,
                                            MongoCollection<Document> tasks) {
        System.out.print("Enter developer email: ");
        String email = sc.nextLine();

        Document dev = devs.find(eq("email", email)).first();
        if (dev == null) {
            System.out.println(" Developer not found!");
            return;
        }

        String role = dev.getString("role");
        List<String> completed = dev.getList("completedTasks", String.class, new ArrayList<>());

        List<Document> allTasks = tasks.find().into(new ArrayList<>());
        List<Document> available = new ArrayList<>();

        for (Document t : allTasks) {
            String taskRole = t.getString("role");
            String taskId = t.getString("_id");
            if ((taskRole.equalsIgnoreCase(role) || taskRole.equalsIgnoreCase("All"))
                    && !completed.contains(taskId)) {
                available.add(t);
            }
        }

        available.sort((a, b) -> b.getInteger("priority") - a.getInteger("priority"));

        System.out.println("\n Recommended Tasks for " + dev.getString("name") + " (" + role + "):");
        if (available.isEmpty()) {
            System.out.println("🎉 All tasks are completed or no role-specific tasks available.");
        } else {
            for (Document t : available) {
                System.out.printf(" %s (Priority %d)\n", t.getString("title"), t.getInteger("priority"));
            }
        }
    }
}