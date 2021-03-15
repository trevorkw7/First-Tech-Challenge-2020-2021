package org.firstinspires.ftc.teamcode.OpModes.TeleOp;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.acmerobotics.roadrunner.util.Angle;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Util;

import org.firstinspires.ftc.teamcode.Commands.ShootCommand;
import org.firstinspires.ftc.teamcode.Common.UtilMethods;
import org.firstinspires.ftc.teamcode.Subsystems.Collector;
import org.firstinspires.ftc.teamcode.Subsystems.Drive.PoseLibrary;
import org.firstinspires.ftc.teamcode.Subsystems.Drive.MecanumDrivebase;
import org.firstinspires.ftc.teamcode.Subsystems.Shooter.Flywheel;
import org.firstinspires.ftc.teamcode.Subsystems.Shooter.Hopper;
import org.firstinspires.ftc.teamcode.Subsystems.WobbleArm;
import org.firstinspires.ftc.teamcode.Vision.BlueGoalVisionPipeline;
import org.firstinspires.ftc.teamcode.Vision.Camera;


@Config
@TeleOp(name="TeleOp Test", group="test")
public class TeleOpTest extends OpMode {

    //Subsystems
    MecanumDrivebase drive;
    Flywheel flywheel;
    WobbleArm wobbleArm;
    Collector collector;
    Hopper hopper;

    Camera camera;
    BlueGoalVisionPipeline pipeline;



    //DcMotor launcherMotor;
//    DcMotor collectorMotor;
//    DcMotor armMotor;
//    Servo liftServo;
//    Servo pushServo;
//    Servo clawServo1;
//    Servo clawServo2;

    ElapsedTime timer = new ElapsedTime();

    double launcherPower;
    double launcherRPM;
    boolean launcherOn;
    //    double collectorPower;
    int armPos;

    // Ensures that the adjustments are made each time the gamepad buttons are pressed rather than each time through loop
    boolean buttonReleased1;
    boolean buttonReleased2;
    boolean triggerReleased;

//    static final double LIFT_UP_POS = 0.50;
//    static final double LIFT_DOWN_POS = 0.75;
//    static final double NOT_PUSH_POS = 0.70;
//    static final double PUSH_POS = 0.52;

    // TODO: test these and edit with accurate values
//    static final int ARM_SPEED = 2;
//    static final int ARM_UPPER_LIMIT = 10000;
//    static final int ARM_LOWER_LIMIT = -10000;
//    static final double CLAW_OPEN_POS = 0.7;
//    static final double CLAW_CLOSE_POS = 0.15;


    int rings = 0;
    int powerShotState = 1; // *** changed from 0 to 1 ***
    double[] powerShotAngles;
    double initialAngle;

    double speed = 0.0;
    double strafe = 0.0;
    double rotation = 0.0;
    double strafePower = 1.0;
    boolean slowmodeOn = false;

    //target angle
    double angle = 0.0;




    enum Mode {
        DRIVER_CONTROL,
        LINE_TO_POINT,
        SHOOT_RINGS,
        GENERATE_NEXT_POWERSHOT_PATH,
        PREPARE_TO_SHOOT_POWERSHOTS,
        SHOOT_RINGS_POWERSHOT,
        ALIGN_TO_ANGLE,
        ALIGN_TO_GOAL;
    }

    Mode currentMode = Mode.DRIVER_CONTROL;


