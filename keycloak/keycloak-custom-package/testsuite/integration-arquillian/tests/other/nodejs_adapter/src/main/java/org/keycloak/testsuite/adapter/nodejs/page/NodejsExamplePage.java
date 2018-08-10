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

package org.keycloak.testsuite.adapter.nodejs.page;

import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class NodejsExamplePage extends AbstractNodejsExamplePage {
    @FindBy(xpath = "//div[@class='nav']//a[text()='Login']")
    private WebElement loginButton;

    @FindBy(xpath = "//div[@class='nav']//a[text()='Logout']")
    private WebElement logoutButton;

    @FindBy(id = "output")
    private WebElement outputBox;

    public void clickLogin() {
        loginButton.click();
    }

    public void clickLogout() {
        logoutButton.click();
    }

    public String getOutput() {
        return outputBox.getText();
    }

    public boolean isOnLoginSecuredPage() {
        UriBuilder uriBuilder = createUriBuilder().path("login");
        return URLUtils.currentUrlEqual(driver, uriBuilder.build().toASCIIString());
    }

    @Override
    public boolean isCurrent() {
        return URLUtils.currentUrlStartWith(driver, toString());
    }
}
