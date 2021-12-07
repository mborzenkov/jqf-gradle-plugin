# Gradle Plugin for JQF

This plugin is based on
[JQF Maven Plugin](https://github.com/rohanpadhye/JQF/wiki/JQF-Maven-Plugin). All Maven dependencies
are replaced with Gradle API.

## Usage

### Add to Gradle project

Add plugin to your project's `build.gradle`:

```
plugins {
    id 'java'
    id 'com.mborzenkov.jqf-gradle-plugin' version '' // replace with latest version
}
```

Add test dependencies:

```
dependencies {
    testImplementation 'com.pholser:junit-quickcheck-generators:0.8'
    testImplementation 'edu.berkeley.cs.jqf:jqf-fuzz:1.7' // replace with latest
}
```

### Write tests

Add a test class annotated with `@RunWith(JQF.class)` and a test method with at least one input and
annotated with `@Fuzz`.

Use this as an example - https://github.com/rohanpadhye/jqf-zest-example

### Run

Run JQF fuzz for your test class (10 seconds example):

```
./gradlew fuzz --class=com.example.YourTestClass --method=yourTestMethod --time=10s
```

There are many configuration options available, see

```
./gradlew help --task fuzz
./gradlew help --task repro
```

or this documentation - https://github.com/rohanpadhye/JQF/wiki/JQF-Maven-Plugin

### Results

By default, all results are saved to build/fuzz-results if not specified otherwise.

### Reproduce

If any errors were discovered during fuzzing, you can reproduce the results and see the exact inputs
with:

```
./gradlew repro --class=com.example.YourTestClass --method=yourTestMethod --input=build/fuzz-results/yourTestClassPath/yourTestMethod/failures/id_000000
```

## Implementation

This section is about JQF Gradle Plugin implementation.

- Plugin is configured in JqfGradlePlugin. It has only two tasks: JqfFuzzTask and JqfReproTask.
- To publish the plugin locally, build it and run `./gradlew publishToMavenLocal`
- To publish the plugin to Gradle Plugins Portal, build it and run `./gradlew publishPlugins`. Note: this action requires secret API keys .

## Improvements

This section is about important parts for the further development of this plugin.

- Update ZestGuidance to print to stdout instead of `System.console`; otherwise, Gradle is not
  showing updates
- There might be an issue with caches that needs some additional settings.
  `--no-daemon` option acts as a temporary solution.

## Collaboration

- JQF Gradle Plugin was developed by [Maksim Borzenkov](https://github.com/mborzenkov) and
  [Ruchi Jawa](https://github.com/RJ0209) as a part of their Master's program at The University of
  British Columbia
- This project is based on [JQF Maven Plugin](https://github.com/rohanpadhye/JQF) developed
  by [Rohan Padhye](https://rohan.padhye.org/) and distributed under BSD-2-clause license.
