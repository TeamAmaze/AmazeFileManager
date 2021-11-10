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

package com.amaze.filemanager.asynchronous.asynctasks.ssh;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;

@RunWith(AndroidJUnit4.class)
public class PemToKeyPairTaskTest2 {

  // public key for authorized_keys: ssh-ed25519
  // AAAAC3NzaC1lZDI1NTE5AAAAIGxJHFewxU9tJn9hUq9e2C/+ELFw83NpmJ5NLFOzU7O3 test-openssh-key
  private static final String unencryptedOpenSshKey =
      "-----BEGIN OPENSSH PRIVATE KEY-----\n"
          + "b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW\n"
          + "QyNTUxOQAAACBsSRxXsMVPbSZ/YVKvXtgv/hCxcPNzaZieTSxTs1OztwAAAJhX2WUxV9ll\n"
          + "MQAAAAtzc2gtZWQyNTUxOQAAACBsSRxXsMVPbSZ/YVKvXtgv/hCxcPNzaZieTSxTs1Oztw\n"
          + "AAAECjSjwwMXPzbZWq/EBoA4HA9Lr7B1/Tw78K+k1zqAJwA2xJHFewxU9tJn9hUq9e2C/+\n"
          + "ELFw83NpmJ5NLFOzU7O3AAAADmFpcndhdmVAaHN2MDEwAQIDBAUGBw==\n"
          + "-----END OPENSSH PRIVATE KEY-----";

  // Passphrase = 12345678
  // public key for authorized_keys: ssh-ed25519
  // AAAAC3NzaC1lZDI1NTE5AAAAIHio1/33U0XoewL1qGLmTzxyVNeYP5b0Tunv/SQrQi92 test-openssh-key
  private static final String encryptedOpenSshKey =
      "-----BEGIN OPENSSH PRIVATE KEY-----\n"
          + "b3BlbnNzaC1rZXktdjEAAAAACmFlczI1Ni1jdHIAAAAGYmNyeXB0AAAAGAAAABCwlfECA9\n"
          + "+EGLwKVApTmomnAAAAZAAAAAEAAAAzAAAAC3NzaC1lZDI1NTE5AAAAIHio1/33U0XoewL1\n"
          + "qGLmTzxyVNeYP5b0Tunv/SQrQi92AAAAoD2dysYInLaJgXIv6k/xFv7OblU9vWkCwcYnDW\n"
          + "8Zj5+ke8QL2/r7EUBEvY1H02GenlEH1Ufct8ce7/eAWwd7aWukaSQXlKW9IBt5YrxW8+P/\n"
          + "wrHcd/Z92eQ0E7NV6b6LnghGYlyCjpSBW+mxa0AAYPD21c95d/HvJF6zxQl/IKCCLdOrr/\n"
          + "ilMCSIGQEdg71hA3MMZsRbUvazsnZTZXD9PLI=\n"
          + "-----END OPENSSH PRIVATE KEY-----";

  @Test
  public void testUnencryptedKeyToKeyPair() throws InterruptedException {
    CountDownLatch waiter = new CountDownLatch(1);
    PemToKeyPairTask task =
        new PemToKeyPairTask(
            unencryptedOpenSshKey,
            result -> {
              assertNotNull(result);
              assertNotNull(result.getPublic());
              assertNotNull(result.getPrivate());
              waiter.countDown();
            });
    task.execute();
    waiter.await();
  }

  @Test
  public void testEncryptedKeyToKeyPair()
      throws InterruptedException, NoSuchFieldException, IllegalAccessException {
    CountDownLatch waiter = new CountDownLatch(1);
    PemToKeyPairTask task =
        new PemToKeyPairTask(
            encryptedOpenSshKey,
            result -> {
              assertNotNull(result);
              assertNotNull(result.getPublic());
              assertNotNull(result.getPrivate());
              waiter.countDown();
            });
    Field field = PemToKeyPairTask.class.getDeclaredField("passwordFinder");
    field.setAccessible(true);
    field.set(
        task,
        new PasswordFinder() {
          @Override
          public char[] reqPassword(Resource<?> resource) {
            return "12345678".toCharArray();
          }

          @Override
          public boolean shouldRetry(Resource<?> resource) {
            return false;
          }
        });
    task.execute();
    waiter.await();
  }
}
