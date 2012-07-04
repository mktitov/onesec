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
package org.onesec.raven.ivr.queue.impl;

import org.onesec.raven.ivr.queue.OperatorDesc;

/**
 *
 * @author Mikhail Titov
 */
public class OperatorDescImpl implements OperatorDesc {
    private String operatorDesc;
    private final String operatorId;

    public OperatorDescImpl(String operatorId) {
        this.operatorId = operatorId;
    }

    public OperatorDescImpl(String operatorId, String operatorDesc) {
        this(operatorId);
        this.operatorDesc = operatorDesc;
    }

    public String getDesc() {
        return operatorDesc;
    }

    public void setDesc(String operatorDesc) {
        this.operatorDesc = operatorDesc;
    }

    public String getId() {
        return operatorId;
    }

    @Override
    public String toString() {
        return "Operator: id - ("+operatorId+"); desc - ("+operatorDesc+")";
    }
}
