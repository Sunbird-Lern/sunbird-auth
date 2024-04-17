FROM bitnami/keycloak:21.1.2
COPY keycloak21/sunbird /opt/bitnami/keycloak/themes/sunbird
WORKDIR /opt/bitnami/keycloak
COPY keycloak21/keycloak-email-phone-autthenticator-1.0-SNAPSHOT.jar /opt/bitnami/keycloak/providers
USER 1000
EXPOSE 8080
EXPOSE 8443


