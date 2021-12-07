FROM openjdk:17-jdk-alpine
#FROM openjdk:8-jdk
ENV LAS2PEER_PORT=9011

# tmitocar dependencies (jq, ruby, coreutils)
RUN apk add --update mysql-client bash apache-ant jq build-base libffi-dev ruby ruby-bundler dos2unix coreutils curl tzdata git gcc cmake libpng-dev graphviz wkhtmltopdf rsync poppler-utils

RUN git clone --recursive https://github.com/kornelski/pngquant.git
RUN cd pngquant && ./configure && make install
RUN apk add --update texlive-xetex
RUN gem install docsplit 
ENV TZ=Europe/Berlin
RUN apk add file

RUN apk add vim

COPY . /src
WORKDIR /src

#RUN wget https://github.com/jgm/pandoc/releases/download/2.5/pandoc-2.5-linux.tar.gz
#RUN tar -zxf pandoc-2.5-linux.tar.gz
#RUN cp -R pandoc-2.5/bin/* /usr/bin/
# && tar -zxf pandoc-2.9.2.1-linux-amd64.tar.gz 

RUN git clone https://gitlab.com/Tech4Comp/tmitocar-tools.git/ tmitocar

RUN chmod -R 777 tmitocar

RUN dos2unix tmitocar/tmitocar.sh

RUN dos2unix tmitocar/feedback.sh

RUN chmod +x /src/docker-entrypoint.sh
RUN dos2unix /src/docker-entrypoint.sh
RUN dos2unix /src/gradle.properties
RUN dos2unix gradlew
RUN chmod +x gradlew && ./gradlew build --exclude-task test

EXPOSE $LAS2PEER_PORT
ENTRYPOINT ["/src/docker-entrypoint.sh"]
