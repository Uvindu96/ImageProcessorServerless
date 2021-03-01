package com.serverless.imageprocessor;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.vision.CloudVisionTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.core.io.Resource;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.CopyWriter;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@RestController
public class ImageController {

    @Autowired private ResourceLoader resourceLoader;


    @Autowired private CloudVisionTemplate cloudVisionTemplate;


    @GetMapping("/extractLabels")
    public ModelAndView extractLabels(ModelMap map) {

        String fileName = "";
        String bucketName = "stone-semiotics-297911-images-input";
        String projectId = "stone-semiotics-297911";
        String targetBucket = "stone-semiotics-297911-images-output";

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        Bucket bucket = storage.get(bucketName);
        Page<Blob> blobs = bucket.list();

        String gcsPath = String.format("gs://%s/%s", bucketName, fileName);

        for (Blob blob : blobs.iterateAll()) {
            System.out.println("Object Name:"+blob.getName());
            fileName = blob.getName();
            gcsPath = String.format("gs://%s/%s", bucketName, blob.getName());
        }

        AnnotateImageResponse response =
                this.cloudVisionTemplate.analyzeImage(
                        this.resourceLoader.getResource(gcsPath), Type.LABEL_DETECTION);

        Map<String, Float> imageLabels =
                response
                        .getLabelAnnotationsList()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        EntityAnnotation::getDescription,
                                        EntityAnnotation::getScore,
                                        (u, v) -> {
                                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                                        },
                                        LinkedHashMap::new));
        // [END spring_vision_image_labelling]

        map.addAttribute("annotations", imageLabels);
        map.addAttribute("imageUrl", gcsPath);

        Blob blob = storage.get(bucketName, fileName);
        // Write a copy of the object to the target bucket
        String targetFileName = fileName + "-analyzed";
        CopyWriter copyWriter = blob.copyTo(targetBucket, targetFileName);
        Blob copiedBlob = copyWriter.getResult();
        // Delete the original blob now that we've copied to where we want it, finishing the "move"
        // operation
        blob.delete();

        return new ModelAndView("result", map);
    }


    @GetMapping("/extractLandMarkGCS")
    public ModelAndView detectLandMarkGCS (ModelMap map) {

        String fileName = "";
        String bucketName = "stone-semiotics-297911-images-input";
        String projectId = "stone-semiotics-297911";
        String targetBucket = "stone-semiotics-297911-images-output";

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        Bucket bucket = storage.get(bucketName);
        Page<Blob> blobs = bucket.list();

        String gcsPath = String.format("gs://%s/%s", bucketName, fileName);

        for (Blob blob : blobs.iterateAll()) {
            System.out.println("Object Name:"+blob.getName());
            fileName = blob.getName();
            gcsPath = String.format("gs://%s/%s", bucketName, blob.getName());
        }

        AnnotateImageResponse response =
                this.cloudVisionTemplate.analyzeImage(
                        this.resourceLoader.getResource(gcsPath), Type.LANDMARK_DETECTION);

        Map<String, Float> imageLabels =
                response
                        .getLandmarkAnnotationsList()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        EntityAnnotation::getDescription,
                                        EntityAnnotation::getScore,
                                        (u, v) -> {
                                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                                        },
                                        LinkedHashMap::new));
        // [END spring_vision_image_labelling]

        map.addAttribute("annotations", imageLabels);
        map.addAttribute("imageUrl", gcsPath);

        Blob blob = storage.get(bucketName, fileName);
        // Write a copy of the object to the target bucket
        String targetFileName = fileName + "-analyzed";
        CopyWriter copyWriter = blob.copyTo(targetBucket, targetFileName);
        Blob copiedBlob = copyWriter.getResult();
        // Delete the original blob now that we've copied to where we want it, finishing the "move"
        // operation
        blob.delete();

        return new ModelAndView("result", map);
    }


    @GetMapping("/detectTextGcs")
    public ModelAndView detectTextGcs(ModelMap map) throws IOException {

        String fileName = "";
        String bucketName = "stone-semiotics-297911-images-input";
        String projectId = "stone-semiotics-297911";
        String targetBucket = "stone-semiotics-297911-images-output";

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        Bucket bucket = storage.get(bucketName);
        Page<Blob> blobs = bucket.list();

        String gcsPath = String.format("gs://%s/%s", bucketName, fileName);

        for (Blob blob : blobs.iterateAll()) {
            System.out.println("Object Name:"+blob.getName());
            fileName = blob.getName();
            gcsPath = String.format("gs://%s/%s", bucketName, blob.getName());
        }

        List<AnnotateImageRequest> requests = new ArrayList<>();

        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);


        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                }


                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    System.out.format("Text: %s%n", annotation.getDescription());
                    System.out.format("Position : %s%n", annotation.getBoundingPoly());
                }

                Map<String, Float> imageLabels =
                        res
                                .getTextAnnotationsList()
                                .stream()
                                .collect(
                                        Collectors.toMap(
                                                EntityAnnotation::getDescription,
                                                EntityAnnotation::getScore,
                                                (u, v) -> {
                                                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                                                },
                                                LinkedHashMap::new));
                // [END spring_vision_image_labelling]

                map.addAttribute("annotations", imageLabels);
                map.addAttribute("imageUrl", gcsPath);

                Blob blob = storage.get(bucketName, fileName);
                // Write a copy of the object to the target bucket
                String targetFileName = fileName + "-analyzed";
                CopyWriter copyWriter = blob.copyTo(targetBucket, targetFileName);
                Blob copiedBlob = copyWriter.getResult();
                // Delete the original blob now that we've copied to where we want it, finishing the "move"
                // operation
                blob.delete();
            }
        }
        return new ModelAndView("result", map);
    }
}
