/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.mixnet.service;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERUTF8String;
import org.cryptoworkshop.ximix.common.message.BoardDetails;
import org.cryptoworkshop.ximix.common.message.BoardUploadMessage;
import org.cryptoworkshop.ximix.common.message.CapabilityMessage;
import org.cryptoworkshop.ximix.common.message.ClientMessage;
import org.cryptoworkshop.ximix.common.message.CommandMessage;
import org.cryptoworkshop.ximix.common.message.Message;
import org.cryptoworkshop.ximix.common.message.MessageReply;
import org.cryptoworkshop.ximix.common.message.MessageType;
import org.cryptoworkshop.ximix.common.service.NodeContext;
import org.cryptoworkshop.ximix.common.service.Service;
import org.cryptoworkshop.ximix.common.service.ServiceConnectionException;

public class RemoteBoardHostingService
    implements Service
{
    private final NodeContext nodeContext;
    private final String nodeName;
    private final CapabilityMessage capabilityMessage;

    public RemoteBoardHostingService(NodeContext context, String nodeName, CapabilityMessage capabilityMessage)
    {
        this.nodeName = nodeName;
        this.nodeContext = context;
        this.capabilityMessage = capabilityMessage;
    }

    public CapabilityMessage getCapability()
    {
        return capabilityMessage;
    }

    public MessageReply handle(Message message)
    {
        try
        {
            return nodeContext.getPeerMap().get(nodeName).sendMessage((MessageType)message.getType(), message.getPayload());
        }
        catch (ServiceConnectionException e)
        {
            return new MessageReply(MessageReply.Type.ERROR, new DERUTF8String(e.toString()));
        }
    }

    public boolean isAbleToHandle(Message message)
    {
        Enum type = message.getType();

        if (type instanceof ClientMessage.Type)
        {
            if (type == ClientMessage.Type.UPLOAD_TO_BOARD)
            {
                BoardUploadMessage uploadMessage = BoardUploadMessage.getInstance(message.getPayload());

                for (ASN1Encodable enc : capabilityMessage.getDetails())
                {
                    BoardDetails details = BoardDetails.getInstance(enc);

                    if (details.getBoardName().equals(uploadMessage.getBoardName()))
                    {
                        return true;
                    }
                }
            }
        }
        else
        {
            if  (type == CommandMessage.Type.SUSPEND_BOARD
                || type == CommandMessage.Type.ACTIVATE_BOARD
                || type == CommandMessage.Type.SHUFFLE_AND_MOVE_BOARD_TO_NODE
                || type == CommandMessage.Type.DOWNLOAD_BOARD_CONTENTS
                || type == CommandMessage.Type.TRANSFER_TO_BOARD)
            {
                return true;
            }
        }

        return false;
    }
}