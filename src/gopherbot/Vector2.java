package gopherbot;

import battlecode.common.*;

import java.util.*;


public strictfp class Vector2 {
    public static float radiansToDegrees(float radians) {
        return (float) (radians*180/Math.PI);
    }
    public float x = 0;
    public float y = 0;

    Vector2(float argX, float argY) {
        x = argX;
        y = argY;
    }

    public float getLength() {
        return (float) Math.sqrt(x*x+y*y);
    }
    public Vector2 add(Vector2 v) {
        return new Vector2(x+v.x, y+v.y);
    }

    public Vector2 subtract(Vector2 v) {
        return new Vector2(x-v.x, y-v.y);
    }

    public Vector2 multiply(float m) {
        return new Vector2(x*m, y*m);
    }

    public Vector2 normalized() {
        return new Vector2(1/getLength()*x, 1/getLength()*y);
    }

    public float getAngle() {
        return radiansToDegrees((float) Math.atan2(y,x));
    }

    public static Direction angleToDirection(float angle) {
        int degree45 = ((int) (Math.round(angle / 45)*45)+720)%360;
        switch (degree45) {
            case 0:
                return Direction.EAST;
            case 45:
                return Direction.NORTHEAST;
            case 90:
                return Direction.NORTH;
            case 135:
                return Direction.NORTHWEST;
            case 180:
                return Direction.WEST;
            case 225:
                return Direction.SOUTHWEST;
            case 270:
                return Direction.SOUTH;
            case 315:
                return Direction.SOUTHEAST;
            default:
                return Direction.CENTER;
        }
    }

    public Direction toDirection() {
        return angleToDirection(getAngle());
    }
}