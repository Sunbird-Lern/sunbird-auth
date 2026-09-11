<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        <div class="login-header">
            <div class="sunbird-logo">
                <img src="${url.resourcesPath}/img/sunbird-logo.svg" alt="Sunbird Logo" class="sunbird-logo-img"/>
            </div>
            <h1 class="welcome-title">Reset link</h1>
            <p class="welcome-subtitle">Email verification sent</p>
        </div>
    <#elseif section = "form">
        <div id="kc-form">
          <div id="kc-form-wrapper">
            <form id="kc-totp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label class="${properties.kcLabelClass!}" style="text-align: center; display: block;">Please check your registered email for reset link is sent!</label>
                    </div>
                </div>
            </form>
          </div>
        </div>
    <#elseif section = "info" >
        <div class="registration-link">
            <#if client?? && client.baseUrl?has_content>
                <a id="backToApplication" href="${client.baseUrl}">${msg("backToApplication")}</a>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>
