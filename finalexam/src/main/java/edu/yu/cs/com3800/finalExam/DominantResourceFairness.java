package edu.yu.cs.com3800.finalExam;

import java.util.*;

/**
 * Implements a single run of the DRF algorithm.
 */
public class DominantResourceFairness {

	/**
	 * Describes the allocation of units of a single resource to a user
	 */
	public class Allocation {
		String resourceName;
		double unitsAllocated;
		User user;

		public Allocation(String resourceName, double unitsAllocated, User user) {
			this.resourceName = resourceName;
			this.unitsAllocated = unitsAllocated;
			this.user = user;
		}

		public String toString() {
			return this.unitsAllocated + " of " + this.resourceName + " allocated to " + this.user;
		}
	}

	/**
	 * a map of the resources that exist in this system. Key is the resource's name,
	 * value is the actial resource object
	 */
	private Map<String, SystemResource> systemResources;
	/**
	 * Users in the system, sorted by their dominant share. Note: to re-sort the
	 * users in the TreeSet,
	 * when a User's dominant share changes, you must remove it from the TreeSet and
	 * re-add it
	 */
	private TreeSet<User> users;

	/**
	 * @param resources
	 * @param users
	 * @throws IllegalArgumentException if either collection is empty or null
	 */
	public DominantResourceFairness(Map<String, SystemResource> systemResources, TreeSet<User> users) {
		if (systemResources == null || systemResources.isEmpty()) {
			throw new IllegalArgumentException("systemResources cannot be null or empty");
		}
		if (users == null || users.isEmpty()) {
			throw new IllegalArgumentException("users cannot be null or empty");
		}
		this.systemResources = systemResources;
		this.users = users;
	}

	/**
	 * Repeatedly allocate resources to the user with the lowest dominant share,
	 * until there are
	 * insufficient unallocated resources remaining to meet any user's requirements.
	 * 
	 * @return a list of the individual resource allocations made by DRF, in order
	 */
	public List<Allocation> allocateResources() {
		List<Allocation> allocations = new ArrayList<>();
		while (!this.users.isEmpty()) {
			User user = this.users.first();
			Map<String, Resource> userMap = user.getRequiredResourcesPerTask();
			double userRequiredCpu = userMap.get("cpu").getUnits();
			double userRequiredRam = userMap.get("ram").getUnits();
			if (userRequiredCpu <= this.systemResources.get("cpu").getAvailable() && userRequiredRam <= this.systemResources.get("ram").getAvailable()) {
				this.users.remove(user);
				this.systemResources.get("cpu").allocate(userMap.get("cpu").getUnits());
				this.systemResources.get("ram").allocate(userMap.get("ram").getUnits());
				Resource cpu = new Resource("cpu", (int) userMap.get("cpu").getUnits());
				Resource ram = new Resource("ram", (int) userMap.get("ram").getUnits());
				List<Resource> resources = new ArrayList<>();
				resources.add(cpu);
				resources.add(ram);
				user.allocateResources(resources);
				this.users.add(user);
				Allocation allocation = new Allocation(cpu.getName(), cpu.getUnits(), user);
				Allocation allocation2 = new Allocation(ram.getName(), ram.getUnits(), user);
				allocations.add(allocation);
				allocations.add(allocation2);
			}
			else {
				break;
			}
		}
		return allocations;
	}

}
