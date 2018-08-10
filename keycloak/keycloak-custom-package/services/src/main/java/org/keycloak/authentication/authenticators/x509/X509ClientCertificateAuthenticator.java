/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.keycloak.authentication.authenticators.x509;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.ServicesLogger;

/**
 * @author <a href="mailto:pnalyvayko@agi.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 *
 */
public class X509ClientCertificateAuthenticator extends AbstractX509ClientCertificateAuthenticator {

    protected static ServicesLogger logger = ServicesLogger.LOGGER;

    @Override
    public void close() {

    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        try {

            dumpContainerAttributes(context);

            X509Certificate[] certs = getCertificateChain(context);
            if (certs == null || certs.length == 0) {
                // No x509 client cert, fall through and
                // continue processing the rest of the authentication flow
                logger.debug("[X509ClientCertificateAuthenticator:authenticate] x509 client certificate is not available for mutual SSL.");
                context.attempted();
                return;
            }

            X509AuthenticatorConfigModel config = null;
            if (context.getAuthenticatorConfig() != null && context.getAuthenticatorConfig().getConfig() != null) {
                config = new X509AuthenticatorConfigModel(context.getAuthenticatorConfig());
            }
            if (config == null) {
                logger.warn("[X509ClientCertificateAuthenticator:authenticate] x509 Client Certificate Authentication configuration is not available.");
                context.challenge(createInfoResponse(context, "X509 client authentication has not been configured yet"));
                context.attempted();
                return;
            }

            // Validate X509 client certificate
            try {
                CertificateValidator.CertificateValidatorBuilder builder = certificateValidationParameters(config);
                CertificateValidator validator = builder.build(certs);
                validator.checkRevocationStatus()
                         .validateKeyUsage()
                         .validateExtendedKeyUsage();
            } catch(Exception e) {
                logger.error(e.getMessage(), e);
                // TODO use specific locale to load error messages
                String errorMessage = "Certificate validation's failed.";
                // TODO is calling form().setErrors enough to show errors on login screen?
                context.challenge(createErrorResponse(context, certs[0].getSubjectDN().getName(),
                        errorMessage, e.getMessage()));
                context.attempted();
                return;
            }

            Object userIdentity = getUserIdentityExtractor(config).extractUserIdentity(certs);
            if (userIdentity == null) {
                logger.warnf("[X509ClientCertificateAuthenticator:authenticate] Unable to extract user identity from certificate.");
                // TODO use specific locale to load error messages
                String errorMessage = "Unable to extract user identity from specified certificate";
                // TODO is calling form().setErrors enough to show errors on login screen?
                context.challenge(createErrorResponse(context, certs[0].getSubjectDN().getName(), errorMessage));
                context.attempted();
                return;
            }

            UserModel user;
            try {
                context.getEvent().detail(Details.USERNAME, userIdentity.toString());
                context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, userIdentity.toString());
                user = getUserIdentityToModelMapper(config).find(context, userIdentity);
            }
            catch(ModelDuplicateException e) {
                logger.modelDuplicateException(e);
                String errorMessage = "X509 certificate authentication's failed.";
                // TODO is calling form().setErrors enough to show errors on login screen?
                context.challenge(createErrorResponse(context, certs[0].getSubjectDN().getName(),
                        errorMessage, e.getMessage()));
                context.attempted();
                return;
            }

            if (invalidUser(context, user)) {
                // TODO use specific locale to load error messages
                String errorMessage = "X509 certificate authentication's failed.";
                // TODO is calling form().setErrors enough to show errors on login screen?
                context.challenge(createErrorResponse(context, certs[0].getSubjectDN().getName(),
                        errorMessage, "Invalid user"));
                context.attempted();
                return;
            }

            if (!userEnabled(context, user)) {
                // TODO use specific locale to load error messages
                String errorMessage = "X509 certificate authentication's failed.";
                // TODO is calling form().setErrors enough to show errors on login screen?
                context.challenge(createErrorResponse(context, certs[0].getSubjectDN().getName(),
                        errorMessage, "User is disabled"));
                context.attempted();
                return;
            }
            if (context.getRealm().isBruteForceProtected()) {
                if (context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user)) {
                    context.getEvent().user(user);
                    context.getEvent().error(Errors.USER_TEMPORARILY_DISABLED);
                    // TODO use specific locale to load error messages
                    String errorMessage = "X509 certificate authentication's failed.";
                    // TODO is calling form().setErrors enough to show errors on login screen?
                    context.challenge(createErrorResponse(context, certs[0].getSubjectDN().getName(),
                            errorMessage, "User is temporarily disabled. Contact administrator."));
                    context.attempted();
                    return;
                }
            }
            context.setUser(user);

