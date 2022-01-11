# tmitocar dependencies (jq, ruby, coreutils)
FROM openjdk:17.0.1-jdk-buster
RUN apt-get update && apt-get install -y default-mysql-client ant jq build-essential libffi-dev ruby ruby-bundler dos2unix coreutils curl tzdata git gcc cmake libpng-dev graphviz wkhtmltopdf pandoc rsync poppler-utils

RUN git clone --recursive https://github.com/kornelski/pngquant.git
RUN cd pngquant && git checkout 22918da1f4ae56b4300dfb30137f0f29cc8a0866 && ./configure && make install
RUN apt-get update && apt-get install -y texlive-xetex
RUN gem install docsplit 
ENV TZ=Europe/Berlin
RUN apt-get install -y file

RUN apt-get install -y vim

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