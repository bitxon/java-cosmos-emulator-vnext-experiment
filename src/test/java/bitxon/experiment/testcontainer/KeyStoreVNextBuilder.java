package bitxon.experiment.testcontainer;

import org.testcontainers.containers.GenericContainer;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

class KeyStoreVNextBuilder {
    private static final String CERTIFICATE_PATH = "/scripts/certs/domain.crt";

    static KeyStore buildByExtractingCertificate(GenericContainer<?> container, String keyStorePassword) {
        try {
            String certificate = extractFileContent(container, CERTIFICATE_PATH);
            System.out.println("Extracted Certificate:\n" + certificate + "\n");

            return buildKeyStore(new ByteArrayInputStream(certificate.getBytes(UTF_8)), keyStorePassword);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String extractFileContent(GenericContainer<?> container, String filePath) {
        return container.copyFileFromContainer(
            filePath, inputStream -> new String(inputStream.readAllBytes(), UTF_8)
        );
    }

    private static KeyStore buildKeyStore(InputStream certificateStream, String keyStorePassword) throws Exception {
        Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(certificateStream);
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(null, keyStorePassword.toCharArray());
        keystore.setCertificateEntry("azure-cosmos-emulator", certificate);
        return keystore;
    }
}
