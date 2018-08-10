<!--
  ~ Copyright 2016 Red Hat, Inc. and/or its affiliates
  ~ and other contributors as indicated by the @author tags.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:j="urn:jboss:domain:4.1"
                xmlns:ds="urn:jboss:domain:datasources:4.0"
                xmlns:k="urn:jboss:domain:keycloak:1.1"
                xmlns:sec="urn:jboss:domain:security:1.2"
                xmlns:u="urn:jboss:domain:undertow:3.1"
                version="2.0"
                exclude-result-prefixes="xalan j ds k sec">

    <xsl:param name="config"/>

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="//j:security-realms">
        <xsl:copy>
            <xsl:apply-templates select="node()[name(.)='security-realm']"/>
            <security-realm name="UndertowRealm">
                <server-identities>
                    <ssl>
                        <keystore path="keycloak.jks" relative-to="jboss.server.config.dir" keystore-password="secret"/>
                    </ssl>
                </server-identities>
            </security-realm>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="//u:http-listener">
        <http-listener name="default" socket-binding="http" redirect-socket="proxy-https" proxy-address-forwarding="true"/>
    </xsl:template>
    <xsl:template match="//u:host">
        <https-listener name="https" socket-binding="proxy-https" security-realm="UndertowRealm"/>
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="//j:socket-binding[@name='http']">
         <xsl:copy-of select="."/>
         <socket-binding name="proxy-https" port="8443"/>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>