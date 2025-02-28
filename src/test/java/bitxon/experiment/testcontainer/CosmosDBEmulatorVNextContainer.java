package bitxon.experiment.testcontainer;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;

public class CosmosDBEmulatorVNextContainer extends GenericContainer<CosmosDBEmulatorVNextContainer> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:vnext-preview");
    private final int port;
    private String protocol = "http";

    public CosmosDBEmulatorVNextContainer() {
        this(DEFAULT_IMAGE_NAME);
    }

    public CosmosDBEmulatorVNextContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.port = getRandomAvailablePort();

        this.withExposedPorts(port);
        this.withEnv(Map.of(
            "PORT", String.valueOf(port),
            "PROTOCOL", protocol,
            "ENABLE_EXPLORER", "false",
            "LOG_LEVEL", "trace"
        ));
        this.withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
            // Internal & Exposed ports must be equal - because cosmos-client is trying to connect to internal port
            new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(port), new ExposedPort(port)))
        ));
        this.waitingFor(Wait.forLogMessage(".*Now listening on.*\\n", 1));
    }

    public CosmosDBEmulatorVNextContainer withProtocol(String protocol) {
        this.protocol = Objects.requireNonNull(protocol, "Protocol must not be null");
        return withEnv("PROTOCOL", protocol);
    }

    public KeyStore buildNewKeyStore() {
        return KeyStoreVNextBuilder.buildByExtractingCertificate(this, this.getEmulatorKey());
    }

    public String getEmulatorKey() {
        return "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";
    }

    public String getEmulatorEndpoint() {
        return "%s://%s:%d".formatted(protocol, getHost(), getMappedPort(port));
    }

    private static int getRandomAvailablePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find an available port", e);
        }
    }
}
