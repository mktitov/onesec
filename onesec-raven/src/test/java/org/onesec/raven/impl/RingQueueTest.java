/*
 * Copyright 2012 Mikhail Titov.
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
package org.onesec.raven.impl;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;

/**
 *
 * @author Mikhail Titov
 */
public class RingQueueTest {
    
    private AtomicInteger counter = new AtomicInteger();
    private  int counter3;
    
    @Test
    public void syncTest() {
        long cycles = 1000000000; 
        counter3 = 100;
        long startTs = System.currentTimeMillis();
        counter.set(100);
        int counter2 = 0;
        for (int i=0; i<cycles; ++i) {
            synchronized(this) {
//                ++counter;
//                counter2 += counter.get();
                counter2 += counter3;
            }
        }
        long diff = System.currentTimeMillis()-startTs;
        System.out.println("!! time: "+diff);
        System.out.println("counter2: "+counter2);
        startTs = System.currentTimeMillis();
        counter.set(100);
        counter2 = 0;
        for (int i=0; i<cycles; ++i) {
//            ++counter;
//            counter2 += counter.get();
            counter2 += counter3;
//            counter+=i%8;
        }
        diff = System.currentTimeMillis()-startTs;
        System.out.println("!! time: "+diff);
        System.out.println("counter2: "+counter2);
    }
    
}
