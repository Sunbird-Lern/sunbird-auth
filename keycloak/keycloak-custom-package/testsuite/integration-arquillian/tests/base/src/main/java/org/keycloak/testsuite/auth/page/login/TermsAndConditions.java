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

import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TermsAndConditions extends LoginActions {

    @FindBy(id = "kc-accept")
    private WebElement acceptButton;

    @FindBy(id = "kc-decline")
    private WebElement declineButton;

    @FindBy(id = "kc-terms-text")
    private WebElement textElem;
    
    @Override
    public boolean isCurrent() {
        return UIUtils.currentTitleEquals(driver, "Terms and Conditions");
    }

    public void acceptTerms() {
        acceptButton.click();
        WaitUtils.waitForPageToLoad(driver);
    }
    public void declineTerms() {
        declineButton.click();
        WaitUtils.waitForPageToLoad(driver);
    }

    public String getAcceptButtonText() {
        return acceptButton.getAttribute("value");
    }
    
    public String getDeclineButtonText() {
        return declineButton.getAttribute("value");
    }
    
    public String getText() {
        return textElem.getText();
    }
    
}
