/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakIdentity implements Identity {

    protected final AccessToken accessToken;
    protected final RealmModel realm;
    protected final KeycloakSession keycloakSession;
    protected final Attributes attributes;

    public KeycloakIdentity(KeycloakSession keycloakSession) {
        this(Tokens.getAccessToken(keycloakSession), keycloakSession);
    }

    public KeycloakIdentity(KeycloakSession keycloakSession, AccessToken accessToken) {
        this(accessToken, keycloakSession, keycloakSession.getContext().getRealm());
    }

    public KeycloakIdentity(AccessToken accessToken, KeycloakSession keycloakSession, RealmModel realm) {
        if (accessToken == null) {
            throw new ErrorResponseException("invalid_bearer_token", "Could not obtain bearer access_token from request.", Status.FORBIDDEN);
        }
        if (keycloakSession == null) {
            throw new ErrorResponseException("no_keycloak_session", "No keycloak session", Status.FORBIDDEN);
        }
        if (realm == null) {
            throw new ErrorResponseException("no_keycloak_session", "No realm set", Status.FORBIDDEN);
        }
        this.accessToken = accessToken;
        this.keycloakSession = keycloakSession;
        this.realm = realm;

        Map<String, Collection<String>> attributes = new HashMap<>();

        try {
            ObjectNode objectNode = JsonSerialization.createObjectNode(this.accessToken);
            Iterator<String> iterator = objectNode.fieldNames();

            while (iterator.hasNext()) {
                String fieldName = iterator.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                List<String> values = new ArrayList<>();

                if (fieldValue.isArray()) {
                    Iterator<JsonNode> valueIterator = fieldValue.iterator();

                    while (valueIterator.hasNext()) {
                        values.add(valueIterator.next().asText());
                    }
                } else {
                    String value = fieldValue.asText();

                    if (StringUtil.isNullOrEmpty(value)) {
                        continue;
                    }

                    values.add(value);
                }

                if (!values.isEmpty()) {
                    attributes.put(fieldName, values);
                }
            }

            AccessToken.Access realmAccess = accessToken.getRealmAccess();

            if (realmAccess != null) {
                attributes.put("kc.realm.roles", realmAccess.getRoles());
            }

            Map<String, AccessToken.Access> resourceAccess = accessToken.getResourceAccess();

            if (resourceAccess != null) {
                resourceAccess.forEach((clientId, access) -> attributes.put("kc.client." + clientId + ".roles", access.getRoles()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while reading attributes from security token.", e);
        }

        this.attributes = Attributes.from(attributes);
    }

    public KeycloakIdentity(AccessToken accessToken, KeycloakSession keycloakSession) {
        if (accessToken == null) {
            throw new ErrorResponseException("invalid_bearer_token", "Could not obtain bearer access_token from request.", Status.FORBIDDEN);
        }
        if (keycloakSession == null) {
            throw new ErrorResponseException("no_keycloak_session", "No keycloak session", Status.FORBIDDEN);
        }
        this.accessToken = accessToken;
        this.keycloakSession = keycloakSession;
        this.realm = keycloakSession.getContext().getRealm();

        Map<String, Collection<String>> attributes = new HashMap<>();

        try {
            ObjectNode objectNode = JsonSerialization.createObjectNode(this.accessToken);
            Iterator<String> iterator = objectNode.fieldNames();

            while (iterator.hasNext()) {
                String fieldName = iterator.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                List<String> values = new ArrayList<>();

                if (fieldValue.isArray()) {
                    Iterator<JsonNode> valueIterator = fieldValue.iterator();

                    while (valueIterator.hasNext()) {
                        values.add(valueIterator.next().asText());
                    }
                } else {
                    String value = fieldValue.asText();

                    if (StringUtil.isNullOrEmpty(value)) {
                        continue;
                    }

                    values.add(value);
                }

                if (!values.isEmpty()) {
                    attributes.put(fieldName, values);
                }
            }

            AccessToken.Access realmAccess = accessToken.getRealmAccess();

            if (realmAccess != null) {
                attributes.put("kc.realm.roles", realmAccess.getRoles());
            }

            Map<String, AccessToken.Access> resourceAccess = accessToken.getResourceAccess();

            if (resourceAccess != null) {
                resourceAccess.forEach((clientId, access) -> attributes.put("kc.client." + clientId + ".roles", access.getRoles()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while reading attributes from security token.", e);
        }

        this.attributes = Attributes.from(attributes);
    }

    @Override
    public String getId() {
        if (isResourceServer()) {
            ClientModel client = getTargetClient();
            return client==null ? null : client.getId();
        }

        return this.accessToken.getSubject();
    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    public AccessToken getAccessToken() {
        return this.accessToken;
    }

    private  boolean isResourceServer() {
        UserModel clientUser = null;

        ClientModel clientModel = getTargetClient();

        if (clientModel != null) {
            clientUser = this.keycloakSession.users().getServiceAccount(clientModel);
        }

        if (clientUser == null) {
            return false;
        }

        return this.accessToken.getSubject().equals(clientUser.getId());
    }

    private ClientModel getTargetClient() {
        if (this.accessToken.getIssuedFor() != null) {
            return realm.getClientByClientId(accessToken.getIssuedFor());
        }

        if (this.accessToken.getAudience() != null && this.accessToken.getAudience().length > 0) {
            String audience = this.accessToken.getAudience()[0];
            return realm.getClientByClientId(audience);
        }

        return null;
    }
}
