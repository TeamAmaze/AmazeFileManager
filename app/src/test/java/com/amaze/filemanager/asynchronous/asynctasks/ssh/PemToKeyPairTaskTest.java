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
public class PemToKeyPairTaskTest {

  private static final String unencryptedPuttyKey =
      "PuTTY-User-Key-File-2: ssh-rsa\n"
          + "Encryption: none\n"
          + "Comment: Key for test only\n"
          + "Public-Lines: 6\n"
          + "AAAAB3NzaC1yc2EAAAABJQAAAQEA6ZhWkS0Xpb1riC5r3dulviIwVFUP4uXnnapv\n"
          + "eqVwB/7aklhu3SnOlBwRMoan+AohhHogy2cjNMqW6x/xLwH9Cbo4kMTeJTPR7ca2\n"
          + "lxmCtGgdFRheR84if6T+i2fb1ADUmncJEkL2H1Q7RG7+opoerDpwdGjopsP7s7H4\n"
          + "ZTvGGXcudhrOFOf/gW8hR8m9wJ05ON8qfKiRWIKDxFpectFOpJC/NGP4F53EHNAk\n"
          + "HIhNPSW5voytGvj4VaS/xRAs2HLmj7jTor/Le/vJlndmnyJkGIwEJbVpp5HsZG7R\n"
          + "VyhqZwCcI6ZiMXvYSH6oplffUGZz5HXkskBmreMauZC1beN31Q==\n"
          + "Private-Lines: 14\n"
          + "AAABAD8iQOj3bizLaSu5hO/af9KFx99w74lukaA75sczobz4xXOpMrhQfQVvXpgI\n"
          + "t8Z/R1Q8rurdms//Zw8dY8eD/zMPvELNbHjB5bXireOmB6ZhU/fdEo/yhd1PMAoA\n"
          + "ZO0wqCm/To9QXjH7Fu/mpa9n7J1AOhGf0C0SX7QGlikyv+s0c4ib+ipR6TEoLRHD\n"
          + "Oa85soSZGjPvPckkfSNncemEWuo4Hp0jiJOU/Gd37YNY4Jc9FhRRuBlTUvhBdg5g\n"
          + "FsvHZD2fZ+5J0Z2Gm9tJL6Uq6rvNWVC9sFlnornPXM9/UvXQ1Q59rIk0CQNEOVr7\n"
          + "kdYpYeUhYrrwhCVQrjbV8CxyRi0AAACBAP1T4/nXa/+tA9f3orMBkxzQF0FnPI/+\n"
          + "e8YXvdHtkHl3/uJEEy9FmliJecKNtNBDN6Tu3iAs5ne/btvyduMgRAxqOyXxdQCq\n"
          + "uR2iNoHLCDqgOUbXCh+swVHPXsdbbhv/54aWjLBbbfZ6S1CwTmPV3eAVJRb78JwS\n"
          + "uBi8Sq/5ZnhFAAAAgQDsDyjp1Bm6nonVwaGCHRsH5JFuiHbP8cyIQzdnC6PrMFvS\n"
          + "EU1PfMKSeudywnEKyljct7Njw5FnMt1InWj1X80adK/gWTmppAPRr0u0ipT4J9ry\n"
          + "1yCj4zeNS/cylZZoP/rOCq8Z2mbzqIO28jN5e7xDMrutNdXhqrOwmMgM0AliUQAA\n"
          + "AIEAjMoM3mw2YXE7U1X2H/hfYymMWC+6XU4XHCI2Fk+CWGUPxvDT3uqUtoEXOXkY\n"
          + "THdPSgA2f6EmqCOPR1VAA4jdQkK8VkN3/O3zWFdfRGqN5Kka7a7cmcyd93sq3LIU\n"
          + "EYe4EYW7BQwe1W5ZCO+lRzjquGAB5rMhdAnzYfvkPc7sfJ8=\n"
          + "Private-MAC: 2cd5ec740c5dd854e8a6bea3773f98697670bdc6";

