/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.ssh;

import java.security.Security;

import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.signature.SignatureDSA;
import net.schmizz.sshj.signature.SignatureRSA;
import net.schmizz.sshj.transport.random.JCERandom;
import net.schmizz.sshj.transport.random.SingletonRandomFactory;

/**
 * sshj {@link net.schmizz.sshj.Config} for our own use.
 *
 * <p>Borrowed from original AndroidConfig, but also use vanilla BouncyCastle from the start
 * altogether.
 *
 * @see net.schmizz.sshj.Config
 * @see net.schmizz.sshj.AndroidConfig
 */
public class CustomSshJConfig extends DefaultConfig {
  // This is where we different from the original AndroidConfig. Found it only work if we remove
  // BouncyCastle bundled with Android before registering our BouncyCastle provider
  public static void init() {
    Security.removeProvider("BC");
    Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 0);
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
