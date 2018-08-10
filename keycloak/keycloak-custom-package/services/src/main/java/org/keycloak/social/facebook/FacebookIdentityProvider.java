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

package org.keycloak.social.facebook;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.oidc.util.JsonSimpleHttp;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FacebookIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

	public static final String AUTH_URL = "https://graph.facebook.com/oauth/authorize";
	public static final String TOKEN_URL = "https://graph.facebook.com/oauth/access_token";
	public static final String PROFILE_URL = "https://graph.facebook.com/me?fields=id,name,email,first_name,last_name";
	public static final String DEFAULT_SCOPE = "email";

	public FacebookIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			JsonNode profile = JsonSimpleHttp.asJson(SimpleHttp.doGet(PROFILE_URL, session).header("Authorization", "Bearer " + accessToken));

			String id = getJsonProperty(profile, "id");

			BrokeredIdentityContext user = new BrokeredIdentityContext(id);

			String email = getJsonProperty(profile, "email");

			user.setEmail(email);

			String username = getJsonProperty(profile, "username");

			if (username == null) {
				if (email != null) {
					username = email;
				} else {
					username = id;
				}
			}

			user.setUsername(username);

			String firstName = getJsonProperty(profile, "first_name");
			String lastName = getJsonProperty(profile, "last_name");

			if (lastName == null) {
				lastName = "";
			} else {
				lastName = " " + lastName;
			}

			user.setName(firstName + lastName);
			user.setIdpConfig(getConfig());
			user.setIdp(this);

			AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

			return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from facebook.", e);
		}
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
