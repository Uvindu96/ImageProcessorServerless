package com.serverless.imageprocessor;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.vision.CloudVisionTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.tensorflow.Tensor;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@RestController
public class ImageController {

    @Autowired private ResourceLoader resourceLoader;


    @Autowired private CloudVisionTemplate cloudVisionTemplate;

    @GetMapping("/getLabelMLModel")
    public String getLabelMLModel () {

        GetImage newImage = new GetImage();
        String getImagePath = newImage.getImageUrl();

        String getOutput = "";
        PreProcessor newP = new PreProcessor();

        String modelDir = "src/inception5h";
        String imageFile = getImagePath;

        byte[] graphDef = newP.readAllBytesOrExit(Paths.get(modelDir, "tensorflow_inception_graph.pb"));
        List<String> labels =
                newP.readAllLinesOrExit(Paths.get(modelDir, "imagenet_comp_graph_label_strings.txt"));
        byte[] imageBytes = newP.readAllBytesOrExit(Paths.get(imageFile));

        try (Tensor<Float> image = newP.constructAndExecuteGraphToNormalizeImage(imageBytes)) {
            float[] labelProbabilities = newP.executeInceptionGraph(graphDef, image);
            int bestLabelIdx = newP.maxIndex(labelProbabilities);
            System.out.println(
                    String.format("BEST MATCH: %s (%.2f%% likely)",
                            labels.get(bestLabelIdx),
                            labelProbabilities[bestLabelIdx] * 100f));
                    getOutput = String.format("BEST MATCH: %s (%.2f%% likely)",
                    labels.get(bestLabelIdx),
                    labelProbabilities[bestLabelIdx] * 100f);
        }
        return getOutput;
    }

    @GetMapping("/extractLabels")
    public ModelAndView extractLabels(ModelMap map) {

        GetImage newImage = new GetImage();
        String gcsPath = newImage.getImageUrl();

        AnnotateImageResponse response =
                this.cloudVisionTemplate.analyzeImage(
                        this.resourceLoader.getResource(newImage.getImageUrl()), Type.LABEL_DETECTION);

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

        newImage.moveImage();

        return new ModelAndView("result", map);
    }

    @GetMapping("/extractLandMarkGCS")
    public ModelAndView detectLandMarkGCS (ModelMap map) {

        GetImage newImage = new GetImage();
        String gcsPath = newImage.getImageUrl();

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

        newImage.moveImage();

        return new ModelAndView("result", map);
    }


    @GetMapping("/detectTextGcs")
    public ModelAndView detectTextGcs(ModelMap map) throws IOException {

        GetImage newImage = new GetImage();
        String gcsPath = newImage.getImageUrl();

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

                newImage.moveImage();
            }
        }
        return new ModelAndView("result", map);
    }
}
