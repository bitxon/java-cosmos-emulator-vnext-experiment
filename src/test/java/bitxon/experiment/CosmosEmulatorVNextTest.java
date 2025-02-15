package bitxon.experiment;

import bitxon.experiment.testcontainer.CosmosDBEmulatorVNextContainer;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("vnext")
public class CosmosEmulatorVNextTest {
    private static final String DATABASE = "Clinic";
    private static final String CONTAINER = "Patients";

    @TempDir
    static File tempFolder;

    static CosmosDBEmulatorVNextContainer cosmos = new CosmosDBEmulatorVNextContainer()
        .withStartupTimeout(Duration.ofSeconds(30));

    @BeforeAll
    public static void setup() throws Exception {
        cosmos.start();

        Path keyStoreFile = new File(tempFolder, "azure-cosmos-emulator.keystore").toPath();
        KeyStore keyStore = cosmos.buildNewKeyStore();
        keyStore.store(Files.newOutputStream(keyStoreFile.toFile().toPath()), cosmos.getEmulatorKey().toCharArray());

        System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", cosmos.getEmulatorKey());
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void test() {
        CosmosClient client = new CosmosClientBuilder()
            .gatewayMode()
            .endpoint(cosmos.getEmulatorEndpoint())
            .credential(new AzureKeyCredential(cosmos.getEmulatorKey()))
            .buildClient();

        // 1️⃣ Create Database
        var databaseResponse = client.createDatabaseIfNotExists(DATABASE);
        assertThat(databaseResponse.getStatusCode()).as("DB created").isEqualTo(201);
        // 2️⃣ Create Container
        var containerResponse = client.getDatabase(DATABASE).createContainerIfNotExists(CONTAINER, "/name");
        assertThat(containerResponse.getStatusCode()).as("Container created").isEqualTo(201);
        // 3️⃣ Create Item
        var patient = new Patient("10001", "John Doe", 27);
        var itemCreateResponse = client.getDatabase(DATABASE).getContainer(CONTAINER).createItem(patient);
        assertThat(itemCreateResponse.getStatusCode()).as("Item created").isEqualTo(201);
        // 4️⃣ Read Item
        var itemReadResponse = client.getDatabase(DATABASE).getContainer(CONTAINER)
            .readItem(patient.id(), new PartitionKey(patient.name()), Patient.class);
        assertThat(itemReadResponse).extracting(CosmosItemResponse::getItem).as("Item read").isEqualTo(patient);
    }

}
