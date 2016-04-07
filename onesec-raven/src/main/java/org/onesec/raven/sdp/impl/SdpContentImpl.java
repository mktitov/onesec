/*
 * Copyright 2016 Mikhail Titov.
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
package org.onesec.raven.sdp.impl;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.onesec.raven.sdp.Connection;
import org.onesec.raven.sdp.MediaDescription;
import org.onesec.raven.sdp.Origin;
import org.onesec.raven.sdp.SdpContent;
import org.onesec.raven.sdp.TimeDescription;

/**
 *
 * @author Mikhail Titov
 */
public class SdpContentImpl implements SdpContent {
    private final byte version;
    private final Origin origin;
    private final String sessionName;
    private final String sessionInformation;
    private final Connection connection;
    private final List<TimeDescription> timeDescriptions;
    private final List<MediaDescription> mediaDescriptions;
            

    public SdpContentImpl(byte version, Origin origin, String sessionName, String sessionInformation, Connection connection, 
            List<TimeDescription> timeDescriptions, List<MediaDescription> mediaDescriptions) 
    {
        this.version = version;
        this.origin = origin;
        this.sessionName = sessionName;
        this.sessionInformation = sessionInformation;
        this.connection = connection;
        this.timeDescriptions = timeDescriptions;
        this.mediaDescriptions = mediaDescriptions;
    }    

    @Override
    public ByteBuf writeTo(ByteBuf buf) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte getVersion() {
        return version;
    }

    @Override
    public Origin getOrigin() {
        return origin;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public String getSessionName() {
        return sessionName;
    }

    @Override
    public String getSessionInformation() {
        return sessionInformation;
    }

    @Override
    public List<TimeDescription> getTimeDescriptions() {
        return timeDescriptions;
    }

    @Override
    public List<MediaDescription> getMediaDescriptions() {
        return mediaDescriptions;
    }
    
}
