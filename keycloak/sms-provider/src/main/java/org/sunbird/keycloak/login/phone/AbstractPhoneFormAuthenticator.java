/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates and other contributors as indicated by
 * the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.sunbird.keycloak.login.phone;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.sunbird.keycloak.resetcredential.sms.KeycloakSmsAuthenticatorConstants;
import org.sunbird.keycloak.utils.Constants;
import org.sunbird.keycloak.utils.SunbirdModelUtils;

public abstract class AbstractPhoneFormAuthenticator extends AbstractUsernameFormAuthenticator {

  private static final Logger logger = Logger.getLogger(AbstractPhoneFormAuthenticator.class);

  @Override
  public boolean validateUserAndPassword(AuthenticationFlowContext context,
      MultivaluedMap<String, String> inputData) {
    String username = inputData.getFirst(AuthenticationManager.FORM_USERNAME);
    logger.debug("AbstractPhoneFormAuthenticator@validateUserAndPassword - Username -" + username);

    if (username == null) {
      context.getEvent().error(Errors.USER_NOT_FOUND);
      Response challengeResponse = invalidUser(context);
      context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
      return false;
    }

    // remove leading and trailing whitespace
    username = username.trim();

    context.getEvent().detail(Details.USERNAME, username);
    context.getAuthenticationSession()
        .setAuthNote(AbstractPhoneFormAuthenticator.ATTEMPTED_USERNAME, username);

    UserModel user = null;
    try {
      
      user = SunbirdModelUtils.getUserByNameEmailOrPhone(context, username);
      
    } catch (ModelDuplicateException mde) {
      ServicesLogger.LOGGER.modelDuplicateException(mde);

      // Could happen during federation import
      if (mde.getDuplicateFieldName() != null
          && mde.getDuplicateFieldName().equals(UserModel.EMAIL)) {
        setDuplicateUserChallenge(context, Errors.EMAIL_IN_USE, Messages.EMAIL_EXISTS,
            AuthenticationFlowError.USER_CONFLICT);
      } else if (mde.getDuplicateFieldName() != null
          && mde.getDuplicateFieldName().equals(UserModel.USERNAME)) {
        setDuplicateUserChallenge(context, Errors.USERNAME_IN_USE, Messages.USERNAME_EXISTS,
            AuthenticationFlowError.USER_CONFLICT);
      } else if (mde.getDuplicateFieldName() != null
          && mde.getDuplicateFieldName().equals(KeycloakSmsAuthenticatorConstants.ATTR_MOBILE)) {
        setDuplicateUserChallenge(context, Constants.MULTIPLE_USER_ASSOCIATED_WITH_PHONE,
            Constants.MULTIPLE_USER_ASSOCIATED_WITH_PHONE, AuthenticationFlowError.USER_CONFLICT);
      }

      return false;
    }

    if (invalidUser(context, user)) {
      return false;
    }

    if (!validatePassword(context, user, inputData)) {
      return false;
    }

    if (!enabledUser(context, user)) {
      return false;
    }

    String rememberMe = inputData.getFirst("rememberMe");
    boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");
    if (remember) {
      context.getAuthenticationSession().setAuthNote(Details.REMEMBER_ME, "true");
      context.getEvent().detail(Details.REMEMBER_ME, "true");
    } else {
      context.getAuthenticationSession().removeAuthNote(Details.REMEMBER_ME);
    }
    context.setUser(user);
    return true;
  }
  
  protected Response temporarilyDisabledUser(AuthenticationFlowContext context) {
      return context.form()
              .setError(Messages.ACCOUNT_TEMPORARILY_DISABLED).createLogin();
  }

}
