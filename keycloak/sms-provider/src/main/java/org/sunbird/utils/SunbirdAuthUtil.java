package org.sunbird.utils;

import java.util.ArrayList;
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
 *         Utility class for keycloak, it contains all the common method used across the
 *         application.
 * 
 */
public class SunbirdAuthUtil {

  private static Logger logger = Logger.getLogger(SunbirdAuthUtil.class);

  private SunbirdAuthUtil() {}

  public static List<UserModel> getUser(AuthenticationFlowContext context, String username) {
    String numberRegex = "\\d+";
    KeycloakSession session = context.getSession();
    logger.debug("KeycloakUtil@getUser " + username);
    List<UserModel> userModels = new ArrayList<>();
    if (username.matches(numberRegex)) {
      userModels = session.users().searchForUserByUserAttribute(
          KeycloakSmsAuthenticatorConstants.ATTR_MOBILE, username, context.getRealm());
      if (userModels != null && !userModels.isEmpty()) {
          return userModels;
      } else {
        userModels = new ArrayList<>();
        userModels.add(KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(),
            username));
        return userModels;
      }
    } else {
      userModels.add(KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(),
          username));
      return userModels;
    }
  }

}
