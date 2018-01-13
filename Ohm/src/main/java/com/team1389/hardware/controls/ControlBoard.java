package com.team1389.hardware.controls;

import com.team1389.hardware.inputs.controllers.LogitechExtreme3D;
import com.team1389.hardware.inputs.controllers.XBoxController;
import com.team1389.hardware.inputs.software.DigitalIn;
import com.team1389.hardware.inputs.software.PercentIn;
import com.team1389.hardware.outputs.software.DigitalOut;

/**
 * A basic framework for the robot controls. Like the RobotHardware, one
 * instance of the ControlBoard object is created upon startup, then other
 * methods request the singleton ControlBoard instance.
 * 
 */
public class ControlBoard
{
	private static ControlBoard mInstance = new ControlBoard();
	public static final double turnSensitivity = 1.0;
	public static final double spinSensitivity = 1.0;

	public static ControlBoard getInstance()
	{
		return mInstance;
	}

	private ControlBoard()
	{
	}

	private final LogitechExtreme3D driveController = new LogitechExtreme3D(0);
	private final XBoxController manipController = new XBoxController(1); 

	public PercentIn driveYAxis()
	{
		return driveController.yAxis().applyDeadband(.075).invert();
	}

	public PercentIn driveXAxis()
	{
		return driveController.xAxis().applyDeadband(.075);
	}

	public PercentIn driveYaw()
	{
		return driveController.yaw().applyDeadband(.075);
	}

	public PercentIn driveTrim()
	{
		return driveController.throttle();
	}

	public DigitalIn driveModeBtn()
	{
		return driveController.thumbButton().latched();
	}

	public DigitalIn driveModifierBtn()
	{
		return driveController.trigger();
	}

	public DigitalIn aButton()
	{
		return manipController.aButton().latched();
	}

	public DigitalIn yButton()
	{
		return manipController.yButton().latched();
	}

	public DigitalIn xButton()
	{
		return manipController.xButton().latched();
	}

	public DigitalIn bButton()
	{
		return manipController.bButton().latched();
	}

	public PercentIn leftTrigger()
	{
		return manipController.leftTrigger();
	}

	public PercentIn leftStickYAxis()
	{
		return manipController.leftStick.yAxis();
	}
	
	public PercentIn leftStickXAxis() 
	{
		return manipController.leftStick.xAxis();
	}

	public DigitalIn rightBumper()
	{
		return manipController.rightBumper().latched();
	}
	public DigitalIn leftBumper()
	{
		return manipController.leftBumper().latched();
	}

	public DigitalOut setRumble()
	{
		return new DigitalOut(b -> manipController.setRumble(b ? 1.0 : 0.0));
	}

	public DigitalIn downDPad()
	{
		return manipController.downArrow().latched();
	}

	public DigitalIn backButton()
	{
		return manipController.backButton().latched();
	}

	public DigitalIn upDPad()
	{
		return manipController.upArrow().latched();
	}

	public PercentIn rightTrigger()
	{
		return manipController.rightTrigger();
	}

	public DigitalIn startButton()
	{
		return manipController.startButton().latched();
	}
}