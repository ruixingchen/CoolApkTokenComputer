# CoolApkTokenComputer
now we can use tons of tokens to hack CoolApk, any time, any language, any where and any platform, 😏

**the oringin project is from [https://github.com/by-syk/CoolapkTokenGetter](https://github.com/by-syk/CoolapkTokenGetter)**

**I'm not an Android developer, my Kotlin code sucks, 😂**

### principle
the token is generated with time and uuid, of cause some tricks like string reverse or else in the .so file. I am not good at reading the assembly, I don't know the way they mix the time and uuid, but there is an other way to generate the token, yes, we change the system time, so we can generate a list of tokens for the future use. the token will be out of date in 300 seconds, so we just have 288 tokens to generate for one day, that is acceptable.

### requirement

* rooted device, simulator or physical device is OK. ARM or X86 is OK.

**the generated json file will be in the Downloads**

**USAGE**

1. UUID

	you can generate with a specific uuid, keep it blank if you want random uuid

2. StartYear

	you can generate tokens for a whole year, the format is yyyy(like 2018), 370 days generated
	
3. StartMonth

	like StartYear, input yyyyMM(like 201802) to generate for a month (32 days actually)

4. StartTime

	the start point of the generation,format is yyyyMMddHHmmss(like 20180201121500), slide on the seekbar to choose how many days to generate, 1 min and 32 max
	
5. DEBUG

	output with some debug and check information
	
**the generation will use the first non-blank field, if the format is wrong, app will crash😂**

**screenshoot:**

![](https://raw.githubusercontent.com/ruixingchen/CoolApkTokenComputer/master/ReadmeImage/mainScreen.jpg)

**the json file:**

![](https://raw.githubusercontent.com/ruixingchen/CoolApkTokenComputer/master/ReadmeImage/textSample.jpg)

