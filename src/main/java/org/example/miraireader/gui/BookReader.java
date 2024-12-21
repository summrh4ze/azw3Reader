package org.example.miraireader.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.miraireader.core.Book;
import org.example.miraireader.utils.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookReader extends HBox {
    private static final int MIN_PIXELS = 10;
    private static final Logger log = LoggerFactory.getLogger(BookReader.class);
    private final Book book;
    private final ImageView left;
    private final ImageView right;
    private final Image placeholder;
    private final boolean separateCover;
    private int currentPage = 0;


    public BookReader(HBox parent, Book book) {
        this.book = book;
        Image page = book.getPage(0);
        this.placeholder = ImageUtils.generatePlaceholder(page.getWidth(), page.getHeight());
        this.separateCover = book.getCoverIndex() > 0;
        this.currentPage = this.separateCover ? -1 : 0;
        Stage s = (Stage)parent.getScene().getWindow();
        String currentTitle = s.getTitle();
        String appTitle = currentTitle.split("-")[0].trim();
        s.setTitle(appTitle + " - " + this.book.getTitle());

        int height = (int)parent.getBoundsInParent().getHeight();
        this.right = new ImageView();
        right.setFitHeight(height);
        right.setPreserveRatio(true);
        this.left = new ImageView();
        left.setFitHeight(height);
        left.setPreserveRatio(true);
        this.getChildren().addAll(this.left, this.right);

        parent.heightProperty().addListener((_, _, newHeight) -> {
            right.setFitHeight((double)newHeight);
            left.setFitHeight((double)newHeight);
        });

        if (book.leftToRight()) {
            right.setOnMouseClicked(this::next);
            left.setOnMouseClicked(this::previous);
        } else {
            right.setOnMouseClicked(this::previous);
            left.setOnMouseClicked(this::next);
        }
        displayPages(this.book.getCover());
    }

    private void displayPages(Image current) {
        if (current == null || current.isError()) {
            current = this.placeholder;
        }
        this.left.setImage(current);
        this.right.setImage(placeholder);
        //setListeners(left);
        //setListeners(right);
    }

    private void displayPages(Image current, Image next) {
        if (current == null || current.isError()) {
            current = this.placeholder;
        }
        if (next == null || current.isError()) {
            next = this.placeholder;
        }
        if (book.leftToRight()) {
            this.left.setImage(current);
            this.right.setImage(next);
        } else {
            this.left.setImage(next);
            this.right.setImage(current);
        }
        //setListeners(right);
        //setListeners(left);
    }

    private void next(MouseEvent mouseEvent) {
        int pageCount = this.book.getPageCount();
        boolean canMoveNext = false;
        if (this.currentPage == -1 || (this.currentPage == 0 && this.currentPage + 1 < pageCount)) {
            this.currentPage += 1;
            canMoveNext = true;
        } else if (this.currentPage > 0 && this.currentPage + 2 < pageCount) {
            this.currentPage += 2;
            canMoveNext = true;
        }
        if (canMoveNext) {
            Image current = this.book.getPage(this.currentPage);
            if (this.currentPage == 0) {
                displayPages(current);
                return;
            }
            Image next = this.book.getPage(this.currentPage + 1);
            if ((current != null && !current.isError()) || (next != null && !next.isError())) {
                displayPages(current, next);
            } else {
                currentPage -= 2;
            }
        }
    }

    private void previous(MouseEvent mouseEvent) {
        if (this.currentPage - 1 == -1 && this.separateCover) {
            this.currentPage -= 1;
            Image cover = this.book.getCover();
            displayPages(cover);
        } else if (this.currentPage - 1 == 0) {
            this.currentPage -= 1;
            if (this.separateCover) {
                Image current = this.book.getPage(this.currentPage);
                displayPages(current);
            } else {
                Image cover = this.book.getCover();
                displayPages(cover);
            }
        } else if (this.currentPage - 1 > 0) {
            this.currentPage -= 2;
            Image current = this.book.getPage(this.currentPage);
            Image next = this.book.getPage(this.currentPage + 1);
            displayPages(current, next);
        }
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
