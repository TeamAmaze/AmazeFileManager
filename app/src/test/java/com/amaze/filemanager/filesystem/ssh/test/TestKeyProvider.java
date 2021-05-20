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

package com.amaze.filemanager.filesystem.ssh.test;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Collections;

import org.apache.sshd.common.keyprovider.KeyPairProvider;

import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

public class TestKeyProvider implements KeyPairProvider, KeyProvider {

  private KeyPair keyPair;

  public TestKeyProvider() throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(1024, new SecureRandom());
    keyPair = keyPairGenerator.generateKeyPair();
  }

  @Override
  public Iterable<KeyPair> loadKeys() {
    return Collections.singleton(keyPair);
  }

  @Override
  public PrivateKey getPrivate() throws IOException {
    return getKeyPair().getPrivate();
  }

  @Override
  public PublicKey getPublic() throws IOException {
    return getKeyPair().getPublic();
  }

  @Override
  public KeyType getType() throws IOException {
    return KeyType.fromKey(getKeyPair().getPublic());
  }

  public KeyPair getKeyPair() {
    return keyPair;
  }
}