  // Passphrase = test
  private static final String encryptedPuttyKey =
      "PuTTY-User-Key-File-2: ssh-rsa\n"
          + "Encryption: aes256-cbc\n"
          + "Comment: Key for test only\n"
          + "Public-Lines: 6\n"
          + "AAAAB3NzaC1yc2EAAAABJQAAAQEAoArpfCYeHImHcTELKVzVjyS6N6viAN4lzkWC\n"
          + "EDCyBX6x4wwgVXRYQTbd6xNCpVb/TdBTN/aVF9EXtMW2TXyvntxGblE+ilK8b3GL\n"
          + "zRfxjrjGsjqffwlHn3JaWpCOYtEqgGOLeKkofbKBXGn1aK6tvowPsY89Dl4aK857\n"
          + "mwisAvCIxmd8b6f2aBy4MLQ7AdmZXxPq6YD9CDXPyQkNG4RH5RGAIAw7jD+O6tUo\n"
          + "g69voQudjy3D51VQDGNxOJVojQiQvmRUR2qkSazPJqE/hFsdN6rKh+Pbe8h6z+rU\n"
          + "bym37+sTK5JwWKHDQ7/ezLdNR38wAPHdz2VW5+0rqKm8LAtCGQ==\n"
          + "Private-Lines: 14\n"
          + "erS8mfUDEme+ujzF1k0GA7d2P4umHriMFQjBZIdvht6amZXoF1L+bkJp82/vG9lv\n"
          + "YYNYQqk5tHezkV3sJncPwr3RI/0Y1L+WtKWSfE6OzSKdYJoX3WpAuMTeMlVrxu0t\n"
          + "RXnjbfSz7Z1czryxO4NgAK3NsYQK6h290uq5/mpqP6fIhT3/tn+mH8kihAt8+uum\n"
          + "1RW9ucNi3TZTG4I00Z3LWHw1VaqYFeCYh3yp8Canv3mKGn5ISqsd5ehNXW1TYvJF\n"
          + "Bd742+JlxK8dhrAr2R+g+erSA0ac8Df3wH72syVPzdewnh+21zff3NGI9GWN799X\n"
          + "CnVtf+psDPuebGQIHewNTGsaziNkAT5rGXdNMo3Xln/B1Wr9l8tIJAtDWSNqjDLp\n"
          + "kAcLQ+Z1wTPZehZKBi0oTsLVm4tEcPQsbnuK9h+Y/d9EWcmBEiHTGM6otesNZNA8\n"
          + "i247YZpyrG3azeRFBVMNSzKJ+vS2rKpgvm8nbYKy+nO5uNZDEm9oARr9QPCxdzXK\n"
          + "dmI9F8IT3tLd4qCekD4DI9MKxJLjzFmyGOHc4zMxgUyL5BT5suVDIAWL/hiekwjt\n"
          + "T1+V+TRScq4c+pIWxfVu4kY+HpLUpSR3RAVaRFar7jaB+YEEXw+gqEVTCyZeXGFf\n"
          + "dWU+8BkhFBF24v3Qoi9SmuWYrSQGl9O8smIHW0H2JCNF+8oqpQG8dwx37L3VyMNq\n"
          + "ApJh0LnRhoRwKo2YaAZKInaFTYS8Gnj7DvZ+l7lxPRfCV5yl9U2How2BI9YPRDDu\n"
          + "Gs4agAG3InJnMiuIOzaNOIFLGM9STtYNyvG411rj6tR4EEQ6cJCxIlVe5a1mEt7M\n"
          + "GVfbB5wUvow0o0a56OBmFMZOCxV2Vpxu6PuGTD8QQ0O0YzNDWFk3Fj2RRnnLCBLF\n"
          + "Private-MAC: f742e2954fbb0c98984db0d9855a0f15507ecc0a";

  @Test
  public void testUnencryptedKeyToKeyPair() throws InterruptedException {
    CountDownLatch waiter = new CountDownLatch(1);
    PemToKeyPairTask task =
        new PemToKeyPairTask(
            unencryptedPuttyKey,
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
            encryptedPuttyKey,
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
            return "test".toCharArray();
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
