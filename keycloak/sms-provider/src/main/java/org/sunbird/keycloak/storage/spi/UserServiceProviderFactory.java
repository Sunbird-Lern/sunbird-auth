package org.sunbird.keycloak.storage.spi;

import java.util.List;
import org.keycloak.Config.Scope;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

public class UserServiceProviderFactory implements UserStorageProviderFactory<UserServiceProvider> {

  @Override
  public UserServiceProvider create(KeycloakSession session, ComponentModel model) {
    UserService userService = new UserService();
    return new UserServiceProvider(session, model, userService);
  }

  @Override
  public void init(Scope config) {
    config.get("host");
  }

  @Override
  public String getId() {
    return "cassandra-storage-provider";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return ProviderConfigurationBuilder.create().property().name("host").label("Host")
        .helpText("Cassandra DB host").type("String").defaultValue("localhost").add().build();
  }
}
