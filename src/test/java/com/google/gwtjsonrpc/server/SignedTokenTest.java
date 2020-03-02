// Copyright (C) 2014 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gwtjsonrpc.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.gwtjsonrpc.common.CheckTokenException;
import org.junit.Before;
import org.junit.Test;

public class SignedTokenTest {

  private static final int maxAge = 5;
  private static final String TEXT = "This is a text";
  private static final String FORGED_TEXT = "This is a forged text";
  private static final String FORGED_TOKEN = String.format("Zm9yZ2VkJTIwa2V5$%s", TEXT);

  private SignedToken signedToken;

  @Before
  public void setUp() throws Exception {
    signedToken = new SignedToken(maxAge);
  }

  /**
   * Test check token (BASE64 encoding and decoding in a safe URL way)
   *
   * @throws XsrfException
   * @throws CheckTokenException
   */
  @Test
  public void checkTokenTest() throws XsrfException, CheckTokenException {
    String token = signedToken.newToken(TEXT, true);
    ValidToken validToken = signedToken.checkToken(token, TEXT, true);
    assertNotNull(validToken);
    assertEquals(TEXT, validToken.getData());
  }

  /**
   * Test check token: input token is null
   *
   * @throws XsrfException
   * @throws CheckTokenException
   */
  @Test(expected = CheckTokenException.class)
  public void checkTokenInputTokenNullTest() throws XsrfException, CheckTokenException {
    String token = signedToken.newToken(TEXT, true);
    signedToken.checkToken(null, TEXT, true);
  }

  /**
   * Test check token: input token is empty
   *
   * @throws XsrfException
   * @throws CheckTokenException
   */
  @Test(expected = CheckTokenException.class)
  public void checkTokenInputTokenEmptyTest() throws XsrfException, CheckTokenException {
    String token = signedToken.newToken(TEXT, true);
    signedToken.checkToken("", TEXT, true);
  }

  /**
   * Test check token: token is not illegal with no '$' character
   *
   * @throws XsrfException
   * @throws CheckTokenException
   */
  @Test(expected = CheckTokenException.class)
  public void checkTokenInputTokenNoDollarSplitorTest() throws XsrfException, CheckTokenException {
    String token = signedToken.newToken(TEXT, true);
    token = token.replace("$", "¥");
    signedToken.checkToken(token, TEXT, true);
  }

  /**
   * Test check token: token is not illegal with BASE64 decoding error
   *
   * @throws XsrfException
   * @throws CheckTokenException
   */
  @Test(expected = CheckTokenException.class)
  public void checkTokenInputTokenKeyBase64DecodeFailTest()
      throws XsrfException, CheckTokenException {
    String token = signedToken.newToken(TEXT, true);
    token = "A" + token;
    signedToken.checkToken(token, TEXT, true);
  }

  /**
   * Test check token: token is not illegal with a forged key
   *
   * @throws XsrfException
   * @throws CheckTokenException
   */
  @Test(expected = CheckTokenException.class)
  public void checkTokenForgedKeyTest() throws XsrfException, CheckTokenException {
    String token = signedToken.newToken(TEXT, true);
    signedToken.checkToken(FORGED_TOKEN, TEXT, true);
  }

  /**
   * Test check token: token is not illegal with a forged text
   *
   * @throws XsrfException
   * @throws CheckTokenException
   */
  @Test(expected = CheckTokenException.class)
  public void checkTokenForgedTextTest()
      throws XsrfException, CheckTokenException, InterruptedException {
    String token = signedToken.newToken(TEXT, true);
    signedToken.checkToken(token, FORGED_TEXT, true);
  }
}
