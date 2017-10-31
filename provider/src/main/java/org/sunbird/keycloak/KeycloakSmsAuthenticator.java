package org.sunbird.keycloak;

import org.apache.http.util.TextUtils;
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
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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


    public void authenticate(AuthenticationFlowContext context) {
        logger.debug("authenticate called ... context = " + context);
        UserModel user = context.getUser();
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();

        List<String> mobileNumberCreds = user.getAttribute("mobile");

        String mobileNumber = null;
        String userEmail = user.getEmail();

        if (mobileNumberCreds != null && !mobileNumberCreds.isEmpty()) {
            mobileNumber = mobileNumberCreds.get(0);
        }

        if (!TextUtils.isEmpty(userEmail)) {
            logger.debug("Email is not null - " + userEmail);
        }

        if (mobileNumber != null) {
            // The mobile number is configured --> send an SMS
            long nrOfDigits = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsAuthenticatorConstants.CONF_PRP_SMS_CODE_LENGTH, 8L);
            logger.debug("Using nrOfDigits " + nrOfDigits);


            long ttl = KeycloakSmsAuthenticatorUtil.getConfigLong(config, KeycloakSmsAuthenticatorConstants.CONF_PRP_SMS_CODE_TTL, 10 * 60L); // 10 minutes in s

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
        } else if (!TextUtils.isEmpty(userEmail)) {
            logger.debug("Trying to send email to - " + userEmail);
            sendEmail(context);

//            try {
//                sendPasswordResetLink("Link", 15, context);
//            } catch (EmailException e) {
//                e.printStackTrace();
//
//                Response challenge = context.form()
//                        .setError("Some error occurred - " + e.getMessage())
//                        .createForm("sms-validation-error.ftl");
//                context.failureChallenge(AuthenticationFlowError.UNKNOWN_USER, challenge);
//            }

        } else {
            // The mobile number is NOT configured --> complain
            Response challenge = context.form()
                    .setError("Missing mobile number and email!")
                    .createForm("sms-validation-error.ftl");
            context.failureChallenge(AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED, challenge);
        }
    }

    private void sendEmail(AuthenticationFlowContext context) {
        logger.debug("sendEmail : entering");

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


    public void action(AuthenticationFlowContext context) {
        logger.debug("action called ... context = " + context);
        CODE_STATUS status = validateCode(context);
        Response challenge = null;
        switch (status) {
            case EXPIRED:
                challenge = context.form()
                        .setError("code is expired")
                        .createForm("sms-validation.ftl");
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
                break;

            case INVALID:
                if (context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.OPTIONAL ||
                        context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.ALTERNATIVE) {
                    logger.debug("Calling context.attempted()");
                    context.attempted();
                } else if (context.getExecution().getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                    challenge = context.form()
                            .setError("Invalid code specified, please enter it again")
                            .createForm("sms-validation.ftl");
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
                } else {
                    // Something strange happened
                    logger.warn("Undefined execution ...");
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
        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(KeycloakSmsAuthenticatorConstants.USR_CRED_MDL_SMS_CODE);
        credentials.setValue(code);

        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);

        credentials.setType(KeycloakSmsAuthenticatorConstants.USR_CRED_MDL_SMS_EXP_TIME);
        credentials.setValue((expiringAt).toString());
        context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), credentials);
    }


    protected CODE_STATUS validateCode(AuthenticationFlowContext context) {
        CODE_STATUS result = CODE_STATUS.INVALID;

        logger.debug("validateCode called ... ");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String enteredCode = formData.getFirst(KeycloakSmsAuthenticatorConstants.ANSW_SMS_CODE);
        KeycloakSession session = context.getSession();

        List codeCreds = session.userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsAuthenticatorConstants.USR_CRED_MDL_SMS_CODE);
        /*List timeCreds = session.userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), KeycloakSmsAuthenticatorConstants.USR_CRED_MDL_SMS_EXP_TIME);*/

        CredentialModel expectedCode = (CredentialModel) codeCreds.get(0);
        /*CredentialModel expTimeString = (CredentialModel) timeCreds.get(0);*/

        logger.debug("Expected code = " + expectedCode + "    entered code = " + enteredCode);

        if (expectedCode != null) {
            result = enteredCode.equals(expectedCode.getValue()) ? CODE_STATUS.VALID : CODE_STATUS.INVALID;
            /*long now = new Date().getTime();

            logger.debug("Valid code expires in " + (Long.parseLong(expTimeString.getValue()) - now) + " ms");
            if (result == CODE_STATUS.VALID) {
                if (Long.parseLong(expTimeString.getValue()) < now) {
                    logger.debug("Code is expired !!");
                    result = CODE_STATUS.EXPIRED;
                }
            }*/
        }
        logger.debug("result : " + result);
        return result;
    }

