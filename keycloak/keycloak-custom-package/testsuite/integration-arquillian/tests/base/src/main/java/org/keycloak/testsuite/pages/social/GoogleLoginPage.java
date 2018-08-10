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

package org.keycloak.testsuite.pages.social;

import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class GoogleLoginPage extends AbstractSocialLoginPage {
    @FindBy(xpath = ".//p[@role='heading'][1]")
    private WebElement firstAccount;

    @FindBy(id = "identifierId")
    private WebElement emailInput;

    @FindBy(xpath = ".//input[@type='password']")
    private WebElement passwordInput;

    @Override
    public void login(String user, String password) {
        try {
            firstAccount.click();
        }
        catch (NoSuchElementException e) {
            emailInput.sendKeys(user);
            emailInput.sendKeys(Keys.RETURN);
        }

        passwordInput.sendKeys(password);
        passwordInput.sendKeys(Keys.RETURN);
    }
}
