package io.jenkins.plugins.spinnaker;

import com.google.gson.Gson;

import org.openapitools.client.ApiException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;

import org.kohsuke.stapler.DataBoundConstructor;

import org.openapitools.client.api.ApplicationControllerApi;
import org.openapitools.client.api.PipelineControllerApi;

/**
 * The PipelineController class exposes a way to version control pipelines using pipeline json.
 * The plugin will update or create a new pipeline for an application based on pipeline json.
 * <p>
 * Spinnaker pipeline config controller.
 *
 * @author  Thomas Swanson
 * @since   2020-03-27
 */

public class PipelineController extends Builder implements SimpleBuildStep {

  // messages static final so we can assert when testing the jenkins plugin "Not trying to be a C programmer I promise"
  // #define MISSI... jk ( ͡° ͜ʖ ͡°)
  /**
   * Error message if key is missing from pipeline.
   */
  public static final String MISSING_KEY_ERROR = "missing key: %s";
  /**
   * Error message for failed pipeline def parsing.
   */
  public static final String JSON_PARSER_ERROR = "failed to parse %s:\n%s";
  /**
   * Message successful for pipeline def parsing.
   */
  public static final String JSON_PARSER_FINAL = "reading in json file with def:\n%s";
  /**
   * Error message for missing application.
   */
  public static final String MISSING_APPICALTION = "cannot find application %s";
  /**
   * Message for pipeline id found.
   */
  public static final String FOUND_PIPELINE_ID = "found pipeline with id: %s";
  /**
   * Error message for failed to update pipeline.
   */
  public static final String FAILED_UPDATE_PIPELINE = "failed to update pipeline on spinnaker check spinnaker, %s";
  /**
   * Message for successful pipeline updated.
   */
  public static final String UPDATED_PIPELINE = "updated pipeline";


  /**
   * List fields to check exist in the pipeline json.
   */
  public static final List<String> REQUIRED_PIPELINE_KEYS = Collections.unmodifiableList(
                                            new ArrayList<String>() {{
                                              add("application");
                                              add("name");
                                            }});
  /**
   * Absolute path of the json with the spinnaker pipeline declaration.
   */
  private String pipelineJsonPath;

  /**
   * Constructor that takes a path to a spinnaker pipeline def.
   * The path wil be saved in a member variable.
   * Pipeline def needs to contain a defined 'id' and 'name' field.
   * Json will loaded during perform
   * @param pipelineJsonPath spinnaker pipeline json def file location.
   * @see perform
   */
  @DataBoundConstructor
  public PipelineController(final String pipelineJsonPath) {
    this.pipelineJsonPath = pipelineJsonPath;
  }
  /**
   * Get the member {@link PipelineController#pipelineJsonPath}.
   *@return pipelineJsonPath member.
   */
  public final String getPipelineJsonPath() {
    return pipelineJsonPath;
  }
  /**
   * Function to check if the pipeline contains required fields.
   * The fields checked can be found in {@link PipelineController#REQUIRED_PIPELINE_KEYS}
   * @param pipeline spinnaker pipeline to check
   * @throws IllegalArgumentException if the pipeline is missing any field in {@link PipelineController#REQUIRED_PIPELINE_KEYS}
   * @see REQUIRED_PIPELINE_KEYS
   */
  public static void checkRequiredPipelineParams(final Map<String, Object> pipeline) throws IllegalArgumentException {
    for (final String key : REQUIRED_PIPELINE_KEYS) {
      if (!pipeline.containsKey(key)) {
        // Missing key
        throw new IllegalArgumentException(String.format(MISSING_KEY_ERROR, key));
      }
    }
    // All keys exists
  }
  /**
   * Save the pipeline config to spinnaker.
   * Parses a pipelineJsonPath file for application name and pipeline name.
   * Gets a the pipeline id if exists and updates pipeline.
   * If pipeline does not exists creates a new pipeline for the application.
   * @throws IllegalArgumentException if {@link PipelineController#pipelineJsonPath} cannot be parsed
   * @throws RuntimeException if pipeline fails to update in spinnaker
   */
  @Override
  public void perform(final Run<?, ?> run, final FilePath workspace, final Launcher launcher, final TaskListener listener) throws IllegalArgumentException, RuntimeException {
    // load the pipleine into gson object
    Map<String, Object> pipeline = new HashMap<String, Object>();
    try {
      Gson gson = new Gson();
      pipeline = (Map<String, Object>) gson.fromJson(new BufferedReader(new FileReader(pipelineJsonPath)), pipeline.getClass());
      checkRequiredPipelineParams(pipeline);
    } catch (Exception e) {
      final String error = String.format(JSON_PARSER_ERROR, pipelineJsonPath, e.getMessage());
      listener.getLogger().println(error);
      throw new IllegalArgumentException(error);
    }
    listener.getLogger().println(String.format(JSON_PARSER_FINAL, pipeline.toString()));
    // Create pipeline on spinnaker
    try {
      // Get the pipeline ID
      final Optional<String> pipelineId = getPipelineID((String) pipeline.get("application"), (String) pipeline.get("name"));
      // Add pipeline id to the pipeline if exist.  If it doesnt exist spinnaker will create a new api.
      if (pipelineId.isPresent()) {
        listener.getLogger().println(String.format(FOUND_PIPELINE_ID, pipelineId.get()));
        pipeline.put("id", pipelineId.get());
      }
      // Save the pipeline
      final PipelineControllerApi pipelineControllerApi = new PipelineControllerApi();
      pipelineControllerApi.savePipelineUsingPOST(pipeline, false);
      listener.getLogger().println(String.format(UPDATED_PIPELINE));
    } catch (Exception e) {
      throw new RuntimeException(String.format(FAILED_UPDATE_PIPELINE, e.getMessage()));
    }
  }
  /**
   * Get the pipeline id from application and pipeline name.
   * @param applicationName name of the application pipeline is stored
   * @param pipelineName name of the pipeline to get pipeline id.
   * @return the pipeline id if found otherwise empty.
   * @throws RuntimeException if application configs not accessible.
   */
  public static Optional<String> getPipelineID(final String applicationName, final String pipelineName) throws RuntimeException, ApiException {
    final ApplicationControllerApi applicationControllerApi = new ApplicationControllerApi();
    try {
      // Get all pipelines for the application
      List<Object> pipelines = applicationControllerApi.getPipelineConfigsForApplicationUsingGET(applicationName);
      for (final Object pipeline : pipelines) {
        Map<String, Object> pipelineDef = (Map<String, Object>) pipeline;
        // If name exist and than check pipelineName equals current pipeline
        if (pipelineDef.containsKey("name") && pipelineName.equals((String) pipelineDef.get("name"))) {
          return Optional.of((String) pipelineDef.get("id"));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(String.format(MISSING_APPICALTION, applicationName));
    }
    return Optional.empty();
  }
  @Symbol("pipelineController")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    @Override
    public String getDisplayName() {
      return "pipeline controller";
    }
    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> t) {
      return true;
    }
  }
}
