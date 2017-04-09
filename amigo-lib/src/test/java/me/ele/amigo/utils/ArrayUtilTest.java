package me.ele.amigo.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArrayUtilTest {

    Object[] nullArray = null;
    Object[] emptyArray = new Object[0];
    Object[] noneEmptyArray = new Object[10];

    @Test
    public void testIsEmpty(){
        assertEquals(false, ArrayUtil.isEmpty(noneEmptyArray));
        assertEquals(true, ArrayUtil.isEmpty(nullArray));
        assertEquals(true, ArrayUtil.isEmpty(emptyArray));
    }

    @Test
    public void testIsNotEmpty(){
        assertEquals(false, ArrayUtil.isNotEmpty(nullArray));
        assertEquals(false, ArrayUtil.isNotEmpty(emptyArray));
        assertEquals(true, ArrayUtil.isNotEmpty(noneEmptyArray));
    }

    @Test
    public void testLength(){
        assertEquals(0, ArrayUtil.length(nullArray));
        assertEquals(0, ArrayUtil.length(emptyArray));
        assertEquals(10, ArrayUtil.length(noneEmptyArray));
    }
}
