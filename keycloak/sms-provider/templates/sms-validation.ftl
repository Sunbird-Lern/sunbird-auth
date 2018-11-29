<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("loginTitle",realm.name)}
    <#elseif section = "header">
        ${msg("loginTitleHtml",realm.name)}
    <#elseif section = "form">

        <div class="ui centered grid container">
        <div class="ten wide column signInGridAlign">
        <div class="ui fluid card">

             <div class="ui centered medium image signInLogo margin-top3em">
                <img src="${url.resourcesPath}/img/logo.png">
             </div>
    <div class="content signin-contentPadding">
     <form id="kc-totp-login-form" class="${properties.kcFormClass!} ui form pre-signin" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="field">
                        <div class="${properties.kcLabelWrapperClass!}">
                            <lable for="totp" class="${properties.kcLabelClass!}">Please enter the OTP that has been sent to you</label>
                        </div>
                      <div class="${properties.kcInputWrapperClass!}">
                            <input id="totp" name="smsCode" type="text" class="${properties.kcInputClass!}" />
                        </div>
                </div>

            </div>

            <div class="${properties.kcFormGroupClass!} margin-top2em">

                 <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <button onclick="javascript:makeDivUnclickable()" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!} ui primary right floated button buttonResizeClass" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}">${msg("doLogIn")}</button>
                        <button onclick="javascript:makeDivUnclickable()" class="ui right floated button buttonResizeClass ${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("doCancel")}">${msg("doCancel")}</button>
                    </div>
                </div>
            </div>
        </form>
        </div>
        </div>
        </div>
        </div>

        <#if client?? && client.baseUrl?has_content>
            <p class="content signin-contentPadding"><a id="backToApplication" onclick="javascript:makeDivUnclickable()" href="${client.baseUrl}">${msg("backToApplication")}</a></p>
        </#if>
    </#if>
</@layout.registrationLayout>
