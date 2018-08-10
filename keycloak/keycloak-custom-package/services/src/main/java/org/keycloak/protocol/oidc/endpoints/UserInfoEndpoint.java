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
package org.keycloak.protocol.oidc.endpoints;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.OAuthErrorException;
import org.keycloak.RSATokenVerifier;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.utils.MediaType;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pedroigor
 */
public class UserInfoEndpoint {

    @Context
    private HttpRequest request;

    @Context
    private HttpResponse response;

    @Context
    private UriInfo uriInfo;

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    private final TokenManager tokenManager;
    private final AppAuthManager appAuthManager;
    private final RealmModel realm;

    public UserInfoEndpoint(TokenManager tokenManager, RealmModel realm) {
        this.realm = realm;
        this.tokenManager = tokenManager;
        this.appAuthManager = new AppAuthManager();
    }

    @Path("/")
    @OPTIONS
    public Response issueUserInfoPreflight() {
        return Cors.add(this.request, Response.ok()).auth().preflight().build();
    }

    @Path("/")
    @GET
    @NoCache
    public Response issueUserInfoGet(@Context final HttpHeaders headers) {
        String accessToken = this.appAuthManager.extractAuthorizationHeaderToken(headers);
        return issueUserInfo(accessToken);
    }

    @Path("/")
    @POST
    @NoCache
    public Response issueUserInfoPost() {
        // Try header first
        HttpHeaders headers = request.getHttpHeaders();
        String accessToken = this.appAuthManager.extractAuthorizationHeaderToken(headers);

        // Fallback to form parameter
        if (accessToken == null) {
            accessToken = request.getDecodedFormParameters().getFirst("access_token");
        }

        return issueUserInfo(accessToken);
    }

    private Response issueUserInfo(String tokenString) {
        EventBuilder event = new EventBuilder(realm, session, clientConnection)
                .event(EventType.USER_INFO_REQUEST)
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);

        if (tokenString == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Token not provided", Response.Status.BAD_REQUEST);
        }

        AccessToken token = null;
        try {
            RSATokenVerifier verifier = RSATokenVerifier.create(tokenString)
                    .realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
            String kid = verifier.getHeader().getKeyId();
            verifier.publicKey(session.keys().getRsaPublicKey(realm, kid));
            token = verifier.verify().getToken();
        } catch (VerificationException e) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Token invalid: " + e.getMessage(), Response.Status.UNAUTHORIZED);
        }

        UserSessionModel userSession = findValidSession(token, event);

        UserModel userModel = userSession.getUser();
        if (userModel == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "User not found", Response.Status.BAD_REQUEST);
        }

        event.user(userModel)
                .detail(Details.USERNAME, userModel.getUsername());


        ClientModel clientModel = realm.getClientByClientId(token.getIssuedFor());
        if (clientModel == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client not found", Response.Status.BAD_REQUEST);
        }

        event.client(clientModel);

        if (!clientModel.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client disabled", Response.Status.BAD_REQUEST);
        }

        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(clientModel.getId());
        if (clientSession == null) {
            event.error(Errors.SESSION_EXPIRED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Session expired", Response.Status.UNAUTHORIZED);
        }

        AccessToken userInfo = new AccessToken();
        tokenManager.transformUserInfoAccessToken(session, userInfo, realm, clientModel, userModel, userSession, clientSession);

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("sub", userModel.getId());
        claims.putAll(userInfo.getOtherClaims());

        Response.ResponseBuilder responseBuilder;
        OIDCAdvancedConfigWrapper cfg = OIDCAdvancedConfigWrapper.fromClientModel(clientModel);

        if (cfg.isUserInfoSignatureRequired()) {
            String issuerUrl = Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName());
            String audience = clientModel.getClientId();
            claims.put("iss", issuerUrl);
            claims.put("aud", audience);

            Algorithm signatureAlg = cfg.getUserInfoSignedResponseAlg();
            PrivateKey privateKey = session.keys().getActiveRsaKey(realm).getPrivateKey();

            String signedUserInfo = new JWSBuilder()
                    .jsonContent(claims)
                    .sign(signatureAlg, privateKey);

            responseBuilder = Response.ok(signedUserInfo).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JWT);

            event.detail(Details.SIGNATURE_REQUIRED, "true");
            event.detail(Details.SIGNATURE_ALGORITHM, cfg.getUserInfoSignedResponseAlg().toString());
        } else {
            responseBuilder = Response.ok(claims).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

            event.detail(Details.SIGNATURE_REQUIRED, "false");
        }

        event.success();

        return Cors.add(request, responseBuilder).auth().allowedOrigins(token).build();
    }


    private UserSessionModel findValidSession(AccessToken token, EventBuilder event) {
        UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionState());
        UserSessionModel offlineUserSession = null;
        if (AuthenticationManager.isSessionValid(realm, userSession)) {
            event.session(userSession);
            return userSession;
        } else {
            offlineUserSession = session.sessions().getOfflineUserSession(realm, token.getSessionState());
            if (AuthenticationManager.isOfflineSessionValid(realm, offlineUserSession)) {
                event.session(offlineUserSession);
                return offlineUserSession;
            }
        }

        if (userSession == null && offlineUserSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "User session not found", Response.Status.BAD_REQUEST);
        }

        if (userSession != null) {
            event.session(userSession);
        } else {
            event.session(offlineUserSession);
        }

        event.error(Errors.SESSION_EXPIRED);
        throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Session expired", Response.Status.UNAUTHORIZED);
    }

}
