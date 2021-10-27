//
// Copyright 2020-2021 Signal Messenger, LLC.
// SPDX-License-Identifier: AGPL-3.0-only
//

package org.signal.zkgroup.groups;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.UUID;
import org.signal.zkgroup.InvalidInputException;
import org.signal.zkgroup.VerificationFailedException;
import org.signal.client.internal.Native;
import org.signal.zkgroup.profiles.ProfileKey;

import static org.signal.zkgroup.internal.Constants.RANDOM_LENGTH;

public class ClientZkGroupCipher {

  private final GroupSecretParams groupSecretParams;

  public ClientZkGroupCipher(GroupSecretParams groupSecretParams) {
    this.groupSecretParams = groupSecretParams;
  }

  public UuidCiphertext encryptUuid(UUID uuid) {
    byte[] newContents = Native.GroupSecretParams_EncryptUuid(groupSecretParams.getInternalContentsForJNI(), uuid);

    try {
      return new UuidCiphertext(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public UUID decryptUuid(UuidCiphertext uuidCiphertext) throws VerificationFailedException {
     return Native.GroupSecretParams_DecryptUuid(groupSecretParams.getInternalContentsForJNI(), uuidCiphertext.getInternalContentsForJNI());
  }

  public ProfileKeyCiphertext encryptProfileKey(ProfileKey profileKey, UUID uuid) {
     byte[] newContents = Native.GroupSecretParams_EncryptProfileKey(groupSecretParams.getInternalContentsForJNI(), profileKey.getInternalContentsForJNI(), uuid);

    try {
      return new ProfileKeyCiphertext(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public ProfileKey decryptProfileKey(ProfileKeyCiphertext profileKeyCiphertext, UUID uuid) throws VerificationFailedException {
    byte[] newContents = Native.GroupSecretParams_DecryptProfileKey(groupSecretParams.getInternalContentsForJNI(), profileKeyCiphertext.getInternalContentsForJNI(), uuid);

    try {
      return new ProfileKey(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public byte[] encryptBlob(byte[] plaintext) throws VerificationFailedException {
    return encryptBlob(new SecureRandom(), plaintext);
  }

  public byte[] encryptBlob(SecureRandom secureRandom, byte[] plaintext) throws VerificationFailedException {

    byte[] paddedPlaintext = new byte[plaintext.length + 4];
    System.arraycopy(plaintext, 0, paddedPlaintext, 4, plaintext.length);

    byte[] random      = new byte[RANDOM_LENGTH];

    secureRandom.nextBytes(random);

    return Native.GroupSecretParams_EncryptBlobDeterministic(groupSecretParams.getInternalContentsForJNI(), random, paddedPlaintext);
  }

  public byte[] decryptBlob(byte[] blobCiphertext) throws VerificationFailedException {
    byte[] newContents = Native.GroupSecretParams_DecryptBlob(groupSecretParams.getInternalContentsForJNI(), blobCiphertext);

    if (newContents.length < 4) {
        throw new VerificationFailedException();
    }

    byte[] padLenBytes = new byte[4];
    System.arraycopy(newContents, 0, padLenBytes, 0, 4);
    int padLen = ByteBuffer.wrap(newContents).getInt();
    if (newContents.length < (4 + padLen))  {
        throw new VerificationFailedException();
    }

    byte[] depaddedContents = new byte[newContents.length - (4 + padLen)];
    System.arraycopy(newContents, 4, depaddedContents, 0, newContents.length - (4 + padLen));

    return depaddedContents;
  }

}
