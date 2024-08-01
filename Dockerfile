# Use a Java base image
FROM openjdk:17-jdk-buster

RUN set -x \
    && apt-get update \
    \
    #: force installing jing due to different issues regarding to java runtime environment \
    #: setup issues (see: https://stackoverflow.com/q/76872534/12171959)
    && apt-get install jing -y 
RUN apt-get update && apt-get install -y default-mysql-client ant jq build-essential libffi-dev ruby ruby-bundler dos2unix coreutils curl tzdata git gcc cmake libpng-dev graphviz wkhtmltopdf pandoc rsync poppler-utils pkg-config
RUN curl https://sh.rustup.rs -sSf | sh -s -- -y
ENV PATH="/root/.cargo/bin:${PATH}"
RUN git clone --recursive https://github.com/kornelski/pngquant.git
RUN cd pngquant && cargo build --release
RUN apt-get update && apt-get install -y texlive-xetex
RUN gem install docsplit 
ENV TZ=Europe/Berlin
RUN apt-get install -y file

RUN apt-get install -y vim

# Set the working directory 
COPY . /src
WORKDIR /src

RUN git clone https://gitlab.com/Tech4Comp/tmitocar-tools.git/ tmitocar
RUN chmod -R 777 tmitocar
RUN dos2unix tmitocar/tmitocar.sh
RUN dos2unix tmitocar/feedback.sh
RUN dos2unix /src/gradle.properties
RUN dos2unix gradlew
RUN chmod +x gradlew && ./gradlew build --exclude-task test

# Copy the Spring Boot application JAR file into the Docker image
# COPY --from=builder /tmitocar-service/build/libs/*.jar /src/services.openAIService-2.0.0.jar

# Set environment variables
ENV SERVER_PORT=8080
ENV ISSUER_URI=https://auth.las2peer.org/auth/realms/main 
ENV SET_URI=https://auth.las2peer.org/auth/realms/main/protocol/openid-connect/certs
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mentoring-workbench
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=
ENV SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/
ENV SPRING_DATA_MONGODB_DATABASE=
ENV SPRING_DATA_MONGODB_USERNAME=
ENV SPRING_DATA_MONGODB_PASSWORD=

# Expose the port that the Spring Boot application is listening on
EXPOSE 8080

# Entry point to run the Spring Boot application
ENTRYPOINT ["java","-jar","src/tmitocar-service/export/jars/services.tmitocar-3.0.0.jar", "--spring.security.oauth2.resourceserver.jwt.issuer-uri=${ISSUER_URI}", "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${SET_URI}"]