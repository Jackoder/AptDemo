AptDemo
====

use case
----

annotated .java

```java
@Builder
public class User {

    String userName;
    int    age;

}
```

generated .class

```java
public class UserBuilder {

    private String userName;
    private int    age;

    public static UserBuilder() {
        return new UserBuilder();
    }

    public UserBuilder userName(String userName) {
        this.userName = userName;
        return this;
    }

    public UserBuilder age(int age) {
        this.age = age;
        return this;
    }

    public User build() {
        User item = new User();
        item.userName = userName;
        item.age = age;
        return item;
    }
} 
```