/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.actiontoken.verifyemail;

import org.keycloak.authentication.actiontoken.AbstractActionTokenHander;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.actiontoken.*;
import org.keycloak.events.*;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import java.util.Objects;
import javax.ws.rs.core.Response;

/**
 * Action token handler for verification of e-mail address.
 * @author hmlnarik
 */
public class VerifyEmailActionTokenHandler extends AbstractActionTokenHander<VerifyEmailActionToken> {

    public VerifyEmailActionTokenHandler() {
        super(
          VerifyEmailActionToken.TOKEN_TYPE,
          VerifyEmailActionToken.class,
          Messages.STALE_VERIFY_EMAIL_LINK,
          EventType.VERIFY_EMAIL,
          Errors.INVALID_TOKEN
        );
    }

    @Override
    public Predicate<? super VerifyEmailActionToken>[] getVerifiers(ActionTokenContext<VerifyEmailActionToken> tokenContext) {
        return TokenUtils.predicates(
          TokenUtils.checkThat(
            t -> Objects.equals(t.getEmail(), tokenContext.getAuthenticationSession().getAuthenticatedUser().getEmail()),
            Errors.INVALID_EMAIL, getDefaultErrorMessage()
          )
        );
    }

    @Override
        public Response handleToken(VerifyEmailActionToken token, ActionTokenContext<VerifyEmailActionToken> tokenContext) {
        UserModel user = tokenContext.getAuthenticationSession().getAuthenticatedUser();
        EventBuilder event = tokenContext.getEvent();

        event.event(EventType.VERIFY_EMAIL).detail(Details.EMAIL, user.getEmail());

        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();

        // verify user email as we know it is valid as this entry point would never have gotten here.
        user.setEmailVerified(true);
        user.removeRequiredAction(RequiredAction.VERIFY_EMAIL);
        authSession.removeRequiredAction(RequiredAction.VERIFY_EMAIL);

        event.success();

        if (tokenContext.isAuthenticationSessionFresh()) {
            AuthenticationSessionManager asm = new AuthenticationSessionManager(tokenContext.getSession());
            asm.removeAuthenticationSession(tokenContext.getRealm(), authSession, true);
            return tokenContext.getSession().getProvider(LoginFormsProvider.class)
                    .setSuccess(Messages.EMAIL_VERIFIED)
                    .createInfoPage();
        }

        tokenContext.setEvent(event.clone().removeDetail(Details.EMAIL).event(EventType.LOGIN));

        String nextAction = AuthenticationManager.nextRequiredAction(tokenContext.getSession(), authSession, tokenContext.getClientConnection(), tokenContext.getRequest(), tokenContext.getUriInfo(), event);
        return AuthenticationManager.redirectToRequiredActions(tokenContext.getSession(), tokenContext.getRealm(), authSession, tokenContext.getUriInfo(), nextAction);
    }

}
