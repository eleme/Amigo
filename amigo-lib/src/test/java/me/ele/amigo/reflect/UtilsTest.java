package me.ele.amigo.reflect;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void testIsSameLength() {
        Integer[] arrayA = new Integer[10];
        Integer[] arrayB = new Integer[6];
        Object[] objects = new Object[10];

        Assert.assertEquals(false, Utils.isSameLength(arrayA, arrayB));
        Assert.assertEquals(false, Utils.isSameLength(arrayB, arrayA));

        Assert.assertEquals(false, Utils.isSameLength(null, arrayA));
        Assert.assertEquals(false, Utils.isSameLength(arrayA, null));

        Assert.assertEquals(true, Utils.isSameLength(null, null));
        Assert.assertEquals(true, Utils.isSameLength(arrayA, objects));
        Assert.assertEquals(true, Utils.isSameLength(objects, arrayA));
    }

    @Test
    public void testToClass() {
        Assert.assertEquals(Object.class, Utils.toClass(new Object())[0]);
        Assert.assertNotEquals(Object.class, Utils.toClass(new Integer(10))[0]);
        Assert.assertNotEquals(new Class[] {Integer.class, Object.class},
                Utils.toClass(new Integer(10), new Object()));
    }
}
