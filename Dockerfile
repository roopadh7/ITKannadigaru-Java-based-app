FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . /app
# Copy SSL certificate for RDS connection
RUN mkdir -p /certs && cp /app/src/main/resources/certs/global-bundle.pem /certs/global-bundle.pem
EXPOSE 8080
CMD ["java","-jar","target/itkannadigaru-webapp-1.0.0.jar"]
