package com.serverless.imageprocessor;


import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.pubsub.v1.ProjectSubscriptionName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;



@RestController
public class ImageController {

    @Autowired private ResourceLoader resourceLoader;


    @Autowired private CloudVisionTemplate cloudVisionTemplate;

    @Autowired
    ApplicationContext ctx;

    public static PubSubReceiver newPub = new PubSubReceiver();


    @GetMapping("/extractLabels")
    public ModelAndView extractLabels(ModelMap map) throws IOException {

        GetImage newImage = new GetImage();
        String gcsFilePath = getMessageDetail();

        AnnotateImageResponse setResponse =
                this.cloudVisionTemplate.analyzeImage(
                        this.resourceLoader.getResource(gcsFilePath), Type.LABEL_DETECTION);

        Map<String, Float> imageLabels =
                setResponse
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

        map.addAttribute("annotations", imageLabels);
        map.addAttribute("imageUrl", gcsFilePath);
        newImage.moveImage();

        return new ModelAndView("result", map);
    }

    @GetMapping("/getLabelMLModel")
    public String getLabelMLModel () throws IOException {
        GetImage newImage = new GetImage();
        String getImagePath = newImage.getImageUrl();
        String gcsFilePath = getMessageDetail();

        String getOutputResult = "";
        PreProcessor newP = new PreProcessor();

        String modelDir = "src/inception5h";

        // getting the inception model from GCS bucket
        String modelFile = "gs://buoyant-climate-307017-model-bucket/inception5h/tensorflow_inception_graph.pb";
        Resource modelFileResource = ctx.getResource(modelFile);
        byte[] modelBytes = StreamUtils.copyToByteArray(modelFileResource.getInputStream());

        // getting the image from GCS bucket
        String imageFile = gcsFilePath;
        Resource getImageFileResource = ctx.getResource(imageFile);
        byte[] getImageBytes = StreamUtils.copyToByteArray(getImageFileResource.getInputStream());

        // getting the label list from GCS bucket
        String labelFile = "gs://buoyant-climate-307017-model-bucket/inception5h/imagenet_comp_graph_label_strings.txt";
        Resource labelFileResource = ctx.getResource(labelFile);
        byte[] labelBytes = StreamUtils.copyToByteArray(labelFileResource.getInputStream());

        Utill newUtil = new Utill();
        List <String> getLabelList = newUtil.bytesToStringList(labelBytes);

        try (Tensor<Float> setImage = newP.constructAndExecuteGraphToNormalizeImage(getImageBytes)) {
            float[] getLabelProbabilities = newP.executeInceptionGraph(modelBytes, setImage);
            int getBestLabelIdx = newP.maxIndex(getLabelProbabilities);
            System.out.println(
                    String.format("BEST MATCH: %s (%.2f%% likely)",
                            getLabelList.get(getBestLabelIdx),
                            getLabelProbabilities[getBestLabelIdx] * 100f));
            getOutputResult = String.format("BEST MATCH: %s (%.2f%% likely)",
                    getLabelList.get(getBestLabelIdx),
                    getLabelProbabilities[getBestLabelIdx] * 100f);
        }
        newImage.moveImage();
        return getOutputResult;
    }

    @GetMapping("/extractLandMarkGCS")
    public ModelAndView detectLandMarkGCS (ModelMap map) throws IOException {

        String gcsFilePath = getMessageDetail();
        GetImage newImage = new GetImage();


        AnnotateImageResponse getResponse =
                this.cloudVisionTemplate.analyzeImage(
                        this.resourceLoader.getResource(gcsFilePath), Type.LANDMARK_DETECTION);

        Map<String, Float> getImageLabels =
                getResponse
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

        map.addAttribute("annotations", getImageLabels);
        map.addAttribute("imageUrl", gcsFilePath);

        newImage.moveImage();

        return new ModelAndView("result", map);
    }


    @GetMapping("/detectTextGcs")
    public ModelAndView detectTextGcs(ModelMap map) throws IOException {

        String gcsFilePath = getMessageDetail();
        GetImage newImage = new GetImage();

        List<AnnotateImageRequest> getRequests = new ArrayList<>();

        ImageSource setImageSource = ImageSource.newBuilder().setGcsImageUri(gcsFilePath).build();
        Image img = Image.newBuilder().setSource(setImageSource).build();
        Feature feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest setRequest =
                AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(img).build();
        getRequests.add(setRequest);


        try (ImageAnnotatorClient setClient = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse setResponse = setClient.batchAnnotateImages(getRequests);
            List<AnnotateImageResponse> getResponses = setResponse.getResponsesList();

            for (AnnotateImageResponse getRes : getResponses) {
                if (getRes.hasError()) {
                    System.out.format("Error: %s%n", getRes.getError().getMessage());
                }


                for (EntityAnnotation annotation : getRes.getTextAnnotationsList()) {
                    System.out.format("Text: %s%n", annotation.getDescription());
                    System.out.format("Position : %s%n", annotation.getBoundingPoly());
                }

                Map<String, Float> getImageLabels =
                        getRes
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

                map.addAttribute("annotations", getImageLabels);
                map.addAttribute("imageUrl", gcsFilePath);

                newImage.moveImage();
            }
        }
        return new ModelAndView("result", map);
    }

