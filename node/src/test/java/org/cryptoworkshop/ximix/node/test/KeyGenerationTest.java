package org.cryptoworkshop.ximix.node.test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.cryptoworkshop.ximix.common.conf.ConfigException;
import org.cryptoworkshop.ximix.common.message.Capability;
import org.cryptoworkshop.ximix.common.message.ClientMessage;
import org.cryptoworkshop.ximix.common.message.CommandMessage;
import org.cryptoworkshop.ximix.common.message.GenerateKeyPairMessage;
import org.cryptoworkshop.ximix.common.message.Message;
import org.cryptoworkshop.ximix.common.message.MessageReply;
import org.cryptoworkshop.ximix.common.message.MessageType;
import org.cryptoworkshop.ximix.common.service.Service;
import org.cryptoworkshop.ximix.common.service.ServiceConnectionException;
import org.cryptoworkshop.ximix.crypto.threshold.ECCommittedSecretShare;
import org.junit.Assert;
import org.cryptoworkshop.ximix.common.conf.Config;
import org.cryptoworkshop.ximix.common.message.ECCommittedSecretShareMessage;
import org.cryptoworkshop.ximix.common.service.ServicesConnection;
import org.cryptoworkshop.ximix.node.XimixNodeContext;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class KeyGenerationTest
{
    @Test
    public void testBasicGenerationNoPeers()
        throws Exception
    {
        XimixNodeContext context = new XimixNodeContext(new HashMap<String, ServicesConnection>(), new Config(createConfig("A")));

        try
        {
            ECCommittedSecretShareMessage[] messages = context.generateThresholdKey("EC_KEY", 5, 0, 4, BigInteger.valueOf(1000001));

            Assert.fail("no exception!");
        }
        catch (IllegalArgumentException e)
        {
            if (!"numberOfPeers must at least be as big as the threshold value.".equals(e.getMessage()))
            {
                Assert.fail("exception but wrong message");
            }
        }
    }

    @Test
    public void testBasicGeneration()
        throws Exception
    {
        Map<String, XimixNodeContext>  contextMap = createContextMap(5);

        XimixNodeContext context = contextMap.get("A");

        BigInteger h = BigInteger.valueOf(1000001);
        ECCommittedSecretShareMessage[] messages = context.generateThresholdKey("EC_KEY", 5, 0, 4, h);

        Assert.assertEquals(5, messages.length);

        X9ECParameters params = SECNamedCurves.getByName("secp256r1"); // TODO: should be on the context!!
        ECDomainParameters domainParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH(), params.getSeed());

        for (int i = 0; i != messages.length; i++)
        {
            ECCommittedSecretShareMessage message = ECCommittedSecretShareMessage.getInstance(params.getCurve(), messages[i].getEncoded());
            ECCommittedSecretShare share = new ECCommittedSecretShare(message.getValue(), message.getWitness(), message.getCommitmentFactors());

            Assert.assertTrue(share.isRevealed(i, domainParams, h));
        }
    }

    @Test
    public void testGenerationViaMessage()
        throws Exception
    {
        Map<String, XimixNodeContext>  contextMap = createContextMap(5);

        XimixNodeContext context = contextMap.get("A");

        BigInteger h = BigInteger.valueOf(1000001);

        ServicesConnection connection = context.getPeerMap().get("B");

        MessageReply reply = connection.sendMessage(CommandMessage.Type.INITIATE_GENERATE_KEY_PAIR, new GenerateKeyPairMessage("ECKEY", new HashSet(Arrays.asList("A", "B", "C", "D", "E")), 4, h));
            System.err.println(reply);
        ECCommittedSecretShareMessage[] messages = context.generateThresholdKey("EC_KEY", 5, 0, 4, h);

        Assert.assertEquals(5, messages.length);

        X9ECParameters params = SECNamedCurves.getByName("secp256r1"); // TODO: should be on the context!!
        ECDomainParameters domainParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH(), params.getSeed());

        for (int i = 0; i != messages.length; i++)
        {
            ECCommittedSecretShareMessage message = ECCommittedSecretShareMessage.getInstance(params.getCurve(), messages[i].getEncoded());
            ECCommittedSecretShare share = new ECCommittedSecretShare(message.getValue(), message.getWitness(), message.getCommitmentFactors());

            Assert.assertTrue(share.isRevealed(i, domainParams, h));
        }
    }

    private Map<String, XimixNodeContext> createContextMap(int size)
        throws ConfigException
    {
        final Map<String, ServicesConnection> connectionMap = new HashMap<>();
        final Map<String, ServicesConnection>[] connectionMaps = new Map[size];
        final Map<String, XimixNodeContext> nodeMap = new HashMap<>();

        for (int i = 0; i != size; i++)
        {
            connectionMaps[i] = new HashMap<>();
        }

        for (int i = 0; i != size; i++)
        {
            final String nodeName = String.valueOf((char)('A' + i));
            final int    nodeNo = i;
            final XimixNodeContext context = new XimixNodeContext(connectionMaps[nodeNo], new Config(createConfig(nodeName)));

            nodeMap.put(nodeName, context);

            connectionMap.put(nodeName, new ServicesConnection()
            {

                @Override
                public Capability[] getCapabilities()
                {
                    return context.getCapabilities();
                }

                @Override
                public MessageReply sendMessage(MessageType type, ASN1Encodable messagePayload)
                    throws ServiceConnectionException
                {
                    Message message;

                    if (type instanceof CommandMessage.Type)
                    {
                         message = new CommandMessage((CommandMessage.Type)type, messagePayload);
                    }
                    else
                    {
                        message = new ClientMessage((ClientMessage.Type)type, messagePayload);
                    }

                    Service service = context.getService(message.getType());

                    return service.handle(message);
                }

                @Override
                public MessageReply sendThresholdMessage(MessageType type, int minimumNumberOfPeers, ASN1Encodable messagePayload)
                    throws ServiceConnectionException
                {
                    for (String nodeName : connectionMaps[nodeNo].keySet())
                    {
                        Message message = Message.getInstance(messagePayload);

                        Service service = context.getService(message.getType());

                        service.handle(message);
                    }

                    return null;
                }
            });
        }

        for (int i = 0; i != size; i++)
        {
            String nodeName = String.valueOf((char)('A' + i));

            for (String node : connectionMap.keySet())
            {
                if (node.equals(nodeName))
                {
                    continue;
                }

                connectionMaps[i].put(node, connectionMap.get(node));
            }
        }

        return nodeMap;
    }

    private Element createConfig(String nodeName)
    {
        try
        {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element rootElement = document.createElement("config");

            Element portNo = document.createElement("name");

            rootElement.appendChild(portNo);
            portNo.appendChild(document.createTextNode(nodeName));

            Element services = document.createElement("services");
            rootElement.appendChild(services);

            Element service = document.createElement("service");
            services.appendChild(service);

            services.appendChild(createService(document, "org.cryptoworkshop.ximix.crypto.service.NodeKeyRetrievalService"));
            services.appendChild(createService(document, "org.cryptoworkshop.ximix.crypto.service.NodeKeyGenerationService"));
            services.appendChild(createService(document, "org.cryptoworkshop.ximix.crypto.service.NodeSigningService"));

            return rootElement;
        }
        catch (Exception e)
        {
            Assert.fail("can't create config: " + e.getMessage());
            return null;
        }
    }

    private Element createService(Document document, String implementation)
    {
        Element service = document.createElement("service");

        Element implementationNode = document.createElement("implementation");
        implementationNode.appendChild(document.createTextNode(implementation));
        service.appendChild(implementationNode);

        return service;
    }
}