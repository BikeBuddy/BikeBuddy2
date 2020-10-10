package com.example.bikebuddy;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

/*
 * Author Theo Brown
 */

public class TripFuelCalculator extends AppCompatActivity {
    ;

    /*
     * Returns the Km/L of the completed trip
     */
    public double calculateTripKmL(Trip trip, Vehicle v) {
        return (trip.distance / v.getFuelUsed());
    }

    /*
     * Returns the predicted maximum range of a vehicle, assuming a full tank and consistent fuel usage.
     * Takes the values from the fields in content_fuel_calculator.xml
     */
    public double calculateMaxRange() {
        TextView fuelTankSize = (TextView) findViewById(R.id.textFuelTankSize);
        TextView avgKML = (TextView) findViewById(R.id.textKPL);
        return (Double.valueOf(String.valueOf(fuelTankSize)) / Double.valueOf(String.valueOf(avgKML)));
    }

    /*
     * Outputs the calculated Maximum range to the text field
     */
    public void showMaxRange() {
        TextView output = (TextView) findViewById(R.id.textMaxRange);
        output.setText(String.valueOf(calculateMaxRange()));
    }

    /*
     *Returns false if trip is longer than the max range of the vehicle, returns true otherwise
     */
    public boolean calculateTripSufficientFuel(Trip trip, Vehicle v) {
        if (trip.distance > calculateMaxRange())
            return false;
        else
            return true;
    }
}
