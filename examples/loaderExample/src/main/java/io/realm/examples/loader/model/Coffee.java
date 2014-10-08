package io.realm.examples.loader.model;

import io.realm.RealmObject;

public class Coffee extends RealmObject {

    private String name;
    private String origin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String originCountry) {
        this.origin = originCountry;
    }

}
