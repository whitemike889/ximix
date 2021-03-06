package org.cryptoworkshop.ximix.test.tests;

import junit.framework.TestCase;
import org.cryptoworkshop.ximix.client.KeyGenerationOptions;
import org.cryptoworkshop.ximix.client.KeyGenerationService;
import org.cryptoworkshop.ximix.client.connection.ServiceConnectionException;
import org.cryptoworkshop.ximix.client.connection.XimixRegistrar;
import org.cryptoworkshop.ximix.client.connection.XimixRegistrarFactory;
import org.cryptoworkshop.ximix.common.crypto.Algorithm;
import org.cryptoworkshop.ximix.test.node.ResourceAnchor;
import org.cryptoworkshop.ximix.test.node.TestNotifier;
import org.junit.Test;

/**
 *
 */
public class RegistrarTest
{

    /**
     * Test the correct exception is thrown when the admin service cannot find a node.
     *
     * @throws Exception
     */
    @Test
    public void testRegistrarWithNoStart()
        throws Exception
    {

        XimixRegistrar adminRegistrar = XimixRegistrarFactory.createAdminServiceRegistrar(ResourceAnchor.load("/conf/mixnet.xml"), new TestNotifier());
        KeyGenerationService keyGenerationService = adminRegistrar.connect(KeyGenerationService.class);
        try
        {

            KeyGenerationOptions keyGenOptions = new KeyGenerationOptions.Builder(Algorithm.EC_ELGAMAL, "secp256r1")
                .withThreshold(2)
                .withNodes("A", "B")
                .build();
            byte[] encPubKey = keyGenerationService.generatePublicKey("ECKEY", keyGenOptions);

            TestCase.fail();
        }
        catch (ServiceConnectionException rse)
        {
            TestCase.assertTrue(true);
        }

        keyGenerationService.shutdown();


    }
}
