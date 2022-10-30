package edu.yu.cs.com3800.finalExam;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class User implements Comparable<User> {
    private double dominantShare;
    /**
     * how many units of theis user's dominant resource have been allocated to him
     */
    private double totalAmountReceivedOfDominantResource;
    private Resource dominantResource;
    private Map<String, Resource> requiredResourcesPerTask;
    private Map<String, SystemResource> totalResourcesInSystem;
    private String userID;

    /**
     * Store all given info, and determine which resource is the user's dominant
     * resource
     * 
     * @param userID
     * @param requiredResourcesPerTask the resources this user needs to do a unit of
     *                                 work
     * @param totalResourcesInSystem   all the resources that exist in the system
     * @throws IllegalArgumentException if either map is null or empty
     * @throws IllegalStateException    if the user requires resources that don't
     *                                  exist at all in the system
     */
    public User(String userID, Map<String, Resource> requiredResourcesPerTask,
            Map<String, SystemResource> totalResourcesInSystem) {
        setTotalResourcesInSystem(totalResourcesInSystem);
        setRequiredResourcesPerTask(requiredResourcesPerTask);
        if (!this.totalResourcesInSystem.keySet().containsAll(this.requiredResourcesPerTask.keySet())) {
            throw new IllegalStateException("User requires resources that don't exist in the system");
        }
        this.userID = userID;
        this.dominantShare = 0;
        determineDominantResource();
    }

    /**
     * A user’s dominant resource is the resource he needs the biggest share of.
     */
    private void determineDominantResource() {
        double maxShare = 0;
        this.dominantResource = null;
        for (Resource required : this.requiredResourcesPerTask.values()) {
            Resource system = this.totalResourcesInSystem.get(required.getName());
            if ((required.getUnits() / system.getUnits()) > maxShare) {
                this.dominantResource = system;
                maxShare = required.getUnits() / system.getUnits();
            }
        }
    }

    /**
     * Allocate the given resources to this user.
     * Update the user's dominant share.
     * 
     * @param allocation
     * @throws IllegalArgumentException if allocation does not include a sufficient
     *                                  amount of all the resources this user
     *                                  requires to do a unit of work
     */
    public void allocateResources(Collection<Resource> allocation) {
        if (!allocation.containsAll(this.requiredResourcesPerTask.values())) {
            throw new IllegalArgumentException(
                    "The given allocation does not include all the resources required by this user");
        }
        Resource dominant = null;
        for (Resource allocated : allocation) {
            // check that the allocation gives us enough of each resource
            if (allocated.getUnits() < this.requiredResourcesPerTask.get(allocated.getName()).getUnits()) {
                throw new IllegalArgumentException("Insufficient allocation of " + allocated.getName());
            }
            // hold on to the allocation of our dominant resource
            if (allocated.equals(this.dominantResource)) {
                dominant = allocated;
            }
        }
        updateDominantShare(dominant.getUnits());
    }

    /**
     * update my dominant share since I was allocated the given additional amount of
     * my dominant resource
     * 
     * @param additionalAllocation
     */
    private void updateDominantShare(double additionalAllocation) {
        this.totalAmountReceivedOfDominantResource += additionalAllocation;
        this.dominantShare = this.totalAmountReceivedOfDominantResource / this.dominantResource.getUnits();
    }

    /**
     * A user's dominant share is the fraction of the total existing amount of his
     * dominant resource he has already been allocated
     * 
     * @return this user's dominant share
     */
    public double getDominantShare() {
        return this.dominantShare;
    }

    private void setRequiredResourcesPerTask(Map<String, Resource> requiredResourcesPerTask) {
        if (requiredResourcesPerTask == null || requiredResourcesPerTask.isEmpty()) {
            throw new IllegalArgumentException("requiredResources may not be null or empty");
        }
        this.requiredResourcesPerTask = Collections.unmodifiableMap(requiredResourcesPerTask);
    }

    public Map<String, Resource> getRequiredResourcesPerTask() {
        return this.requiredResourcesPerTask;
    }

    private void setTotalResourcesInSystem(Map<String, SystemResource> totalResourcesInSystem) {
        if (totalResourcesInSystem == null || totalResourcesInSystem.isEmpty()) {
            throw new IllegalArgumentException("allResources may not be null or empty");
        }
        this.totalResourcesInSystem = Collections.unmodifiableMap(totalResourcesInSystem);
    }

    @Override
    public int compareTo(User other) {
        if (this.dominantShare < other.dominantShare) {
            return -1;
        } else if (this.equals(other)) {
            return 0;
        }
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User user = (User) o;
        return userID.equals(user.userID);
    }

    @Override
    public int hashCode() {
        return userID.hashCode();
    }

    @Override
    public String toString() {
        return this.userID;
    }

}