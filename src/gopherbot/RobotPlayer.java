package gopherbot;

import battlecode.common.*;

import java.util.*;

public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;
    static int sumX = 0;
    static int sumY = 0;
    static final int hqInfoStart = 7;
    static final int mapInfoStart1 = 15; //where the map info queue starts in the shared array; WELL AND ISLAND ONLY
    static final int mapInfoStart2 = 27;
    static final int sharedArraySize = 64;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (69420); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static Random rng;

    static float[] buildRatios = {5, 10, 1, 2, 2};

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

    static void readBroadcastedHQInfos(RobotController rc) throws GameActionException{
        for (int i = hqInfoStart; i < mapInfoStart1; i ++ ){
            int currVal = rc.readSharedArray(i);
            if (currVal != 0){
                int x = currVal >> 10;
                int y = (currVal >> 4) & 0b111111;
                if ((currVal & 0b1111) == 1) {
                    //selfHQs.add(new MapLocation(x,y));
                } else {
                    opponentHQs.add(new MapLocation(x,y));
                }
            }
        } 
    }
    static int hqInfoToInt(RobotInfo robotInfo, Team selfTeam) {
        MapLocation location = robotInfo.getLocation();
        int result = location.x*(1<<10) + location.y * (1<<4);
        if (robotInfo.getTeam() == selfTeam) {
            result++;
        } else {
            result += 2;
        }
        return result;
    }


    static void broadcastHQInfos(RobotController rc, Team team) throws GameActionException {
        if (!rc.canWriteSharedArray(0, 0)) return;
        int firstAvailableInd = hqInfoStart;
        while (firstAvailableInd < mapInfoStart1 && rc.readSharedArray(firstAvailableInd) != 0) {
            firstAvailableInd++;
        }
        
        RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, team);
        for (RobotInfo robotInfo : robotInfos) {
            if (firstAvailableInd >= mapInfoStart1) {
                break;
            }
            if (robotInfo.getType() == RobotType.HEADQUARTERS) {
                //System.out.println(robotInfo.getLocation());
                rc.writeSharedArray(firstAvailableInd, hqInfoToInt(robotInfo,rc.getTeam()));
                firstAvailableInd++;
            }
        }
    }
    

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if (rc.getType() == RobotType.HEADQUARTERS) {
            numHeadquarters = rc.getRobotCount();
        }
        rng = new Random(rc.getID());
        launcherIsSuicidal = rng.nextInt(100) <= 59;
        spawnPoint = rc.getLocation();
        staticInfoGrid = new int[rc.getMapWidth()][rc.getMapHeight()];
        while (true) {
            turnCount += 1;  // We have now been alive for one more turn!
            sumX += rc.getLocation().x;
            sumY += rc.getLocation().y;

            try {
                if (rc.getType() != RobotType.HEADQUARTERS && rc.canWriteSharedArray(0, 0)) {
                    rc.writeSharedArray(robotTypeToInt(rc.getType()), rc.readSharedArray(robotTypeToInt(rc.getType()))+1);
                }
                if (rc.getType() != RobotType.HEADQUARTERS && rc.getRoundNum() % 2 == 1) {
                    if (rng.nextInt(rc.getRobotCount()) <= 10-1) {
                        broadcastMapInfos(rc);
                    }
                    // if (rng.nextInt(rc.getRobotCount()) <= 5-1) {
                    //     broadcastHQInfos(rc, rc.getTeam());
                    // }
                    broadcastHQInfos(rc, rc.getTeam().opponent());
                }
                if (rc.getRoundNum() % 2 == 0) {
                    readBroadcastedMapInfos(rc);
                    readBroadcastedHQInfos(rc);
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


    //static Set<MapLocation> impassibleLocations = new TreeSet<MapLocation>();
    //static Set<Current> currentLocations = new TreeSet<Current>();
    static Map<MapLocation, ResourceType> wellLocations = new TreeMap<MapLocation, ResourceType>();
    static Map<MapLocation, Team> islandLocations = new TreeMap<MapLocation, Team>(); //neutral island means unoccupied or occupied by other team
    //static Set<MapLocation> cloudLocations = new TreeSet<MapLocation>();

    static int[][] staticInfoGrid;
    //static Set<MapLocation> selfHQs = new TreeSet<MapLocation>();
    static Set<MapLocation> opponentHQs = new TreeSet<MapLocation>();

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

            //impassibleLocations.add(broadcastedMapInfo.location);
            staticInfoGrid[broadcastedMapInfo.location.x][broadcastedMapInfo.location.y] = broadcastedMapInfo.locType;

        } else if (broadcastedMapInfo.locType == 2) { //cloud

            //cloudLocations.add(broadcastedMapInfo.location);
            staticInfoGrid[broadcastedMapInfo.location.x][broadcastedMapInfo.location.y] = broadcastedMapInfo.locType;

        } else if (broadcastedMapInfo.locType >= 3 && broadcastedMapInfo.locType <= 10) { //current

            //currentLocations.add(new Current(intToDirection(broadcastedMapInfo.locType), broadcastedMapInfo.location));
            staticInfoGrid[broadcastedMapInfo.location.x][broadcastedMapInfo.location.y] = broadcastedMapInfo.locType;

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

        // for (MapLocation well : wellLocations.keySet()) {
        //     rc.setIndicatorDot(well, 255, 255, 255);
        // }


        // for (MapLocation islandLoc : islandLocations.keySet()) {
        //     if (islandLocations.get(islandLoc) == Team.A) {
        //         rc.setIndicatorDot(islandLoc, 255, 0, 0);
        //     } else {
        //         rc.setIndicatorDot(islandLoc, 0, 0, 255);
        //     }
        // }

        // for (MapLocation selfHQ :selfHQs) {
        //     rc.setIndicatorDot(selfHQ, 255, 255, 255);
        // }
        // for (MapLocation opponentHQ : opponentHQs) {
        //     rc.setIndicatorDot(opponentHQ, 0, 0, 0);
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
                for (int i = hqInfoStart; i < sharedArraySize; i ++) {
                    if (rc.readSharedArray(i) != 0) {
                        rc.writeSharedArray(i, 0);
                    }
                }

                int i = 0;
                for (MapLocation opponentHQ : opponentHQs) {
                    int encoded = opponentHQ.x * (1<<10) + opponentHQ.y * (1<<4)+2;
                    rc.writeSharedArray(hqInfoStart+i, encoded);
                    i++;
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
                    int numOccupiedIslands = 0;
                    for (MapLocation islandLocation : islandLocations.keySet()) {
                        if (islandLocations.get(islandLocation) != rc.getTeam()) {
                            numOccupiedIslands++;
                            break;
                        }
                    }
                    float anchorDiff= 0 ;
                    if (numOccupiedIslands > 0 && rc.readSharedArray(6) + rc.getNumAnchors(Anchor.STANDARD) == 0) {
                        anchorDiff = 1000;
                    }
                    indicatorString += "| " + round(anchorDiff) + "," + round(maxBuildDiff);
    
                    if (anchorDiff > maxBuildDiff) {
                        if (rc.canBuildAnchor(Anchor.STANDARD)) {
                            rc.buildAnchor(Anchor.STANDARD);
                            break;
                        }
                        continue;
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
    static int moveTargetRange = 0;

    static boolean isUnloading = false;
    static boolean isLoading = false;

    static int[][] currDists;
    static MapLocation currDistsTarget;

    static final int bigDist = 100000;

    static void calcDists(RobotController rc, MapLocation pos) {
        currDistsTarget = pos;
        if (currDists == null) {
            currDists = new int[rc.getMapWidth()][rc.getMapHeight()];
        }
        for (int i = 0; i < rc.getMapWidth(); i ++) {
            for (int j = 0 ;j < rc.getMapHeight(); j ++) {
                currDists[i][j] = bigDist+1;
            }
        }

        Deque<MapLocation> bfs = new ArrayDeque<MapLocation>();

        bfs.add(currMoveTarget);
        currDists[currMoveTarget.x][currMoveTarget.y] = 0;

        while (!bfs.isEmpty()) {
            MapLocation currLocation = bfs.getFirst();
            bfs.pop();
            for (Direction direction : directions) {
                MapLocation newLocation = currLocation.add(direction);
                if (rc.onTheMap(newLocation) && staticInfoGrid[newLocation.x][newLocation.y] != 1 && currDists[newLocation.x][newLocation.y] == bigDist+1) {
                    bfs.add(newLocation);
                    currDists[newLocation.x][newLocation.y] = currDists[currLocation.x][currLocation.y]+1;
                }
            }
        }
    }

    static void pathfindTowardMoveTarget(RobotController rc) throws GameActionException {
        if (currMoveTarget != null && !rc.getLocation().equals(currMoveTarget)) { //TODO: general navigation
            // if (currDistsTarget != currMoveTarget) {
            //     calcDists(rc, currMoveTarget);
            // }
            // Direction minCostDir = null;
            // int currMinCost = bigDist;
            // boolean isTargetReachable = false;
            // for (Direction direction : directions) {
            //     MapLocation newPos = rc.getLocation().add(direction);
            //     if (rc.onTheMap(newPos) && currDists[newPos.x][newPos.y] < currMinCost) {
            //         if (rc.canMove(direction)) {
            //             currMinCost = currDists[newPos.x][newPos.y];
            //             minCostDir = direction;
            //         }
            //         isTargetReachable = true;
            //     }
            // }
            // if (minCostDir != null && rc.canMove(minCostDir)) {
            //     rc.move(minCostDir);
            // }
            // if (!isTargetReachable) {
            //     currMoveTarget = null;
            // }
            Direction direction = rc.getLocation().directionTo(currMoveTarget);
            if (rc.canMove(direction)) {
                rc.move(direction);
            } else {
                randomMove(rc);
            }
        }
        if (currMoveTarget != null && rc.getLocation().distanceSquaredTo(currMoveTarget) <= moveTargetRange) {
            currMoveTarget = null;
            moveTargetRange = 0;
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
            if (rc.canPlaceAnchor() && rc.senseTeamOccupyingIsland(rc.senseIsland(rc.getLocation())) != rc.getTeam()) { //TODO: get this to work
                rc.placeAnchor();
            }
            //TODO: also don't let a launcher move to the same island
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
                if (wellLocations.size() >= 1) {
                    ResourceType targetType = ResourceType.values()[turnCount%2+1]; //TODO: include elixir wells when converted
                    int minWellDistSquared = Integer.MAX_VALUE;
                    for (MapLocation well : wellLocations.keySet()) {
                        if (wellLocations.get(well) == targetType && well.distanceSquaredTo(rc.getLocation())<minWellDistSquared && well.distanceSquaredTo(rc.getLocation())<200) {
                            foundWellTarget = true;
                            currMoveTarget = well;
                            moveTargetRange = 2;
                            minWellDistSquared = well.distanceSquaredTo(rc.getLocation());
                        }
                    }
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
                    currMoveTarget = null;
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


        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1);
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
        if (enemyHasAttackers && !selfHasAttackers) {
            if (rc.canAttack(firstEnemy.location)) {
                rc.attack(firstEnemy.location);
            }
        }
    }
    

    static boolean launcherIsSuicidal;
    static boolean launcherIsFindingHQ = false;

    static void targetRandomPossHQ(RobotController rc) {
        int moveTargetInd = rng.nextInt(3);
        if (moveTargetInd == 0) {
            currMoveTarget = new MapLocation(rc.getMapWidth() - 1 - spawnPoint.x, rc.getMapHeight() - 1 - spawnPoint.y);
        } else if (moveTargetInd == 1) {
            currMoveTarget = new MapLocation(spawnPoint.x, rc.getMapHeight() - 1 - spawnPoint.y);
        } else {
            currMoveTarget = new MapLocation(rc.getMapWidth() - 1 - spawnPoint.x, spawnPoint.y);
        }
        launcherIsFindingHQ = true;
    }

    static void targetRandomPos(RobotController rc) {
        currMoveTarget = null;
        while (currMoveTarget == null || !rc.onTheMap(currMoveTarget) || staticInfoGrid[currMoveTarget.x][currMoveTarget.y] == 1) {
            currMoveTarget = rc.getLocation().translate(rng.nextInt(21)-10, rng.nextInt(21)-10);
        }
    }

    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length >= 1) {
            int[] robotTypeToInd = {-1, -1, -1, -1, -1, -1};
            int i  =0;
            for (RobotInfo enemy : enemies) {
                robotTypeToInd[robotTypeToInt(enemy.getType())] = i;
                i++;
            }

            MapLocation toAttack = null;

            if (robotTypeToInd[2] != -1) {
                toAttack = enemies[robotTypeToInd[2]].getLocation();
            } else if (robotTypeToInd[1] != -1) {
                toAttack = enemies[robotTypeToInd[1]].getLocation();
            } else if (robotTypeToInd[3] != -1) {
                toAttack = enemies[robotTypeToInd[3]].getLocation();
            } else if (robotTypeToInd[4] != -1) {
                toAttack = enemies[robotTypeToInd[4]].getLocation();
            } else if (robotTypeToInd[5] != -1) {
                toAttack = enemies[robotTypeToInd[5]].getLocation();
            }

            if (toAttack != null && rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }
        
        if (!launcherIsSuicidal && currMoveTarget == null) {
            int numNonOccupiedIslands = 0;
            int numOccupiedIslands = 0;
            for (MapLocation mapLocation : islandLocations.keySet()) {
                if (islandLocations.get(mapLocation) == Team.NEUTRAL) {
                    numNonOccupiedIslands++;
                } else {
                    numOccupiedIslands++;
                }
            }
            if (numNonOccupiedIslands > 0) {
                int i = 0;
                int targetIsland = rng.nextInt(numNonOccupiedIslands);
                for (MapLocation mapLocation : islandLocations.keySet()) {
                    if (islandLocations.get(mapLocation) == Team.NEUTRAL) {
                        if (i == targetIsland) {
                            currMoveTarget = mapLocation;
                            break;
                        }
                        i++;
                    }
                }
            } else if (numOccupiedIslands > 0 && rng.nextInt(3) == 0) {
                int i = 0;
                int targetIsland = rng.nextInt(numOccupiedIslands);
                for (MapLocation mapLocation : islandLocations.keySet()) {
                    if (islandLocations.get(mapLocation) == rc.getTeam()) {
                        if (i == targetIsland) {
                            currMoveTarget = mapLocation;
                            break;
                        }
                        i++;
                    }
                }
            } else {
                exploreMove(rc);
            }
        } else if (launcherIsSuicidal) {
            if (currMoveTarget == null && opponentHQs.size() == 0) {
                targetRandomPossHQ(rc);
            } else if ((launcherIsFindingHQ && opponentHQs.size() >= 1) || currMoveTarget == null) {
                launcherIsFindingHQ = false;

                int i = 0;
                int moveTargetInd = rng.nextInt(opponentHQs.size());
                for (MapLocation opponentHQ : opponentHQs) {
                    if (moveTargetInd == i) {
                        currMoveTarget = opponentHQ;
                        break;
                    }
                    i++;
                }
            }
        }
        rc.setIndicatorString(String.valueOf(currMoveTarget) + " " + launcherIsFindingHQ);
        pathfindTowardMoveTarget(rc);
        // float[] followWeights = {-1, -1, -1, -1, -1, -1};
        // followTeammates(rc, followWeights);
    }

    static void exploreMove(RobotController rc) throws GameActionException {
        Vector2 avgPos = new Vector2((float) sumX / (float) turnCount, (float) sumY / (float) turnCount);
        Direction direction = Vector2.angleToDirection(new Vector2(rc.getLocation().x, rc.getLocation().y).subtract(avgPos).getAngle());
        if (rc.canMove(direction)) {
            rc.move(direction);
        } else {
            randomMove(rc);
        }
    }

    static void followTeammates(RobotController rc, float[] weights) throws GameActionException {
        MapLocation me = rc.getLocation();
        Vector2 currVector = new Vector2(0,0);
        RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, rc.getTeam());
        int numRobotsSensed = 0;
        for (RobotInfo robotInfo : robotInfos) {
            if (robotInfo.getLocation() == me) {
                continue;
            }
            Vector2 vectorToRobot = new Vector2(robotInfo.getLocation().x-me.x, robotInfo.getLocation().y-me.y);
            Vector2 normVectorToRobot = vectorToRobot.normalized();
            Vector2 weightedNormVectorToRobot = normVectorToRobot.multiply(weights[robotTypeToInt(robotInfo.getType())]);
            currVector = currVector.add(weightedNormVectorToRobot);
            numRobotsSensed ++;
        }

        Direction direction = currVector.toDirection();
        rc.setIndicatorString(currVector.x + " " + currVector.y + " " + String.valueOf(currVector.getAngle()) + " " + numRobotsSensed);
        if ((currVector.x != 0 || currVector.y != 0) && rc.canMove(direction)) {
            rc.move(direction);
        } else if (currVector.x == 0 && currVector.y == 0) {
            randomMove(rc);
        }
    }

    static void runBooster(RobotController rc) throws GameActionException {
        float[] weights = {-0.3f,1,1,1,1,1};
        followTeammates(rc, weights);
    }

    static void runDestabilizer(RobotController rc) throws GameActionException {
        float[] weights = {-0.3f,1,1,1,1,1};
        followTeammates(rc, weights);
    }

    static void runAmplifier(RobotController rc) throws GameActionException {
        float[] weights = {0,1,3,1,1,-5};
        followTeammates(rc, weights);
    }
}
