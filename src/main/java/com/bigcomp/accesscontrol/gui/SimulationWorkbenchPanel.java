package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.core.Router;
import com.bigcomp.accesscontrol.database.DatabaseManager;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.User;
import com.bigcomp.accesscontrol.simulation.DemoDataGenerator;
import com.bigcomp.accesscontrol.simulation.EventScriptParser;
import com.bigcomp.accesscontrol.simulation.engine.RealtimeSimulationEngine;
import com.bigcomp.accesscontrol.simulation.engine.SimulationEngine;
import com.bigcomp.accesscontrol.simulation.engine.SimulationMetrics;
import com.bigcomp.accesscontrol.simulation.engine.StressSimulationConfig;
import com.bigcomp.accesscontrol.simulation.engine.StressSimulationEngine;
import com.bigcomp.accesscontrol.util.SystemClock;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SimulationWorkbenchPanel extends JPanel {
    private static final int MODE_REALTIME = 0;
    private static final int MODE_STRESS = 1;
    private static final int MODE_MANUAL = 2;

    private final AccessControlSystem accessControlSystem;
    private final SimulationMetrics metrics = new SimulationMetrics();

    private JTable userTable;
    private JTable readerTable;
    private DefaultListModel<String> eventQueueModel;
    private JTextArea feedArea;
    private JLabel statsLabel;
    private JLabel timeLabel;
    private JTextArea detailsArea;
    private JLabel detailsHeader;

    private JComboBox<String> modeCombo;
    private JPanel modeCard;
    private JButton startButton;
    private JButton stopButton;
    private JSplitPane rightSplit;
    private JPanel queuePanel;
    private JPanel queuePlaceholder;

    private JSpinner realtimeIntervalSpinner;
    private JCheckBox realtimeConsistentCheck;

    private JSpinner stressThreadsSpinner;
    private JSpinner stressRpsSpinner;
    private JSpinner stressDurationSpinner;
    private JComboBox<StressSimulationConfig.RateModel> stressRateModelCombo;
    private JSpinner stressStepStartSpinner;
    private JSpinner stressStepEndSpinner;
    private JSpinner stressStepSecondsSpinner;

    private JComboBox<String> eventActionCombo;
    private JComboBox<String> eventUserCombo;
    private JComboBox<String> eventReaderCombo;
    private JComboBox<String> eventResourceCombo;
    private JSpinner eventCountSpinner;
    private JButton queueAddButton;
    private JButton queueRunButton;
    private JButton queueClearButton;
    private JButton queueImportButton;
    private JList<String> queueList;

    private JSpinner genUsersSpinner;
    private JSpinner genResourcesSpinner;
    private JProgressBar progressBar;

    private SimulationEngine engine;
    private RealtimeSimulationEngine realtimeEngine;
    private StressSimulationEngine stressEngine;

    private Router.AccessEventListener accessListener;
    private Router.ReaderEventListener readerListener;
    private final javax.swing.Timer uiTimer;

    public SimulationWorkbenchPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        eventQueueModel = new DefaultListModel<>();
        add(buildMain(), BorderLayout.CENTER);
        refreshData();
        installSelectionListeners();
        uiTimer = new javax.swing.Timer(500, e -> refreshStatsAndTime());
        uiTimer.start();
    }

    public void applyLanguage() {
        SwingUtilities.invokeLater(() -> {
            stopEngine();
            unregisterRouterListeners();
            removeAll();
            add(buildMain(), BorderLayout.CENTER);
            refreshData();
            installSelectionListeners();
            showNoSelectionDetails();
            switchMode();
            registerRouterListeners();
            revalidate();
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        registerRouterListeners();
    }

    @Override
    public void removeNotify() {
        unregisterRouterListeners();
        uiTimer.stop();
        stopEngine();
        super.removeNotify();
    }

    private JComponent buildMain() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeft(), buildRight());
        split.setResizeWeight(0.45);
        split.setDividerSize(8);
        split.setBorder(null);
        return split;
    }

    private JComponent buildLeft() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(I18n.t("simw.tab.users"), buildUsersPanel());
        tabs.addTab(I18n.t("simw.tab.readers"), buildReadersPanel());
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, buildDetails());
        split.setResizeWeight(0.70);
        split.setDividerSize(8);
        split.setBorder(null);
        return split;
    }

    private JComponent buildUsersPanel() {
        userTable = new JTable(new javax.swing.table.DefaultTableModel(new Object[]{
            I18n.t("simw.col.userId"),
            I18n.t("simw.col.userName"),
            I18n.t("simw.col.badgeCode")
        }, 0));
        userTable.setAutoCreateRowSorter(true);
        return new JScrollPane(userTable);
    }

    private JComponent buildReadersPanel() {
        readerTable = new JTable(new javax.swing.table.DefaultTableModel(new Object[]{
            I18n.t("simw.col.readerId"),
            I18n.t("simw.col.resourceId")
        }, 0));
        readerTable.setAutoCreateRowSorter(true);
        return new JScrollPane(readerTable);
    }

    private JComponent buildRight() {
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.add(buildControls(), BorderLayout.NORTH);
        queuePlaceholder = new JPanel();
        queuePlaceholder.setPreferredSize(new Dimension(0, 0));
        queuePanel = buildQueuePanel();
        JComponent feed = buildFeed();
        rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, feed, queuePlaceholder);
        rightSplit.setResizeWeight(1.0);
        rightSplit.setDividerSize(8);
        rightSplit.setBorder(null);
        right.add(rightSplit, BorderLayout.CENTER);
        return right;
    }

    private JPanel buildQueuePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel(I18n.t("simw.queue.title")), BorderLayout.NORTH);
        queueList = new JList<>(eventQueueModel);
        panel.add(new JScrollPane(queueList), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        queueRunButton = new JButton(I18n.t("simw.action.runQueue"));
        queueRunButton.addActionListener(e -> runQueueWithProgress());
        queueClearButton = new JButton(I18n.t("simw.action.clearQueue"));
        queueClearButton.addActionListener(e -> eventQueueModel.clear());
        queueImportButton = new JButton(I18n.t("simw.action.importScript"));
        queueImportButton.addActionListener(e -> importScriptIntoQueue());
        actions.add(queueRunButton);
        actions.add(queueClearButton);
        actions.add(queueImportButton);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildDetails() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        detailsHeader = new JLabel(I18n.t("simw.details.title"));
        panel.add(detailsHeader, BorderLayout.NORTH);
        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        showNoSelectionDetails();
        return panel;
    }

    private void installSelectionListeners() {
        if (userTable != null) {
            userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            userTable.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                showSelectedUserDetails();
            });
        }
        if (readerTable != null) {
            readerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            readerTable.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                showSelectedReaderDetails();
            });
        }
    }

    private void showNoSelectionDetails() {
        if (detailsArea == null) {
            return;
        }
        detailsArea.setText(I18n.t("simw.details.noneSelected"));
        if (detailsHeader != null) {
            detailsHeader.setText(I18n.t("simw.details.title"));
        }
    }

    private void showSelectedUserDetails() {
        if (userTable == null) {
            return;
        }
        int viewRow = userTable.getSelectedRow();
        Object userId = SimulationDetailsFormatter.modelValue(userTable, viewRow, 0);
        Object badgeCode = SimulationDetailsFormatter.modelValue(userTable, viewRow, 2);
        if (userId == null) {
            return;
        }
        String text = buildUserDetailsText(userId.toString(), badgeCode != null ? badgeCode.toString() : "");
        detailsArea.setText(text);
        if (detailsHeader != null) {
            detailsHeader.setText(I18n.t("simw.details.userTitle"));
        }
        detailsArea.setCaretPosition(0);
    }

    private void showSelectedReaderDetails() {
        if (readerTable == null) {
            return;
        }
        int viewRow = readerTable.getSelectedRow();
        Object readerId = SimulationDetailsFormatter.modelValue(readerTable, viewRow, 0);
        if (readerId == null) {
            return;
        }
        String text = buildReaderDetailsText(readerId.toString());
        detailsArea.setText(text);
        if (detailsHeader != null) {
            detailsHeader.setText(I18n.t("simw.details.readerTitle"));
        }
        detailsArea.setCaretPosition(0);
    }

    private String buildUserDetailsText(String userId, String badgeCode) {
        DatabaseManager db = accessControlSystem.getDatabaseManager();
        User user = db.loadAllUsers().get(userId);
        Badge badge = null;
        if (user != null) {
            badge = user.getBadgeId() != null ? db.loadBadgeById(user.getBadgeId()) : db.loadBadgeByUserId(user.getId());
        }
        var profilesByUser = db.loadUserProfiles();
        var profiles = profilesByUser.get(userId);
        return SimulationDetailsFormatter.formatUser(user, badge, badgeCode, profiles);
    }

    private String buildReaderDetailsText(String readerId) {
        DatabaseManager db = accessControlSystem.getDatabaseManager();
        BadgeReader reader = accessControlSystem.getRouter().getBadgeReaders().get(readerId);
        Resource resource = null;
        String groupName = null;

        if (reader != null) {
            resource = db.loadAllResources().get(reader.getResourceId());
        }
        if (resource != null) {
            groupName = db.loadResourceGroups().get(resource.getId());
        }
        return SimulationDetailsFormatter.formatReader(reader, resource, groupName);
    }

    private JComponent buildControls() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridx = 0; gbc.gridy = 0;
        modeCombo = new JComboBox<>(new String[]{I18n.t("simw.mode.realtime"), I18n.t("simw.mode.stress"), I18n.t("simw.mode.manual")});
        modeCombo.addActionListener(e -> switchMode());
        top.add(modeCombo, gbc);

        gbc.gridx = 1;
        startButton = new JButton(I18n.t("simw.action.start"));
        startButton.addActionListener(e -> startEngine());
        top.add(startButton, gbc);

        gbc.gridx = 2;
        stopButton = new JButton(I18n.t("simw.action.stop"));
        stopButton.addActionListener(e -> stopEngine());
        top.add(stopButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 3;
        statsLabel = new JLabel();
        top.add(statsLabel, gbc);

        gbc.gridy = 2;
        timeLabel = new JLabel();
        top.add(timeLabel, gbc);

        gbc.gridy = 3;
        top.add(buildTimeControls(), gbc);

        gbc.gridy = 4;
        top.add(buildModeCards(), gbc);

        gbc.gridy = 5;
        top.add(buildDataGeneratorControls(), gbc);

        gbc.gridy = 6;
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        top.add(progressBar, gbc);

        panel.add(top, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildTimeControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton setTime = new JButton(I18n.t("simw.action.setTime"));
        setTime.addActionListener(e -> showSetTimeDialog());
        JButton reset = new JButton(I18n.t("simw.action.resetTime"));
        reset.addActionListener(e -> {
            SystemClock.clearCustomTime();
            refreshStatsAndTime();
        });

        JComboBox<String> scale = new JComboBox<>(new String[]{"0.5x", "1x", "5x", "10x"});
        scale.setSelectedItem("1x");
        scale.addActionListener(e -> {
            Object selected = scale.getSelectedItem();
            if (selected instanceof String s) {
                String cleaned = s.trim().toLowerCase().replace("x", "");
                try {
                    double v = Double.parseDouble(cleaned);
                    SystemClock.setTimeScale(v);
                } catch (Exception ignored) {
                }
            }
        });

        JButton stepH = new JButton(I18n.t("simw.action.stepHour"));
        stepH.addActionListener(e -> SystemClock.step(Duration.ofHours(1)));
        JButton stepD = new JButton(I18n.t("simw.action.stepDay"));
        stepD.addActionListener(e -> SystemClock.step(Duration.ofDays(1)));

        panel.add(setTime);
        panel.add(reset);
        panel.add(new JLabel(I18n.t("simw.field.scale")));
        panel.add(scale);
        panel.add(stepH);
        panel.add(stepD);
        return panel;
    }

    private JComponent buildModeCards() {
        modeCard = new JPanel(new CardLayout());
        modeCard.add(buildRealtimeConfig(), "Realtime");
        modeCard.add(buildStressConfig(), "Stress");
        modeCard.add(buildManualConfig(), "Manual");
        return modeCard;
    }

    private JComponent buildRealtimeConfig() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        realtimeIntervalSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 60, 1));
        realtimeConsistentCheck = new JCheckBox(I18n.t("simw.field.consistentPath"), true);
        JButton refresh = new JButton(I18n.t("common.refresh"));
        refresh.addActionListener(e -> refreshData());
        panel.add(new JLabel(I18n.t("simw.field.intervalSeconds")));
        panel.add(realtimeIntervalSpinner);
        panel.add(realtimeConsistentCheck);
        panel.add(refresh);
        return panel;
    }

    private JComponent buildStressConfig() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        stressThreadsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 128, 1));
        stressRpsSpinner = new JSpinner(new SpinnerNumberModel(200, 1, 200000, 50));
        stressDurationSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        stressRateModelCombo = new JComboBox<>(StressSimulationConfig.RateModel.values());
        stressRateModelCombo.setSelectedItem(StressSimulationConfig.RateModel.FIXED);
        stressRateModelCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof StressSimulationConfig.RateModel m) {
                    if (m == StressSimulationConfig.RateModel.FIXED) setText(I18n.t("simw.rate.fixed"));
                    else if (m == StressSimulationConfig.RateModel.POISSON) setText(I18n.t("simw.rate.poisson"));
                    else setText(I18n.t("simw.rate.step"));
                }
                return this;
            }
        });
        stressStepStartSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 200000, 50));
        stressStepEndSpinner = new JSpinner(new SpinnerNumberModel(500, 1, 200000, 50));
        stressStepSecondsSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 3600, 1));
        panel.add(new JLabel(I18n.t("simw.field.threads")));
        panel.add(stressThreadsSpinner);
        panel.add(new JLabel(I18n.t("simw.field.rps")));
        panel.add(stressRpsSpinner);
        panel.add(new JLabel(I18n.t("simw.field.durationSeconds")));
        panel.add(stressDurationSpinner);
        panel.add(new JLabel(I18n.t("simw.field.rateModel")));
        panel.add(stressRateModelCombo);
        panel.add(new JLabel(I18n.t("simw.field.stepStart")));
        panel.add(stressStepStartSpinner);
        panel.add(new JLabel(I18n.t("simw.field.stepEnd")));
        panel.add(stressStepEndSpinner);
        panel.add(new JLabel(I18n.t("simw.field.stepSeconds")));
        panel.add(stressStepSecondsSpinner);
        return panel;
    }

    private JComponent buildManualConfig() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        eventActionCombo = new JComboBox<>(new String[]{
            I18n.t("simw.event.swipe"),
            I18n.t("simw.event.updateBadge"),
            I18n.t("simw.event.submitRequest"),
            I18n.t("simw.event.stepHour"),
            I18n.t("simw.event.stepDay"),
            I18n.t("simw.event.setResourceUncontrolled"),
            I18n.t("simw.event.setResourceControlled")
        });
        eventUserCombo = new JComboBox<>();
        eventReaderCombo = new JComboBox<>();
        eventResourceCombo = new JComboBox<>();
        eventCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 10));

        queueAddButton = new JButton(I18n.t("simw.action.queue"));
        queueAddButton.addActionListener(e -> queueEvent());

        panel.add(eventActionCombo);
        panel.add(new JLabel(I18n.t("simw.field.user")));
        panel.add(eventUserCombo);
        panel.add(new JLabel(I18n.t("simw.field.reader")));
        panel.add(eventReaderCombo);
        panel.add(new JLabel(I18n.t("simw.field.resource")));
        panel.add(eventResourceCombo);
        panel.add(new JLabel(I18n.t("simw.field.count")));
        panel.add(eventCountSpinner);
        panel.add(queueAddButton);
        return panel;
    }

    private JComponent buildDataGeneratorControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        genUsersSpinner = new JSpinner(new SpinnerNumberModel(300, 1, 2000, 50));
        genResourcesSpinner = new JSpinner(new SpinnerNumberModel(400, 1, 5000, 50));
        JButton generate = new JButton(I18n.t("simw.action.generateDemoData"));
        generate.addActionListener(e -> generateDemoData());
        panel.add(new JLabel(I18n.t("simw.field.users")));
        panel.add(genUsersSpinner);
        panel.add(new JLabel(I18n.t("simw.field.resourcesReaders")));
        panel.add(genResourcesSpinner);
        panel.add(generate);
        return panel;
    }

    private JComponent buildFeed() {
        feedArea = new JTextArea();
        feedArea.setEditable(false);
        return new JScrollPane(feedArea);
    }

    private void switchMode() {
        CardLayout cl = (CardLayout) modeCard.getLayout();
        int idx = modeCombo.getSelectedIndex();
        if (idx == MODE_STRESS) {
            cl.show(modeCard, "Stress");
        } else if (idx == MODE_MANUAL) {
            cl.show(modeCard, "Manual");
        } else {
            cl.show(modeCard, "Realtime");
        }

        boolean manual = idx == MODE_MANUAL;
        if (startButton != null) startButton.setEnabled(!manual);
        if (stopButton != null) stopButton.setEnabled(!manual);

        if (rightSplit != null) {
            rightSplit.setRightComponent(manual ? queuePanel : queuePlaceholder);
            SwingUtilities.invokeLater(() -> rightSplit.setDividerLocation(manual ? 0.75 : 1.0));
        }
    }

    private void startEngine() {
        if (modeCombo.getSelectedIndex() == MODE_MANUAL) {
            appendFeed(I18n.t("simw.msg.manualNoEngine") + "\n");
            return;
        }
        stopEngine();
        if (modeCombo.getSelectedIndex() == MODE_STRESS) {
            StressSimulationConfig cfg = new StressSimulationConfig();
            cfg.setThreads((Integer) stressThreadsSpinner.getValue());
            cfg.setRequestsPerSecond((Integer) stressRpsSpinner.getValue());
            cfg.setDurationSeconds((Integer) stressDurationSpinner.getValue());
            cfg.setSeed(1L);
            cfg.setRateModel((StressSimulationConfig.RateModel) stressRateModelCombo.getSelectedItem());
            cfg.setStepStartRps((Integer) stressStepStartSpinner.getValue());
            cfg.setStepEndRps((Integer) stressStepEndSpinner.getValue());
            cfg.setStepSeconds((Integer) stressStepSecondsSpinner.getValue());
            stressEngine = new StressSimulationEngine(accessControlSystem.getRouter(), metrics);
            stressEngine.setConfig(cfg);
            populateStressTargets(stressEngine);
            engine = stressEngine;
        } else {
            realtimeEngine = new RealtimeSimulationEngine(loadReadersList(), loadResourcesMap(), 1L, metrics);
            realtimeEngine.setIntervalSeconds((Integer) realtimeIntervalSpinner.getValue());
            realtimeEngine.setConsistentBehavior(realtimeConsistentCheck.isSelected());
            populateRealtimeTargets(realtimeEngine);
            engine = realtimeEngine;
        }
        engine.start();
        appendFeed(I18n.f("simw.msg.engineStarted", modeCombo.getSelectedIndex() == MODE_STRESS ? I18n.t("simw.mode.stress") : I18n.t("simw.mode.realtime")) + "\n");
    }

    private void stopEngine() {
        if (engine != null) {
            engine.stop();
            engine = null;
            realtimeEngine = null;
            stressEngine = null;
            appendFeed(I18n.t("simw.msg.engineStopped") + "\n");
        }
    }

    private void populateRealtimeTargets(RealtimeSimulationEngine engine) {
        Map<String, User> users = accessControlSystem.getDatabaseManager().loadAllUsers();
        Map<String, Badge> badges = accessControlSystem.getDatabaseManager().loadAllBadges();
        for (User user : users.values()) {
            Badge badge = null;
            if (user.getBadgeId() != null) {
                badge = accessControlSystem.getDatabaseManager().loadBadgeById(user.getBadgeId());
            }
            if (badge == null) {
                badge = badges.values().stream().filter(b -> user.getId().equals(b.getUserId())).findFirst().orElse(null);
            }
            if (badge != null) {
                engine.addSimulatedUser(user, badge);
            }
        }
    }

    private void populateStressTargets(StressSimulationEngine engine) {
        DatabaseManager db = accessControlSystem.getDatabaseManager();
        Map<String, User> users = db.loadAllUsers();
        for (User user : users.values()) {
            Badge badge = user.getBadgeId() != null ? db.loadBadgeById(user.getBadgeId()) : db.loadBadgeByUserId(user.getId());
            if (badge != null) {
                engine.addUser(user, badge);
            }
        }
        Map<String, Resource> resources = db.loadAllResources();
        for (Resource resource : resources.values()) {
            engine.addResource(resource, accessControlSystem.getRouter().getBadgeReaders().get(resource.getBadgeReaderId()));
        }
    }

    private void queueEvent() {
        String action = (String) eventActionCombo.getSelectedItem();
        String user = (String) eventUserCombo.getSelectedItem();
        String reader = (String) eventReaderCombo.getSelectedItem();
        String resource = (String) eventResourceCombo.getSelectedItem();
        int count = (Integer) eventCountSpinner.getValue();
        String line = action + " user=" + user + " reader=" + reader + " resource=" + resource + " x" + count;
        eventQueueModel.addElement(line);
    }

    private void runQueue() {
        if (eventQueueModel.isEmpty()) {
            return;
        }
        for (int i = 0; i < eventQueueModel.size(); i++) {
            executeQueuedLine(eventQueueModel.get(i));
        }
        appendFeed(I18n.t("simw.msg.queueExecuted") + "\n");
    }

    private void runQueueWithProgress() {
        if (eventQueueModel.isEmpty()) {
            return;
        }
        progressBar.setValue(0);
        progressBar.setString(I18n.t("simw.progress.running"));
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int total = eventQueueModel.size();
                for (int i = 0; i < total; i++) {
                    executeQueuedLine(eventQueueModel.get(i));
                    int percent = (int) ((i + 1) * 100L / total);
                    publish(percent);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int v = chunks.get(chunks.size() - 1);
                progressBar.setValue(v);
                progressBar.setString(v + "%");
            }

            @Override
            protected void done() {
                progressBar.setValue(100);
                progressBar.setString(I18n.t("simw.progress.done"));
                appendFeed(I18n.t("simw.msg.queueExecuted") + "\n");
            }
        };
        worker.execute();
    }

    private void importScriptIntoQueue() {
        JFileChooser chooser = new JFileChooser();
        int ret = chooser.showOpenDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        EventScriptParser parser = new EventScriptParser();
        List<String> lines;
        try {
            lines = parser.parseFile(path);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (String l : lines) {
            eventQueueModel.addElement(l);
        }
        appendFeed(I18n.f("simw.msg.importedLines", lines.size()) + "\n");
    }

    private void executeQueuedLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            return;
        }
        String action = normalizeAction(parts[0]);
        String userPart = parts[1];
        String readerPart = parts[2];
        String resourcePart = parts[3];
        String countPart = parts.length >= 5 ? parts[4] : "x1";

        String badgeCode = userPart.replace("user=", "");
        String readerId = readerPart.replace("reader=", "");
        String resourceId = resourcePart.replace("resource=", "");
        int count = 1;
        try {
            count = Integer.parseInt(countPart.replace("x", ""));
        } catch (Exception ignored) {
        }

        BadgeReader reader = accessControlSystem.getRouter().getBadgeReaders().get(readerId);
        DatabaseManager db = accessControlSystem.getDatabaseManager();
        Badge badge = db.loadAllBadges().values().stream().filter(b -> badgeCode.equals(b.getCode())).findFirst().orElse(null);

        for (int i = 0; i < count; i++) {
            if ("Swipe".equals(action) && reader != null && badge != null) {
                reader.swipeBadge(badge);
            } else if ("UpdateBadge".equals(action) && reader != null && badge != null) {
                reader.updateBadge(badge);
            } else if ("SubmitRequest".equals(action) && badge != null) {
                accessControlSystem.getRouter().submitAccessRequest(new com.bigcomp.accesscontrol.model.AccessRequest(
                    badge.getCode(),
                    readerId,
                    resourceId,
                    SystemClock.now()
                ));
            } else if ("StepTime(+1h)".equals(action)) {
                SystemClock.step(Duration.ofHours(1));
            } else if ("StepTime(+1d)".equals(action)) {
                SystemClock.step(Duration.ofDays(1));
            } else if ("SetResource(UNCONTROLLED)".equals(action)) {
                Resource res = db.loadAllResources().get(resourceId);
                if (res != null) {
                    res.setState(Resource.ResourceState.UNCONTROLLED);
                    try {
                        db.addResource(res);
                        accessControlSystem.getAccessRequestProcessor().reloadData();
                    } catch (Exception ex) {
                        appendFeed(I18n.f("simw.msg.updateResourceFailed", ex.getMessage()) + "\n");
                    }
                }
            } else if ("SetResource(CONTROLLED)".equals(action)) {
                Resource res = db.loadAllResources().get(resourceId);
                if (res != null) {
                    res.setState(Resource.ResourceState.CONTROLLED);
                    try {
                        db.addResource(res);
                        accessControlSystem.getAccessRequestProcessor().reloadData();
                    } catch (Exception ex) {
                        appendFeed(I18n.f("simw.msg.updateResourceFailed", ex.getMessage()) + "\n");
                    }
                }
            }
        }
    }

    private String normalizeAction(String action) {
        if (action == null) {
            return "";
        }
        if (action.equals(I18n.t("simw.event.swipe"))) return "Swipe";
        if (action.equals(I18n.t("simw.event.updateBadge"))) return "UpdateBadge";
        if (action.equals(I18n.t("simw.event.submitRequest"))) return "SubmitRequest";
        if (action.equals(I18n.t("simw.event.stepHour"))) return "StepTime(+1h)";
        if (action.equals(I18n.t("simw.event.stepDay"))) return "StepTime(+1d)";
        if (action.equals(I18n.t("simw.event.setResourceUncontrolled"))) return "SetResource(UNCONTROLLED)";
        if (action.equals(I18n.t("simw.event.setResourceControlled"))) return "SetResource(CONTROLLED)";
        if (action.equals("刷卡")) return "Swipe";
        if (action.equals("更新徽章")) return "UpdateBadge";
        if (action.equals("提交请求")) return "SubmitRequest";
        if (action.equals("时间步进(+1小时)")) return "StepTime(+1h)";
        if (action.equals("时间步进(+1天)")) return "StepTime(+1d)";
        if (action.equals("资源设为(不受控)")) return "SetResource(UNCONTROLLED)";
        if (action.equals("资源设为(受控)")) return "SetResource(CONTROLLED)";
        return action;
    }

    private void generateDemoData() {
        int users = (Integer) genUsersSpinner.getValue();
        int resources = (Integer) genResourcesSpinner.getValue();
        DemoDataGenerator generator = new DemoDataGenerator();
        progressBar.setValue(0);
        progressBar.setString(I18n.t("simw.progress.generating"));
        appendFeed(I18n.t("simw.msg.generatingDemoData") + "\n");
        SwingWorker<DemoDataGenerator.Result, int[]> worker = new SwingWorker<>() {
            @Override
            protected DemoDataGenerator.Result doInBackground() {
                return generator.generate(accessControlSystem, users, resources, 1L, (percent, message) -> {
                    publish(new int[]{percent});
                    appendFeed(message + "\n");
                });
            }

            @Override
            protected void process(List<int[]> chunks) {
                int[] last = chunks.get(chunks.size() - 1);
                int v = last[0];
                progressBar.setValue(v);
                progressBar.setString(v + "%");
            }

            @Override
            protected void done() {
                try {
                    DemoDataGenerator.Result result = get();
                    appendFeed(I18n.f("simw.msg.generatedSummary", result.getUsersCreated(), result.getResourcesCreated(), result.getReadersCreated()) + "\n");
                } catch (Exception e) {
                    appendFeed(I18n.f("simw.msg.generateFailed", e.getMessage()) + "\n");
                }
                progressBar.setValue(100);
                progressBar.setString(I18n.t("simw.progress.done"));
                refreshData();
            }
        };
        worker.execute();
    }

    private void refreshData() {
        syncReadersFromDatabase();
        loadUsersIntoTable();
        loadReadersIntoTable();
        refreshEventCombos();
    }

    private void syncReadersFromDatabase() {
        DatabaseManager db = accessControlSystem.getDatabaseManager();
        Map<String, Resource> resources = db.loadAllResources();
        var router = accessControlSystem.getRouter();
        for (Resource resource : resources.values()) {
            String readerId = resource.getBadgeReaderId();
            if (readerId == null || readerId.isBlank()) {
                continue;
            }
            BadgeReader existing = router.getBadgeReaders().get(readerId);
            if (existing == null) {
                router.registerBadgeReader(new BadgeReader(readerId, resource.getId()));
            }
        }
    }

    private void loadUsersIntoTable() {
        DatabaseManager db = accessControlSystem.getDatabaseManager();
        Map<String, User> users = db.loadAllUsers();
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) userTable.getModel();
        model.setRowCount(0);
        Map<String, Badge> badges = db.loadAllBadges();
        Map<String, Badge> badgesByUser = new HashMap<>();
        for (Badge badge : badges.values()) {
            badgesByUser.put(badge.getUserId(), badge);
        }
        for (User user : users.values()) {
            Badge badge = user.getBadgeId() != null ? db.loadBadgeById(user.getBadgeId()) : badgesByUser.get(user.getId());
            String badgeCode = badge != null ? badge.getCode() : "";
            model.addRow(new Object[]{user.getId(), user.getFullName(), badgeCode});
        }
    }

    private void loadReadersIntoTable() {
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) readerTable.getModel();
        model.setRowCount(0);
        for (BadgeReader reader : accessControlSystem.getRouter().getBadgeReaders().values()) {
            model.addRow(new Object[]{reader.getId(), reader.getResourceId()});
        }
    }

    private void refreshEventCombos() {
        eventUserCombo.removeAllItems();
        javax.swing.table.DefaultTableModel um = (javax.swing.table.DefaultTableModel) userTable.getModel();
        for (int r = 0; r < um.getRowCount(); r++) {
            Object code = um.getValueAt(r, 2);
            if (code != null && !code.toString().isBlank()) {
                eventUserCombo.addItem(code.toString());
            }
        }

        eventReaderCombo.removeAllItems();
        eventResourceCombo.removeAllItems();
        javax.swing.table.DefaultTableModel rm = (javax.swing.table.DefaultTableModel) readerTable.getModel();
        for (int r = 0; r < rm.getRowCount(); r++) {
            Object readerId = rm.getValueAt(r, 0);
            Object resourceId = rm.getValueAt(r, 1);
            if (readerId != null) {
                eventReaderCombo.addItem(readerId.toString());
            }
            if (resourceId != null) {
                eventResourceCombo.addItem(resourceId.toString());
            }
        }
    }

    private void registerRouterListeners() {
        if (accessListener != null || readerListener != null) {
            return;
        }
        Router router = accessControlSystem.getRouter();
        accessListener = (request, response) -> SwingUtilities.invokeLater(() -> {
            if (response != null && response.isGranted()) {
                metrics.recordGranted();
            } else {
                metrics.recordDenied();
            }
            appendFeed(I18n.f("simw.feed.access",
                request.getBadgeReaderId(),
                request.getBadgeCode(),
                request.getResourceId(),
                (response != null && response.isGranted()),
                (response != null ? response.getMessage() : "")
            ) + "\n");
        });
        readerListener = event -> SwingUtilities.invokeLater(() -> {
            String ts = event.getTimestamp() != null ? event.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "";
            String msg = event.getType() == Router.ReaderEventType.MESSAGE ? event.getMessage() : event.getType().name();
            appendFeed(I18n.f("simw.feed.readerEvent", ts, event.getReaderId(), msg) + "\n");
        });
        router.addAccessEventListener(accessListener);
        router.addReaderEventListener(readerListener);
    }

    private void unregisterRouterListeners() {
        Router router = accessControlSystem.getRouter();
        if (accessListener != null) {
            router.removeAccessEventListener(accessListener);
            accessListener = null;
        }
        if (readerListener != null) {
            router.removeReaderEventListener(readerListener);
            readerListener = null;
        }
    }

    private void refreshStatsAndTime() {
        statsLabel.setText("submitted=" + metrics.getSubmitted() + " granted=" + metrics.getGranted() + " denied=" + metrics.getDenied());
        LocalDateTime now = SystemClock.now();
        String t = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (SystemClock.isUsingCustomTime()) {
            t += " custom x" + SystemClock.getTimeScale();
        }
        timeLabel.setText(t);
    }

    private void showSetTimeDialog() {
        LocalDateTime currentTime = SystemClock.now();
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), I18n.t("simw.dialog.setTime.title"), true);
        dialog.setSize(360, 240);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel(I18n.t("simw.field.year")), gbc);
        gbc.gridx = 1;
        JSpinner year = new JSpinner(new SpinnerNumberModel(currentTime.getYear(), 2000, 2100, 1));
        panel.add(year, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel(I18n.t("simw.field.month")), gbc);
        gbc.gridx = 1;
        JSpinner month = new JSpinner(new SpinnerNumberModel(currentTime.getMonthValue(), 1, 12, 1));
        panel.add(month, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel(I18n.t("simw.field.day")), gbc);
        gbc.gridx = 1;
        JSpinner day = new JSpinner(new SpinnerNumberModel(currentTime.getDayOfMonth(), 1, 31, 1));
        panel.add(day, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel(I18n.t("simw.field.hour")), gbc);
        gbc.gridx = 1;
        JSpinner hour = new JSpinner(new SpinnerNumberModel(currentTime.getHour(), 0, 23, 1));
        panel.add(hour, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel(I18n.t("simw.field.minute")), gbc);
        gbc.gridx = 1;
        JSpinner minute = new JSpinner(new SpinnerNumberModel(currentTime.getMinute(), 0, 59, 1));
        panel.add(minute, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton ok = new JButton(I18n.t("common.ok"));
        ok.addActionListener(e -> {
            LocalDateTime custom = LocalDateTime.of((Integer) year.getValue(), (Integer) month.getValue(), (Integer) day.getValue(),
                (Integer) hour.getValue(), (Integer) minute.getValue(), 0);
            SystemClock.setCustomTime(custom);
            SystemClock.setTimeScale(1.0);
            refreshStatsAndTime();
            dialog.dispose();
        });
        JButton cancel = new JButton(I18n.t("common.cancel"));
        cancel.addActionListener(e -> dialog.dispose());
        buttons.add(ok);
        buttons.add(cancel);
        panel.add(buttons, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private List<BadgeReader> loadReadersList() {
        return new ArrayList<>(accessControlSystem.getRouter().getBadgeReaders().values());
    }

    private Map<String, Resource> loadResourcesMap() {
        return accessControlSystem.getDatabaseManager().loadAllResources();
    }

    private void appendFeed(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            feedArea.append(text);
        } else {
            SwingUtilities.invokeLater(() -> feedArea.append(text));
        }
    }
}
