
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

You'll need an [OpenJDK][openjdk] installation. The precompiled binaries from Oracle are basically
the same thing. Download the version for your platform, and install it wherever you like.
If you install it to `jfxgl-env/openjdk`, this script will automatically know where to find it.
If you install it somewhere else, see the [Troubleshooting](#markdown-header-troubleshooting) section for
instructions on how to tell the setup script where it is.

[openjdk]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html


### 3. Install prerequisites

This setup script requires a few tools to work. Make sure they're installed on your system.

 * [Gradle][gradle] (used to build OpenJFX)
 * [Eclipse][eclipse] (the IDE we'll use)
 * [OpenJFX prerequisites][openjfx-prereq]

Other IDEs aren't supported yet, but they could be. Feel free to contribute instructions/scripts
for your favorite IDE.

[gradle]: https://gradle.org/
[eclipse]: http://www.eclipse.org/
[openjfx-prereq]: https://wiki.openjdk.java.net/display/OpenJFX/Building+OpenJFX#BuildingOpenJFX-PlatformPrerequisites

**WARNING:** If you intend to build on Windows, OpenJFK is *very* difficult to build on that platform.
There are a ton of pre-requisites to install (like cygwin) and you need to hack the Windows gradle build files manually
so they can find the compilers, libraries, headers, etc. It's generally a huge pain in the ass and I can't
recommend even people I don't like to do it. I've made some attempt to get the JFXGL setup script to work on Windows,
but it can't automate the gradle build script hacks, so the script will usually fail at the OpenJFX Gradle step.

However, if you're determined to build on Windows, it is possible. Just prepare for lots of headaches.
On the other hand, If you do managed to get the setup script working in Windows in a portable way,
contributions are very welcome. =)

Really though, I recommend building in Linux. The compiled bytecode and jar files are cross-platform,
and this setup script works very well in Linux.


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
Eclipse will go mad trying to compile everything. When it's done, you should end up with only a few compile errors
from the OpenJFX modules. I think there's a couple missing dependencies (like Eclipse SWT), but we shouldn't need
those components in a development environment anyway, so you should be able to safely ignore those compile errors.

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


### Finding build tools

The setup script checks that it can find the build tools it needs.
If the build tools can't be found, you'll see an error like "can't find executable tool.exe".

If this happens, make the text file ``jfxlg-env/build/boot/options.properties`` and add paths for the
missing executable files using your favorite text editor (mine's [gVim][gvim]):
```
pathHg=/path/to/your/hg
pathGradle=/path/to/your/gradle
pathEclipse=/path/to/your/eclipse
```

[gvim]: http://www.vim.org/


### Finding the JDK

If the setup script complains about "no JDK found at ...", then you'll need to specify the JDK
path. Make the text file ``jfxgl-env/build/boot/options.properties`` and add the path for your JDK:
```
pathJDK=/path/to/your/JDK
```


### Still doesn't work?

[Report an issue.](https://bitbucket.org/cuchaz/jfxgl-env/issues)
