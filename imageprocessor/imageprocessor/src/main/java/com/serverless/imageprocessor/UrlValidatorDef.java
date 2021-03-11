package com.serverless.imageprocessor;

public class UrlValidatorDef {
    public UrlValidatorDef() {

    }

    public UrlValidatorDef(String url) {

        this.url = url;
    }
    String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url){ this.url = url;}

}
