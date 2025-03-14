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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import jakarta.activation.DataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.MultipartDataSource;

import org.apache.geronimo.mail.util.SessionUtil;

/**
 * @version $Rev$ $Date$
 */
public class MimeMultipart extends Multipart {
	private static final String MIME_IGNORE_MISSING_ENDBOUNDARY = "mail.mime.multipart.ignoremissingendboundary";
	private static final String MIME_IGNORE_MISSING_BOUNDARY_PARAMETER = "mail.mime.multipart.ignoremissingboundaryparameter";
	private static final String MIME_IGNORE_EXISTING_BOUNDARY_PARAMETER = "mail.mime.multipart.ignoreexistingboundaryparameter";
	private static final String MIME_ALLOWEMPTY = "mail.mime.multipart.allowempty";
	
    /**
     * DataSource that provides our InputStream.
     */
    protected DataSource ds;
    
    /**
     * Indicates if the data has been parsed.
     */
    protected boolean parsed = true;

    // the content type information
    private transient ContentType type;

    /** Have we seen the final bounary line?
    *
    * @since   JavaMail 1.5
    */
    protected boolean complete = true;

    /**
     * The MIME multipart preamble text, the text that
     * occurs before the first boundary line.
     *
     * @since   JavaMail 1.5
     */
    protected String preamble = null;
    
    
    /**
     * Flag corresponding to the "mail.mime.multipart.ignoremissingendboundary"
     * property, set in the {@link #initializeProperties} method called from
     * constructors and the parse method.
     *
     * @since   JavaMail 1.5
     */
    protected boolean ignoreMissingEndBoundary = true;

    /**
     * Flag corresponding to the
     * "mail.mime.multipart.ignoremissingboundaryparameter"
     * property, set in the {@link #initializeProperties} method called from
     * constructors and the parse method.
     *
     * @since   JavaMail 1.5
     */
    protected boolean ignoreMissingBoundaryParameter = true;

    /**
     * Flag corresponding to the
     * "mail.mime.multipart.ignoreexistingboundaryparameter"
     * property, set in the {@link #initializeProperties} method called from
     * constructors and the parse method.
     *
     * @since   JavaMail 1.5
     */
    protected boolean ignoreExistingBoundaryParameter = false;

    /**
     * Flag corresponding to the "mail.mime.multipart.allowempty"
     * property, set in the {@link #initializeProperties} method called from
     * constructors and the parse method.
     *
     * @since   JavaMail 1.5
     */
    protected boolean allowEmpty = false;

    /**
     * Initialize flags that control parsing behavior,
     * based on System properties described above in
     * the class documentation.
     *
     * @since   JavaMail 1.5
     */
    protected void initializeProperties() {
        
        ignoreMissingEndBoundary = SessionUtil.getBooleanProperty(MIME_IGNORE_MISSING_ENDBOUNDARY, true);
        ignoreMissingBoundaryParameter = SessionUtil.getBooleanProperty(MIME_IGNORE_MISSING_BOUNDARY_PARAMETER, true);
        ignoreExistingBoundaryParameter = SessionUtil.getBooleanProperty(MIME_IGNORE_EXISTING_BOUNDARY_PARAMETER, false);
        allowEmpty = SessionUtil.getBooleanProperty(MIME_ALLOWEMPTY, false);
         
    }

    /**
     * Create an empty MimeMultipart with content type "multipart/mixed"
     */
    public MimeMultipart() {
        this("mixed");
    }

    /**
     * Create an empty MimeMultipart with the subtype supplied.
     *
     * @param subtype the subtype
     */
    public MimeMultipart(final String subtype) {
        type = new ContentType("multipart", subtype, null);
        type.setParameter("boundary", getBoundary());
        contentType = type.toString();
        initializeProperties();
    }

    /**
     * Create a MimeMultipart from the supplied DataSource.
     *
     * @param dataSource the DataSource to use
     * @throws MessagingException
     * @throws ParseException
     */
    public MimeMultipart(final DataSource dataSource) throws MessagingException {
        ds = dataSource;
        if (dataSource instanceof MultipartDataSource) {
            super.setMultipartDataSource((MultipartDataSource) dataSource);
            parsed = true;
        } else {
            // We keep the original, provided content type string so that we
            // don't end up changing quoting/formatting of the header unless
            // changes are made to the content type.  James is somewhat dependent
            // on that behavior.
            contentType = ds.getContentType();
            type = new ContentType(contentType);
            parsed = false;
        }
    }
    
    
    /**
     * Construct a MimeMultipart object of the default "mixed" subtype,
     * and with the given body parts.  More body parts may be added later.
     *
     * @since   JavaMail 1.5
     */
    public MimeMultipart(final BodyPart... parts) throws MessagingException {
        this("mixed");
        this.parts.addAll(Arrays.asList(parts));
    }

