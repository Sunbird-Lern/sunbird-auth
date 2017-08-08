FROM jboss/base-jdk:8

ENV KEYCLOAK_VERSION 3.2.1.Final
# Enables signals getting passed from startup script to JVM
# ensuring clean shutdown when container is stopped.
ENV LAUNCH_JBOSS_IN_BACKGROUND 1
ENV PROXY_ADDRESS_FORWARDING true
USER root

RUN yum install -y epel-release && yum install -y jq && yum clean all

USER jboss

RUN cd /opt/jboss/ && curl -L https://downloads.jboss.org/keycloak/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz | tar zx && mv /opt/jboss/keycloak-$KEYCLOAK_VERSION /opt/jboss/keycloak

ADD docker-entrypoint.sh /opt/jboss/

COPY standalone.xml /opt/jboss/keycloak/standalone/configuration/

USER root
ADD setLogLevel.xsl /opt/jboss/keycloak/
RUN \
chown jboss:jboss /opt/jboss/keycloak/setLogLevel.xsl && \ 
java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/setLogLevel.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml

ENV JBOSS_HOME /opt/jboss/keycloak

#Enabling Proxy address forwarding so we can correctly handle SSL termination in front ends
#such as an OpenShift Router or Apache Proxy
RUN sed -i -e 's/<http-listener /& proxy-address-forwarding="${env.PROXY_ADDRESS_FORWARDING}" /' $JBOSS_HOME/standalone/configuration/standalone.xml
RUN sed -i -e 's/<http-listener /& proxy-address-forwarding="${env.PROXY_ADDRESS_FORWARDING}" /' $JBOSS_HOME/standalone/configuration/standalone-ha.xml

ENV DB_CONNECTOR_VERSION 9.4-1201-jdbc41

RUN mkdir -p /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main; cd /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main; curl -O http://central.maven.org/maven2/org/postgresql/postgresql/$DB_CONNECTOR_VERSION/postgresql-$DB_CONNECTOR_VERSION.jar
ADD module.xml /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main/

# setup SSL
#USER root
#ADD keycloak.jks $JBOSS_HOME/standalone/configuration/ 
#RUN chown jboss:jboss $JBOSS_HOME/standalone/configuration/keycloak.jks
#USER jboss
#RUN sed -i -e 's/<security-realms>/&\n            <security-realm name="UndertowRealm">\n                <server-identities>\n                    <ssl>\n                        <keystore path="keycloak.jks" relative-to="jboss.server.config.dir" keystore-password="secret" \/>\n                    <\/ssl>\n                <\/server-identities>\n            <\/security-realm>/' $JBOSS_HOME/standalone/configuration/standalone-ha.xml
#RUN sed -i -e 's/<server name="default-server">/&\n                <https-listener name="https" socket-binding="https" security-realm="UndertowRealm"\/>/' $JBOSS_HOME/standalone/configuration/standalone-ha.xml

EXPOSE 8080

ENTRYPOINT [ "/opt/jboss/docker-entrypoint.sh" ]

