package com.serverless.imageprocessor;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;

import java.io.IOException;

public class GetImage {

    private static final String PROJECT_ID = "buoyant-climate-307017";
    private static Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
    private static final String TARGET_BUCKET_NAME = "buoyant-climate-307017-images-output";
    private static final String INPUT_BUCKET_NAME = "buoyant-climate-307017-images-input";


    public String getImageUrl () throws IOException {
        String fileName = "";
        Storage setStorage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
        Bucket bucket = setStorage.get(INPUT_BUCKET_NAME);
        Page<Blob> blobs = bucket.list();

        String gcsPath = String.format("gs://%s/%s", INPUT_BUCKET_NAME, fileName);

        for (Blob blob : blobs.iterateAll()) {
            System.out.println("Object Name:"+blob.getName());
            fileName = blob.getName();
            gcsPath = String.format("gs://%s/%s", INPUT_BUCKET_NAME, blob.getName());
        }
        return gcsPath;
    }

    public void moveImage () {

        String fileName = "";
        Storage setStorage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
        Bucket bucket = setStorage.get(INPUT_BUCKET_NAME);
        Page<Blob> blobs = bucket.list();

        String gcsPath = String.format("gs://%s/%s",INPUT_BUCKET_NAME, fileName);

        for (Blob blob : blobs.iterateAll()) {
            System.out.println("Object Name:"+blob.getName());
            fileName = blob.getName();
        }

        Blob blob = storage.get(INPUT_BUCKET_NAME, fileName);
        String targetFileName = blob.getName() + "-analyzed";
        CopyWriter copyWriter = blob.copyTo(TARGET_BUCKET_NAME, targetFileName);
        Blob copiedBlob = copyWriter.getResult();
        blob.delete();
    }
}
