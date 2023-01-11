package gopherbot;

import battlecode.common.*;

import java.util.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

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

    static int[] buildRatios = {5, 5, 3,2,1}; //in order of buildPriorities
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
                if (rc.getType() != RobotType.HEADQUARTERS) {
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

        String robotCountData = "";
        for (int i = 1; i <= 5; i ++) {
            robotCountData += rc.readSharedArray(i) + " ";
        }
        rc.setIndicatorString(robotCountData);

        MapLocation newLoc = null;
        for (Direction dir : directions) {
            newLoc = rc.getLocation().add(dir);
            if (rc.sensePassability(newLoc) && rc.senseRobotAtLocation(newLoc) == null) {
                break;
            }
        }
        for (int i = 0; i <= 4; i++) {
            int refInd = (i + 1) % 5;
            int currAmount = rc.readSharedArray(robotTypeToInteger.get(buildPriorities[i]));
            if (currAmount < 5) {
                if (rc.canBuildRobot(buildPriorities[i], newLoc)) {
                    rc.buildRobot(buildPriorities[i], newLoc);
                    break;
                }
            }
            int refAmount = rc.readSharedArray(robotTypeToInteger.get(buildPriorities[refInd]));
            if (currAmount*buildRatios[refInd] < refAmount * buildRatios[i]) {
                if (rc.canBuildRobot(buildPriorities[i], newLoc)) {
                    rc.buildRobot(buildPriorities[i], newLoc);
                    break;
                }
            }
        }

        rc.writeSharedArray(0, rc.readSharedArray(0)+1);
        if (isLastToRun) {
            for (int i = 1; i <= 5; i ++) {
                rc.writeSharedArray(i, 0);
            }
        }
    }

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
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
                while (!rc.getLocation().equals(islandLocation)) { //TODO: general navigation
                    Direction dir = rc.getLocation().directionTo(islandLocation);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    } else {
                        break;
                    }
                }
                if (rc.canPlaceAnchor()) {
                    rc.placeAnchor();
                }
            }
        }
        // Try to gather from squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation newLocation = new MapLocation(me.x + dx, me.y + dy);
                if (rc.canCollectResource(newLocation, -1)) {
                    if (rng.nextBoolean()) {
                        rc.collectResource(newLocation, -1);
                    }
                }
                if (rc.canTakeAnchor(newLocation, Anchor.STANDARD)) {
                    rc.takeAnchor(newLocation, Anchor.STANDARD);
                }
            }
        }
//        // Occasionally try out the carriers attack
//        if (rng.nextInt(20) == 1) {
//            RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
//            if (enemyRobots.length > 0) {
//                if (rc.canAttack(enemyRobots[0].location)) {
//                    rc.attack(enemyRobots[0].location);
//                }
//            }
//        }
        
        // If we can see a well, move towards it
        WellInfo[] wells = rc.senseNearbyWells();
        if (wells.length > 1 && rng.nextInt(3) == 1) {
            WellInfo well_one = wells[1];
            Direction dir = me.directionTo(well_one.getMapLocation());
            if (rc.canMove(dir)) 
                rc.move(dir);
        }
        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
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

    static void runBooster(RobotController rc) throws GameActionException {
    }

    static void runDestabilizer(RobotController rc) throws GameActionException {
    }

    static void runAmplifier(RobotController rc) throws GameActionException {
    }
}
