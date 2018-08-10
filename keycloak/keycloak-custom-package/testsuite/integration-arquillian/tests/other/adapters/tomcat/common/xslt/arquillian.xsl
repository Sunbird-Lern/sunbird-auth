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
                xmlns:a="http://jboss.org/schema/arquillian"
                version="2.0"
                exclude-result-prefixes="xalan a">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="/a:arquillian">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
            
            <container qualifier="app-server-${{app.server}}" mode="manual" >
                <configuration>
                    <property name="enabled">true</property>
                    <property name="adapterImplClass">org.jboss.arquillian.container.tomcat.managed_7.TomcatManagedContainer</property>
                    <property name="catalinaHome">${app.server.home}</property>
                    <property name="catalinaBase">${app.server.home}</property>
                    <property name="bindHttpPort">${app.server.http.port}</property>
                    <property name="jmxPort">${app.server.management.port}</property>
                    <property name="user">manager</property>
                    <property name="pass">arquillian</property>
                    <property name="javaVmArguments">${adapter.test.props}</property>
                </configuration>
            </container>
    
        </xsl:copy>
    </xsl:template>
    

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
    

</xsl:stylesheet>