    /**
     * Construct a MimeMultipart object of the given subtype
     * and with the given body parts.  More body parts may be added later.
     *
     * @since   JavaMail 1.5
     */
    public MimeMultipart(final String subtype, final BodyPart... parts)
                throws MessagingException {
        this(subtype);
        this.parts.addAll(Arrays.asList(parts));
    }

    public void setSubType(final String subtype) throws MessagingException {
        type.setSubType(subtype);
        contentType = type.toString();
    }

    @Override
    public int getCount() throws MessagingException {
        parse();
        return super.getCount();
    }

    @Override
    public synchronized BodyPart getBodyPart(final int part) throws MessagingException {
        parse();
        return super.getBodyPart(part);
    }

    public BodyPart getBodyPart(final String cid) throws MessagingException {
        parse();
        for (int i = 0; i < parts.size(); i++) {
            final MimeBodyPart bodyPart = (MimeBodyPart) parts.get(i);
            if (cid.equals(bodyPart.getContentID())) {
                return bodyPart;
            }
        }
        return null;
    }

    protected void updateHeaders() throws MessagingException {
        parse();
        for (int i = 0; i < parts.size(); i++) {
            final MimeBodyPart bodyPart = (MimeBodyPart) parts.get(i);
            bodyPart.updateHeaders();
        }
    }

    private static byte[] dash = { '-', '-' };
    private static byte[] crlf = { 13, 10 };

    @Override
    public void writeTo(final OutputStream out) throws IOException, MessagingException {
        parse();
        final String boundary = type.getParameter("boundary");
        final byte[] bytes = boundary.getBytes("ISO8859-1");
        
        if(!allowEmpty && parts.size() == 0) {
            throw new MessagingException("Multipart content with no body parts is not allowed");
        }

        if (preamble != null) {
            final byte[] preambleBytes = preamble.getBytes("ISO8859-1");
            // write this out, followed by a line break.
            out.write(preambleBytes);
            out.write(crlf);
        }

        for (int i = 0; i < parts.size(); i++) {
            final BodyPart bodyPart = (BodyPart) parts.get(i);
            out.write(dash);
            out.write(bytes);
            out.write(crlf);
            bodyPart.writeTo(out);
            out.write(crlf);
        }
        out.write(dash);
        out.write(bytes);
        out.write(dash);
        out.write(crlf);
        out.flush();
    }

    protected void parse() throws MessagingException {
        if (parsed) {
            return;
        }
                
        initializeProperties();

        try {
            final ContentType cType = new ContentType(contentType);
            final String boundaryString = cType.getParameter("boundary");
            
            if(!ignoreMissingBoundaryParameter && boundaryString  == null) {
                throw new MessagingException("Missing boundary parameter in content-type");
            }           
                        
            final InputStream is = new BufferedInputStream(ds.getInputStream());
            BufferedInputStream pushbackInStream = null;
            boolean boundaryFound = false;
            
            byte[] boundary = null;
            if (boundaryString == null || ignoreExistingBoundaryParameter) {
                pushbackInStream = new BufferedInputStream(is, 1200);
                // read until we find something that looks like a boundary string
                boundary = readTillFirstBoundary(pushbackInStream);
                boundaryFound = boundary != null;
            }
            else {
                boundary = ("--" + boundaryString).getBytes("ISO8859-1");
                pushbackInStream = new BufferedInputStream(is, boundary.length + 1000);
                boundaryFound = readTillFirstBoundary(pushbackInStream, boundary);
            }
            
            
            
            if(allowEmpty && !boundaryFound) {
            	parsed = true;
                return;
            }
            
            if(!allowEmpty && !boundaryFound) {
                throw new MessagingException("Multipart content with no body parts is not allowed");
            }
            

            while (true) {
                MimeBodyPartInputStream partStream;
                partStream = new MimeBodyPartInputStream(pushbackInStream, boundary);
                addBodyPart(new MimeBodyPart(partStream));

                // terminated by an EOF rather than a proper boundary?
                if (!partStream.boundaryFound) {
                	
                    if (!ignoreMissingEndBoundary) {
                        throw new MessagingException("Missing Multi-part end boundary");
                    }
                    complete = false;
                    break;
                }
                // if we hit the final boundary, stop processing this
                if (partStream.finalBoundaryFound) {
                    break;
                }
            }
            
           
            
        } catch (final Exception e){
            throw new MessagingException(e.toString(),e);
        }
        parsed = true;
    }

