package org.sunbird.keycloak.storage.spi;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
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

  public User getUserByName(String username) {
      User user = null;
      String numberRegex = "\\d+";
      //mobile number length is of 10 digit
      if (username.matches(numberRegex) && 10 == username.length()) {
        user = getUserBy(Constants.PHONE, username);
        if (user != null) {
          return user;
        } else {
          return findUserByNameOrEmail(username);
        }
      } else {
        return findUserByNameOrEmail(username);
      }
  }

  private User findUserByNameOrEmail(String username) {
    String emailRegex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    User user = null;
    
    if (username.matches(emailRegex)) {
      user = getUserBy(Constants.EMAIL, username);
      if (user != null)
        return user;
    } else {
      user = getUserBy(Constants.USERNAME, username);
      if (user != null)
        return user;
    }
    return null;
  }

  private String decrypt(String data) {
    return decryptionService.decryptData(data);
  }
  
  private User getUserBy(String key, String searchValue) {
    logger.info("calling ES search api");
    return EsOperation.getUserByKey(key, searchValue);
  }
}
