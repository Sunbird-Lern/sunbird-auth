package org.sunbird.keycloak.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
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
public class SunbirdModelUtils {

  private static Logger logger = Logger.getLogger(SunbirdModelUtils.class);

  private SunbirdModelUtils() {}

  public static UserModel getUserByNameEmailOrPhone(AuthenticationFlowContext context,
      String username) {
    String numberRegex = "\\d+";
    KeycloakSession session = context.getSession();
    logger.info("SunbirdModelUtils@getUser " + username);
    if (username.matches(numberRegex)) {
        Stream<UserModel> userModels = session.users().searchForUserByUserAttributeStream(
              context.getRealm(), KeycloakSmsAuthenticatorConstants.ATTR_MOBILE, username);
        //try the whole thing with streams if possible
        List<UserModel> userModelsList = userModels.collect(Collectors.toList());
      if (userModelsList != null && !userModelsList.isEmpty()) {
        // multiple user found for same attribute
    	for(UserModel model : userModelsList) {
      		logger.info("SunbirdModelUtils@getUser userModel id=" + model.getId()+", userName=" + model.getUsername()+", firstName"+model.getFirstName());
      	}  
    	if (userModelsList.size() > 1) {
    		List<UserModel> filtered = new ArrayList<>();
    		Set<String> ids = new HashSet<>();
            userModelsList.forEach(model->{
    			if(model.getId().startsWith("f:") && ids.add(model.getId())) {
    				filtered.add(model);
    			}
    		});
            userModelsList = filtered;
    	}
    	logger.info("SunbirdModelUtils@getUser user model size "+userModelsList.size());
    	if (userModelsList.size() > 1) {
          throw new ModelDuplicateException(Constants.MULTIPLE_USER_ASSOCIATED_WITH_PHONE,
              KeycloakSmsAuthenticatorConstants.ATTR_MOBILE);
        }
        return userModelsList.get(0);
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
