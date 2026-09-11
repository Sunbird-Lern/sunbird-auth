<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "form">
        <div class="spark-form-pane">
            <div class="sunbird-logo-wrapper">
                <img src="${url.resourcesPath}/img/sunbird-logo.svg" alt="Sunbird" class="sunbird-logo-img" onerror="this.src='${url.resourcesPath}/img/sunbird-logo.svg'">
            </div>
            <h1 class="page-title">${msg("errorTitle")! "We're sorry..."}</h1>
            <#if message?has_content>
                <div class="alert alert-error">
                    <span class="kc-feedback-text">${message.summary}</span>
                </div>
            </#if>
            <div class="kc-form-buttons">
                <#if client?? && client.baseUrl?has_content>
                    <a id="backToApplication" class="kc-button" href="${client.baseUrl}">${msg("backToApplication")}</a>
                </#if>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
