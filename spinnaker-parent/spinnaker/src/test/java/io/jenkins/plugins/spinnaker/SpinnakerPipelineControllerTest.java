package io.jenkins.plugins.spinnaker;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

import static org.hamcrest.CoreMatchers.is;

import io.jenkins.plugins.spinnaker.PipelineController;

import java.lang.IllegalArgumentException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jvnet.hudson.test.JenkinsRule;

import org.openapitools.client.ApiException;
import org.openapitools.client.api.ApplicationControllerApi;
import org.openapitools.client.api.PipelineControllerApi;

import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ApplicationControllerApi.class, PipelineController.class, PipelineControllerApi.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*"})
public class SpinnakerPipelineControllerTest {
  @Rule public JenkinsRule jenkins = new JenkinsRule();
  public static final String CORRUPT_FILEPATH = "src/test/resources/corrupt.json";
  public static final String VALID_FILEPATH = "src/test/resources/valid.json";
  public final Map<String, String> EXIST_PIPELINE = new HashMap<String, String>() {{
        put("name","exist");
        put("id", "1234");
  }};
  public final Map<String, String> NOT_EXIST_PIPELINE = new HashMap<String, String>() {{
        put("name","not");
        put("id", "1234");
  }};
  // Static function tests
  @Test(expected = IllegalArgumentException.class)
  public void CheckJsonKeys_MissingKey_Exception() throws Exception {
    final Map<String,Object> pipeline = new HashMap<String, Object>();
    PipelineController.checkRequiredPipelineParams(pipeline);
  }
  @Test(expected = Test.None.class)
  public void CheckJsonKeys_AllKeys_NoException() {
    final Map<String,Object> pipeline = new HashMap<String, Object>();
    pipeline.put("application", "applicaition");
    pipeline.put("name", "leeroyJenkins");
    PipelineController.checkRequiredPipelineParams(pipeline);
  }
  @Test(expected = RuntimeException.class)
  public void GetPipelineId_NoApplication_Exception() throws Exception {
    final ApplicationControllerApi applicationControllerApi = PowerMockito.mock(ApplicationControllerApi.class);
    PowerMockito.whenNew(ApplicationControllerApi.class).withNoArguments().thenReturn(applicationControllerApi);
    PowerMockito.when(applicationControllerApi.getPipelineConfigsForApplicationUsingGET(anyString())).thenThrow(ApiException.class);
    PipelineController.getPipelineID("not", "exist");
  }
  @Test 
  public void GetPipelineId_NoConfigExist_EmptyOptional() throws Exception {
    final ApplicationControllerApi applicationControllerApi = PowerMockito.mock(ApplicationControllerApi.class);
    PowerMockito.whenNew(ApplicationControllerApi.class).withNoArguments().thenReturn(applicationControllerApi);
    PowerMockito.when(applicationControllerApi.getPipelineConfigsForApplicationUsingGET(anyString())).thenReturn(Collections.emptyList());
    assertThat(PipelineController.getPipelineID("exist", "not").isPresent(), is(false));
  }
  @Test                                                                                                                                          
  public void GetPipelineId_WrongConfigExist_EmptyOptional() throws Exception {
    final ApplicationControllerApi applicationControllerApi = PowerMockito.mock(ApplicationControllerApi.class);                                  
    PowerMockito.whenNew(ApplicationControllerApi.class).withNoArguments().thenReturn(applicationControllerApi);
    PowerMockito.when(applicationControllerApi.getPipelineConfigsForApplicationUsingGET(anyString())).thenReturn(Arrays.asList(NOT_EXIST_PIPELINE));
    assertFalse(PipelineController.getPipelineID("exist", "exist").isPresent());
  }
  @Test 
  public void GetPipelineId_ConfigExist_PipelineId() throws Exception {
    final ApplicationControllerApi applicationControllerApi = PowerMockito.mock(ApplicationControllerApi.class);                                  
    PowerMockito.whenNew(ApplicationControllerApi.class).withNoArguments().thenReturn(applicationControllerApi);
    PowerMockito.when(applicationControllerApi.getPipelineConfigsForApplicationUsingGET(anyString())).thenReturn(Arrays.asList(EXIST_PIPELINE));
    assertTrue(PipelineController.getPipelineID("exist", "exist").isPresent());
    assertThat(PipelineController.getPipelineID("exist", "exist").get(), is(EXIST_PIPELINE.get("id")));
  }
  // Build step tests
  @Test
  public void Buildstep_InvalidJson_Exception() throws Exception {
    final FreeStyleProject project = jenkins.createFreeStyleProject();
    final PipelineController builder = new PipelineController(CORRUPT_FILEPATH);
    project.getBuildersList().add(builder);
    final FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));
    // As we dont want to be dependent on Gson changes for this unit test we will set the second param to null...
    jenkins.assertLogContains(String.format(PipelineController.JSON_PARSER_ERROR, CORRUPT_FILEPATH, ""), build);
  }
  @Test
  public void Buildstep_InvalidApplication_Exception() throws Exception {
    final ApplicationControllerApi applicationControllerApi = PowerMockito.mock(ApplicationControllerApi.class);
    PowerMockito.whenNew(ApplicationControllerApi.class).withNoArguments().thenReturn(applicationControllerApi);
    PowerMockito.when(applicationControllerApi.getPipelineConfigsForApplicationUsingGET(anyString())).thenThrow(ApiException.class);
    final FreeStyleProject project = jenkins.createFreeStyleProject();
    final PipelineController builder = new PipelineController(VALID_FILEPATH);
    project.getBuildersList().add(builder);
    final FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));
    jenkins.assertLogContains(String.format(PipelineController.FAILED_UPDATE_PIPELINE, String.format(PipelineController.MISSING_APPICALTION, "exist")), build);
  }
  @Test
  public void Buildstep_ConfigNotExist_CreatePipeline() throws Exception {
    final ApplicationControllerApi applicationControllerApi = PowerMockito.mock(ApplicationControllerApi.class);                                  
    PowerMockito.whenNew(ApplicationControllerApi.class).withNoArguments().thenReturn(applicationControllerApi);
    PowerMockito.when(applicationControllerApi.getPipelineConfigsForApplicationUsingGET(anyString())).thenReturn(Arrays.asList(NOT_EXIST_PIPELINE));
    final PipelineControllerApi pipelineControllerApi = PowerMockito.mock(PipelineControllerApi.class);                                  
    PowerMockito.whenNew(PipelineControllerApi.class).withNoArguments().thenReturn(pipelineControllerApi);
    PowerMockito.doNothing().when(pipelineControllerApi).savePipelineUsingPOST(any(), any());
    final FreeStyleProject project = jenkins.createFreeStyleProject();
    final PipelineController builder = new PipelineController(VALID_FILEPATH);
    project.getBuildersList().add(builder);
    final FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));
    jenkins.assertLogContains(PipelineController.UPDATED_PIPELINE, build);
  }
  @Test
  public void Buildstep_ConfigExist_CreatePipeline() throws Exception {
    final ApplicationControllerApi applicationControllerApi = PowerMockito.mock(ApplicationControllerApi.class);                                  
    PowerMockito.whenNew(ApplicationControllerApi.class).withNoArguments().thenReturn(applicationControllerApi);
    PowerMockito.when(applicationControllerApi.getPipelineConfigsForApplicationUsingGET(anyString())).thenReturn(Arrays.asList(EXIST_PIPELINE));
    final PipelineControllerApi pipelineControllerApi = PowerMockito.mock(PipelineControllerApi.class);
    PowerMockito.whenNew(PipelineControllerApi.class).withNoArguments().thenReturn(pipelineControllerApi);
    PowerMockito.doNothing().when(pipelineControllerApi).savePipelineUsingPOST(any(), any());
    final FreeStyleProject project = jenkins.createFreeStyleProject();
    final PipelineController builder = new PipelineController(VALID_FILEPATH);
    project.getBuildersList().add(builder);
    final FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));
    jenkins.assertLogContains(PipelineController.UPDATED_PIPELINE, build);
  }
}
