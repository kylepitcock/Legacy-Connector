package com.example.legacytest;

public class CommonProxy {
    public void preInit() {
        LegacyTestMod.log.info("LegacyTest proxy: " + getClass().getSimpleName());
    }
}
