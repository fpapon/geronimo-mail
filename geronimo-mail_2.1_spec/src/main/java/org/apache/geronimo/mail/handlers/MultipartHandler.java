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

package org.apache.geronimo.mail.handlers;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.activation.ActivationDataFlavor;
import jakarta.activation.DataContentHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;

public class MultipartHandler implements DataContentHandler {
    /**
     * Field dataFlavor
     */
    ActivationDataFlavor dataFlavor;

    public MultipartHandler(){
        dataFlavor = new ActivationDataFlavor(MimeMultipart.class, "multipart/mixed", "Multipart");
    }

    /**
     * Constructor TextHandler
     *
     * @param dataFlavor
     */
    public MultipartHandler(final ActivationDataFlavor dataFlavor) {
        this.dataFlavor = dataFlavor;
    }

    /**
     * Method getDF
     *
     * @return dataflavor
     */
    protected ActivationDataFlavor getDF() {
        return dataFlavor;
    }

    /**
     * Method getTransferDataFlavors
     *
     * @return dataflavors
     */
    public ActivationDataFlavor[] getTransferDataFlavors() {
        return (new ActivationDataFlavor[]{dataFlavor});
    }

    /**
     * Method getTransferData
     *
     * @param dataflavor
     * @param datasource
     * @return
     * @throws IOException
     */
    public Object getTransferData(final ActivationDataFlavor dataflavor, final DataSource datasource)
            throws IOException {
        if (getDF().equals(dataflavor)) {
            return getContent(datasource);
        }
        return null;
    }

    /**
     * Method getContent
     *
     * @param datasource
     * @return
     * @throws IOException
     */
    public Object getContent(final DataSource datasource) throws IOException {
        try {
            return new MimeMultipart(datasource);
        } catch (final MessagingException e) {
            // if there is a syntax error from the datasource parsing, the content is
            // just null.
            return null;
        }
    }

    /**
     * Method writeTo
     *
     * @param object
     * @param s
     * @param outputstream
     * @throws IOException
     */
    public void writeTo(final Object object, final String s, final OutputStream outputstream) throws IOException {
        // if this object is a MimeMultipart, then delegate to the part.
        if (object instanceof MimeMultipart) {
            try {
                ((MimeMultipart)object).writeTo(outputstream);
            } catch (final MessagingException e) {
                // we need to transform any exceptions into an IOException.
                throw new IOException("Exception writing MimeMultipart: " + e.toString());
            }
        }
    }
}
