package org.sunbird.utils;

import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.sunbird.keycloak.resetcredential.sms.KeycloakSmsAuthenticatorConstants;

/**
 * 
 * @author Amit Kumar
 * 
 * Utility class for keycloak, it contains all the 
 * common method used across the application.
 * 
 */
public class KeycloakUtil {

  private static Logger logger = Logger.getLogger(KeycloakUtil.class);

  private KeycloakUtil() {}

  public static UserModel getUser(AuthenticationFlowContext context, String username) {
    String numberRegex = "\\d+";
    KeycloakSession session = context.getSession();
    logger.debug("KeycloakUtil@getUser " + username);
    if (username.matches(numberRegex)) {
      List<UserModel> userModels = session.users().searchForUserByUserAttribute(
          KeycloakSmsAuthenticatorConstants.ATTR_MOBILE, username, context.getRealm());
      if (userModels != null && !userModels.isEmpty()) {
        return userModels.get(0);
      } else {
        return KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(),
            username);
      }
    } else {
      return KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(),
          username);
    }
  }

}
