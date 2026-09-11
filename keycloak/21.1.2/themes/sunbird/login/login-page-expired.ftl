<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("pageExpiredTitle")}
    <#elseif section = "form">
        <div class="spark-form-pane">
            <div class="sunbird-logo-wrapper">
                <img src="${url.resourcesPath}/img/sunbird-logo.svg" alt="Sunbird" class="sunbird-logo-img" onerror="this.src='${url.resourcesPath}/img/sunbird-logo.svg'">
            </div>
            <h1 class="page-title">${msg("pageExpiredTitle")}</h1>
            <p class="page-subtitle">${msg("pageExpiredMsg1")}</p>
            <div class="kc-form-buttons">
                <a id="loginRestartLink" class="kc-button" href="${url.loginRestartFlowUrl}">${msg("doClickHere")}</a>
            </div>
            <p class="page-subtitle" style="margin-top:16px;">${msg("pageExpiredMsg2")}</p>
            <div class="kc-form-buttons">
                <a id="loginContinueLink" class="kc-button kc-button-outline" href="${url.loginAction}">${msg("doClickHere")}</a>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
