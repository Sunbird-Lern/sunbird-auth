 
FROM quay.io/keycloak/keycloak:21.1.2 AS builder

ENV KC_FEATURES=token-exchange
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

 
FROM quay.io/keycloak/keycloak:21.1.2
COPY --from=builder /opt/keycloak/ /opt/keycloak/


COPY ./conf/ /opt/keycloak/conf/
COPY ./providers/ /opt/keycloak/providers/
COPY ./themes/ /opt/keycloak/themes/
COPY ./imports/ /opt/keycloak/imports/

 
USER root
RUN chown -R keycloak:keycloak /opt/keycloak
USER keycloak

 
ENV KC_PROXY=edge
ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_HTTPS=false
ENV KC_HTTP_RELATIVE_PATH=/auth
ENV KC_SPI_LOGIN_PROTOCOL_OPENID_CONNECT_LEGACY_LOGOUT_REDIRECT_URI=true

RUN /opt/keycloak/bin/kc.sh build

EXPOSE 8080

WORKDIR /opt/keycloak

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--import-realm", "--spi-connections-jpa-legacy-migration-strategy=update", "--spi-login-protocol-openid-connect-suppress-logout-confirmation-screen=true"]