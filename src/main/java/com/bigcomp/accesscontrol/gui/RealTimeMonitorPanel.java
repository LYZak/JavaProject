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
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
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
    private enum ViewType {
        SITE("site"),
        BUILDING("building");

        private final String key;

        ViewType(String key) {
            this.key = key;
        }

        String getKey() {
            return key;
        }
    }

    private AccessControlSystem accessControlSystem;
    private JComboBox<ViewType> viewCombo;
    private MapViewPanel mapViewPanel;
    private JTextArea eventLogArea;
    private TitledBorder logBorder;
    private JLabel viewLabel;
    private JLabel zoomLabel;
    private JButton refreshButton;
    private JButton configureButton;
    private JButton autoConfigureButton;
    private JButton savePositionsButton;
    private JButton zoomOutBtn;
    private JButton zoomInBtn;
    private JButton resetZoomBtn;
    private JButton clearLogButton;
    private JLabel scaleLabel;
    private Map<String, Point> badgeReaderPositions; // Badge reader ID -> Position
    private Map<String, FlashIndicator> flashIndicators; // Badge reader ID -> Flash indicator
    private Map<String, BadgeReader> badgeReaderMap; // Badge reader ID -> Badge reader object
    private String selectedReaderId; // Currently selected badge reader ID
    private Point dragStartPoint; // Drag start point
    private Point dragOffsetPoint; // Drag offset (mouse point - reader center) in view coordinates
    private boolean isDragging = false;
    private String selectedBuilding;
    private String selectedFloor;
    private Map<String, Resource> cachedResources = new HashMap<>();
    private JPanel floorSidebar;
    private JLabel buildingTitleLabel;
    private JPanel floorButtonsPanel;
    private JButton backToSiteButton;
    
    public RealTimeMonitorPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.badgeReaderPositions = new ConcurrentHashMap<>();
        this.flashIndicators = new ConcurrentHashMap<>();
        this.badgeReaderMap = new HashMap<>();
        
        initializeComponents();
        setupLayout();
        applyLanguage();
        registerEventListeners();
        refreshResources();
        loadBadgeReaderPositions();
    }
    
    private void initializeComponents() {
        // View selection combo box
        viewCombo = new JComboBox<>(new ViewType[]{ViewType.SITE, ViewType.BUILDING});
        viewCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ViewType) {
                    ViewType v = (ViewType) value;
                    if (v == ViewType.SITE) {
                        setText(I18n.t("monitor.view.site"));
                    } else {
                        setText(I18n.t("monitor.view.building"));
                    }
                }
                return this;
            }
        });
        viewCombo.addActionListener(e -> {
            ViewType selected = (ViewType) viewCombo.getSelectedItem();
            if (selected != null) {
                if (selected == ViewType.BUILDING) {
                    ensureBuildingAndFloorSelected();
                }
                mapViewPanel.setViewType(selected);
            }
            mapViewPanel.repaint();
            updateFloorSidebar();
        });
        
        // Map view panel
        mapViewPanel = new MapViewPanel();

        floorSidebar = new JPanel();
        floorSidebar.setLayout(new BorderLayout());
        floorSidebar.setBorder(new EmptyBorder(8, 8, 8, 8));

        buildingTitleLabel = new JLabel("", SwingConstants.CENTER);
        buildingTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        floorSidebar.add(buildingTitleLabel, BorderLayout.NORTH);

        floorButtonsPanel = new JPanel();
        floorButtonsPanel.setLayout(new BoxLayout(floorButtonsPanel, BoxLayout.Y_AXIS));
        JScrollPane floorScroll = new JScrollPane(floorButtonsPanel);
        floorScroll.setBorder(null);
        floorScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        floorSidebar.add(floorScroll, BorderLayout.CENTER);

        backToSiteButton = new JButton();
        backToSiteButton.addActionListener(e -> switchToSite());
        floorSidebar.add(backToSiteButton, BorderLayout.SOUTH);
        
        // Event log area
        eventLogArea = new JTextArea(10, 30);
        eventLogArea.setEditable(false);
        Font textAreaFont = UIManager.getFont("TextArea.font");
        if (textAreaFont != null) {
            eventLogArea.setFont(textAreaFont);
        }
        eventLogArea.setMargin(new Insets(8, 8, 8, 8));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        
        // Top control panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        viewLabel = new JLabel();
        topPanel.add(viewLabel);
        topPanel.add(viewCombo);
        topPanel.add(Box.createHorizontalStrut(20));
        refreshButton = new JButton();
        refreshButton.addActionListener(e -> {
            loadBadgeReaderPositions();
            mapViewPanel.repaint();
        });
        configureButton = new JButton();
        configureButton.addActionListener(e -> showPositionConfigDialog());
        autoConfigureButton = new JButton();
        autoConfigureButton.addActionListener(e -> autoConfigureAllPositions());
        savePositionsButton = new JButton();
        savePositionsButton.addActionListener(e -> savePositions());
        topPanel.add(refreshButton);
        topPanel.add(configureButton);
        topPanel.add(autoConfigureButton);
        topPanel.add(savePositionsButton);
        
        // Zoom controls
        topPanel.add(Box.createHorizontalStrut(10));
        zoomLabel = new JLabel();
        topPanel.add(zoomLabel);
        zoomOutBtn = new JButton();
        zoomOutBtn.addActionListener(e -> mapViewPanel.zoomOut());
        topPanel.add(zoomOutBtn);
        
        scaleLabel = new JLabel("", SwingConstants.CENTER);
        scaleLabel.setPreferredSize(new Dimension(60, 20));
        topPanel.add(scaleLabel);
        
        zoomInBtn = new JButton();
        zoomInBtn.addActionListener(e -> mapViewPanel.zoomIn());
        topPanel.add(zoomInBtn);
        
        resetZoomBtn = new JButton();
        resetZoomBtn.addActionListener(e -> mapViewPanel.resetZoom());
        topPanel.add(resetZoomBtn);
        
        // Pass scaleLabel to mapViewPanel for updates
        mapViewPanel.setScaleLabel(scaleLabel);
        
        // Center: Map view (use scroll pane to support large images)
        JScrollPane mapScrollPane = new JScrollPane(mapViewPanel);
        mapScrollPane.setPreferredSize(new Dimension(1000, 700));
        mapScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mapScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel mapContainer = new JPanel(new BorderLayout());
        mapContainer.add(floorSidebar, BorderLayout.WEST);
        mapContainer.add(mapScrollPane, BorderLayout.CENTER);
        
        // Right: Event log
        JPanel rightPanel = new JPanel(new BorderLayout());
        logBorder = new TitledBorder("");
        rightPanel.setBorder(logBorder);
        JScrollPane logScroll = new JScrollPane(eventLogArea);
        logScroll.setBorder(new EmptyBorder(8, 8, 8, 8));
        rightPanel.add(logScroll, BorderLayout.CENTER);
        clearLogButton = new JButton();
        clearLogButton.addActionListener(e -> eventLogArea.setText(""));
        JPanel logActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logActions.setBorder(new EmptyBorder(0, 8, 8, 8));
        logActions.add(clearLogButton);
        rightPanel.add(logActions, BorderLayout.SOUTH);
        
        // Main layout: Left map, right log
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
            mapContainer, rightPanel);
        splitPane.setDividerLocation(800);
        splitPane.setResizeWeight(0.7);
        
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        updateFloorSidebar();
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
        String status = granted ? I18n.t("monitor.event.granted") : I18n.t("monitor.event.denied");
        String logEntry = I18n.f("monitor.event.log", timestamp, status, readerId, request.getResourceId(), response.getMessage());
        eventLogArea.append(logEntry);
        eventLogArea.setCaretPosition(eventLogArea.getDocument().getLength());
    }

    public void applyLanguage() {
        viewLabel.setText(I18n.t("monitor.label.view"));
        refreshButton.setText(I18n.t("common.refresh"));
        configureButton.setText(I18n.t("monitor.action.configure"));
        autoConfigureButton.setText(I18n.t("monitor.action.autoConfigure"));
        savePositionsButton.setText(I18n.t("monitor.action.savePositions"));

        zoomLabel.setText(I18n.t("monitor.label.zoom"));
        zoomOutBtn.setText(I18n.t("monitor.action.zoomOut"));
        zoomOutBtn.setToolTipText(I18n.t("monitor.tip.zoomOut"));
        zoomInBtn.setText(I18n.t("monitor.action.zoomIn"));
        zoomInBtn.setToolTipText(I18n.t("monitor.tip.zoomIn"));
        resetZoomBtn.setText(I18n.t("monitor.action.resetZoom"));
        resetZoomBtn.setToolTipText(I18n.t("monitor.tip.resetZoom"));

        viewCombo.setToolTipText(I18n.t("monitor.tip.viewCombo"));
        viewCombo.repaint();

        logBorder.setTitle(I18n.t("monitor.title.log"));
        clearLogButton.setText(I18n.t("monitor.action.clearLog"));

        mapViewPanel.updateScaleLabel();
        backToSiteButton.setText(I18n.t("monitor.action.backToSite"));
        updateFloorSidebar();

        revalidate();
        repaint();
    }

    private ViewType getSelectedViewType() {
        ViewType v = (ViewType) viewCombo.getSelectedItem();
        return v != null ? v : ViewType.SITE;
    }

    private String getSelectedViewKey() {
        return computeViewKey();
    }
    
    private void loadBadgeReaderPositions() {
        badgeReaderPositions.clear();
        badgeReaderMap.clear();
        Router router = accessControlSystem.getRouter();
        Map<String, BadgeReader> readers = router.getBadgeReaders();
        refreshResources();

        // Load saved position configuration
        loadSavedPositions();

        // Assign default positions for badge readers without configured positions
        int index = 0;
        for (BadgeReader reader : readers.values()) {
            Resource resource = cachedResources.get(reader.getResourceId());
            if (!shouldIncludeReader(resource)) {
                continue;
            }

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
                
                String viewKey = getSelectedViewKey();
                loadPositionsFromProperties(props, viewKey);
                if (ViewType.SITE == getSelectedViewType()) {
                    loadPositionsFromProperties(props, "Site Layout");
                } else {
                    loadPositionsFromProperties(props, "Office Layout");
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
            String viewKey = getSelectedViewKey();
            for (Map.Entry<String, Point> entry : badgeReaderPositions.entrySet()) {
                String key = viewKey + "." + entry.getKey();
                Point pos = entry.getValue();
                props.setProperty(key, pos.x + "," + pos.y);
            }
            
            // Save to file
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(posFile)) {
                props.store(fos, I18n.t("monitor.save.comment"));
            }
            
            JOptionPane.showMessageDialog(this, I18n.t("monitor.msg.posSaved"), I18n.t("common.success"), 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, I18n.f("monitor.msg.saveFailed", e.getMessage()), 
                I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Auto-configure all badge reader positions
     */
    private void autoConfigureAllPositions() {
        Router router = accessControlSystem.getRouter();
        Map<String, BadgeReader> readers = router.getBadgeReaders();
        
        if (readers.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.t("monitor.msg.noReaders"), I18n.t("common.info"), 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            I18n.t("monitor.msg.confirmAutoConfig"),
            I18n.t("monitor.title.autoConfig"),
            JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> resources = dbManager.loadAllResources();
        
        ViewType viewType = getSelectedViewType();
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
                if (viewType == ViewType.SITE) {
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
            I18n.f("monitor.msg.autoConfigComplete", totalReaders),
            I18n.t("monitor.title.configComplete"),
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Show position configuration dialog
     */
    private void showPositionConfigDialog() {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
            I18n.t("monitor.title.configDialog"), true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea(I18n.t("monitor.text.configInstructions"));
        infoArea.setEditable(false);
        infoArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton(I18n.t("common.close"));
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private Point calculatePosition(String resourceId, int index, int total) {
        // Calculate position based on view type and resource ID
        ViewType viewType = getSelectedViewType();
        
        if (viewType == ViewType.SITE) {
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
        private ViewType viewType = ViewType.SITE;
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
            loadBackgroundImage();
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
        
        public void updateScaleLabel() {
            if (scaleLabel != null) {
                scaleLabel.setText(I18n.f("monitor.text.scale", (int)(scaleFactor * 100)));
            }
        }
        
        public double getScaleFactor() {
            return scaleFactor;
        }
        
        public void setViewType(ViewType viewType) {
            if (viewType == null) {
                return;
            }
            this.viewType = viewType;
            loadBackgroundImage();
            loadBadgeReaderPositions();
        }
        
        /**
         * Load background image
         */
        private void loadBackgroundImage() {
            List<String> paths = getBackgroundImageCandidates(viewType);
            for (String path : paths) {
                File imageFile = new File(path);
                if (!imageFile.exists() || !imageFile.isFile()) {
                    continue;
                }
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

            if (viewType == ViewType.SITE) {
                drawBuildingOverlays(g2d);
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
                String label = reader != null ? getReaderLabel(reader) : I18n.f("monitor.reader.prefix", readerId.substring(0, Math.min(4, readerId.length())));
                g.drawString(label, scaledX + (int)(10 * scaleFactor), scaledY - (int)(10 * scaleFactor));
            }
        }
        
        /**
         * Get badge reader label
         */
        private String getReaderLabel(BadgeReader reader) {
            try {
                Resource resource = cachedResources.get(reader.getResourceId());
                if (resource != null) {
                    return resource.getName();
                }
            } catch (Exception e) {
                // Ignore error
            }
            return I18n.f("monitor.reader.prefix", reader.getId().substring(0, Math.min(4, reader.getId().length())));
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
                if (viewType == ViewType.SITE) {
                    String building = findBuildingAtPoint(clickPoint);
                    if (building != null) {
                        enterBuilding(building);
                        return;
                    }
                }
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
                dragOffsetPoint = new Point(0, 0);
                Point originalPos = badgeReaderPositions.get(clickedReader);
                if (originalPos != null) {
                    int scaledX = (int) Math.round(originalPos.x * scaleFactor);
                    int scaledY = (int) Math.round(originalPos.y * scaleFactor);
                    dragOffsetPoint = new Point(clickPoint.x - scaledX, clickPoint.y - scaledY);
                }
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
                Point viewPos = e.getPoint();
                Point adjustedView = dragOffsetPoint != null
                    ? new Point(viewPos.x - dragOffsetPoint.x, viewPos.y - dragOffsetPoint.y)
                    : viewPos;
                Point originalPos = new Point(
                    (int) Math.round(adjustedView.x / scaleFactor),
                    (int) Math.round(adjustedView.y / scaleFactor)
                );
                badgeReaderPositions.put(selectedReaderId, originalPos);
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
                    Point viewPos = e.getPoint();
                    Point adjustedView = dragOffsetPoint != null
                        ? new Point(viewPos.x - dragOffsetPoint.x, viewPos.y - dragOffsetPoint.y)
                        : viewPos;
                    Point originalPos = new Point(
                        (int) Math.round(adjustedView.x / scaleFactor),
                        (int) Math.round(adjustedView.y / scaleFactor)
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

        private String findBuildingAtPoint(Point point) {
            Map<String, Rectangle> rects = computeBuildingRects();
            for (Map.Entry<String, Rectangle> entry : rects.entrySet()) {
                if (entry.getValue().contains(point)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        private void drawBuildingOverlays(Graphics2D g) {
            Map<String, Rectangle> rects = computeBuildingRects();
            if (rects.isEmpty()) {
                return;
            }
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, (int) (14 * scaleFactor))));
            for (Map.Entry<String, Rectangle> entry : rects.entrySet()) {
                String building = entry.getKey();
                Rectangle rect = entry.getValue();
                g.setColor(new Color(0, 90, 200, 40));
                g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 18, 18);
                g.setColor(new Color(0, 90, 200, 140));
                g.setStroke(new BasicStroke((float) Math.max(1, 2 * scaleFactor)));
                g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 18, 18);

                g.setColor(new Color(20, 20, 20, 220));
                int textX = rect.x + (int) (10 * scaleFactor);
                int textY = rect.y + (int) (20 * scaleFactor);
                g.drawString(building, textX, textY);
            }
        }

        private Map<String, Rectangle> computeBuildingRects() {
            Map<String, Rectangle> rects = new HashMap<>();
            if (badgeReaderPositions.isEmpty()) {
                return rects;
            }
            Map<String, int[]> bounds = new HashMap<>();
            for (Map.Entry<String, Point> entry : badgeReaderPositions.entrySet()) {
                String readerId = entry.getKey();
                Point pos = entry.getValue();
                BadgeReader reader = badgeReaderMap.get(readerId);
                if (reader == null) {
                    continue;
                }
                Resource resource = cachedResources.get(reader.getResourceId());
                if (resource == null || resource.getBuilding() == null || resource.getBuilding().isBlank()) {
                    continue;
                }
                String building = resource.getBuilding();
                int[] b = bounds.computeIfAbsent(building, k -> new int[] { pos.x, pos.y, pos.x, pos.y });
                b[0] = Math.min(b[0], pos.x);
                b[1] = Math.min(b[1], pos.y);
                b[2] = Math.max(b[2], pos.x);
                b[3] = Math.max(b[3], pos.y);
            }

            int padding = 40;
            for (Map.Entry<String, int[]> entry : bounds.entrySet()) {
                int[] b = entry.getValue();
                int x = b[0] - padding;
                int y = b[1] - padding;
                int w = (b[2] - b[0]) + padding * 2;
                int h = (b[3] - b[1]) + padding * 2;
                int sx = (int) (x * scaleFactor);
                int sy = (int) (y * scaleFactor);
                int sw = (int) (w * scaleFactor);
                int sh = (int) (h * scaleFactor);
                rects.put(entry.getKey(), new Rectangle(sx, sy, sw, sh));
            }
            return rects;
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
        info.append(I18n.t("monitor.info.readerId")).append(reader.getId()).append("\n");
        
        Resource resource = cachedResources.get(reader.getResourceId());
        if (resource != null) {
            info.append(I18n.t("monitor.info.resourceName")).append(resource.getName()).append("\n");
            info.append(I18n.t("monitor.info.resourceType")).append(I18n.t("resource.type." + resource.getType().name())).append("\n");
            info.append(I18n.t("monitor.info.location")).append(resource.getLocation()).append("\n");
            info.append(I18n.t("monitor.info.building")).append(resource.getBuilding()).append("\n");
            info.append(I18n.t("monitor.info.floor")).append(resource.getFloor()).append("\n");
        } else {
            info.append(I18n.t("monitor.info.resourceId")).append(reader.getResourceId()).append("\n");
        }
        
        Point pos = badgeReaderPositions.get(readerId);
        if (pos != null) {
            info.append(I18n.f("monitor.info.posOriginal", pos.x, pos.y)).append("\n");
            double currentScale = mapViewPanel.getScaleFactor();
            info.append(I18n.f("monitor.info.posCurrent", (int)(pos.x * currentScale), (int)(pos.y * currentScale)));
        }
        
        JOptionPane.showMessageDialog(RealTimeMonitorPanel.this, 
            info.toString(), 
            I18n.t("monitor.title.readerInfo"), 
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

    private void switchToSite() {
        viewCombo.setSelectedItem(ViewType.SITE);
        mapViewPanel.setViewType(ViewType.SITE);
        mapViewPanel.repaint();
        updateFloorSidebar();
    }

    private void enterBuilding(String building) {
        if (building == null || building.isBlank()) {
            return;
        }
        selectedBuilding = building;
        selectedFloor = pickDefaultFloor(building);
        viewCombo.setSelectedItem(ViewType.BUILDING);
        mapViewPanel.setViewType(ViewType.BUILDING);
        mapViewPanel.repaint();
        updateFloorSidebar();
    }

    private void updateFloorSidebar() {
        boolean buildingView = getSelectedViewType() == ViewType.BUILDING;
        floorSidebar.setVisible(buildingView);
        if (!buildingView) {
            return;
        }
        ensureBuildingAndFloorSelected();
        buildingTitleLabel.setText(selectedBuilding == null ? "" : selectedBuilding);
        List<String> floors = getFloorsForBuilding(selectedBuilding);

        floorButtonsPanel.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (String floor : floors) {
            JToggleButton btn = new JToggleButton(floor);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(120, 28));
            btn.setSelected(floor.equals(selectedFloor));
            btn.addActionListener(e -> {
                selectedFloor = floor;
                mapViewPanel.setViewType(ViewType.BUILDING);
                mapViewPanel.repaint();
                updateFloorSidebar();
            });
            group.add(btn);
            floorButtonsPanel.add(btn);
            floorButtonsPanel.add(Box.createVerticalStrut(6));
        }
        floorButtonsPanel.revalidate();
        floorButtonsPanel.repaint();
    }

    private List<String> getFloorsForBuilding(String building) {
        List<String> floors = new ArrayList<>();
        if (building == null || building.isBlank()) {
            return floors;
        }
        for (Resource r : cachedResources.values()) {
            if (r == null) {
                continue;
            }
            if (!building.equals(r.getBuilding())) {
                continue;
            }
            String floor = r.getFloor();
            if (floor == null || floor.isBlank() || floors.contains(floor)) {
                continue;
            }
            floors.add(floor);
        }
        floors.sort((a, b) -> floorSortKey(a).compareTo(floorSortKey(b)));
        return floors;
    }

    private String floorSortKey(String floor) {
        if (floor == null) {
            return "z";
        }
        String f = floor.trim().toUpperCase();
        if (f.matches("^B\\d+$")) {
            int n = Integer.parseInt(f.substring(1));
            return "0-" + String.format("%03d", n);
        }
        if (f.matches("^\\d+F$")) {
            int n = Integer.parseInt(f.substring(0, f.length() - 1));
            return "1-" + String.format("%03d", n);
        }
        if (f.equals("G") || f.equals("GF") || f.equals("GROUND")) {
            return "1-000";
        }
        return "2-" + f;
    }

    private void refreshResources() {
        try {
            DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
            cachedResources = dbManager.loadAllResources();
        } catch (Exception e) {
            cachedResources = new HashMap<>();
        }
    }

    private boolean shouldIncludeReader(Resource resource) {
        if (getSelectedViewType() == ViewType.SITE) {
            return true;
        }
        if (resource == null) {
            return false;
        }
        if (selectedBuilding == null || selectedFloor == null) {
            return false;
        }
        return selectedBuilding.equals(resource.getBuilding()) && selectedFloor.equals(resource.getFloor());
    }

    private void ensureBuildingAndFloorSelected() {
        if (selectedBuilding != null && selectedFloor != null) {
            return;
        }
        if (cachedResources == null || cachedResources.isEmpty()) {
            refreshResources();
        }
        if (cachedResources.isEmpty()) {
            selectedBuilding = null;
            selectedFloor = null;
            return;
        }

        String building = null;
        for (Resource r : cachedResources.values()) {
            if (r != null && r.getBuilding() != null && !r.getBuilding().isBlank()) {
                building = r.getBuilding();
                break;
            }
        }
        selectedBuilding = building;
        selectedFloor = pickDefaultFloor(building);
    }

    private String pickDefaultFloor(String building) {
        if (building == null) {
            return null;
        }
        List<String> floors = new ArrayList<>();
        for (Resource r : cachedResources.values()) {
            if (r == null) {
                continue;
            }
            if (!building.equals(r.getBuilding())) {
                continue;
            }
            if (r.getFloor() != null && !r.getFloor().isBlank() && !floors.contains(r.getFloor())) {
                floors.add(r.getFloor());
            }
        }
        if (floors.contains("1F")) {
            return "1F";
        }
        floors.sort((a, b) -> floorSortKey(a).compareTo(floorSortKey(b)));
        return floors.isEmpty() ? null : floors.get(0);
    }

    private String computeViewKey() {
        ViewType type = getSelectedViewType();
        if (type == ViewType.SITE) {
            return type.getKey();
        }
        ensureBuildingAndFloorSelected();
        if (selectedBuilding == null || selectedFloor == null) {
            return type.getKey();
        }
        return type.getKey() + "." + sanitizeKey(selectedBuilding) + "." + sanitizeKey(selectedFloor);
    }

    private String sanitizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replace('\\', '_')
            .replace('/', '_')
            .replace('.', '_')
            .replace(':', '_')
            .replace('|', '_');
    }

    private void loadPositionsFromProperties(java.util.Properties props, String viewKeyPrefix) {
        if (props == null || viewKeyPrefix == null || viewKeyPrefix.isBlank()) {
            return;
        }
        String prefix = viewKeyPrefix + ".";
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            String readerId = key.substring(prefix.length());
            String value = props.getProperty(key);
            String[] coords = value.split(",");
            if (coords.length != 2) {
                continue;
            }
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            badgeReaderPositions.put(readerId, new Point(x, y));
        }
    }

    private List<String> getBackgroundImageCandidates(ViewType viewType) {
        List<String> candidates = new ArrayList<>();
        if (viewType == ViewType.SITE) {
            addImageCandidates(candidates, "site-layout.png");
            addImageCandidates(candidates, "site.png");
            addImageCandidates(candidates, "site.jpg");
            return candidates;
        }

        ensureBuildingAndFloorSelected();
        if (selectedBuilding == null || selectedFloor == null) {
            addImageCandidates(candidates, "office-layout.png");
            return candidates;
        }

        String buildingKey = selectedBuilding.trim();
        String floorKey = selectedFloor.trim();
        String[] nameVariants = new String[] {
            buildingKey + "/" + floorKey + ".png",
            buildingKey + "/" + floorKey + ".jpg",
            buildingKey + "-" + floorKey + ".png",
            buildingKey + "-" + floorKey + ".jpg"
        };
        for (String name : nameVariants) {
            candidates.add("data/images/" + name);
            candidates.add("images/" + name);
            candidates.add(name);
        }
        addImageCandidates(candidates, "office-layout.png");
        addImageCandidates(candidates, "office-layout.jpg");
        return candidates;
    }

    private void addImageCandidates(List<String> candidates, String imageName) {
        candidates.add("images/" + imageName);
        candidates.add("data/images/" + imageName);
        candidates.add(imageName);
        candidates.add("../images/" + imageName);
        candidates.add("./images/" + imageName);
    }
}
