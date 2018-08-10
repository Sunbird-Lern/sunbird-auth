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

package org.keycloak.adapters.saml.elytron;

import java.net.URI;
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jboss.logging.Logger;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.SamlUtil;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.wildfly.security.http.HttpScope;
import org.wildfly.security.http.Scope;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ElytronSamlSessionStore implements SamlSessionStore, ElytronTokeStore {
    protected static Logger log = Logger.getLogger(SamlSessionStore.class);
    public static final String SAML_REDIRECT_URI = "SAML_REDIRECT_URI";

    private final SessionIdMapper idMapper;
    protected final SamlDeployment deployment;
    private final ElytronHttpFacade exchange;


    public ElytronSamlSessionStore(ElytronHttpFacade exchange, SessionIdMapper idMapper, SamlDeployment deployment) {
        this.exchange = exchange;
        this.idMapper = idMapper;
        this.deployment = deployment;
    }

    @Override
    public void setCurrentAction(CurrentAction action) {
        if (action == CurrentAction.NONE && !exchange.getScope(Scope.SESSION).exists()) return;
        exchange.getScope(Scope.SESSION).setAttachment(CURRENT_ACTION, action);
    }

    @Override
    public boolean isLoggingIn() {
        HttpScope session = exchange.getScope(Scope.SESSION);
        if (!session.exists()) return false;
        CurrentAction action = (CurrentAction) session.getAttachment(CURRENT_ACTION);
        return action == CurrentAction.LOGGING_IN;
    }

    @Override
    public boolean isLoggingOut() {
        HttpScope session = exchange.getScope(Scope.SESSION);
        if (!session.exists()) return false;
        CurrentAction action = (CurrentAction) session.getAttachment(CURRENT_ACTION);
        return action == CurrentAction.LOGGING_OUT;
    }

    @Override
    public void logoutAccount() {
        HttpScope session = getSession(false);
        if (session.exists()) {
            SamlSession samlSession = (SamlSession)session.getAttachment(SamlSession.class.getName());
            if (samlSession != null) {
                if (samlSession.getSessionIndex() != null) {
                    idMapper.removeSession(session.getID());
                }
                session.setAttachment(SamlSession.class.getName(), null);
            }
            session.setAttachment(SAML_REDIRECT_URI, null);
        }
    }

    @Override
    public void logoutByPrincipal(String principal) {
        Set<String> sessions = idMapper.getUserSessions(principal);
        if (sessions != null) {
            List<String> ids = new LinkedList<>();
            ids.addAll(sessions);
            logoutSessionIds(ids);
            for (String id : ids) {
                idMapper.removeSession(id);
            }
        }

    }

    @Override
    public void logoutBySsoId(List<String> ssoIds) {
        if (ssoIds == null) return;
        List<String> sessionIds = new LinkedList<>();
        for (String id : ssoIds) {
             String sessionId = idMapper.getSessionFromSSO(id);
             if (sessionId != null) {
                 sessionIds.add(sessionId);
                 idMapper.removeSession(sessionId);
             }

        }
        logoutSessionIds(sessionIds);
    }

    protected void logoutSessionIds(List<String> sessionIds) {
        sessionIds.forEach(id -> {
            HttpScope scope = exchange.getScope(Scope.SESSION, id);

            if (scope.exists()) {
                scope.invalidate();
            }
        });
    }

    @Override
    public boolean isLoggedIn() {
        HttpScope session = getSession(false);
        if (!session.exists()) {
            log.debug("session was null, returning null");
            return false;
        }
        final SamlSession samlSession = (SamlSession)session.getAttachment(SamlSession.class.getName());
        if (samlSession == null) {
            log.debug("SamlSession was not in session, returning null");
            return false;
        }

        exchange.authenticationComplete(samlSession);
        restoreRequest();
        return true;
    }

    @Override
    public void saveAccount(SamlSession account) {
        HttpScope session = getSession(true);
        session.setAttachment(SamlSession.class.getName(), account);
        String sessionId = changeSessionId(session);
        idMapper.map(account.getSessionIndex(), account.getPrincipal().getSamlSubject(), sessionId);

    }

    protected String changeSessionId(HttpScope session) {
        if (!deployment.turnOffChangeSessionIdOnLogin()) return session.getID();
        else return session.getID();
    }

    @Override
    public SamlSession getAccount() {
        HttpScope session = getSession(true);
        return (SamlSession)session.getAttachment(SamlSession.class.getName());
    }

    @Override
    public String getRedirectUri() {
        HttpScope session = exchange.getScope(Scope.SESSION);
        String redirect = (String) session.getAttachment(SAML_REDIRECT_URI);
        if (redirect == null) {
            URI uri = exchange.getURI();
            String path = uri.getPath();
            String relativePath = exchange.getRequest().getRelativePath();
            String contextPath = path.substring(0, path.indexOf(relativePath));

            if (!contextPath.isEmpty()) {
                contextPath = contextPath + "/";
            }

            String baseUri = KeycloakUriBuilder.fromUri(path).replacePath(contextPath).build().toString();
            return SamlUtil.getRedirectTo(exchange, contextPath, baseUri);
        }
        return redirect;
    }

    @Override
    public void saveRequest() {
        exchange.suspendRequest();
        HttpScope scope = exchange.getScope(Scope.SESSION);

        if (!scope.exists()) {
            scope.create();
        }

        KeycloakUriBuilder uriBuilder = KeycloakUriBuilder.fromUri(exchange.getURI()).replaceQuery(exchange.getURI().getQuery());
        String uri = uriBuilder.build().toString();

        scope.setAttachment(SAML_REDIRECT_URI, uri);
    }

    @Override
    public boolean restoreRequest() {
        return exchange.restoreRequest();
    }

    protected HttpScope getSession(boolean create) {
        HttpScope scope = exchange.getScope(Scope.SESSION);

        if (!scope.exists() && create) {
            scope.create();
        }

        return scope;
    }

    @Override
    public void logout(boolean glo) {
        logoutAccount();
    }
}
