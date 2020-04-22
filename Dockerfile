# Docker for Content Vault microserivce 
FROM openjdk:11
ARG env
# local file storage path
RUN mkdir -p /opt/content-vault/data
RUN mkdir -p /opt/content-vault/log
COPY src/main/resources/application.${env}.properties /opt/content-vault/application.properties
COPY target/content-vault-*.jar /opt/content-vault/content-vault.jar
RUN ln -sf /dev/stdout /opt/content-vault/log/content-vault.sys.log

CMD ["java", "-jar", "/opt/content-vault/content-vault.jar", "--spring.config.additional-location=/opt/content-vault/application.properties"]

EXPOSE 8080
