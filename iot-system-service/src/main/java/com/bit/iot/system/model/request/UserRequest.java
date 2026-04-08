package com.bit.iot.system.model.request;

import lombok.Data;

@Data
public class UserRequest {
    private String id;
    private String username;
    private String password;
    private String phoneNumber;
    private String nameCn;
    private String email;
    private Integer status;
}
