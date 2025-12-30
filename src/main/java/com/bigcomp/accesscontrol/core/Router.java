// Group 2 ChenGong ZhangZhao LiangYiKuo
package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.BadgeReader;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Router class - Forwards messages between badge readers and access control system
 */
public class Router implements PropertyChangeListener {
    private AccessRequestProcessor arp; // Access Request Processor
    private Map<String, BadgeReader> badgeReaders; // Badge reader map
    private List<AccessEventListener> accessEventListeners; // Access event listener list

    public Router(AccessRequestProcessor arp) {
        this.arp = arp;
        this.badgeReaders = new ConcurrentHashMap<>();
        this.accessEventListeners = new ArrayList<>();
    }
    
    /**
     * Add access event listener
     */
    public void addAccessEventListener(AccessEventListener listener) {
        accessEventListeners.add(listener);
    }
    
    /**
     * Remove access event listener
     */
    public void removeAccessEventListener(AccessEventListener listener) {
        accessEventListeners.remove(listener);
    }
    
    /**
     * Notify all listeners of access event
     */
    private void notifyAccessEvent(AccessRequest request, AccessResponse response) {
        for (AccessEventListener listener : accessEventListeners) {
            listener.onAccessEvent(request, response);
        }
    }

    /**
     * Register badge reader
     */
    public void registerBadgeReader(BadgeReader reader) {
        badgeReaders.put(reader.getId(), reader);
        reader.addPropertyChangeListener(this);
    }

    /**
     * Unregister badge reader
     */
    public void unregisterBadgeReader(String readerId) {
        BadgeReader reader = badgeReaders.remove(readerId);
        if (reader != null) {
            reader.removePropertyChangeListener(this);
        }
    }

    /**
     * Handle property change event (from badge reader)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("accessRequest".equals(evt.getPropertyName())) {
            AccessRequest request = (AccessRequest) evt.getNewValue();
            handleAccessRequest(request);
        }
    }

    /**
     * Handle access request
     */
    private void handleAccessRequest(AccessRequest request) {
        // Forward to access request processor
        AccessResponse response = arp.processRequest(request);
        
        // Notify listeners
        notifyAccessEvent(request, response);
        
        // Forward response back to corresponding badge reader
        BadgeReader reader = badgeReaders.get(request.getBadgeReaderId());
        if (reader != null) {
            reader.handleAccessResponse(response);
        }
    }
    
    /**
     * Access event listener interface
     */
    public interface AccessEventListener {
        void onAccessEvent(AccessRequest request, AccessResponse response);
    }

    /**
     * Get all registered badge readers
     */
    public Map<String, BadgeReader> getBadgeReaders() {
        return badgeReaders;
    }
}

