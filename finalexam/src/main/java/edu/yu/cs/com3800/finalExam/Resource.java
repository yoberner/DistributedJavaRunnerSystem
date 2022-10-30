package edu.yu.cs.com3800.finalExam;

/**
 * Represents an allocatable resource. Can be used both to represent
 * a requirement of a user or an grant of a resource to a user.
 */
public class Resource {
    /** name of the resource, e.g. "CPU" */
    private String name;
    /** Total units of this resource that are required of that have been granted */
    private double units;

    public Resource(String name, int units) {
        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }
        this.name = name;
        this.units = units;
    }

    public String getName() {
        return this.name;
    }

    public double getUnits() {
        return this.units;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resource)) {
            return false;
        }
        Resource resource = (Resource) o;
        return name.equals(resource.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}