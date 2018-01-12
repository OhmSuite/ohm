package com.team1389.hardware.outputs.hardware;

import java.util.Optional;
import java.util.function.Consumer;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.team1389.configuration.PIDConstants;
import com.team1389.hardware.Hardware;
import com.team1389.hardware.inputs.software.RangeIn;
import com.team1389.hardware.outputs.software.RangeOut;
import com.team1389.hardware.registry.Registry;
import com.team1389.hardware.registry.port_types.CAN;
import com.team1389.hardware.value_types.Position;
import com.team1389.util.list.AddList;
import com.team1389.watch.Watchable;

/**
 * This class offers input/output stream sources for a Talon SRX.
 * <p>
 * Furthermore, this class will ensure that the Talon has been given all
 * required configuration before it enters any control mode. <br>
 * TODO add limit switch support
 * 
 * @author amind
 *
 */
public class CANTalonHardware extends Hardware<CAN> {
	public static final int kTimeoutMs = 10;
	public static final int kMagicProfileSlotIdx = 0;
	public static final int kMagicPIDLoopIdx = 1;
	public static final int kDefaultPIDLoopIdx = 0;
	public static final int kPositionPIDLoopIdx = 0;

	private Optional<WPI_TalonSRX> wpiTalon;
	private Consumer<WPI_TalonSRX> initialConfig;
	private boolean outputInverted;
	private boolean sensorPhase;
	private FeedbackDevice selectedSensor;
	private double sensorRange;

	/**
	 * @param outInverted
	 *            whether the motor output should be inverted (used for both voltage
	 *            and position control modes)
	 * @param inpInverted
	 *            whether the sensor input should be inverted
	 * @param requestedPort
	 *            the port to attempt to initialize this hardware
	 * @param registry
	 *            the registry associated with the robot
	 * @see <a href=
	 *      "https://www.ctr-electronics.com/Talon%20SRX%20Software%20Reference%20Manual.pdf">Talon
	 *      SRX user manual</a> for more information on output/input inversion
	 */
	public CANTalonHardware(boolean outInverted, boolean sensorPhase, FeedbackDevice selectedSensor, double sensorRange,
			CAN requestedPort, Registry registry, Consumer<WPI_TalonSRX> initialConfig) {
		this.outputInverted = outInverted;
		this.sensorPhase = sensorPhase;
		this.selectedSensor = selectedSensor;
		this.sensorRange = sensorRange;
		this.initialConfig = initialConfig;
		attachHardware(requestedPort, registry);
	}

	public CANTalonHardware(boolean outInverted, boolean sensorPhase, FeedbackDevice selectedSensor, double sensorRange,
			CAN requestedPort, Registry registry) {
		this(outInverted, sensorPhase, selectedSensor, sensorRange, requestedPort, registry, t -> {
		});
	}

	/**
	 * assumes input is not inverted
	 * 
	 * @param outInverted
	 *            whether the motor output should be inverted (used for both voltage
	 *            and position control modes)
	 * @param requestedPort
	 *            the port to attempt to initialize this hardware
	 * @param registry
	 *            the registry associated with the robot
	 * @see <a href=
	 *      "https://www.ctr-electronics.com/Talon%20SRX%20Software%20Reference%20Manual.pdf">Talon
	 *      SRX user manual</a> for more information on output/input inversion
	 */
	public CANTalonHardware(boolean outInverted, CAN requestedPort, Registry registry,
			Consumer<WPI_TalonSRX> initialConfig) {
		this(outInverted, false, FeedbackDevice.None, 0, requestedPort, registry, initialConfig);
	}

	public CANTalonHardware(boolean outInverted, CAN requestedPort, Registry registry) {
		this(outInverted, requestedPort, registry, t -> {
		});
	}

	@Override
	public AddList<Watchable> getSubWatchables(AddList<Watchable> stem) {
		return stem;
	}

	@Override
	protected String getHardwareIdentifier() {
		return "Talon";
	}

