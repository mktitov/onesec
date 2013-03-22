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
package org.onesec.raven;

/**
 *
 * @author Mikhail Titov
 */
public interface Constants {
    
    public final static String RESOURCE_BASE = "IVR/";
    public final static String SOUNDS_RESOURCES_BASE = RESOURCE_BASE+"sounds/";
    public final static String NUMBERS_FEMALE_RESOURCE = SOUNDS_RESOURCES_BASE + "numbers/female/";
    public final static String NUMBERS_MALE_RESOURCE = SOUNDS_RESOURCES_BASE + "numbers/male/";
    public final static String TIME_WORDS_RESOURCE = SOUNDS_RESOURCES_BASE + "time words/male/";
    public final static String VMAIL_BOX = "vmailBox";
    public final static String VMAIL_BOX_NUMBER = "vmailBoxNumber";
}
