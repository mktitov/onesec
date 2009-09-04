/*
 *  Copyright 2009 Mikhail Titov.
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

package org.onesec.raven.ivr.impl;

import java.io.InputStream;
import org.onesec.raven.ivr.InputStreamSource;

/**
 *
 * @author Mikhail Titov
 */
public class ResourceInputStreamSource implements InputStreamSource
{
    private final String resourceName;

    public ResourceInputStreamSource(String resourceName)
    {
        this.resourceName = resourceName;
    }

    public InputStream getInputStream()
    {
        return this.getClass().getResourceAsStream(resourceName);
    }
}
