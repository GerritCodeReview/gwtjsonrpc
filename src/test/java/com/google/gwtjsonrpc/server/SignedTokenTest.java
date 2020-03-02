package com.google.gwtjsonrpc.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SignedTokenTest {

  private int maxAge;
  private String text;

  @Before
  public void setUp() throws Exception {
    maxAge = 5;
    text = "This is a text";
  }

  @Test
  public void checkTokenTest() {
    try {
      SignedToken signedToken = new SignedToken(maxAge);
      String token = signedToken.newToken(text);
      System.out.println(token);
      ValidToken validToken = signedToken.checkToken(token, text);
      Assert.assertNotNull(validToken);
    } catch (XsrfException e) {
      e.printStackTrace();
      Assert.fail(String.format("should not throw XsrfException!error:%s", e.getMessage()));
    }
  }

  @Test
  public void CheckTokenUrlSafeTest() {
    try {
      SignedToken signedToken = new SignedToken(maxAge);
      String token = signedToken.newToken(text, true);
      ValidToken validToken = signedToken.checkToken(token, text, true);
      Assert.assertNotNull(validToken);
    } catch (XsrfException e) {
      e.printStackTrace();
      Assert.fail(String.format("should not throw XsrfException!error:%s", e.getMessage()));
    }
  }
}
