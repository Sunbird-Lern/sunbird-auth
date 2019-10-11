FROM jboss/keycloak-ha-postgres

ADD docker-entrypoint.sh /opt/jboss/

EXPOSE 8080

ENTRYPOINT [ "/opt/jboss/docker-entrypoint.sh" ]

CMD ["-b", "0.0.0.0", "--server-config", "standalone-ha.xml"]
