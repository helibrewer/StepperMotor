package com.iot3.steppermotor;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import net.calit2.mooc.iot_db410c.db410c_gpiolib.GpioProcessor;

public class MainActivity extends Activity
{
    static final String tag = "IOT3";
    static final boolean DEBUG = false;     // Set to true to run in emulator mode & test logic only
    static final boolean DEBUGV = false;    // Set true for very verbose log messages

    static final int DELAY = 1;  // Delay Time (should be 1 ms for alternate motor)??

    GpioProcessor gpioProcessor;

    GpioProcessor.Gpio blueWire;
    GpioProcessor.Gpio pinkWire;
    GpioProcessor.Gpio yellowWire;
    GpioProcessor.Gpio orangeWire;

    private EditText degreesTE;
    private int direction = 0;  // 0 = clockwise, 1 = counter-clockwise

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        RadioGroup radioGroup;

        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        radioGroup = (RadioGroup)findViewById( R.id.frg );
        radioGroup.setOnCheckedChangeListener( new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged( RadioGroup group, int checkedId )
            {
                if ( checkedId == R.id.cw )
                    direction = 0;
                else
                    direction = 1;
            }
        } );

        degreesTE = (EditText) findViewById( R.id.degrees );
    }

    public void setGpio( GpioProcessor.Gpio gpioVal, int ssVal )
    {
        if ( DEBUG )
            return;

        int LOW = 0;
        int HIGH = 1;

        if ( ssVal == LOW )
        {
            gpioVal.low();
        }
        else
        {
            gpioVal.high();
        }
        return;
    }


    // Run on GO button click
    public void RunStepperMotor (View view )
    {
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                // See HANNAH's comment on the BOM stepper motor order page of Amazon:
                // https://www.amazon.com/TOOGOO-28BYJ-48-28BYJ48-4-Phase-Stepper/dp/B00H8KVDHE?ie=UTF8&keywords=stepper%20motor%2028byj-48&qid=1435249378&ref_=sr_1_4&sr=8-4
                int rev1 = 64;  // 64 gears / revolution
                // Also here are 32 steps = 8 steps in each sequence * 4 wire settings per step

                // Forward (Clockwise) Sequence - BOM Stepper Motor
                int [][] SS_F = new int [][] { {0,0,0,1},{0,0,1,1},{0,0,1,0}, {0,1,1,0},
                        {0,1,0,0}, {1,1,0,0}, {1,0,0,0}, {1,0,0,1} };

                // Reverse (Counter-Clockwise) Sequence - BOM Stepper Motor
                int [][] SS_R = new int [][] { {0,0,0,1},{1,0,0,1},{1,0,0,0}, {1,1,0,0},
                        {0,1,0,0}, {0,1,1,0}, {0,0,1,0}, {0,0,1,1} };

                // Stepper Motor Controls
                gpioProcessor = new GpioProcessor();
                blueWire = gpioProcessor.getPin34();     // Blue
                pinkWire = gpioProcessor.getPin24();     // Pink
                yellowWire = gpioProcessor.getPin33();   // Yellow
                orangeWire = gpioProcessor.getPin26();   // Orange

                if ( !DEBUG )
                {
                    blueWire.out();
                    pinkWire.out();
                    yellowWire.out();
                    orangeWire.out();
                }
                else
                {
                    Log.d( tag, " ");
                    Log.d( tag, "DEBUG so not setting up GPIOs");
                }

                int degrees = 0;
                try
                {
                    degrees = Integer.parseInt( degreesTE.getText().toString() );
                }
                catch ( NumberFormatException e ) // Default: set degrees = 360
                {
                    degrees = 360;
                }

                Log.d( tag,"Running RunStepperMotor for degrees = "+degrees );

                int sets = 8;   // number of sets in the sequence

                // x is number of steps for the desired angle
                int x = (int)((double)(degrees/360.)*rev1*sets);

                // Set the Sequence array to traverse based on the selected direction
                int [][] SEQ;

                if ( direction == 0 )
                    SEQ = SS_F;
                else
                    SEQ = SS_R;

                Log.d( tag, "direction = "+ direction);
                Log.d( tag, "degrees = "+ degrees + "    x = "+ x );

                // For loop for number of times through the loop to turn motor the specified
                // number of degrees.
                for ( int j = 0; j < x; j++ )
                {
                    if ( DEBUG && DEBUGV )
                    {
                        Log.d( tag, " " );
                        Log.d( tag, "J - " + j );
                    }

                    // for loop to move through stepper motor sequence
                    for ( int i = 0; i < sets; i++ )
                    {
                        if ( DEBUG && DEBUGV )
                            Log.d( tag, "set " + i );

                        if ( !DEBUG )
                        {
                            setGpio( blueWire, SEQ[i][0] );
                            delay( DELAY );

                            setGpio( pinkWire, SEQ[i][1] );
                            delay( DELAY );

                            setGpio( yellowWire, SEQ[i][2] );
                            delay( DELAY );

                            setGpio( orangeWire, SEQ[i][3] );
                            delay( DELAY );
                        }

                        if ( DEBUG && DEBUGV )
                        {
                            Log.d( tag, "blueWire = " + SEQ[i][0] + "     " +
                                    "pinkWire = " + SEQ[i][1] + "     " +
                                    "yellowWire = " + SEQ[i][2] + "     " +
                                    "orangeWire = " + SEQ[i][3] );
                        }
                    } //end for loop
                }

                Log.d( tag, "Done with loops" );
                cleanup();
            }
        } ).start();
    }


    /**
     * Name:        onResume
     * Description: Issues a call to the helper method beginNewGame().
     *              We begin the game here because this method is called when
     *              the screen is live.
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d( tag, "In onResume" );
    }

    /**
     * Name:        onPause
     * Description: Issues a call to the helper method endGame().
     *              We end the game here because this method is called when
     *              the screen is dead.
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        cleanup();
    }

    // cleanup
    public void cleanup()
    {
        if ( !DEBUG )
        {
            // Set all pins off/low????    NEEDED???
            blueWire.low();
            pinkWire.low();
            yellowWire.low();
            orangeWire.low();
        }
        Log.d( tag, "All CLEAN" );
    }

    // delay
    public void delay( int d )
    {
        try
        {
            Thread.sleep( d );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }
}
