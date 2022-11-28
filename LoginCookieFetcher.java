package util;

import okhttp3.*;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Retrieve authentication cookies from microsoft login as List of HttpCookie.
 * @author Michael Pflueger
 *
 * Add to Selenium for Java using:
 * cookies.forEach(c -> driver.manage().addCookie(new Cookie(c.getName(), c.getValue(), c.getDomain(), c.getPath(), null)));
 *
 */
public class LoginCookieFetcher {

    public static List<HttpCookie> login(String loginUrl, String user, String password) {

        //Setup
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();

        //Get Session Cookie
        Request requestForCookies = new Request.Builder()
                .url(loginUrl)
                .method("GET", null)
                .build();
        try {
            Response responseForCookie = client.newCall(requestForCookies).execute();

            //Get sFT, sCtx, canary, sessionId
            if (responseForCookie.body() == null) {
                return null;
            }
            String responseBody = responseForCookie.body().string();
            String sft = getSftField(responseBody);
            String sctx = getCtxField(responseBody);
            String canary = getCanaryField(responseBody);
            String sessionId = getSessionIdField(responseBody);

            //Post Login
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("login", user)
                    .addFormDataPart("passwd", password)
                    .addFormDataPart("flowToken", sft)
                    .addFormDataPart("type", "11")
                    .addFormDataPart("ctx", sctx)
                    .addFormDataPart("canary", canary)
                    .addFormDataPart("hpgrequestid", sessionId)
                    .build();
            Request requestForLogin = new Request.Builder()
                    .url("https://login.microsoftonline.com/common/login")
                    .method("POST", body)
                    .build();
            client.newCall(requestForLogin).execute();
        } catch (IOException e) {
            return null;
        }
        List<HttpCookie> cookieList = cookieManager.getCookieStore().getCookies();
        return cookieList.stream().map(c -> addCookieDomain(c, loginUrl)).collect(Collectors.toList());
    }

    private static String getCtxField(String responseBody) {

        responseBody = responseBody.substring(responseBody.indexOf("sCtx\":\"") + 7);
        return responseBody.substring(0, responseBody.indexOf("\""));
    }

    private static String getSftField(String responseBody) {

        responseBody = responseBody.substring(responseBody.indexOf("sFT\":\"") + 6);
        return responseBody.substring(0, responseBody.indexOf("\""));
    }

    private static String getCanaryField(String responseBody) {

        responseBody = responseBody.substring(responseBody.indexOf("canary\":\"") + 9);
        return responseBody.substring(0, responseBody.indexOf("\""));
    }

    private static String getSessionIdField(String responseBody) {

        responseBody = responseBody.substring(responseBody.indexOf("sessionId\":\"") + 12);
        return responseBody.substring(0, responseBody.indexOf("\""));
    }

    private static HttpCookie addCookieDomain(HttpCookie cookie, String baseUrl) {

        List<String> microsoftSpecificCookieList = Arrays.asList("fpc", "buid", "ESTSAUTHLIGHT", "ESTCC", "x-ms-gateway-slice", "stsservicecookie");
        if (cookie.getName().equals("JSESSIONID")) {
            cookie.setDomain(baseUrl);
        } else if (microsoftSpecificCookieList.contains(cookie.getName())) {
            cookie.setDomain("login.microsoftonline.com");
        } else {
            cookie.setDomain(".login.microsoftonline.com");
        }
        return cookie;
    }
}
