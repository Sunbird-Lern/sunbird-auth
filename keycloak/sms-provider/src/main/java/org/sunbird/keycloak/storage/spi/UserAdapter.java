package org.sunbird.keycloak.storage.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {
	private static final Logger logger = Logger.getLogger(UserAdapter.class);
  private final User user;
  private final String keycloakId;

  public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel,
      User user) {
    super(session, realm, storageProviderModel);
    this.user = user;
    this.keycloakId = StorageId.keycloakId(storageProviderModel, user.getId());
    logger.info("UserAdapter:StorageId.keycloakId method called to get keycloakId completed");
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  @Override
  public void setUsername(String username) {
    user.setUsername(username);
  }

  @Override
  public String getFirstName() {
    return user.getFirstName();
  }

  @Override
  public void setFirstName(String firstName) {
    user.setFirstName(firstName);
  }

  @Override
  public String getLastName() {
    return user.getLastName();
  }

  @Override
  public void setLastName(String lastName) {
    user.setLastName(lastName);
  }

  @Override
  public String getEmail() {
    return user.getEmail();
  }

  @Override
  public void setEmail(String email) {
    user.setEmail(email);
  }

  public String getPassword() {
    return user.getPassword();
  }

  public void setPassword(String password) {
    user.setPassword(password);
  }
  
  @Override
  public boolean isEnabled() {
      return user.isEnabled();
  }

  @Override
  public void setEnabled(boolean enabled) {
     user.setEnabled(enabled);
  }
  
  @Override
  public List<String> getAttribute(String name) {
    logger.info("UserAdapter:getAttribute method started " + name);
     List<String> list = getFederatedStorage().getAttributes(realm, keycloakId).get(name);
     list.forEach(e -> logger.info("UserAdapter:getAttribute attribute value: " + e));
     return list;
  }


  
  @Override
  public Map<String, List<String>> getAttributes() {
	logger.info("UserAdapter:getAttributes method started " );  
    Map<String, List<String>> attributes = new HashMap<>();
    List<String> phoneValues = new ArrayList<>();
    phoneValues.add(user.getPhone());
    attributes.put("phone", phoneValues);
    List<String> countrycodeValues = new ArrayList<>();
    countrycodeValues.add(user.getCountryCode());
    attributes.put("countryCode", countrycodeValues);
    logger.info("UserAdapter:getAttributes method ended " + attributes);
    return attributes;
  }

  @Override
  public String getId() {
    return keycloakId;
  }
}
