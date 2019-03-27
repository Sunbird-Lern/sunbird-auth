package org.sunbird.keycloak.utils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CassandraConnection {

  private Cluster cluster;

  private Session session;

  private static CassandraConnection instance;

  public static CassandraConnection getInstance() {
    if (null == instance) {
      instance = new CassandraConnection();
      return instance;
    } else {
      return instance;
    }
  }

  private CassandraConnection() {
    String host = System.getenv(Constants.SUNBIRD_CASSANDRA_IP);
    List<String> hostList = Arrays.stream(host.split(",")).collect(Collectors.toList());
    if (hostList.isEmpty()) {
      hostList.add("localhost");
    }
    connect(hostList);
  }

  public void connect(List<String> nodeList) {
    cluster = Cluster.builder().addContactPoints(String.join(",", nodeList))
        .withRetryPolicy(DefaultRetryPolicy.INSTANCE).build();
    session = cluster.connect();
  }

  public Session getSession() {
    return this.session;
  }

  public void close() {
    session.close();
    cluster.close();
  }
}
