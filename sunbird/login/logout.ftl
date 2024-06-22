<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
<#if section = "title">
    ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
    <#elseif section = "form">
    <#if realm.password>
    <div class="fullpage-background-image">
    <div class="container-wrapper">
                <div class="ui header centered mb-8">
                    <img onerror="" alt="">
                    <p class="subtitle">Logout Page</p>
                </div>
               <p class="instruction mb-0 textCenter">${msg("logoutConfirmHeader")}</p>
    </div>
    </div>
    </#if>
</#if>
</@layout.registrationLayout>