package org.example.miraireader.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ImageUtils {
    private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

    public static Image generatePlaceholder(double width, double height) {
        double ratio = height / width;
        int w = 2;
        int h = (int)Math.ceil(w * ratio);
        log.info("creating placeholder with width {} and height {} and ratio {}", w, h, ratio);
        WritableImage placeholder = new WritableImage(w, h);
        PixelWriter pw = placeholder.getPixelWriter();
        // Should really verify 0.0 <= red, green, blue, opacity <= 1.0
        int alpha = 255;
        int r = 255;
        int g = 255;
        int b = 255;

        int pixel = (alpha << 24) | (r << 16) | (g << 8) | b ;
        int[] pixels = new int[w * h];
        Arrays.fill(pixels, pixel);

        pw.setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
        return placeholder;
    }
}
