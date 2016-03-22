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
package org.onesec.raven.sdp;

import java.util.List;
import org.onesec.raven.sip.SipMessageContent;

/**
 *
 * @author Mikhail Titov
 */
public interface SdpContent extends SipMessageContent {
    public byte getVersion();
    public Origin getOrigin();
    public String getSessionName();
    public String getSessionInformation();
    public Connection getConnection();
    public List<TimeDescription> getTimeDescriptions();
    public List<MediaDescription> getMediaDescriptions();    
}