    /**
     * Move the read pointer to the beginning of the first part
     * read till the end of first boundary.  Any data read before this point are
     * saved as the preamble.
     *
     * @param pushbackInStream
     * @param boundary
     * @throws MessagingException
     */
    private byte[] readTillFirstBoundary(final BufferedInputStream pushbackInStream) throws MessagingException {
        final ByteArrayOutputStream preambleStream = new ByteArrayOutputStream();

        try {
            while (true) {
                // read the next line
                final byte[] line = readLine(pushbackInStream);
                // hit an EOF?
                if (line == null || line.length==0) {
                    return null;//throw new MessagingException("Unexpected End of Stream while searching for first Mime Boundary");
                }
                // if this looks like a boundary, then make it so
                if (line.length > 2 && line[0] == '-' && line[1] == '-') {
                    // save the preamble, if there is one.
                    final byte[] preambleBytes = preambleStream.toByteArray();
                    if (preambleBytes.length > 0) {
                        preamble = new String(preambleBytes, "ISO8859-1");
                    }
                    return stripLinearWhiteSpace(line);
                }
                else {
                    // this is part of the preamble.
                    preambleStream.write(line);
                    preambleStream.write('\r');
                    preambleStream.write('\n');
                }
            }
        } catch (final IOException ioe) {
            throw new MessagingException(ioe.toString(), ioe);
        }
    }


    /**
     * Scan a line buffer stripping off linear whitespace
     * characters, returning a new array without the
     * characters, if possible.
     *
     * @param line   The source line buffer.
     *
     * @return A byte array with white space characters removed,
     *         if necessary.
     */
    private byte[] stripLinearWhiteSpace(final byte[] line) {
        int index = line.length - 1;
        // if the last character is not a space or tab, we
        // can use this unchanged
        if (line[index] != ' ' && line[index] != '\t') {
            return line;
        }
        // scan backwards for the first non-white space
        for (; index > 0; index--) {
            if (line[index] != ' ' && line[index] != '\t') {
                break;
            }
        }
        // make a shorter copy of this
        final byte[] newLine = new byte[index + 1];
        System.arraycopy(line, 0, newLine, 0, index + 1);
        return newLine;
    }

    /**
     * Move the read pointer to the beginning of the first part
     * read till the end of first boundary.  Any data read before this point are
     * saved as the preamble.
     *
     * @param pushbackInStream
     * @param boundary
     * @throws MessagingException
     */
    private boolean readTillFirstBoundary(final BufferedInputStream pushbackInStream, final byte[] boundary) throws MessagingException {
        final ByteArrayOutputStream preambleStream = new ByteArrayOutputStream();

        try {
            while (true) {
                // read the next line
                final byte[] line = readLine(pushbackInStream);
                // hit an EOF?
                if (line == null || line.length==0) {
                	return false;//throw new MessagingException("Unexpected End of Stream while searching for first Mime Boundary");
                }

                // apply the boundary comparison rules to this
                if (compareBoundary(line, boundary)) {
                    // save the preamble, if there is one.
                    final byte[] preambleBytes = preambleStream.toByteArray();
                    if (preambleBytes.length > 0) {
                        preamble = new String(preambleBytes, "ISO8859-1");
                    }
                    return true;
                }

                // this is part of the preamble.
                preambleStream.write(line);
                preambleStream.write('\r');
                preambleStream.write('\n');
            }
        } catch (final IOException ioe) {
            throw new MessagingException(ioe.toString(), ioe);
        }
    }


    /**
     * Perform a boundary comparison, taking into account
     * potential linear white space
     *
     * @param line     The line to compare.
     * @param boundary The boundary we're searching for
     *
     * @return true if this is a valid boundary line, false for
     *         any mismatches.
     */
    private boolean compareBoundary(final byte[] line, final byte[] boundary) {
        // if the line is too short, this is an easy failure
        if (line.length < boundary.length) {
            return false;
        }

        // this is the most common situation
        if (line.length == boundary.length) {
            return Arrays.equals(line, boundary);
        }
        // the line might have linear white space after the boundary portions
        for (int i = 0; i < boundary.length; i++) {
            // fail on any mismatch
            if (line[i] != boundary[i]) {
                return false;
            }
        }
        // everything after the boundary portion must be linear whitespace
        for (int i = boundary.length; i < line.length; i++) {
            // fail on any mismatch
            if (line[i] != ' ' && line[i] != '\t') {
                return false;
            }
        }
        // these are equivalent
        return true;
    }

