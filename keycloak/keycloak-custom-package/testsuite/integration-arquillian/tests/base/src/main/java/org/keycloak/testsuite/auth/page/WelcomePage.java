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

package org.keycloak.testsuite.auth.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class WelcomePage extends AuthServer {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "passwordConfirmation")
    private WebElement passwordConfirmationInput;

    @FindBy(id = "create-button")
    private WebElement createButton;

    public boolean isPasswordSet() {
        return !(driver.getPageSource().contains("Please create an initial admin user to get started.") ||
                 driver.getPageSource().contains("You need local access to create the initial admin user."));
    }

    public void setPassword(String username, String password) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        passwordConfirmationInput.clear();
        passwordConfirmationInput.sendKeys(password);

        createButton.click();

        if (!driver.getPageSource().contains("User created")) {
            throw new RuntimeException("Failed to updated password");
        }
    }

    public void navigateToAdminConsole() {
        driver.findElement(By.linkText("Administration Console")).click();
    }
    
}
