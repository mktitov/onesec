/*
 * Copyright 2016 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onesec.raven.codec;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class ByteUtilsTest {
    
    @Test
    public void readShortTest() {
        byte[] arr = new byte[]{(byte)0xFF, (byte)0xFF};
        int val = ByteUtils.readUnsignedShort(0, arr, true);
        assertEquals(65535, val);
        byte[] srcArr = new byte[]{0,-128};
        val = ByteUtils.readUnsignedShort(0, srcArr, true);
        System.out.println("val: "+val);
        ByteUtils.writeShort(0, val, arr, true);
        assertArrayEquals(srcArr, arr);
    }
    
    @Test
    public void writeShortTest() {
        byte[] arr = new byte[2];
        ByteUtils.writeShort(0, 65535, arr, true);
        assertEquals(0xFF, arr[0]&0xFF);
        assertEquals(0xFF, arr[1]&0xFF);
    }
    
    @Test public void readSignedShort() {
        byte[] arr = new byte[]{(byte)0xFF, (byte)0xFF};
        short res = ByteUtils.readSignedShort(0, arr, true);
        assertEquals(-1, res);
    }
    
    @Test public void writeSignedShort() {
        byte[] arr = new byte[2];
        ByteUtils.writeShort(0, -1, arr, true);
        assertArrayEquals(new byte[]{(byte)0xFF, (byte)0xFF}, arr);
    }
}