    @Override
    public void init() {

        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        //Init Drive and set estimate
        drive = new MecanumDrivebase(hardwareMap);
        drive.setPoseEstimate(PoseLibrary.AUTO_ENDING_POSE);



        //Init Hopper
        hopper = new Hopper(hardwareMap);
//        liftServo = hardwareMap.servo.get("liftServo");
//        pushServo = hardwareMap.servo.get("pushServo");
//        // Starting position
//        liftServo.setPosition(LIFT_DOWN_POS);
//        pushServo.setPosition(NOT_PUSH_POS);


        //Init Collector
        collector = new Collector(hardwareMap);
//      collectorMotor = hardwareMap.dcMotor.get("collectorMotor");



        //Init Wobble Arm
        // TODO: Do not initialize the arm to the current position after autonomous because it would not be in the starting position at the end of autonomous
        wobbleArm = new WobbleArm(hardwareMap);
//        armMotor = hardwareMap.dcMotor.get("armMotor");
//        armMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        armMotor.setTargetPosition(0);
//        armMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
//        clawServo1 = hardwareMap.servo.get("clawServo");
//        clawServo2 = hardwareMap.servo.get("clawServo2");

        flywheel = new Flywheel(hardwareMap);

        //Init Camera
        pipeline = new BlueGoalVisionPipeline(telemetry);
        camera = new Camera(hardwareMap, pipeline);
        camera.setHighGoalPosition();

        // Initialization values
        launcherPower = 0.0;
        launcherRPM = 3300;
        launcherOn = false;
//        collectorPower = 1.0;
        armPos = 0;
        buttonReleased1 = true;
        buttonReleased2 = true;
        triggerReleased = true;


    }

