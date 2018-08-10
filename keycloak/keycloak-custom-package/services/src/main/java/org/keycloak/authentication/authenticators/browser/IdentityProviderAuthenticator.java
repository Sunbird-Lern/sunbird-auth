/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
 */

package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(IdentityProviderAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getUriInfo().getQueryParameters().containsKey(AdapterConstants.KC_IDP_HINT)) {
            String providerId = context.getUriInfo().getQueryParameters().getFirst(AdapterConstants.KC_IDP_HINT);
            if (providerId == null || providerId.equals("")) {
                LOG.tracef("Skipping: kc_idp_hint query parameter is empty");
                context.attempted();
            } else {
                LOG.tracef("Redirecting: %s set to %s", AdapterConstants.KC_IDP_HINT, providerId);
                redirect(context, providerId);
            }
        } else if (context.getAuthenticatorConfig() != null && context.getAuthenticatorConfig().getConfig().containsKey(IdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER)) {
            String defaultProvider = context.getAuthenticatorConfig().getConfig().get(IdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER);
            LOG.tracef("Redirecting: default provider set to %s", defaultProvider);
            redirect(context, defaultProvider);
        } else {
            LOG.tracef("No default provider set or %s query parameter provided", AdapterConstants.KC_IDP_HINT);
            context.attempted();
        }
    }

    private void redirect(AuthenticationFlowContext context, String providerId) {
        List<IdentityProviderModel> identityProviders = context.getRealm().getIdentityProviders();
        for (IdentityProviderModel identityProvider : identityProviders) {
            if (identityProvider.isEnabled() && providerId.equals(identityProvider.getAlias())) {
                String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(), context.getAuthenticationSession()).getCode();
                String clientId = context.getAuthenticationSession().getClient().getClientId();
                Response response = Response.seeOther(
                        Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), providerId, context.getRealm().getName(), accessCode, clientId))
                        .build();

                LOG.debugf("Redirecting to %s", providerId);
                context.forceChallenge(response);
                return;
            }
        }

        LOG.warnf("Provider not found or not enabled for realm %s", providerId);
        context.attempted();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

}
