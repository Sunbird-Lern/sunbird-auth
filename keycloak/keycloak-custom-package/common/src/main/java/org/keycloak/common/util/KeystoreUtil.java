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

package org.keycloak.common.util;

import org.keycloak.common.constants.GenericConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeystoreUtil {
    static {
        BouncyIntegration.init();
    }

    public enum KeystoreFormat {
        JKS,
        PKCS12
    }

    public static KeyStore loadKeyStore(String filename, String password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream trustStream = (filename.startsWith(GenericConstants.PROTOCOL_CLASSPATH))
                ?KeystoreUtil.class.getResourceAsStream(filename.replace(GenericConstants.PROTOCOL_CLASSPATH, ""))
                :new FileInputStream(new File(filename));
        trustStore.load(trustStream, password.toCharArray());
        trustStream.close();
        return trustStore;
    }

    public static KeyPair loadKeyPairFromKeystore(String keystoreFile, String storePassword, String keyPassword, String keyAlias, KeystoreFormat format) {
        InputStream stream = FindFile.findFile(keystoreFile);

        try {
            KeyStore keyStore = null;
            if (format == KeystoreFormat.JKS) {
                keyStore = KeyStore.getInstance(format.toString());
            } else {
                keyStore = KeyStore.getInstance(format.toString(), "BC");
            }

            keyStore.load(stream, storePassword.toCharArray());
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
            if (privateKey == null) {
                throw new RuntimeException("Couldn't load key with alias '" + keyAlias + "' from keystore");
            }
            PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key: " + e.getMessage(), e);
        }
    }
}
