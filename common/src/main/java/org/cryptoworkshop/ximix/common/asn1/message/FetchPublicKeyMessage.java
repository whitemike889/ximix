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
package org.cryptoworkshop.ximix.common.asn1.message;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERUTF8String;

/**
 * Request message to fetch a public key.
 */
public class FetchPublicKeyMessage
    extends ASN1Object
{
    private final String keyID;

    /**
     * Base constructor.
     *
     * @param keyID the ID of the public key being requested.
     */
    public FetchPublicKeyMessage(String keyID)
    {
        this.keyID = keyID;
    }

    private FetchPublicKeyMessage(DERUTF8String keyID)
    {
        this.keyID = keyID.getString();
    }

    public static final FetchPublicKeyMessage getInstance(Object o)
    {
        if (o instanceof FetchPublicKeyMessage)
        {
            return (FetchPublicKeyMessage)o;
        }
        else if (o != null)
        {
            return new FetchPublicKeyMessage(DERUTF8String.getInstance(o));
        }

        return null;
    }

    @Override
    public ASN1Primitive toASN1Primitive()
    {
        return new DERUTF8String(keyID);
    }

    public String getKeyID()
    {
        return keyID;
    }
}
