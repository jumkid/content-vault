# Docker for Content Vault microserivce 
FROM openjdk:11
ARG env
# local file storage path
RUN mkdir -p /opt/content-vault/logs

COPY keystore/truststore.p12 /opt/content-vault/keystore/truststore.p12
COPY src/main/resources/application.${env}.properties /opt/content-vault/application.properties
COPY target/content-vault-*.jar /opt/content-vault/content-vault.jar

RUN ln -sf /dev/stdout /opt/content-vault/logs/content-vault.sys.log
WORKDIR /opt/content-vault

CMD ["java", "-jar", "content-vault.jar", "--spring.config.additional-location=application.properties"]

COPY data/misc/*.* /opt/content-vault/data/misc/.

EXPOSE 8080
