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
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import static org.onesec.raven.impl.NumberToDigitConverter.*;

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

        double d = 195912.12;
        long l = (long) d;
        assertEquals(195912l, l);
        assertEquals(12l, (long)(Math.round((d-l)*100)));
    }
    
    @Test 
    public void getDigitGroupsTest1() {
        List<List<String>> groups = NumberToDigitConverter.getDigitGroups(121, new LinkedList<List<String>>());
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals(3, groups.get(0).size());
        assertEquals("100", groups.get(0).get(0));
        assertEquals("20", groups.get(0).get(1));
        assertEquals("1", groups.get(0).get(2));
    }

    @Test 
    public void getDigitGroupsTest2() {
        List<List<String>> groups = NumberToDigitConverter.getDigitGroups(119, new LinkedList<List<String>>());
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).size());
        assertEquals("100", groups.get(0).get(0));
        assertEquals("19", groups.get(0).get(1));
    }

    @Test 
    public void getDigitGroupsTest3() {
        List<List<String>> groups = NumberToDigitConverter.getDigitGroups(20119, new LinkedList<List<String>>());
        assertNotNull(groups);
        assertEquals(2, groups.size());
        assertEquals(1, groups.get(1).size());
        assertEquals("20", groups.get(1).get(0));
        assertEquals(2, groups.get(0).size());
        assertEquals("100", groups.get(0).get(0));
        assertEquals("19", groups.get(0).get(1));
    }
    
    @Test
    public void getDigitsWithGenus_1() {
        assertArrayEquals(new Object[]{"1"}, getDigits(1, Genus.MALE).toArray());
        assertArrayEquals(new Object[]{"1'"}, getDigits(1, Genus.FEMALE).toArray());
        assertArrayEquals(new Object[]{"1''"}, getDigits(1, Genus.NEUTER).toArray());
    }

    @Test
    public void getDigitsWithGenus_2() {
        assertArrayEquals(new Object[]{"2"}, getDigits(2, Genus.MALE).toArray());
        assertArrayEquals(new Object[]{"2'"}, getDigits(2, Genus.FEMALE).toArray());
        assertArrayEquals(new Object[]{"2"}, getDigits(2, Genus.NEUTER).toArray());
    }

    @Test
    public void getDigitsTest()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(921, Genus.MALE);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertArrayEquals(new Object[]{"900", "20", "1"}, list.toArray());
    }

    @Test
    public void getDigits_ZeroTest() {
        Collection<String> list = NumberToDigitConverter.getDigits(0, Genus.MALE);
        assertTrue(list.isEmpty());
        list = NumberToDigitConverter.getDigits(0, Genus.MALE, true);
        assertArrayEquals(new Object[]{"0"}, list.toArray());
    }

    @Test
    public void getDigits_OneTest()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(1, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"1"}, list.toArray());
    }

    @Test
    public void getDigits_11Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(11, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"11"}, list.toArray());
    }

    @Test
    public void getDigits_19Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(19, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"19"}, list.toArray());
    }

    @Test
    public void getDigits_119Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(119, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"100", "19"}, list.toArray());
    }

    @Test
    public void getDigits_20Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(20, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"20"}, list.toArray());
    }

    @Test
    public void getDigits_90Test()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(90, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"90"}, list.toArray());
    }

    @Test
    public void getDigits_1000()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(1000, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"1'", "тыс€ча"}, list.toArray());
    }

    @Test
    public void getDigits_22000()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(22000, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"20", "2'", "тыс€чи"}, list.toArray());
    }

    @Test
    public void getDigits_33000()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(33000, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"30", "3", "тыс€чи"}, list.toArray());
    }

    @Test
    public void getDigits_44000()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(44000, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"40", "4", "тыс€чи"}, list.toArray());
    }

    @Test
    public void getDigits_55000()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(55000, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"50", "5", "тыс€ч"}, list.toArray());
    }

    @Test
    public void getDigits_1001()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(1001, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"1'", "тыс€ча", "1"}, list.toArray());
    }

    @Test
    public void getDigits_1011()
    {
        Collection<String> list = NumberToDigitConverter.getDigits(1011, Genus.MALE);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"1'", "тыс€ча", "11"}, list.toArray());
    }

    @Test
    public void getCurrencyDigits_1()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(1.);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"1", "рубль"}, list.toArray());
    }

    @Test
    public void getCurrencyDigits_2()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(2.);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"2", "рубл€"}, list.toArray());
    }

    @Test
    public void getCurrencyDigits_3()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(3.);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"3", "рубл€"}, list.toArray());
    }
    
    @Test
    public void getCurrencyDigits_4()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(4.);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"4", "рубл€"}, list.toArray());
    }
    
    @Test
    public void getCurrencyDigits_5()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(5.);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"5", "рублей"}, list.toArray());
    }

    @Test
    public void getCurrencyDigits_1000()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(1000.);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"1'", "тыс€ча", "рублей"}, list.toArray());
    }

    @Test
    public void getCurrencyDigits_0_1()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(.01);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"1'", "копейка"}, list.toArray());
    }

    @Test
    public void getCurrencyDigits_0_22()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(.22);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"20", "2'", "копейки"}, list.toArray());
    }
    
    @Test
    public void getCurrencyDigits_0_33()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(.33);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"30", "3", "копейки"}, list.toArray());
    }
    
    @Test
    public void getCurrencyDigits_0_44()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(.44);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"40", "4", "копейки"}, list.toArray());
    }
    
    @Test
    public void getCurrencyDigits_0_55()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(.55);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"50", "5", "копеек"}, list.toArray());
    }

    @Test
    public void getCurrencyDigits_0_19()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(.19);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"19", "копеек"}, list.toArray());
    }

    @Test
    public void getCurrencyDigits_101023_19()
    {
        Collection<String> list = NumberToDigitConverter.getCurrencyDigits(101023.19);
        assertNotNull(list);
        assertArrayEquals(new Object[]{"100", "1'", "тыс€ча","20", "3", "рубл€", "19", "копеек"}, list.toArray());
    }
}