package com.example.nexthire;

import android.content.Context;
import android.net.Uri;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;

public class CloudinaryUploader {

    private static final String CLOUD_NAME = "zfuq164c";       // from Cloudinary Dashboard
    private static final String UPLOAD_PRESET = "nexthire_unsigned";  // from Upload preset step

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onError(String errorMessage);
    }

    public static void uploadPhoto(Context context, Uri imageUri, UploadCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            byte[] bytes = inputStream.readAllBytes();
            inputStream.close();

            OkHttpClient client = new OkHttpClient();
            String url = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

            RequestBody fileBody = RequestBody.create(bytes, MediaType.parse("image/*"));

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "profile.jpg", fileBody)
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("Upload failed: " + response.code());
                        return;
                    }
                    String body = response.body().string();
                    try {
                        String secureUrl = new JSONObject(body).getString("secure_url");
                        callback.onSuccess(secureUrl);
                    } catch (Exception e) {
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                }
            });

        } catch (IOException e) {
            callback.onError("Could not read image: " + e.getMessage());
        }
    }
}