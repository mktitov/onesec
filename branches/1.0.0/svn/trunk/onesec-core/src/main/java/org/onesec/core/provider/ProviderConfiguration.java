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
     * Возвращает уникальный номер конфигурации
     * @return
     */
    public int getId();
    /**
     * Возвращает уинкальное имя конфигурации
     * @return
     */
    public String getName();
    /**
     * Возвращает первый номер в диапазоне обслуживающимся указанным пользователем и хостом
     * на котором установлен CallManager.
     * @return
     */
    public Integer getFromNumber();
    /**
     * Возвращает последний номер в диапазоне обслуживающимся указанным пользователем и хостом
     * на котором установлен CallManager.
     * @return
     */
    public Integer getToNumber();
    /**
     * Возвращает имя пользователя в CallManager из под которого будет осуществляться действия 
     * на заданным диапазоном номеров и связанными с номерами терминалами.
     * @return
     */
    public String getUser();
    /**
     * Пароль пользователя
     * @return
     */
    public String getPassword();
    /**
     * Хост на котором установлен CallManager
     * @return
     */
    public String getHost();
}
