package org.sunbird.keycloak.storage.spi;

import java.util.ArrayList;
import java.util.Collections;
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

  private static DecryptionService decryptionService = new DefaultDecryptionServiceImpl();
  
  public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel,
      User user) {
    super(session, realm, storageProviderModel);
    logger.info("UserAdapter:UserAdapter constructor called");
    this.user = user;
    logger.info("UserAdapter:StorageId.keycloakId method called to get keycloakId started");
    this.keycloakId = StorageId.keycloakId(storageProviderModel, user.getId());
    logger.info("UserAdapter:StorageId.keycloakId method called to get keycloakId completed");
  }

  @Override
  public String getUsername() {
    return decrypt(user.getUsername());
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
    return decrypt(user.getEmail());
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
     return Collections.emptyList();
  }
  
  @Override
  public Map<String, List<String>> getAttributes() {
    return Collections.emptyMap();
  }

  @Override
  public String getId() {
    return keycloakId;
  }
  
  private static String decrypt(String data) {
    return decryptionService.decryptData(data);
  }
}