	@Override
	public void init(CAN port) {
		WPI_TalonSRX talon = new WPI_TalonSRX(port.index());
		talon.setSensorPhase(sensorPhase);
		talon.configSelectedFeedbackSensor(selectedSensor, kDefaultPIDLoopIdx, kTimeoutMs);
		talon.setInverted(outputInverted);
		talon.configNominalOutputForward(0, kTimeoutMs);
		talon.configNominalOutputReverse(0, kTimeoutMs);
		talon.configPeakOutputForward(1, kTimeoutMs);
		talon.configPeakOutputReverse(-1, kTimeoutMs);
		initialConfig.accept(talon);
		wpiTalon = Optional.of(talon);
	}

	@Override
	public void failInit() {
		wpiTalon = Optional.empty();
	}

	public RangeIn<Position> getSensorPositionStream() {
		return new RangeIn<Position>(Position.class,
				() -> wpiTalon.map(t -> (double) t.getSelectedSensorPosition(kDefaultPIDLoopIdx)).orElse(0.0), 0.0,
				sensorRange);
	}

	// Configurations
	/**
	 * @param talon
	 *            talon to configure
	 * @param vcruise
	 *            max velocity in sensor units/sec
	 * @param acc
	 *            max velocity in sensor units/sec^2
	 * @param pid
	 */
	private static void configMotionMagic(WPI_TalonSRX talon, int vCruise, int acc, PIDConstants pid) {
		int vCruiseConv = vCruise / 10;
		int accConv = acc / 10;
		talon.setStatusFramePeriod(StatusFrameEnhanced.Status_13_Base_PIDF0, 10, kTimeoutMs);
		talon.setStatusFramePeriod(StatusFrameEnhanced.Status_10_MotionMagic, 10, kTimeoutMs);

		talon.selectProfileSlot(kMagicProfileSlotIdx, kMagicPIDLoopIdx);

		talon.config_kF(kMagicPIDLoopIdx, pid.f, kTimeoutMs);
		talon.config_kP(kMagicPIDLoopIdx, pid.p, kTimeoutMs);
		talon.config_kI(kMagicPIDLoopIdx, pid.i, kTimeoutMs);
		talon.config_kD(kMagicPIDLoopIdx, pid.d, kTimeoutMs);
		/* set acceleration and vcruise velocity - see documentation */
		talon.configMotionCruiseVelocity(vCruiseConv, kTimeoutMs);
		talon.configMotionAcceleration(accConv, kTimeoutMs);
		/* zero the sensor */
	}

	public RangeOut<Position> getMotionController(int vCruise, int acc, PIDConstants pid) {
		wpiTalon.ifPresent(t -> configMotionMagic(t, vCruise, acc, pid));
		Consumer<Double> positionSetter = d -> {
			if (wpiTalon.map(t -> t.getControlMode() == ControlMode.MotionMagic).orElse(false)) {
				wpiTalon.ifPresent(t -> t.set(ControlMode.MotionMagic, d));
			} else {
				throw new RuntimeException(
						"Error! attempted to use motion magic after control mode was changed, ensure you are only controlling the talon from one place!");
			}
		};
		return new RangeOut<>(positionSetter, 0, sensorRange);
	}

	private static void configPositionControl(WPI_TalonSRX talon, PIDConstants pid) {
		talon.configAllowableClosedloopError(0, kPositionPIDLoopIdx, kTimeoutMs); /* always servo */
		/* set closed loop gains in slot0 */
		talon.config_kF(kPositionPIDLoopIdx, pid.f, kTimeoutMs);
		talon.config_kP(kPositionPIDLoopIdx, pid.p, kTimeoutMs);
		talon.config_kI(kPositionPIDLoopIdx, pid.i, kTimeoutMs);
		talon.config_kD(kPositionPIDLoopIdx, pid.d, kTimeoutMs);
	}

	public RangeOut<Position> getPositionController(PIDConstants pid) {
		wpiTalon.ifPresent(t -> configPositionControl(t, pid));
		Consumer<Double> positionSetter = d -> {
			if (wpiTalon.map(t -> t.getControlMode() == ControlMode.Position).orElse(false)) {
				wpiTalon.ifPresent(t -> t.set(ControlMode.Position, d));
			} else {
				throw new RuntimeException(
						"Error! attempted to use position mode after control mode was changed, ensure you are only controlling the talon from one place!");
			}
		};
		return new RangeOut<>(positionSetter, 0, sensorRange);
	}
}