    /**
     * Read a single line of data from the input stream,
     * returning it as an array of bytes.
     *
     * @param in     The source input stream.
     *
     * @return A byte array containing the line data.  Returns
     *         null if there's nothing left in the stream.
     * @exception MessagingException
     */
    private byte[] readLine(final BufferedInputStream in) throws IOException
    {
        final ByteArrayOutputStream line = new ByteArrayOutputStream();

        while (in.available() > 0) {
            int value = in.read();
            if (value == -1) {
                // if we have nothing in the accumulator, signal an EOF back
                if (line.size() == 0) {
                    return null;
                }
                break;
            }
            else if (value == '\r') {
                in.mark(10);
                value = in.read();
                // we expect to find a linefeed after the carriage return, but
                // some things play loose with the rules.
                if (value != '\n') {
                    in.reset();
                }
                break;
            }
            else if (value == '\n') {
                // naked linefeed, allow that
                break;
            }
            else {
                // write this to the line
                line.write((byte)value);
            }
        }
        // return this as an array of bytes
        return line.toByteArray();
    }


    protected InternetHeaders createInternetHeaders(final InputStream in) throws MessagingException {
        return new InternetHeaders(in);
    }

    protected MimeBodyPart createMimeBodyPart(final InternetHeaders headers, final byte[] data) throws MessagingException {
        return new MimeBodyPart(headers, data);
    }

    protected MimeBodyPart createMimeBodyPart(final InputStream in) throws MessagingException {
        return new MimeBodyPart(in);
    }

    // static used to track boundary value allocations to help ensure uniqueness.
    private static int part;

    private synchronized static String getBoundary() {
        int i;
        synchronized(MimeMultipart.class) {
            i = part++;
        }
        final StringBuffer buf = new StringBuffer(64);
        buf.append("----=_Part_").append(i).append('_').append((new Object()).hashCode()).append('.').append(System.currentTimeMillis());
        return buf.toString();
    }

    private class MimeBodyPartInputStream extends InputStream {
        BufferedInputStream inStream;
        public boolean boundaryFound = false;
        byte[] boundary;
        public boolean finalBoundaryFound = false;

        public MimeBodyPartInputStream(final BufferedInputStream inStream, final byte[] boundary) {
            super();
            this.inStream = inStream;
            this.boundary = boundary;
        }

