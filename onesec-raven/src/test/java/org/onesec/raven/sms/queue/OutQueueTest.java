/*
 * Copyright 2013 Mikhail Titov.
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
package org.onesec.raven.sms.queue;

import org.junit.Before;
import org.junit.Test;
import org.onesec.raven.ivr.impl.ContainerParserDataSource;
import org.raven.log.LogLevel;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class OutQueueTest {
    private final static Logger logger = LoggerFactory.getLogger(OutQueueTest.class);
    private static LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "SMS. ", null, logger);

    @Before
    public void prepare() {
        
    }
    
    @Test
    public void queueIsFullTest() {
        
    }
}