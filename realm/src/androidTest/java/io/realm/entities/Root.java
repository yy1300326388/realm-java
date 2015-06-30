package io.realm.entities;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Root extends RealmObject {

    @PrimaryKey
    private String uuid;
    private RealmList<Levels> levels;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public RealmList<Levels> getLevels() {
        return levels;
    }

    public void setLevels(RealmList<Levels> levels) {
        this.levels = levels;
    }
}