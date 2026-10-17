package com.nowshowing.tv.ui.player

import org.junit.Assert.*
import org.junit.Test

class PointerMotionTest {
    @Test fun tapsMoveLessThanSustainedHolds() {
        assertTrue(PointerMotion.stepDp(0) < PointerMotion.stepDp(8))
        assertTrue(PointerMotion.stepDp(8) < PointerMotion.stepDp(20))
    }
    @Test fun longHoldsStayBounded() {
        assertEquals(PointerMotion.stepDp(20), PointerMotion.stepDp(10000))
    }
    @Test fun stepsNeverDecreaseAsHoldContinues() {
        val steps = (0..100).map(PointerMotion::stepDp)
        assertTrue(steps.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(steps.all { it > 0f && it <= 28f })
    }
}
