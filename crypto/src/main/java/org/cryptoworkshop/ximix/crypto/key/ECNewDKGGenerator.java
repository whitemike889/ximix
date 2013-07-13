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
package org.cryptoworkshop.ximix.crypto.key;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.cryptoworkshop.ximix.common.service.KeyType;
import org.cryptoworkshop.ximix.common.service.ThresholdKeyPairGenerator;
import org.cryptoworkshop.ximix.crypto.key.message.ECCommittedSecretShareMessage;
import org.cryptoworkshop.ximix.crypto.key.message.ECKeyGenParams;
import org.cryptoworkshop.ximix.crypto.threshold.ECCommittedSecretShare;
import org.cryptoworkshop.ximix.crypto.threshold.ECCommittedSplitSecret;
import org.cryptoworkshop.ximix.crypto.threshold.ECNewDKGSecretSplitter;

public class ECNewDKGGenerator
    implements ThresholdKeyPairGenerator
{
    private final KeyType algorithm;
    private final ECKeyManager keyManager;

    public ECNewDKGGenerator(KeyType algorithm, ECKeyManager keyManaged)
    {
        this.algorithm = algorithm;
        keyManager = keyManaged;
    }

    public ECCommittedSecretShareMessage[] generateThresholdKey(String keyID, ECKeyGenParams ecKeyGenParams)
    {
        // TODO: should have a source of randomness.
        AsymmetricCipherKeyPair keyPair = keyManager.generateKeyPair(keyID, algorithm, ecKeyGenParams.getNodesToUse().size(), ecKeyGenParams);

        ECPrivateKeyParameters privKey = (ECPrivateKeyParameters) keyPair.getPrivate();
        ECNewDKGSecretSplitter secretSplitter = new ECNewDKGSecretSplitter(ecKeyGenParams.getNodesToUse().size(), ecKeyGenParams.getThreshold(), ecKeyGenParams.getH(), privKey.getParameters(), new SecureRandom());

        ECCommittedSplitSecret splitSecret = secretSplitter.split(privKey.getD());
        ECCommittedSecretShare[] shares = splitSecret.getCommittedShares();
        ECCommittedSecretShareMessage[] messages = new ECCommittedSecretShareMessage[shares.length];

        BigInteger[] aCoefficients = splitSecret.getCoefficients();
        ECPoint[] qCommitments = new ECPoint[aCoefficients.length];

        for (int i = 0; i != qCommitments.length; i++)
        {
            qCommitments[i] = privKey.getParameters().getG().multiply(aCoefficients[i]);
        }

        for (int i = 0; i != shares.length; i++)
        {
            messages[i] = new ECCommittedSecretShareMessage(i, shares[i].getValue(), shares[i].getWitness(), shares[i].getCommitmentFactors(),
                    ((ECPublicKeyParameters) keyPair.getPublic()).getQ(), qCommitments);
        }

        return messages;
    }

    public void storeThresholdKeyShare(String keyID, ECCommittedSecretShareMessage message)
    {
        try
        {
            keyManager.buildSharedKey(keyID, message);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}