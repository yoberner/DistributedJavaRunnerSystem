package edu.yu.cs.com3800.finalExam;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;

public class DRFTest {

	@Test
	public void test() {
		String CPU = "cpu";
        String RAM = "ram";

        // create system resources
        HashMap<String, SystemResource> systemResources = new HashMap<>(2);
        SystemResource systemCPU = new SystemResource(CPU, 9);
        systemResources.put(CPU, systemCPU);
        SystemResource systemRAM = new SystemResource(RAM, 18);
        systemResources.put(RAM, systemRAM);

        TreeSet<User> users = new TreeSet<User>();
        // create user 1
        HashMap<String, Resource> userResources = new HashMap<>(2);
        userResources.put(CPU, new Resource(CPU, 1));
        userResources.put(RAM, new Resource(RAM, 4));
        users.add(new User("User A", userResources, systemResources));
        // create user 2
        userResources = new HashMap<>(2);
        userResources.put(CPU, new Resource(CPU, 3));
        userResources.put(RAM, new Resource(RAM, 1));
        users.add(new User("User B", userResources, systemResources));

        // run DRF
        DominantResourceFairness drf = new DominantResourceFairness(systemResources, users);
        List<DominantResourceFairness.Allocation> allocations = drf.allocateResources();

        // check results
        String results = "";
        for (DominantResourceFairness.Allocation allocation : allocations) {
            System.out.println(allocation);
            results += allocation + "\n";
        }
        String expected = "1.0 of cpu allocated to User A\n4.0 of ram allocated to User A\n3.0 of cpu allocated to User B\n1.0 of ram allocated to User B\n1.0 of cpu allocated to User A\n4.0 of ram allocated to User A\n3.0 of cpu allocated to User B\n1.0 of ram allocated to User B\n1.0 of cpu allocated to User A\n4.0 of ram allocated to User A\n";
        assert results.equals(expected);
        double allocatedCPU = systemCPU.getUnits() - systemCPU.getAvailable();
        double allocatedRAM = systemRAM.getUnits() - systemRAM.getAvailable();
        assert allocatedRAM == 14;
        assert allocatedCPU == 9;
		assertEquals(expected, results);
	}
	
}
