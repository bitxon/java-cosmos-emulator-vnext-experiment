package bitxon.experiment;

import bitxon.experiment.testcontainer.CosmosDBEmulatorVNextContainer;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.*;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("vnext")
public class CosmosEmulatorVNextHttpsNoCertValidationTest {
    private static final String DATABASE = "Clinic";
    private static final String CONTAINER = "Patients";

    static CosmosDBEmulatorVNextContainer cosmos = new CosmosDBEmulatorVNextContainer()
        .withStartupTimeout(Duration.ofSeconds(30))
        .withProtocol("https");

    @BeforeAll
    public static void setup() {
        cosmos.start();
        System.setProperty("COSMOS.EMULATOR_SERVER_CERTIFICATE_VALIDATION_DISABLED", "true");
    }

    @AfterAll
    public static void tearDown() {
        System.clearProperty("COSMOS.EMULATOR_SERVER_CERTIFICATE_VALIDATION_DISABLED");
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
