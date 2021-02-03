# Installation

OTM uses [Java 11.0.5](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html)

## Direct download
You can obtain OTM as a jar file [here](https://mymavenrepo.com/repo/XtcMAROnIu3PyiMCmbdY/edu/berkeley/ucbtrans/otm-sim/1.0-SNAPSHOT/).
Download the most recent large file of the form:

```
otm-sim-1.0-<date>.<time>-<number>-jar-with-dependencies.jar
```

## Build from source

Follow these steps if you wish to build OTM from the source code.

1. Fork and/or clone the [repo](https://github.com/ggomes/otm-sim).
2. Install [Apache Maven](https://maven.apache.org/install.html).
3. Add OTM's Maven repository profile to your Maven settings file: `~/.m2/settings.xml`:
```xml
    <profile>
        <id>myMavenRepoOTM</id>
        <activation>
            <property>
                <name>!doNotUseMyMavenRepo</name>
            </property>
        </activation>
        <properties>
            <myMavenRepoOTMReadUrl>https://mymavenrepo.com/repo/XtcMAROnIu3PyiMCmbdY/</myMavenRepoOTMReadUrl>
        </properties>
    </profile>
```
If you do not have `~/.m2/settings.xml` then you can use [this one](https://github.com/ggomes/otm-sim/blob/master/settings.xml).
4. Install:
```
mvn clean install -DskipTests
```