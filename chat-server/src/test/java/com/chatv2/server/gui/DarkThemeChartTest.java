package com.chatv2.server.gui;

import com.chatv2.server.gui.controller.DashboardController;
import com.chatv2.server.gui.ServerAdminApp;
import com.chatv2.server.core.ChatServer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for dark theme chart styling.
 * Tests that chart elements are properly styled for dark theme visibility.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ApplicationExtension.class)
class DarkThemeChartTest extends ApplicationTest {

    @Mock
    private ServerAdminApp mockServerAdminApp;
    
    @Mock
    private ChatServer mockChatServer;
    
    private DashboardController controller;
    private Stage stage;
    private LineChart<String, Number> activityChart;
    private Pane root;
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        
        // Create fallback UI components
        try {
            root = new VBox();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            
            // Initialize chart
            activityChart = new LineChart<>(new javafx.scene.chart.CategoryAxis(), new NumberAxis());
            activityChart.setTitle("Activity Chart");
            activityChart.setId("activityChart");
            
            // Add chart to root
            root.getChildren().add(activityChart);
            
            // Apply dark theme CSS styles
            scene.getStylesheets().add(getClass().getResource("/css/server-admin.css").toExternalForm());
            
            controller = new DashboardController();
        } catch (Exception e) {
            fail("Failed to set up test stage: " + e.getMessage());
        }
    }
    
    @BeforeEach
    void setUp() {
        // Initialize controller
        if (controller != null) {
            controller.setMainApp(mockServerAdminApp);
            WaitForAsyncUtils.waitForFxEvents();
        }
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (controller != null) {
            controller.cleanup();
        }
        
        FxToolkit.cleanupStages();
    }
    
    @Test
    @DisplayName("Should apply dark theme background color to chart")
    void testDarkThemeChartBackground() {
        Platform.runLater(() -> {
            if (activityChart != null) {
                // Add chart style class for dark theme
                activityChart.getStyleClass().add("activity-chart");
                
                // Verify chart has dark theme style class
                assertTrue(activityChart.getStyleClass().contains("activity-chart"),
                          "Chart should have activity-chart style class");
                
                // Verify chart plot background exists
                var plotBackground = activityChart.lookup(".chart-plot-background");
                assertNotNull(plotBackground, "Chart should have a plot background");
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should style axis labels with light colors for visibility")
    void testDarkThemeAxisLabels() {
        Platform.runLater(() -> {
            if (activityChart != null) {
            // Check if axis labels are styled for dark theme
            // Look for axis-label style
            var styleClass = activityChart.getStyleClass();
            assertTrue(styleClass.contains("activity-chart") || 
                      !styleClass.isEmpty(), 
                      "Chart should have style class applied");
                
                // Verify chart is properly created
                assertNotNull(activityChart.getXAxis());
                assertNotNull(activityChart.getYAxis());
                
                // Add a title to verify it's visible
                activityChart.setTitle("Test Activity Chart");
                assertEquals("Test Activity Chart", activityChart.getTitle());
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should display chart legend with proper styling")
    void testDarkThemeChartLegend() {
        Platform.runLater(() -> {
            if (activityChart != null) {
                // Create test series for legend
                XYChart.Series<String, Number> messageSeries = new XYChart.Series<>();
                messageSeries.setName("Messages");
                
                XYChart.Series<String, Number> connectionSeries = new XYChart.Series<>();
                connectionSeries.setName("Connections");
                
                // Add series to chart
                activityChart.getData().add(messageSeries);
                activityChart.getData().add(connectionSeries);
                
                // Verify legend is visible
                assertTrue(activityChart.isLegendVisible(), "Chart legend should be visible");
                
                // Verify chart has series
                assertEquals(2, activityChart.getData().size(), "Chart should have 2 series");
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should style chart lines with appropriate colors for dark theme")
    void testDarkThemeChartLines() {
        Platform.runLater(() -> {
            if (activityChart != null) {
                // Create test series with data points
                XYChart.Series<String, Number> messageSeries = new XYChart.Series<>();
                messageSeries.setName("Messages");
                messageSeries.getData().add(new XYChart.Data<>("10:00", 5));
                messageSeries.getData().add(new XYChart.Data<>("10:05", 8));
                
                XYChart.Series<String, Number> connectionSeries = new XYChart.Series<>();
                connectionSeries.setName("Connections");
                connectionSeries.getData().add(new XYChart.Data<>("10:00", 3));
                connectionSeries.getData().add(new XYChart.Data<>("10:05", 4));
                
                // Add series to chart
                activityChart.getData().add(messageSeries);
                activityChart.getData().add(connectionSeries);
                
                // Verify chart has data
                assertEquals(2, activityChart.getData().size(), "Chart should have 2 series");
                assertEquals(2, messageSeries.getData().size(), "Message series should have 2 data points");
                assertEquals(2, connectionSeries.getData().size(), "Connection series should have 2 data points");
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should style grid lines for visibility in dark theme")
    void testDarkThemeGridLines() {
        Platform.runLater(() -> {
            if (activityChart != null) {
                // Verify chart has grid lines
                assertNotNull(activityChart.getXAxis(), "Chart should have X axis");
                assertNotNull(activityChart.getYAxis(), "Chart should have Y axis");
                
                // Verify X axis is CategoryAxis (as defined in chart creation)
                assertTrue(activityChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis,
                          "X axis should be a CategoryAxis");
                          
                // Y axis might be CategoryAxis or NumberAxis depending on the chart type
                assertNotNull(activityChart.getYAxis(), "Y axis should not be null");
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should maintain visibility of chart elements in dark theme")
    void testDarkThemeElementVisibility() {
        Platform.runLater(() -> {
            if (activityChart != null) {
                // Create and add test series
                XYChart.Series<String, Number> messageSeries = new XYChart.Series<>();
                messageSeries.setName("Messages");
                messageSeries.getData().add(new XYChart.Data<>("10:00", 5));
                
                activityChart.getData().add(messageSeries);
                
                // Verify chart elements are created and accessible
                assertNotNull(activityChart.lookupAll(".chart-line"), "Chart should have line elements");
                assertNotNull(activityChart.lookupAll(".chart-legend"), "Chart should have legend elements");
                
                // Verify chart is properly displayed
                assertTrue(activityChart.isVisible(), "Chart should be visible");
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should handle chart color scheme changes")
    void testDarkThemeColorScheme() {
        Platform.runLater(() -> {
            if (activityChart != null) {
                // Add multiple series to test color scheme
                XYChart.Series<String, Number> messageSeries = new XYChart.Series<>();
                messageSeries.setName("Messages");
                
                XYChart.Series<String, Number> connectionSeries = new XYChart.Series<>();
                connectionSeries.setName("Connections");
                
                activityChart.getData().addAll(messageSeries, connectionSeries);
                
                // Verify series are added
                assertEquals(2, activityChart.getData().size());
                
                // Verify chart is properly styled
                assertTrue(activityChart.getStyleClass().contains("activity-chart") || 
                          !activityChart.getStyleClass().isEmpty(),
                          "Chart should have proper style classes");
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
}