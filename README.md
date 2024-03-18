## Search Engine

Final study work in Skillbox course "Java development". Web application, that realize indexing web-sites and search on result.

+ Frontend: provided by Skillbox
+ Backend: Java 17
+ Database: MySQL

Technologies used in Backend:
+ Spring Boot
+ Multithreading by using ForkJoinPool
+ Hibernate ORM
+ SQL for optimized queries
+ [Russian Morphology for Apache Lucene](https://github.com/AKuznetsov/russianmorphology) 


Settings available in application.yaml.\
Database fully created by app, just need create schema in MySql and use setting: 
```
server:
  port: 8080

spring:
  datasource:
    username: root
    password: masterkey
    url: jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: validate
```

List of indexing sites in application.yaml:
```
indexing-settings:
  sites:
    - url: https://www.svetlovka.ru
      name: Светловка.ру
    - url: https://www.playback.ru
      name: PlayBack.Ru
```

