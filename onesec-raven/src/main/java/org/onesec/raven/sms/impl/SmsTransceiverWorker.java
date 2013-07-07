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
package org.onesec.raven.sms.impl;

import org.onesec.raven.sms.ISmeConfig;
import org.onesec.raven.sms.SmsConfig;
import org.onesec.raven.sms.SmsMessageEncoder;
import org.onesec.raven.sms.queue.OutQueue;
import org.onesec.raven.sms.sm.SMTextFactory;
import org.raven.sched.ExecutorService;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class SmsTransceiverWorker {
    private final SmsConfig config;
    private final ExecutorService executor;
    private final OutQueue queue;
    private final LoggerHelper logger;
    private final SmsMessageEncoder messageEncoder;

    public SmsTransceiverWorker(SmsConfig config, ExecutorService executor, LoggerHelper logger) {
        this.config = config;
        this.executor = executor;
        this.logger = new LoggerHelper(logger, "Transceiver. ");
        this.messageEncoder = new SmsMessageEncoderImpl(config, logger);
        this.queue = null;
//        this.queue = new OutQueue(new SMTextFactory(config), this.logger);
    }
}
