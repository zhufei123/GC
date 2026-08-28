package com.recycle.admin.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminLoginVO {

    private String token;
    private AdminProfileVO admin;
    private List<String> roles;
    private List<String> perms;

    @Data
    public static class AdminProfileVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
    }
}
