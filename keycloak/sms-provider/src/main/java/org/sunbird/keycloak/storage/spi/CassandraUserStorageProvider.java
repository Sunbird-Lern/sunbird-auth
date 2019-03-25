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

public class CassandraUserStorageProvider
    implements UserStorageProvider, UserLookupProvider, UserQueryProvider {
  private static final Logger logger = Logger.getLogger(UserStorageProvider.class);

  public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";
  private final KeycloakSession session;
  private final ComponentModel model;
  private final UserRepository repository;

  public CassandraUserStorageProvider(
      KeycloakSession session, ComponentModel model, UserRepository repository) {
    this.session = session;
    this.model = model;
    this.repository = repository;
  }

  @Override
  public void close() {}

  @Override
  public UserModel getUserById(String id, RealmModel realm) { 
    String externalId = StorageId.externalId(id);
    logger.info("Get user by id " + id); 
    return new UserAdapter(session, realm, model, repository.findUserById(externalId));
  }

  @Override
  public UserModel getUserByUsername(String username, RealmModel realm) {
    logger.info("Get user by name " + username);
    List<User> users = repository.findUserByUsernameOrEmail(username);
    if (users != null && users.size() == 1) {
      return new UserAdapter(session, realm, model, users.get(0));
    } else {
      throw new ModelDuplicateException("Multiple users are associated with this login credentilas.",
          "login credentilas");
    }
  }

  @Override
  public UserModel getUserByEmail(String email, RealmModel realm) {
    logger.info("Get user by email " + email);
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
    logger.info("Search user, with search phrase: " + search);
    return repository
        .findUsers(search)
        .stream()
        .map(user -> new UserAdapter(session, realm, model, user))
        .collect(Collectors.toList());
  }

  @Override
  public List<UserModel> searchForUser(
      String search, RealmModel realm, int firstResult, int maxResults) {
    logger.info("Search user, with search phrase: " + search);
    return searchForUser(search, realm);
  }

  @Override
  public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
    return Collections.emptyList();
  }

  @Override
  public List<UserModel> searchForUser(
      Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {

    return Collections.emptyList();
  }

  @Override
  public List<UserModel> getGroupMembers(
      RealmModel realm, GroupModel group, int firstResult, int maxResults) {
 
    return Collections.emptyList();
  }

  @Override
  public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {

    return Collections.emptyList();
  }

  @Override
  public List<UserModel> searchForUserByUserAttribute(
      String attrName, String attrValue, RealmModel realm) {
    if(Constants.PHONE.equalsIgnoreCase(attrName)){
      return EsOperation.getUserByKey(attrName, attrValue).stream()
          .map(user -> new UserAdapter(session, realm, model, user))
          .collect(Collectors.toList());
    }
	return Collections.emptyList();
  }
}
