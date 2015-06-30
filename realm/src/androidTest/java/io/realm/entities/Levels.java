package io.realm.entities;

import io.realm.RealmList;
import io.realm.RealmObject;

public class Levels extends RealmObject {

    private int zoomLevel;
    private RealmList<Points> points;

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public RealmList<Points> getPoints() {
        return points;
    }

    public void setPoints(RealmList<Points> points) {
        this.points = points;
    }
}