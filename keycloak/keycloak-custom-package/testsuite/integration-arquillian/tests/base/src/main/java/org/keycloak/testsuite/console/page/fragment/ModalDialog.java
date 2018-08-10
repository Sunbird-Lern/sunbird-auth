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

package org.keycloak.testsuite.console.page.fragment;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.fragment.Root;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.WaitUtils.waitForModalFadeIn;
import static org.keycloak.testsuite.util.WaitUtils.waitForModalFadeOut;

/**
 *
 * @author tkyjovsk
 */
public class ModalDialog {

    @Root
    private WebElement root;

    @Drone
    private WebDriver driver;

    @FindBy(xpath = ".//button[text()='Cancel']")
    private WebElement cancelButton;
    @FindBy(xpath = ".//button[text()='Delete']")
    private WebElement deleteButton;

    @FindBy(xpath = ".//button[@ng-click='ok()']")
    private WebElement okButton;
    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(className = "modal-body")
    private WebElement message;

    public void ok() {
        waitForModalFadeIn(driver);
        okButton.click();
        waitForModalFadeOut(driver);
    }
    
    public void confirmDeletion() {
        waitForModalFadeIn(driver);
        deleteButton.click();
        waitForModalFadeOut(driver);
    }

    public void cancel() {
        waitForModalFadeIn(driver);
        cancelButton.click();
        waitForModalFadeOut(driver);
    }

    public void setName(String name) {
        nameInput.clear();
        nameInput.sendKeys(name);
    }

    public WebElement getMessage() {
        return message;
    }
}