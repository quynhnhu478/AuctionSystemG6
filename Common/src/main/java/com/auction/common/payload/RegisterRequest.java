<<<<<<<< HEAD:Server/src/main/java/com/auction/server/payload/User/RegisterRequest.java
package com.auction.server.payload.User;
========
package com.auction.common.payload;
>>>>>>>> origin/ngọc_2:Common/src/main/java/com/auction/common/payload/RegisterRequest.java

public class RegisterRequest {
    private String name;
    private String email;
    private String password;

    public RegisterRequest() {}

    public RegisterRequest(String name, String email, String password){
        this.name = name;
        this.email = email;
        this.password = password;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public void setPassword(String password){
        this.password = password;
    }
    public String getName(){
        return name;
    }
    public String getEmail(){
        return email;
    }

    public String getPassword(){
        return password;
    }
}
