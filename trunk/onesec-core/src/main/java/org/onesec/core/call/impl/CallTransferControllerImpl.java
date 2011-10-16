/*
 *  Copyright 2011 Mikhail Titov.
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

package org.onesec.core.call.impl;

import javax.telephony.Call;
import org.onesec.core.call.CallTransferController;
import org.onesec.core.call.TransferState;

/**
 *
 * @author Mikhail Titov
 */
public class CallTransferControllerImpl implements CallTransferController
{
    public TransferState transfer(Call call, String address, String dtmfs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
