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
package org.onesec.raven.ivr;

import java.util.Collection;
import java.util.List;
import org.raven.sched.ExecutorServiceException;

/**
 *
 * @author Mikhail Titov
 */
public interface IvrActionExecutor {
    public static final int CANCEL_TIMEOUT = 5000;
    
    public void executeActions(Collection<IvrAction> actions) throws ExecutorServiceException, InterruptedException;
    public boolean hasDtmfProcessPoint(char dtmf);
    public List<Character> getCollectedDtmfs();
    public void cancelActionsExecution() throws InterruptedException;
}
