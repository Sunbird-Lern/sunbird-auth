package org.sunbird.keycloak.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Access;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keycloak.utils.Constants;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RequiredActionLinkProviderFactory.class, KeycloakSession.class,
    KeycloakContext.class, KeycloakModelUtils.class, RealmModel.class, RedirectUtils.class,
    AppAuthManager.class, RequiredActionLinkProvider.class, UriInfo.class, AccessToken.class,
    Access.class, AuthResult.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
//@Ignore
public class RequiredActionLinkProviderTest {

  private static KeycloakSession session = null;
  private static KeycloakContext context = null;
  private static RealmModel model = null;
  private static AppAuthManager authMangr = null;
  private static AuthResult authResult = null;
  private static UserModel userModel = null;
  private static ClientModel client = null;
  private static Map<String, String> request = new HashMap<>();


  @BeforeClass
  public static void setUp() throws Exception {
    request.put(Constants.REDIRECT_URI, "/login");
    request.put(Constants.CLIENT_ID, "master");
    request.put(Constants.REQUIRED_ACTION, "UPDATE_PASSWORD");
    request.put(Constants.USERNAME, "amit");

    session = PowerMockito.mock(KeycloakSession.class);
    context = PowerMockito.mock(KeycloakContext.class);
    model = PowerMockito.mock(RealmModel.class);
    authMangr = PowerMockito.mock(AppAuthManager.class);
    authResult = PowerMockito.mock(AuthResult.class);
    userModel = PowerMockito.mock(UserModel.class);
    client = PowerMockito.mock(ClientModel.class);
    PowerMockito.when(session.getContext()).thenReturn(context);
    PowerMockito.when(session.getContext().getRealm()).thenReturn(model);
  }

