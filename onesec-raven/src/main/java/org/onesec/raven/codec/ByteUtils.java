/*
 * Copyright 2016 Mihail Titov.
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

/**
 *
 * @author Mikhail Titov
 */
public class ByteUtils {
    private ByteUtils() {}
    
    public static int readUnsignedAsSigned(int index, byte[] array, boolean littleEndian) {
        return readUnsignedShort(index, array, littleEndian) - 32768;
    }
    
    public static int readUnsignedShort(int index, byte[] array, boolean littleEndian) {
        int hiInd = littleEndian? index+1 : index;
        int lowInd = littleEndian? index : index+1;
        return ((array[hiInd]&0xFF) << 8) | (array[lowInd] & 0xFF);
    }
    
    public static short readSignedShort(int index, byte[] array, boolean littleEndian) {
        return (short)readUnsignedShort(index, array, littleEndian);
    }
    
    public static void writeSignedShortAsUnsigned(int index, int value, byte[] array, boolean littleEndian) {
        writeShort(index, value+32768, array, littleEndian);
    }
    
    public static void writeShort(int index, int value, byte[] array, boolean littleEndian) {
        int hiInd = littleEndian? index+1 : index;
        int lowInd = littleEndian? index : index+1;
        array[hiInd]  = (byte) (value >> 8);
        array[lowInd] = (byte) value;
    }        
}
