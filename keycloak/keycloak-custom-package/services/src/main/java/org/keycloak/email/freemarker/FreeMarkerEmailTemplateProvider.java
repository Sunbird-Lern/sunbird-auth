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

package org.keycloak.email.freemarker;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.EventBean;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeProvider;
import org.keycloak.theme.beans.MessageFormatterMethod;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerEmailTemplateProvider implements EmailTemplateProvider {

    private KeycloakSession session;
    private FreeMarkerUtil freeMarker;
    private RealmModel realm;
    private UserModel user;
    private final Map<String, Object> attributes = new HashMap<>();

    public FreeMarkerEmailTemplateProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
    }

    @Override
    public EmailTemplateProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public EmailTemplateProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public EmailTemplateProvider setAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    private String getRealmName() {
        if (realm.getDisplayName() != null) {
            return realm.getDisplayName();
        } else {
            return ObjectUtil.capitalize(realm.getName());
        }
    }

    @Override
    public void sendEvent(Event event) throws EmailException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("event", new EventBean(event));

        send(toCamelCase(event.getType()) + "Subject", "event-" + event.getType().toString().toLowerCase() + ".ftl", attributes);
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("link", link);
        attributes.put("linkExpiration", expirationInMinutes);

        attributes.put("realmName", getRealmName());

        send("passwordResetSubject", "password-reset.ftl", attributes);
    }

    @Override
    public void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException {
        setRealm(session.getContext().getRealm());
        setUser(user);

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("realmName", realm.getName());

        EmailTemplate email = processTemplate("emailTestSubject", Collections.emptyList(), "email-test.ftl", attributes);
        send(config, email.getSubject(), email.getTextBody(), email.getHtmlBody());
    }

    @Override
    public void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("link", link);
        attributes.put("linkExpiration", expirationInMinutes);

        attributes.put("realmName", getRealmName());

        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext) this.attributes.get(IDENTITY_PROVIDER_BROKER_CONTEXT);
        String idpAlias = brokerContext.getIdpConfig().getAlias();
        idpAlias = ObjectUtil.capitalize(idpAlias);

        attributes.put("identityProviderContext", brokerContext);
        attributes.put("identityProviderAlias", idpAlias);

        List<Object> subjectAttrs = Arrays.<Object>asList(idpAlias);
        send("identityProviderLinkSubject", subjectAttrs, "identity-provider-link.ftl", attributes);
    }

    @Override
    public void sendExecuteActions(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("link", link);
        attributes.put("linkExpiration", expirationInMinutes);

        attributes.put("realmName", getRealmName());

        send("executeActionsSubject", "executeActions.ftl", attributes);

    }

    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("link", link);
        attributes.put("linkExpiration", expirationInMinutes);

        attributes.put("realmName", getRealmName());

        send("emailVerificationSubject", "email-verification.ftl", attributes);
    }

    private void send(String subjectKey, String template, Map<String, Object> attributes) throws EmailException {
        send(subjectKey, Collections.emptyList(), template, attributes);
    }

    private EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException {
        try {
            ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
            Theme theme = themeProvider.getTheme(realm.getEmailTheme(), Theme.Type.EMAIL);
            Locale locale = session.getContext().resolveLocale(user);
            attributes.put("locale", locale);
            Properties rb = theme.getMessages(locale);
            attributes.put("msg", new MessageFormatterMethod(locale, rb));
            String subject = new MessageFormat(rb.getProperty(subjectKey,subjectKey),locale).format(subjectAttributes.toArray());
            String textTemplate = String.format("text/%s", template);
            String textBody;
            try {
                textBody = freeMarker.processTemplate(attributes, textTemplate, theme);
            } catch (final FreeMarkerException e ) {
                textBody = null;
            }
            String htmlTemplate = String.format("html/%s", template);
            String htmlBody;
            try {
                htmlBody = freeMarker.processTemplate(attributes, htmlTemplate, theme);
            } catch (final FreeMarkerException e ) {
                htmlBody = null;
            }

            return new EmailTemplate(subject, textBody, htmlBody);
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }
    private void send(String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException {
        try {
            EmailTemplate email = processTemplate(subjectKey, subjectAttributes, template, attributes);
            send(email.getSubject(), email.getTextBody(), email.getHtmlBody());
        } catch (Exception e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    private void send(String subject, String textBody, String htmlBody) throws EmailException {
        send(realm.getSmtpConfig(), subject, textBody, htmlBody);
    }

    private void send(Map<String, String> config, String subject, String textBody, String htmlBody) throws EmailException {
        EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class);
        emailSender.send(config, user, subject, textBody, htmlBody);
    }

    @Override
    public void close() {
    }

    private String toCamelCase(EventType event){
        StringBuilder sb = new StringBuilder("event");
        for(String s : event.name().toString().toLowerCase().split("_")){
            sb.append(ObjectUtil.capitalize(s));
        }
        return sb.toString();
    }

    private class EmailTemplate {

        private String subject;
        private String textBody;
        private String htmlBody;

        public EmailTemplate(String subject, String textBody, String htmlBody) {
            this.subject = subject;
            this.textBody = textBody;
            this.htmlBody = htmlBody;
        }

        public String getSubject() {
            return subject;
        }

        public String getTextBody() {
            return textBody;
        }

        public String getHtmlBody() {
            return htmlBody;
        }
    }

}
