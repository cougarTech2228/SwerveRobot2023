package frc.robot.commands;

import java.util.HashMap;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.Constants;
import frc.robot.subsystems.DrivetrainSubsystem;
import frc.robot.subsystems.ElevatorSubsystem;
import frc.robot.subsystems.ExtendoSubsystem;
import frc.robot.subsystems.PneumaticSubsystem;
import frc.robot.subsystems.ShuffleboardSubsystem;
import frc.robot.utils.OutPathFileNameChooser;
import frc.robot.utils.PlacePreloadedPieceCommandChooser;

public class AutoThreeCommand extends SequentialCommandGroup {

    private double m_startTime = 0;
    private static ElevatorSubsystem m_elevatorSubsystem;
    private static ExtendoSubsystem m_extendoSubsystem;
    private static DrivetrainSubsystem m_drivetrainSubsystem;
    private static ShuffleboardSubsystem m_shuffleboardSubsystem;
    private static PneumaticSubsystem m_pneumaticSubsystem;

    public AutoThreeCommand(ElevatorSubsystem elevatorSubsystem, ExtendoSubsystem extendoSubsystem,
            DrivetrainSubsystem drivetrainSubystem, ShuffleboardSubsystem shuffleboardSubsystem,
            PneumaticSubsystem pneumaticSubsystem) {

        m_elevatorSubsystem = elevatorSubsystem;
        m_extendoSubsystem = extendoSubsystem;
        m_drivetrainSubsystem = drivetrainSubystem;
        m_shuffleboardSubsystem = shuffleboardSubsystem;
        m_pneumaticSubsystem = pneumaticSubsystem;

        HashMap<String, Command> m_eventMap = new HashMap<>();

        // Select the "out path" file based on Shuffleboard configuration
        OutPathFileNameChooser m_outPathFileNameChooser = new OutPathFileNameChooser(m_shuffleboardSubsystem);
        String m_outPathFileName = m_outPathFileNameChooser.getOutPathFileName();

        // Get the appropriate command group to place the Preloaded Game Piece
        PlacePreloadedPieceCommandChooser m_placePreloadedPieceCommandChooser = new PlacePreloadedPieceCommandChooser(
                m_elevatorSubsystem, m_extendoSubsystem, m_pneumaticSubsystem, m_drivetrainSubsystem,
                m_shuffleboardSubsystem
                        .getPreloadedPieceLevel());
        SequentialCommandGroup m_placePreloadedPieceSequentialCommandGroup = m_placePreloadedPieceCommandChooser
                .getPlacePieceCommand();

        addCommands(
                new InstantCommand(() -> printStartCommand()),
                new InstantCommand(m_drivetrainSubsystem::zeroGyroscope),
                new InstantCommand(m_drivetrainSubsystem::setMotorsToBrake),
                m_placePreloadedPieceSequentialCommandGroup,

                new FollowTrajectoryCommand(m_drivetrainSubsystem, m_outPathFileName,
                        m_eventMap,
                        Constants.MAX_AUTO_VELOCITY, Constants.MAX_AUTO_ACCELERATION, true),
                new FollowTrajectoryCommand(m_drivetrainSubsystem, "auto3_back", m_eventMap,
                        Constants.MAX_AUTO_VELOCITY, Constants.MAX_AUTO_ACCELERATION, true),
                new InstantCommand(() -> m_drivetrainSubsystem.reverseGyroscope()),
                new InstantCommand(() -> printEndCommand()));
    }

    private void printStartCommand() {
        m_startTime = Timer.getFPGATimestamp();
        System.out.println("Starting AutoThreeCommand");
    }

    private void printEndCommand() {
        System.out.println("AutoThreeCommand completed in " + (Timer.getFPGATimestamp() - m_startTime) + " seconds");
    }
}
