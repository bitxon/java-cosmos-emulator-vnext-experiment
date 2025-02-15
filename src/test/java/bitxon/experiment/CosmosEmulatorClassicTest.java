package bitxon.experiment;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("classic")
public class CosmosEmulatorClassicTest {

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

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void test() {
        CosmosClient client = new CosmosClientBuilder()
            .endpoint(cosmos.getEmulatorEndpoint())
            .credential(new AzureKeyCredential(cosmos.getEmulatorKey()))
            .gatewayMode()
            .buildClient();

        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists("Azure");
        assertThat(databaseResponse.getStatusCode()).isEqualTo(201);
        CosmosContainerResponse containerResponse = client.getDatabase("Azure")
            .createContainerIfNotExists("ServiceContainer", "/name");
        assertThat(containerResponse.getStatusCode()).isEqualTo(201);
    }

}
