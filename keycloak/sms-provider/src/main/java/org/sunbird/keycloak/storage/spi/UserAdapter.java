package org.sunbird.keycloak.storage.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {

  private final User user;
  private final String keycloakId;

  public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel,
      User user) {
    super(session, realm, storageProviderModel);
    this.user = user;
    this.keycloakId = StorageId.keycloakId(storageProviderModel, user.getId());
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
  public String getId() {
    return keycloakId;
  }

}
