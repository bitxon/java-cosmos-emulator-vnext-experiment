package bitxon.experiment;

import bitxon.experiment.testcontainer.CosmosDBEmulatorVNextContainer;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("vnext")
public class CosmosEmulatorVNextTest {

    @TempDir
    static File tempFolder;

    static CosmosDBEmulatorVNextContainer cosmos = new CosmosDBEmulatorVNextContainer()
        .withStartupTimeout(Duration.ofSeconds(30));

    @BeforeAll
    public static void setup() throws Exception {
        System.out.println("CosmosDb Container - Starting");
        cosmos.start();
        System.out.println("CosmosDb Container - Ready");


        Path keyStoreFile = new File(tempFolder, "azure-cosmos-emulator.keystore").toPath();
        KeyStore keyStore = cosmos.buildNewKeyStore();
        keyStore.store(Files.newOutputStream(keyStoreFile.toFile().toPath()), cosmos.getEmulatorKey().toCharArray());
        System.out.println("CosmosDb Container - Certificate Extracted");

        var trustManagerFactory = TrustManagerFactory.getInstance("X509");
        trustManagerFactory.init(keyStore);
        var sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        SSLContext.setDefault(sslContext);

        System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", cosmos.getEmulatorKey());
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        System.out.println("CosmosDb Container - TrustStore configured");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void test() {
        System.out.println("CosmosDb Client - Initializing");
        CosmosClient client = new CosmosClientBuilder()
            .endpoint(cosmos.getEmulatorEndpoint())
            .credential(new AzureKeyCredential(cosmos.getEmulatorKey()))
            .buildClient();
        System.out.println("CosmosDb Client - Ready");

        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists("Azure");
        assertThat(databaseResponse.getStatusCode()).isEqualTo(201);
        CosmosContainerResponse containerResponse = client.getDatabase("Azure")
            .createContainerIfNotExists("ServiceContainer", "/name");
        assertThat(containerResponse.getStatusCode()).isEqualTo(201);
    }

}

