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
package org.keycloak.authentication.actiontoken.idpverifyemail;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.authentication.actiontoken.DefaultActionToken;

/**
 * Representation of a token that represents a time-limited verify e-mail action.
 *
 * @author hmlnarik
 */
public class IdpVerifyAccountLinkActionToken extends DefaultActionToken {

    public static final String TOKEN_TYPE = "idp-verify-account-via-email";

    private static final String JSON_FIELD_IDENTITY_PROVIDER_USERNAME = "idpu";
    private static final String JSON_FIELD_IDENTITY_PROVIDER_ALIAS = "idpa";

    @JsonProperty(value = JSON_FIELD_IDENTITY_PROVIDER_USERNAME)
    private String identityProviderUsername;

    @JsonProperty(value = JSON_FIELD_IDENTITY_PROVIDER_ALIAS)
    private String identityProviderAlias;

    public IdpVerifyAccountLinkActionToken(String userId, int absoluteExpirationInSecs, String authenticationSessionId,
      String identityProviderUsername, String identityProviderAlias) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null, authenticationSessionId);
        this.identityProviderUsername = identityProviderUsername;
        this.identityProviderAlias = identityProviderAlias;
    }

    private IdpVerifyAccountLinkActionToken() {
    }

    public String getIdentityProviderUsername() {
        return identityProviderUsername;
    }

    public void setIdentityProviderUsername(String identityProviderUsername) {
        this.identityProviderUsername = identityProviderUsername;
    }

    public String getIdentityProviderAlias() {
        return identityProviderAlias;
    }

    public void setIdentityProviderAlias(String identityProviderAlias) {
        this.identityProviderAlias = identityProviderAlias;
    }
}
