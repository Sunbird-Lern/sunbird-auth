<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "header">
        <div class="login-header">
            <div class="sunbird-logo">
                <img src="${url.resourcesPath}/img/sunbird-logo.svg" alt="Sunbird Logo" class="sunbird-logo-img"/>
            </div>
            <h1 class="welcome-title">${msg("loginSunbird")}</h1>
            <p class="welcome-subtitle">${msg("loginSubtitle")}</p>
        </div>
    <#elseif section = "form">
    <div id="kc-form">
      <div id="kc-form-wrapper">
        
        <#-- Google Sign In Button -->
        <a href="#" id="googleSignInBtn" class="google-signin-btn" onclick="navigate('google'); return false;" aria-label="${msg('signIn')} ${msg('doSignWithGoogle')}">
            <img src="${url.resourcesPath}/img/google-icon.svg" alt="Google" class="google-icon" />
            <span>${msg("signIn")} ${msg("doSignWithGoogle")}</span>
        </a>
            
        <#-- OR Divider -->
        <div class="or-divider">
            <span>${msg("orDivider")}</span>
        </div>
        
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <label for="emailormobile" class="${properties.kcLabelClass!}">${msg("emailormobile")}</label>

                    <#if usernameEditDisabled??>
                        <input tabindex="1" id="emailormobile" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}" type="text" disabled placeholder="${msg("emailormobilePlaceholder")}" />
                    <#else>
                        <input tabindex="1" id="emailormobile" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}" onfocusin="inputBoxFocusIn(this)" onfocusout="inputBoxFocusOut(this)" type="text" autofocus autocomplete="username" placeholder="${msg("emailormobilePlaceholder")}" />
                    </#if>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
                    <div class="password-wrapper">
                        <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" onfocusin="inputBoxFocusIn(this)" onfocusout="inputBoxFocusOut(this)" autocomplete="current-password" placeholder="${msg("passwordPlaceholder")}" />
                        <button type="button" class="password-toggle" onclick="togglePassword()" aria-label="${msg("togglePasswordVisibility")}">
                            <svg id="eye-icon" width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M10 4C4.5 4 1.5 10 1.5 10C1.5 10 4.5 16 10 16C15.5 16 18.5 10 18.5 10C18.5 10 15.5 4 10 4Z" stroke="#666" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                                <circle cx="10" cy="10" r="3" stroke="#666" stroke-width="1.5"/>
                            </svg>
                        </button>
                    </div>
                </div>

                <div class="forgot-password">
                    <#if realm.resetPasswordAllowed>
                        <a id="fgtKeycloakFlow" class="hide" tabindex="3" onclick="javascript:storeLocation(); javascript:makeDivUnclickable(); javascript:storeForgotPasswordLocation(event);" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
                        <a id="fgtPortalFlow" class="hide" tabindex="3" href="#" onclick="javascript:makeDivUnclickable(); javascript:createTelemetryEvent(event); javascript:forgetPassword('/forgot-password'); return false;">${msg("doForgotPassword")}</a>
                    </#if>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                    <button tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} login-button" name="login" id="kc-login" type="submit" onclick="doLogin(event)">${msg("doLogIn")}</button>
                </div>
            </form>
        </#if>
        </div>
      </div>
    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !usernameEditDisabled??>
            <div id="kc-registration" class="registration-link">
                <span>${msg("registerNewUser")} <a tabindex="5" onclick=navigate('self')>${msg("doRegister")}</a> ${msg("registerToContinue")}</span>
            </div>
        </#if>
    </#if>

    <#-- JavaScript for password toggle -->
    <script>
        function togglePassword() {
            const passwordInput = document.getElementById('password');
            const eyeIcon = document.getElementById('eye-icon');
            
            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                eyeIcon.innerHTML = '<path d="M10 4C4.5 4 1.5 10 1.5 10C1.5 10 4.5 16 10 16C15.5 16 18.5 10 18.5 10C18.5 10 15.5 4 10 4Z" stroke="#666" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="10" cy="10" r="3" stroke="#666" stroke-width="1.5"/><line x1="2" y1="2" x2="18" y2="18" stroke="#666" stroke-width="1.5" stroke-linecap="round"/>';
            } else {
                passwordInput.type = 'password';
                eyeIcon.innerHTML = '<path d="M10 4C4.5 4 1.5 10 1.5 10C1.5 10 4.5 16 10 16C15.5 16 18.5 10 18.5 10C18.5 10 15.5 4 10 4Z" stroke="#666" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="10" cy="10" r="3" stroke="#666" stroke-width="1.5"/>';
            }
        }
    </script>

</@layout.registrationLayout>
