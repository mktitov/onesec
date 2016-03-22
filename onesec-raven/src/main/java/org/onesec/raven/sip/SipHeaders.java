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
package org.onesec.raven.sip;

import org.onesec.raven.sip.impl.SipUtils;

/**
 *
 * @author Mikhail Titov
 */
public interface SipHeaders extends ByteBufWriteable {
    public enum Names {
        Via("v"), 
        Call_Id("i"), 
        From("f"), To("t"), Contact("m"), 
        Content_Encoding("e"), Content_Length("l"), Content_Type("c"),
        Subject("s"),
        Supported("k");
        
        public final String headerName;
        private final byte[] bytesOfHeaderName;
        private final String compactForm;
        private final byte[] bytesOfCompactForm;

        private Names() {
            this(null);
        }
        
        private Names(final String compactForm) {
            this.compactForm = compactForm;
            this.bytesOfCompactForm = compactForm==null? null : SipUtils.toBytes(compactForm);
            this.headerName = name().replace('_', '-');
            this.bytesOfHeaderName = SipUtils.toBytes(headerName);            
        }

        public byte[] getBytesOfHeaderName() {
            return bytesOfHeaderName;
        }

        public String getHeaderName() {
            return headerName;
        }

        public String getCompactForm() {
            return compactForm;
        }

        public byte[] getBytesOfCompactForm() {
            return bytesOfCompactForm;
        }
        
        public static Names getByHeaderName(String headerName) {
            final boolean compactForm = headerName.length()==1;
            for (Names name: Names.values()) {
                if (compactForm) {
                    if (name.compactForm.equals(headerName))
                        return name;
                } else {
                    if (name.headerName.equals(headerName))
                        return name;
                }
            }
            return null;
        }
    }
    public <T extends SipHeader> T get(String name);
    public void add(SipHeader header);
}
