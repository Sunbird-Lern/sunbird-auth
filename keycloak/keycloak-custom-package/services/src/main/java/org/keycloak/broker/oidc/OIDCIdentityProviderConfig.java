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
package org.keycloak.broker.oidc;

import org.keycloak.models.IdentityProviderModel;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProviderConfig extends OAuth2IdentityProviderConfig {

    private static final String JWKS_URL = "jwksUrl";

    private static final String USE_JWKS_URL = "useJwksUrl";


    public OIDCIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public String getPrompt() {
        return getConfig().get("prompt");
    }
    public void setPrompt(String prompt) {
        getConfig().put("prompt", prompt);
    }

    public String getIssuer() {
        return getConfig().get("issuer");
    }
    public void setIssuer(String issuer) {
        getConfig().put("issuer", issuer);
    }
    public String getLogoutUrl() {
        return getConfig().get("logoutUrl");
    }
    public void setLogoutUrl(String url) {
        getConfig().put("logoutUrl", url);
    }

    public String getPublicKeySignatureVerifier() {
        return getConfig().get("publicKeySignatureVerifier");
    }

    public void setPublicKeySignatureVerifier(String signingCertificate) {
        getConfig().put("publicKeySignatureVerifier", signingCertificate);
    }

    public String getPublicKeySignatureVerifierKeyId() {
        return getConfig().get("publicKeySignatureVerifierKeyId");
    }

    public void setPublicKeySignatureVerifierKeyId(String publicKeySignatureVerifierKeyId) {
        getConfig().put("publicKeySignatureVerifierKeyId", publicKeySignatureVerifierKeyId);
    }

    public boolean isValidateSignature() {
        return Boolean.valueOf(getConfig().get("validateSignature"));
    }

    public void setValidateSignature(boolean validateSignature) {
        getConfig().put("validateSignature", String.valueOf(validateSignature));
    }

    public boolean isUseJwksUrl() {
        return Boolean.valueOf(getConfig().get(USE_JWKS_URL));
    }

    public void setUseJwksUrl(boolean useJwksUrl) {
        getConfig().put(USE_JWKS_URL, String.valueOf(useJwksUrl));
    }

    public String getJwksUrl() {
        return getConfig().get(JWKS_URL);
    }

    public void setJwksUrl(String jwksUrl) {
        getConfig().put(JWKS_URL, jwksUrl);
    }

    public boolean isBackchannelSupported() {
        return Boolean.valueOf(getConfig().get("backchannelSupported"));
    }

    public void setBackchannelSupported(boolean backchannel) {
        getConfig().put("backchannelSupported", String.valueOf(backchannel));
    }

    public boolean isDisableUserInfoService() {
        String disableUserInfo = getConfig().get("disableUserInfo");
        return disableUserInfo == null ? false : Boolean.valueOf(disableUserInfo);
    }

    public void setDisableUserInfoService(boolean disable) {
        getConfig().put("disableUserInfo", String.valueOf(disable));
    }




}
