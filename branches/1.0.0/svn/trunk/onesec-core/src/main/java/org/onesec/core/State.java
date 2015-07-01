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
     * ���������� ������ �� ���������� �������� ������� ����������
     * @return
     */
    public O getObservableObject();
    /**
     * ���������� ������������� �������� ��������� 
     * @return
     */
    public int getId();
    /**
     * ���������� ��� �������������� �������� ��������� 
     * @return
     */
    public String getIdName();
    /**
     * ������� ��������� �������� ���������. �� ���� ���������� ���������� ��������� ���������.
     * @return
     */
    public long getCounter();
    /**
     * ������ <b>true</b> ���� � �������, ��������� �������� ���������� ������ ��������, ���������
     * ����� �� ������.
     * @return
     * @see #getErrorException()
     * @see #getErrorMessage()
     */
    public boolean hasError();
    /**
     * ������ �������������� �������� ������� �������� � ������������ �������.
     * @return
     */
    public Throwable getErrorException();
    /**
     * ������ ��������� �� ������, ������������ � ������������ �������.
     * @return
     */
    public String getErrorMessage();
    /**
     * ������� ���� �� ��������� ���������� � ��������� <code>states</code>.
     * 
     * @param states ������ ��������� ���� �� ������� ����� ���������
     * @param timeout ����� ����� ������� ����� ���������� �������� 
     * @return ���������� � ��� ��� ����������� �������� � ���� �������� ����������� ������� �� 
     *      � ����� ���������.
     */
    public StateWaitResult<T> waitForState(int[] states, long timeout);
    
    public StateWaitResult<T> waitForNewState(State state, long timeout);
    /**
     * ��������� ��������� ���������
     * @param listener
     */
    public void addStateListener(StateListener listener);
    /**
     * ������� ��������� ���������
     * @param listener
     */
    public void removeStateListener(StateListener listener);
    
}
