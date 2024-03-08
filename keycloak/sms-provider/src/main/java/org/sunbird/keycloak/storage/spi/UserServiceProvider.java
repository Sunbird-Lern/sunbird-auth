package org.sunbird.keycloak.storage.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  public UserModel getUserById(RealmModel realm, String id) {
    logger.info("UserServiceProvider:getUserById: id = " + id);
    String externalId = StorageId.externalId(id);
    logger.info("UserServiceProvider:getUserById: externalId found = " + externalId);
    return new UserAdapter(session, realm, model, userService.getById(externalId));
  }

  @Override
  public UserModel getUserByUsername(RealmModel realm, String username) {
    logger.info("UserServiceProvider: getUserByUsername called");
    List<User> users = userService.getByUsername(username);
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
  public UserModel getUserByEmail(RealmModel realm, String email) {
    logger.info("UserServiceProvider: getUserByEmail called");
    return getUserByUsername(realm, email);
  }

  @Override
  public int getUsersCount(RealmModel realm) {
    return 0;
  }

  /*@Override
  public List<UserModel> getUsers(RealmModel realm) {
    return Collections.emptyList();
  }*/

  /*@Override
  public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
    return Collections.emptyList();
  }*/

  @Override
  public Stream<UserModel> searchForUserStream(RealmModel realm, String search) {
    logger.info("UserServiceProvider: searchForUser called");
    return userService.getByUsername(search).stream().map(user -> new UserAdapter(session, realm, model, user));
  }

  @Override
  public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult,
      Integer maxResults) {
    logger.info("UserServiceProvider: searchForUser called with firstResult = " + firstResult);
    return searchForUserStream(realm, search);
  }

  @Override
  public Stream<UserModel> searchForUserStream(RealmModel realmModel, Map<String, String> params, Integer firstResult, Integer maxResults) {
    return Stream.empty();
  }

  @Override
  public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
    return Stream.empty();
  }

  @Override
  public Stream<UserModel> getGroupMembersStream(RealmModel realmModel, GroupModel groupModel, Integer firstResult,
                                                 Integer maxResults) {
    return Stream.empty();
  }

  @Override
  public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
    logger.info("UserServiceProvider: searchForUserByUserAttribute called");
    if (Constants.PHONE.equalsIgnoreCase(attrName)) {
      return userService.getByKey(attrName, attrValue).stream()
              .map(user -> new UserAdapter(session, realm, model, user));
    }
    return Stream.empty();
  }

}
