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
package org.keycloak.testsuite.auth.page.login;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.auth.page.account.AccountFields;
import org.keycloak.testsuite.auth.page.account.PasswordFields;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;


/**
 *
 * @author vramik
 */
public class ResetCredentials extends LoginActions {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("reset-credentials");
    }

    @Page
    private AccountFields accountFields;
    @Page
    private PasswordFields passwordFields;

    @FindBy(id = "kc-info")
    private WebElement info;
    
    public void resetCredentials(String value) {
        accountFields.setUsername(value);
        submit();
    }
    
    public void updatePassword(String password) {
        passwordFields.setNewPassword(password);
        passwordFields.setConfirmPassword(password);
        submit();
    }

    public String getInfoMessage() {
        waitUntilElement(info, "Info message should be visible").is().present();
        return info.getText();
    }
}
