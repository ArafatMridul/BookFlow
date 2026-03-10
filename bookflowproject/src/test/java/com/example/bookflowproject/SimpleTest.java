package com.example.bookflowproject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleTest {

    @Test
    public void testAlwaysPasses() {
        assertTrue(true, "This test should always pass");
        System.out.println("✅ Simple test ran successfully!");
    }

    @Test
    public void testMathOperations() {
        int result = 2 + 2;
        assertTrue(result == 4, "2 + 2 should equal 4");
        System.out.println("✅ Math test passed!");
    }
}