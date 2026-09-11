
# Stage 1: Get the official Keycloak image
FROM quay.io/keycloak/keycloak:21.1.2 AS source

# Stage 2: Create a patched base image using UBI 9 Minimal (pinned to a stable version)
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.4 AS patched_base
USER root
# Fix OS vulnerabilities (glibc, java, etc.) and install Java 17
RUN microdnf update -y && \
    microdnf install -y shadow-utils java-17-openjdk-headless && \
    groupadd -g 1000 keycloak && \
    useradd -u 1000 -g keycloak -m -d /opt/keycloak -s /sbin/nologin keycloak && \
    microdnf remove -y shadow-utils && \
    microdnf clean all

# Stage 3: Final Image
FROM patched_base
COPY --from=source --chown=keycloak:keycloak /opt/keycloak/ /opt/keycloak/

# Copy your local configurations/providers
COPY --chown=keycloak:keycloak ./conf/ /opt/keycloak/conf/
COPY --chown=keycloak:keycloak ./providers/ /opt/keycloak/providers/
COPY --chown=keycloak:keycloak ./themes/ /opt/keycloak/themes/
COPY --chown=keycloak:keycloak ./imports/ /opt/keycloak/imports/

USER keycloak

ENV KC_FEATURES=token-exchange
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
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