# Java CPAS Library [![Build Status](https://travis-ci.org/agent6262/Java-CPAS-Library.svg?branch=master)](https://travis-ci.org/agent6262/Java-CPAS-Library) [![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

Java Cross-Platform Admin Service Library is designed to provides easy access to the CPAS REST API. The Java CPAS 
Library is completely asynchronous by nature because of the interactions with the REST api. However if needed it is 
possible to hold a thread for the api response there by making it synchronous.

## Usage
**Standard Example** *(asynchronous)***:** Creates a new thread for the callback to execute on.
```
Cpas.getInstance().function(params, callBack);
```

**Synchronous Example:** Creates a new thread for the callback to execute on but the future will  hold the current 
thread.
```
Cpas.getInstance().function(params, callBack).get();
```

## Building
**Note:** If you do not have Gradle installed then use ./gradlew for Unix systems or Git Bash and gradlew.bat for Windows 
systems in place of any 'gradle' command.

In order to build Java CPAS Library just run the `gradle build` command. Once that is finished you will find library, sources, and 
javadoc .jars exported into the `./build/libs` folder and the will be labeled like the following.
```
CpasLibrary-x.x.x.jar
CpasLibrary-x.x.x-javadoc.jar
CpasLibrary-x.x.x-sources.jar
```

**Alternatively** you can include Java CPAS Library in your build.gradle file by using the following.
```
repositories {
    maven {
        name = 'reallifegames'
        url = 'https://reallifegames.net/artifactory/library-release'
    }
}

dependencies {
    compile 'net.cpas:CpasLibrary:x.x.x' // For compile time.
    runtime 'net.cpas:CpasLibrary:x.x.x' // For usage in a runtime application.
}
```