package com.example.helio.arduino.dso.chart;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChartControllerTest {

    @Test
    public void testMoveY() throws Exception {
        ChartController controller = new ChartController();
        assertEquals(1, controller.getYMoveStep());
    }

    @Test
    public void testMoveX() throws Exception {
        ChartController controller = new ChartController();
        assertEquals(0, controller.getXMoveStep());
    }

    @Test
    public void testScaleY() throws Exception {
        ChartController controller = new ChartController();
        assertEquals(0, controller.getYScaleStep());
    }
}