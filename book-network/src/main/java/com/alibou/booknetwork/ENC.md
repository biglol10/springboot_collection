```
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>

<plugin>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-maven-plugin</artifactId>
    <version>3.0.5</version>
</plugin>
```

---

```
./mvnw jasypt:encrypt-value "-Djasypt.encryptor.password=jasyptpassword" "-Djasypt.plugin.value=jwtKey"

```

---

username: ENC(OYYA1qBAmbdxYvwTjIkkdjz0ZHuhzzEBs2hTbNBddeHAivXXz89H7gtKzjoEPSuU)
