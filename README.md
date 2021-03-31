
::: warning
This plugin is still a work in progress.  Would be open to any suggestions or discusions 😸

# Spinnaker
The Spinnaker plugin is an api interface into controlling spinnaker.

## Getting started

### Configuring spin-gate location 
+ Setup the global configuartion for the spin-gate path.

> The spinnaker spin-gate url needs to be set in the jenkins global configuration.  Depending how you deploy spinnaker this will change.  The path set here will be used in all subsequent api calls.
![image](https://user-images.githubusercontent.com/9701912/113105163-2376dd80-91b6-11eb-847a-42da2f9e081d.png)

### pipelineController
+ The pipeline controller build step used to configure and create pipelines in spinnaker.
 
> Pipelines are defined as a pipeline json file.  This pipeline json matches the syntax that spinnaker uses.  The pipeline json must contain the **`name`** and **`application`** field.
>> This pipeline is named bar and defined for an application foo.
>> ```
>>  {
>>    "application":"foo"
>>    "name":"bar"
>>    "keepWaitingPipelines": false,
>>    "limitConcurrent": true,
>>    "spelEvaluator": "v4",
>>    "stages": [],
>>    "triggers": []
>>  }
>> ```
> Buildstep can be invoked with the syntax below.
>> ```pipelineController spinnaker-pipeline.json```



## Issues

Please report issues through github.

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