  @Test
  public void checkRealmAdminAccessForUnAuthorized() throws Exception {
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(null);
    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(
        ErrorResponse.error(Constants.ERROR_NOT_AUTHORIZED, Status.UNAUTHORIZED));

    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Test
  public void checkRealmAdminAccessForForbidden() throws Exception {
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(false);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(false);

    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(
        ErrorResponse.error(Constants.ERROR_REALM_ADMIN_ROLE_ACCESS, Status.FORBIDDEN));

    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Test
  public void usernameMandatoryCheck() throws Exception {
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(true);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(true);

    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    String userName = null;
    WebApplicationException expectedException = new WebApplicationException(ErrorResponse.error(
        MessageFormat.format(Constants.ERROR_MANDATORY_PARAM_MISSING, userName, Constants.USERNAME),
        Status.BAD_REQUEST));
    request.put("userName", userName);
    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Test
  public void invalidUserNameCheck() throws Exception {
    String userName = "amit";
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(true);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(true);
    PowerMockito.mockStatic(KeycloakModelUtils.class);
    PowerMockito.when(KeycloakModelUtils.findUserByNameOrEmail(session, model, userName))
        .thenReturn(null);
    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(ErrorResponse.error(
        MessageFormat.format(Constants.ERROR_INVALID_PARAMETER_VALUE, userName, Constants.USERNAME),
        Status.BAD_REQUEST));

    request.put("userName", userName);
    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Test
  public void userEnabilityCheck() throws Exception {
    String userName = "amit";
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(true);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(true);
    PowerMockito.mockStatic(KeycloakModelUtils.class);
    PowerMockito.when(KeycloakModelUtils.findUserByNameOrEmail(session, model, userName))
        .thenReturn(userModel);
    PowerMockito.when(userModel.isEnabled()).thenReturn(false);

    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(
        ErrorResponse.error(Constants.ERROR_USER_IS_DISABLED, Status.BAD_REQUEST));

    request.put("userName", userName);
    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Test
  public void clientIdMandatoryCheck() throws Exception {
    String userName = "amit";
    String clientId = null;
    request.put("clientId", clientId);
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(true);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(true);
    PowerMockito.mockStatic(KeycloakModelUtils.class);
    PowerMockito.when(KeycloakModelUtils.findUserByNameOrEmail(session, model, userName))
        .thenReturn(userModel);
    PowerMockito.when(userModel.isEnabled()).thenReturn(true);
    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(
        ErrorResponse.error(MessageFormat.format(Constants.ERROR_MANDATORY_PARAM_MISSING, clientId,
            Constants.CLIENT_ID), Status.BAD_REQUEST));
    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Ignore
  @Test
  public void invalidClientIdCheck() throws Exception {
    String userName = "amit";
    String clientId = "master1";
    request.put("clientId", clientId);
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(true);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(true);
    PowerMockito.mockStatic(KeycloakModelUtils.class);
    PowerMockito.when(KeycloakModelUtils.findUserByNameOrEmail(session, model, userName))
        .thenReturn(userModel);
    PowerMockito.when(userModel.isEnabled()).thenReturn(true);
    PowerMockito.when(model.getClientByClientId(clientId)).thenReturn(null);
    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(
        ErrorResponse.error(MessageFormat.format(Constants.ERROR_INVALID_PARAMETER_VALUE, clientId,
            Constants.CLIENT_ID), Status.BAD_REQUEST));

    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Ignore
  @Test
  public void clientEnabilityCheck() throws Exception {
    String userName = "amit";
    String clientId = "master1";
    request.put("clientId", clientId);
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(true);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(true);
    PowerMockito.mockStatic(KeycloakModelUtils.class);
    PowerMockito.when(KeycloakModelUtils.findUserByNameOrEmail(session, model, userName))
        .thenReturn(userModel);
    PowerMockito.when(userModel.isEnabled()).thenReturn(true);
    PowerMockito.when(model.getClientByClientId(clientId)).thenReturn(client);
    PowerMockito.when(client.isEnabled()).thenReturn(false);
    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(
        ErrorResponse.error(clientId + Constants.ERROR_NOT_ENABLED, Status.BAD_REQUEST));

    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Ignore
  @Test
  public void verifyRedirectsUri() throws Exception {
    String userName = "amit";
    String clientId = "master1";
    String redirectUri = "/login";
    request.put("clientId", clientId);
    request.put("redirectUri", redirectUri);
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(true);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(true);
    PowerMockito.mockStatic(KeycloakModelUtils.class);
    PowerMockito.when(KeycloakModelUtils.findUserByNameOrEmail(session, model, userName))
        .thenReturn(userModel);
    PowerMockito.when(userModel.isEnabled()).thenReturn(true);
    PowerMockito.when(model.getClientByClientId(clientId)).thenReturn(client);
    PowerMockito.when(client.isEnabled()).thenReturn(true);
    PowerMockito.mockStatic(RedirectUtils.class);
    PowerMockito.when(RedirectUtils.verifyRedirectUri(session.getContext().getUri(), redirectUri,
        session.getContext().getRealm(), client)).thenReturn(null);
    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(
        ErrorResponse.error(MessageFormat.format(Constants.ERROR_INVALID_PARAMETER_VALUE,
            redirectUri, Constants.REDIRECT_URI), Status.BAD_REQUEST));

    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Test
  public void validateRequiredAction() throws Exception {
    String userName = "amit";
    String clientId = "master1";
    String redirectUri = "/login";
    request.put("clientId", clientId);
    request.put("redirectUri", redirectUri);
    String actionName = null;
    request.put("requiredAction", actionName);
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(true);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(true);
    PowerMockito.mockStatic(KeycloakModelUtils.class);
    PowerMockito.when(KeycloakModelUtils.findUserByNameOrEmail(session, model, userName))
        .thenReturn(userModel);
    PowerMockito.when(userModel.isEnabled()).thenReturn(true);
    PowerMockito.when(model.getClientByClientId(clientId)).thenReturn(client);
    PowerMockito.when(client.isEnabled()).thenReturn(true);
    PowerMockito.mockStatic(RedirectUtils.class);
    PowerMockito.when(RedirectUtils.verifyRedirectUri(session.getContext().getUri(), redirectUri,
        session.getContext().getRealm(), client)).thenReturn("/login");
    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(
        ErrorResponse.error(MessageFormat.format(Constants.ERROR_MANDATORY_PARAM_MISSING,
            actionName, Constants.REQUIRED_ACTION), Status.BAD_REQUEST));

    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }

  @Ignore
  @Test
  public void invalidRequiredAction() throws Exception {
    String userName = "amit";
    String clientId = "master1";
    String redirectUri = "/login";
    request.put("clientId", clientId);
    request.put("redirectUri", redirectUri);
    String actionName = "INVALID_ACTION";
    request.put("requiredAction", actionName);
    PowerMockito.whenNew(AppAuthManager.class).withAnyArguments().thenReturn(authMangr);
    PowerMockito.when(authMangr.authenticateBearerToken(session, model)).thenReturn(authResult);
    AccessToken accessToken = PowerMockito.mock(AccessToken.class);
    PowerMockito.when(authResult.getToken()).thenReturn(accessToken);
    Access access = PowerMockito.mock(Access.class);
    PowerMockito.when(accessToken.getRealmAccess()).thenReturn(access);
    PowerMockito.when(access.isUserInRole(Constants.ADMIN)).thenReturn(true);
    PowerMockito.when(authResult.getToken().getRealmAccess().isUserInRole(Mockito.anyString()))
        .thenReturn(true);
    PowerMockito.mockStatic(KeycloakModelUtils.class);
    PowerMockito.when(KeycloakModelUtils.findUserByNameOrEmail(session, model, userName))
        .thenReturn(userModel);
    PowerMockito.when(userModel.isEnabled()).thenReturn(true);
    PowerMockito.when(model.getClientByClientId(clientId)).thenReturn(client);
    PowerMockito.when(client.isEnabled()).thenReturn(true);
    PowerMockito.mockStatic(RedirectUtils.class);
    PowerMockito.when(RedirectUtils.verifyRedirectUri(session.getContext().getUri(), redirectUri,
        session.getContext().getRealm(), client)).thenReturn("/login");
    RequiredActionLinkProvider provider = new RequiredActionLinkProvider(session);
    WebApplicationException expectedException = new WebApplicationException(
        ErrorResponse.error(MessageFormat.format(Constants.ERROR_INVALID_PARAMETER_VALUE,
            actionName, Constants.REQUIRED_ACTION), Status.BAD_REQUEST));

    try {
      Response response = provider.generateRequiredActionLink(request);
      assertTrue(response == null);
    } catch (WebApplicationException ex) {
      assertEquals(ex.getResponse().getStatus(), expectedException.getResponse().getStatus());
      assertEquals(((ErrorRepresentation) (ex.getResponse().getEntity())).getErrorMessage(),
          ((ErrorRepresentation) (expectedException.getResponse().getEntity())).getErrorMessage());
    }
  }
}
