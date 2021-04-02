package io.jenkins.plugins.spinnaker;

import hudson.Extension;
import hudson.model.Descriptor.FormException;

import java.net.URL;

import jenkins.model.GlobalConfiguration;

import net.sf.json.JSONObject;

import org.jenkinsci.Symbol;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import org.openapitools.client.ApiClient;
import org.openapitools.client.Configuration;

/**
 * The Config class exposes spinnaker config options in the global Jenkins config page.
 * <p>
 * Spinnaker gate service configuration
 *
 * @author  Thomas Swanson
 * @since   2020-03-21
 */

@Symbol("spinnaker-config")
@Extension
public class Config extends GlobalConfiguration {
  /**
   * The name of the global config form that contains these options.
   */
  public static final String FORM_NAME = "Spinnaker";
  /**
   * Error message to show if basepath url is not valid.
   */
  public static final String BASEPATH_ERROR = "%s is not a valid url...";
  /**
   * The basepath that spinnaker gate is located.
   */
  private String basepath = Configuration.getDefaultApiClient().getBasePath();
  /**
   * Constructs a new Config and loads the class data from disk if available.
   */
  @DataBoundConstructor
  public Config() {
    load();
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(basepath);
    Configuration.setDefaultApiClient(apiClient);
  }
  /**
   * Sets the basepath of the gate api in spinnaker.
   * @param gateBasepath URL location of spin gate service.
   * @exception IllegalArgumentException
   *            The URL is not a valid url
   * @see basepath
   */
  @DataBoundSetter
  public void setBasepath(final String gateBasepath) {
    // Check if the url is valid
    try {
      new URL(gateBasepath).toURI();
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format(BASEPATH_ERROR, gateBasepath));
    }
    // Set value local
    this.basepath = gateBasepath;
    // Update the basepath url and set apiclient as default
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(this.basepath);
    Configuration.setDefaultApiClient(apiClient);
    save();
  }
  /**
   * Gets the basepath from the Configuration.getDefaultApiClient().
   * @return basepath
   */
  public String getBasepath() {
    return Configuration.getDefaultApiClient().getBasePath();
  }
  @Override
  public boolean configure(final StaplerRequest req, final JSONObject json) throws FormException {
    try {
      req.bindJSON(this, json);
    } catch (Exception e) {
      throw new FormException(e.getMessage(), FORM_NAME);
    }
    return true;
  }
}
