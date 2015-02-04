package io.realm.entities;

import io.realm.RealmObject;
import io.realm.annotations.Index;

/**
 * Created by kneth on 03/02/15.
 */
public class MoveCrash extends RealmObject {
    @Index
    private long second;

    public long getSecond() {
        return second;
    }

    public void setSecond(long second) {
        this.second = second;
    }
}
