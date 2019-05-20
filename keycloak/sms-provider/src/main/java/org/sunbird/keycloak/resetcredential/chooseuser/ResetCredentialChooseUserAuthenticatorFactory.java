package org.sunbird.keycloak.resetcredential.chooseuser;

import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * 
 * @author Amit Kumar
 *
 */
public class ResetCredentialChooseUserAuthenticatorFactory
    implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

  public static final String PROVIDER_ID = "reset-credentials-choose-user";

  private static Logger logger =
      Logger.getLogger(ResetCredentialChooseUserAuthenticatorFactory.class);
  private static final ResetCredentialChooseUserAuthenticator SINGLETON =
      new ResetCredentialChooseUserAuthenticator();

  protected static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES =
      {AuthenticationExecutionModel.Requirement.REQUIRED,
          AuthenticationExecutionModel.Requirement.OPTIONAL,
          AuthenticationExecutionModel.Requirement.DISABLED};

  @Override
  public Authenticator create(KeycloakSession session) {
    logger.debug("create called ... returning " + SINGLETON);
    return SINGLETON;
  }


  @Override
  public void init(Scope config) {
    logger.debug("ResetCredentialChooseUserAuthenticatorFactory init called ... ");
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    logger.debug("ResetCredentialChooseUserAuthenticatorFactory postInit called ... ");
  }

  @Override
  public void close() {
    logger.debug("ResetCredentialChooseUserAuthenticatorFactory close called ... ");
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public String getHelpText() {
    return "Reset Credential Choose User";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return null;
  }

  @Override
  public String getDisplayType() {
    return "Reset Credential Choose User";
  }

  @Override
  public String getReferenceCategory() {
    return null;
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public Requirement[] getRequirementChoices() {
    logger.debug("getRequirementChoices called ... returning " + REQUIREMENT_CHOICES);
    return REQUIREMENT_CHOICES;
  }

  @Override
  public boolean isUserSetupAllowed() {
    return true;
  }

}
