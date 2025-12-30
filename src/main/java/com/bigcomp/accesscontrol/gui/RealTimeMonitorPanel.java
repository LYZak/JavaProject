// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.core.Router;
import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * Real-time Monitor Panel - Displays site map and floor plan, shows access attempts in real-time
 */
public class RealTimeMonitorPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private JComboBox<String> viewCombo;
    private MapViewPanel mapViewPanel;
    private JTextArea eventLogArea;
    private Map<String, Point> badgeReaderPositions; // Badge reader ID -> Position
    private Map<String, FlashIndicator> flashIndicators; // Badge reader ID -> Flash indicator
    private Map<String, BadgeReader> badgeReaderMap; // Badge reader ID -> Badge reader object
    private String selectedReaderId; // Currently selected badge reader ID
    private Point dragStartPoint; // Drag start point
    private boolean isDragging = false;
    
    public RealTimeMonitorPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.badgeReaderPositions = new ConcurrentHashMap<>();
        this.flashIndicators = new ConcurrentHashMap<>();
        this.badgeReaderMap = new HashMap<>();
        
        initializeComponents();
        setupLayout();
        registerEventListeners();
        loadBadgeReaderPositions();
    }
    
    private void initializeComponents() {
        // View selection combo box
        viewCombo = new JComboBox<>(new String[]{"Site Layout", "Office Layout"});
        viewCombo.setToolTipText("Site Layout: site-layout.png | Office Layout: office-layout.png");
        viewCombo.addActionListener(e -> {
            String selected = (String) viewCombo.getSelectedItem();
            mapViewPanel.setViewType(selected);
            mapViewPanel.repaint();
        });
        
        // Map view panel
        mapViewPanel = new MapViewPanel();
        
        // Event log area
        eventLogArea = new JTextArea(10, 30);
        eventLogArea.setEditable(false);
        eventLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Top control panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("View:"));
        topPanel.add(viewCombo);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(new JButton("Refresh") {{
            addActionListener(e -> {
                loadBadgeReaderPositions();
                mapViewPanel.repaint();
            });
        }});
        topPanel.add(new JButton("Configure Badge Reader Positions") {{
            addActionListener(e -> showPositionConfigDialog());
        }});
        topPanel.add(new JButton("Auto-configure All Positions") {{
            addActionListener(e -> autoConfigureAllPositions());
        }});
        topPanel.add(new JButton("Save Position Configuration") {{
            addActionListener(e -> savePositions());
        }});
        
        // Zoom controls
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(new JLabel("Zoom:"));
        JButton zoomOutBtn = new JButton("-");
        zoomOutBtn.setToolTipText("Zoom Out");
        zoomOutBtn.addActionListener(e -> {
            mapViewPanel.zoomOut();
        });
        topPanel.add(zoomOutBtn);
        
        JLabel scaleLabel = new JLabel("100%");
        scaleLabel.setPreferredSize(new Dimension(60, 20));
        topPanel.add(scaleLabel);
        
        JButton zoomInBtn = new JButton("+");
        zoomInBtn.setToolTipText("Zoom In");
        zoomInBtn.addActionListener(e -> {
            mapViewPanel.zoomIn();
        });
        topPanel.add(zoomInBtn);
        
        JButton resetZoomBtn = new JButton("Reset");
        resetZoomBtn.setToolTipText("Reset Zoom");
        resetZoomBtn.addActionListener(e -> {
            mapViewPanel.resetZoom();
        });
        topPanel.add(resetZoomBtn);
        
        // Pass scaleLabel to mapViewPanel for updates
        mapViewPanel.setScaleLabel(scaleLabel);
        
        // Center: Map view (use scroll pane to support large images)
        JScrollPane mapScrollPane = new JScrollPane(mapViewPanel);
        mapScrollPane.setPreferredSize(new Dimension(1000, 700));
        mapScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mapScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Right: Event log
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Real-time Event Log"));
        rightPanel.add(new JScrollPane(eventLogArea), BorderLayout.CENTER);
        rightPanel.add(new JButton("Clear Log") {{
            addActionListener(e -> eventLogArea.setText(""));
        }}, BorderLayout.SOUTH);
        
        // Main layout: Left map, right log
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
            mapScrollPane, rightPanel);
        splitPane.setDividerLocation(800);
        splitPane.setResizeWeight(0.7);
        
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }
    
    private void registerEventListeners() {
        Router router = accessControlSystem.getRouter();
        router.addAccessEventListener((request, response) -> {
            SwingUtilities.invokeLater(() -> {
                handleAccessEvent(request, response);
            });
        });
    }
    
    private void handleAccessEvent(AccessRequest request, AccessResponse response) {
        String readerId = request.getBadgeReaderId();
        boolean granted = response.isGranted();
        
        // Create or update flash indicator
        FlashIndicator indicator = flashIndicators.computeIfAbsent(readerId, 
            k -> new FlashIndicator(readerId));
        indicator.trigger(granted);
        
        // Refresh map display
        mapViewPanel.repaint();
        
        // Add to event log
        String timestamp = java.time.LocalDateTime.now().toString();
        String status = granted ? "✓ Granted" : "✗ Denied";
        String logEntry = String.format("[%s] %s - Badge Reader: %s, Resource: %s, Message: %s%n",
            timestamp, status, readerId, request.getResourceId(), response.getMessage());
        eventLogArea.append(logEntry);
        eventLogArea.setCaretPosition(eventLogArea.getDocument().getLength());
    }
    
    private void loadBadgeReaderPositions() {
        badgeReaderPositions.clear();
        badgeReaderMap.clear();
        Router router = accessControlSystem.getRouter();
        Map<String, BadgeReader> readers = router.getBadgeReaders();
        
        // Load saved position configuration
        loadSavedPositions();
        
        // Assign default positions for badge readers without configured positions
        int index = 0;
        for (BadgeReader reader : readers.values()) {
            badgeReaderMap.put(reader.getId(), reader);
            if (!badgeReaderPositions.containsKey(reader.getId())) {
                String resourceId = reader.getResourceId();
                Point position = calculatePosition(resourceId, index, readers.size());
                badgeReaderPositions.put(reader.getId(), position);
            }
            index++;
        }
    }
    
    /**
     * Load saved badge reader positions from file
     */
    private void loadSavedPositions() {
        try {
            File posFile = new File("data/reader_positions.properties");
            if (posFile.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(posFile)) {
                    props.load(fis);
                }
                
                String viewType = (String) viewCombo.getSelectedItem();
                for (String key : props.stringPropertyNames()) {
                    if (key.startsWith(viewType + ".")) {
                        String readerId = key.substring(viewType.length() + 1);
                        String value = props.getProperty(key);
                        String[] coords = value.split(",");
                        if (coords.length == 2) {
                            int x = Integer.parseInt(coords[0]);
                            int y = Integer.parseInt(coords[1]);
                            badgeReaderPositions.put(readerId, new Point(x, y));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore load errors, use default positions
        }
    }
    
    /**
     * Save badge reader positions to file
     */
    private void savePositions() {
        try {
            File posFile = new File("data/reader_positions.properties");
            File parentDir = posFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            java.util.Properties props = new java.util.Properties();
            
            // If file exists, load existing configuration first
            if (posFile.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(posFile)) {
                    props.load(fis);
                }
            }
            
            // Save positions for current view
            String viewType = (String) viewCombo.getSelectedItem();
            for (Map.Entry<String, Point> entry : badgeReaderPositions.entrySet()) {
                String key = viewType + "." + entry.getKey();
                Point pos = entry.getValue();
                props.setProperty(key, pos.x + "," + pos.y);
            }
            
            // Save to file
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(posFile)) {
                props.store(fos, "Badge Reader Position Configuration");
            }
            
            JOptionPane.showMessageDialog(this, "Position configuration saved", "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to save position configuration: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Auto-configure all badge reader positions
     */
    private void autoConfigureAllPositions() {
        Router router = accessControlSystem.getRouter();
        Map<String, BadgeReader> readers = router.getBadgeReaders();
        
        if (readers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No available badge readers", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Automatically assign positions for all badge readers?\n" +
            "System will automatically assign coordinates based on resource type and location.\n" +
            "You can manually adjust positions later.",
            "Auto-configure Positions",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> resources = dbManager.loadAllResources();
        
        String viewType = (String) viewCombo.getSelectedItem();
        int totalReaders = readers.size();
        
        // Group by resource type to assign positions
        Map<Resource.ResourceType, List<BadgeReader>> readersByType = new HashMap<>();
        for (BadgeReader reader : readers.values()) {
            Resource resource = resources.get(reader.getResourceId());
            if (resource != null) {
                readersByType.computeIfAbsent(resource.getType(), k -> new ArrayList<>())
                    .add(reader);
            }
        }
        
        // Assign position areas for each type of badge reader
        int yOffset = 0;
        for (Map.Entry<Resource.ResourceType, List<BadgeReader>> entry : readersByType.entrySet()) {
            List<BadgeReader> typeReaders = entry.getValue();
            int cols = (int) Math.ceil(Math.sqrt(typeReaders.size()));
            
            for (int i = 0; i < typeReaders.size(); i++) {
                BadgeReader reader = typeReaders.get(i);
                int row = i / cols;
                int col = i % cols;
                
                Point position;
                if ("Site Layout".equals(viewType)) {
                    // Site map: Group by type, distribute in different areas
                    int baseX = 150 + (yOffset % 3) * 300;
                    int baseY = 150 + (yOffset / 3) * 200;
                    position = new Point(baseX + col * 200, baseY + row * 150);
                } else {
                    // Floor plan: Group by type
                    int baseX = 100 + (yOffset % 4) * 200;
                    int baseY = 100 + (yOffset / 4) * 150;
                    position = new Point(baseX + col * 150, baseY + row * 120);
                }
                
                badgeReaderPositions.put(reader.getId(), position);
            }
            yOffset++;
        }
        
        // If image exists, use image dimensions to assign positions
        if (mapViewPanel.backgroundImage != null) {
            int imgWidth = mapViewPanel.backgroundImage.getWidth();
            int imgHeight = mapViewPanel.backgroundImage.getHeight();
            
            // Reassign positions based on image dimensions
            yOffset = 0;
            for (Map.Entry<Resource.ResourceType, List<BadgeReader>> entry : readersByType.entrySet()) {
                List<BadgeReader> typeReaders = entry.getValue();
                int cols = (int) Math.ceil(Math.sqrt(typeReaders.size()));
                
                for (int i = 0; i < typeReaders.size(); i++) {
                    BadgeReader reader = typeReaders.get(i);
                    int row = i / cols;
                    int col = i % cols;
                    
                    // Calculate position based on image dimensions
                    int cellWidth = imgWidth / (cols + 2);
                    int cellHeight = imgHeight / (readersByType.size() + 2);
                    int x = cellWidth * (col + 1) + (yOffset % 3) * 50;
                    int y = cellHeight * (yOffset + 1) + row * 80;
                    
                    badgeReaderPositions.put(reader.getId(), new Point(x, y));
                }
                yOffset++;
            }
        }
        
        mapViewPanel.repaint();
        JOptionPane.showMessageDialog(this, 
            "Automatically assigned positions for " + totalReaders + " badge readers\n" +
            "You can drag to adjust positions on the map, then click 'Save Position Configuration' to save.",
            "Configuration Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Show position configuration dialog
     */
    private void showPositionConfigDialog() {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
            "Configure Badge Reader Positions", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea(
            "Badge Reader Position Configuration Instructions:\n\n" +
            "Purpose:\n" +
            "• Set badge reader display positions on the map\n" +
            "• Used for visual display of badge reader positions in real-time monitor panel\n" +
            "• When access events occur, flash indicators will be shown at corresponding badge reader positions\n\n" +
            "Usage:\n" +
            "1. Click 'Auto-configure All Positions' button to assign positions for all badge readers at once\n" +
            "2. Click and drag badge reader icons on the map to manually adjust positions\n" +
            "3. Click badge reader icons to view detailed information\n" +
            "4. After adjustment, click 'Save Position Configuration' button to save\n" +
            "5. Position configurations are saved separately by view type (Site Layout/Office Layout)\n\n" +
            "Tips:\n" +
            "• It's recommended to use 'Auto-configure All Positions' for quick configuration first\n" +
            "• Then manually fine-tune positions according to actual layout\n" +
            "• Position configurations for different view types are independent"
        );
        infoArea.setEditable(false);
        infoArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private Point calculatePosition(String resourceId, int index, int total) {
        // Calculate position based on view type and resource ID
        String viewType = (String) viewCombo.getSelectedItem();
        
        if ("Site Layout".equals(viewType)) {
            // Site map: Distribute badge readers at different positions on the map
            int cols = (int) Math.ceil(Math.sqrt(total));
            int row = index / cols;
            int col = index % cols;
            int x = 150 + col * 200;
            int y = 150 + row * 150;
            return new Point(x, y);
        } else {
            // Floor plan: Assign based on resource type and location
            int cols = (int) Math.ceil(Math.sqrt(total));
            int row = index / cols;
            int col = index % cols;
            int x = 100 + col * 150;
            int y = 100 + row * 120;
            return new Point(x, y);
        }
    }
    
    /**
     * Map view panel - Draws site map or floor plan
     */
    private class MapViewPanel extends JPanel {
        private String viewType = "Site Layout";
        private BufferedImage backgroundImage;
        private String currentImagePath;
        private double scaleFactor = 1.0; // Scale factor
        private static final double MIN_SCALE = 0.25; // Minimum scale
        private static final double MAX_SCALE = 3.0; // Maximum scale
        private static final double SCALE_STEP = 0.1; // Scale step
        private JLabel scaleLabel; // Scale label
        
        public MapViewPanel() {
            // Add mouse listeners for interaction
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleMousePress(e);
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    handleMouseRelease(e);
                }
                
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleMouseClick(e);
                }
            });
            
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    handleMouseDrag(e);
                }
            });
            
            // Add mouse wheel zoom
            addMouseWheelListener(e -> {
                int rotation = e.getWheelRotation();
                if (rotation < 0) {
                    // Scroll up, zoom in
                    zoomIn();
                } else {
                    // Scroll down, zoom out
                    zoomOut();
                }
            });
            
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        
        public void setScaleLabel(JLabel label) {
            this.scaleLabel = label;
            updateScaleLabel();
        }
        
        public void zoomIn() {
            if (scaleFactor < MAX_SCALE) {
                scaleFactor = Math.min(MAX_SCALE, scaleFactor + SCALE_STEP);
                updateScaleLabel();
                revalidate();
                repaint();
            }
        }
        
        public void zoomOut() {
            if (scaleFactor > MIN_SCALE) {
                scaleFactor = Math.max(MIN_SCALE, scaleFactor - SCALE_STEP);
                updateScaleLabel();
                revalidate();
                repaint();
            }
        }
        
        public void resetZoom() {
            scaleFactor = 1.0;
            updateScaleLabel();
            revalidate();
            repaint();
        }
        
        private void updateScaleLabel() {
            if (scaleLabel != null) {
                scaleLabel.setText(String.format("%.0f%%", scaleFactor * 100));
            }
        }
        
        public double getScaleFactor() {
            return scaleFactor;
        }
        
        public void setViewType(String viewType) {
            this.viewType = viewType;
            loadBackgroundImage();
            loadBadgeReaderPositions();
        }
        
        /**
         * Load background image
         */
        private void loadBackgroundImage() {
            String imageName;
            if ("Site Layout".equals(viewType)) {
                imageName = "site-layout.png"; // Site layout
            } else {
                imageName = "office-layout.png"; // Office layout
            }
            
            // Try to load image from multiple locations
            String[] paths = {
                "images/" + imageName,
                "data/images/" + imageName,
                imageName,
                "../images/" + imageName,
                "./images/" + imageName
            };
            
            for (String path : paths) {
                File imageFile = new File(path);
                if (imageFile.exists() && imageFile.isFile()) {
                    try {
                        BufferedImage img = ImageIO.read(imageFile);
                        if (img != null) {
                            backgroundImage = img;
                            currentImagePath = path;
                            return;
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to load image: " + path + " - " + e.getMessage());
                    }
                }
            }
            
            // If image doesn't exist, use null
            backgroundImage = null;
            currentImagePath = null;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Draw background image or default graphics
            if (backgroundImage != null) {
                // Draw image background - apply scaling
                int imgWidth = backgroundImage.getWidth();
                int imgHeight = backgroundImage.getHeight();
                
                int scaledWidth = (int) (imgWidth * scaleFactor);
                int scaledHeight = (int) (imgHeight * scaleFactor);
                
                // Draw scaled image
                g2d.drawImage(backgroundImage, 0, 0, scaledWidth, scaledHeight, null);
                
                // Update panel size to fit scaled image
                setPreferredSize(new Dimension(scaledWidth, scaledHeight));
                revalidate();
            } else {
                // If no image, show blank background
                g2d.setColor(new Color(240, 240, 240));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
            
            // Draw badge readers and flash indicators (on top of image)
            drawBadgeReaders(g2d);
        }
        
        private void drawBadgeReaders(Graphics2D g) {
            // Draw all badge reader positions (apply scaling)
            for (Map.Entry<String, Point> entry : badgeReaderPositions.entrySet()) {
                String readerId = entry.getKey();
                Point originalPos = entry.getValue();
                
                // Convert original position to scaled position
                int scaledX = (int) (originalPos.x * scaleFactor);
                int scaledY = (int) (originalPos.y * scaleFactor);
                
                // Check if selected
                boolean isSelected = readerId.equals(selectedReaderId);
                
                // Check if there's a flash indicator
                FlashIndicator indicator = flashIndicators.get(readerId);
                int baseSize = (int) (8 * scaleFactor); // Base size adjusts with scale
                int selectedSize = (int) (12 * scaleFactor);
                
                if (indicator != null && indicator.isActive()) {
                    // Draw flashing point
                    Color color = indicator.isGranted() ? Color.GREEN : Color.RED;
                    g.setColor(color);
                    int size = (int) (indicator.getFlashSize() * scaleFactor);
                    g.fillOval(scaledX - size/2, scaledY - size/2, size, size);
                    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
                    g.fillOval(scaledX - size, scaledY - size, size * 2, size * 2);
                } else {
                    // Draw normal badge reader icon
                    if (isSelected) {
                        // Selected state: draw outer circle
                        g.setColor(new Color(255, 200, 0, 150));
                        g.fillOval(scaledX - selectedSize, scaledY - selectedSize, selectedSize * 2, selectedSize * 2);
                    }
                    g.setColor(isSelected ? Color.ORANGE : Color.BLUE);
                    g.fillOval(scaledX - baseSize, scaledY - baseSize, baseSize * 2, baseSize * 2);
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke((float) (2 * scaleFactor)));
                    g.drawOval(scaledX - baseSize, scaledY - baseSize, baseSize * 2, baseSize * 2);
                }
                
                // Draw badge reader label (font size also adjusts with scale)
                g.setColor(Color.BLACK);
                int fontSize = Math.max(10, (int) (11 * scaleFactor));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
                BadgeReader reader = badgeReaderMap.get(readerId);
                String label = reader != null ? getReaderLabel(reader) : "R" + readerId.substring(0, Math.min(4, readerId.length()));
                g.drawString(label, scaledX + (int)(10 * scaleFactor), scaledY - (int)(10 * scaleFactor));
            }
        }
        
        /**
         * Get badge reader label
         */
        private String getReaderLabel(BadgeReader reader) {
            try {
                DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
                Map<String, Resource> resources = dbManager.loadAllResources();
                Resource resource = resources.get(reader.getResourceId());
                if (resource != null) {
                    return resource.getName();
                }
            } catch (Exception e) {
                // Ignore error
            }
            return "R" + reader.getId().substring(0, Math.min(4, reader.getId().length()));
        }
        
        /**
         * Handle mouse click
         */
        private void handleMouseClick(MouseEvent e) {
            if (isDragging) {
                return; // If drag ended, don't handle click
            }
            
            Point clickPoint = e.getPoint();
            String clickedReader = findReaderAtPoint(clickPoint);
            
            if (clickedReader != null) {
                selectedReaderId = clickedReader;
                showReaderInfo(clickedReader);
                repaint();
            } else {
                selectedReaderId = null;
                repaint();
            }
        }
        
        /**
         * Handle mouse press
         */
        private void handleMousePress(MouseEvent e) {
            Point clickPoint = e.getPoint();
            String clickedReader = findReaderAtPoint(clickPoint);
            
            if (clickedReader != null) {
                selectedReaderId = clickedReader;
                dragStartPoint = clickPoint;
                isDragging = false;
                repaint();
            }
        }
        
        /**
         * Handle mouse release
         */
        private void handleMouseRelease(MouseEvent e) {
            if (isDragging && selectedReaderId != null) {
                // Drag ended, update position
                Point newPos = e.getPoint();
                badgeReaderPositions.put(selectedReaderId, newPos);
                repaint();
            }
            isDragging = false;
        }
        
        /**
         * Handle mouse drag (consider scaling)
         */
        private void handleMouseDrag(MouseEvent e) {
            if (selectedReaderId != null && dragStartPoint != null) {
                int dx = Math.abs(e.getX() - dragStartPoint.x);
                int dy = Math.abs(e.getY() - dragStartPoint.y);
                
                if (dx > 5 || dy > 5) { // Drag threshold
                    isDragging = true;
                    Point scaledPos = e.getPoint();
                    // Convert scaled position back to original position
                    Point originalPos = new Point(
                        (int) (scaledPos.x / scaleFactor),
                        (int) (scaledPos.y / scaleFactor)
                    );
                    badgeReaderPositions.put(selectedReaderId, originalPos);
                    repaint();
                }
            }
        }
        
        /**
         * Find badge reader at click point (consider scaling)
         */
        private String findReaderAtPoint(Point point) {
            for (Map.Entry<String, Point> entry : badgeReaderPositions.entrySet()) {
                Point originalPos = entry.getValue();
                // Convert original position to scaled position
                int scaledX = (int) (originalPos.x * scaleFactor);
                int scaledY = (int) (originalPos.y * scaleFactor);
                Point scaledPos = new Point(scaledX, scaledY);
                
                double distance = point.distance(scaledPos);
                int clickRadius = (int) (15 * scaleFactor); // Click range adjusts with scale
                if (distance <= clickRadius) {
                    return entry.getKey();
                }
            }
            return null;
        }
        
        /**
         * Show badge reader information
         */
        private void showReaderInfo(String readerId) {
            BadgeReader reader = badgeReaderMap.get(readerId);
            if (reader == null) {
                return;
            }
            
            StringBuilder info = new StringBuilder();
            info.append("Badge Reader ID: ").append(reader.getId()).append("\n");
            
            try {
                DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
                Map<String, Resource> resources = dbManager.loadAllResources();
                Resource resource = resources.get(reader.getResourceId());
                if (resource != null) {
                    info.append("Resource Name: ").append(resource.getName()).append("\n");
                    info.append("Resource Type: ").append(resource.getType()).append("\n");
                    info.append("Location: ").append(resource.getLocation()).append("\n");
                    info.append("Building: ").append(resource.getBuilding()).append("\n");
                    info.append("Floor: ").append(resource.getFloor()).append("\n");
                }
            } catch (Exception e) {
                info.append("Resource ID: ").append(reader.getResourceId()).append("\n");
            }
            
            Point pos = badgeReaderPositions.get(readerId);
            if (pos != null) {
                info.append("Position (Original): (").append(pos.x).append(", ").append(pos.y).append(")\n");
                double currentScale = mapViewPanel.getScaleFactor();
                info.append("Position (Current Scale): (").append((int)(pos.x * currentScale))
                    .append(", ").append((int)(pos.y * currentScale)).append(")");
            }
            
            JOptionPane.showMessageDialog(RealTimeMonitorPanel.this, 
                info.toString(), 
                "Badge Reader Information", 
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        @Override
        public Dimension getPreferredSize() {
            if (backgroundImage != null) {
                // If background image exists, use image dimensions
                return new Dimension(backgroundImage.getWidth(), backgroundImage.getHeight());
            }
            return new Dimension(1200, 800);
        }
        
        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
    }
    
    /**
     * Flash indicator - Visual feedback for access attempts
     */
    private class FlashIndicator {
        private String readerId;
        private boolean active;
        private boolean granted;
        private long triggerTime;
        private static final long FLASH_DURATION = 2000; // Flash duration (milliseconds)
        
        public FlashIndicator(String readerId) {
            this.readerId = readerId;
            this.active = false;
        }
        
        public void trigger(boolean granted) {
            this.granted = granted;
            this.active = true;
            this.triggerTime = System.currentTimeMillis();
            
            // Start timer to automatically turn off flash
            javax.swing.Timer timer = new javax.swing.Timer((int) FLASH_DURATION, e -> {
                this.active = false;
                mapViewPanel.repaint();
            });
            timer.setRepeats(false);
            timer.start();
        }
        
        public boolean isActive() {
            if (active) {
                long elapsed = System.currentTimeMillis() - triggerTime;
                if (elapsed > FLASH_DURATION) {
                    active = false;
                }
            }
            return active;
        }
        
        public boolean isGranted() {
            return granted;
        }
        
        public int getFlashSize() {
            if (!isActive()) return 10;
            long elapsed = System.currentTimeMillis() - triggerTime;
            // Flash effect: size varies between 10-30
            double progress = (elapsed % 400) / 400.0;
            int baseSize = 15;
            int variation = (int) (10 * Math.sin(progress * Math.PI * 2));
            return baseSize + variation;
        }
    }
}
