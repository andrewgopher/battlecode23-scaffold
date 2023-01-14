package gopherbot;

import battlecode.common.*;
import battlecode.schema.Vec;

import java.util.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {
    public static float radiansToDegrees(float radians) {
        return (float) (radians*180/Math.PI);
    }

    public static strictfp class Vector2 {
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
        public static Vector2 normalized() {
            return  new Vector2(1/getLength()*x, 1/getLength()*y);
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

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(69420);

    static float[] buildRatios = {8, 6, 2,3,5};
    static RobotType[] buildPriorities = {RobotType.CARRIER, RobotType.LAUNCHER, RobotType.AMPLIFIER, RobotType.BOOSTER, RobotType.DESTABILIZER};

    static Map<RobotType, Integer> robotTypeToInteger;

    static {
        robotTypeToInteger = new HashMap<RobotType, Integer>();
        robotTypeToInteger.put(RobotType.HEADQUARTERS, 0);
        robotTypeToInteger.put(RobotType.CARRIER, 1);
        robotTypeToInteger.put(RobotType.LAUNCHER, 2);
        robotTypeToInteger.put(RobotType.BOOSTER, 3);
        robotTypeToInteger.put(RobotType.DESTABILIZER, 4);
        robotTypeToInteger.put(RobotType.AMPLIFIER, 5);
    }

    static Map<Integer, RobotType> integerToRobotType;

    static {
        integerToRobotType = new HashMap<Integer, RobotType>();
        integerToRobotType.put(0, RobotType.HEADQUARTERS);
        integerToRobotType.put(1, RobotType.CARRIER);
        integerToRobotType.put(2, RobotType.LAUNCHER);
        integerToRobotType.put(3, RobotType.BOOSTER);
        integerToRobotType.put(4, RobotType.DESTABILIZER);
        integerToRobotType.put(5, RobotType.AMPLIFIER);
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

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if (rc.getType() == RobotType.HEADQUARTERS) {
            numHeadquarters = rc.getRobotCount();
        }
        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                if (rc.getType() != RobotType.HEADQUARTERS && rc.canWriteSharedArray(0, 0)) {
                    rc.writeSharedArray(robotTypeToInteger.get(rc.getType()), rc.readSharedArray(robotTypeToInteger.get(rc.getType()))+1);
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

    /**
     * Run a single turn for a Headquarters.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */

    static void runHeadquarters(RobotController rc) throws GameActionException {
        boolean isLastToRun = (rc.readSharedArray(0) == numHeadquarters-1);
        if (rc.readSharedArray(0) == numHeadquarters) { //is first to run
            rc.writeSharedArray(0, 0);
        }
        // Pick a direction to build in.

        if (rc.canBuildAnchor(Anchor.STANDARD)) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
        }

        //set indicator string to robot counts
        String indicatorString = "";
        indicatorString += "Est counts: ";
        for (int i = 1; i <= 5; i ++) {
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


        float[] reccAmounts = {0, 0, 0, 0, 0};
        int totalCountedRobots = 0;
        float totalRatio = 0;
        for (int i = 1; i <= 5; i ++) {
            totalCountedRobots += rc.readSharedArray(i);
            totalRatio += buildRatios[i-1];
        }
        int newRobotCount = totalCountedRobots+1;

        //calculate recommended amounts
        indicatorString += "recc amts: ";
        for (int i = 1; i <= 5; i ++) {
            reccAmounts[i-1] = ((float) newRobotCount) * buildRatios[i-1]/totalRatio;
            indicatorString += ((float)Math.round(reccAmounts[i-1]*10))/10 + " ";
        }


        float maxBuildDiff = -Float.MAX_VALUE;
        RobotType maxBuildDiffType = RobotType.CARRIER;
        for (int i = 0; i <= 4; i ++) {
            float currDiff = reccAmounts[i] - ((float) rc.readSharedArray(i+1));
            if (currDiff > maxBuildDiff) {
                maxBuildDiff = currDiff;
                maxBuildDiffType = integerToRobotType.get(i+1);
            }
        }
        if (rc.canBuildRobot(maxBuildDiffType, newLoc)) {
            rc.buildRobot(maxBuildDiffType, newLoc);
        }

        //increase headquarter run count
        rc.writeSharedArray(0, rc.readSharedArray(0)+1);
        if (rc.readSharedArray(0) == numHeadquarters) {
            for (int i = 1; i <= 5; i ++) {
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

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static boolean isUnloading = false;
    static void runCarrier(RobotController rc) throws GameActionException {
        if (rc.getAnchor() != null) {
            // If I have an anchor singularly focus on getting it to the first island I see
            int[] islands = rc.senseNearbyIslands();
            Set<MapLocation> islandLocs = new HashSet<>();
            for (int id : islands) {
                MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
                islandLocs.addAll(Arrays.asList(thisIslandLocs));
            }
            if (islandLocs.size() > 0) {
                MapLocation islandLocation = islandLocs.iterator().next();
                if (!rc.getLocation().equals(islandLocation)) { //TODO: general navigation
                    Direction dir = rc.getLocation().directionTo(islandLocation);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }
                if (rc.canPlaceAnchor()) {
                    rc.placeAnchor();
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
                    if (rc.canTransferResource(newLocation, resourceType, 1) && rc.canSenseRobotAtLocation(newLocation) && rc.senseRobotAtLocation(newLocation).getType() == RobotType.HEADQUARTERS) {
                        rc.transferResource(newLocation, resourceType, 1);
                        isUnloading = true;
                        break;
                    }
                }
            }
        }
        rc.setIndicatorString("Is unloading: " + String.valueOf(isUnloading));
//        // Occasionally try out the carriers attack
//        if (rng.nextInt(20) == 1) {
//            RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
//            if (enemyRobots.length > 0) {
//                if (rc.canAttack(enemyRobots[0].location)) {
//                    rc.attack(enemyRobots[0].location);
//                }
//            }
//        }

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

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length >= 0) {
            // MapLocation toAttack = enemies[0].location;
            MapLocation toAttack = rc.getLocation().add(Direction.EAST);

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
            currVector = currVector.add((new Vector2(robotInfo.getLocation().x-me.x, robotInfo.getLocation().y-me.y)).normalized());
        }

        Direction direction = currVector.toDirection();
        rc.setIndicatorString(currVector.x + " " + currVector.y + " " + String.valueOf(currVector.getAngle()));
        if (rc.canMove(direction)) {
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
