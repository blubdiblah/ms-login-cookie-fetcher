package util;

import okhttp3.*;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LoginCookieFetcher {

    public static List<String> login(String loginUrl, String user, String password) throws IOException {
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
        Response responseForCookie = client.newCall(requestForCookies).execute();

        //Get sFT, sCtx, canary, sessionId
        assert responseForCookie.body() != null;
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

        return toJsonString(cookieManager.getCookieStore().getCookies(), loginUrl);
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

    private static List<String> toJsonString(List<HttpCookie> cookieList, String baseUrl) {
        if (cookieList.size()<11) {
            throw new RuntimeException("Invalid credentials or password rotation needed!");
        }
        return cookieList.stream()
                .map(httpCookie -> cookieToJsonString(httpCookie, baseUrl))
                .collect(Collectors.toList());
    }

    private static String cookieToJsonString(HttpCookie cookie, String baseUrl) {
        String[] cookieKeyValue = cookie.toString().split("=", 2);
        List<String> microsoftSpecificCookieList = Arrays.asList("fpc", "buid", "ESTSAUTHLIGHT", "ESTCC", "x-ms-gateway-slice", "stsservicecookie");
        String domain;
        if (cookieKeyValue[0].equals("JSESSIONID")) {
            domain = baseUrl;
        } else if (microsoftSpecificCookieList.contains(cookieKeyValue[0])) {
            domain = "login.microsoftonline.com";
        } else {
            domain = ".login.microsoftonline.com";
        }
        return "{\"name\": \"" + cookieKeyValue[0] + "\", \"value\": \"" + cookieKeyValue[1] + "\", \"domain\": \"" + domain + "\"}";
    }
}
