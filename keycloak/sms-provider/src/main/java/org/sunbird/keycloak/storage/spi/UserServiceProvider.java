package org.sunbird.keycloak.storage.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.sunbird.keycloak.utils.Constants;

public class UserServiceProvider
    implements UserStorageProvider, UserLookupProvider, UserQueryProvider {
  private static final Logger logger = Logger.getLogger(UserStorageProvider.class);

  public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";
  private final KeycloakSession session;
  private final ComponentModel model;
  private final UserService userService;

  public UserServiceProvider(KeycloakSession session, ComponentModel model,
      UserService userService) {
    this.session = session;
    this.model = model;
    this.userService = userService;
  }

  @Override
  public void close() {}

  @Override
  public UserModel getUserById(String id, RealmModel realm) {
    logger.info("UserServiceProvider:getUserById: id = " + id);
    String externalId = StorageId.externalId(id);
    return new UserAdapter(session, realm, model, userService.getById(externalId));
  }

  @Override
  public UserModel getUserByUsername(String username, RealmModel realm) {
    logger.info("UserServiceProvider: getUserByUsername called");
    long t1 = System.currentTimeMillis();
   
    List<User> users = userService.getByUsername(username);
    long t2 = System.currentTimeMillis();
    logger.info("UserServiceProvider: getUserByUsername for username "+ username +" TIME in ms: "+(t2-t1));
    if (users != null && users.size() == 1) {
   
      return new UserAdapter(session, realm, model, users.get(0));
    } else if (users != null && users.size() > 1) {
      throw new ModelDuplicateException(
          "Multiple users are associated with this login credentials.", "login credentials");
    } else {
      return null;
    }
  }

  @Override
  public UserModel getUserByEmail(String email, RealmModel realm) {
    logger.info("UserServiceProvider: getUserByEmail called");
    return getUserByUsername(email, realm);
  }

  @Override
  public int getUsersCount(RealmModel realm) {
    return 0;
  }

  @Override
  public List<UserModel> getUsers(RealmModel realm) {
    return Collections.emptyList();
  }

  @Override
  public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
    return Collections.emptyList();
  }

  @Override
  public List<UserModel> searchForUser(String search, RealmModel realm) {
    logger.info("UserServiceProvider: searchForUser called");
    return userService.getByUsername(search).stream()
        .map(user -> new UserAdapter(session, realm, model, user)).collect(Collectors.toList());
  }

  @Override
  public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult,
      int maxResults) {
    logger.info("UserServiceProvider: searchForUser called with firstResult = " + firstResult);
    return searchForUser(search, realm);
  }

  @Override
  public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
    return Collections.emptyList();
  }

  @Override
  public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm,
      int firstResult, int maxResults) {

    return Collections.emptyList();
  }

  @Override
  public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult,
      int maxResults) {

    return Collections.emptyList();
  }

  @Override
  public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {

    return Collections.emptyList();
  }

  @Override
  public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue,
      RealmModel realm) {
    logger.info("UserServiceProvider: searchForUserByUserAttribute called");
    if (Constants.PHONE.equalsIgnoreCase(attrName)) {
      return userService.getByKey(attrName, attrValue).stream()
          .map(user -> new UserAdapter(session, realm, model, user)).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

}
