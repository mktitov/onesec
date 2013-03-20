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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import static org.weda.beans.ObjectUtils.*;

/**
 *
 * @author Mikhail Titov
 */
public class NumberToDigitConverter {
    private static String[][] ORDER_NAMES = {
        {},
        {"тыс€ча", "тыс€чи", "тыс€ч"},
        {"миллион", "миллиона", "миллионов"},
        {"миллиард", "миллиарда", "миллиардов"}
    };
    private static Genus[] ORDER_GENUS = {null, Genus.FEMALE, Genus.MALE, Genus.MALE};
    
//    public static List<String> getDigits(long number) {
//        List<String> numbers = null;
//        int sotNumber = 1;
//        while (number>0) {
//            if (numbers==null)
//                numbers = new LinkedList<String>();
//            List<String> tempNums = new LinkedList<String>();
//            long tempNum = number-number/1000*1000;
//            long sot = tempNum/100*100;
//            if (sot>0)
//                tempNums.add(""+sot);
//            long tempNum2 = tempNum-sot;
//            int ed=0;
//            if (tempNum2>10 && tempNum2<20)
//                tempNums.add(""+tempNum2);
//            else {
//                long des = tempNum2/10*10;
//                if (des>0)
//                    tempNums.add(""+des);
//                ed = (int) (tempNum2 - des);
//                if (ed>0)
//                    tempNums.add(""+ed+(sotNumber==2 && ed<=2? "'" : ""));
//            }
//            if (sotNumber==2) {
//                String t = null;
//                switch(ed) {
//                    case 1: t = "тыс€ча"; break;//миллион
//                    case 2:                     
//                    case 3:
//                    case 4: t = "тыс€чи"; break;//миллиона
//                    default: t = "тыс€ч";
//                }
//                tempNums.add(t);
//            }
//            numbers.addAll(0, tempNums);
//            number /= 1000;
//            ++sotNumber;
//        }
//
//        return numbers;
//    }

    public static List<String> getCurrencyDigits(double amount) {
        long rub = (long) amount;
        long kop = (long) (Math.round((amount - rub) * 100));

        List<String> res = null;

        List<String> rubDigits = getDigits(rub, Genus.MALE);
        if (rubDigits!=null && !rubDigits.isEmpty()) {
            String rubString = "рублей";
            String lastElem = rubDigits.get(rubDigits.size()-1);
            if ("1".equals(lastElem))
                rubString = "рубль";
            else if (in(lastElem, "2", "3", "4"))
                rubString = "рубл€";
            rubDigits.add(rubString);
            res = rubDigits;
        }

        List<String> kopDigits = getDigits(kop, Genus.MALE);
        if (kopDigits!=null && !kopDigits.isEmpty()) {
            String kopString = "копеек";
            int lastIndex = kopDigits.size()-1;
            String lastElem = kopDigits.get(lastIndex);
            if ("1".equals(lastElem))
                kopString = "копейка";
            else if (in(lastElem, "2", "3", "4"))
                kopString = "копейки";
            if (in(lastElem, "1", "2"))
                kopDigits.set(lastIndex, lastElem+"'");
            kopDigits.add(kopString);
            if (res==null)
                res = kopDigits;
            else
                res.addAll(kopDigits);
        }

        return res;
    }
    
    public static List<String> getDigits(long number, Genus genus) {
        List<List<String>> groups = getDigitGroups(number, new LinkedList<List<String>>());
        LinkedList<String> digits = new LinkedList<String>();
        Iterator<List<String>> it = groups.iterator();
        for (int i=0; i<ORDER_NAMES.length && it.hasNext(); ++i) {
            List<String> nums = it.next();
            if (i>0 && !nums.isEmpty()) {
                int num = Integer.parseInt(nums.get(nums.size()-1));
                modifyGenus(nums, ORDER_GENUS[i]);
                if (num==1) digits.add(0, ORDER_NAMES[i][0]);
                else if (num<5) digits.add(0, ORDER_NAMES[i][1]);
                else digits.add(0, ORDER_NAMES[i][2]);
            }
            if (i==0 && !nums.isEmpty()) modifyGenus(nums, genus);
            digits.addAll(0, nums);
        }
        return digits.isEmpty()? Collections.EMPTY_LIST : digits;
    }
    
    public static List<List<String>> getDigitGroups(final long number, final List<List<String>> groups) {
        if (number==0) return groups;
        List<String> group = new ArrayList<String>(3);
        groups.add(group);
        long num = number - number/1000*1000;
        long sot = addDigitToGroup(num/100*100, group);
        num -= sot;
        if (num<=20) addDigitToGroup(num, group);
        else {
            long des = addDigitToGroup(num/10*10, group);
            addDigitToGroup(num-des, group);
        }
        return getDigitGroups(number/1000, groups);
    }
    
    private static long addDigitToGroup(long num, List<String> group) {
        if (num>0) group.add(Long.toString(num));
        return num;
    }
    
    private static void modifyGenus(List<String> nums, Genus genus) {
        String lastNum = nums.get(nums.size()-1);
        if ("1".equals(lastNum)) {
            if (genus==Genus.FEMALE) nums.set(nums.size()-1, lastNum+"'");
            else if (genus==Genus.NEUTER) nums.set(nums.size()-1, lastNum+"''");
        } else if ("2".equals(lastNum) && genus==Genus.FEMALE) nums.set(nums.size()-1, lastNum+"'");
    }
}
