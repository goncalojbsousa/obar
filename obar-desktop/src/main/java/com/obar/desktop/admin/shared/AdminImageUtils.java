package com.obar.desktop.admin.shared;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminImageUtils {

    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();

    private AdminImageUtils() {
    }

    public static void preload(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return;
        }
        try {
            IMAGE_CACHE.computeIfAbsent(photoUrl, url -> new Image(url, true));
        } catch (IllegalArgumentException ignored) {
            // Invalid URLs keep using the configured fallback.
        }
    }

    public static void showAvatar(ImageView imageView, Label fallback, String photoUrl, double size) {
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(false);
        imageView.setClip(new Circle(size / 2, size / 2, size / 2));
        load(imageView, fallback, photoUrl);
    }

    public static void showVehicle(ImageView imageView, Label fallback, String photoUrl,
            double width, double height) {
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        imageView.setClip(clip);
        load(imageView, fallback, photoUrl);
    }

    private static void load(ImageView imageView, Label fallback, String photoUrl) {
        if (imageView == null || fallback == null) {
            return;
        }
        imageView.setImage(null);
        imageView.setVisible(false);
        fallback.setVisible(true);

        if (photoUrl == null || photoUrl.isBlank()) {
            return;
        }

        Image image;
        try {
            preload(photoUrl);
            image = IMAGE_CACHE.get(photoUrl);
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (image == null) {
            return;
        }
        imageView.setImage(image);
        image.progressProperty().addListener((observable, oldValue, newValue) -> {
            if (imageView.getImage() == image && newValue.doubleValue() >= 1 && !image.isError()) {
                applyCenteredCrop(imageView, image);
                imageView.setVisible(true);
                fallback.setVisible(false);
            }
        });
        image.errorProperty().addListener((observable, oldValue, hasError) -> {
            if (imageView.getImage() == image && Boolean.TRUE.equals(hasError)) {
                imageView.setVisible(false);
                fallback.setVisible(true);
            }
        });
        if (image.getProgress() >= 1 && !image.isError()) {
            applyCenteredCrop(imageView, image);
            imageView.setVisible(true);
            fallback.setVisible(false);
        }
    }

    private static void applyCenteredCrop(ImageView imageView, Image image) {
        double targetRatio = imageView.getFitWidth() / imageView.getFitHeight();
        double imageRatio = image.getWidth() / image.getHeight();
        double width = image.getWidth();
        double height = image.getHeight();
        double x = 0;
        double y = 0;

        if (imageRatio > targetRatio) {
            width = height * targetRatio;
            x = (image.getWidth() - width) / 2;
        } else {
            height = width / targetRatio;
            y = (image.getHeight() - height) / 2;
        }
        imageView.setViewport(new Rectangle2D(x, y, width, height));
    }
}
