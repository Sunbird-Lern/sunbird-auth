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

package org.keycloak.credential.hash;

import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.UserCredentialModel;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * @author <a href="mailto:me@tsudot.com">Kunal Kerkar</a>
 */
public class Pbkdf2PasswordHashProvider implements PasswordHashProvider {

    private final String providerId;

    private final String pbkdf2Algorithm;
    private int defaultIterations;

    public static final int DERIVED_KEY_SIZE = 512;

    public Pbkdf2PasswordHashProvider(String providerId, String pbkdf2Algorithm, int defaultIterations) {
        this.providerId = providerId;
        this.pbkdf2Algorithm = pbkdf2Algorithm;
        this.defaultIterations = defaultIterations;
    }

    @Override
    public boolean policyCheck(PasswordPolicy policy, CredentialModel credential) {
        return credential.getHashIterations() == policy.getHashIterations() && providerId.equals(credential.getAlgorithm());
    }

    @Override
    public void encode(String rawPassword, int iterations, CredentialModel credential) {
        if (iterations == -1) {
            iterations = defaultIterations;
        }

        byte[] salt = getSalt();
        String encodedPassword = encode(rawPassword, iterations, salt);

        credential.setAlgorithm(providerId);
        credential.setType(UserCredentialModel.PASSWORD);
        credential.setSalt(salt);
        credential.setHashIterations(iterations);
        credential.setValue(encodedPassword);
    }

    @Override
    public boolean verify(String rawPassword, CredentialModel credential) {
        return encode(rawPassword, credential.getHashIterations(), credential.getSalt()).equals(credential.getValue());
    }

    public void close() {
    }

    private String encode(String rawPassword, int iterations, byte[] salt) {
        KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, DERIVED_KEY_SIZE);

        try {
            byte[] key = getSecretKeyFactory().generateSecret(spec).getEncoded();
            return Base64.encodeBytes(key);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Credential could not be encoded", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private byte[] getSalt() {
        byte[] buffer = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(buffer);
        return buffer;
    }

    private SecretKeyFactory getSecretKeyFactory() {
        try {
            return SecretKeyFactory.getInstance(pbkdf2Algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("PBKDF2 algorithm not found", e);
        }
    }
}
