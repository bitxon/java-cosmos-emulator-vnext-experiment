# CosmosDb VNext emulator & Java SDK

## Run container
 ```shell
docker run --detach --name cosmosdb-vnext --publish 8081:8081 --publish 1234:1234 mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:vnext-preview --protocol https
 ```

---
## Run Application
1. Copy certificates (.crt) & (*.pem)
```shell
docker cp cosmosdb-vnext:/scripts/certs/domain.crt .temp/domain.crt
docker cp cosmosdb-vnext:/scripts/certs/rootCA.crt .temp/rootCA.crt
docker cp cosmosdb-vnext:/root/cosmos-explorer/node_modules/.cache/webpack-dev-server/server.pem .temp/ui.pem
```
2. Save to toolchain 
```shell
keytool -importcert -alias cosmosdb-root -file .temp/rootCA.crt -trustcacerts -cacerts -storepass changeit -noprompt
keytool -importcert -alias cosmosdb-domain -file .temp/domain.crt -trustcacerts -cacerts -storepass changeit -noprompt
```

3. Run Java App
```shell
./gradlew run
```

4. Cleanup
```shell
keytool -delete -alias cosmosdb-root -cacerts -storepass changeit -noprompt
keytool -delete -alias cosmosdb-domain -cacerts -storepass changeit -noprompt
```

---
## Play With Certificates

1. Download expected certificate
```shell
openssl s_client -connect localhost:8081 -showcerts </dev/null | openssl x509 -outform PEM > .temp/db-downloaded.pem
openssl s_client -connect localhost:1234 -showcerts </dev/null | openssl x509 -outform PEM > .temp/ui-downloaded.pem
```

2. Verify certificate 
```shell
openssl verify -CAfile .temp/rootCA.crt .temp/db-downloaded.pem
openssl verify -CAfile .temp/domain.crt .temp/db-downloaded.pem
```

3. Compare fingerprint
```shell
openssl x509 -in .temp/rootCA.crt -noout -fingerprint
openssl x509 -in .temp/domain.crt -noout -fingerprint
openssl x509 -in .temp/db-downloaded.pem -noout -fingerprint
```
Note: domain.crt match db-downloaded.pem

---
## Other useful commands
Find Certificates on file system
```shell
find / -type f \( -iname "*.crt" -o -iname "*.pem" \) 2>/dev/null
find / -type f \( -iname "*.crt" -o -iname "*.pem" \) -exec grep -H "BEGIN CERTIFICATE" {} +
find / -type f \( -iname "*.crt" -o -iname "*.pem" \)  2>/dev/null -exec grep -H "MIIEIzCCAwugAwIBAgIUSRjST9HfaxF8S2zMPAFM33xNIRMwDQYJKoZIhvcNAQEL" {} +
```