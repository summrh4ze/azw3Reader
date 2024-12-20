package org.example.miraireader.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ImageUtils {
    public static Image generatePlaceholder() {
        WritableImage placeholder = new WritableImage(1, 1);
        PixelWriter pw = placeholder.getPixelWriter();
        Color color = Color.color(1, 1, 1, 1);
        pw.setColor(0, 0, color);
        return placeholder;
    }
}
