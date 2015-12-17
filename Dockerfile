FROM centos:centos7
RUN /usr/bin/yum -y update

RUN /usr/bin/yum install -y java-1.8.0-openjdk-devel.x86_64
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk
ENV PATH $JAVA_HOME/bin:$PATH

RUN /usr/bin/yum install -y apache php
COPY src/ /var/www/html/
COPY conf.d/ /etc/httpd/conf.d/

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
RUN /usr/bin/yum install -y maven

WORKDIR /tmp/address-search
ADD address-search/pom.xml pom.xml
RUN ["mvn", "dependency:resolve"]  

ADD address-search/src src/  
RUN ["mvn", "package"]

RUN cp -r target/address-search "$CATALINA_HOME/webapps/"
RUN rm -fr /tmp/address-search

RUN /usr/bin/yum clean all

EXPOSE 80
ADD bin/start-address-search.sh /usr/local/bin/start-address-search.sh
RUN chmod +x /usr/local/bin/start-address-search.sh
CMD ["/usr/local/bin/start-address-search.sh"]
