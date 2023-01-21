package frc.robot.commands;

import java.util.HashMap;
import java.util.Map;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.PrintCommand;
import edu.wpi.first.wpilibj2.command.SelectCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.Constants;
import frc.robot.RobotContainer;
import frc.robot.utils.OutPathFileNameChooser;

public class AutoOneCommand extends SequentialCommandGroup {

    private enum CommandSelector {

        STRAFE_LEFT,
        STRAFE_RIGHT,
        STRAFE_NONE
    }

    public AutoOneCommand() {

        // TODO - do we want to do something cool at each stage like with LEDs?
        // We could create multiple eventMaps
        HashMap<String, Command> eventMap = new HashMap<>();

        OutPathFileNameChooser outPathFileNameChooser = new OutPathFileNameChooser();
        String outPathFileName = outPathFileNameChooser.getOutPathFileName();

        addCommands(new PrintCommand("Starting AutoOneCommand"),
                new InstantCommand(() -> RobotContainer.getDrivetrainSubsystem().zeroGyroscope()),
                new InstantCommand(() -> RobotContainer.getDrivetrainSubsystem()
                        .setPathPlannerDriving(true)),
                new InstantCommand(RobotContainer.getDrivetrainSubsystem()::setMotorsToBrake),
                new PlacePreloadedPieceCommand(),
                new FollowTrajectoryCommand(RobotContainer.getDrivetrainSubsystem(), outPathFileName,
                        eventMap,
                        Constants.MAX_AUTO_VELOCITY, Constants.MAX_AUTO_ACCELERATION, true),
                new FollowTrajectoryCommand(RobotContainer.getDrivetrainSubsystem(), "auto1_back",
                        eventMap,
                        Constants.MAX_AUTO_VELOCITY, Constants.MAX_AUTO_ACCELERATION, true),
                new InstantCommand(() -> RobotContainer.getDrivetrainSubsystem()
                        .setPathPlannerDriving(false)),
                new DockWithAprilTagCommand(false, true),
                new InstantCommand(() -> RobotContainer.getDrivetrainSubsystem()
                        .setPathPlannerDriving(true)),
                new SelectCommand(
                        Map.ofEntries(
                                Map.entry(CommandSelector.STRAFE_LEFT,
                                        new FollowTrajectoryCommand(RobotContainer.getDrivetrainSubsystem(),
                                                "strafe_left", eventMap,
                                                Constants.MAX_AUTO_VELOCITY, Constants.MAX_AUTO_ACCELERATION, true)),
                                Map.entry(CommandSelector.STRAFE_RIGHT,
                                        new FollowTrajectoryCommand(RobotContainer.getDrivetrainSubsystem(),
                                                "strafe_right", eventMap,
                                                Constants.MAX_AUTO_VELOCITY, Constants.MAX_AUTO_ACCELERATION, true)),
                                Map.entry(CommandSelector.STRAFE_NONE,
                                        new PrintCommand("We're already lined up, no strafing necessary"))),
                        this::selectStagedStrafe),
                new PlaceStagedPieceCommand(),
                new InstantCommand(() -> RobotContainer.getDrivetrainSubsystem()
                        .setPathPlannerDriving(false)),
                new PrintCommand("AutoOneCommand Complete!"));
    }

    // Choose whether or not we have to strafe and in what direction based on
    // Shuffleboard inputs
    private CommandSelector selectStagedStrafe() {

        Constants.PlacePosition placePosition = RobotContainer.getShuffleboardManager().getPreloadedPieceLevel();
        Constants.ConeOffsetPosition conePosition = RobotContainer.getShuffleboardManager()
                .getPreloadedConeOffsetPosition();

        if ((placePosition == Constants.PlacePosition.HighCone) ||
                (placePosition == Constants.PlacePosition.MiddleCone) ||
                (placePosition == Constants.PlacePosition.LowCone)) {
            if (conePosition == Constants.ConeOffsetPosition.Left) {
                return CommandSelector.STRAFE_RIGHT;
            } else {
                return CommandSelector.STRAFE_LEFT;
            }
        } else {
            return CommandSelector.STRAFE_NONE;
        }
    }
}
