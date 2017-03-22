
# JFXGL Build Environment

*script for creating the [JFXGL][jfxgl] build environment*

[jfxgl]: https://bitbucket.org/cuchaz/jfxgl


## How to use


### 1. Download the script
```
$ mkdir jfxgl-env
$ cd jfxgl-env
$ hg clone https://cuchaz@bitbucket.org/cuchaz/jfxgl-env .
```
Of course, you'll need to make sure [Mercurial][hg] is installed.

[hg]: https://www.mercurial-scm.org/


### 2. Download OpenJDK

You'll need an [OpenJDK 8u121][8u121] installation. The precompiled binaries from Oracle are basically
the same thing. Download the version for your platform, and make sure it's installed at
`jfxgl-env/openjdk-8u121`. If for some reason your JDK installation
is in a different location, see the [Troubleshooting](#markdown-header-troubleshooting) section for
instructions on how to tell the setup script where it is.

[8u121]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html


### 3. Install prerequisites

This setup script requires a few tools to work. Make sure they're installed on your system.

 * [Gradle][gradle] (used to build OpenJFX)
 * [Eclipse][eclipse] (the IDE we'll use)

Other IDEs aren't supported yet, but they could be. Feel free to contribute instructions/scripts
for your favorite IDE.

[gradle]: https://gradle.org/
[eclipse]: http://www.eclipse.org/


### 4. Run the script
```
$ ./jerkar setup
```


### 5. Wait and see what happens

This script does a ton of work and downloads a bunch of huge files, so it takes a while to finish.
It takes maybe 15 minutes or so depending on your internet connection speed and CPU speed.
I'd suggest starting it and then amusing yourself with something else while you wait for it to finish.

To see what to do next, choose your own adventure!

[Great, it worked!](#markdown-header-it-worked) :D

[Crap, it didn't work.](#markdown-header-troubleshooting) D:


## It worked

When the setup script is done, you can start Eclipse at the workspace `jfxgl-env` and everything should Just Work.
Eclipse will go mad trying to compile everything. When it's done, you should end up with a bunch of compile errors.
Since we broke the crap out of the OpenJFX project with our hacky patch, OpenJFX won't fully compile anymore.

But that's ok.

The stuff we actually need should compile just fine.

If the `JFXGL` project has compile errors, probably Eclipse is just dumb and you should refresh all the projects
until the errors go away. Eventually, the `JFXGL` and `JFXGL-demos` projects should have no compile errors.


### Try running the demos

Find the `cuchaz.jfxgl.HelloWorld` class and run it. (right click, "Run As" > "Java Application")
Eclipse will complain about compile errors in the workspace. It's not wrong. Just ignore the errors.
If the app works, you'll be greeted with a small window containing a white background and centered black
text that reads simply "Hello World".

If you make it that far, then congratulations! Everything works.

You can try the `HelloWorldPane`, `demo.overlay.Main`, and `demo.pane.Main` classes too.


## Troubleshooting


### Finding the JDK

If the setup script doesn't work, the first thing to check is if the script can find the JDK installation.

Open `build/def/Build.java` in your favorite text editor (mine's [gVim][gvim]) and look for these lines near the top:
```java
// by default, we'll assume the JDK is at this relative path
// if it's not, feel free to edit this script and add the absolute path here
private static final File JDKDir = new File("openjdk-8u121");

```
If that `File` doesn't point to your JDK installation, then edit the script until it does. Then re-run the script.

[gvim]: http://www.vim.org/


### Finding build tools

The next thing to check is if the script can find the build tools it needs.
The setup scripts assumes a Sane and Normal Shell Environment that can find executables simply by naming them.
This works fantastically well in Linux, but I haven't tested it in Mac or Windows, so who knows what will happen
there.

If you see errors that look like the script can't find an executable file, you can fix that by making tiny edits
to the top of the script file.

Open `build/def/Build.java` in your favorite text editor and look for these lines near the top:
```java
// by default, we'll omit absolute paths and let the operating system find the paths for these tools
// if for some reason, the OS doesn't get it right, feel free to edit this script and add absolute paths
private static final String HG = "hg";
private static final String GRADLE = "gradle";
private static final String ECLIPSE = "eclipse";

```
Simply edit the string constants to the full path to those executables and re-run the script. Of course, if those
tools aren't installed, you'll need to install them.


### Still doesn't work?

[Report an issue.](https://bitbucket.org/cuchaz/jfxgl-env/issues)
