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

package org.onesec.core.provider;

/**
 *
 * @author Mikhail Titov
 */
public interface ProviderConfiguration {
    /**
     * ���������� ���������� ����� ������������
     * @return
     */
    public int getId();
    /**
     * ���������� ���������� ��� ������������
     * @return
     */
    public String getName();
    /**
     * ���������� ������ ����� � ��������� ��������������� ��������� ������������� � ������
     * �� ������� ���������� CallManager.
     * @return
     */
    public Integer getFromNumber();
    /**
     * ���������� ��������� ����� � ��������� ��������������� ��������� ������������� � ������
     * �� ������� ���������� CallManager.
     * @return
     */
    public Integer getToNumber();
    /**
     * ���������� ��� ������������ � CallManager �� ��� �������� ����� �������������� �������� 
     * �� �������� ���������� ������� � ���������� � �������� �����������.
     * @return
     */
    public String getUser();
    /**
     * ������ ������������
     * @return
     */
    public String getPassword();
    /**
     * ���� �� ������� ���������� CallManager
     * @return
     */
    public String getHost();
}
