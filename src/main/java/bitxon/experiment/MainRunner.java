package bitxon.experiment;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;

public class MainRunner {
    public static final String ENDPOINT = "https://localhost:8081";
    public static final String KEY = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";
    public static final String DATABASE = "Clinic";
    public static final String CONTAINER = "Patients";

    public static void main(String[] args) {
        CosmosClient client = new CosmosClientBuilder()
            .gatewayMode()
            .endpoint(ENDPOINT)
            .credential(new AzureKeyCredential(KEY))
            .buildClient();

        // 1️⃣ Create Database
        client.createDatabaseIfNotExists(DATABASE);
        // 2️⃣ Create Container
        client.getDatabase(DATABASE).createContainerIfNotExists(CONTAINER, "/name");
        // 3️⃣ Create Item
        client.getDatabase(DATABASE).getContainer(CONTAINER)
            .createItem(new Patient("10001", "John Doe", 27));
    }
}
