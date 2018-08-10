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

import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.Constants;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountUpdateProfilePage extends AbstractAccountPage {

    public static String PATH = RealmsResource.accountUrl(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT)).build("test").toString();

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;


    @FindBy(id = "referrer")
    private WebElement backToApplicationLink;

    @FindBy(css = "button[type=\"submit\"][value=\"Save\"]")
    private WebElement submitButton;

    @FindBy(css = "button[type=\"submit\"][value=\"Cancel\"]")
    private WebElement cancelButton;

    @FindBy(className = "alert-success")
    private WebElement successMessage;

    @FindBy(className = "alert-error")
    private WebElement errorMessage;

    public void updateProfile(String firstName, String lastName, String email) {
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);
        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);
        emailInput.clear();
        emailInput.sendKeys(email);

        submitButton.click();
    }

    public void updateProfile(String username, String firstName, String lastName, String email) {
        usernameInput.clear();
        usernameInput.sendKeys(username);
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);
        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);
        emailInput.clear();
        emailInput.sendKeys(email);

        submitButton.click();
    }

    public void updateUsername(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);
        submitButton.click();
    }

    public void updateEmail(String email) {
        emailInput.clear();
        emailInput.sendKeys(email);
        submitButton.click();
    }

    public void clickCancel() {
        cancelButton.click();
    }


    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public String getFirstName() {
        return firstNameInput.getAttribute("value");
    }

    public String getLastName() {
        return lastNameInput.getAttribute("value");
    }

    public String getEmail() {
        return emailInput.getAttribute("value");
    }

    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getPageSource().contains("Edit Account");
    }

    public void open() {
        driver.navigate().to(PATH);
    }

    public void backToApplication() {
        backToApplicationLink.click();
    }

    public String getSuccess(){
        return successMessage.getText();
    }

    public String getError() {
        return errorMessage.getText();
    }

    public boolean isPasswordUpdateSupported() {
        return driver.getPageSource().contains(PATH + "/password");
    }
}
