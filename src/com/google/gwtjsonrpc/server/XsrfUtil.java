// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gwtjsonrpc.server;

import com.google.gwtjsonrpc.client.Shared;

import org.apache.commons.codec.binary.Base64;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility function to compute and verify XSRF tokens.
 * <p>
 * {@link JsonServlet} uses this class to verify tokens appearing in the custom
 * {@link Shared#XSRF_HEADER} HTTP header. The tokens protect against cross-site
 * request forgery by depending upon the browser's security model. The classic
 * browser security model prohibits a script from site A from reading any data
 * received from site B. By sending unforgeable tokens from the server and
 * asking the client to return them to us, the client script must have had read
 * access to the token at some point and is therefore also from our server.
 */
public class XsrfUtil {
  private static final int INT_SZ = 4;
  private static final int MAX_XSRF_WINDOW = 4 * 60 * 60; // seconds
  private static final String MAC_ALG = "HmacSHA1";

  /**
   * Generate a random key for use with the XSRF library.
   * 
   * @return a new private key, base 64 encoded.
   */
  public static String generateRandomKey() {
    final byte[] r = new byte[26];
    new SecureRandom().nextBytes(r);
    return encodeBase64(r);
  }

  private final SecretKeySpec key;
  private final int tokenLength;

  /**
   * Create a new utility, using a randomly generated key.
   * 
   * @throws XsrfException the JVM doesn't support the necessary algorithms.
   */
  public XsrfUtil() throws XsrfException {
    this(generateRandomKey());
  }

  /**
   * Create a new utility, using the specific key.
   * 
   * @param keyBase64 base 64 encoded representation of the key.
   * @throws XsrfException the JVM doesn't support the necessary algorithms.
   */
  public XsrfUtil(final String keyBase64) throws XsrfException {
    key = new SecretKeySpec(decodeBase64(keyBase64), MAC_ALG);
    tokenLength = INT_SZ + newMac().getMacLength();
  }

  /**
   * Create a new token for the user and the resource.
   * 
   * @param user name or other unique identification of the user.
   * @param resource resource (e.g. servlet path) the token protects.
   * @return a new token string, typically base 64 encoded.
   * @throws XsrfException the JVM doesn't support the necessary algorithms to
   *         generate a token. XSRF services are simply not available.
   */
  public String newToken(final String user, final String resource)
      throws XsrfException {
    final byte[] buf = new byte[tokenLength];
    encodeInt(buf, now());
    computeToken(buf, user, resource);
    return encodeBase64(buf);
  }

  /**
   * Validate a returned token.
   * 
   * @param tokenString a token string previously created by this class.
   * @param user name or other unique identification of the user.
   * @param resource resource (e.g. servlet path) the token protects.
   * @return true if the token is valid; false if the token is null, the empty
   *         string, has expired, does not match the user and resource
   *         combination supplied, or is a forged token.
   * @throws XsrfException the JVM doesn't support the necessary algorithms to
   *         generate a token. XSRF services are simply not available.
   */
  public boolean checkToken(final String tokenString, final String user,
      final String resource) throws XsrfException {
    if (tokenString == null || tokenString.length() == 0) {
      return false;
    }

    final byte[] in = decodeBase64(tokenString);
    if (in.length != tokenLength) {
      return false;
    }

    if (Math.abs(decodeInt(in) - now()) > MAX_XSRF_WINDOW) {
      return false;
    }

    final byte[] gen = new byte[tokenLength];
    System.arraycopy(in, 0, gen, 0, INT_SZ);
    computeToken(gen, user, resource);
    return Arrays.equals(gen, in);
  }

  private void computeToken(final byte[] buf, final String user,
      final String resource) throws XsrfException {
    final Mac m = newMac();

    m.update(buf, 0, INT_SZ);

    m.update((byte) ':');
    if (user != null) {
      m.update(toBytes(user));
    }

    m.update((byte) ':');
    if (resource != null) {
      m.update(toBytes(resource));
    }

    try {
      m.doFinal(buf, INT_SZ);
    } catch (ShortBufferException e) {
      throw new XsrfException("Unexpected token overflow", e);
    }
  }

  private Mac newMac() throws XsrfException {
    try {
      final Mac m = Mac.getInstance(MAC_ALG);
      m.init(key);
      return m;
    } catch (NoSuchAlgorithmException e) {
      throw new XsrfException(MAC_ALG + " not supported", e);
    } catch (InvalidKeyException e) {
      throw new XsrfException("Invalid private key", e);
    }
  }

  private static int now() {
    return (int) (System.currentTimeMillis() / 1000L);
  }

  private static byte[] decodeBase64(final String s) {
    return Base64.decodeBase64(toBytes(s));
  }

  private static String encodeBase64(final byte[] buf) {
    return toString(Base64.encodeBase64(buf));
  }

  private static void encodeInt(final byte[] buf, int v) {
    buf[3] = (byte) v;
    v >>>= 8;

    buf[2] = (byte) v;
    v >>>= 8;

    buf[1] = (byte) v;
    v >>>= 8;

    buf[0] = (byte) v;
  }

  private static int decodeInt(final byte[] buf) {
    int r = buf[0] << 8;

    r |= buf[1] & 0xff;
    r <<= 8;

    r |= buf[2] & 0xff;
    return (r << 8) | (buf[3] & 0xff);
  }

  private static byte[] toBytes(final String s) {
    final byte[] r = new byte[s.length()];
    for (int k = r.length - 1; k >= 0; k--) {
      r[k] = (byte) s.charAt(k);
    }
    return r;
  }

  private static String toString(final byte[] b) {
    final StringBuilder r = new StringBuilder(b.length);
    for (int i = 0; i < b.length; i++) {
      r.append((char) b[i]);
    }
    return r.toString();
  }
}
