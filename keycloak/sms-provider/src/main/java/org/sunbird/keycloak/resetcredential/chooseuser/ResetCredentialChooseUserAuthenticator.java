package org.sunbird.keycloak.resetcredential.chooseuser;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.sunbird.keycloak.resetcredential.sms.KeycloakSmsAuthenticator;
import org.sunbird.utils.KeycloakUtil;


/**
 * 
 * @author Amit Kumar
 *
 * This class will override the choose user action for reset credential flow under
 * Authentication. Here we are overriding action method for getting user details by
 * attributes (i.e phone)
 */
public class ResetCredentialChooseUserAuthenticator implements Authenticator {


  private static Logger logger = Logger.getLogger(KeycloakSmsAuthenticator.class);
  public static final String PROVIDER_ID = "reset-credentials-choose-user";


  @Override
  public void authenticate(AuthenticationFlowContext context) {

    String existingUserId =
        context.getAuthenticationSession().getAuthNote(AbstractIdpAuthenticator.EXISTING_USER_INFO);
    if (existingUserId != null) {
      UserModel existingUser = AbstractIdpAuthenticator.getExistingUser(context.getSession(),
          context.getRealm(), context.getAuthenticationSession());

      logger.debugf(
          "Forget-password triggered when reauthenticating user after first broker login. Skipping reset-credential-choose-user screen and using user '%s' ",
          existingUser.getUsername());
      context.setUser(existingUser);
      context.success();
      return;
    }

    String actionTokenUserId =
        context.getAuthenticationSession().getAuthNote(DefaultActionTokenKey.ACTION_TOKEN_USER_ID);
    if (actionTokenUserId != null) {
      UserModel existingUser =
          context.getSession().users().getUserById(actionTokenUserId, context.getRealm());

      // Action token logics handles checks for user ID validity and user being enabled

      logger.debugf(
          "Forget-password triggered when reauthenticating user after authentication via action token. Skipping reset-credential-choose-user screen and using user '%s' ",
          existingUser.getUsername());
      context.setUser(existingUser);
      context.success();
      return;
    }

    Response challenge = context.form().createPasswordReset();
    context.challenge(challenge);

  }

  @Override
  public void action(AuthenticationFlowContext context) {
    EventBuilder event = context.getEvent();
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
    String username = formData.getFirst("username");
    if (username == null || username.isEmpty()) {
      event.error(Errors.USERNAME_MISSING);
      Response challenge = context.form().setError(Messages.MISSING_USERNAME).createPasswordReset();
      context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
      return;
    }
    UserModel user = KeycloakUtil.getUser(context, username);

    context.getAuthenticationSession()
        .setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

    // we don't want people guessing usernames, so if there is a problem, just continue, but don't
    // set the user
    // a null user will notify further executions, that this was a failure.
    if (user == null) {
      event.clone().detail(Details.USERNAME, username).error(Errors.USER_NOT_FOUND);
    } else if (!user.isEnabled()) {
      event.clone().detail(Details.USERNAME, username).user(user).error(Errors.USER_DISABLED);
    } else {
      context.setUser(user);
    }

    context.success();

  }

  @Override
  public void close() {
    logger.debug("ResetCredentialChooseUserAuthenticator close called ... ");
  }

  @Override
  public boolean requiresUser() {
    return false;
  }

  @Override
  public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
    return true;
  }

  @Override
  public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    logger.debug("ResetCredentialChooseUserAuthenticator setRequiredActions called ... ");
  }

}
