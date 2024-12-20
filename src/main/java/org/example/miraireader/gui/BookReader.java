package org.example.miraireader.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.miraireader.core.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

public class BookReader extends HBox {
    private static final Logger log = LoggerFactory.getLogger(BookReader.class);
    private static final int MIN_PIXELS = 10;
    private final Book book;
    private final Node parent;
    private int currentIndex = 0;


    public BookReader(Node parent, String title, Book book) {
        this.parent = parent;
        this.book = book;
        Stage s =(Stage)parent.getScene().getWindow();
        String currentTitle = s.getTitle();
        s.setTitle(currentTitle + " - " + this.book.getTitle());
        displayPages();
    }

    private ImageView getPage(int index) {
        int height = (int)this.parent.getBoundsInParent().getHeight();
        Image image = new Image(new ByteArrayInputStream(this.book.getPage(index)));
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(height);
        return imageView;
    }

    private void displayPages() {
        if (this.currentIndex == 0) {
            ImageView imageView = getPage(this.currentIndex);
            this.getChildren().add(imageView);

            imageView.setOnMouseClicked(this::next);
            this.getChildren().clear();
            this.getChildren().add(imageView);
            return;
        }
        // right-to-left TODO: handle left-to-right
        ImageView right = getPage(currentIndex);
        ImageView left = getPage(currentIndex + 1);
        right.setOnMouseClicked(this::previous);
        left.setOnMouseClicked(this::next);

        setListeners(right);
        setListeners(left);

        this.getChildren().clear();
        this.getChildren().addAll(left, right);
    }

    private void next(MouseEvent mouseEvent) {
        this.currentIndex += 2;
        displayPages();
    }

    private void previous(MouseEvent mouseEvent) {
        this.currentIndex -= 2;
        displayPages();
    }

    private void setListeners(ImageView imageView) {
        double height = imageView.getImage().getHeight();
        double width = imageView.getImage().getWidth();
        imageView.setViewport(new Rectangle2D(0, 0, width, height));
        ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();
        imageView.setOnMousePressed(e -> {
            Point2D mousePress = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
            mouseDown.set(mousePress);
        });

        imageView.setOnMouseDragged(e -> {
            Point2D dragPoint = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
            shift(imageView, dragPoint.subtract(mouseDown.get()));
            mouseDown.set(imageViewToImage(imageView, new Point2D(e.getX(), e.getY())));
        });

        imageView.setOnScroll(e -> {
            double delta = e.getDeltaY();
            Rectangle2D viewport = imageView.getViewport();

            double scale = clamp(Math.pow(1.01, delta),

                    // don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
                    Math.min(MIN_PIXELS / viewport.getWidth(), MIN_PIXELS / viewport.getHeight()),

                    // don't scale so that we're bigger than image dimensions:
                    Math.max(width / viewport.getWidth(), height / viewport.getHeight())

            );

            Point2D mouse = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));

            double newWidth = viewport.getWidth() * scale;
            double newHeight = viewport.getHeight() * scale;

            // To keep the visual point under the mouse from moving, we need
            // (x - newViewportMinX) / (x - currentViewportMinX) = scale
            // where x is the mouse X coordinate in the image

            // solving this for newViewportMinX gives

            // newViewportMinX = x - (x - currentViewportMinX) * scale

            // we then clamp this value so the image never scrolls out
            // of the imageview:

            double newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale,
                    0, width - newWidth);
            double newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale,
                    0, height - newHeight);

            imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
        });
    }

    // shift the viewport of the imageView by the specified delta, clamping so
    // the viewport does not move off the actual image:
    private void shift(ImageView imageView, Point2D delta) {
        Rectangle2D viewport = imageView.getViewport();

        double width = imageView.getImage().getWidth() ;
        double height = imageView.getImage().getHeight() ;

        double maxX = width - viewport.getWidth();
        double maxY = height - viewport.getHeight();

        double minX = clamp(viewport.getMinX() - delta.getX(), 0, maxX);
        double minY = clamp(viewport.getMinY() - delta.getY(), 0, maxY);

        imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
    }

    private double clamp(double value, double min, double max) {

        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    // convert mouse coordinates in the imageView to coordinates in the actual image:
    private Point2D imageViewToImage(ImageView imageView, Point2D imageViewCoordinates) {
        double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
        double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

        Rectangle2D viewport = imageView.getViewport();
        return new Point2D(
                viewport.getMinX() + xProportion * viewport.getWidth(),
                viewport.getMinY() + yProportion * viewport.getHeight());
    }
}
