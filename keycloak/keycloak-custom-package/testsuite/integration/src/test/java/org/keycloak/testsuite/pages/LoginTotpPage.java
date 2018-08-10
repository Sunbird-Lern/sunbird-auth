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
package org.keycloak.testsuite.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginTotpPage extends AbstractPage {

    @FindBy(id = "totp")
    private WebElement totpInput;

    @FindBy(id = "password-token")
    private WebElement passwordToken;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(id = "kc-cancel")
    private WebElement cancelButton;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    public void login(String totp) {
        totpInput.clear();
        if (totp != null) totpInput.sendKeys(totp);

        submitButton.click();
    }

    public void cancel() {
        cancelButton.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public boolean isCurrent() {
        if (driver.getTitle().startsWith("Log in to ")) {
            try {
                driver.findElement(By.id("totp"));
                return true;
            } catch (Throwable t) {
            }
        }
        return false;
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

}
