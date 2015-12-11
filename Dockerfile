FROM java:8-jdk

ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH
RUN mkdir -p "$CATALINA_HOME"
WORKDIR $CATALINA_HOME

ENV TOMCAT_MAJOR 8
ENV TOMCAT_VERSION 8.0.28
ENV TOMCAT_TGZ_URL https://archive.apache.org/dist/tomcat/tomcat-$TOMCAT_MAJOR/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz

RUN set -x \
	&& curl -fSL "$TOMCAT_TGZ_URL" -o tomcat.tar.gz \
	&& tar -xvf tomcat.tar.gz --strip-components=1 \
	&& rm bin/*.bat \
	&& rm tomcat.tar.gz*
	
# Install maven
RUN apt-get update  
RUN apt-get install -y maven

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
ENV PATH $JAVA_HOME/bin:$PATH

WORKDIR /tmp/address-search
ADD address-search/pom.xml pom.xml
RUN ["mvn", "dependency:resolve"]  

ADD address-search/src src/  
RUN ["mvn", "package"]

RUN cp -r target/address-search "$CATALINA_HOME/webapps/"
RUN rm -fr /tmp/address-search

EXPOSE 8080
WORKDIR $CATALINA_HOME
CMD ["catalina.sh", "run"]
