/*
 * Copyright 2015 Mikhail Titov.
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

import com.logica.smpp.pdu.DeliverSM;
import org.raven.ds.DataProcessor;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public class InQueue implements DataProcessor {
    private final LoggerHelper logger;

    public InQueue(LoggerHelper logger) {
        this.logger = new LoggerHelper(logger, "Inbound queue. ");
    }
    
    public boolean processData(Object message) throws Exception {
        if (message instanceof DeliverSM)
            processDeliverSMMessage((DeliverSM) message);
        return true;
    }
    
    private void processDeliverSMMessage(DeliverSM pdu) {
        
    }
}
