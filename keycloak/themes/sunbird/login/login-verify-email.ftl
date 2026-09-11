<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <div class="spark-form-pane">
            <div class="sunbird-logo-wrapper">
                <img src="${url.resourcesPath}/img/sunbird-logo.svg" alt="Sunbird" class="sunbird-logo-img" onerror="this.src='https://raw.githubusercontent.com/sunbird-ed/sunbird-ed-portal/master/src/assets/images/sunbird_logo.png'">
            </div>
            <h1 class="page-title">Reset link</h1>
            <p class="page-subtitle">Email verification sent</p>
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
            <div class="registration-link">
                <#if client?? && client.baseUrl?has_content>
                    <a id="backToApplication" href="${client.baseUrl}">${msg("backToApplication")}</a>
                </#if>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
