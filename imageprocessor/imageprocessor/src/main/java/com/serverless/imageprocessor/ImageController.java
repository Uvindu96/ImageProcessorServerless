package com.serverless.imageprocessor;


import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.vision.CloudVisionTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.ModelMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.tensorflow.Tensor;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;



@RestController
public class ImageController {

    @Autowired private ResourceLoader resourceLoader;


    @Autowired private CloudVisionTemplate cloudVisionTemplate;

    @Autowired
    ApplicationContext ctx;


    @GetMapping("/extractLabels")
    public ModelAndView extractLabels(ModelMap map) throws IOException {

        GetImage newImage = new GetImage();
        //String gcsPath = newImage.getImageUrl();
        PubSubReceiver newMsg = ImageprocessorApplication.newPub;
        String gcsPath = newMsg.getGcsPath();

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

       newImage.moveImage();

        return new ModelAndView("result", map);
    }

    @GetMapping("/getLabelMLModel")
    public String getLabelMLModel () throws IOException {
        GetImage newImage = new GetImage();
        String getImagePath = newImage.getImageUrl();
        PubSubReceiver newMsg = ImageprocessorApplication.newPub;
        String gcsPath = newMsg.getGcsPath();

        String getOutput = "";
        PreProcessor newP = new PreProcessor();

        String modelDir = "src/inception5h";

        // getting the inception model from GCS bucket
        String modelFile = "gs://buoyant-climate-307017-model-bucket/inception5h/tensorflow_inception_graph.pb";
        Resource modelFileResource = ctx.getResource(modelFile);
        byte[] modelBytes = StreamUtils.copyToByteArray(modelFileResource.getInputStream());

        // getting the image from GCS bucket
        String imageFile = gcsPath;
        Resource imageFileResource = ctx.getResource(imageFile);
        byte[] imageBytes = StreamUtils.copyToByteArray(imageFileResource.getInputStream());

        // getting the label list from GCS bucket
        String labelFile = "gs://buoyant-climate-307017-model-bucket/inception5h/imagenet_comp_graph_label_strings.txt";
        Resource labelFileResource = ctx.getResource(labelFile);
        byte[] labelBytes = StreamUtils.copyToByteArray(labelFileResource.getInputStream());

        Utill newUtil = new Utill();
        List <String> getLabelList = newUtil.bytesToStringList(labelBytes);


    //byte[] graphDef = newP.readAllBytesOrExit(Paths.get(modelDir, "tensorflow_inception_graph.pb"));
        //List<String> labels = newP.readAllLinesOrExit(Paths.get(modelDir, "imagenet_comp_graph_label_strings.txt"));
        //byte[] imageBytes = newP.readAllBytesOrExit(Paths.get(imageFile));

        try (Tensor<Float> image = newP.constructAndExecuteGraphToNormalizeImage(imageBytes)) {
            float[] labelProbabilities = newP.executeInceptionGraph(modelBytes, image);
            int bestLabelIdx = newP.maxIndex(labelProbabilities);
            System.out.println(
                    String.format("BEST MATCH: %s (%.2f%% likely)",
                            getLabelList.get(bestLabelIdx),
                            labelProbabilities[bestLabelIdx] * 100f));
            getOutput = String.format("BEST MATCH: %s (%.2f%% likely)",
                    getLabelList.get(bestLabelIdx),
                    labelProbabilities[bestLabelIdx] * 100f);
        }
        return getOutput;
    }

    @GetMapping("/extractLandMarkGCS")
    public ModelAndView detectLandMarkGCS (ModelMap map) throws IOException {

        PubSubReceiver newMsg = ImageprocessorApplication.newPub;
        String gcsPath = newMsg.getGcsPath();
        GetImage newImage = new GetImage();
        //String gcsPath = newImage.getImageUrl();

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

        PubSubReceiver newMsg = ImageprocessorApplication.newPub;
        String gcsPath = newMsg.getGcsPath();
        GetImage newImage = new GetImage();
        //String gcsPath = newImage.getImageUrl();

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

    @GetMapping ("/getPubSubLabel")
    public ModelAndView getPubSubLabel (ModelMap map) throws IOException {

        PubSubReceiver newMsg = ImageprocessorApplication.newPub;
        String gcsPath = newMsg.getGcsPath();
        GetImage newImage = new GetImage();

        System.out.println("Path is :"+ gcsPath);
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
        newImage.moveImage();

        return new ModelAndView("result", map);
    }
}
