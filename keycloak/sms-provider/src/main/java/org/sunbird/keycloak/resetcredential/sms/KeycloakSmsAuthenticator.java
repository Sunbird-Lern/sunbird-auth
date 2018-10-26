package org.sunbird.keycloak.resetcredential.sms;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

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

        if (StringUtils.isNotBlank(mobileNumber) || StringUtils.isNotBlank(userEmail)) {
          if (StringUtils.isNotBlank(userEmail)) {
            logger.debug("KeycloakSmsAuthenticator@authenticate - Sending Email - " + userEmail);
            sendEmail(context);
          }
          if (StringUtils.isNotBlank(mobileNumber)) {
            logger.debug("KeycloakSmsAuthenticator@authenticate - Sending SMS - " + mobileNumber);
            sendSMS(context, mobileNumber);
          }
        } else {
          // The mobile number is NOT configured --> complain
          Response challenge = context.form().setError("Missing mobile number and email!")
              .createForm("sms-validation-error.ftl");
          context.failureChallenge(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED,
              challenge);
        }
    }

    private void sendSMS(AuthenticationFlowContext context, String mobileNumber) {
        // The mobile number is configured --> send an SMS
        long nrOfDigits = KeycloakSmsAuthenticatorUtil.getConfigLong(context.getAuthenticatorConfig(), KeycloakSmsAuthenticatorConstants.CONF_PRP_SMS_CODE_LENGTH, 8L);
        logger.debug("Using nrOfDigits " + nrOfDigits);

        logger.debug("KeycloakSmsAuthenticator@sendSMS");

        long ttl = KeycloakSmsAuthenticatorUtil.getConfigLong(context.getAuthenticatorConfig(), KeycloakSmsAuthenticatorConstants.CONF_PRP_SMS_CODE_TTL, 10 * 60L); // 10 minutes in s

        logger.debug("Using ttl " + ttl + " (s)");

        String code = KeycloakSmsAuthenticatorUtil.getSmsCode(nrOfDigits);

        storeSMSCode(context, code, new Date().getTime() + (ttl * 1000)); // s --> ms
        if (KeycloakSmsAuthenticatorUtil.sendSmsCode(mobileNumber, code, context.getAuthenticatorConfig())) {
            Response challenge = context.form().createForm("sms-validation.ftl");
            context.challenge(challenge);
        } else {
            Response challenge = context.form()
                    .setError("SMS could not be sent.")
                    .createForm("sms-validation-error.ftl");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }
    }

    private void sendEmail(AuthenticationFlowContext context) {
        logger.debug("KeycloakSmsAuthenticator@sendEmail");

        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        String username = authenticationSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);

        // we don't want people guessing usernames, so if there was a problem obtaining the user, the user will be null.
        // just reset login for with a success message
        if (user == null) {
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return;
        }

        String actionTokenUserId = authenticationSession.getAuthNote(DefaultActionTokenKey.ACTION_TOKEN_USER_ID);
        authenticationSession.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        if (actionTokenUserId != null && Objects.equals(user.getId(), actionTokenUserId)) {
            logger.debugf("Forget-password triggered when reauthenticating user after authentication via action token. Skipping " + CREDENTIAL_TYPE + " screen and using user '%s' ", user.getUsername());
            context.success();
            return;
        }


        EventBuilder event = context.getEvent();
        // we don't want people guessing usernames, so if there is a problem, just continuously challenge
        if (user.getEmail() == null || user.getEmail().trim().length() == 0) {
            event.user(user)
                    .detail(Details.USERNAME, username)
                    .error(Errors.INVALID_EMAIL);

            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return;
        }

        int validityInSecs = context.getRealm().getActionTokenGeneratedByUserLifespan();
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

        // We send the secret in the email in a link as a query param.
        ResetCredentialsActionToken token = new ResetCredentialsActionToken(user.getId(), absoluteExpirationInSecs, authenticationSession.getId());
        String link = UriBuilder
                .fromUri(context.getActionTokenUrl(token.serialize(context.getSession(), context.getRealm(), context.getUriInfo())))
                .build()
                .toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);
        try {
            logger.debug("sendEmail - Reset Link : " + link);

            context.getSession().getProvider(EmailTemplateProvider.class).setRealm(context.getRealm()).setUser(user).sendPasswordReset(link, expirationInMinutes);

            event.clone().event(EventType.SEND_RESET_PASSWORD)
                    .user(user)
                    .detail(Details.USERNAME, username)
                    .detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, authenticationSession.getId()).success();
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));


            Response challenge = context.form()
                    .createForm("password-reset-email.ftl");
            context.failureChallenge(AuthenticationFlowError.UNKNOWN_USER, challenge);

        } catch (EmailException e) {
            event.clone().event(EventType.SEND_RESET_PASSWORD)
                    .detail(Details.USERNAME, username)
                    .user(user)
                    .error(Errors.EMAIL_SEND_FAILED);
            ServicesLogger.LOGGER.failedToSendPwdResetEmail(e);
            Response challenge = context.form()
                    .setError(Messages.EMAIL_SENT_ERROR)
                    .createErrorPage();
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
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
