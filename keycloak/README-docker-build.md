# Sunbird Keycloak Setup


## Configuration Values
Here are the configuration values pulled from `keycloak.conf`:
- **Database Type**: `db=postgres`
- **Database Username**: `db-username=postgres`
- **Database Password**: `db-password=postgres`
- **Database URL**: `db-url=jdbc:postgresql://localhost:5432/keycloak?sslmode=require`
- **HTTP Relative Path**: `http-relative-path=/auth`

## Configuration Values with Placeholders

Any placeholders in the pattern `{{ .Values.<key> }}` in `imports/sunbird-realm.json` need to be filled with appropriate values during local setup.

## Docker Build Command
To build the Docker image, use the following command:
```bash
docker build -t my-keycloak-image .
```

---

## Internationalization (i18n)

### Overview
Keycloak login pages support 4 locales: English (`en`), French (`fr`), Arabic (`ar`), and Portuguese Brazil (`pt_BR`). The locale is synced from the portal/mobile app so users see Keycloak pages in their selected language.

### How It Works

1. Portal stores the user's selected language in `localStorage('app-language')`
2. When Keycloak loads a login page, `template.ftl` runs an inline JS script that:
   - Reads `localStorage('app-language')`
   - Maps it to a Keycloak locale code (e.g. `pt` -> `pt_BR`)
   - Sets the `KEYCLOAK_LOCALE` cookie
   - Reloads the page so Keycloak renders in the correct locale
3. On the second load, the cookie already matches, so no redirect happens
4. For the mobile app (InAppBrowser), the language is passed via a `?lang=` URL param to the portal, which writes it to `localStorage` before redirecting to Keycloak

### Realm Configuration

In `imports/sunbird-realm.json`:
- `internationalizationEnabled` must be `true`
- `supportedLocales` must include `["en", "fr", "ar", "pt_BR"]`
- `defaultLocale` should be `"en"`

### Theme Structure

```
themes/sunbird/login/
  template.ftl              -- Master layout, locale redirect script, RTL support
  login.ftl                 -- Login form page
  login-reset-password.ftl  -- Forgot password page
  login-update-password.ftl -- Set new password page
  messages/
    messages_en.properties  -- English translations
    messages_fr.properties  -- French translations
    messages_ar.properties  -- Arabic translations
    messages_pt_BR.properties -- Portuguese (Brazil) translations
  resources/css/
    login.css               -- Styles including RTL overrides
```

### FTL i18n Pattern

All user-facing strings in FTL templates use Keycloak's `${msg('key')}` syntax. The key is looked up in the corresponding `messages_XX.properties` file based on the active locale.

For strings used inside `<script>` blocks, use:
```
'${msg("key")?js_string}'
```
The `?js_string` filter escapes quotes and special characters for safe embedding in JavaScript.

### RTL (Arabic) Support

When the locale is Arabic (`ar`):
- `template.ftl` sets `dir="rtl"` on the `<html>` element
- Noto Sans Arabic font is loaded via Google Fonts
- CSS RTL overrides in `login.css` flip layout direction (close button, password toggle, border-radius, text alignment, etc.)

### Adding a New Locale

1. Add the locale code to `theme.properties`: `locales=en,fr,ar,pt_BR,NEW_LOCALE`
2. Create `messages/messages_NEW_LOCALE.properties` with translated keys
3. Add the locale to `sunbird-realm.json` `supportedLocales` array
4. Add the mapping in `template.ftl` JS: `var map = { ..., xx:'NEW_LOCALE' };`
5. If the new locale is RTL, add it to the `isRTL` check in `template.ftl`
6. Rebuild and restart the Keycloak Docker container

### Locale Code Mapping

| Portal / Mobile app code | Keycloak code |
|---|---|
| en | en |
| fr | fr |
| ar | ar |
| pt | pt_BR |