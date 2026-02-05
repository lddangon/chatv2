package com.chatv2.client.gui.component;

import com.chatv2.common.model.UserStatus;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.scene.layout.Region;

/**
 * Custom Control for visual status indication (online/away/offline).
 * Displays a circular indicator with optional pulse animation.
 */
public class StatusIndicator extends Region {

    private static final PseudoClass ONLINE_PSEUDO_CLASS = PseudoClass.getPseudoClass("online");
    private static final PseudoClass AWAY_PSEUDO_CLASS = PseudoClass.getPseudoClass("away");
    private static final PseudoClass OFFLINE_PSEUDO_CLASS = PseudoClass.getPseudoClass("offline");
    private static final PseudoClass ANIMATED_PSEUDO_CLASS = PseudoClass.getPseudoClass("animated");

    private UserStatus status = UserStatus.OFFLINE;
    private StatusSize size = StatusSize.MEDIUM;
    private boolean animated = false;
    private Timeline pulseAnimation;

    /**
     * Default constructor with MEDIUM size.
     */
    public StatusIndicator() {
        this(StatusSize.MEDIUM);
    }

    /**
     * Constructor with specified size.
     *
     * @param size the size of the indicator
     */
    public StatusIndicator(StatusSize size) {
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
        getStyleClass().add("status-indicator");
        updateSize();
        updatePseudoClasses();
    }

    /**
     * Set the status to display.
     *
     * @param status the user status to display
     */
    public void setStatus(UserStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (this.status != status) {
            this.status = status;
            updatePseudoClasses();
            updateAnimation();
        }
    }

    /**
     * Get the current status.
     *
     * @return the current status
     */
    public UserStatus getStatus() {
        return status;
    }

    /**
     * Set the size of the indicator.
     *
     * @param size the size to set
     */
    public void setSize(StatusSize size) {
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
    public StatusSize getSize() {
        return size;
    }

    /**
     * Enable or disable pulse animation for ONLINE status.
     *
     * @param animated true to enable animation, false to disable
     */
    public void setAnimated(boolean animated) {
        if (this.animated != animated) {
            this.animated = animated;
            pseudoClassStateChanged(ANIMATED_PSEUDO_CLASS, animated);
            updateAnimation();
        }
    }

    /**
     * Check if animation is enabled.
     *
     * @return true if animation is enabled
     */
    public boolean isAnimated() {
        return animated;
    }

    /**
     * Update the size based on current StatusSize.
     */
    private void updateSize() {
        setPrefSize(size.getPixels(), size.getPixels());
        setMinSize(size.getPixels(), size.getPixels());
        setMaxSize(size.getPixels(), size.getPixels());
    }

    /**
     * Update CSS pseudo classes based on current status.
     */
    private void updatePseudoClasses() {
        pseudoClassStateChanged(ONLINE_PSEUDO_CLASS, status == UserStatus.ONLINE);
        pseudoClassStateChanged(AWAY_PSEUDO_CLASS, status == UserStatus.AWAY);
        pseudoClassStateChanged(OFFLINE_PSEUDO_CLASS, status == UserStatus.OFFLINE);
    }

    /**
     * Update pulse animation based on status and animated flag.
     */
    private void updateAnimation() {
        // Stop existing animation
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }

        // Start animation if ONLINE and animated
        if (animated && status == UserStatus.ONLINE) {
            pulseAnimation = new Timeline(
                new KeyFrame(javafx.util.Duration.ZERO,
                    new KeyValue(opacityProperty(), 1.0)),
                new KeyFrame(javafx.util.Duration.millis(500),
                    new KeyValue(opacityProperty(), 0.5)),
                new KeyFrame(javafx.util.Duration.millis(1000),
                    new KeyValue(opacityProperty(), 1.0))
            );
            pulseAnimation.setCycleCount(Animation.INDEFINITE);
            pulseAnimation.setAutoReverse(false);
            pulseAnimation.play();
        } else {
            setOpacity(1.0);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
        }
        super.finalize();
    }

    /**
     * Enum for status indicator sizes.
     */
    public enum StatusSize {
        SMALL(8),
        MEDIUM(12),
        LARGE(16);

        private final int pixels;

        StatusSize(int pixels) {
            this.pixels = pixels;
        }

        public int getPixels() {
            return pixels;
        }
    }
}