    @GetMapping ("/getPubSubLabel")
    public ModelAndView getPubSubLabel (ModelMap map) throws IOException {

        String gcsPath = getMessageDetail();
        GetImage newImage = new GetImage();

        System.out.println("Path is :"+ gcsPath);
        AnnotateImageResponse getResponse =
                this.cloudVisionTemplate.analyzeImage(
                        this.resourceLoader.getResource(gcsPath), Type.LABEL_DETECTION);

        Map<String, Float> getImageLabels =
                getResponse
                        .getLabelAnnotationsList()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        EntityAnnotation::getDescription,
                                        EntityAnnotation::getScore,
                                        (u, v) -> {
                                            throw new IllegalStateException(String.format("Duplicated key %s", u));
                                        },
                                        LinkedHashMap::new));
        // [END spring_vision_image_labelling]

        map.addAttribute("annotations", getImageLabels);
        map.addAttribute("imageUrl", gcsPath);
        newImage.moveImage();

        return new ModelAndView("result", map);
    }


    @RequestMapping("/getFaceDetection")
    public String getFaceDetection() throws IOException {

        String getGcsPath = getMessageDetail();
        GetImage newImage = new GetImage();

        Resource setImageResource = this.resourceLoader.getResource(getGcsPath);
        AnnotateImageResponse getResponse = this.cloudVisionTemplate.analyzeImage(
                setImageResource, Feature.Type.FACE_DETECTION);

        writeWithFaces(getGcsPath,getResponse.getFaceAnnotationsList());
        return "Face Detection Completed Successfully";
    }
    public void writeWithFaces(String gcsFilePath,List<FaceAnnotation> faces)
            throws IOException {

        String imageFile = gcsFilePath;
        Resource getImageFileResource = ctx.getResource(imageFile);
        byte[] getImageBytes = StreamUtils.copyToByteArray(getImageFileResource.getInputStream());

        InputStream inputStream = new ByteArrayInputStream(getImageBytes);
        BufferedImage setImage = ImageIO.read(inputStream);
        for (FaceAnnotation face : faces) {
            Graphics2D setGfx = setImage.createGraphics();
            Polygon setPoly = new Polygon();
            for (Vertex getVertex : face.getFdBoundingPoly().getVerticesList()) {
                setPoly.addPoint(getVertex.getX(), getVertex.getY());
            }
            setGfx.setStroke(new BasicStroke(5));
            setGfx.setColor(new Color(0x00ff00));
            setGfx.draw(setPoly);
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(setImage, "jpg", byteArrayOutputStream);
        byte[] finalImageBytes = byteArrayOutputStream.toByteArray();

        Storage setStorage = StorageOptions.newBuilder().setProjectId("buoyant-climate-307017").build().getService();
        BlobId setBlobId = BlobId.of("buoyant-climate-307017-images-output", "face-detect-output.jpg");
        BlobInfo setBlobInfo = BlobInfo.newBuilder(setBlobId).build();
        setStorage.create(setBlobInfo, finalImageBytes);
    }


    public String getMessageDetail() {
            String SET_PROJECT_ID = ServiceOptions.getDefaultProjectId();
            String SET_SUBSCRIPTION_ID = "test-topic-sub";
            ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(SET_PROJECT_ID, SET_SUBSCRIPTION_ID);
            Subscriber subscriber = null ;
            Log log = LogFactory.getLog(ImageprocessorApplication.class);
            log.info(String.format("Project ID is, %s", SET_PROJECT_ID) );
            try {
                subscriber = Subscriber.newBuilder(subscriptionName, newPub).build() ;
                subscriber.startAsync().awaitRunning();
                subscriber.awaitTerminated(10, TimeUnit.SECONDS);
                System.out.println(newPub.getGcsPath());
            } catch (Exception ex) {
                subscriber.stopAsync();
            }
            return newPub.gcsPath;
    }
}
