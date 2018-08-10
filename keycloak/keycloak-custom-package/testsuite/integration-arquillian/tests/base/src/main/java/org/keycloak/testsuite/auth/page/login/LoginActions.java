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

import org.keycloak.testsuite.auth.page.AuthRealm;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 *
 * @author tkyjovsk
 */
public class LoginActions extends AuthRealm {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("login-actions");
    }

    @FindBy(css = "input[type='submit']")
    private WebElement submitButton;

    @FindBy(css = "div[id='kc-form-options'] span a")
    private WebElement backToLoginForm;

    @FindBy(xpath = "//span[@class='kc-feedback-text' and string-length(text())>1]")
    private WebElement feedbackText;
    
    public String getFeedbackText() {
        waitUntilElement(feedbackText, "Feedback message should be present").is().visible();
        return feedbackText.getText();
    }
    
    public void backToLoginPage() {
        backToLoginForm.click();
    }

    public void submit() {
        submitButton.click();
    }

}
