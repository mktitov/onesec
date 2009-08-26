/*
 *  Copyright 2007 Mikhail Titov.
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

package org.onesec.core.call;

import org.onesec.core.State;

/**
 *
 * @author Mikhail Titov
 */
public interface CallState extends State<CallState, CallController> {
    /**
     * Идет подготовка к вызову
     */
    public final static int PREPARING = 1;
    /**
     * Готов к выполнению вызова
     */
    public final static int PREPARED = 2;
    /**
     * Идет вызов
     */
    public final static int CALLING = 3;
    /**
     * Разговор
     */
    public final static int TALKING = 4;
    /**
     * Разговор завершен.
     */
    public final static int FINISHED = 5;

}
