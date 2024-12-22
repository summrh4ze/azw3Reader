package org.example.azw3reader.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.azw3reader.core.Book;
import org.example.azw3reader.utils.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookReader extends HBox {
    private static final int MIN_PIXELS = 100;
    private static final Logger log = LoggerFactory.getLogger(BookReader.class);
    private final Book book;
    private final ImageView left;
    private final ImageView right;
    private final Image placeholder;
    private final boolean separateCover;
    private final ObjectProperty<Boolean> isShiftDown = new SimpleObjectProperty<>(false);
    private int currentPage;


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
            right.setOnMouseClicked(e -> {
                if (!this.isShiftDown.get()) {
                    this.next(e);
                }
            });
            left.setOnMouseClicked(e -> {
                if (!this.isShiftDown.get()) {
                    this.previous(e);
                }
            });
        } else {
            right.setOnMouseClicked(e -> {
                if (!this.isShiftDown.get()) {
                    this.previous(e);
                }
            });
            left.setOnMouseClicked(e -> {
                if (!this.isShiftDown.get()) {
                    this.next(e);
                }
            });
        }

        parent.getScene().setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.SHIFT) {
                isShiftDown.set(true);
                this.right.setCursor(Cursor.CLOSED_HAND);
                this.left.setCursor(Cursor.CLOSED_HAND);
            }
        });

        parent.getScene().setOnKeyReleased(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.SHIFT) {
                isShiftDown.set(false);
                this.right.setCursor(Cursor.DEFAULT);
                this.left.setCursor(Cursor.DEFAULT);
            }
        });


        displayPages(this.book.getCover());
    }

    private void displayPages(Image current) {
        if (current != null && !current.isError()) {
            this.left.setImage(current);
            setZoomListeners(left);
        } else {
            this.left.setImage(placeholder);
            removeZoomListeners(this.left);
        }
        this.right.setImage(placeholder);
        removeZoomListeners(this.right);
    }

    private void displayPages(Image current, Image next) {
        if (current != null && !current.isError()) {
            if (book.leftToRight()) {
                this.left.setImage(current);
                setZoomListeners(this.left);
            } else {
                this.right.setImage(current);
                setZoomListeners(this.right);
            }
        } else {
            if (book.leftToRight()) {
                this.left.setImage(this.placeholder);
                removeZoomListeners(this.left);
            } else {
                this.right.setImage(this.placeholder);
                removeZoomListeners(this.right);
            }
        }
        if (next != null && !next.isError()) {
            if (book.leftToRight()) {
                this.right.setImage(next);
                setZoomListeners(this.right);
            } else {
                this.left.setImage(next);
                setZoomListeners(this.left);
            }
        } else {
            if (book.leftToRight()) {
                this.right.setImage(this.placeholder);
                removeZoomListeners(this.right);
            } else {
                this.left.setImage(this.placeholder);
                removeZoomListeners(this.left);
            }
        }
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

    private void removeZoomListeners(ImageView imageView) {
        imageView.setViewport(null);
        imageView.setOnMousePressed(null);
        imageView.setOnMouseDragged(null);
        imageView.setOnScroll(null);
    }

    private void setZoomListeners(ImageView imageView) {
        double height = imageView.getImage().getHeight();
        double width = imageView.getImage().getWidth();
        imageView.setViewport(new Rectangle2D(0, 0, width, height));
        ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();
        imageView.setOnMousePressed(e -> {
            if (isShiftDown.get()) {
                Point2D mousePress = ImageUtils.imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
                mouseDown.set(mousePress);
            }
        });

        imageView.setOnMouseDragged(e -> {
            if (isShiftDown.get()) {
                Point2D dragPoint = ImageUtils.imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
                ImageUtils.shift(imageView, dragPoint.subtract(mouseDown.get()));
                mouseDown.set(ImageUtils.imageViewToImage(imageView, new Point2D(e.getX(), e.getY())));
            }
        });

        imageView.setOnScroll(e -> {
            double delta = e.getDeltaY();
            Rectangle2D viewport = imageView.getViewport();

            double scale = ImageUtils.clamp(Math.pow(1.01, delta),

                    // don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
                    Math.min(MIN_PIXELS / viewport.getWidth(), MIN_PIXELS / viewport.getHeight()),

                    // don't scale so that we're bigger than image dimensions:
                    Math.max(width / viewport.getWidth(), height / viewport.getHeight())

            );

            Point2D mouse = ImageUtils.imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));

            double newWidth = viewport.getWidth() * scale;
            double newHeight = viewport.getHeight() * scale;

            // To keep the visual point under the mouse from moving, we need
            // (x - newViewportMinX) / (x - currentViewportMinX) = scale
            // where x is the mouse X coordinate in the image

            // solving this for newViewportMinX gives

            // newViewportMinX = x - (x - currentViewportMinX) * scale

            // we then clamp this value so the image never scrolls out
            // of the imageview:

            double newMinX = ImageUtils.clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale,
                    0, width - newWidth);
            double newMinY = ImageUtils.clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale,
                    0, height - newHeight);

            imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
        });
    }
}
