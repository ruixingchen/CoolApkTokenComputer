# CoolApkTokenComputer
now we can use tons of tokens to hack CoolApk, any time, any language, any where and any platform, 😏

**the oringin project is from [https://github.com/by-syk/CoolapkTokenGetter](https://github.com/by-syk/CoolapkTokenGetter)**

**I'm not an Android developer, my Kotlin code sucks, 😂**

### principle
the token is generated with time and uuid, of cause some tricks like string reverse or else in the .so file. I am not good at reading the assembly, I don't know the way they mix the time and uuid, but there is an other way to generate the token, yes, we change the system time, so we can generate a list of tokens for the future use. the token will be out of date in 300 seconds, so we just have 288 tokens to generate for one day, that is acceptable.

### requirement

* rooted device, simulator or physical device is OK. ARM or X86 is OK.

**the generated json file will be in the Downloads**

**screenshoot:**

![](https://raw.githubusercontent.com/ruixingchen/CoolApkTokenComputer/master/ReadmeImage/mainScreen.jpg)

**the json file:**

![](https://raw.githubusercontent.com/ruixingchen/CoolApkTokenComputer/master/ReadmeImage/textSample.jpg)

