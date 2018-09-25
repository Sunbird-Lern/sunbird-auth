package org.sunbird.keycloak.rest;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.LoginActionsService;
import org.sunbird.keycloak.utils.Constants;

public class RequiredActionLinkProvider implements RealmResourceProvider {

  private static Logger logger = Logger.getLogger(RequiredActionLinkProvider.class);
  private KeycloakSession session;

  public RequiredActionLinkProvider(KeycloakSession session) {
    this.session = session;
  }

  /**
   * Generate user required action link. The supported required actions links are for update
   * password and verify email.
   *
   * @param request Request to generate required action link. The request contains following
   *        attributes: redirectUri: Redirect URI after performing required action clientId: Client
   *        ID requiredAction: Either UPDATE_PASSWORD or VERIFY_EMAIL userName: User name
   * 
   * @return Response containing generated required action link or error in case of failure.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response generateRequiredActionLink(Map<String, String> request) {
    logger.debug("RestResourceProvider:generateRequiredActionLink: called ");

    checkRealmAdminAccess();

    String redirectUri = request.get(Constants.REDIRECT_URI);
    String clientId = request.get(Constants.CLIENT_ID);
    String actionName = request.get(Constants.REQUIRED_ACTION);
    String userName = request.get(Constants.USERNAME);

    UserModel user = getEnabledUserByUsernameOrError(userName);
    ClientModel client = getClientByClientIdOrError(clientId);
    validateRedirectUri(redirectUri, client);

    int expirationInSecs = getExpirationInSecs(request.get(Constants.EXPIRATION_IN_SECS));
    int expiration = Time.currentTime() + expirationInSecs;

    List<String> requiredActionList = getRequiredActionListOrError(actionName);

    try {
      ExecuteActionsActionToken token = new ExecuteActionsActionToken(user.getId(), expiration,
          requiredActionList, redirectUri, clientId);
      UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
      builder.queryParam(Constants.KEY,
          token.serialize(session, session.getContext().getRealm(), session.getContext().getUri()));

      String link = builder.build(session.getContext().getRealm().getName()).toString();

      Map<String, Object> response = new HashMap<>();
      response.put(Constants.LINK, link);
      return Response.ok(response).build();
    } catch (Exception e) {
      return ErrorResponse.error(Constants.ERROR_CREATE_LINK, Status.INTERNAL_SERVER_ERROR);
    }
  }

  private UserModel getEnabledUserByUsernameOrError(String userName) {
    logger.debug("RestResourceProvider: getEnabledUserByUsernameOrError called");
    if (StringUtils.isBlank(userName)) {
      throw new WebApplicationException(
          ErrorResponse.error(MessageFormat.format(Constants.ERROR_MANDATORY_PARAM_MISSING,
              userName, Constants.USERNAME), Status.BAD_REQUEST));
    }
    UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session,
        session.getContext().getRealm(), userName);

    if (user == null) {
      throw new WebApplicationException(
          ErrorResponse.error(MessageFormat.format(Constants.ERROR_INVALID_PARAMETER_VALUE,
              userName, Constants.USERNAME), Status.BAD_REQUEST));
    }

    if (!user.isEnabled()) {
      throw new WebApplicationException(
          ErrorResponse.error(Constants.ERROR_USER_IS_DISABLED, Status.BAD_REQUEST));
    }

    return user;
  }

  private List<String> getRequiredActionListOrError(String actionName) {
    if (StringUtils.isBlank(actionName)) {
      throw new WebApplicationException(
          ErrorResponse.error(MessageFormat.format(Constants.ERROR_MANDATORY_PARAM_MISSING,
              actionName, Constants.REQUIRED_ACTION), Status.BAD_REQUEST));
    }

    List<String> requiredActionList = new ArrayList<>();
    switch (actionName) {
      case Constants.UPDATE_PASSWORD:
        requiredActionList.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        break;
      case Constants.VERIFY_EMAIL:
        requiredActionList.add(UserModel.RequiredAction.VERIFY_EMAIL.name());
        break;
      default:
        throw new WebApplicationException(
            ErrorResponse.error(MessageFormat.format(Constants.ERROR_INVALID_PARAMETER_VALUE,
                actionName, Constants.REQUIRED_ACTION), Status.BAD_REQUEST));
    }

    return requiredActionList;
  }

  private int getExpirationInSecs(String expirationInSecsStr) {
    int expirationInSecs;

    try {
      if (StringUtils.isNotBlank(expirationInSecsStr)) {
        expirationInSecs = Integer.parseInt(expirationInSecsStr);
      } else {
        expirationInSecs = Constants.DEFAULT_LINK_EXPIRATION_IN_SECS;
      }
    } catch (Exception ex) {
      throw new WebApplicationException(
          ErrorResponse.error(MessageFormat.format(Constants.ERROR_INVALID_PARAMETER_VALUE,
              expirationInSecsStr, Constants.EXPIRATION_IN_SECS), Status.BAD_REQUEST));
    }

    return expirationInSecs;
  }

  private void checkRealmAdminAccess() {
    logger.debug("RestResourceProvider: checkRealmAdminAccess called");

    AuthResult authResult =
        new AppAuthManager().authenticateBearerToken(session, session.getContext().getRealm());

    if (authResult == null) {
      throw new WebApplicationException(
          ErrorResponse.error(Constants.ERROR_NOT_AUTHORIZED, Status.UNAUTHORIZED));
    } else if (authResult.getToken().getRealmAccess() == null
        || !authResult.getToken().getRealmAccess().isUserInRole(Constants.ADMIN)) {
      throw new WebApplicationException(
          ErrorResponse.error(Constants.ERROR_REALM_ADMIN_ROLE_ACCESS, Status.FORBIDDEN));
    }
  }

  private void validateRedirectUri(String redirectUri, ClientModel client) {
    logger.debug("RestResourceProvider: validateRedirectUri called");
    if (StringUtils.isNotBlank(redirectUri)) {
      String redirect = RedirectUtils.verifyRedirectUri(session.getContext().getUri(), redirectUri,
          session.getContext().getRealm(), client);
      if (redirect == null) {
        throw new WebApplicationException(
            ErrorResponse.error(MessageFormat.format(Constants.ERROR_INVALID_PARAMETER_VALUE,
                redirectUri, Constants.REDIRECT_URI), Status.BAD_REQUEST));
      }
    }
  }

  private ClientModel getClientByClientIdOrError(String clientId) {
    logger.debug("RestResourceProvider: getClientByClientIdOrError called");
    if (StringUtils.isBlank(clientId)) {
      throw new WebApplicationException(
          ErrorResponse.error(MessageFormat.format(Constants.ERROR_MANDATORY_PARAM_MISSING,
              clientId, Constants.CLIENT_ID), Status.BAD_REQUEST));
    }
    ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
    if (client == null) {
      throw new WebApplicationException(
          ErrorResponse.error(MessageFormat.format(Constants.ERROR_INVALID_PARAMETER_VALUE,
              clientId, Constants.CLIENT_ID), Status.BAD_REQUEST));
    }
    if (!client.isEnabled()) {
      throw new WebApplicationException(
          ErrorResponse.error(clientId + Constants.ERROR_NOT_ENABLED, Status.BAD_REQUEST));
    }
    return client;
  }

  @Override
  public Object getResource() {
    return this;
  }

  @Override
  public void close() {

  }
}