            // Check whether to display the identity confirmation
            if (!config.getConfirmationPageDisallowed()) {
                // FIXME calling forceChallenge was the only way to display
                // a form to let users either choose the user identity from certificate
                // or to ignore it and proceed to a normal login screen. Attempting
                // to call the method "challenge" results in a wrong/unexpected behavior.
                // The question is whether calling "forceChallenge" here is ok from
                // the design viewpoint?
                context.forceChallenge(createSuccessResponse(context, certs[0].getSubjectDN().getName()));
                // Do not set the flow status yet, we want to display a form to let users
                // choose whether to accept the identity from certificate or to specify username/password explicitly
            }
            else {
                // Bypass the confirmation page and log the user in
                context.success();
            }
        }
        catch(Exception e) {
            logger.errorf("[X509ClientCertificateAuthenticator:authenticate] Exception: %s", e.getMessage());
            context.attempted();
        }
    }

    private Response createErrorResponse(AuthenticationFlowContext context,
                                         String subjectDN,
                                         String errorMessage,
                                         String ... errorParameters) {

        return createResponse(context, subjectDN, false, errorMessage, errorParameters);
    }

    private Response createSuccessResponse(AuthenticationFlowContext context,
                                           String subjectDN) {
        return createResponse(context, subjectDN, true, null, null);
    }

    private Response createResponse(AuthenticationFlowContext context,
                                         String subjectDN,
                                         boolean isUserEnabled,
                                         String errorMessage,
                                         Object[] errorParameters) {

        LoginFormsProvider form = context.form();
        if (errorMessage != null && errorMessage.trim().length() > 0) {
            List<FormMessage> errors = new LinkedList<>();

            errors.add(new FormMessage(errorMessage));
            if (errorParameters != null) {

                for (Object errorParameter : errorParameters) {
                    if (errorParameter == null) continue;
                    for (String part : errorParameter.toString().split("\n")) {
                        errors.add(new FormMessage(part));
                    }
                }
            }
            form.setErrors(errors);
        }

        return form
                .setAttribute("username", context.getUser() != null ? context.getUser().getUsername() : "unknown user")
                .setAttribute("subjectDN", subjectDN)
                .setAttribute("isUserEnabled", isUserEnabled)
                .createForm("login-x509-info.ftl");
    }

    private void dumpContainerAttributes(AuthenticationFlowContext context) {

        Enumeration<String> attributeNames = context.getHttpRequest().getAttributeNames();
        while(attributeNames.hasMoreElements()) {
            String a = attributeNames.nextElement();
            logger.tracef("[X509ClientCertificateAuthenticator:dumpContainerAttributes] \"%s\"", a);
        }
    }

    private boolean userEnabled(AuthenticationFlowContext context, UserModel user) {
        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().error(Errors.USER_DISABLED);
            return false;
        }
        return true;
    }

    private boolean invalidUser(AuthenticationFlowContext context, UserModel user) {
        if (user == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            return true;
        }
        return false;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.clearUser();
            context.attempted();
            return;
        }
        if (context.getUser() != null) {
            context.success();
            return;
        }
        context.attempted();
    }
}
