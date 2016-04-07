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
import org.onesec.raven.sdp.SdpAttr;
import org.onesec.raven.sip.impl.AbstractMultiValueSipHeader;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractMultiValueSdpAttr<T> extends AbstractMultiValueSipHeader<T> implements SdpAttr<T>{

    public AbstractMultiValueSdpAttr(String name, List<String> values) {
        super(name, values);
    }

    @Override
    public ByteBuf writeTo(ByteBuf buf) {
        for (T value: values) {
            buf.writeBytes(A_BYTES);
            buf.writeBytes(getBytesOfName()).writeByte(':');
            buf.writeBytes(getBytesOfValue(value));
        }
        return buf;
    }    
}
