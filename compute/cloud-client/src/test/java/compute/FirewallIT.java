/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package compute;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.compute.v1.FirewallsClient;
import com.google.cloud.testing.junit4.MultipleAttemptsRule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@Timeout(value = 10, unit = TimeUnit.MINUTES)
public class FirewallIT {

  @Rule
  public final MultipleAttemptsRule multipleAttemptsRule = new MultipleAttemptsRule(3);

  private static final String PROJECT_ID = System.getenv("GOOGLE_CLOUD_PROJECT");
  private static String FIREWALL_RULE_CREATE;
  private static String NETWORK_NAME;

  private ByteArrayOutputStream stdOut;

  // Check if the required environment variables are set.
  public static void requireEnvVar(String envVarName) {
    assertWithMessage(String.format("Missing environment variable '%s' ", envVarName))
        .that(System.getenv(envVarName)).isNotEmpty();
  }

  @BeforeAll
  public static void setUp()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    final PrintStream out = System.out;
    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stdOut));
    requireEnvVar("GOOGLE_APPLICATION_CREDENTIALS");
    requireEnvVar("GOOGLE_CLOUD_PROJECT");

    FIREWALL_RULE_CREATE = "firewall-rule-" + UUID.randomUUID();
    NETWORK_NAME = "global/networks/default";

    compute.CreateFirewallRule.createFirewall(PROJECT_ID, FIREWALL_RULE_CREATE, NETWORK_NAME);
    TimeUnit.SECONDS.sleep(10);

    stdOut.close();
    System.setOut(out);
  }


  @AfterAll
  public static void cleanup()
      throws IOException, InterruptedException, TimeoutException {
    final PrintStream out = System.out;
    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stdOut));
    // Delete all instances created for testing.
    requireEnvVar("GOOGLE_APPLICATION_CREDENTIALS");
    requireEnvVar("GOOGLE_CLOUD_PROJECT");

    try {
      DeleteFirewallRule.deleteFirewallRule(PROJECT_ID, FIREWALL_RULE_CREATE);
    } catch (NotFoundException e) {
      System.out.println("Rule already deleted! ");
    } catch (InvalidArgumentException | NullPointerException | ExecutionException e) {
      System.out.println("Rule is not ready (probably being deleted).");
    }

    stdOut.close();
    System.setOut(out);
  }

  @BeforeEach
  public void beforeEach() {
    stdOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stdOut));
  }

  @AfterEach
  public void afterEach() {
    stdOut = null;
    System.setOut(null);
  }

  @Test
  public void testListFirewallRules()
      throws IOException {
    final PrintStream out = System.out;
    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stdOut));
    try {
      compute.ListFirewallRules.listFirewallRules(PROJECT_ID);
      if (!stdOut.toString().contains(FIREWALL_RULE_CREATE)) {
        throw new NoSuchElementException("Rule already deleted or being deleted.");
      }
    } catch (NoSuchElementException e) {
      System.out.println(e.getMessage());
    }
    // Clear system output to not affect other tests.
    // Refrain from setting out to null.
    stdOut.close();
    System.setOut(out);
  }

  @Test
  public void testPatchFirewallRule()
      throws IOException, InterruptedException, TimeoutException {
    final PrintStream out = System.out;
    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stdOut));

    try (FirewallsClient client = FirewallsClient.create()) {
      Assert.assertEquals(1000, client.get(PROJECT_ID, FIREWALL_RULE_CREATE).getPriority());
      compute.PatchFirewallRule.patchFirewallPriority(PROJECT_ID, FIREWALL_RULE_CREATE, 500);
      TimeUnit.SECONDS.sleep(5);
      Assert.assertEquals(500, client.get(PROJECT_ID, FIREWALL_RULE_CREATE).getPriority());
    } catch (NotFoundException e) {
      /* (**INTERNAL snippet**)
      Firewall rule is auto-deleted by GCE Enforcer within a few minutes.
      Catching exceptions will prevent test failure if the firewall rule was auto-deleted
      by GCE Enforcer.
      (Feel free to remove this method if not running on a Google-owned project.)
      */
      System.out.println("Rule already deleted! ");
    } catch (ExecutionException | InvalidArgumentException | NullPointerException e) {
      System.out.println("Rule is not ready (probably being deleted).");
    }

    // Clear system output to not affect other tests.
    // Refrain from setting out to null as it will throw NullPointer in the subsequent tests.
    stdOut.close();
    System.setOut(out);
  }

}
