package com.mad.qut.budgetr.model;

import com.google.gson.Gson;

public class Currency {

    public String id;
    public String name;
    public String symbol;

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Currency fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Currency.class);
    }

}
