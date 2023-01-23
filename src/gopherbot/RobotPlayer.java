package gopherbot;

import battlecode.common.*;

import java.util.*;

public strictfp class RobotPlayer {
    public static float radiansToDegrees(float radians) {
        return (float) (radians*180/Math.PI);
    }

    private static strictfp class Vector2 {
        public static float x;
        public static float y;

        Vector2(float argX, float argY) {
            x = argX;
            y = argY;
        }

        public static float getLength() {
            return (float) Math.sqrt(x*x+y*y);
        }
        public static Vector2 add(Vector2 v) {
            return new Vector2(x+v.x, y+v.y);
        }

        public static Vector2 subtract(Vector2 v) {
            return new Vector2(x-v.x, y-v.y);
        }

        public static Vector2 multiply(float m) {
            return new Vector2(x*m, y*m);
        }

        public static Vector2 normalized() {
            return new Vector2(1/getLength()*x, 1/getLength()*y);
        }

        public static float getAngle() {
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

        public static Direction toDirection() {
            return angleToDirection(getAngle());
        }
    }

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;
    static int sumX = 0;
    static int sumY = 0;
    static final int mapInfoStart1 = 7; //where the map info queue starts in the shared array; WELL AND ISLAND ONLY
    static final int mapInfoStart2 = 27;
    static final int sharedArraySize = 64;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (69420); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(69420);

    static float[] buildRatios = {5, 6, 1, 2, 2};

    static int robotTypeToInt(RobotType robotType) {
        if (robotType == RobotType.HEADQUARTERS) {
            return 0;
        } else if (robotType == RobotType.CARRIER) {
            return 1;
        } else if (robotType == RobotType.LAUNCHER) {
            return 2;
        } else if (robotType == RobotType.DESTABILIZER) {
            return 3;
        } else if (robotType == RobotType.BOOSTER) {
            return 4;
        } else {
            return 5;
        }
    }

    static RobotType intToRobotType(int robotType) {
        return RobotType.values()[robotType];
    }

    static RobotType[] adRobotTypes = new RobotType[]{RobotType.CARRIER, RobotType.AMPLIFIER};
    static RobotType[] mnRobotTypes = new RobotType[]{RobotType.AMPLIFIER, RobotType.LAUNCHER};
    static RobotType[] exRobotTypes = new RobotType[]{RobotType.BOOSTER, RobotType.DESTABILIZER};

    static RobotType[] resourceTypeToRobotTypes(ResourceType resourceType) {
        if (resourceType == ResourceType.ADAMANTIUM) {
            return adRobotTypes;
        } else if (resourceType == ResourceType.MANA) {
            return mnRobotTypes;
        } else {
            return exRobotTypes;
        }
    }

    static int numHeadquarters;
    static MapLocation spawnPoint;

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if (rc.getType() == RobotType.HEADQUARTERS) {
            numHeadquarters = rc.getRobotCount();
        }
        spawnPoint = rc.getLocation();
        while (true) {
            turnCount += 1;  // We have now been alive for one more turn!
            sumX += rc.getLocation().x;
            sumY += rc.getLocation().y;

            try {
                if (rc.getType() != RobotType.HEADQUARTERS && rc.canWriteSharedArray(0, 0)) {
                    rc.writeSharedArray(robotTypeToInt(rc.getType()), rc.readSharedArray(robotTypeToInt(rc.getType()))+1);
                }
                if (rc.getType() != RobotType.HEADQUARTERS && rc.getRoundNum() % 2 == 1) {
                    broadcastMapInfos(rc);
                }
                if (rc.getRoundNum() % 2 == 0) {
                    readBroadcastedMapInfos(rc);
                }
                switch (rc.getType()) {
                    case HEADQUARTERS:     runHeadquarters(rc);  break;
                    case CARRIER:      runCarrier(rc);   break; //1
                    case LAUNCHER: runLauncher(rc); break; //2
                    case BOOSTER: runBooster(rc); break; //3
                    case DESTABILIZER: runDestabilizer(rc); break; //4
                    case AMPLIFIER:       runAmplifier(rc); break; //5
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    
    static float round(float x) {
        return ((float)Math.round(x*10))/10;
    }

    static int directionToInt(Direction direction) {
        return direction.getDirectionOrderNum()+2;
    }

    static Direction intToDirection(int direction) {
        return Direction.allDirections()[direction-2];
    }

    static int resourceTypeToInt(ResourceType resourceType) {
        if (resourceType == ResourceType.ADAMANTIUM) {
            return 11;
        } else if (resourceType == ResourceType.MANA) {
            return 12;
        } else {
            return 13;
        }
    }

    static ResourceType intToResourceType(int resourceType) {
        if (resourceType == 11) {
            return ResourceType.ADAMANTIUM;
        } else if (resourceType == 12) {
            return ResourceType.MANA;
        } else {
            return ResourceType.ELIXIR;
        }
    }

    static int teamToInt(Team currTeam, Team team) {
        if (team == currTeam) {
            return 14;
        } else {
            return 15;
        }
    }

    static Team intToTeam(Team currTeam, int team) {
        if (team == 14) {
            return currTeam;
        } else {
            return Team.NEUTRAL;
        }
    }

    static int mapInfoToInt(RobotController rc, MapInfo mapInfo) throws GameActionException { //assumes that empty positions will never be sensed
        MapLocation location = mapInfo.getMapLocation();

        int result = 0;
        if (!mapInfo.isPassable()) {
            result += 1;
        } else if (mapInfo.hasCloud()) {
            result += 2;
        } else {
            Direction currentDir = mapInfo.getCurrentDirection();
            if (currentDir != Direction.CENTER) {
                result += directionToInt(currentDir);
            } else {
                WellInfo wellInfo = rc.senseWell(location);
                if (wellInfo != null) {
                    result += resourceTypeToInt(wellInfo.getResourceType());
                } else {
                    int island = rc.senseIsland(location);
                    if (island != -1) {
                        result += teamToInt(rc.getTeam(),rc.senseTeamOccupyingIsland(island));
                    }
                }
            }
        }

        if (result == 0) { //empty
            return -1;
        }

        result += location.y << 4;
        result += location.x << 10;
        
        return result;
    }

    static private strictfp class BroadcastedMapInfo {
        public static MapLocation location;
        public static int locType;
        BroadcastedMapInfo(MapLocation argLocation, int argLocType) {
            location = argLocation;
            locType = argLocType;
        }
    }

    static BroadcastedMapInfo intToBroadcastedInfo(RobotController rc, int i) throws GameActionException {
        int x = i >> 10;
        int y = (i >> 4) & 0b111111;
        int locType = i & 0b1111;
        return new BroadcastedMapInfo(new MapLocation(x,y), locType);
    }

    static void broadcastMapInfos(RobotController rc) throws GameActionException {
        int byteCodeStart = Clock.getBytecodeNum();

        int firstAvailableInd1 = 64;
        int firstAvailableInd2 = 64;
        for (int i = mapInfoStart1; i < 64; i ++) {
            if (rc.readSharedArray(i) == 0) {
                if (i >= mapInfoStart1 && i < mapInfoStart2 && firstAvailableInd1 == 64) {
                    firstAvailableInd1 = i;
                } else if (firstAvailableInd2 == 64) {
                    firstAvailableInd2 = i;
                }
            }
        }
        if (firstAvailableInd1 == mapInfoStart2 && firstAvailableInd2 == 64) {
            return;
        }

        
        for (int islandInd : rc.senseNearbyIslands()) {
            if (firstAvailableInd1 == mapInfoStart2) {
                break;
            }
            MapLocation[] mapLocations = rc.senseNearbyIslandLocations(islandInd);
            for (MapLocation mapLocation : mapLocations) {
                if (firstAvailableInd1 == mapInfoStart2) {
                    break;
                }
                int encoded = mapInfoToInt(rc, new MapInfo(mapLocation, false, true, null, Direction.CENTER, null, null));

                if (rc.canWriteSharedArray(firstAvailableInd1,encoded)) {
                    rc.writeSharedArray(firstAvailableInd1, encoded);
                }
                firstAvailableInd1++;
                if (Clock.getBytecodeNum() - byteCodeStart >= rc.getType().bytecodeLimit/4) {
                    break;
                }
            }
        }

        WellInfo[] wellInfos = rc.senseNearbyWells();
        for (WellInfo wellInfo : wellInfos) {
            if (firstAvailableInd1 == mapInfoStart2) {
                break;
            }
            int encoded = mapInfoToInt(rc, new MapInfo(wellInfo.getMapLocation(), false, true, null, Direction.CENTER, null, null));

            if (rc.canWriteSharedArray(firstAvailableInd1,encoded)) {
                rc.writeSharedArray(firstAvailableInd1, encoded);
            }
            firstAvailableInd1++;
            if (Clock.getBytecodeNum() - byteCodeStart >= rc.getType().bytecodeLimit/4) {
                break;
            }
        }

        MapInfo[] mapInfos = rc.senseNearbyMapInfos();

        for (MapInfo mapInfo : mapInfos) {
            if (firstAvailableInd1 == mapInfoStart2 && firstAvailableInd2 == 64) {
                return;
            }
            int encoded = mapInfoToInt(rc, mapInfo);
            if (encoded != -1) {
                int locType = encoded & 0b1111;
                if (locType >= 11 && locType <= 15 && firstAvailableInd1 < mapInfoStart2) {
                    // if (rc.canWriteSharedArray(firstAvailableInd1,encoded)) {
                    //     rc.writeSharedArray(firstAvailableInd1, encoded);
                    // }
                    // firstAvailableInd1++;
                } else if (locType < 11 && firstAvailableInd2 < 64 && rng.nextInt(3) == 1) {
                    if (rc.canWriteSharedArray(firstAvailableInd2,encoded)) {
                        rc.writeSharedArray(firstAvailableInd2, encoded);
                    }
                    firstAvailableInd2++;
                }
            }
            if (Clock.getBytecodeNum() - byteCodeStart >= rc.getType().bytecodeLimit/4) {
                break;
            }
        }
    }

    private static class Current implements Comparable<Current> {
        Current(Direction argDirection, MapLocation argLocation) {
            direction = argDirection;
            location = argLocation;
        }

        @Override
        public int hashCode() {
            return location.hashCode() ^ direction.hashCode();
        }

        public int compareTo(Current other) {
            if (location != other.location) {
                return location.compareTo(other.location);
            } else {
                return direction.compareTo(other.direction);
            }
        }

        Direction direction;
        MapLocation location;
    }


    static Set<MapLocation> impassibleLocations = new TreeSet<MapLocation>();
    static Set<Current> currentLocations = new TreeSet<Current>();
    static Map<MapLocation, ResourceType> wellLocations = new TreeMap<MapLocation, ResourceType>();
    static Map<MapLocation, Team> islandLocations = new TreeMap<MapLocation, Team>(); //neutral island means unoccupied or occupied by other team
    static Set<MapLocation> cloudLocations = new TreeSet<MapLocation>();

    static void dfsIsland(RobotController rc, MapLocation location, Team mark) {
        islandLocations.put(location, mark);

        for (Direction direction : Direction.cardinalDirections()) {
            MapLocation newLoc = location.add(direction);
            Team res = islandLocations.get(newLoc);
            if (res != null && res != mark) {
                dfsIsland(rc, newLoc, mark);
            }
        }
    }

    static void processBroadcastedMapInfo(RobotController rc, BroadcastedMapInfo broadcastedMapInfo) {
        if (broadcastedMapInfo.locType == 1) { //impassible

            impassibleLocations.add(broadcastedMapInfo.location);

        } else if (broadcastedMapInfo.locType == 2) { //cloud

            cloudLocations.add(broadcastedMapInfo.location);

        } else if (broadcastedMapInfo.locType >= 3 && broadcastedMapInfo.locType <= 10) { //current

            currentLocations.add(new Current(intToDirection(broadcastedMapInfo.locType), broadcastedMapInfo.location));

        } else if (broadcastedMapInfo.locType >= 10 && broadcastedMapInfo.locType <= 13) { //well

            wellLocations.put(broadcastedMapInfo.location, intToResourceType(broadcastedMapInfo.locType));

        } else if (broadcastedMapInfo.locType >= 14 && broadcastedMapInfo.locType <= 15) { //island

            // islandLocations.put(broadcastedMapInfo.location, intToTeam(rc.getTeam(), broadcastedMapInfo.locType));
            dfsIsland(rc, broadcastedMapInfo.location, intToTeam(rc.getTeam(), broadcastedMapInfo.locType));

        }
    }

    static void readBroadcastedMapInfos(RobotController rc) throws GameActionException {
        int byteCodeStart = Clock.getBytecodeNum();
        for (int i = mapInfoStart1; i < 64; i ++) {
            int currVal = rc.readSharedArray(i);
            if (currVal == 0) {
                continue;
            }
            BroadcastedMapInfo currBroadcastedMapInfo = intToBroadcastedInfo(rc, currVal);
            
            processBroadcastedMapInfo(rc, currBroadcastedMapInfo);
            if (Clock.getBytecodeNum() - byteCodeStart >= rc.getType().bytecodeLimit/4) {
                break;
            }
        }
    }

    static void indicateMapInfos(RobotController rc) {
        // for (MapLocation mapLocation : impassibleLocations) {
        //     rc.setIndicatorDot(mapLocation, 255, 255, 255);
        // }
        
        // for (MapLocation mapLocation : cloudLocations) {
        //     rc.setIndicatorDot(mapLocation, 255, 255, 255);
        // }

        // for (Current current : currentLocations) {
        //     rc.setIndicatorDot(current.location, 255, 255, 255);
        // }

        for (MapLocation well : wellLocations.keySet()) {
            rc.setIndicatorDot(well, 255, 255, 255);
        }


        for (MapLocation islandLoc : islandLocations.keySet()) {
            if (islandLocations.get(islandLoc) == Team.A) {
                rc.setIndicatorDot(islandLoc, 255, 0, 0);
            } else {
                rc.setIndicatorDot(islandLoc, 0, 0, 255);
            }
        }
    }

    static void dfsIslandVisited(MapLocation loc, Set<MapLocation> visited) {
        visited.add(loc);
        for (Direction dir : Direction.cardinalDirections()) {
            MapLocation newLoc = loc.add(dir);
            if (!visited.contains(newLoc) && islandLocations.containsKey(newLoc)) {
                dfsIslandVisited(newLoc, visited);
            }
        }
    }


    static void runHeadquarters(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() == 1) { //pass first round to get correct headquarter count
            return;
        }

        indicateMapInfos(rc);

        if (rc.readSharedArray(0) == numHeadquarters) { //is first to run
            if (rc.getRoundNum() % 2 == 1) { //clear map info queue after even turns (odd turns for writing, even turns for reading)
                for (int i = mapInfoStart1; i < sharedArraySize; i ++) {
                    if (rc.readSharedArray(i) != 0) {
                        rc.writeSharedArray(i, 0);
                    } else {
                        break;
                    }
                }
            }
            rc.writeSharedArray(0, 0);
        }


        //set indicator string to robot counts
        String indicatorString = "";
        for (int i = 1; i <= 6; i ++) {
            indicatorString += rc.readSharedArray(i) + " ";
        }

        //calculate new robot position if we were to make a new robot
        MapLocation newLoc = null;
        for (Direction dir : directions) {
            newLoc = rc.getLocation().add(dir);
            if (rc.onTheMap(newLoc) && rc.sensePassability(newLoc) && rc.senseRobotAtLocation(newLoc) == null) {
                break;
            }
        }

        if (rc.onTheMap(newLoc) && rc.sensePassability(newLoc) && rc.senseRobotAtLocation(newLoc) == null) {
            float[] reccAmounts = {0, 0, 0, 0, 0};
            int totalCountedRobots = 0;
            float totalRatio = 0;
            for (int i = 1; i <= 5; i ++) {
                totalCountedRobots += rc.readSharedArray(i);
                totalRatio += buildRatios[i-1];
            }
            int newRobotCount = totalCountedRobots+1;
    
            //calculate recommended amounts
            indicatorString += "| ";
            for (int i = 1; i <= 5; i ++) {
                reccAmounts[i-1] = ((float) newRobotCount) * buildRatios[i-1]/totalRatio;
                indicatorString += round(reccAmounts[i-1]) + " ";
            }
    
            for (ResourceType resourceType : ResourceType.values()) {
                if (resourceType == ResourceType.NO_RESOURCE) {
                    continue;
                }
                float maxBuildDiff = -Float.MAX_VALUE;
                RobotType maxBuildDiffType = RobotType.CARRIER;
    
                for (RobotType robotType : resourceTypeToRobotTypes(resourceType)) {
                    int i = robotTypeToInt(robotType) - 1;
                    float currDiff = reccAmounts[i] - ((float) rc.readSharedArray(i + 1));
                    if (currDiff > maxBuildDiff) {
                        maxBuildDiff = currDiff;
                        maxBuildDiffType = intToRobotType(i + 1);
                    }
                }
                if (resourceType == ResourceType.ADAMANTIUM || resourceType == ResourceType.MANA) {
                    // Set<MapLocation> visited = new TreeSet<MapLocation>();
                    // int numOccupiedIslands = 0;
                    // for (MapLocation islandLoc : islandLocations.keySet()) {
                    //     Team res = islandLocations.get(islandLoc);
                    //     if (res == rc.getTeam()) {
                    //         if (!visited.contains(islandLoc)) {
                    //             numOccupiedIslands++;
                    //             dfsIslandVisited(islandLoc, visited);
                    //         }
                    //     }
                    // }
                    //TODO: fix numOccupiedIslands and well resource types (bcs conversion)
                    float anchorDiff = (rc.readSharedArray(1)*0.2f - rc.readSharedArray(6))/(float)numHeadquarters - rc.getNumAnchors(Anchor.STANDARD);
                    indicatorString += "| " + round(anchorDiff) + "," + round(maxBuildDiff);
    
                    if (anchorDiff > maxBuildDiff && anchorDiff < rc.readSharedArray(robotTypeToInt(RobotType.CARRIER))) {
                        if (rc.canBuildAnchor(Anchor.STANDARD)) {
                            rc.buildAnchor(Anchor.STANDARD);
                            break;
                        }
                    }
                }
    
                if (rc.canBuildRobot(maxBuildDiffType, newLoc)) {
                    rc.buildRobot(maxBuildDiffType, newLoc);
                    break;
                }
            }
        }

        //increase headquarter run count
        rc.writeSharedArray(0, rc.readSharedArray(0)+1);
        if (rc.readSharedArray(0) == numHeadquarters) {
            for (int i = 1; i <= 6; i ++) {
                rc.writeSharedArray(i, 0);
            }
        }
        rc.setIndicatorString(indicatorString);
    }

    static void randomMove(RobotController rc) throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }

    static MapLocation currMoveTarget = null;
    static boolean moveTargetIsWell = false;

    static boolean isUnloading = false;
    static boolean isLoading = false;

    static void pathfindTowardMoveTarget(RobotController rc) throws GameActionException {
        if (currMoveTarget != null && !rc.getLocation().equals(currMoveTarget)) { //TODO: general navigation
            rc.setIndicatorString(currMoveTarget.x + " " + currMoveTarget.y);
            Direction dir = rc.getLocation().directionTo(currMoveTarget);
            if (rc.canMove(dir)) {
                rc.move(dir);
            } else {
                randomMove(rc);
            }
        }
        if (rc.getLocation().equals(currMoveTarget) || (moveTargetIsWell && rc.getLocation().isAdjacentTo(currMoveTarget))) {
            currMoveTarget = null;
            moveTargetIsWell = false;
        }
    }

    static void runCarrier(RobotController rc) throws GameActionException {
        String indicatorString = "";

        MapLocation me = rc.getLocation();
        boolean foundWellTarget = false;
        if (rc.getAnchor() != null) {
            if (rc.canWriteSharedArray(0,0)) {
                rc.writeSharedArray(6, rc.readSharedArray(6)+1);
            }
            if (islandLocations.size() >= 1) {
                if (currMoveTarget == null) {
                    for (MapLocation mapLocation : islandLocations.keySet()) {
                        if (islandLocations.get(mapLocation) == Team.NEUTRAL) {
                            currMoveTarget = mapLocation;
                            break;
                        }
                    }
                }
            }
                
            if (currMoveTarget != null) {
                if (rc.getLocation().equals(currMoveTarget)) {
                    if (rc.canPlaceAnchor()) {
                        rc.placeAnchor();
                        int encoded = mapInfoToInt(rc, rc.senseMapInfo(rc.getLocation()));
                        if (rc.canWriteSharedArray(mapInfoStart2-1, encoded)) {
                            rc.writeSharedArray(mapInfoStart2-1, encoded);
                        }
                    }
                }
                if (islandLocations.get(currMoveTarget) == rc.getTeam()) {
                    currMoveTarget = null;
                    int encoded = mapInfoToInt(rc, rc.senseMapInfo(rc.getLocation()));
                    if (rc.canWriteSharedArray(mapInfoStart2-1, encoded)) {
                        rc.writeSharedArray(mapInfoStart2-1, encoded);
                    }
                }
            }
            if (rc.canPlaceAnchor() && rc.senseTeamOccupyingIsland(rc.senseIsland(rc.getLocation())) != rc.getTeam()) {
                rc.placeAnchor();
            }
        } else {
            if (!isUnloading && rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR) == 40) {
                currMoveTarget = spawnPoint;
            } else if (!isUnloading && !isLoading && currMoveTarget == null) {
                if (wellLocations.size() >= 1 && rng.nextInt(3) == 0) {
                    ResourceType targetType = ResourceType.values()[turnCount%2+1]; //TODO: include elixir wells when converted
                    int minWellDistSquared = Integer.MAX_VALUE;
                    for (MapLocation well : wellLocations.keySet()) {
                        if (wellLocations.get(well) == targetType && well.distanceSquaredTo(rc.getLocation())<minWellDistSquared && well.distanceSquaredTo(rc.getLocation())<200) {
                            foundWellTarget = true;
                            currMoveTarget = well;
                            moveTargetIsWell = true;
                            minWellDistSquared = well.distanceSquaredTo(rc.getLocation());
                        }
                    }
                } else {
                    currMoveTarget = new MapLocation(rc.getMapHeight()-1-spawnPoint.x, rc.getMapWidth()-1-spawnPoint.y);
                }
            }
        }
        if (!isLoading && !isUnloading) {
            pathfindTowardMoveTarget(rc);
            randomMove(rc);
        }

        indicatorString += " | " + String.valueOf(currMoveTarget);

        // Try to gather from and transfer to squares around us.
        isUnloading = false;
        isLoading = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation newLocation = new MapLocation(me.x + dx, me.y + dy);
                if (((currMoveTarget == null && foundWellTarget) || (currMoveTarget != null && !foundWellTarget)) && rc.canCollectResource(newLocation, -1)) {
                    isLoading = true;
                    rc.collectResource(newLocation, -1);
                }
                if (rc.canTakeAnchor(newLocation, Anchor.STANDARD)) {
                    rc.takeAnchor(newLocation, Anchor.STANDARD);
                }
                for (ResourceType resourceType : ResourceType.values()) {
                    if (rc.canTransferResource(newLocation, resourceType, 1) && rc.canSenseRobotAtLocation(newLocation) && rc.senseRobotAtLocation(newLocation).getType() == RobotType.HEADQUARTERS && rc.senseRobotAtLocation(newLocation).getTeam() == rc.getTeam()) {
                        rc.transferResource(newLocation, resourceType, 1);
                        isUnloading = true;
                        break;
                    }
                }
            }
        }
        indicatorString += " | unloading: " + String.valueOf(isUnloading) + " | loading: " + String.valueOf(isLoading);
        rc.setIndicatorString(indicatorString);


        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        boolean enemyHasAttackers = false;
        boolean selfHasAttackers = false;
        RobotInfo firstEnemy = null;
        for (RobotInfo robotInfo : nearbyRobots) {
            if (robotInfo.getType() == RobotType.LAUNCHER) {
                if (robotInfo.getTeam() == rc.getTeam()) {
                    selfHasAttackers = true;
                } else {
                    enemyHasAttackers = true;
                }
                if (selfHasAttackers && enemyHasAttackers) {
                    break;
                }
            }
            if (firstEnemy == null && robotInfo.getTeam() != rc.getTeam()) {
                firstEnemy = robotInfo;
            }
        }
        if (enemyHasAttackers && !selfHasAttackers) { //TODO: check if there are nearby friendly robots, but they can't attack
            if (rc.canAttack(firstEnemy.location)) {
                rc.attack(firstEnemy.location);
            }
        }
    }

    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length >= 1) {
            MapLocation toAttack = enemies[0].location;//TODO: bias for attacking certain robot types

            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

        if (currMoveTarget == null) {
            if (islandLocations.size() > 0 && rng.nextInt(20) == 0) {
                for (MapLocation mapLocation : islandLocations.keySet()) {
                    if (islandLocations.get(mapLocation) == Team.NEUTRAL && rng.nextInt(islandLocations.size()) == 1) { //TODO: (unrelated to code here) create channel in shared array specializing in island and well updates (priority)
                        currMoveTarget = mapLocation;
                        break;
                    }
                }
            } else {
                currMoveTarget = new MapLocation(rc.getMapHeight()-1-spawnPoint.x, rc.getMapWidth()-1-spawnPoint.y);
            }
        }
        rc.setIndicatorString(String.valueOf(currMoveTarget));
        pathfindTowardMoveTarget(rc);
        randomMove(rc);
    }

    static void exploreMove(RobotController rc) throws GameActionException {
        Vector2 avgPos = new Vector2((float) sumX / (float) turnCount, (float) sumY / (float) turnCount);
        Direction direction = Vector2.angleToDirection(new Vector2(rc.getLocation().x, rc.getLocation().y).subtract(avgPos).getAngle() + rng.nextFloat()*180-90);
        if (rc.canMove(direction)) {
            rc.move(direction);
        } else {
            randomMove(rc);
        }
    }

    static void followTeammates(RobotController rc, float[] weights) throws GameActionException{
        MapLocation me = rc.getLocation();
        Vector2 currVector = new Vector2(0,0);
        RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo robotInfo : robotInfos) {
            Vector2 normVectorToRobot = (new Vector2(robotInfo.getLocation().x-me.x, robotInfo.getLocation().y-me.y)).normalized();
            currVector = currVector.add(normVectorToRobot.multiply(weights[robotTypeToInt(robotInfo.getType())]));
        }

        Direction direction = currVector.toDirection();
        rc.setIndicatorString(currVector.x + " " + currVector.y + " " + String.valueOf(currVector.getAngle()));
        if ((currVector.x != 0 || currVector.y != 0) && rc.canMove(direction)) { //at most one other friendly amplifier
            rc.move(direction);
        } else {
            direction = rc.getLocation().directionTo(spawnPoint); //try to move back to spawn and find another robot to follow on the way there
            if (rc.canMove(direction)) {
                rc.move(direction);
            } else {
                randomMove(rc);
            }
        }
    }

    static void runBooster(RobotController rc) throws GameActionException {
        float[] weights = {-0.3f,1,1,1,1,1,1};
        followTeammates(rc, weights);
    }

    static void runDestabilizer(RobotController rc) throws GameActionException {
        float[] weights = {-0.3f,1,1,1,1,1,1};
        followTeammates(rc, weights);
    }

    static void runAmplifier(RobotController rc) throws GameActionException {
        float[] weights = {-0.3f,1,1,1,1,1,-1};
        followTeammates(rc, weights);
    }
}
