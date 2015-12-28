/*
 *  Copyright 2010 Mikhail Titov.
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

package org.onesec.raven.ivr.actions;

/**
 *
 * @author Mikhail Titov
 */
public class DtmfProcessPointAction extends ContinueConversationAction
{
    private final String dtmfs;

    public DtmfProcessPointAction(String dtmfs) {
        super("DTMF Processing point");
        this.dtmfs = dtmfs;
    }

    public String getDtmfs() {
        return dtmfs;
    }
}
