/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.onesec.raven.impl;

import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class NumberToDigitConverterTest extends Assert
{
    @Test
    public void devisionTest()
    {
        int n = 1234-1234/1000*1000;
        assertEquals(234, n);
        n/=100;
        assertEquals(2, n);
    }

    @Test
    public void getDigitsTest()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(921);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertArrayEquals(new Object[]{"900", "20", "1"}, list.toArray());
    }

    @Test
    public void getDigits_ZeroTest()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(0);
        assertNull(list);
    }

    @Test
    public void getDigits_OneTest()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(1);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"1"}, list.toArray());
    }

    @Test
    public void getDigits_11Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(11);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"11"}, list.toArray());
    }

    @Test
    public void getDigits_19Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(19);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"19"}, list.toArray());
    }

    @Test
    public void getDigits_119Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(119);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"100", "19"}, list.toArray());
    }

    @Test
    public void getDigits_20Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(20);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"20"}, list.toArray());
    }

    @Test
    public void getDigits_90Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(90);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"90"}, list.toArray());
    }
}