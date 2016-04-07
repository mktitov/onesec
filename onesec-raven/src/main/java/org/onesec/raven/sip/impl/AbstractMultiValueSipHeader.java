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
package org.onesec.raven.sip.impl;

import io.netty.buffer.ByteBuf;
import java.util.List;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractMultiValueSipHeader<T> extends AbstractSipHeader<T> {
    protected final List<T> values;
    
    public AbstractMultiValueSipHeader(final String name, final List<String> values) {
        super(name);
        this.values = createValuesList(values);
    }        
    
    protected abstract List<T> createValuesList(final List<String> stringValues);
    
    @Override
    public T getFirstValue() {
        return values.isEmpty()? null : values.get(0);
    }

    @Override
    public List<T> getValues() {
        return values;    
    }
    
    @Override
    public ByteBuf writeTo(final ByteBuf buf) {
        for (T value: values) {
            buf.writeBytes(getBytesOfName()).writeByte(':').writeByte(' ');
            buf.writeBytes(getBytesOfValue(value));
        }
        return buf;
    }    
    
}
