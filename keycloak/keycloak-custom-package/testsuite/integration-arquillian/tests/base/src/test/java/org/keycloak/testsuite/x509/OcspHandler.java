/*
 * Copyright 2017 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */
package org.keycloak.testsuite.x509;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPRespBuilder;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;
import org.bouncycastle.cert.ocsp.Req;
import org.bouncycastle.cert.ocsp.RespID;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

final class OcspHandler implements HttpHandler {

    private static final String OCSP_RESPONDER_CERT_PATH = "/client-auth-test/intermediate-ca.crt";

    private static final String OCSP_RESPONDER_KEYPAIR_PATH = "/client-auth-test/intermediate-ca.key";

    // add any certificates that the OCSP responder needs to know about in the tests here
    private static final Map<BigInteger, CertificateStatus> REVOKED_CERTIFICATES_STATUS = ImmutableMap
            .of(BigInteger.valueOf(4096), new RevokedStatus(new Date(1472169600000L), CRLReason.unspecified));

    private final SubjectPublicKeyInfo subjectPublicKeyInfo;

    private final X509CertificateHolder[] chain;

    private final AsymmetricKeyParameter privateKey;

    OcspHandler() throws OperatorCreationException, GeneralSecurityException, IOException {
        final Certificate certificate = CertificateFactory.getInstance("X509")
                .generateCertificate(X509OCSPResponderTest.class.getResourceAsStream(OCSP_RESPONDER_CERT_PATH));

        chain = new X509CertificateHolder[] {new X509CertificateHolder(certificate.getEncoded())};

        final AsymmetricKeyParameter publicKey = PublicKeyFactory.createKey(certificate.getPublicKey().getEncoded());

        subjectPublicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey);

        final InputStream keyPairStream = X509OCSPResponderTest.class.getResourceAsStream(OCSP_RESPONDER_KEYPAIR_PATH);

        try (final PEMParser keyPairReader = new PEMParser(new InputStreamReader(keyPairStream))) {
            final PEMKeyPair keyPairPem = (PEMKeyPair) keyPairReader.readObject();
            privateKey = PrivateKeyFactory.createKey(keyPairPem.getPrivateKeyInfo());
        }
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        final byte[] buffy = new byte[16384];
        try (InputStream requestStream = exchange.getInputStream()) {
            requestStream.read(buffy);
        }

        final OCSPReq request = new OCSPReq(buffy);
        final Req[] requested = request.getRequestList();

        final Extension nonce = request.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);

        final DigestCalculator sha1Calculator = new JcaDigestCalculatorProviderBuilder().build()
                .get(AlgorithmIdentifier.getInstance(RespID.HASH_SHA1));

        final BasicOCSPRespBuilder responseBuilder = new BasicOCSPRespBuilder(subjectPublicKeyInfo, sha1Calculator);

        if (nonce != null) {
            responseBuilder.setResponseExtensions(new Extensions(nonce));
        }

        for (final Req req : requested) {
            final CertificateID certId = req.getCertID();

            final BigInteger certificateSerialNumber = certId.getSerialNumber();
            responseBuilder.addResponse(certId, REVOKED_CERTIFICATES_STATUS.get(certificateSerialNumber));
        }

        final ContentSigner contentSigner = new BcRSAContentSignerBuilder(
                new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption),
                new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256)).build(privateKey);

        final OCSPResp response = new OCSPRespBuilder().build(OCSPResp.SUCCESSFUL,
                responseBuilder.build(contentSigner, chain, new Date()));

        final byte[] responseBytes = response.getEncoded();

        final HeaderMap responseHeaders = exchange.getResponseHeaders();
        responseHeaders.put(Headers.CONTENT_TYPE, "application/ocsp-response");

        final Sender responseSender = exchange.getResponseSender();
        responseSender.send(ByteBuffer.wrap(responseBytes));

        exchange.endExchange();
    }
}