    @Override
    public void loop() {
        drive.update();

        // Retrieve pose
        Pose2d currentPose = drive.getPoseEstimate();
        telemetry.addData("At Setpoint Angle",UtilMethods.inRange(Math.toDegrees(drive.getRawExternalHeading()), angle - 1, angle + 1));
        telemetry.addData("raw heading", Math.toDegrees(drive.getRawExternalHeading()));

        telemetry.addData("Current Robot Position", pipeline.getFieldPositionFromGoal().toString());
        telemetry.addData("Distance to Goal", pipeline.getDistanceToGoalWall());

        telemetry.addData("Goal Visibility", pipeline.isGoalVisible());
        telemetry.addData("Distance (in)", pipeline.getDistanceToGoalWall());
        telemetry.addData("Current Mode", currentMode);
//        telemetry.addData("Goal Height (px)", getGoalHeight());
//        telemetry.addData("Goal Pitch (degrees)", getPitch());
        telemetry.addData("Goal Yaw (degrees)",pipeline.getYaw());
//        telemetry.addData("Width:", input.width());
//        telemetry.addData("Height:", input.height());
        telemetry.addData("Motor Power", pipeline.getMotorPower());
        telemetry.addData("At Set Point", pipeline.isGoalCentered());
        telemetry.addData("Real motor power", drive.leftFront.getPower());
        telemetry.addData("Timer", timer.seconds());

        // Controls and Information
        telemetry.addData("Drive Mode: ", currentMode);
        telemetry.addData("x", currentPose.getX());
        telemetry.addData("y", currentPose.getY());
        telemetry.addData("heading", Math.toDegrees(currentPose.getHeading()));



        telemetry.addData("Launcher RPM", launcherRPM);
        telemetry.addData("Actual RPM", flywheel.getRPM());
        telemetry.addData("Arm Position", armPos);
        telemetry.addData("Gamepad2 Left Stick Y", gamepad2.left_stick_y);
        telemetry.addData("Actual Arm Position", wobbleArm.getArmPosition());
        telemetry.addData("Slowmode On", slowmodeOn);
        telemetry.addLine("--- Controls (Gamepad 1) ---");
        telemetry.addData("Turn collector on", "Button A");
        telemetry.addData("Turn collector off", "Button B");
        telemetry.addData("Reverse collector direction", "Button X");
        telemetry.addData("Slowmode", "Button Y");
        telemetry.addData("Dpad Up", "Drive to shoot");
        telemetry.addData("Dpad Down", "Cancel Auto");
        telemetry.addLine("--- Experimental ---");
        telemetry.addLine("DPAD UP - Drive to BC shooting position");
        telemetry.addLine("DPAD RIGHT - Reset pose estimate and auto power shots\n");
        telemetry.addLine("DPAD LEFT - Set pose estimate to powershot start pose\n");
        telemetry.addLine("LEFT BUMPER - return to driver control mode");
        telemetry.addLine("RIGHT BUMPER - ALIGN TO GOAL");


        //DPAD RIGHT - Reset pose estimate and auto power shots
        //DPAD DOWN - shoot based on wait logic
        // DPAD LEFT - Set pose estimate to powershot start pose
        //LEFT BUMPER - return to driver control mode
        //RIGHT BUMPER - ALIGN TO GOAL


        telemetry.addLine();
        telemetry.addLine("--- Controls (Gamepad 2) ---");
        telemetry.addData("Open Claw", "Button A");
        telemetry.addData("Close Claw", "Button B");
        telemetry.addData("Move Arm", "Left Stick Up/Down");
        telemetry.addData("Arm Up", "Dpad Up");
        telemetry.addData("Arm Down", "Dpad Down");
        telemetry.addData("Arm Over Wall", "Dpad Right");
        telemetry.addData("Turn launcher on/off", "Button X");
        telemetry.addData("Push/retract collector servo", "Button Y");
        telemetry.addData("Lower collector platform", "Left Bumper");
        telemetry.addData("Lift collector platform", "Right Bumper");
        telemetry.addData("Decrease Launcher Speed", "Left Trigger");
        telemetry.addData("Increase Launcher Speed", "Right Trigger");

        switch (currentMode){
            case DRIVER_CONTROL:

                // Chassis code
                speed = -gamepad1.left_stick_y * strafePower;
                strafe = gamepad1.left_stick_x * strafePower;
                rotation = gamepad1.right_stick_x * strafePower;


//                leftFront.setPower(speed + strafe + rotation);
//                leftBack.setPower(speed - strafe + rotation);
//                rightBack.setPower(speed + strafe - rotation);
//                rightFront.setPower(speed - strafe - rotation);
                drive.setMotorPowers(speed + strafe + rotation, speed - strafe + rotation, speed + strafe - rotation, speed - strafe - rotation);
//                drive.setWeightedDrivePower(
//                        new Pose2d(
//                                -gamepad1.left_stick_y * MecanumDrivebase.VY_WEIGHT,
//                                -gamepad1.left_stick_x * MecanumDrivebase.VX_WEIGHT,
//                                -gamepad1.right_stick_x * MecanumDrivebase.OMEGA_WEIGHT
//                        )
//                );

                // Slowmode

                if (gamepad1.y && buttonReleased1) {
                    if (slowmodeOn) {
//                        MecanumDrivebase.VX_WEIGHT = 1.0;
//                        MecanumDrivebase.VY_WEIGHT = 1.0;
//                        MecanumDrivebase.OMEGA_WEIGHT = 1.0;
                        strafePower = 1.0;
                        slowmodeOn = false;
                    } else {
//                        MecanumDrivebase.VX_WEIGHT = 0.5;
//                        MecanumDrivebase.VY_WEIGHT = 0.5;
//                        MecanumDrivebase.OMEGA_WEIGHT = 0.5;
                        strafePower = 0.5;
                        slowmodeOn = true;
                    }
                    buttonReleased1 = false;
                }

                // Turns collector on/off
                if (gamepad1.a && buttonReleased1) {
                    collector.turnCollectorOn();
                    hopper.setPushOutPos();
                    hopper.setLiftDownPos();
                    buttonReleased1 = false;
//                    collectorPower = 1.0;
//                    pushServo.setPosition(NOT_PUSH_POS);
//                    liftServo.setPosition(LIFT_DOWN_POS);
                }

                if (gamepad1.b && buttonReleased1) {
                    collector.turnCollectorOff();
                    buttonReleased1 = false;
//                    collectorPower = 0.0;
                }

                // Reverses collector

                if (gamepad1.x && buttonReleased1) {
                    collector.turnCollectorReverse();
                    buttonReleased1 = false;
//                    collectorPower = -1.0;
                }

                // Turns launcher on/off
                if (gamepad2.x && buttonReleased2) {
                    /*
                    if (launcherPower == 0.0) {
                        launcherPower = -0.68;
                    } else {
                        launcherPower = 0.0;
                    }
                     */
                    if (launcherOn) {
                        flywheel.setRPM(0);
                        launcherOn = false;
                    } else {
                        flywheel.setRPM(launcherRPM);
                        launcherOn = true;
                    }
                    buttonReleased2 = false;
                }

                // Adjusts launcher speed every time trigger goes below 0.4
                if (gamepad2.left_trigger > 0.4 && triggerReleased && launcherOn) {
                    //launcherPower -= 0.05;
                    launcherRPM -= 50;
                    flywheel.setRPM(launcherRPM);
                    triggerReleased = false;
                }

                if (gamepad2.right_trigger > 0.4 && triggerReleased && launcherOn) {
                    //launcherPower += 0.05;
                    launcherRPM += 50;
                    flywheel.setRPM(launcherRPM);
                    triggerReleased = false;
                }

                // Pushes/retracts collector servo
                if (gamepad2.y && buttonReleased2) {
                    hopper.setPushInPos();
                    timer.reset();
                    buttonReleased2 = false;
//                  pushServo.setPosition(PUSH_POS);
                }

                if (timer.seconds() > 0.75) {
                    hopper.setPushOutPos();
//                  pushServo.setPosition(NOT_PUSH_POS);
                }

                // Lifts/Lowers the collecting platform
                if (gamepad2.left_bumper && buttonReleased2) {
                    hopper.setLiftDownPos();
//                    liftServo.setPosition(LIFT_DOWN_POS);
                    buttonReleased2 = false;
                }

                if (gamepad2.right_bumper && buttonReleased2) {
                    hopper.setLiftUpPos();
//                    liftServo.setPosition(LIFT_UP_POS);
                    buttonReleased2 = false;
                }

                // Lifts/Lowers Wobble Goal Arm

                armPos -= 10 * gamepad2.left_stick_y;
                if (armPos < wobbleArm.ARM_LOWER_LIMIT) {
                    armPos = wobbleArm.ARM_LOWER_LIMIT;
                }
                if (armPos > wobbleArm.ARM_UPPER_LIMIT) {
                    armPos = wobbleArm.ARM_UPPER_LIMIT;
                }

                // Arm Presets

                if (gamepad2.dpad_up && buttonReleased2) {
                    armPos = wobbleArm.ARM_POS_LIFT_ARM;
                    buttonReleased2 = false;
                }
                if (gamepad2.dpad_down && buttonReleased2) {
                    armPos = wobbleArm.ARM_POS_PICKUP_GOAL;
                    wobbleArm.openClaw();
                    buttonReleased2 = false;
                }
                if (gamepad2.dpad_right && buttonReleased2) {
                    armPos = wobbleArm.ARM_POS_OVER_WALL;
                    buttonReleased2 = false;
                }
                if (gamepad2.dpad_left && buttonReleased2) {
                    armPos = 0;
                    buttonReleased2 = false;
                }
                wobbleArm.setArmPos(armPos);

                // Opens/Closes Wobble Goal Claw

                if (gamepad2.a && buttonReleased2) {
                    wobbleArm.openClaw();
                    buttonReleased2 = false;
                }
                if (gamepad2.b && buttonReleased2) {
                    wobbleArm.closeClaw();
                    buttonReleased2 = false;
                }



                // Do not adjust values again until after buttons are released (and pressed again) so the
                // adjustments are made each time the gamepad buttons are pressed rather than each time through loop
                if (!gamepad1.a && !gamepad1.b && !gamepad1.x && !gamepad1.y) {
                    buttonReleased1 = true;
                }

                if(!gamepad2.left_bumper && !gamepad2.right_bumper && !gamepad2.a && !gamepad2.b && !gamepad2.x && !gamepad2.y && !gamepad2.dpad_up && !gamepad2.dpad_right && !gamepad2.dpad_down && !gamepad2.dpad_left) {
                    buttonReleased2 = true;
                }

                if (gamepad2.left_trigger < 0.4 && gamepad2.right_trigger < 0.4) {
                    triggerReleased = true;
                }

                //launcherMotor.setPower(launcherPower);
//                collectorMotor.setPower(collectorPower);

                //EXPERIMENTAL CONTROLS
                // DPAD UP - Drive to BC shooting position
                //DPAD RIGHT - Reset pose estimate and auto power shots
                //DPAD DOWN - shoot based on wait logic
                // DPAD LEFT - Set pose estimate to powershot start pose
                //LEFT BUMPER - return to driver control mode
                //RIGHT BUMPER - ALIGN TO GOAL

                //create trajectory to shooting position on the fly
                if (gamepad1.dpad_up) {
                    // If the D-pad up button is pressed on gamepad1, we generate a lineTo()
                    // trajectory on the fly and follow it
                    // We switch the state to AUTOMATIC_CONTROL

//                    liftServo.setPosition(LIFT_UP_POS);

                    Trajectory driveToShootPositionPath = drive.trajectoryBuilder(currentPose)
                            .lineToLinearHeading(PoseLibrary.SHOOTING_POSE_BC.getPose2d())
                            .build();

                    drive.followTrajectoryAsync(driveToShootPositionPath);
                    flywheel.setRPM(PoseLibrary.SHOOTING_POSE_BC.getRPM());

                    currentMode = Mode.LINE_TO_POINT;
                }

                //reset encoders to powershot starting pose, set powershot state to zero
                if (gamepad1.dpad_right) {
                    //set starting position at left wall
//                    drive.setPoseEstimate(PoseLibrary.POWER_SHOT_START_POSE.getPose2d());
                    drive.setPoseEstimate(pipeline.getFieldPositionFromGoal());
                    powerShotState = 0;
//                    drive.turnTo(0.0);
//                    if(drive.isAtAngle(0.0)) {
                        initialAngle = drive.getRawExternalHeading();
                        powerShotAngles = pipeline.getPowerShotAngles(pipeline.getDistanceFromGoalCenter(), pipeline.getDistanceToGoalWall());
                        timer.reset();
                        currentMode = Mode.GENERATE_NEXT_POWERSHOT_PATH;
//                    }
                }

                //shoot three rings with wait for rpm logic
                if (gamepad1.dpad_down) {
                    rings = 3;
                    timer.reset();
                    launcherOn = true;
                    currentMode = Mode.SHOOT_RINGS;
                }

                if (gamepad1.dpad_left) {
                    timer.reset();
                    currentMode = Mode.ALIGN_TO_ANGLE;
                }

                if(gamepad1.right_bumper && pipeline.isGoalVisible()) {
                    //turn to zero degrees
//                    drive.turnAsync(Angle.normDelta(Math.toRadians(0.0) - currentPose.getHeading()));
                    currentMode = Mode.ALIGN_TO_GOAL;
                    timer.reset();

                }

                break;



            case ALIGN_TO_ANGLE:


                //put angle in degrees
                drive.turnTo(angle);

                if (UtilMethods.inRange(Math.toDegrees(drive.getRawExternalHeading()), angle - 1, angle + 1) && timer.seconds() > 0.02){
                    currentMode = Mode.DRIVER_CONTROL;
                    timer.reset();
                } else{
                    timer.reset();
                }


                if (gamepad1.left_bumper)
                    currentMode = Mode.DRIVER_CONTROL;

                break;

            case ALIGN_TO_GOAL:
                //if goal is centered for 1 second shoot rings, else reset timer
                if (pipeline.isGoalCentered() && timer.seconds() > 0.02) {
                    rings = 3;
                    timer.reset();
                    launcherOn = true;
                    currentMode = Mode.SHOOT_RINGS;
                } else {
                    timer.reset();
                }

                //emergency exit
                if (gamepad1.left_bumper)
                    currentMode = Mode.DRIVER_CONTROL;

                if(pipeline.isGoalVisible()) {
                    //returns positive if robot needs to turn counterclockwise
                    double motorPower = pipeline.getMotorPower();

                    drive.leftFront.setPower(-motorPower);
                    drive.leftRear.setPower(-motorPower);
                    drive.rightFront.setPower(motorPower);
                    drive.rightRear.setPower(motorPower);
                }



                break;
            // generate a trajectory based on powershot state and move to stop and aim state
            case GENERATE_NEXT_POWERSHOT_PATH:
                if (gamepad1.left_bumper) {
                    drive.cancelFollowing();
                    currentMode = Mode.DRIVER_CONTROL;
                    // 0 1 2
                } else if (powerShotState < PoseLibrary.POWER_SHOT_POSES.length - 1) {
                    //0 1 2
//                    Trajectory driveToPowerShotPose = drive.trajectoryBuilder(PoseLibrary.POWER_SHOT_POSES[powerShotState].getPose2d())
//                            //1 2 3
//                            .lineToSplineHeading(PoseLibrary.POWER_SHOT_POSES[powerShotState + 1].getPose2d())
//                            .build();

//                    drive.followTrajectoryAsync(driveToPowerShotPose);
                    angle = powerShotAngles[powerShotState] + initialAngle;

                    drive.turnTo(angle);
                    if(drive.isAtAngle(angle) || timer.seconds() > 1) {
//                        drive.stop();
                        powerShotState++;
                        currentMode = Mode.PREPARE_TO_SHOOT_POWERSHOTS;
                    }
                } else {
                    flywheel.setRPM(0);
                    currentMode = Mode.DRIVER_CONTROL;
                }
                break;

            //when robot has reached the end of it's generated trajectory, reset timer and rings to 1, then move to shoot rings state
            case PREPARE_TO_SHOOT_POWERSHOTS:
                if (gamepad1.left_bumper) {
                    drive.cancelFollowing();
                    currentMode = Mode.DRIVER_CONTROL;
                } else if (!drive.isBusy()) {
                    rings = 1;
                    timer.reset();
                    currentMode = Mode.SHOOT_RINGS_POWERSHOT;
                }
                break;


            //set rings to shoot and reset timer required before moving to this state
            case SHOOT_RINGS_POWERSHOT:
                //emergency exit
                if (gamepad1.left_bumper) {
                    rings = 0;
                    currentMode = Mode.DRIVER_CONTROL;
                }

                flywheel.setRPM(PoseLibrary.POWER_SHOT_POSES[powerShotState].getRPM());
                hopper.setLiftUpPos();
                if (rings > 0) {
                    if (UtilMethods.inRange(flywheel.getRPM(), PoseLibrary.POWER_SHOT_POSES[powerShotState].getRPM() - 150,
                            PoseLibrary.POWER_SHOT_POSES[powerShotState].getRPM() + 150)
                            && hopper.getPushMode() == Hopper.PushMode.PUSH_OUT && timer.seconds() > 0.5) {
                        hopper.setPushInPos();
                        timer.reset();
                    }
                    if (hopper.getPushMode() == Hopper.PushMode.PUSH_IN  && timer.seconds() > 0.5) {
                        hopper.setPushOutPos();
                        timer.reset();
                        rings--;
                    }
                } else {
                    timer.reset();
                    currentMode = Mode.GENERATE_NEXT_POWERSHOT_PATH;
                }

                break;


            //set rings to shoot and reset timer required before moving to this state
            case SHOOT_RINGS:
                drive.setMotorPowers(0,0,0,0);
                //emergency exit
                if (gamepad1.left_bumper || rings == 0) {
                    rings = 0;
                    currentMode = Mode.DRIVER_CONTROL;
                    flywheel.setRPM(0);
                } else {
                    flywheel.setRPM(launcherRPM);
                    hopper.setLiftUpPos();
                }
                if (rings > 0) {
                    if (UtilMethods.inRange(flywheel.getRPM(), launcherRPM - 150, launcherRPM + 150) && hopper.getPushMode() == Hopper.PushMode.PUSH_OUT && timer.seconds() > 0.5) {
                        hopper.setPushInPos();
                        timer.reset();
                    }
                    if (hopper.getPushMode() == Hopper.PushMode.PUSH_IN  && timer.seconds() > 0.5) {
                        hopper.setPushOutPos();
                        timer.reset();
                        rings--;
                    }
                }
                break;




            case LINE_TO_POINT:
                // If left bumper is pressed, we break out of the automatic following
                if (gamepad1.left_bumper) {
                    drive.cancelFollowing();
                    currentMode = Mode.DRIVER_CONTROL;
                }

                // If drive finishes its task, shoot rings
                if (!drive.isBusy()) {
                    rings = 3;
                    timer.reset();
                    currentMode = Mode.SHOOT_RINGS;
                }
                break;


        }

    }


}