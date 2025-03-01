package bitxon.experiment;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("classic")
public class CosmosEmulatorClassicHttpsTest {
    private static final String DATABASE = "Clinic";
    private static final String CONTAINER = "Patients";

    @TempDir
    static File tempFolder;

    static CosmosDBEmulatorContainer cosmos = new CosmosDBEmulatorContainer(
        DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest"))
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

    @AfterAll
    public static void tearDown() {
        System.clearProperty("javax.net.ssl.trustStoreType");
        System.clearProperty("javax.net.ssl.trustStorePassword");
        System.clearProperty("javax.net.ssl.trustStore");
        cosmos.stop();
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void test() {
        CosmosClient client = new CosmosClientBuilder()
            .gatewayMode()
            .endpoint(cosmos.getEmulatorEndpoint())
            .credential(new AzureKeyCredential(cosmos.getEmulatorKey()))
            .buildClient();

        // 1️⃣ Create Database & Container
        client.createDatabaseIfNotExists(DATABASE);
        client.getDatabase(DATABASE).createContainerIfNotExists(CONTAINER, "/name");
        var container = client.getDatabase(DATABASE).getContainer(CONTAINER);

        // 2️⃣ Create Items
        var patient0 = new Patient("10000", "Alice", 20);
        var patient1 = new Patient("10001", "Alice", 21);
        var patient2 = new Patient("10002", "Bob", 22);
        var patient3 = new Patient("10003", "Charlie", 23);
        container.createItem(patient0);
        container.createItem(patient1);
        container.createItem(patient2);
        container.createItem(patient3);

        // 3️⃣ Read Item
        var readResult = container.readItem(patient2.id(), new PartitionKey(patient2.name()), Patient.class).getItem();
        assertThat(readResult).as("Read Item").isEqualTo(patient2);

        // 4️⃣ Read Many
        var identities = Stream.of(patient1, patient3)
            .map(p -> new CosmosItemIdentity(new PartitionKey(p.name()), p.id())).toList();
        var readManyResults = container.readMany(identities, Patient.class).getResults();
        assertThat(readManyResults).as("Read Many").containsExactlyInAnyOrder(patient1, patient3);

        // 5️⃣ Query Items
        var query = new SqlQuerySpec("SELECT * FROM c WHERE c.name = @name")
            .setParameters(List.of(new SqlParameter("@name", "Alice")));
        var options = new CosmosQueryRequestOptions();
        var queryResults = container.queryItems(query, options, Patient.class).stream().toList();
        assertThat(queryResults).as("Query Items").containsExactlyInAnyOrder(patient0, patient1);
    }
}
