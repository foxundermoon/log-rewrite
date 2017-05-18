# log rewrite
## log-rewrite-app
项目根目录运行
`gradle :log-rewrite-app:distZip`

`log-rewrite-app/build/distributions/log-rewrite-app.zip`为目标打包应用

解压之后`bin`目录下有可执行脚本文件

然后运行脚本`./log-rewrite-app -c  config-path`
配置文件格式见下面详细说明

## log-recover-app
项目根目录运行
`gradle :log-recover-app:distZip`
同上然后运行脚本`./log-recover-app -m mapping-1.txt -m mapping-2.txt  -l f1.log -l f2.log  -d the/dist/dir`
便可恢复日志。 `-m` `-l` 后可以多次添加map、日志文件；`-d`为恢复后的日志输出文件夹路劲。


#  配置文件说明


# 格式为toml

```toml

projectShortName="grd" #the shot name for target project
versionCode=1

[[unit]]
[[unit.signatures]]
sign="com.getui.rewrite.test.LogUtil log(String)"
argsIndex=[0]

[[unit.signatures]]
sign="com.getui.rewrite.test.LogUtil log(String,String)"
argsIndex=[1]

[[unit.signatures]]
sign="com.getui.rewrite.test.LogUtil log(String,String)"
argsIndex=[0]
isTag=true

[[unit.signatures]]
sign="com.getui.rewrite.test.LogUtil debug(String)"
#  ## default is argsIndex=[0]

[unit.source]
dirs=[
"/Users/fox/workspace/getui/tools/log-rewrite/rewrite-test-java/src/main/java"
]

[unit.distribution]
dir="/Users/fox/workspace/getui/tools/log-rewrite/rewrite-test-java/build/rewrite/src/main/java"
mappingDir="/Users/fox/workspace/getui/tools/log-rewrite/rewrite-test-java/build/rewrite/mapping"
```

### 字段说明
* `projectShortName` 模块名简称，一个项目里有多模块的时候区分各模块，比如`sdk` `gks` `etc`,如果只有一个模块就写项目名简称，字符尽量短 比如 `gy`

* `versionCode` 版本号，每次发版的时候递增1， 用来区分不同版本的日志

* `unit.signatures.sign` 日志函数签名， 参数若为其他类型需要加上完整包名 
`demo.LogUtil log(String,String,com.getui.LogInfo)`

*  `unit.signatures.argsIndex` 需要混淆的参数index , 用中括号包围起来 eg: `[1]`
* `unit.source.dirs` 源代码路径，若为相对路径则为 `log-rewrite-app`脚本的相对路径
* `unit.distribution.dir` 重写后存放代码的路径
* `unit.distribution.mappingDir` 重写后存放映射表的路径





