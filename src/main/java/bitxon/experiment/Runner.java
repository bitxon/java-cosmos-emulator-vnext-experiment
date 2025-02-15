package bitxon.experiment;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;

import java.util.Set;

public class Runner {
    public static final String ENDPOINT = "https://localhost:56001";
    public static final String KEY = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";
    public static final Set<Integer> SUCCESS_CODES = Set.of(200, 201);

    public static void main(String[] args) {
        System.out.println("CosmosDb Client - Initializing");
        CosmosClient client = new CosmosClientBuilder()
            .gatewayMode()
            .endpoint(ENDPOINT)
            .credential(new AzureKeyCredential(KEY))
            .buildClient();
        System.out.println("CosmosDb Client - Ready");


        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists("Azure");
        if (!SUCCESS_CODES.contains(databaseResponse.getStatusCode())) {
            System.err.println("Failed to  Create DB");
            System.exit(1);
        }


        CosmosContainerResponse containerResponse = client.getDatabase("Azure")
            .createContainerIfNotExists("ServiceContainer", "/name");
        if (!SUCCESS_CODES.contains(containerResponse.getStatusCode())) {
            System.err.println("Failed to Create Container");
            System.exit(1);
        }
        System.out.println("==== Done ====");
    }
}
