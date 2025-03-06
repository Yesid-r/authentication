package com.example.authentication.constants;


import io.jsonwebtoken.security.Keys;

public class ApplicationConstants {
    public static final String OTP_CHARACTERS = "123456789";
    public static final Integer OTP_LENGTH = 6;

    public static final String SECRET_KEY = "5a3f12d9eb78c206fd53b921ae4d7894c917f05e23d8e6ab419cf753d802a91c";
    public static final long ACCESS_TOKEN_VALIDITY_SECONDS = 60*60;
    public static final long REFRESH_TOKEN_VALIDITY_SECONDS = 30*24*60*60;
}
