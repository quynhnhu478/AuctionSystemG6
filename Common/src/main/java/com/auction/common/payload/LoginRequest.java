<<<<<<<< HEAD:Server/src/main/java/com/auction/server/payload/User/LoginRequest.java
package com.auction.server.payload.User;
========
package com.auction.common.payload;
>>>>>>>> origin/ngọc_2:Common/src/main/java/com/auction/common/payload/LoginRequest.java

public class LoginRequest {
    private String name;
    private String password;

    public LoginRequest() {}

    public LoginRequest(String name, String password){
        this.name = name;
        this.password = password;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setPassword(String password){
        this.password =password;
    }
    public String getName (){
        return name;
    }
    public String getPassword(){
        return password;
    }
}
