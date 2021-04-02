
::: warning
This plugin is still a work in progress.  Would be open to any suggestions or discussion 😸

# Spinnaker
The Spinnaker plugin is an api interface used for controlling spinnaker.

## Getting Started

### Configuring spin-gate location 
+ Setup the global configuartion for the spin-gate path.

> The spinnaker spin-gate url needs to be set in the jenkins global configuration.  Depending how you deploy spinnaker this will change.  The path set here will be used in all subsequent api calls.
![image](https://user-images.githubusercontent.com/9701912/113105163-2376dd80-91b6-11eb-847a-42da2f9e081d.png)

### pipelineController
+ The pipeline controller build step is used to configure and create pipelines in spinnaker.
 
> Pipelines are defined as a pipeline json file.  This pipeline json file matches the syntax that spinnaker uses inside spin-deck.  The pipeline json must contain the **`name`** and **`application`** keys.
> 
>>  **For example:**
>>> This pipeline is named bar and defined for an application foo.
>>> ```
>>>  {
>>>   "application":"foo"
>>>    "name":"bar"
>>>    "keepWaitingPipelines": false,
>>>    "limitConcurrent": true,
>>>    "spelEvaluator": "v4",
>>>    "stages": [],
>>>    "triggers": []
>>>  }
>>> ```
> Buildstep can be invoked with the syntax below.
>> ```pipelineController spinnaker-pipeline.json```

## For Developers
The project has multiple modules.

+ **spinnaker-generator**
  > Top level wrapper module that contains the maven source for generating api inside spinnaker-parent.
+ **spinnaker-parent**
  > Parent of multi-module maven project.
  + **api** *(generated)*
    > [openapi](https://openapi-generator.tech/) generated client for spinnaker.
  + **spinnaker**
    > Contains source for jenkins plugins.
    + **Plugins**
      + **PipelineController** [Builder](https://javadoc.jenkins.io/hudson/tasks/Builder.html)
        > Update and create pipelines for an application.
      + **Config** [GlobalConfiguration](https://javadoc.jenkins.io/jenkins/model/GlobalConfiguration.html)
        > Global spinnaker configuration
## Building and running

At a minimum you will need a JVM and Maven installed.

### Build everything (from root directory)
Generates and Builds all maven modules (run this the first time you check things out, must be run at least once)

```
$ mvn clean install
```

### Running spinnaker-plugin

```
$ mvn -f spinnaker-parent/spinnaker/pom.xml hpi:run
```
Then open http://localhost:8080/jenkins/

### Adding features
Once the **api** is generated `mvn test` can be run inside the **spinnaker-parent** module.
This will build and test the **api** and **spinnaker** modules without regenerating sources. 


## Issues

Please report issues through github.

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

**If you write code without:**
  - Tests
  - Comments
  - Pride
   
These projects would be more suitable for you
  - [python-code-disasters](https://github.com/sobolevn/python-code-disasters)
  - [spaghetti](https://www.allrecipes.com/recipe/158140/spaghetti-sauce-with-ground-beef/)
  - [cheese-quest](https://github.com/EvanQuan/cheese-quest)
  - [windows 8](https://en.wikipedia.org/wiki/Windows_8)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
