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
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.internal.AppendableCharSequence;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onesec.raven.sip.SipConstants;
import org.onesec.raven.sip.SipMessageDecoderException;
import static org.onesec.raven.sip.impl.SipMessageDecoder.decoderException;
import org.raven.Pair;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class SipUtils {
    
    public final static int split(final String str, final char ch, final int startFrom, final int max, final boolean expandLast, final String[] arr) {
        if (arr==null || arr.length==0)
            return 0;
        final int strlen = str.length();
        if (strlen==0) {
            arr[0] = str;
            return 1;
        }
        int start = startFrom;
        int ind=0;
        int pos;
        while ( (pos=str.indexOf(ch, start))>=0 ) {
            if (ind+1>=arr.length || (max!=-1 && ind+1>=max)) {
                arr[ind++] = expandLast? str.substring(start) : str.substring(start, pos);
                return ind;
            } 
            arr[ind++] = str.substring(start, pos);
            start = pos+1;            
        }
        arr[ind++] = start==strlen? "" : str.substring(start, strlen);
        return ind;
    }
    
    public static boolean splitExactByWs(final String[] dest, final AppendableCharSequence line, final int startFrom) {
        int start=startFrom;
        boolean findWS = true;
        int ind=0;
        for (int i=startFrom; i<line.length(); ++i) {
            if (Character.isWhitespace(line.charAtUnsafe(i))) {
                if (findWS) {
                    findWS = false;
                    if (ind>=dest.length)
                        return false;
                    dest[ind++] = line.subStringUnsafe(start, i);
                }
            } else {
                if (!findWS) {
                    start = i;
                    findWS = true;
                }
            }
        }
        if (findWS) {
            if (ind>=dest.length)
                return false;
            dest[ind++] = line.subStringUnsafe(start, line.length());            
        }
        return ind==dest.length;
    }
        
    public final static int splitByWs(final String[] dest, final CharSequence line) {
        int start=0;
        boolean findWS = true;
        int ind=0;
        for (int i=0; i<line.length(); ++i) {
            if (Character.isWhitespace(line.charAt(i))) {
                if (findWS) {
                    findWS = false;
                    if (ind>=dest.length)
                        return ind;
                    dest[ind++] = line.subSequence(start, i).toString();
                }
            } else {
                if (!findWS) {
                    start = i;
                    findWS = true;
                }
            }
        }
        if (findWS) {
            if (ind>=dest.length)
                return ind;
            dest[ind++] = line.subSequence(start, line.length()).toString();            
        }
        return ind;
    }
    
    public final static String toHeaderName(String name) {
        StringBuilder buf = new StringBuilder(name.length());
        boolean needUpperCase = true;
        char ch;
        for (int i=0; i<name.length(); i++) {
            ch = name.charAt(i);
            if (!Character.isWhitespace(ch)) {
                if (ch=='-') {
                    needUpperCase = true;
                } else {
                    ch = needUpperCase? Character.toUpperCase(ch) : Character.toLowerCase(ch);
                    needUpperCase = false;
                }
                buf.append(ch);
            }
        }
        return buf.toString();        
    }
    
    public final static Object splitHeaderValues(String rawValue, char delimChar, char quoteChar) {
        return splitHeaderValues(rawValue, delimChar, quoteChar, false, null);        
    }
    
    public final static Object splitHeaderValuesByWS(String rawValue, char quoteChar) {
        return splitHeaderValues(rawValue, ' ', quoteChar, true, null);        
    }
    
    public final static Object splitHeaderValues(String rawValue, char delimChar, char quoteChar, boolean delimIsWS, List<String> dest) {
        int start = 0;
        boolean inQuotes = false;
        int end=-1;
        char ch;
        List<String> list = dest;
        for (int i=0; i<rawValue.length(); i++) {
            ch = rawValue.charAt(i);
            if (inQuotes) {
                if (ch==quoteChar) {
                    inQuotes = false;
                }
                end = i;
            } else {
                if ((delimIsWS && Character.isWhitespace(ch)) || (!delimIsWS && delimChar==ch)) {
                    end++;
                    if (list==null)
                        list = new ArrayList<>(4);
                    if (start<end) {
                        list.add(rawValue.substring(start, end));
                    }
                    start = i+1;
                    end = i;
                } else if (ch==quoteChar) {
                        inQuotes = true;
                    end = i;
                } else if (Character.isWhitespace(ch)) {
                    if (start==i)
                        start++;                        
                } else {
                    end = i;
                }
            }
        }
        end++;
        if (list!=null) {
            if (start<end && end<=rawValue.length()) 
                list.add(rawValue.substring(start, end));
            return list;
        } else 
            return start!=0 || end!=rawValue.length()? rawValue.substring(start, end) : rawValue;
    }
    
    public static Pair<String, Map<String, String>> slitHeaderValueAndParams(String rawValue) {
        if (rawValue==null)
            return null;
        List<String> paramsAndValues = new ArrayList<>(4);
        splitHeaderValues(rawValue, ';', '"', false, paramsAndValues);
        int size = paramsAndValues.size();
        if (size==0)
            return null;
        if (size==1)
            return new Pair<>(paramsAndValues.get(0), null);
        //parsing params
        Map<String, String> params = new HashMap<>(paramsAndValues.size()-1);
        int end; int len;
        String paramAndValue;
        String name;
        String value;
        for (int i=1; i<paramsAndValues.size(); i++) {
            paramAndValue = paramsAndValues.get(i);
            end = paramAndValue.indexOf('=');
            len = paramAndValue.length();
            if (end==-1)
                params.put(paramAndValue, null);
            else if (end+1==len) {
                params.put(paramAndValue.substring(0, end), null);
            } else {
                name = paramAndValue.substring(0, end).toLowerCase();
                if (paramAndValue.charAt(end+1)=='"') {
                    if (end+3>=len)
                        value = null;
                    else
                        value = paramAndValue.substring(end+2, len-1);
                } else {
                    value = paramAndValue.substring(end+1).toLowerCase();
                }
                params.put(name, value);
            }
        }
        return new Pair(paramsAndValues.get(0), params);
    }
    
    public static final int indexOf(char ch, CharSequence seq) {
        for (int i=0; i<seq.length(); i++)
            if (ch==seq.charAt(i))
                return i;
        return -1;
    }
    
    public static byte[] toBytes(final String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
    
    public static boolean readLine(final ByteBuf buf, final AppendableCharSequence line, final int maxSize, final boolean allDataInBuf, final Logger logger) 
            throws SipMessageDecoderException 
    {
        final int cycles = buf.readableBytes();
        for (int i=0; i<cycles; ++i) {
            char ch = (char)buf.readByte();
            if (ch==SipConstants.LF) {
                return true;
            } else if (ch!=SipConstants.CR) {
                if (line.length()>=maxSize) {
                    if (logger.isErrorEnabled())
                        logger.error("SIP header or initial line is lager than "+maxSize+" bytes.");
                    throw decoderException;
                }
                line.append(ch);                
            }
        }
        return allDataInBuf;
    }    
}
