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

/**
 *
 * @author Mikhail Titov
 */
public class NumberToDigitConverter
{
    public static Collection<String> getDigits(long number)
    {
        List<String> numbers = null;
        while (number>0)
        {
            if (numbers==null)
                numbers = new LinkedList<String>();
            List<String> tempNums = new LinkedList<String>();
            long tempNum = number-number/1000*1000;
            long sot = tempNum/100*100;
            if (sot>0)
                tempNums.add(""+sot);
            long tempNum2 = tempNum-sot;
            if (tempNum2>10 && tempNum2<20)
                tempNums.add(""+tempNum2);
            else
            {
                long des = tempNum2/10*10;
                if (des>0)
                    tempNums.add(""+des);
                long ed = tempNum2-des;
                if (ed>0)
                    tempNums.add(""+ed);
            }

            numbers.addAll(0, tempNums);

            number /= 1000;
        }

        return numbers;
    }
}
