# Compiling

## Clone the repo <br/>
`git clone https://github.com/md5sha256/AreaShop.git`

## Compile a Jar
This task may take a while as the dev environment for platform adapters in `adapters/platform` take a while to set up 
<br/>
<strong>Compilation command:</strong> `./gradlew build` (UNIX) or `gradlew build` (Windows)
<br/>
The plugin jar can be found under the `AreaShop` module in `AreaShop/build/libs/`<br/>
There are three jars, use the one which has no suffix. Ex: `AreaShop-VERSION.jar`

## Gradle Tasks
The following is a simple table describing commands which may be useful for building the plugin. <br>
For more details about gradle please view their documentation [here](https://docs.gradle.org/current/userguide/userguide.html). 
</br>
A shorthand for the CLI [here](https://docs.gradle.org/current/userguide/command_line_interface.html).
<br>

| Command                       | Description                                                                                        | 
|-------------------------------|----------------------------------------------------------------------------------------------------|
| `gradlew build`               | Compiles all modules and packages the plugin into one jar                                          |
| `gradlew publishToMavenLocal` | Compiles and publishes all modules except the platform adapters to `.m2`                           |
| `gradlew clean`               | Cleans the development workspace and clears caches. Note, you don't have to clean before you build |