// Group 2 ChenGong ZhangZhao LiangYizhuo
package com.bigcomp.accesscontrol.core;

import com.bigcomp.accesscontrol.model.AccessRequest;
import com.bigcomp.accesscontrol.model.AccessResponse;
import com.bigcomp.accesscontrol.model.Badge;
import com.bigcomp.accesscontrol.model.BadgeReader;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.bigcomp.accesscontrol.util.SystemClock;

/**
 * Router class - Forwards messages between badge readers and access control system
 */
public class Router implements PropertyChangeListener {
    private AccessRequestProcessor arp; // Access Request Processor
    private Map<String, BadgeReader> badgeReaders; // Badge reader map
    private List<AccessEventListener> accessEventListeners; // Access event listener list
    private List<ReaderEventListener> readerEventListeners;

    public Router(AccessRequestProcessor arp) {
        this.arp = arp;
        this.badgeReaders = new ConcurrentHashMap<>();
        this.accessEventListeners = new CopyOnWriteArrayList<>();
        this.readerEventListeners = new CopyOnWriteArrayList<>();
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

    public void addReaderEventListener(ReaderEventListener listener) {
        readerEventListeners.add(listener);
    }

    public void removeReaderEventListener(ReaderEventListener listener) {
        readerEventListeners.remove(listener);
    }

    private void notifyReaderEvent(ReaderEvent event) {
        for (ReaderEventListener listener : readerEventListeners) {
            listener.onReaderEvent(event);
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

    public void submitAccessRequest(AccessRequest request) {
        handleAccessRequest(request);
    }

    /**
     * Handle property change event (from badge reader)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("accessRequest".equals(evt.getPropertyName())) {
            AccessRequest request = (AccessRequest) evt.getNewValue();
            handleAccessRequest(request);
        } else if ("badgeUpdated".equals(evt.getPropertyName())) {
            Badge badge = (Badge) evt.getNewValue();
            String oldCode = evt.getOldValue() instanceof String s ? s : null;
            arp.updateBadge(oldCode, badge);
        } else if ("message".equals(evt.getPropertyName())) {
            if (evt.getSource() instanceof BadgeReader reader && evt.getNewValue() instanceof String message) {
                notifyReaderEvent(new ReaderEvent(SystemClock.now(), reader.getId(), reader.getResourceId(), ReaderEventType.MESSAGE, message));
            }
        } else if ("resourceActivated".equals(evt.getPropertyName())) {
            if (evt.getSource() instanceof BadgeReader reader) {
                notifyReaderEvent(new ReaderEvent(SystemClock.now(), reader.getId(), reader.getResourceId(), ReaderEventType.RESOURCE_ACTIVATED, null));
            }
        } else if ("resourceDeactivated".equals(evt.getPropertyName())) {
            if (evt.getSource() instanceof BadgeReader reader) {
                notifyReaderEvent(new ReaderEvent(SystemClock.now(), reader.getId(), reader.getResourceId(), ReaderEventType.RESOURCE_DEACTIVATED, null));
            }
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

    public enum ReaderEventType {
        MESSAGE,
        RESOURCE_ACTIVATED,
        RESOURCE_DEACTIVATED
    }

    public static class ReaderEvent {
        private final LocalDateTime timestamp;
        private final String readerId;
        private final String resourceId;
        private final ReaderEventType type;
        private final String message;

        public ReaderEvent(LocalDateTime timestamp, String readerId, String resourceId, ReaderEventType type, String message) {
            this.timestamp = timestamp;
            this.readerId = readerId;
            this.resourceId = resourceId;
            this.type = type;
            this.message = message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getReaderId() {
            return readerId;
        }

        public String getResourceId() {
            return resourceId;
        }

        public ReaderEventType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }

    public interface ReaderEventListener {
        void onReaderEvent(ReaderEvent event);
    }

    /**
     * Get all registered badge readers
     */
    public Map<String, BadgeReader> getBadgeReaders() {
        return badgeReaders;
    }
}
