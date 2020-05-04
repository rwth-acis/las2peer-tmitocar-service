FROM openjdk:8-jdk-alpine

ENV LAS2PEER_PORT=9011

# tmitocar dependencies (jq, ruby, coreutils)
RUN apk add --update bash mysql-client apache-ant jq build-base libffi-dev ruby ruby-bundler dos2unix coreutils curl tzdata git gcc cmake libpng-dev graphviz wkhtmltopdf rsync && rm -f /var/cache/apk/*
RUN apk add pandoc --update-cache --repository http://dl-3.alpinelinux.org/alpine/edge/testing/ --allow-untrusted
RUN apk add --update git gcc cmake libpng-dev graphviz wkhtmltopdf && rm -f /var/cache/apk/*
RUN git clone --recursive https://github.com/kornelski/pngquant.git
RUN cd pngquant && ./configure && make install
RUN apk add --no-cache texlive-xetex librsvg

RUN gem install docsplit --no-rdoc --no-ri 
ENV TZ=Europe/Berlin

RUN addgroup -g 1000 -S las2peer && \
    adduser -u 1000 -S las2peer -G las2peer


COPY --chown=las2peer:las2peer . /src
WORKDIR /src

#RUN wget https://github.com/jgm/pandoc/releases/download/2.5/pandoc-2.5-linux.tar.gz
#RUN tar -zxf pandoc-2.5-linux.tar.gz
#RUN cp -R pandoc-2.5/bin/* /usr/bin/
# && tar -zxf pandoc-2.9.2.1-linux-amd64.tar.gz 

RUN git clone https://gitlab.com/Tech4Comp/tmitocar-tools.git tmitocar

RUN chmod -R 777 tmitocar

RUN dos2unix tmitocar/tmitocar.sh

RUN chmod +x /src/docker-entrypoint.sh
# run the rest as unprivileged user
USER las2peer
RUN ant jar startscripts

EXPOSE $LAS2PEER_PORT
ENTRYPOINT ["/src/docker-entrypoint.sh"]
