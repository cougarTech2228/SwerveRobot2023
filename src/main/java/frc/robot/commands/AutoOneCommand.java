package frc.robot.commands;

import java.util.HashMap;
import java.util.Map;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.PrintCommand;
import edu.wpi.first.wpilibj2.command.SelectCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.Constants;
import frc.robot.RobotContainer;
import frc.robot.subsystems.ElevatorSubsystem;
import frc.robot.subsystems.ExtendoSubsystem;
import frc.robot.utils.OutPathFileNameChooser;
import frc.robot.utils.PlacePieceCommandChooser;

public class AutoOneCommand extends SequentialCommandGroup {

    private static ElevatorSubsystem m_elevatorSubsystem;
    private static ExtendoSubsystem m_extendoSubsystem;

    private enum CommandSelector {

        STRAFE_LEFT,
        STRAFE_RIGHT,
        STRAFE_NONE
    }

    private double m_startTime = 0;

    public AutoOneCommand(ElevatorSubsystem elevatorSubsystem, ExtendoSubsystem extendoSubsystem) {

        m_elevatorSubsystem = elevatorSubsystem;
        m_extendoSubsystem = extendoSubsystem;

        // TODO - do we want to do something cool at each stage like with LEDs?
        // We could create multiple eventMaps
        HashMap<String, Command> m_eventMap = new HashMap<>();

        OutPathFileNameChooser m_outPathFileNameChooser = new OutPathFileNameChooser();
        String m_outPathFileName = m_outPathFileNameChooser.getOutPathFileName();

        // Get the appropriate command group to place the Preloaded Game Piece
        PlacePieceCommandChooser m_placePreloadedPieceCommandChooser = new PlacePieceCommandChooser(
            m_elevatorSubsystem, m_extendoSubsystem, RobotContainer.getShuffleboardSubsystem()
                        .getPreloadedPieceLevel());
        SequentialCommandGroup m_placePreloadedPieceSequentialCommandGroup = m_placePreloadedPieceCommandChooser
                .getPlacePieceCommand();

        // Get the appropriate command group to place the Staged Game Piece
        PlacePieceCommandChooser m_placeStagedPieceCommandChooser = new PlacePieceCommandChooser(
            m_elevatorSubsystem, m_extendoSubsystem, RobotContainer.getShuffleboardSubsystem()
                        .getStagedPieceLevel());
        SequentialCommandGroup m_placeStagedPieceSequentialCommandGroup = m_placeStagedPieceCommandChooser
                .getPlacePieceCommand();

        addCommands(new InstantCommand(() -> printStartCommand()),
                new InstantCommand(() -> RobotContainer.getDrivetrainSubsystem().zeroGyroscope()),
                new InstantCommand(RobotContainer.getDrivetrainSubsystem()::setMotorsToBrake),
                m_placePreloadedPieceSequentialCommandGroup,
                new FollowTrajectoryCommand(RobotContainer.getDrivetrainSubsystem(), m_outPathFileName,
                        m_eventMap,
                        Constants.MAX_AUTO_VELOCITY, Constants.MAX_AUTO_ACCELERATION, true),
                new FollowTrajectoryCommand(RobotContainer.getDrivetrainSubsystem(), "auto1_back",
                        m_eventMap,
                        Constants.MAX_AUTO_VELOCITY, Constants.MAX_AUTO_ACCELERATION, true),
                new DockWithAprilTagCommand(false),
                new WaitCommand(Constants.WAIT_TIME_AFTER_APRIL_TAG_DOCK_S), // Let the Network Table updates settle a bit
                new SelectCommand(
                        Map.ofEntries(
                                Map.entry(CommandSelector.STRAFE_LEFT,
                                        new StrafeCommand(Constants.GRID_STRAFE_DISTANCE, -Constants.STRAFE_SPEED,
                                                true)),
                                Map.entry(CommandSelector.STRAFE_RIGHT,
                                        new StrafeCommand(Constants.GRID_STRAFE_DISTANCE, Constants.STRAFE_SPEED,
                                                true)),
                                Map.entry(CommandSelector.STRAFE_NONE,
                                        new PrintCommand("We're already lined up, no strafing necessary"))),
                        this::selectStagedStrafe),
                m_placeStagedPieceSequentialCommandGroup,
                new InstantCommand(() -> RobotContainer.getDrivetrainSubsystem().reverseGyroscope()),
                new InstantCommand(() -> printEndCommand()));
    }

    // Choose whether or not we have to strafe and in what direction based on
    // Shuffleboard inputs
    private CommandSelector selectStagedStrafe() {

        Constants.PlacePosition placePosition = RobotContainer.getShuffleboardSubsystem().getPreloadedPieceLevel();
        Constants.ConeOffsetPosition conePosition = RobotContainer.getShuffleboardSubsystem()
                .getPreloadedConeOffsetPosition();

        if ((placePosition == Constants.PlacePosition.HighCone) ||
                (placePosition == Constants.PlacePosition.MiddleCone) ||
                (placePosition == Constants.PlacePosition.LowCone)) {
            if (conePosition == Constants.ConeOffsetPosition.Left) {
                return CommandSelector.STRAFE_LEFT;
            } else {
                return CommandSelector.STRAFE_RIGHT;
            }
        } else {
            return CommandSelector.STRAFE_NONE;
        }
    }

    private void printStartCommand() {
        m_startTime = Timer.getFPGATimestamp();
        System.out.println("Starting AutoOneCommand");
    }

    private void printEndCommand() {
        System.out.println("AutoOneCommand completed in " + (Timer.getFPGATimestamp() - m_startTime) + " seconds");
    }
}
