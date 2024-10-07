FROM amazoncorretto:21
LABEL authors="Piotrek"
ADD target/gateway-0.0.1-SNAPSHOT.jar gateway-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "gateway-0.0.1-SNAPSHOT.jar"]