package es.parkingfy.android;

import android.content.Context;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;

public class BeaconManager {

    static boolean estoyEmitiendo = false;
    static private BeaconTransmitter beaconTransmitter;


    static public void encenderBeacon (Context applicationContext ){
        Beacon beacon = new Beacon.Builder()
                .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
                .setId2("1")
                .setId3("2")
                .setManufacturer(0x0118)
                .setTxPower(-59)
                .setDataFields(Arrays.asList(new Long[] {0l}))
                .build();
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        beaconTransmitter = new BeaconTransmitter(applicationContext, beaconParser);
        beaconTransmitter.startAdvertising(beacon);
        estoyEmitiendo = true;
    }

    static public void apagarBeacon (){
        if (estoyEmitiendo) {
            beaconTransmitter.stopAdvertising();
            estoyEmitiendo = false;
        }
    }
}
