package me.ele.amigo;

import org.junit.Assert;
import org.junit.Test;

public class LoadPatchErrorTest {

    @Test
    public void testRecord() {
        Exception err = new RuntimeException("mock exception");
        LoadPatchError recordedError = LoadPatchError.record(LoadPatchError.LOAD_ERR, err);
        Assert.assertEquals(err, recordedError.getException());
        Assert.assertEquals(LoadPatchError.LOAD_ERR, recordedError.getType());
    }
}
