package io.jenkins.plugins.spinnaker;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import io.jenkins.plugins.spinnaker.Config;

import java.lang.IllegalArgumentException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import org.openapitools.client.ApiClient;
import org.openapitools.client.Configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ConfigTest {
  @Rule public RestartableJenkinsRule rr = new RestartableJenkinsRule();
  @Rule public JenkinsRule jenkinsRule = new JenkinsRule();

  private static final String VALID_URL = "http://localhost:8081";
  private static final String DEFAULT_URL = "http://localhost";
  private static final String CORRUPT_URL = "|@";

  Config config;

  @Before
  public void init() {
    // Reset the default client
    Configuration.setDefaultApiClient(new ApiClient());
    config = new Config();
  }
  @Test
  public void Constrctor_InitialValue_DefaultURL() {
    assertThat(Configuration.getDefaultApiClient().getBasePath(), equalTo(DEFAULT_URL));
  }
  @Test
  public void SetBasePath_ValidUrl_UpdateDefaultApi() {
    config.setBasepath(VALID_URL);
    assertThat(Configuration.getDefaultApiClient().getBasePath(), equalTo(VALID_URL));
  }
  @Test(expected = IllegalArgumentException.class)
  public void SetBasePath_CorruptUrl_Exception() throws Exception {
    config.setBasepath(CORRUPT_URL);
  }
  @Test(expected = IllegalArgumentException.class)
  public void SetBasePath_NullUrl_Exception() throws Exception {
    config.setBasepath("");
  }
  @Test
  public void SetBasePathThroughHtml_ValidUrl_UpdateDefaultApi() throws Exception {
    rr.then(r -> {
      HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
      HtmlTextInput textbox = config.getInputByName("_.basepath");
      textbox.setText(VALID_URL);
      r.submit(config);
      assertThat(Configuration.getDefaultApiClient().getBasePath(), equalTo(VALID_URL));
    });
    rr.then(r -> {
      assertThat(Configuration.getDefaultApiClient().getBasePath(), equalTo(VALID_URL));
    });
  }
  @Test(expected = FailingHttpStatusCodeException.class)
  public void SetBasePathThroughHtml_CorruptUrl_FormError() throws Exception {
    HtmlForm config = jenkinsRule.createWebClient().goTo("configure").getFormByName("config");
    HtmlTextInput textbox = config.getInputByName("_.basepath");
    textbox.setText(CORRUPT_URL);
    jenkinsRule.submit(config);
  }
}
