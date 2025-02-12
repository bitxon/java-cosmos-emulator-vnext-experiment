package bitxon.experiment.testcontainer;

import org.testcontainers.containers.GenericContainer;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

class KeyStoreVNextBuilder {
    private static final String DOMAIN_CRT = "/scripts/certs/domain.crt";
    private static final String ROOT_CA_CRT = "/scripts/certs/rootCA.crt";

    static KeyStore buildByExtractingCertificate(GenericContainer<?> container, String keyStorePassword) {
        try {
            String rootCert = extractFileContent(container, ROOT_CA_CRT);
            System.out.println("Extracted Root Certificate:\n" + rootCert + "\n");

            String domainCert = extractFileContent(container, DOMAIN_CRT);
            System.out.println("Extracted Domain Certificate:\n" + domainCert + "\n");

            return buildKeyStore(
                new ByteArrayInputStream(rootCert.getBytes(UTF_8)),
                new ByteArrayInputStream(domainCert.getBytes(UTF_8)),
                keyStorePassword
            );
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String extractFileContent(GenericContainer<?> container, String filePath) {
        return container.copyFileFromContainer(
            filePath, inputStream -> new String(inputStream.readAllBytes(), UTF_8)
        );
    }

    private static KeyStore buildKeyStore(InputStream rootCertStream, InputStream domainCertStream, String keyStorePassword) throws Exception {
        Certificate rootCert = CertificateFactory.getInstance("X.509").generateCertificate(rootCertStream);
        Certificate domainCert = CertificateFactory.getInstance("X.509").generateCertificate(domainCertStream);

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(null, keyStorePassword.toCharArray());
        keystore.setCertificateEntry("azure-cosmos-emulator-root", rootCert);
        keystore.setCertificateEntry("azure-cosmos-emulator-domain", domainCert);

        return keystore;
    }
}
