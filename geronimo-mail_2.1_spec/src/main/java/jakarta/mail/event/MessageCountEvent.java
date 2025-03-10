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

package jakarta.mail.event;

import jakarta.mail.Folder;
import jakarta.mail.Message;

/**
 * Event indicating a change in the number of messages in a folder.
 *
 * @version $Rev$ $Date$
 */
public class MessageCountEvent extends MailEvent {
	
	private static final long serialVersionUID = -7447022340837897369L;
	
    /**
     * Messages were added to the folder.
     */
    public static final int ADDED = 1;

    /**
     * Messages were removed from the folder.
     */
    public static final int REMOVED = 2;

    /**
     * The affected messages.
     */
    protected transient Message msgs[];

    /**
     * The event type.
     */
    protected int type;

    /**
     * If true, then messages were expunged from the folder by this client
     * and message numbers reflect the deletion; if false, then the change
     * was the result of an expunge by a different client.
     */
    protected boolean removed;

    /**
     * Construct a new event.
     *
     * @param folder   the folder containing the messages
     * @param type     the event type
     * @param removed  indicator of whether messages were expunged by this client
     * @param messages the affected messages
     */
    public MessageCountEvent(final Folder folder, final int type, final boolean removed, final Message messages[]) {
        super(folder);
        this.msgs = messages;
        this.type = type;
        this.removed = removed;
    }

    /**
     * Return the event type.
     *
     * @return the event type
     */
    public int getType() {
        return type;
    }

    /**
     * @return whether this event was the result of an expunge by this client
     * @see MessageCountEvent#removed
     */
    public boolean isRemoved() {
        return removed;
    }

    /**
     * Return the affected messages.
     *
     * @return the affected messages
     */
    public Message[] getMessages() {
        return msgs;
    }

    @Override
    public void dispatch(final Object listener) {
        final MessageCountListener l = (MessageCountListener) listener;
        switch (type) {
        case ADDED:
            l.messagesAdded(this);
            break;
        case REMOVED:
            l.messagesRemoved(this);
            break;
        default:
            throw new IllegalArgumentException("Invalid type " + type);
        }
    }
}