        /**
         * The base reading method for reading one character
         * at a time.
         *
         * @return The read character, or -1 if an EOF was encountered.
         * @exception IOException
         */
        @Override
        public int read() throws IOException {
            if (boundaryFound) {
                return -1;
            }

            // read the next value from stream
            final int firstChar = inStream.read();
            // premature end?  Handle it like a boundary located
            if (firstChar == -1) {
            	//DO NOT treat this a a boundary because if we do so we have no chance to detect missing end boundaries
                return -1;
            }

            // we first need to look for a line boundary.  If we find a boundary, it can be followed by the
            // boundary marker, so we need to remember what sort of thing we found, then read ahead looking
            // for the part boundary.

            // NB:, we only handle [\r]\n--boundary marker[--]
            // we need to at least accept what most mail servers would consider an
            // invalid format using just '\n'
            if (firstChar != '\r' && firstChar != '\n') {
                // not a \r, just return the byte as is
                return firstChar;
            }
            // we might need to rewind to this point.  The padding is to allow for
            // line terminators and linear whitespace on the boundary lines
            inStream.mark(boundary.length + 1000);
            // we need to keep track of the first read character in case we need to
            // rewind back to the mark point
            int value = firstChar;
            // if this is a '\r', then we require the '\n'
            if (value == '\r') {
                // now scan ahead for the second character
                value = inStream.read();
                if (value != '\n') {
                    // only a \r, so this can't be a boundary.  Return the
                    // \r as if it was data, after first resetting
                    inStream.reset();
                    return '\r';
                }
            }

            value = inStream.read();
            // if the next character is not a boundary start, we
            // need to handle this as a normal line end
            if ((byte) value != boundary[0]) {
                // just reset and return the first character as data
                inStream.reset();
                return firstChar;
            }

            // we're here because we found a "\r\n-" sequence, which is a potential
            // boundary marker.  Read the individual characters of the next line until
            // we have a mismatch

            // read value is the first byte of the boundary. Start matching the
            // next characters to find a boundary
            int boundaryIndex = 0;
            while ((boundaryIndex < boundary.length) && ((byte) value == boundary[boundaryIndex])) {
                value = inStream.read();
                boundaryIndex++;
            }
            // if we didn't match all the way, we need to push back what we've read and
            // return the EOL character
            if (boundaryIndex != boundary.length) {
                // Boundary not found. Restoring bytes skipped.
                // just reset and return the first character as data
                inStream.reset();
                return firstChar;
            }

            // The full boundary sequence should be \r\n--boundary string[--]\r\n
            // if the last character we read was a '-', check for the end terminator
            if (value == '-') {
                value = inStream.read();
                // crud, we have a bad boundary terminator.  We need to unwind this all the way
                // back to the lineend and pretend none of this ever happened
                if (value != '-') {
                    // Boundary not found. Restoring bytes skipped.
                    // just reset and return the first character as data
                    inStream.reset();
                    return firstChar;
                }
                // on the home stretch, but we need to verify the LWSP/EOL sequence
                value = inStream.read();
                // first skip over the linear whitespace
                while (value == ' ' || value == '\t') {
                    value = inStream.read();
                }

                // We've matched the final boundary, skipped any whitespace, but
                // we've hit the end of the stream.  This is highly likely when
                // we have nested multiparts, since the linend terminator for the
                // final boundary marker is eated up as the start of the outer
                // boundary marker.  No CRLF sequence here is ok.
                if (value == -1) {
                    // we've hit the end of times...
                    finalBoundaryFound = true;
                    // we have a boundary, so return this as an EOF condition
                    boundaryFound = true;
                    return -1;
                }

                // this must be a CR or a LF...which leaves us even more to push back and forget
                if (value != '\r' && value != '\n') {
                    // Boundary not found. Restoring bytes skipped.
                    // just reset and return the first character as data
                    inStream.reset();
                    return firstChar;
                }

                // if this is carriage return, check for a linefeed
                if (value == '\r') {
                    // last check, this must be a line feed
                    value = inStream.read();
                    if (value != '\n') {
                        // SO CLOSE!
                        // Boundary not found. Restoring bytes skipped.
                        // just reset and return the first character as data
                        inStream.reset();
                        return firstChar;
                    }
                }

                // we've hit the end of times...
                finalBoundaryFound = true;
            }
            else {
                // first skip over the linear whitespace
                while (value == ' ' || value == '\t') {
                    value = inStream.read();
                }
                // this must be a CR or a LF...which leaves us even more to push back and forget
                if (value != '\r' && value != '\n') {
                    // Boundary not found. Restoring bytes skipped.
                    // just reset and return the first character as data
                    inStream.reset();
                    return firstChar;
                }

                // if this is carriage return, check for a linefeed
                if (value == '\r') {
                    // last check, this must be a line feed
                    value = inStream.read();
                    if (value != '\n') {
                        // SO CLOSE!
                        // Boundary not found. Restoring bytes skipped.
                        // just reset and return the first character as data
                        inStream.reset();
                        return firstChar;
                    }
                }
            }
            // we have a boundary, so return this as an EOF condition
            boundaryFound = true;
            return -1;
        }
    }


    /**
     * Return true if the final boundary line for this multipart was
     * seen when parsing the data.
     *
     * @return
     * @exception MessagingException
     */
    public boolean isComplete() throws MessagingException {
        // make sure we've parsed this
        parse();
        return complete;
    }


    /**
     * Returns the preamble text that appears before the first bady
     * part of a MIME multi part.  The preamble is optional, so this
     * might be null.
     *
     * @return The preamble text string.
     * @exception MessagingException
     */
    public String getPreamble() throws MessagingException {
        parse();
        return preamble;
    }

    /**
     * Set the message preamble text.  This will be written before
     * the first boundary of a multi-part message.
     *
     * @param preamble The new boundary text.  This is complete lines of text, including
     *                 new lines.
     *
     * @exception MessagingException
     */
    public void setPreamble(final String preamble) throws MessagingException {
        this.preamble = preamble;
    }
}
