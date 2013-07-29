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
package org.cryptoworkshop.ximix.mixnet.shuffle;

import java.io.IOException;

import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.cryptoworkshop.ximix.common.message.BoardMessage;
import org.cryptoworkshop.ximix.common.message.BoardUploadBlockMessage;
import org.cryptoworkshop.ximix.common.message.CommandMessage;
import org.cryptoworkshop.ximix.common.message.MessageReply;
import org.cryptoworkshop.ximix.common.message.PermuteAndReturnMessage;
import org.cryptoworkshop.ximix.common.message.PostedMessage;
import org.cryptoworkshop.ximix.common.message.PostedMessageBlock;
import org.cryptoworkshop.ximix.common.service.NodeContext;
import org.cryptoworkshop.ximix.common.service.ServiceConnectionException;
import org.cryptoworkshop.ximix.common.service.ServicesConnection;
import org.cryptoworkshop.ximix.mixnet.board.BulletinBoard;
import org.cryptoworkshop.ximix.mixnet.board.BulletinBoardRegistry;
import org.cryptoworkshop.ximix.mixnet.transform.Transform;

public class TransformShuffleAndReturnTask
    implements Runnable
{
    private final NodeContext nodeContext;
    private final PermuteAndReturnMessage message;
    private final BulletinBoardRegistry boardRegistry;

    public TransformShuffleAndReturnTask(NodeContext nodeContext, BulletinBoardRegistry boardRegistry, PermuteAndReturnMessage message)
    {
        this.nodeContext = nodeContext;
        this.boardRegistry = boardRegistry;
        this.message = message;
    }

    public void run()
    {
        BulletinBoard board = boardRegistry.getTransitBoard(message.getBoardName());
        Transform transform = boardRegistry.getTransform(message.getTransformName());
        IndexNumberGenerator indexGen = new IndexNumberGenerator(board.size());

        try
        {
            ServicesConnection peerConnection = nodeContext.getPeerMap().get(nodeContext.getBoardHost(message.getBoardName()));
            PostedMessageBlock.Builder messageBlockBuilder = new PostedMessageBlock.Builder(20);                  // TODO: make configurable

            if (message.getKeyID() != null)
            {
                transform.init(PublicKeyFactory.createKey(nodeContext.getPublicKey(message.getKeyID())));

                for (PostedMessage postedMessage : board)
                {
                    byte[] transformed = transform.transform(postedMessage.getMessage());

                    messageBlockBuilder.add(indexGen.nextIndex(postedMessage.getIndex()), transformed);

                    if (messageBlockBuilder.isFull())
                    {
                        MessageReply reply = peerConnection.sendMessage(CommandMessage.Type.TRANSFER_TO_BOARD, new BoardUploadBlockMessage(board.getName(), messageBlockBuilder.build()));

                        if (reply.getType() != MessageReply.Type.OKAY)
                        {
                            throw new ServiceConnectionException("message failed");
                        }
                    }
                }
            }
            else
            {
                for (PostedMessage postedMessage : board)
                {
                    messageBlockBuilder.add(postedMessage.getIndex(), postedMessage.getMessage());

                    if (messageBlockBuilder.isFull())
                    {
                        MessageReply reply = peerConnection.sendMessage(CommandMessage.Type.TRANSFER_TO_BOARD, new BoardUploadBlockMessage(board.getName(), messageBlockBuilder.build()));

                        if (reply.getType() != MessageReply.Type.OKAY)
                        {
                            throw new ServiceConnectionException("message failed");
                        }
                    }
                }
            }

            if (!messageBlockBuilder.isEmpty())
            {
                MessageReply reply = peerConnection.sendMessage(CommandMessage.Type.TRANSFER_TO_BOARD, new BoardUploadBlockMessage(board.getName(), messageBlockBuilder.build()));

                if (reply.getType() != MessageReply.Type.OKAY)
                {
                    throw new ServiceConnectionException("message failed");
                }
            }

            MessageReply reply = peerConnection.sendMessage(CommandMessage.Type.TRANSFER_TO_BOARD_ENDED, new BoardMessage(board.getName()));

            if (reply.getType() != MessageReply.Type.OKAY)
            {
                throw new ServiceConnectionException("message failed");
            }

            board.clear();
        }
        catch (ServiceConnectionException e)
        {
            e.printStackTrace();
            // TODO: log?
        }
        catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
        }
    }
}