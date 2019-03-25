package org.sunbird.keycloak.storage.spi;

import java.util.List;
import org.keycloak.Config.Scope;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

public class CassandraUserStorgeProviderFactory
    implements UserStorageProviderFactory<CassandraUserStorageProvider> {

  @Override
  public CassandraUserStorageProvider create(KeycloakSession session, ComponentModel model) {
    UserRepository repository = new UserRepository();
    return new CassandraUserStorageProvider(session, model, repository);
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
    return ProviderConfigurationBuilder.create()
        .property()
        .name("host")
        .label("Host")
        .helpText("Cassandra DB host")
        .type("String")
        .defaultValue("localhost")
        .add()
        .build();
  }
}
