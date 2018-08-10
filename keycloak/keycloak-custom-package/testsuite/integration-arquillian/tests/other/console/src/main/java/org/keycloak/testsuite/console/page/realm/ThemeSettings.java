/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.page.realm;

import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author Filip Kiss
 */
public class ThemeSettings extends RealmSettings {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/theme-settings";
    }

    @FindBy(css = "#loginTheme")
    private Select loginThemeSelect;

    @FindBy(css = "#accountTheme")
    private Select accountThemeSelect;

    @FindBy(css = "#adminTheme")
    private Select adminConsoleThemeSelect;

    @FindBy(css = "#emailTheme")
    private Select emailThemeSelect;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='internationalizationEnabled']]")
    private OnOffSwitch internatEnabledSwitch;

    public void changeLoginTheme(String themeName) {
        loginThemeSelect.selectByVisibleText(themeName);
    }

    public void changeAccountTheme(String themeName) {
        accountThemeSelect.selectByVisibleText(themeName);
    }

    public void changeAdminConsoleTheme(String themeName) {
        adminConsoleThemeSelect.selectByVisibleText(themeName);
    }

    public void changeEmailTheme(String themeName) {
        emailThemeSelect.selectByVisibleText(themeName);
    }

    public void setInternatEnabled(boolean value) {
        internatEnabledSwitch.setOn(value);
    }

    public boolean isInternatEnabled() {
        return internatEnabledSwitch.isOn();
    }

    public void saveTheme() {
        primaryButton.click();
    }

}
