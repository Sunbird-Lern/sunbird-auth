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

package org.keycloak.forms.login.freemarker;

import org.keycloak.forms.login.LoginFormsPages;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Templates {

    public static String getTemplate(LoginFormsPages page) {
        switch (page) {
            case LOGIN:
                return "login.ftl";
            case LOGIN_TOTP:
                return "login-totp.ftl";
            case LOGIN_CONFIG_TOTP:
                return "login-config-totp.ftl";
            case LOGIN_VERIFY_EMAIL:
                return "login-verify-email.ftl";
            case LOGIN_IDP_LINK_CONFIRM:
                return "login-idp-link-confirm.ftl";
            case LOGIN_IDP_LINK_EMAIL:
                return "login-idp-link-email.ftl";
            case OAUTH_GRANT:
                return "login-oauth-grant.ftl";
            case LOGIN_RESET_PASSWORD:
                return "login-reset-password.ftl";
            case LOGIN_UPDATE_PASSWORD:
                return "login-update-password.ftl";
            case REGISTER:
                return "register.ftl";
            case INFO:
                return "info.ftl";
            case ERROR:
                return "error.ftl";
            case LOGIN_UPDATE_PROFILE:
                return "login-update-profile.ftl";
            case CODE:
                return "code.ftl";
            case LOGIN_PAGE_EXPIRED:
                return "login-page-expired.ftl";
            default:
                throw new IllegalArgumentException();
        }
    }

}
