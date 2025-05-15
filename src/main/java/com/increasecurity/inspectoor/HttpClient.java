package com.increasecurity.inspectoor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;


@Slf4j
public class HttpClient {

    public static final String JSON = "application/json";
    public static final MediaType JSON_MEDIA_TYPE = MediaType.get(JSON);

    private HttpClient() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Upload project meta data
     * @param json
     * @param url
     * @param apiKey
     */
    public static void doPostRequest(String json, String url, String apiKey) {
        log.info("doPostRequest URL {}", url);
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, json);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);

        Request request = requestBuilder.header("Content-Type", JSON)
                .header("X-INSPECTOOR-APIKEY", apiKey).build();

        try (Response response = getClient().newCall(request).execute()) {
            log.info("Response Code: {}", response.code());
        } catch (IOException e) {
            log.error("Exception in doPostRequest", e);
        }
    }

    /**
     * Upload sbom json file
     * @param sbomFileName
     * @param system
     * @param projectName
     * @param url
     * @param apiKey
     */
    public static void uploadSBOM(String sbomFileName, String system, String projectName, String url, String apiKey) {
        log.info("uploadSBOM URL ={}", url);
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("projectName", projectName)
                .addFormDataPart("system", system)
                .addFormDataPart("bom", "bom.json",
                        RequestBody.create(new File(sbomFileName), JSON_MEDIA_TYPE))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-INSPECTOOR-APIKEY", apiKey)
                .addHeader("Accept", JSON)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            log.info("Response Code: {}", response.code());
        } catch (IOException e) {
            log.error("Exception in uploadSBOM ", e);
        }
    }

    private static OkHttpClient getClient() {
        return new OkHttpClient().newBuilder().build();
    }

}
