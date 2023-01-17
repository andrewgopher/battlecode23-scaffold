package gopherbot;

import battlecode.common.*;
import battlecode.schema.Vec;

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
        public static Vector2 normalized() {
            return new Vector2(1/getLength()*x, 1/getLength()*y);
        }

        public static float getAngle() {
            return radiansToDegrees((float) Math.atan2(y,x));
        }

        public static Direction toDirection() {
            int degree45 = ((int) (Math.round(getAngle() / 45)*45)+720)%360;
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
    }

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;
    static final int mapInfoStart = 7; //where the map info queue starts in the shared array
    static final int sharedArraySize = 64;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(69420);

    static float[] buildRatios = {5, 6, 2,3,4};

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
        while (true) {
            turnCount += 1;  // We have now been alive for one more turn!

            try {
                if (rc.getType() != RobotType.HEADQUARTERS && rc.canWriteSharedArray(0, 0)) {
                    rc.writeSharedArray(robotTypeToInt(rc.getType()), rc.readSharedArray(robotTypeToInt(rc.getType()))+1);
                }
                if (rc.getType() != RobotType.HEADQUARTERS && rc.getRoundNum() % 2 == 1 && (turnCount%5 == 4)) {
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

        int firstAvailableInd = 64;
        for (int i = mapInfoStart; i < 64; i ++) {
            if (rc.readSharedArray(i) == 0) {
                firstAvailableInd = i;
                break;
            }
        }
        if (firstAvailableInd == 64) {
            return;
        }
        MapInfo[] mapInfos = rc.senseNearbyMapInfos();
        for (MapInfo mapInfo : mapInfos) {
            int encoded = mapInfoToInt(rc, mapInfo);
            if (encoded != -1) {
                if (rc.canWriteSharedArray(firstAvailableInd,encoded)) {
                    rc.writeSharedArray(firstAvailableInd, encoded);
                }
                firstAvailableInd++;
                if (firstAvailableInd == 64) {
                    return;
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

    private static class Well implements Comparable<Well> {
        Well(MapLocation argLocation, ResourceType argResourceType) {
            location = argLocation;
            resourceType = argResourceType;
        }

        @Override
        public int hashCode() {
            return location.hashCode() ^ resourceType.hashCode();
        }

        public int compareTo(Well other) {
            if (location != other.location) {
                return location.compareTo(other.location);
            } else {
                return resourceType.compareTo(other.resourceType);
            }
        }

        MapLocation location;
        ResourceType resourceType;
    }


    static Set<MapLocation> impassibleLocations = new TreeSet<MapLocation>();
    static Set<Current> currentLocations = new TreeSet<Current>();
    static Set<Well> wellLocations = new TreeSet<Well>();
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

            wellLocations.add(new Well(broadcastedMapInfo.location, intToResourceType(broadcastedMapInfo.locType)));

        } else if (broadcastedMapInfo.locType >= 14 && broadcastedMapInfo.locType <= 15) { //island

            // islandLocations.put(broadcastedMapInfo.location, intToTeam(rc.getTeam(), broadcastedMapInfo.locType));
            dfsIsland(rc, broadcastedMapInfo.location, intToTeam(rc.getTeam(), broadcastedMapInfo.locType));

        }
    }

    static void readBroadcastedMapInfos(RobotController rc) throws GameActionException {
        int byteCodeStart = Clock.getBytecodeNum();
        for (int i = mapInfoStart; i < 64; i ++) {
            int currVal = rc.readSharedArray(i);
            if (currVal == 0) {
                return;
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

        // for (Well well : wellLocations) {
        //     rc.setIndicatorDot(well.location, 255, 255, 255);
        // }

        // for (MapLocation mapLocation : cloudLocations) {
        //     rc.setIndicatorDot(mapLocation, 255, 255, 255);
        // }

        // for (Current current : currentLocations) {
        //     rc.setIndicatorDot(current.location, 255, 255, 255);
        // }

        // for (MapLocation islandLoc : islandLocations.keySet()) {
        //     if (islandLocations.get(islandLoc) == Team.A) {
        //         rc.setIndicatorDot(islandLoc, 255, 0, 0);
        //     } else {
        //         rc.setIndicatorDot(islandLoc, 0, 0, 255);
        //     }
        // }
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
                for (int i = mapInfoStart; i < sharedArraySize; i ++) {
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
            if (rc.sensePassability(newLoc) && rc.senseRobotAtLocation(newLoc) == null) {
                break;
            }
        }

        if (rc.sensePassability(newLoc) && rc.senseRobotAtLocation(newLoc) == null) {
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
                    Set<MapLocation> visited = new TreeSet<MapLocation>();
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
                    float anchorDiff = (rc.readSharedArray(1)*0.1f - rc.readSharedArray(6))/(float)numHeadquarters - rc.getNumAnchors(Anchor.STANDARD);
                    indicatorString += "| " + round(anchorDiff) + "," + round(maxBuildDiff);
    
                    if (anchorDiff > maxBuildDiff && anchorDiff < rc.readSharedArray(robotTypeToInt(RobotType.CARRIER))) {
                        if (rc.canBuildAnchor(Anchor.STANDARD)) {
                            rc.buildAnchor(Anchor.STANDARD);
                        }
                        break;
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

    static boolean isUnloading = false;
    static void runCarrier(RobotController rc) throws GameActionException {
        if (rc.getAnchor() != null) {
            if (rc.canWriteSharedArray(0,0)) {
                rc.writeSharedArray(6, rc.readSharedArray(6)+1);
            }
            // If I have an anchor singularly focus on getting it to the first island I see
            int[] islands = rc.senseNearbyIslands();
            Set<MapLocation> islandLocs = new HashSet<>();
            for (int id : islands) {
                MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
                islandLocs.addAll(Arrays.asList(thisIslandLocs));
            }
            if (islandLocs.size() > 0) {
                MapLocation islandLocation = null;
                for (MapLocation mapLocation : islandLocs) {
                    if (rc.senseTeamOccupyingIsland(rc.senseIsland(mapLocation)) == Team.NEUTRAL) {
                        islandLocation = mapLocation;
                    }
                }
                if (islandLocation != null) {
                    if (!rc.getLocation().equals(islandLocation)) { //TODO: general navigation
                        Direction dir = rc.getLocation().directionTo(islandLocation);
                        if (rc.canMove(dir)) {
                            rc.move(dir);
                        }
                    }
                    if (rc.canPlaceAnchor() && rc.senseTeamOccupyingIsland(rc.senseIsland(islandLocation)) == Team.NEUTRAL) {
                        rc.placeAnchor();
                    }
                } else {
                    randomMove(rc);
                }
            } else {
                randomMove(rc);
            }
        }
        // Try to gather from and transfer to squares around us.
        MapLocation me = rc.getLocation();
        isUnloading = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation newLocation = new MapLocation(me.x + dx, me.y + dy);
                if (rc.canCollectResource(newLocation, -1)) {
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
        rc.setIndicatorString("Is unloading: " + String.valueOf(isUnloading));
       // Occasionally try out the carriers attack
       if (turnCount % 5 == 1) {
           RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
           if (enemyRobots.length > 0 && rc.senseNearbyRobots(-1, rc.getTeam()).length == 0) { //TODO: check if there are nearby friendly robots, but they can't attack
               if (rc.canAttack(enemyRobots[0].location)) {
                   rc.attack(enemyRobots[0].location);
               }
           }
       }

        if (!isUnloading && rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR) < 40) {
            // If we can see a well, move towards it
            WellInfo[] wells = rc.senseNearbyWells();
            if (wells.length >= 1) {
                WellInfo well_one = wells[0];
                Direction dir = me.directionTo(well_one.getMapLocation());
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        } else {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1);
            for (RobotInfo robot : nearbyRobots) {
                if (robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.HEADQUARTERS) {
                    Direction dir = me.directionTo(robot.getLocation());
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        break;
                    }
                }
            }
        }


        // Also try to move randomly.
        if (!isUnloading) {
            randomMove(rc);
        }
    }

    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length >= 1) {
            MapLocation toAttack = enemies[0].location;

            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }

    static void followTeammates(RobotController rc) throws GameActionException{
        MapLocation me = rc.getLocation();
        Vector2 currVector = new Vector2(0,0);
        RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo robotInfo : robotInfos) {
            Vector2 normVectorToRobot = (new Vector2(robotInfo.getLocation().x-me.x, robotInfo.getLocation().y-me.y)).normalized();
            if (robotInfo.getType() == RobotType.AMPLIFIER || robotInfo.getType() == RobotType.HEADQUARTERS) {
                // currVector = currVector.subtract(normVectorToRobot);
            } else {
                currVector = currVector.add(normVectorToRobot);
            }
        }

        Direction direction = currVector.toDirection();
        rc.setIndicatorString(currVector.x + " " + currVector.y + " " + String.valueOf(currVector.getAngle()));
        if (rc.canMove(direction) && (currVector.x != 0 || currVector.y != 0)) {
            rc.move(direction);
        } else {
            randomMove(rc);
        }
    }

    static void runBooster(RobotController rc) throws GameActionException {
        followTeammates(rc);
    }

    static void runDestabilizer(RobotController rc) throws GameActionException {
        followTeammates(rc);
    }

    static void runAmplifier(RobotController rc) throws GameActionException {
        followTeammates(rc);
    }
}
