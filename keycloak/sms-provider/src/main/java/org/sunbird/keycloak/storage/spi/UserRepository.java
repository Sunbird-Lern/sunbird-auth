package org.sunbird.keycloak.storage.spi;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.keycloak.utils.Constants;

class UserRepository {

  private CassandraDbOperation cassandraOperation;

  public UserRepository() {
   CassandraConnection connection  = new CassandraConnection();
   String host = System.getenv(Constants.SUNBIRD_CASSANDRA_IP);
   String port = System.getenv(Constants.SUNBIRD_CASSANDRA_PORT);
   if(StringUtils.isBlank(host)){
     host = "localhost";
   }
   if(StringUtils.isBlank(port)){
     host = "9042";
   }
   connection.connect(host, Integer.parseInt(port));
   cassandraOperation = new CassandraDbOperation(connection);
  }

  public int getUsersCount() {
    return 0;
  }

  public User findUserById(String id) {
    return cassandraOperation.getUserById(id);
  }

  public User findUserByUsernameOrEmail(String username) {
    return cassandraOperation.getUserByName(username);
  }

  public List<User> findUsers(String query) {
    return Arrays.asList(cassandraOperation.getUserByName(query));
  }
}
