# BOTC-Fab
Blood on the Clocktower - Fabric edition

## Getting started
### Developer
1. Make sure you have the latest version of JDK downloaded (JDK 21 minimum, I prefer using [Zulu JDK 21 LTS](https://www.azul.com/downloads/?version=java-21-lts&package=jdk#zulu))
2. Open IntelliJ, install the [Minecraft-Development](https://plugins.jetbrains.com/plugin/8327-minecraft-development) plugin
3. Open a terminal, navigate to the folder location you wish to clone the repo, and `git clone git@github.com:KenV1040/botc-fabric.git`
4. Open the directory you just cloned in IntelliJ, and let it set itself up.

## Updating mod to latest minecraft version
1. Navigate to the [fabricmc](https://fabricmc.net/develop/)'s develop page
2. Select the minecraft version you want
3. Edit the `gradle.properties` file as the site has suggested
4. Update the `fabric.mod.json` to the appropriate values
5. On the right pane of IntelliJ, the Gradle icon, click on runServer again. This will regenerate the build configs to make it work

## Contributing
1. On your editor's terminal (IntelliJ use `Alt+F12`), checkout a new branch and create it. Make sure you label it appropriately. E.g. `git checkout -b <new-feature/new-version>`
2. Do your changes. Commit regularly to allow easy rollback if something fucks up.
3. Once you're done, do `git push` and it'll error. Just enter the command the error tells you
4. Now you should be able to see it on Github. Refresh the page just in case.
