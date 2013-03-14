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
package org.onesec.raven.ivr.vmail.impl;

import java.util.List;
import org.onesec.raven.ivr.vmail.VMailBox;
import org.onesec.raven.ivr.vmail.VMailMessage;
import org.raven.annotations.NodeClass;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=VMailManagerNode.class)
public class VMailBoxNode extends BaseNode implements VMailBox {

    public int getNewMessagesCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<VMailMessage> getNewMessages() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getSavedMessagesCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<VMailMessage> getSavedMessages() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addMessage(VMailMessage message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isVBoxPhoneNumber(String phoneNumber) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
