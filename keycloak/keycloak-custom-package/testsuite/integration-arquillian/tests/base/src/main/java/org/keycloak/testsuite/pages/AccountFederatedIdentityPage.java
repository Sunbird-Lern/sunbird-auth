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

import org.keycloak.services.Urls;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccountFederatedIdentityPage extends AbstractAccountPage {

    @FindBy(className = "alert-error")
    private WebElement errorMessage;

    public AccountFederatedIdentityPage() {};

    private String realmName = "test";

    public void open() {
        driver.navigate().to(getPath());
    }

    public void realm(String realmName) {
        this.realmName = realmName;
    }

    public String getPath() {
        return Urls.accountFederatedIdentityPage(getAuthServerRoot(), realmName).toString();
    }

    @Override
    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getPageSource().contains("Federated Identities");
    }
    
    public WebElement findAddProviderButton(String alias) {
        return driver.findElement(By.id("add-" + alias));
    }
    
    public WebElement findRemoveProviderButton(String alias) {
        return driver.findElement(By.id("remove-" + alias));
    }

    public void clickAddProvider(String alias) {
        WebElement addButton = findAddProviderButton(alias);
        addButton.click();
    }

    public void clickRemoveProvider(String alias) {
        WebElement addButton = findRemoveProviderButton(alias);
        addButton.click();
    }

    public String getError() {
        return errorMessage.getText();
    }
}