//    public void sendPasswordResetLink (String link, long expirationInMinutes, AuthenticationFlowContext context) throws EmailException {
//        logger.debug("sendPasswordResetLink : entered");
//        Map<String, Object> attributes = new HashMap<String, Object>();
//        attributes.put("link", link);
//        attributes.put("linkExpiration", expirationInMinutes);
//
//        String realmName = context.getRealm().getName().substring(0, 1).toUpperCase() + context.getRealm().getName().substring(1);
//        attributes.put("realmName", realmName);
//
//        logger.debug("sendPasswordResetLink : exiting");
//
//        send("passwordResetSubject", "password-reset-email.ftl", attributes, context);
//    }
//
//    private void send(String subjectKey, String template, Map<String, Object> attributes, AuthenticationFlowContext context) throws EmailException {
//        try {
//            logger.debug("send1 : entered");
//
//            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
//            ThemeProvider themeProvider = context.getSession().getProvider(ThemeProvider.class, "extending");
//            Theme theme = themeProvider.getTheme(context.getRealm().getEmailTheme(), Theme.Type.EMAIL);
//            Locale locale = LocaleHelper.getLocale(context.getSession(), context.getRealm(), context.getUser());
//            attributes.put("locale", locale);
//            Properties rb = theme.getMessages(locale);
//            attributes.put("msg", new MessageFormatterMethod(locale, rb));
//            String subject = new MessageFormat(rb.getProperty(subjectKey, subjectKey), locale).format(new Object[0]);
////            String body = freeMarkerUtil.processTemplate(attributes, template, theme);
//            String body = "some body with link!" ;
//
//            logger.debug("send1 : exiting");
//            send(subject, body, context);
//        } catch (Exception e) {
//            throw new EmailException("Failed to template email", e);
//        }
//    }
//
//    private void send(String subject, String body, AuthenticationFlowContext context) throws EmailException {
//        try {
//            logger.debug("send2 : entered");
//
//            String address = context.getUser().getEmail();
//            Map<String, String> config = context.getRealm().getSmtpConfig();
//
//            Properties props = new Properties();
//            props.setProperty("mail.smtp.host", config.get("host"));
//
//            boolean auth = "true".equals(config.get("auth"));
//            boolean ssl = "true".equals(config.get("ssl"));
//            boolean starttls = "true".equals(config.get("starttls"));
//
//            if (config.containsKey("port")) {
//                props.setProperty("mail.smtp.port", config.get("port"));
//            }
//
//            if (auth) {
//                props.put("mail.smtp.auth", "true");
//            }
//
//            if (ssl) {
//                props.put("mail.smtp.socketFactory.port", config.get("port"));
//                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//            }
//
//            if (starttls) {
//                props.put("mail.smtp.starttls.enable", "true");
//            }
//
//            String from = config.get("from");
//
//            Session session = Session.getInstance(props);
//
//            Message msg = new MimeMessage(session);
//            msg.setFrom(new InternetAddress(from));
//            msg.setHeader("To", address);
//            msg.setSubject(subject);
//            msg.setText(body);
//            msg.saveChanges();
//            msg.setSentDate(new Date());
//
//            Transport transport = session.getTransport("smtp");
//            if (auth) {
//                transport.connect(config.get("user"), config.get("password"));
//            } else {
//                transport.connect();
//            }
//            transport.sendMessage(msg, new InternetAddress[]{new InternetAddress(address)});
//            logger.debug("send2 : exiting");
//        } catch (Exception e) {
//            logger.warn("Failed to send email", e);
//            throw new EmailException(e);
//        }
//    }

    public boolean requiresUser() {
        logger.debug("requiresUser called ... returning true");
        return true;
    }

    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("configuredFor called ... session=" + session + ", realm=" + realm + ", user=" + user);
        return true;
    }

    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        logger.debug("setRequiredActions called ... session=" + session + ", realm=" + realm + ", user=" + user);
    }

    public void close() {
        logger.debug("close called ...");
    }

}
