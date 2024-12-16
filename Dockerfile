FROM openjdk:21-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the .jar file and dependencies into the container
COPY ./target/EventRouter-1.0-SNAPSHOT.jar /app/EventRouter-1.0-SNAPSHOT.jar
COPY target/lib /app/lib

# Set the command to run the Kafka test
CMD ["java", "-cp", "/app/EventRouter-1.0-SNAPSHOT.jar:/app/lib/*", "com.cwsoft.messaging.kafka.test.KafkaIntegrationTest" ]