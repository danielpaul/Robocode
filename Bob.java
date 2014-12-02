
package nuim;


/* Import */
import robocode.*;
import java.awt.*;
import java.util.*;
import java.awt.geom.Point2D;
import robocode.util.Utils;
import static robocode.util.Utils.normalRelativeAngleDegrees;

/**
 * Bob - a winning team's robot
 *
 * @author Daniel Paul Jesudason
 * @author Matthew Duffy
 * @author Jonatan Sala
 */

public class Bob extends AdvancedRobot {


	/* declare global variables */
	final double PI = Math.PI; // constant
	int direction = 1; // 1 = forward, -1 = backwards
	int turnDirection = 1; // clockwise or anticlockwise
	double firePower; // the power of our shots


	// Enemy target variables
	String target_name;
	double target_bearing, target_head, target_speed, target_x, target_y;
	double target_distance = 100000;
	long target_ctime; //game time that the scan was produced



	public void run()
	{

		/* Main method */

		// Set our colours
		setBodyColor(Color.blue);
		setGunColor(Color.orange);
		setRadarColor(Color.red);
		setBulletColor(Color.red);
		setScanColor(Color.orange);


		// some settings
		/* set gun and radar to independantly move */
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		// view the whole field first

		if(!inCentre()) goTo(new Point2D.Double (400, 400));

		// view the whole field first
		turnRadarRightRadians(2*PI);

		// infinate loop to detect 
		while(true)
		{
			doAction();
			doScanner();
			
			// move gun in angle
			// works out how long it would take a bullet to travel to where the enemy is now
			// this is the best estimation we have
			long time = getTime() + (int)(target_distance/(20-(3*firePower)));
			
			//offsets the gun by the angle to the next shot based on linear targeting provided by the enemy class
			double gunOffset = getGunHeadingRadians() - absbearing(getX(),getY(),guessX(time),guessY(time));
			setTurnGunLeftRadians(NormaliseBearing(gunOffset));


			//selects a bullet power based on our distance away from the target and our enery level
			firePower = Math.min(400/target_distance, getEnergy() - .1);

			// check if gun is cool before fireing
			if (getGunHeat() == 0) {
				fire(firePower);
			}

			execute();


			if(target_distance > 650 && getEnergy() > 80)
			{
				// goTo(new Point2D.Double (400, 400));
			}

			if(getEnergy() < 50 && !inCentre())
			{
				// goTo(new Point2D.Double (400, 400));
			}



		}


	}
	
	


	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {

			//if the robot is not sentry and we have found a closer robot....
			if (!e.isSentryRobot() && (e.getDistance() < target_distance)||(target_name == e.getName())) {
				// get the absolute bearing to the point where the bot is
				double absbearing_rad = (getHeadingRadians()+e.getBearingRadians())%(2*PI);
				
				// set all the information about our target
				target_name = e.getName();
				target_x = getX()+Math.sin(absbearing_rad)*e.getDistance(); //works out the x coordinate of where the target is
				target_y = getY()+Math.cos(absbearing_rad)*e.getDistance(); //works out the y coordinate of where the target is
				target_bearing = e.getBearingRadians();
				target_head = e.getHeadingRadians();
				target_ctime = getTime(); //game time at which this scan was produced
				target_speed = e.getVelocity();
				target_distance = e.getDistance();
			}



	}


	// if our target robot is dead, set target distance so we 
	public void onRobotDeath(RobotDeathEvent e) {
		if (e.getName() == target_name)
			target_distance = 10000; //this will effectively make it search for a new target
	}



	/**
	 * onHitRobot:  Set him as our new target
	 */
	public void onHitRobot(HitRobotEvent e) {

		if (e.getName() != target_name)
		{
			out.println("Tracking " + e.getName() + " due to collision");
			target_distance = 10000; //this will effectively make it search for a new target
		}

	}


	/**
	 * onHitWall
	 */
	public void onHitWall(HitWallEvent e) {
		// Bounce off!
		//goTo(new Point2D.Double (30, getY() - 30));
	}


	/**
	 * onHitByBullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {

	}

	


	/* Robot loop functions */

