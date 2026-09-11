<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <div class="spark-form-pane validation-pane">
            <div class="sunbird-logo-wrapper">
                <img src="${url.resourcesPath}/img/sunbird-logo.svg" alt="Sunbird" class="sunbird-logo-img" onerror="this.src='https://raw.githubusercontent.com/sunbird-ed/sunbird-ed-portal/master/src/assets/images/sunbird_logo.png'">
            </div>
            <h1 class="page-title text-center">Verification Error</h1>
            <#if message?has_content>
                <div class="alert alert-${message.type} text-center mb-4">
                    <span class="kc-feedback-text">${message.summary}</span>
                </div>
            </#if>
            <p class="page-subtitle text-center">Please request a new code and try again.</p>
            <div class="kc-form-buttons">
                <a class="kc-button" href="${url.loginAction}">${msg("doClickHere")! "Back"}</a>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
