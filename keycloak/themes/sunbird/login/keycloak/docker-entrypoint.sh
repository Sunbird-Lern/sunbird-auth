#!/bin/bash

KEYCLOAK_USER=admin
KEYCLOAK_PASSWORD=ash12345678

if [ $KEYCLOAK_USER ] && [ $KEYCLOAK_PASSWORD ]; then
    keycloak/bin/add-user-keycloak.sh --user $KEYCLOAK_USER --password $KEYCLOAK_PASSWORD
fi

exec /opt/jboss/keycloak/bin/standalone.sh -b 0.0.0.0 -bprivate=$(hostname) --server-config standalone-ha.xml
exit $?
