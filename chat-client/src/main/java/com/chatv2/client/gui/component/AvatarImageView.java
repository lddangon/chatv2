package com.chatv2.client.gui.component;

import javafx.css.PseudoClass;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Custom Control for displaying circular avatar images.
 * Supports both image avatars and placeholder avatars with initials.
 */
public class AvatarImageView extends StackPane {

    private static final PseudoClass PLACEHOLDER_PSEUDO_CLASS = PseudoClass.getPseudoClass("placeholder");

    private ImageView imageView;
    private StackPane placeholderPane;
    private Text placeholderText;
    private Circle clipCircle;

    private AvatarSize size = AvatarSize.MEDIUM;
    private byte[] avatarData;
    private String username;
    private String initial;

    /**
     * Default constructor with MEDIUM size.
     */
    public AvatarImageView() {
        this(AvatarSize.MEDIUM);
    }

    /**
     * Constructor with specified size.
     *
     * @param size the size of the avatar
     */
    public AvatarImageView(AvatarSize size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null");
        }
        this.size = size;
        initialize();
    }

    /**
     * Initialize the component.
     */
    private void initialize() {
        getStyleClass().add("avatar-image-view");

        // Create clip for circular shape
        clipCircle = new Circle();
        setClip(clipCircle);

        // Create image view
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        getChildren().add(imageView);

        // Create placeholder
        placeholderPane = new StackPane();
        placeholderPane.getStyleClass().add("avatar-placeholder");
        placeholderText = new Text();
        placeholderText.setFont(Font.font(18));
        placeholderText.setFill(Color.WHITE);
        placeholderPane.getChildren().add(placeholderText);
        placeholderPane.setVisible(false);
        getChildren().add(placeholderPane);

        updateSize();
    }

    /**
     * Set the avatar image from byte data.
     *
     * @param imageData the image data as byte array
     */
    public void setAvatar(byte[] imageData) {
        this.avatarData = imageData;
        if (imageData != null && imageData.length > 0) {
            try {
                Image image = new Image(new java.io.ByteArrayInputStream(imageData));
                imageView.setImage(image);
                showImage();
            } catch (Exception e) {
                // If image loading fails, show placeholder
                showPlaceholder();
            }
        } else {
            imageView.setImage(null);
            showPlaceholder();
        }
    }

    /**
     * Get the current avatar data.
     *
     * @return the avatar data as byte array
     */
    public byte[] getAvatar() {
        return avatarData;
    }

    /**
     * Set the username for placeholder generation.
     *
     * @param username the username to generate initial from
     */
    public void setUsername(String username) {
        this.username = username;
        this.initial = extractInitial(username);
        placeholderText.setText(this.initial);
        updatePlaceholderColor();
    }

    /**
     * Get the current username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the size of the avatar.
     *
     * @param size the size to set
     */
    public void setSize(AvatarSize size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null");
        }
        if (this.size != size) {
            this.size = size;
            updateSize();
        }
    }

    /**
     * Get the current size.
     *
     * @return the current size
     */
    public AvatarSize getSize() {
        return size;
    }

    /**
     * Update the size based on current AvatarSize.
     */
    private void updateSize() {
        int pixelSize = size.getPixels();
        setPrefSize(pixelSize, pixelSize);
        setMinSize(pixelSize, pixelSize);
        setMaxSize(pixelSize, pixelSize);

        // Update clip
        clipCircle.setRadius(pixelSize / 2.0);
        clipCircle.setCenterX(pixelSize / 2.0);
        clipCircle.setCenterY(pixelSize / 2.0);

        // Update placeholder text font size based on avatar size
        double fontSize = pixelSize * 0.45;
        placeholderText.setFont(Font.font(fontSize));
    }

    /**
     * Show the image view and hide placeholder.
     */
    private void showImage() {
        imageView.setVisible(true);
        placeholderPane.setVisible(false);
        pseudoClassStateChanged(PLACEHOLDER_PSEUDO_CLASS, false);
    }

    /**
     * Show the placeholder and hide image view.
     */
    private void showPlaceholder() {
        imageView.setVisible(false);
        placeholderPane.setVisible(true);
        pseudoClassStateChanged(PLACEHOLDER_PSEUDO_CLASS, true);
        updatePlaceholderColor();
    }

    /**
     * Extract the first character from username as initial.
     *
     * @param username the username
     * @return the first character, or "?" if username is null/empty
     */
    private String extractInitial(String username) {
        if (username == null || username.isEmpty()) {
            return "?";
        }
        // Take first non-whitespace character, converted to uppercase
        return username.trim().substring(0, 1).toUpperCase();
    }

    /**
     * Update the placeholder background color based on username hash.
     * This ensures consistent colors for the same username.
     */
    private void updatePlaceholderColor() {
        if (username != null) {
            // Generate a consistent color from username hash
            int hash = username.hashCode();
            // Use HSL-like colors for better visibility
            double hue = Math.abs(hash % 360);
            Color color = Color.hsb(hue, 0.7, 0.5);
            placeholderPane.setStyle("-fx-background-color: " + toRgbString(color) + ";");
        } else {
            // Default gray color
            placeholderPane.setStyle("-fx-background-color: #666666;");
        }
    }

    /**
     * Convert Color to CSS RGB string.
     *
     * @param color the color
     * @return the RGB string in format "rgb(r, g, b)"
     */
    private String toRgbString(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("rgb(%d, %d, %d)", r, g, b);
    }

    /**
     * Check if avatar has image data.
     *
     * @return true if avatar has image data
     */
    public boolean hasAvatar() {
        return avatarData != null && avatarData.length > 0;
    }

    /**
     * Clear the avatar and show placeholder.
     */
    public void clearAvatar() {
        setAvatar(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvatarImageView that = (AvatarImageView) o;
        return size == that.size &&
            Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, username);
    }

    /**
     * Enum for avatar sizes.
     */
    public enum AvatarSize {
        SMALL(32),
        MEDIUM(48),
        LARGE(64);

        private final int pixels;

        AvatarSize(int pixels) {
            this.pixels = pixels;
        }

        public int getPixels() {
            return pixels;
        }
    }
}
