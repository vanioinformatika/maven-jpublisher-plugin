Maven JPublisher Plugin
=======================

Maven plugin for generating Java classes from SQL types and packages with [Oracle JPublisher](http://docs.oracle.com/cd/B28359_01/java.111/b31226/intro.htm "Oracle JPublisher documentation")

Usage
-----
```xml
<plugin>
    <groupId>hu.vanio.maven.plugins</groupId>
    <artifactId>maven-jpublisher-plugin</artifactId>
    <version>1.1.0</version>
    <configuration>
        <executable>/opt/java5/bin/java</executable>  
        <dbUrl>jdbc:oracle:thin:@my_oracle_host:1521:SID</dbUrl>
        <dbUser>scott</dbUser>
        <dbPassword>tiger</dbPassword>
        <skip>false</skip>
        <propsFile>${basedir}/src/main/jpublisher/props.txt</propsFile>
        <typeListFile>${basedir}/src/main/jpublisher/typelist.txt</typeListFile>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
- executable: java version to start oracle jpub plugin
