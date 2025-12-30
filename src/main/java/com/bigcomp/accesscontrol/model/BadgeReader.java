// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.model;

import com.bigcomp.accesscontrol.util.SystemClock;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Badge Reader class - Simulates physical badge reader
 * Can read badge information and control resources
 */
public class BadgeReader {
    private String id; // Badge reader unique identifier
    private String resourceId; // Associated resource ID
    private boolean active; // Whether badge reader is active
    private PropertyChangeSupport pcs; // For event notification

    public BadgeReader(String id, String resourceId) {
        this.id = id;
        this.resourceId = resourceId;
        this.active = true;
        this.pcs = new PropertyChangeSupport(this);
    }

    /**
     * Swipe badge operation
     * @param badge Badge to swipe
     * @return Access request object
     */
    public AccessRequest swipeBadge(Badge badge) {
        if (!active) {
            return null; // Badge reader not active
        }

        // Read badge code
        String badgeCode = badge.getCode();
        
        // Create access request
        AccessRequest request = new AccessRequest(
            badgeCode,
            this.id,
            this.resourceId,
            SystemClock.now()
        );

        // Notify router
        pcs.firePropertyChange("accessRequest", null, request);

        return request;
    }

    /**
     * Update badge code (bring badge near reader without swiping)
     * @param badge Badge to update
     * @return Whether update succeeded
     */
    public boolean updateBadge(Badge badge) {
        if (!active) {
            return false;
        }

        if (badge.needsUpdate()) {
            badge.updateCode();
            return true;
        }
        return false;
    }

    /**
     * Handle access response
     * @param response Access response
     */
    public void handleAccessResponse(AccessResponse response) {
        this.active = false; // Temporarily disable badge reader

        if (response.isGranted()) {
            // Access granted, activate resource
            activateResource();
        } else {
            // Access denied, display denial message
            displayMessage("Access denied: " + response.getMessage());
        }

        // Reactivate badge reader after a few seconds
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000); // Reactivate after 3 seconds
                this.active = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Activate resource (e.g., open door)
     */
    private void activateResource() {
        displayMessage("Access granted");
        // Notify resource is activated
        pcs.firePropertyChange("resourceActivated", null, resourceId);
        
        // Simulate resource operation time (e.g., door opens and closes)
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // Resource recovers after 5 seconds
                pcs.firePropertyChange("resourceDeactivated", null, resourceId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Display message (simulate badge reader display)
     */
    private void displayMessage(String message) {
        System.out.println("Badge Reader " + id + ": " + message);
        pcs.firePropertyChange("message", null, message);
    }

    /**
     * Add property change listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

