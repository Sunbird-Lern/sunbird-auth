package org.sunbird.keycloak.resetcredential.sms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.sunbird.keycloak.utils.Constants;
import org.sunbird.keycloak.utils.HttpClient;

/**
 * Created by joris on 11/11/2016.
 */
public class KeycloakSmsAuthenticator implements Authenticator {

    private static Logger logger = Logger.getLogger(KeycloakSmsAuthenticator.class);

    public static final String CREDENTIAL_TYPE = "sms_validation";

    private enum CODE_STATUS {
        VALID,
        INVALID,
        EXPIRED
    }


    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.debug("KeycloakSmsAuthenticator@authenticate called ... context = " + context);

        UserModel user = context.getUser();
        logger.debug("KeycloakSmsAuthenticator@authenticate - User = " + user.getUsername());

        List<String> mobileNumberCreds = user.getAttribute(KeycloakSmsAuthenticatorConstants.ATTR_MOBILE);

        String mobileNumber = null;
        String userEmail = user.getEmail();

        if (mobileNumberCreds != null && !mobileNumberCreds.isEmpty()) {
            mobileNumber = mobileNumberCreds.get(0);
        }

        Map<String, Object> otpResponse = generateOtp(context);
        if (StringUtils.isNotBlank(mobileNumber) || StringUtils.isNotBlank(userEmail)) {
          logger.debug("KeycloakSmsAuthenticator@authenticate - Sending SMS - " + mobileNumber);
          if (StringUtils.isNotBlank(mobileNumber)) {
            sendSMS(otpResponse, context, mobileNumber);
          }
          if (StringUtils.isNotBlank(userEmail)) {
            logger.debug(
                "KeycloakSmsAuthenticator@authenticate - Sending Email via sunbird - " + userEmail);
            logger.debug("KeycloakSmsAuthenticator@authenticate - realmName - "
                + context.getRealm().getDisplayName());
            sendEmailViaSunbird(otpResponse, context, userEmail);
          }
        } else {
          // The mobile number is NOT configured --> complain
          Response challenge = context.form().setError("Missing mobile number and email!")
              .createForm("sms-validation-error.ftl");
          context.failureChallenge(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED,
              challenge);
        }
    }

    private Map<String, Object> generateOtp(AuthenticationFlowContext context) {
      // The mobile number is configured --> send an SMS
      long nrOfDigits = KeycloakSmsAuthenticatorUtil.getConfigLong(context.getAuthenticatorConfig(),
          KeycloakSmsAuthenticatorConstants.CONF_PRP_SMS_CODE_LENGTH, 8L);
      logger.debug("Using nrOfDigits " + nrOfDigits);

      logger.debug("KeycloakSmsAuthenticator@sendSMS");

      long ttl = KeycloakSmsAuthenticatorUtil.getConfigLong(context.getAuthenticatorConfig(),
          KeycloakSmsAuthenticatorConstants.CONF_PRP_SMS_CODE_TTL, 10 * 60L); // 10 minutes in s

      logger.debug("Using ttl " + ttl + " (s)");
      String code = KeycloakSmsAuthenticatorUtil.getSmsCode(nrOfDigits);
      storeSMSCode(context, code, new Date().getTime() + (ttl * 1000)); // s --> ms
      Map<String, Object> response = new HashMap<>();
      response.put(Constants.OTP, code);
      response.put(Constants.TTL, (ttl / 60));
      return response;
    }
    
    private void sendSMS(Map<String, Object> otpResponse, AuthenticationFlowContext context,
        String mobileNumber) {
      if (KeycloakSmsAuthenticatorUtil.sendSmsCode(mobileNumber,
          (String) otpResponse.get(Constants.OTP), context.getAuthenticatorConfig())) {
        setEnterOTPPage(context, true);
      } else {
        setEnterOTPPage(context, false);
      }
    }

    private void setEnterOTPPage(AuthenticationFlowContext context, Boolean flag) {
      if (flag) {
        Response challenge = context.form().createForm("sms-validation.ftl");
        context.challenge(challenge);
      } else {
        Response challenge =
            context.form().setError("OTP could not be sent.").createForm("sms-validation-error.ftl");
        context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
      }
    }
    
    private void sendEmailViaSunbird(Map<String, Object> otpResponse,
        AuthenticationFlowContext context, String userEmail) {
      List<String> emails = new ArrayList<>(Arrays.asList(userEmail));
      otpResponse.put(Constants.RECIPIENT_EMAILS, emails);
      otpResponse.put(Constants.SUBJECT, Constants.MAIL_SUBJECT);
      otpResponse.put(Constants.REALM_NAME, context.getRealm().getDisplayName());
      otpResponse.put(Constants.EMAIL_TEMPLATE_TYPE, Constants.FORGOT_PASSWORD_EMAIL_TEMPLATE);
      otpResponse.put(Constants.BODY, Constants.BODY);

      logger.debug("KeycloakSmsAuthenticator@sendEmailViaSunbird - Sending Email - " + userEmail);
      Map<String, Object> request = new HashMap<>();
      request.put(Constants.REQUEST, otpResponse);

      HttpResponse response = HttpClient.post(request,
          (System.getenv(Constants.SUNBIRD_LMS_BASE_URL) + Constants.SEND_NOTIFICATION_URI),
          System.getenv(Constants.SUNBIRD_LMS_AUTHORIZATION));
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 200) {
        setEnterOTPPage(context, true);
      } else {
        setEnterOTPPage(context, false);
      }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.debug("action called ... context = " + context);
        logger.debug("KeycloakSmsAuthenticator@action called ... for User = " + context.getUser().getUsername());
        CODE_STATUS status = validateCode(context);
        Response challenge = null;
        switch (status) {
            case EXPIRED:
                logger.debug("KeycloakSmsAuthenticator@action - EXPIRED");
                challenge = context.form()
                        .setError("code is expired")
                        .createForm("sms-validation.ftl");
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
                break;

            case INVALID:
                logger.debug("KeycloakSmsAuthenticator@action - INVALID");

                if (context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.OPTIONAL ||
                        context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.ALTERNATIVE) {
                    logger.debug("KeycloakSmsAuthenticator@action - OPTIONAL || ALTERNATIVE");

                    logger.debug("Calling context.attempted()");
                    context.attempted();
                } else if (context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                    logger.debug("KeycloakSmsAuthenticator@action - INVALID_CREDENTIALS");

                    challenge = context.form()
                            .setError("Invalid code specified, please enter it again")
                            .createForm("sms-validation.ftl");
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
                } else {
                    // Something strange happened
                    logger.warn("Undefined execution ...");
                    logger.debug("KeycloakSmsAuthenticator@action - SOMETHING STRANGE HAPPENED!");
                    logger.debug("KeycloakSmsAuthenticator@action - " + context.getExecution().getRequirement());
                    logger.debug("KeycloakSmsAuthenticator@action - " + context.getExecution().getAuthenticator());
                    logger.debug("KeycloakSmsAuthenticator@action - " + context.getExecution().getAuthenticatorConfig());
                    logger.debug("KeycloakSmsAuthenticator@action - " + context.getExecution().getFlowId());
                    logger.debug("KeycloakSmsAuthenticator@action - " + context.getExecution().getId());
                    logger.debug("KeycloakSmsAuthenticator@action - " + context.getExecution().getParentFlow());
                    logger.debug("KeycloakSmsAuthenticator@action - " + context.getExecution().getPriority());
                }
                break;

            case VALID:
                context.success();
                break;

        }
    }
    
    // Store the code + expiration time in a UserCredential. Keycloak will persist these in the DB.
    // When the code is validated on another node (in a clustered environment) the other nodes have access to it's values too.
    private void storeSMSCode(AuthenticationFlowContext context, String code, Long expiringAt) {
        logger.debug("KeycloakSmsAuthenticator@storeSMSCode" + "User name = " + context.getUser().getUsername());

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(KeycloakSmsAuthenticatorConstants.USR_CRED_MDL_SMS_CODE);
        credentials.setValue(code);

        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);

        credentials.setType(KeycloakSmsAuthenticatorConstants.USR_CRED_MDL_SMS_EXP_TIME);
        credentials.setValue((expiringAt).toString());
        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);
    }


    protected CODE_STATUS validateCode(AuthenticationFlowContext context) {
        logger.debug("KeycloakSmsAuthenticator@validateCode");
        CODE_STATUS result = CODE_STATUS.INVALID;

        logger.debug("validateCode called ... ");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String enteredCode = formData.getFirst(KeycloakSmsAuthenticatorConstants.ANSW_SMS_CODE);
        KeycloakSession session = context.getSession();

        List codeCreds = session.userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsAuthenticatorConstants.USR_CRED_MDL_SMS_CODE);
        /*List timeCreds = session.userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsAuthenticatorConstants.USR_CRED_MDL_SMS_EXP_TIME);*/

        CredentialModel expectedCode = (CredentialModel) codeCreds.get(0);
        /*CredentialModel expTimeString = (CredentialModel) timeCreds.get(0);*/

        logger.debug("KeycloakSmsAuthenticator@validateCode " + "User name = " + context.getUser().getUsername());
        logger.debug("KeycloakSmsAuthenticator@validateCode " + "Expected code = " + expectedCode.getValue() + "    entered code = " + enteredCode);

        if (expectedCode != null) {
            result = enteredCode.equals(expectedCode.getValue()) ? CODE_STATUS.VALID : CODE_STATUS.INVALID;
        }
        logger.debug("result : " + result);

        logger.debug("KeycloakSmsAuthenticator@validateCode- Result -" + result);
        return result;
    }

    @Override
    public boolean requiresUser() {
        logger.debug("requiresUser called ... returning true");
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("KeycloakSmsAuthenticator@validateCode configuredFor called ... session=" + session + ", realm=" + realm + ", user=" + user);
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("KeycloakSmsAuthenticator@validateCode - setRequiredActions called ... session=" + session + ", realm=" + realm + ", user=" + user);
    }

    @Override
    public void close() {
        logger.debug("close called ...");
    }

}
