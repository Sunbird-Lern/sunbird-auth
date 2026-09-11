<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <div class="spark-form-pane">
            <div class="sunbird-logo-wrapper">
                <img src="${url.resourcesPath}/img/sunbird-logo.svg" alt="Sunbird" class="sunbird-logo-img" onerror="this.src='${url.resourcesPath}/img/sunbird-logo.svg'">
            </div>
            <h1 class="page-title">${msg("logoutConfirmTitle")! "Logout Confirmation"}</h1>
            <p class="page-subtitle">${msg("logoutConfirmText")! "Are you sure you want to log out?"}</p>
            <div class="kc-form-buttons">
                <form action="${url.logout}" method="post" style="display:inline;">
                    <button type="submit" class="kc-button">${msg("doLogOut")! "Yes"}</button>
                </form>
                <a class="kc-button kc-button-outline" href="${url.cancel}">${msg("doCancel")! "No"}</a>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
