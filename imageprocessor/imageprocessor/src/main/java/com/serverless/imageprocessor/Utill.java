package com.serverless.imageprocessor;

import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Utill {

    @Autowired
    static
    ApplicationContext ctx;


    public static List<String> bytesToStringList(byte[] bytes) {
        List<String> lines = new ArrayList<String>();

        if (bytes == null) {
            return lines;
        }

        BufferedReader r = null;

        try {
            r = new BufferedReader(
                    new InputStreamReader(
                            new ByteArrayInputStream(bytes),
                            "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // If UTF-8 is not supported we are in big trouble.
            throw new RuntimeException(e);
        }

        try {
            try {
                    for (String line = r.readLine(); line != null; line = r.readLine()) {
                        lines.add(line);
                    }
            } finally {
                r.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return lines;
    }

}


