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

package org.keycloak.models.utils;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultRequiredActions {
    public static void addActions(RealmModel realm) {
        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.VERIFY_EMAIL.name()) == null) {
            RequiredActionProviderModel verifyEmail = new RequiredActionProviderModel();
            verifyEmail.setEnabled(true);
            verifyEmail.setAlias(UserModel.RequiredAction.VERIFY_EMAIL.name());
            verifyEmail.setName("Verify Email");
            verifyEmail.setProviderId(UserModel.RequiredAction.VERIFY_EMAIL.name());
            verifyEmail.setDefaultAction(false);
            realm.addRequiredActionProvider(verifyEmail);

        }

        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.UPDATE_PROFILE.name()) == null) {
            RequiredActionProviderModel updateProfile = new RequiredActionProviderModel();
            updateProfile.setEnabled(true);
            updateProfile.setAlias(UserModel.RequiredAction.UPDATE_PROFILE.name());
            updateProfile.setName("Update Profile");
            updateProfile.setProviderId(UserModel.RequiredAction.UPDATE_PROFILE.name());
            updateProfile.setDefaultAction(false);
            realm.addRequiredActionProvider(updateProfile);
        }

        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.CONFIGURE_TOTP.name()) == null) {
            RequiredActionProviderModel totp = new RequiredActionProviderModel();
            totp.setEnabled(true);
            totp.setAlias(UserModel.RequiredAction.CONFIGURE_TOTP.name());
            totp.setName("Configure OTP");
            totp.setProviderId(UserModel.RequiredAction.CONFIGURE_TOTP.name());
            totp.setDefaultAction(false);
            realm.addRequiredActionProvider(totp);
        }

        if (realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name()) == null) {
            RequiredActionProviderModel updatePassword = new RequiredActionProviderModel();
            updatePassword.setEnabled(true);
            updatePassword.setAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            updatePassword.setName("Update Password");
            updatePassword.setProviderId(UserModel.RequiredAction.UPDATE_PASSWORD.name());
            updatePassword.setDefaultAction(false);
            realm.addRequiredActionProvider(updatePassword);
        }

        if (realm.getRequiredActionProviderByAlias("terms_and_conditions") == null) {
            RequiredActionProviderModel termsAndConditions = new RequiredActionProviderModel();
            termsAndConditions.setEnabled(false);
            termsAndConditions.setAlias("terms_and_conditions");
            termsAndConditions.setName("Terms and Conditions");
            termsAndConditions.setProviderId("terms_and_conditions");
            termsAndConditions.setDefaultAction(false);
            realm.addRequiredActionProvider(termsAndConditions);
        }


    }
}
