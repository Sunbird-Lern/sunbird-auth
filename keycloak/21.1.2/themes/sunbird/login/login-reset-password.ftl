<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
    <#elseif section = "form">
        <div class="spark-form-pane">
            
            <div class="sunbird-logo-wrapper">
                <img src="${url.resourcesPath}/img/sunbird-logo.svg" alt="Sunbird" class="sunbird-logo-img" onerror="this.src='https://raw.githubusercontent.com/sunbird-ed/sunbird-ed-portal/master/src/assets/images/sunbird_logo.png'">
            </div>
            
            <div>
                <h1 class="page-title">${msg("emailForgotTitle")}</h1>
                <p class="page-subtitle">${msg("forgotPasswordSubtitle")}</p>

                <#if message?has_content>
                    <div class="alert alert-${message.type}">
                        <span class="kc-feedback-text">${message.summary}</span>
                    </div>
                </#if>

                <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
                    <div class="kc-form-group">
                        <label for="username" class="kc-label">${msg("emailormobile")}*</label>
                        <div class="input-wrapper">
                            <input type="text" id="username" name="username" class="kc-input" placeholder="${msg("emailormobilePlaceholder")}" autofocus autocomplete="username" required/>
                        </div>
                    </div>

                    <div class="kc-form-buttons">
                        <input id="login" class="kc-button" type="submit" value="${msg("doSubmit")}"/>
                    </div>
                </form>
            </div>
        </div>
    <#elseif section = "info" >
    </#if>
</@layout.registrationLayout>
