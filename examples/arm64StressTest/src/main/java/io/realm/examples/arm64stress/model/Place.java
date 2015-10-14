package io.realm.examples.arm64stress.model;

import io.realm.RealmObject;

public class Place extends RealmObject {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
