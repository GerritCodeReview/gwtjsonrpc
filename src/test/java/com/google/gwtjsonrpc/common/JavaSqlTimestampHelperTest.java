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

package com.google.gwtjsonrpc.common;

import static com.google.gwtjsonrpc.common.JavaSqlTimestampHelper.parseTimestamp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class JavaSqlTimestampHelperTest {
  private SimpleDateFormat format;
  private TimeZone systemTimeZone;

  @Before
  public void setUp() {
    synchronized (TimeZone.class) {
      systemTimeZone = TimeZone.getDefault();
      TimeZone.setDefault(TimeZone.getTimeZone("GMT-5:00"));
      format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
    }
  }

  @After
  public void resetTimeZone() {
    TimeZone.setDefault(systemTimeZone);
  }

  @Test
  public void parseFullTimestamp() {
    assertEquals("2006-01-02 10:04:05.999 -0500",
        reformat("2006-01-02 15:04:05.999000000"));
  }

  @Test
  public void parseInvalidTimestamps() {
    assertInvalid("2006-01-02-15:04:05.999000000");
    assertInvalid("2006-01-02T15:04:05.999000000");
    assertInvalid("2006-01-02");
    assertInvalid("15:04:05");
    assertInvalid("15:04:05.999000000");
    assertInvalid("2006-01-02 10:04:05.999 -0500");
    assertInvalid("2006-01-02 10:04:05.999000000 -0500");
  }

  private static void assertInvalid(String input) {
    try {
      parseTimestamp(input);
      fail("Expected IllegalArgumentException for: " + input);
    } catch (IllegalArgumentException e) {
      // Expected;
    }
  }

  private String reformat(String input) {
    return format.format(parseTimestamp(input));
  }
}
