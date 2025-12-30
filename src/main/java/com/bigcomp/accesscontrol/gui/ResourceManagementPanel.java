// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.gui;

import com.bigcomp.accesscontrol.core.AccessControlSystem;
import com.bigcomp.accesscontrol.model.Resource;
import com.bigcomp.accesscontrol.model.BadgeReader;
import com.bigcomp.accesscontrol.profile.GroupManager;
import com.bigcomp.accesscontrol.database.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.UUID;
import java.util.Map;

/**
 * Resource Management Panel
 */
public class ResourceManagementPanel extends JPanel {
    private AccessControlSystem accessControlSystem;
    private JTable resourceTable;
    private DefaultTableModel tableModel;
    private DatabaseManager dbManager;
    private JTextField nameField;
    private JComboBox<Resource.ResourceType> typeCombo;
    private JTextField locationField;
    private JTextField buildingField;
    private JTextField floorField;

    public ResourceManagementPanel(AccessControlSystem accessControlSystem) {
        this.accessControlSystem = accessControlSystem;
        this.dbManager = accessControlSystem.getDatabaseManager();
        initializeComponents();
        setupLayout();
        loadResources();
    }

    private void initializeComponents() {
        String[] columnNames = {"ID", "Name", "Type", "Location", "Building", "Floor", "State"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only state column is editable
            }
        };
        resourceTable = new JTable(tableModel);
        resourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Input fields
        nameField = new JTextField(15);
        typeCombo = new JComboBox<>(Resource.ResourceType.values());
        locationField = new JTextField(15);
        buildingField = new JTextField(15);
        floorField = new JTextField(15);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Top: Input form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        formPanel.add(typeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        formPanel.add(locationField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Building:"), gbc);
        gbc.gridx = 1;
        formPanel.add(buildingField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Floor:"), gbc);
        gbc.gridx = 1;
        formPanel.add(floorField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton("Add Resource") {{
            addActionListener(e -> addResource());
        }});
        buttonPanel.add(new JButton("Delete Resource") {{
            addActionListener(e -> deleteResource());
        }});
        buttonPanel.add(new JButton("Create Badge Reader") {{
            addActionListener(e -> createBadgeReader());
        }});
        buttonPanel.add(new JButton("Create Badge Readers for All Resources") {{
            addActionListener(e -> createBadgeReadersForAll());
        }});
        buttonPanel.add(new JButton("Link to Resource Group") {{
            addActionListener(e -> linkToResourceGroup());
        }});
        formPanel.add(buttonPanel, gbc);
        
        // Center: Table
        JScrollPane scrollPane = new JScrollPane(resourceTable);
        
        add(formPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void addResource() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter resource name", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String resourceId = UUID.randomUUID().toString();
            Resource resource = new Resource(
                resourceId,
                name,
                (Resource.ResourceType) typeCombo.getSelectedItem(),
                locationField.getText().trim(),
                buildingField.getText().trim(),
                floorField.getText().trim()
            );
            
            dbManager.addResource(resource);
            loadResources();
            
            // Clear input fields
            nameField.setText("");
            locationField.setText("");
            buildingField.setText("");
            floorField.setText("");
            
            JOptionPane.showMessageDialog(this, "Resource added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to add resource: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteResource() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a resource to delete", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String resourceId = (String) tableModel.getValueAt(selectedRow, 0);
        String resourceName = (String) tableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete resource \"" + resourceName + "\"?\nThis operation will also delete associated badge readers and resource group associations.", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dbManager.deleteResource(resourceId);
                
                // Unregister badge reader from router
                var router = accessControlSystem.getRouter();
                Map<String, BadgeReader> readers = router.getBadgeReaders();
                BadgeReader readerToRemove = null;
                for (BadgeReader reader : readers.values()) {
                    if (reader.getResourceId().equals(resourceId)) {
                        readerToRemove = reader;
                        break;
                    }
                }
                if (readerToRemove != null) {
                    router.unregisterBadgeReader(readerToRemove.getId());
                }
                
                loadResources();
                JOptionPane.showMessageDialog(this, "Resource deleted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to delete resource: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void createBadgeReader() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a resource", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String resourceId = (String) tableModel.getValueAt(selectedRow, 0);
        Resource resource = dbManager.loadAllResources().get(resourceId);
        
        // Check if resource already has a badge reader
        if (resource != null && resource.getBadgeReaderId() != null && !resource.getBadgeReaderId().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Resource \"" + resource.getName() + "\" already has a badge reader: " + resource.getBadgeReaderId() + "\n\n" +
                "Do you want to create a new badge reader? This will replace the existing one.",
                "Confirm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        try {
            String readerId = UUID.randomUUID().toString();
            BadgeReader reader = new BadgeReader(readerId, resourceId);
            
            dbManager.addBadgeReader(reader);
            accessControlSystem.getRouter().registerBadgeReader(reader);
            
            // Update resource
            if (resource != null) {
                resource.setBadgeReaderId(readerId);
                dbManager.addResource(resource);
            }
            
            loadResources();
            JOptionPane.showMessageDialog(this, "Badge reader created successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to create badge reader: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Create badge readers for all resources
     */
    private void createBadgeReadersForAll() {
        Map<String, Resource> allResources = dbManager.loadAllResources();
        
        if (allResources.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No available resources, please add resources first", "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Count resources that need badge readers
        int totalResources = allResources.size();
        int resourcesWithReader = 0;
        int resourcesWithoutReader = 0;
        
        for (Resource resource : allResources.values()) {
            if (resource.getBadgeReaderId() != null && !resource.getBadgeReaderId().isEmpty()) {
                resourcesWithReader++;
            } else {
                resourcesWithoutReader++;
            }
        }
        
        if (resourcesWithoutReader == 0) {
            JOptionPane.showMessageDialog(this, 
                "All resources already have badge readers configured!\n\n" +
                "Total resources: " + totalResources + "\n" +
                "With badge readers: " + resourcesWithReader,
                "Info", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(this,
            "Will create badge readers for " + resourcesWithoutReader + " resources\n\n" +
            "Statistics:\n" +
            "• Total resources: " + totalResources + "\n" +
            "• Already have badge readers: " + resourcesWithReader + "\n" +
            "• Need to create: " + resourcesWithoutReader + "\n\n" +
            "Continue?",
            "Create Badge Readers for All Resources",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Get all existing badge readers to generate new IDs
        var router = accessControlSystem.getRouter();
        Map<String, BadgeReader> existingReaders = router.getBadgeReaders();
        int maxReaderIndex = 0;
        
        // Find maximum badge reader index (format: BR001, BR002, etc.)
        for (BadgeReader reader : existingReaders.values()) {
            String readerId = reader.getId();
            if (readerId.startsWith("BR") && readerId.length() == 5) {
                try {
                    int index = Integer.parseInt(readerId.substring(2));
                    maxReaderIndex = Math.max(maxReaderIndex, index);
                } catch (NumberFormatException e) {
                    // Ignore non-numeric format IDs
                }
            }
        }
        
        int createdCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        StringBuilder details = new StringBuilder();
        
        // Create badge readers for resources without readers
        for (Resource resource : allResources.values()) {
            // Skip resources that already have badge readers
            if (resource.getBadgeReaderId() != null && !resource.getBadgeReaderId().isEmpty()) {
                skippedCount++;
                continue;
            }
            
            try {
                maxReaderIndex++;
                String readerId = "BR" + String.format("%03d", maxReaderIndex);
                
                // Check if ID already exists
                while (existingReaders.containsKey(readerId)) {
                    maxReaderIndex++;
                    readerId = "BR" + String.format("%03d", maxReaderIndex);
                }
                
                BadgeReader reader = new BadgeReader(readerId, resource.getId());
                dbManager.addBadgeReader(reader);
                accessControlSystem.getRouter().registerBadgeReader(reader);
                
                // Update resource
                resource.setBadgeReaderId(readerId);
                dbManager.addResource(resource);
                
                createdCount++;
                details.append("  ✓ ").append(resource.getName())
                       .append(" -> ").append(readerId).append("\n");
                
            } catch (Exception e) {
                errorCount++;
                details.append("  ✗ ").append(resource.getName())
                       .append(" -> Failed: ").append(e.getMessage()).append("\n");
            }
        }
        
        // Refresh resource list
        loadResources();
        
        // Display results
        String message = String.format(
            "Badge reader creation completed!\n\n" +
            "Statistics:\n" +
            "• Successfully created: %d\n" +
            "• Skipped (already exists): %d\n" +
            "• Failed: %d\n\n" +
            "Details:\n%s",
            createdCount, skippedCount, errorCount, details.toString()
        );
        
        JOptionPane.showMessageDialog(this, message, 
            "Batch Badge Reader Creation Complete", 
            createdCount > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }
    
    private void linkToResourceGroup() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a resource", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String resourceId = (String) tableModel.getValueAt(selectedRow, 0);
        String resourceName = (String) tableModel.getValueAt(selectedRow, 1);
        
        // Get all available resource groups
        GroupManager groupManager = new GroupManager();
        Map<String, com.bigcomp.accesscontrol.profile.ResourceGroup> groups = groupManager.getAllGroups();
        
        if (groups.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No available resource groups.\n\nPlease create resource group JSON files in data/groups/ directory first.\n" +
                "Example: {\"name\": \"Office Area\", \"securityLevel\": 1, \"resources\": []}\n\n" +
                "After creating, please restart the program or refresh resource groups.", 
                "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String[] groupNames = groups.keySet().toArray(new String[0]);
        
        // Show selection dialog
        String selectedGroup = (String) JOptionPane.showInputDialog(this,
            "Select resource group to link resource \"" + resourceName + "\" to:",
            "Link to Resource Group",
            JOptionPane.QUESTION_MESSAGE,
            null,
            groupNames,
            groupNames.length > 0 ? groupNames[0] : null);
        
        if (selectedGroup != null) {
            try {
                // Link to database
                dbManager.linkResourceToGroup(resourceId, selectedGroup);
                
                // Update resource group file (add resource ID to JSON file)
                com.bigcomp.accesscontrol.profile.ResourceGroup group = groups.get(selectedGroup);
                if (group != null) {
                    group.addResource(resourceId);
                    groupManager.saveGroup(group);
                }
                
                // Reload in-memory data
                accessControlSystem.getAccessRequestProcessor().reloadData();
                
                JOptionPane.showMessageDialog(this, 
                    "Resource has been linked to resource group \"" + selectedGroup + "\"\n\n" +
                    "Resource ID: " + resourceId + "\n" +
                    "You can now configure access permissions for this resource group in profiles.", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to link resource group: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void loadResources() {
        tableModel.setRowCount(0);
        try {
            Map<String, Resource> resources = dbManager.loadAllResources();
            for (Resource resource : resources.values()) {
                tableModel.addRow(new Object[]{
                    resource.getId(),
                    resource.getName(),
                    resource.getType().toString(),
                    resource.getLocation(),
                    resource.getBuilding(),
                    resource.getFloor(),
                    resource.getState().toString()
                });
            }
            
            // Reload in-memory data
            accessControlSystem.getAccessRequestProcessor().reloadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load resources: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

