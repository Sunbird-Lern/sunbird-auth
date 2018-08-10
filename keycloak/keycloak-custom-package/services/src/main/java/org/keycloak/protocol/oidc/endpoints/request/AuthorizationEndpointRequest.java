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

package org.keycloak.protocol.oidc.endpoints.request;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthorizationEndpointRequest {

    String clientId;
    String redirectUriParam;
    String responseType;
    String responseMode;
    String state;
    String scope;
    String loginHint;
    String prompt;
    String nonce;
    Integer maxAge;
    String idpHint;
    Map<String, String> additionalReqParams = new HashMap<>();

    // https://tools.ietf.org/html/rfc7636#section-6.1
    String codeChallenge;
    String codeChallengeMethod;

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUriParam() {
        return redirectUriParam;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public String getState() {
        return state;
    }

    public String getScope() {
        return scope;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getNonce() {
        return nonce;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public String getIdpHint() {
        return idpHint;
    }

    public Map<String, String> getAdditionalReqParams() {
        return additionalReqParams;
    }

    // https://tools.ietf.org/html/rfc7636#section-6.1
    public String getCodeChallenge() {
        return codeChallenge;
    }

    // https://tools.ietf.org/html/rfc7636#section-6.1
    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

}
