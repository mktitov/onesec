/*
 *  Copyright 2011 Mikhail Titov.
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

import java.io.IOException;
import javax.media.Time;
import javax.media.protocol.DataSource;

/**
 *
 * @author Mikhail Titov
 */
public class TestDataSource extends DataSource
{

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void connect() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void disconnect() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void start() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void stop() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getControl(String arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object[] getControls() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Time getDuration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
