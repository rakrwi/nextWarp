plugins {
    `java-library`
}

dependencies {
    api("redis.clients:jedis:5.1.0")
    api("com.google.code.gson:gson:2.10.1")
    api("com.zaxxer:HikariCP:5.1.0")
    api("mysql:mysql-connector-java:8.0.33")
    api("org.yaml:snakeyaml:2.2")
}