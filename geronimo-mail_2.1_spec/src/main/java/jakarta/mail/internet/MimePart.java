/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jakarta.mail.internet;

import java.util.Enumeration;

import jakarta.mail.MessagingException;
import jakarta.mail.Part;

/**
 * @version $Rev$ $Date$
 */
public interface MimePart extends Part {
    public abstract void addHeaderLine(String line) throws MessagingException;

    public abstract Enumeration<String> getAllHeaderLines() throws MessagingException;

    public abstract String getContentID() throws MessagingException;

    public abstract String[] getContentLanguage() throws MessagingException;

    public abstract String getContentMD5() throws MessagingException;

    public abstract String getEncoding() throws MessagingException;

    public abstract String getHeader(String header, String delimiter)
            throws MessagingException;

    public abstract Enumeration<String> getMatchingHeaderLines(String[] names)
            throws MessagingException;

    public abstract Enumeration<String> getNonMatchingHeaderLines(String[] names)
            throws MessagingException;

    public abstract void setContentLanguage(String[] languages)
            throws MessagingException;

    public abstract void setContentMD5(String content)
            throws MessagingException;

    public abstract void setText(String text) throws MessagingException;

    public abstract void setText(String text, String charset)
            throws MessagingException;

    public abstract void setText(String text, String charset, String subType)
            throws MessagingException;
}
