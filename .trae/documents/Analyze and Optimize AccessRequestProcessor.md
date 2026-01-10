# Code Analysis & Optimization Plan: AccessRequestProcessor

## 1. Detailed Analysis of Current Code
The `AccessRequestProcessor.java` is the core engine of the access control system. It is designed to process access requests efficiently in memory without querying the database for every request.

### Key Components:
- **In-Memory Caching**: It loads users, badges, resources, and profiles into `HashMap` structures during initialization.
- **Validation Logic**:
    1.  **Badge Check**: Verifies if the badge exists, is valid, and not expired.
    2.  **User Check**: Finds the user associated with the badge.
    3.  **Resource Check**: Verifies resource existence and checks for `UNCONTROLLED` state (Emergency/Open access).
    4.  **Permission Check**: Iterates through user's profiles to see if any profile grants access to the resource's group.
    5.  **Time Filter**: Checks if the current time matches the profile's allowed time ranges.

### Current Logic Flow:
```java
processRequest(request)
  -> Check Badge (Valid/Expired?)
  -> Find User
  -> Check Resource (Uncontrolled?)
  -> Get User Profiles
  -> For each profile:
       -> Check Resource Group match
       -> Check Time Filter
  -> Return AccessResponse
```

## 2. Identified Optimization Opportunities

### A. Concurrency & Thread Safety (Critical)
**Current Issue**: The `reloadData()` method calls `loadDataIntoMemory()`, which updates the maps (`usersByBadgeCode`, `badgesByCode`, etc.) one by one.
- **Risk**: If an access request comes in *while* data is reloading, the processor might be in an inconsistent state (e.g., `usersByBadgeCode` is updated but `userProfiles` is old). This can lead to incorrect access denials or runtime errors.

### B. Incremental Updates
**Current Issue**: The system only supports full reload (`reloadData`).
- **Impact**: If a single badge is updated (e.g., user swipes to update), reloading the entire database is inefficient.
- **Optimization**: Add support for updating individual badges or resources in the cache.

### C. Denial Reason Logic
**Current Issue**: The `buildDenyReason` logic is complex and embedded.
- **Optimization**: Isolate this logic for better readability and maintainability.

## 3. Implementation Plan

I propose refactoring `AccessRequestProcessor` to use the **Snapshot Pattern** for thread safety.

### Step 1: Create `AccessControlContext`
Encapsulate all data maps into an immutable (or effectively immutable) inner class `AccessControlContext`.

```java
private static class AccessControlContext {
    final Map<String, User> usersByBadgeCode;
    final Map<String, Badge> badgesByCode;
    // ... other maps
}
```

### Step 2: Atomic Reference
Store the context in a `volatile` field.
```java
private volatile AccessControlContext context;
```

### Step 3: Atomic Reload
Rewrite `reloadData()` to build a *new* `AccessControlContext` completely in the background, then swap the `context` reference in one atomic operation.
```java
public void reloadData() {
    AccessControlContext newContext = loadContextFromDb();
    this.context = newContext; // Atomic swap
}
```

### Step 4: Add Incremental Update Support
Add a method `updateBadgeCache(Badge badge)` to allow `EventSimulator` or `BadgeReader` to update the cache immediately without a full reload.

## 4. Expected Outcome
- **100% Thread Safe**: No race conditions during data updates.
- **High Performance**: Reads are lock-free (using `volatile` read).
- **Better Maintainability**: Clean separation of data state and processing logic.
