package com.amaze.filemanager.services.ssh;

import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.signature.SignatureDSA;
import net.schmizz.sshj.signature.SignatureRSA;
import net.schmizz.sshj.transport.random.JCERandom;
import net.schmizz.sshj.transport.random.SingletonRandomFactory;

import java.security.Security;

/**
 * Borrowed from original AndroidConfig, but also use SpongyCastle from the start altogether.
 *
 * @see net.schmizz.sshj.AndroidConfig
 */
public class CustomSshJConfig extends DefaultConfig
{
    static
    {
        Security.removeProvider("BC");
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), Security.getProviders().length+1);
    }

    // don't add ECDSA
    protected void initSignatureFactories() {
        setSignatureFactories(new SignatureRSA.Factory(), new SignatureDSA.Factory());
    }

    @Override
    protected void initRandomFactory(boolean ignored) {
        setRandomFactory(new SingletonRandomFactory(new JCERandom.Factory()));
    }
}
