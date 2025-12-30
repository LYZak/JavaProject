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
 * 实时监控面板 - 显示场地图和楼层图，实时显示访问尝试
 */
public class RealTimeMonitorPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private JComboBox<String> viewCombo;
    private MapViewPanel mapViewPanel;
    private JTextArea eventLogArea;
    private Map<String, Point> badgeReaderPositions; // 读卡器ID -> 位置
    private Map<String, FlashIndicator> flashIndicators; // 读卡器ID -> 闪烁指示器
    private Map<String, BadgeReader> badgeReaderMap; // 读卡器ID -> 读卡器对象
    private String selectedReaderId; // 当前选中的读卡器ID
    private Point dragStartPoint; // 拖拽起始点
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
        // 视图选择下拉框
        viewCombo = new JComboBox<>(new String[]{"场地平面图", "办公楼楼层图"});
        viewCombo.setToolTipText("场地平面图: site-layout.png | 办公楼楼层图: office-layout.png");
        viewCombo.addActionListener(e -> {
            String selected = (String) viewCombo.getSelectedItem();
            mapViewPanel.setViewType(selected);
            mapViewPanel.repaint();
        });
        
        // 地图视图面板
        mapViewPanel = new MapViewPanel();
        
        // 事件日志区域
        eventLogArea = new JTextArea(10, 30);
        eventLogArea.setEditable(false);
        eventLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 顶部控制面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("视图:"));
        topPanel.add(viewCombo);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(new JButton("刷新") {{
            addActionListener(e -> {
                loadBadgeReaderPositions();
                mapViewPanel.repaint();
            });
        }});
        topPanel.add(new JButton("配置读卡器位置") {{
            addActionListener(e -> showPositionConfigDialog());
        }});
        topPanel.add(new JButton("自动配置所有位置") {{
            addActionListener(e -> autoConfigureAllPositions());
        }});
        topPanel.add(new JButton("保存位置配置") {{
            addActionListener(e -> savePositions());
        }});
        
        // 缩放控制
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(new JLabel("缩放:"));
        JButton zoomOutBtn = new JButton("-");
        zoomOutBtn.setToolTipText("缩小");
        zoomOutBtn.addActionListener(e -> {
            mapViewPanel.zoomOut();
        });
        topPanel.add(zoomOutBtn);
        
        JLabel scaleLabel = new JLabel("100%");
        scaleLabel.setPreferredSize(new Dimension(60, 20));
        topPanel.add(scaleLabel);
        
        JButton zoomInBtn = new JButton("+");
        zoomInBtn.setToolTipText("放大");
        zoomInBtn.addActionListener(e -> {
            mapViewPanel.zoomIn();
        });
        topPanel.add(zoomInBtn);
        
        JButton resetZoomBtn = new JButton("重置");
        resetZoomBtn.setToolTipText("重置缩放");
        resetZoomBtn.addActionListener(e -> {
            mapViewPanel.resetZoom();
        });
        topPanel.add(resetZoomBtn);
        
        // 将scaleLabel传递给mapViewPanel以便更新
        mapViewPanel.setScaleLabel(scaleLabel);
        
        // 中间：地图视图（使用滚动面板以支持大图片）
        JScrollPane mapScrollPane = new JScrollPane(mapViewPanel);
        mapScrollPane.setPreferredSize(new Dimension(1000, 700));
        mapScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mapScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // 右侧：事件日志
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("实时事件日志"));
        rightPanel.add(new JScrollPane(eventLogArea), BorderLayout.CENTER);
        rightPanel.add(new JButton("清空日志") {{
            addActionListener(e -> eventLogArea.setText(""));
        }}, BorderLayout.SOUTH);
        
        // 主布局：左侧地图，右侧日志
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
        
        // 创建或更新闪烁指示器
        FlashIndicator indicator = flashIndicators.computeIfAbsent(readerId, 
            k -> new FlashIndicator(readerId));
        indicator.trigger(granted);
        
        // 刷新地图显示
        mapViewPanel.repaint();
        
        // 添加到事件日志
        String timestamp = java.time.LocalDateTime.now().toString();
        String status = granted ? "✓ 授权" : "✗ 拒绝";
        String logEntry = String.format("[%s] %s - 读卡器: %s, 资源: %s, 消息: %s%n",
            timestamp, status, readerId, request.getResourceId(), response.getMessage());
        eventLogArea.append(logEntry);
        eventLogArea.setCaretPosition(eventLogArea.getDocument().getLength());
    }
    
    private void loadBadgeReaderPositions() {
        badgeReaderPositions.clear();
        badgeReaderMap.clear();
        Router router = accessControlSystem.getRouter();
        Map<String, BadgeReader> readers = router.getBadgeReaders();
        
        // 加载保存的位置配置
        loadSavedPositions();
        
        // 为没有配置位置的读卡器分配默认位置
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
     * 从文件加载保存的读卡器位置
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
            // 忽略加载错误，使用默认位置
        }
    }
    
    /**
     * 保存读卡器位置到文件
     */
    private void savePositions() {
        try {
            File posFile = new File("data/reader_positions.properties");
            File parentDir = posFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            java.util.Properties props = new java.util.Properties();
            
            // 如果文件存在，先加载现有配置
            if (posFile.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(posFile)) {
                    props.load(fis);
                }
            }
            
            // 保存当前视图的位置
            String viewType = (String) viewCombo.getSelectedItem();
            for (Map.Entry<String, Point> entry : badgeReaderPositions.entrySet()) {
                String key = viewType + "." + entry.getKey();
                Point pos = entry.getValue();
                props.setProperty(key, pos.x + "," + pos.y);
            }
            
            // 保存到文件
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(posFile)) {
                props.store(fos, "读卡器位置配置");
            }
            
            JOptionPane.showMessageDialog(this, "位置配置已保存", "成功", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存位置配置失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 自动配置所有读卡器位置
     */
    private void autoConfigureAllPositions() {
        Router router = accessControlSystem.getRouter();
        Map<String, BadgeReader> readers = router.getBadgeReaders();
        
        if (readers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可用的读卡器", "提示", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "是否自动为所有读卡器分配位置？\n" +
            "系统将根据资源类型和位置自动分配坐标。\n" +
            "您之后可以手动调整位置。",
            "自动配置位置",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
        Map<String, Resource> resources = dbManager.loadAllResources();
        
        String viewType = (String) viewCombo.getSelectedItem();
        int totalReaders = readers.size();
        
        // 按资源类型分组分配位置
        Map<Resource.ResourceType, List<BadgeReader>> readersByType = new HashMap<>();
        for (BadgeReader reader : readers.values()) {
            Resource resource = resources.get(reader.getResourceId());
            if (resource != null) {
                readersByType.computeIfAbsent(resource.getType(), k -> new ArrayList<>())
                    .add(reader);
            }
        }
        
        // 为每种类型的读卡器分配位置区域
        int yOffset = 0;
        for (Map.Entry<Resource.ResourceType, List<BadgeReader>> entry : readersByType.entrySet()) {
            List<BadgeReader> typeReaders = entry.getValue();
            int cols = (int) Math.ceil(Math.sqrt(typeReaders.size()));
            
            for (int i = 0; i < typeReaders.size(); i++) {
                BadgeReader reader = typeReaders.get(i);
                int row = i / cols;
                int col = i % cols;
                
                Point position;
                if ("场地平面图".equals(viewType)) {
                    // 场地图：按类型分组，分布在不同的区域
                    int baseX = 150 + (yOffset % 3) * 300;
                    int baseY = 150 + (yOffset / 3) * 200;
                    position = new Point(baseX + col * 200, baseY + row * 150);
                } else {
                    // 楼层图：按类型分组
                    int baseX = 100 + (yOffset % 4) * 200;
                    int baseY = 100 + (yOffset / 4) * 150;
                    position = new Point(baseX + col * 150, baseY + row * 120);
                }
                
                badgeReaderPositions.put(reader.getId(), position);
            }
            yOffset++;
        }
        
        // 如果图片存在，使用图片尺寸来分配位置
        if (mapViewPanel.backgroundImage != null) {
            int imgWidth = mapViewPanel.backgroundImage.getWidth();
            int imgHeight = mapViewPanel.backgroundImage.getHeight();
            
            // 重新分配位置，基于图片尺寸
            yOffset = 0;
            for (Map.Entry<Resource.ResourceType, List<BadgeReader>> entry : readersByType.entrySet()) {
                List<BadgeReader> typeReaders = entry.getValue();
                int cols = (int) Math.ceil(Math.sqrt(typeReaders.size()));
                
                for (int i = 0; i < typeReaders.size(); i++) {
                    BadgeReader reader = typeReaders.get(i);
                    int row = i / cols;
                    int col = i % cols;
                    
                    // 基于图片尺寸计算位置
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
            "已为 " + totalReaders + " 个读卡器自动分配位置\n" +
            "您可以在地图上拖拽调整位置，然后点击'保存位置配置'保存。",
            "配置完成",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * 显示位置配置对话框
     */
    private void showPositionConfigDialog() {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
            "配置读卡器位置", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea(
            "配置读卡器位置说明：\n\n" +
            "功能用途：\n" +
            "• 设置读卡器在地图上的显示位置\n" +
            "• 用于实时监控面板中可视化显示读卡器位置\n" +
            "• 当有访问事件时，会在对应读卡器位置显示闪烁提示\n\n" +
            "使用方法：\n" +
            "1. 点击'自动配置所有位置'按钮可一键为所有读卡器分配位置\n" +
            "2. 在地图上点击并拖拽读卡器图标可以手动调整位置\n" +
            "3. 点击读卡器图标可以查看详细信息\n" +
            "4. 调整完成后点击'保存位置配置'按钮保存\n" +
            "5. 位置配置会按视图类型（场地平面图/楼层图）分别保存\n\n" +
            "提示：\n" +
            "• 建议先使用'自动配置所有位置'功能快速配置\n" +
            "• 然后根据实际布局手动微调位置\n" +
            "• 不同视图类型的位置配置是独立的"
        );
        infoArea.setEditable(false);
        infoArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("关闭");
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private Point calculatePosition(String resourceId, int index, int total) {
        // 根据视图类型和资源ID计算位置
        String viewType = (String) viewCombo.getSelectedItem();
        
        if ("场地平面图".equals(viewType)) {
            // 场地图：将读卡器分布在地图的不同位置
            int cols = (int) Math.ceil(Math.sqrt(total));
            int row = index / cols;
            int col = index % cols;
            int x = 150 + col * 200;
            int y = 150 + row * 150;
            return new Point(x, y);
        } else {
            // 楼层图：根据资源类型和位置分配
            int cols = (int) Math.ceil(Math.sqrt(total));
            int row = index / cols;
            int col = index % cols;
            int x = 100 + col * 150;
            int y = 100 + row * 120;
            return new Point(x, y);
        }
    }
    
    /**
     * 地图视图面板 - 绘制场地图或楼层图
     */
    private class MapViewPanel extends JPanel {
        private String viewType = "场地平面图";
        private BufferedImage backgroundImage;
        private String currentImagePath;
        private double scaleFactor = 1.0; // 缩放比例
        private static final double MIN_SCALE = 0.25; // 最小缩放比例
        private static final double MAX_SCALE = 3.0; // 最大缩放比例
        private static final double SCALE_STEP = 0.1; // 缩放步长
        private JLabel scaleLabel; // 缩放比例标签
        
        public MapViewPanel() {
            // 添加鼠标监听器用于交互
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
            
            // 添加鼠标滚轮缩放
            addMouseWheelListener(e -> {
                int rotation = e.getWheelRotation();
                if (rotation < 0) {
                    // 向上滚动，放大
                    zoomIn();
                } else {
                    // 向下滚动，缩小
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
         * 加载背景图片
         */
        private void loadBackgroundImage() {
            String imageName;
            if ("场地平面图".equals(viewType)) {
                imageName = "site-layout.png"; // 场地平面图
            } else {
                imageName = "office-layout.png"; // 办公楼楼层图
            }
            
            // 尝试从多个位置加载图片
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
                        System.err.println("加载图片失败: " + path + " - " + e.getMessage());
                    }
                }
            }
            
            // 如果图片不存在，使用null
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
            
            // 绘制背景图片或默认图形
            if (backgroundImage != null) {
                // 绘制图片背景 - 应用缩放
                int imgWidth = backgroundImage.getWidth();
                int imgHeight = backgroundImage.getHeight();
                
                int scaledWidth = (int) (imgWidth * scaleFactor);
                int scaledHeight = (int) (imgHeight * scaleFactor);
                
                // 绘制缩放后的图片
                g2d.drawImage(backgroundImage, 0, 0, scaledWidth, scaledHeight, null);
                
                // 更新面板尺寸以适应缩放后的图片
                setPreferredSize(new Dimension(scaledWidth, scaledHeight));
                revalidate();
            } else {
                // 如果没有图片，显示空白背景
                g2d.setColor(new Color(240, 240, 240));
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
            
            // 绘制读卡器和闪烁指示器（在图片上方）
            drawBadgeReaders(g2d);
        }
        
        private void drawBadgeReaders(Graphics2D g) {
            // 绘制所有读卡器位置（应用缩放）
            for (Map.Entry<String, Point> entry : badgeReaderPositions.entrySet()) {
                String readerId = entry.getKey();
                Point originalPos = entry.getValue();
                
                // 将原始位置转换为缩放后的位置
                int scaledX = (int) (originalPos.x * scaleFactor);
                int scaledY = (int) (originalPos.y * scaleFactor);
                
                // 检查是否被选中
                boolean isSelected = readerId.equals(selectedReaderId);
                
                // 检查是否有闪烁指示器
                FlashIndicator indicator = flashIndicators.get(readerId);
                int baseSize = (int) (8 * scaleFactor); // 基础大小随缩放调整
                int selectedSize = (int) (12 * scaleFactor);
                
                if (indicator != null && indicator.isActive()) {
                    // 绘制闪烁的点
                    Color color = indicator.isGranted() ? Color.GREEN : Color.RED;
                    g.setColor(color);
                    int size = (int) (indicator.getFlashSize() * scaleFactor);
                    g.fillOval(scaledX - size/2, scaledY - size/2, size, size);
                    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
                    g.fillOval(scaledX - size, scaledY - size, size * 2, size * 2);
                } else {
                    // 绘制普通读卡器图标
                    if (isSelected) {
                        // 选中状态：绘制外圈
                        g.setColor(new Color(255, 200, 0, 150));
                        g.fillOval(scaledX - selectedSize, scaledY - selectedSize, selectedSize * 2, selectedSize * 2);
                    }
                    g.setColor(isSelected ? Color.ORANGE : Color.BLUE);
                    g.fillOval(scaledX - baseSize, scaledY - baseSize, baseSize * 2, baseSize * 2);
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke((float) (2 * scaleFactor)));
                    g.drawOval(scaledX - baseSize, scaledY - baseSize, baseSize * 2, baseSize * 2);
                }
                
                // 绘制读卡器标签（字体大小也随缩放调整）
                g.setColor(Color.BLACK);
                int fontSize = Math.max(10, (int) (11 * scaleFactor));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
                BadgeReader reader = badgeReaderMap.get(readerId);
                String label = reader != null ? getReaderLabel(reader) : "R" + readerId.substring(0, Math.min(4, readerId.length()));
                g.drawString(label, scaledX + (int)(10 * scaleFactor), scaledY - (int)(10 * scaleFactor));
            }
        }
        
        /**
         * 获取读卡器标签
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
                // 忽略错误
            }
            return "R" + reader.getId().substring(0, Math.min(4, reader.getId().length()));
        }
        
        /**
         * 处理鼠标点击
         */
        private void handleMouseClick(MouseEvent e) {
            if (isDragging) {
                return; // 如果是拖拽结束，不处理点击
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
         * 处理鼠标按下
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
         * 处理鼠标释放
         */
        private void handleMouseRelease(MouseEvent e) {
            if (isDragging && selectedReaderId != null) {
                // 拖拽结束，更新位置
                Point newPos = e.getPoint();
                badgeReaderPositions.put(selectedReaderId, newPos);
                repaint();
            }
            isDragging = false;
        }
        
        /**
         * 处理鼠标拖拽（考虑缩放）
         */
        private void handleMouseDrag(MouseEvent e) {
            if (selectedReaderId != null && dragStartPoint != null) {
                int dx = Math.abs(e.getX() - dragStartPoint.x);
                int dy = Math.abs(e.getY() - dragStartPoint.y);
                
                if (dx > 5 || dy > 5) { // 拖拽阈值
                    isDragging = true;
                    Point scaledPos = e.getPoint();
                    // 将缩放后的位置转换回原始位置
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
         * 查找点击位置的读卡器（考虑缩放）
         */
        private String findReaderAtPoint(Point point) {
            for (Map.Entry<String, Point> entry : badgeReaderPositions.entrySet()) {
                Point originalPos = entry.getValue();
                // 将原始位置转换为缩放后的位置
                int scaledX = (int) (originalPos.x * scaleFactor);
                int scaledY = (int) (originalPos.y * scaleFactor);
                Point scaledPos = new Point(scaledX, scaledY);
                
                double distance = point.distance(scaledPos);
                int clickRadius = (int) (15 * scaleFactor); // 点击范围随缩放调整
                if (distance <= clickRadius) {
                    return entry.getKey();
                }
            }
            return null;
        }
        
        /**
         * 显示读卡器信息
         */
        private void showReaderInfo(String readerId) {
            BadgeReader reader = badgeReaderMap.get(readerId);
            if (reader == null) {
                return;
            }
            
            StringBuilder info = new StringBuilder();
            info.append("读卡器ID: ").append(reader.getId()).append("\n");
            
            try {
                DatabaseManager dbManager = accessControlSystem.getDatabaseManager();
                Map<String, Resource> resources = dbManager.loadAllResources();
                Resource resource = resources.get(reader.getResourceId());
                if (resource != null) {
                    info.append("资源名称: ").append(resource.getName()).append("\n");
                    info.append("资源类型: ").append(resource.getType()).append("\n");
                    info.append("位置: ").append(resource.getLocation()).append("\n");
                    info.append("建筑: ").append(resource.getBuilding()).append("\n");
                    info.append("楼层: ").append(resource.getFloor()).append("\n");
                }
            } catch (Exception e) {
                info.append("资源ID: ").append(reader.getResourceId()).append("\n");
            }
            
            Point pos = badgeReaderPositions.get(readerId);
            if (pos != null) {
                info.append("位置坐标（原始）: (").append(pos.x).append(", ").append(pos.y).append(")\n");
                double currentScale = mapViewPanel.getScaleFactor();
                info.append("位置坐标（当前缩放）: (").append((int)(pos.x * currentScale))
                    .append(", ").append((int)(pos.y * currentScale)).append(")");
            }
            
            JOptionPane.showMessageDialog(RealTimeMonitorPanel.this, 
                info.toString(), 
                "读卡器信息", 
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        @Override
        public Dimension getPreferredSize() {
            if (backgroundImage != null) {
                // 如果有背景图片，使用图片尺寸
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
     * 闪烁指示器 - 表示访问尝试的视觉反馈
     */
    private class FlashIndicator {
        private String readerId;
        private boolean active;
        private boolean granted;
        private long triggerTime;
        private static final long FLASH_DURATION = 2000; // 闪烁持续时间（毫秒）
        
        public FlashIndicator(String readerId) {
            this.readerId = readerId;
            this.active = false;
        }
        
        public void trigger(boolean granted) {
            this.granted = granted;
            this.active = true;
            this.triggerTime = System.currentTimeMillis();
            
            // 启动定时器，自动关闭闪烁
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
            // 闪烁效果：大小在10-30之间变化
            double progress = (elapsed % 400) / 400.0;
            int baseSize = 15;
            int variation = (int) (10 * Math.sin(progress * Math.PI * 2));
            return baseSize + variation;
        }
    }
}
