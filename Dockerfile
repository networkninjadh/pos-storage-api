FROM adoptopenjdk/openjdk11:alpine-jre
CMD mnv clean package
COPY target/pos-storage-api-0.0.1-SNAPSHOT.jar pos-storage-api.jar
ENTRYPOINT ["java", "-jar", "pos-storage-api.jar"]
EXPOSE 8090