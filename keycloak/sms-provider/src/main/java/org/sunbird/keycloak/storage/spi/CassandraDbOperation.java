package org.sunbird.keycloak.storage.spi;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import java.util.Collections;
import java.util.List;
import org.jboss.logging.Logger;
import org.sunbird.keycloak.utils.Constants;

public class CassandraDbOperation {

  private static Logger logger = Logger.getLogger(CassandraDbOperation.class);
  
  private CassandraConnection connection;

  private DecryptionService decryptionService = new DefaultDecryptionServiceImpl();
  
  public CassandraDbOperation(CassandraConnection connection) {
    this.connection = connection;
  }

  public User getUserById(String id) {
    ResultSet rs =
        this.connection.getSession().execute("select * from sunbird.user where id = '" + id + "'");
    Row r = rs.one();
    User user = new User(r.getString(Constants.ID), r.getString(Constants.FIRST_NAME), "");
    String email = decrypt(r.getString(Constants.EMAIL));
    logger.info("getUserById user email : "+email);
    user.setEmail(email);
    String username = decrypt(r.getString(Constants.USERNAME.toLowerCase()));
    user.setUsername(username);
    String phone = decrypt(r.getString(Constants.PHONE));
    user.setPhone(phone);
    user.setLastName(r.getString(Constants.LAST_NAME.toLowerCase()));
    return user;
  }

  public List<User> getUserByName(String username) {
    List<User> users = null;
      String numberRegex = "\\d+";
      //mobile number length is of 10 digit
      if (username.matches(numberRegex) && 10 == username.length()) {
        users = getUserBy(Constants.PHONE, username);
        if (users != null) {
          return users;
        } else {
          return findUserByNameOrEmail(username);
        }
      } else {
        return findUserByNameOrEmail(username);
      }
  }

  private List<User> findUserByNameOrEmail(String username) {
    String emailRegex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    List<User> users = null;
    
    if (username.matches(emailRegex)) {
      users = getUserBy(Constants.EMAIL, username);
      if (users != null)
        return users;
    } else { 
      users = getUserBy(Constants.USERNAME, username); 
      if (users != null)
        return users;
    }
    return Collections.emptyList();
  }

  private String decrypt(String data) {
    return decryptionService.decryptData(data);
  }
  
  private List<User> getUserBy(String key, String searchValue) {
    logger.info("calling ES search api");
    return EsOperation.getUserByKey(key, searchValue);
  }
}
