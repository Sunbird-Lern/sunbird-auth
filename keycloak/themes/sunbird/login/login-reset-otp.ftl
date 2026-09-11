<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "form">
        <div class="spark-form-pane validation-pane">
            <div class="sunbird-logo-wrapper">
                <img src="${url.resourcesPath}/img/sunbird-logo.svg" alt="Sunbird" class="sunbird-logo-img" onerror="this.src='${url.resourcesPath}/img/sunbird-logo.svg'">
            </div>
            <h1 class="page-title text-center">Enter the code</h1>
            <p class="page-subtitle text-center">Enter the 6 digit code sent to your Email ID and complete the verification</p>
            <#if message?has_content>
                <div class="alert alert-${message.type} text-center mb-4">
                    <span class="kc-feedback-text">${message.summary}</span>
                </div>
            </#if>
            <p class="otp-validity-text text-center">OTP is valid for 30 minutes</p>
            <form id="kc-totp-login-form" class="kc-form" action="${url.loginAction}" method="post">
                <div class="otp-container">
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required oninput="this.value = this.value.replace(/[^0-9]/g, ''); focusNext(this)" onkeydown="handleBackspace(this, event)">
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required oninput="this.value = this.value.replace(/[^0-9]/g, ''); focusNext(this)" onkeydown="handleBackspace(this, event)">
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required oninput="this.value = this.value.replace(/[^0-9]/g, ''); focusNext(this)" onkeydown="handleBackspace(this, event)">
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required oninput="this.value = this.value.replace(/[^0-9]/g, ''); focusNext(this)" onkeydown="handleBackspace(this, event)">
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required oninput="this.value = this.value.replace(/[^0-9]/g, ''); focusNext(this)" onkeydown="handleBackspace(this, event)">
                    <input type="text" class="otp-input" maxlength="1" pattern="[0-9]" inputmode="numeric" required oninput="this.value = this.value.replace(/[^0-9]/g, ''); collectOtp()" onkeydown="handleBackspace(this, event)">
                    <input id="totp" name="smsCode" type="hidden" required pattern="^[0-9]{6}$" />
                </div>
                <div class="kc-form-buttons">
                    <input class="kc-button block" type="submit" value="Submit"/>
                </div>
            </form>
            <script>
                function focusNext(el) {
                    if (el.value.length === 1) {
                        var next = el.nextElementSibling;
                        if (next && next.classList.contains('otp-input')) {
                            next.focus();
                        }
                    }
                }
                function handleBackspace(el, event) {
                    if (event.key === 'Backspace' && el.value.length === 0) {
                        var prev = el.previousElementSibling;
                        if (prev && prev.classList.contains('otp-input')) {
                            prev.focus();
                        }
                    }
                }
                function collectOtp() {
                    var inputs = document.querySelectorAll('.otp-input');
                    var otp = '';
                    inputs.forEach(function(input){ otp += input.value; });
                    document.getElementById('totp').value = otp;
                }
            </script>
        </div>
    </#if>
</@layout.registrationLayout>
