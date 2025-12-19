# Jar Stubify

A simple tool that converts a jar into an 'api only' jar.
All data files will be deleted, and all class files will have their code stubed out.
The intended use is creating stub jar files for projects that can be used to compile. 
This uses the ClassFile API and so needs java 25+

## Usage

```shell
java -jar jar-stubify.jar --input minecraft-1.21.11.jar --output minecraft-1.21.11-api.jar
```

> [!WARNING]
> **There is no public API for this tool!** This is designed to solely be a CLI tool, which means that all of the implementations are internal. We reserve the right to change the internal implementation at any time.
