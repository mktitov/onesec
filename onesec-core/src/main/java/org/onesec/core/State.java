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

package org.onesec.core;

/**
 *
 * @author Mikhail Titov
 */
public interface State<T extends State, O extends ObjectDescription> {
    /**
     * Возвращает объект за состоянием которого ведется наблюдение
     * @return
     */
    public O getObservableObject();
    /**
     * Возвращает целочисленное значение состояние 
     * @return
     */
    public int getId();
    /**
     * Возвращает имя целочисленного значение состояние 
     * @return
     */
    public String getIdName();
    /**
     * Текущее состояние счетчика состояния. По сути возвращает количество установок состояний.
     * @return
     */
    public long getCounter();
    /**
     * Вернет <b>true</b> если в объекте, состояние которого отражается данным объектом, произошла
     * какая то ошибка.
     * @return
     * @see #getErrorException()
     * @see #getErrorMessage()
     */
    public boolean hasError();
    /**
     * Вернет исключительную ситуацию которая возникла в контролируем объекте.
     * @return
     */
    public Throwable getErrorException();
    /**
     * Вернет сообщение об ошибке, произошедшей в контролируем объекте.
     * @return
     */
    public String getErrorMessage();
    /**
     * Ожидает одно из состояний переданных в параметре <code>states</code>.
     * 
     * @param states список состояний одно из которых нужно дождаться
     * @param timeout время через которое нужно прекратить ожидание 
     * @return Информацию о том как закончилось ожидание и если ожидание закончилось успешно то 
     *      и копия состояния.
     */
    public StateWaitResult<T> waitForState(int[] states, long timeout);
    
    public StateWaitResult<T> waitForNewState(State state, long timeout);
    /**
     * Добавляет слушатель состояния
     * @param listener
     */
    public void addStateListener(StateListener listener);
    /**
     * Удаляет слушатель состояния
     * @param listener
     */
    public void removeStateListener(StateListener listener);
    
}
