/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package tests;

import org.junit.Test;
import profiles.Profile1D;
import profiles.TimeValue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestProfiles {

    Profile1D profile1;
    double delta = 0.1d;

    public TestProfiles(){
        List<Double> values = new ArrayList<>();
        values.add(30d);
        values.add(40d);
        values.add(50d);
        profile1 = new Profile1D(50f,10f,values);

    }

    @Test
    public void testProfile1(){
        profile1.multiply(2d);
        assertEquals(3,profile1.get_length());
        assertEquals(0f,profile1.get_value_for_time(0f),delta);
        assertEquals(60f,profile1.get_value_for_time(50f),delta);
        assertEquals(60f,profile1.get_value_for_time(55f),delta);
        assertEquals(80f,profile1.get_value_for_time(60f),delta);
        assertEquals(80f,profile1.get_value_for_time(65f),delta);
        assertEquals(100f,profile1.get_value_for_time(70f),delta);
        assertEquals(100f,profile1.get_value_for_time(75f),delta);
        assertEquals(100f,profile1.get_value_for_time(1000f),delta);

        TimeValue x;
        x = profile1.get_change_following(0f);
        assertEquals(50f,x.time,delta);
        assertEquals(60d,x.value,delta);

        x = profile1.get_change_following(49f);
        assertEquals(50f,x.time,delta);
        assertEquals(60d,x.value,delta);

        x = profile1.get_change_following(50f);
        assertEquals(60f,x.time,delta);
        assertEquals(80d,x.value,delta);

        x = profile1.get_change_following(69f);
        assertEquals(70f,x.time,delta);
        assertEquals(100d,x.value,delta);

        x = profile1.get_change_following(70f);
        assertNull(x);

        x = profile1.get_change_following(71f);
        assertNull(x);
    }
}
