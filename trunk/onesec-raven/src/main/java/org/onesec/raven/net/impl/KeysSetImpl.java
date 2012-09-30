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
package org.onesec.raven.net.impl;

import java.nio.channels.SelectionKey;
import org.onesec.raven.net.KeysSet;
import org.onesec.raven.net.PacketProcessor;

/**
 *
 * @author Mikhail Titov
 */
public class KeysSetImpl implements KeysSet {
    private final SelectionKey[] keys;
    private int pos = 0;
    private int size = 0;
    private volatile boolean waitingForProcess = false;
    private volatile boolean processing = false;

    public KeysSetImpl(int capacity) {
        keys = new SelectionKey[capacity];
    }
    
    public boolean add(SelectionKey key) {
        keys[pos++] = key;
        ((PacketProcessor)key.attachment()).changeToProcessing();
//        key.interestOps(0);
        if (pos==keys.length) {
            switchToWaitingForProcess();
            return false;
        } else
            return true;
    }

    public SelectionKey getNext() {
        if (pos==size) {
            processing = false;
            pos = 0;
            size = 0;
            return null;
        } else 
            return keys[pos++];
    }

    public boolean hasKeys() {
        return pos>0;
    }

    public boolean isFree() {
        return !waitingForProcess && !processing;
    }

    public boolean isWaitingForProcess() {
        return waitingForProcess;
    }

    public KeysSet switchToWaitingForProcess() {
        size = pos;
        pos = 0;
        waitingForProcess = true;
        return this;
    }
    
    public KeysSet switchToProcessing() {
        processing = true;
        waitingForProcess = false;
        return this;
    }
}
