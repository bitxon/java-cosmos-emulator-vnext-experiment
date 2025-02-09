package bitxon.experiment.testcontainer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.security.KeyStore;
import java.util.Map;

public class CosmosDBEmulatorVNextContainer extends GenericContainer<CosmosDBEmulatorVNextContainer> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:vnext-preview");
    private static final int PORT = 8081;

    public CosmosDBEmulatorVNextContainer() {
        this(DEFAULT_IMAGE_NAME);
    }

    public CosmosDBEmulatorVNextContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.withExposedPorts(PORT);
        this.withEnv(Map.of(
            "PORT", String.valueOf(PORT),
            "PROTOCOL", "https",
            "ENABLE_EXPLORER", "false",
            "LOG_LEVEL", "trace"
        ));
        this.waitingFor(Wait.forLogMessage(".*Now listening on.*\\n", 1));
    }

    public KeyStore buildNewKeyStore() {
        return KeyStoreVNextBuilder.buildByExtractingCertificate(this, this.getEmulatorKey());
    }

    public String getEmulatorKey() {
        return "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";
    }

    public String getEmulatorEndpoint() {
        return "https://%s:%d".formatted(getHost(), getMappedPort(PORT));
    }
}
