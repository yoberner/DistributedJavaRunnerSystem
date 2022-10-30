package edu.yu.cs.com3800.finalExam;

public class SystemResource extends Resource {
    /** How many units of the system resource has been allocated to users */
    private double allocated;

    public SystemResource(String name, int units) {
        super(name, units);
        this.allocated = 0;
    }

    public double getAvailable() {
        return this.getUnits() - this.allocated;
    }

    public void allocate(double amount) throws IllegalArgumentException {
        if (amount > getAvailable()) {
            throw new IllegalArgumentException("insufficient resources to allocate " + amount + " units");
        }
        this.allocated += amount;
    }

}
