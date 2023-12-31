/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aiplatform;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.assertNotNull;

import com.google.cloud.testing.junit4.MultipleAttemptsRule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class PredictTextPromptSampleTest {

  @Rule public final MultipleAttemptsRule multipleAttemptsRule = new MultipleAttemptsRule(3);

  private static final String PROJECT = System.getenv("UCAIP_PROJECT_ID");
  private static final String INSTANCE =
      "{ \"prompt\": " + "\"Give me ten interview questions for the role of program manager.\"}";
  private static final String PARAMETERS =
      "{\n"
          + "  \"temperature\": 0.2,\n"
          + "  \"maxOutputTokens\": 256,\n"
          + "  \"topP\": 0.95,\n"
          + "  \"topK\": 40\n"
          + "}";
  private static final String PUBLISHER = "google";
  private static final String LOCATION = "us-central1";
  private static final String MODEL = "text-bison@001";

  private ByteArrayOutputStream bout;
  private PrintStream out;
  private PrintStream originalPrintStream;

  private static void requireEnvVar(String varName) {
    String errorMessage =
        String.format("Environment variable '%s' is required to perform these tests.", varName);
    assertNotNull(errorMessage, System.getenv(varName));
  }

  @BeforeClass
  public static void checkRequirements() {
    requireEnvVar("GOOGLE_APPLICATION_CREDENTIALS");
    requireEnvVar("UCAIP_PROJECT_ID");
  }

  @Before
  public void setUp() {
    bout = new ByteArrayOutputStream();
    out = new PrintStream(bout);
    originalPrintStream = System.out;
    System.setOut(out);
  }

  @After
  public void tearDown() {
    System.out.flush();
    System.setOut(originalPrintStream);
  }

  @Test
  public void testPredictTextPrompt() throws IOException {
    // Act
    PredictTextPromptSample.predictTextPrompt(
        INSTANCE, PARAMETERS, PROJECT, LOCATION, PUBLISHER, MODEL);

    // Assert
    String got = bout.toString();
    assertThat(got).contains("Predict Response");
  }
}
