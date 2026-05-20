<<<<<<<< HEAD:Server/src/main/java/com/auction/server/payload/User/UserResponse.java
package com.auction.server.payload.User;
========
package com.auction.common.payload;
>>>>>>>> origin/ngọc_2:Common/src/main/java/com/auction/common/payload/UserResponse.java

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String message;
    public UserResponse(){}
    public UserResponse(Long id, String name, String email, String message){
        this.id = id;
        this.name = name;
        this.email = email;
        this.message = message;
    }
    public void setId(Long id){
        this.id = id;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setEmail(String email){
        this.email =email;
    }
    public void setMessage(String message){
        this.message = message;
    }
    public Long getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String getEmail(){
        return email;
    }
    public String getMessage(){
        return message;
    }

}
