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

package org.keycloak.email;

import org.keycloak.events.Event;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EmailTemplateProvider extends Provider {

    String IDENTITY_PROVIDER_BROKER_CONTEXT = "identityProviderBrokerCtx";

    public EmailTemplateProvider setRealm(RealmModel realm);

    public EmailTemplateProvider setUser(UserModel user);

    public EmailTemplateProvider setAttribute(String name, Object value);

    public void sendEvent(Event event) throws EmailException;

    /**
     * Reset password sent from forgot password link on login
     *
     * @param link
     * @param expirationInMinutes
     * @throws EmailException
     */
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException;

    /**
     * Test SMTP connection with current logged in user
     *
     * @param config SMTP server configuration
     * @param user SMTP recipient
     * @throws EmailException
     */
    public void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException;

    /**
     * Send to confirm that user wants to link his account with identity broker link
     */
    void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException;

    /**
     * Change password email requested by admin
     *
     * @param link
     * @param expirationInMinutes
     * @throws EmailException
     */
    public void sendExecuteActions(String link, long expirationInMinutes) throws EmailException;

    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException;

}