	/**
	* Method to move in arching movement
	*/
	private void doAction() {
		if (getTime()%20 == 0)  {	//every twenty times
			direction *= -1;	//reverse direction
			setAhead(direction*300); // move in that direction
		}
		setTurnRightRadians(target_bearing + (PI/2)); 
		//every turn move to circle strafe the enemy
	}



	/** 
	* Method to scan for targets
	*/
	private void doScanner() {
		double radarOffset;
		if (getTime() - target_ctime > 4) { //if we haven't seen anybody for a bit....
			radarOffset = 360; //rotate the radar to find a target
		} else {	
			
			//next is the amount we need to rotate the radar by to scan where the target is now
			radarOffset = getRadarHeadingRadians() - absbearing(getX(),getY(),target_x,target_y);
			
			
			//this adds or subtracts small amounts from the bearing for the radar to produce the wobbling
			//and make sure we don't lose the target
			if (radarOffset < 0)
				radarOffset -= PI/8;
			else
				radarOffset += PI/8; 
		}
		//turn the radar
		setTurnRadarLeftRadians(NormaliseBearing(radarOffset));
	}


	



	/* Useful functions */
	private Point2D getRobotLocation() {
		return new Point2D.Double(getX(), getY());
    }


    private void goTo(Point2D point) {
        setTurnRightRadians(
            normalRelativeAngleRadians(
                absoluteBearingRadians(getRobotLocation(), point) - getHeadingRadians()
            )
         );
        waitFor(new TurnCompleteCondition(this)); // wait for the turn to complete
        setAhead(getRobotLocation().distance(point));

        waitFor(new MoveCompleteCondition(this));
    }


    /**
	* Method to check if we are in the 'safe zone'
	*/
	private boolean inCentre()
	{
		// get out position on the map
		double x = getX();
		double y = getY();

		// check axis
		if((y > 350 || y < 450) && (x > 350 || x < 450))
		{
			return false;
		}
		else
		{
			return true;
		}

	}


	/* Target prediction functions */

	/**
	*	Methods to guess our target's next position
	*/
	public double guessX(long when)
	{
		long diff = when - target_ctime;
		return target_x+Math.sin(target_head)*target_speed*diff;
	}
	public double guessY(long when)
	{
		long diff = when - target_ctime;
		return target_y+Math.cos(target_head)*target_speed*diff;
	}



	/* Math functions to do all the complex calculations :P */

	// Method to get the angle of our target point relative to us, measured in radians
	private double absoluteBearingRadians(Point2D source, Point2D target) {
        return Math.atan2(target.getX() -
            source.getX(), target.getY() - source.getY());
    }

    // gets the arctan of passed agngle's sin (x) and cos (y)
    private double normalRelativeAngleRadians(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }


    //returns the distance between two x and y coordinates
	public double getrange( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		double h = Math.sqrt( xo*xo + yo*yo );
		return h;	
	}


    //gets the absolute bearing between to x,y coordinates
	public double absbearing( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		double h = getrange( x1,y1, x2,y2 );
		if( xo > 0 && yo > 0 )
		{
			return Math.asin( xo / h );
		}
		if( xo > 0 && yo < 0 )
		{
			return Math.PI - Math.asin( xo / h );
		}
		if( xo < 0 && yo < 0 )
		{
			return Math.PI + Math.asin( -xo / h );
		}
		if( xo < 0 && yo > 0 )
		{
			return 2.0*Math.PI - Math.asin( -xo / h );
		}
		return 0;
	}

	//if a bearing is not within the -pi to pi range, alters it to provide the shortest angle
	double NormaliseBearing(double ang) {
		if (ang > PI)
		{
			ang = ang - 2*PI;
		}
		if (ang < -PI)
		{
			ang = ang + 2*PI;
		}
		return ang;
	}
	
	//if a heading is not within the 0 to 2pi range, alters it to provide the shortest angle
	double NormaliseHeading(double ang) {
		if (ang > 2*PI)
		{
			ang = ang - 2*PI;
		}
		if (ang < 0)
		{
			ang = ang + 2*PI;
		}
		return ang;
	}




}