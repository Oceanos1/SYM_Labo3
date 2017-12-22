package com.remikaddie.labo3;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;

import static org.altbeacon.beacon.service.BeaconService.TAG;

/**
 * Inspired by http://altbeacon.github.io/android-beacon-library/samples.html
 * Authorization : http://altbeacon.github.io/android-beacon-library/requesting_permission.html
 */
public class IBeaconActivity extends Activity implements BeaconConsumer {

    private BeaconManager beaconManager;

    private Collection<Beacon> beacons = new ArrayList<>();

    private ListView listView = null;

    private BeaconAdapter beaconAdapter = new BeaconAdapter();

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ibeacon);

        listView = findViewById(R.id.listView);
        listView.setEmptyView(findViewById(R.id.emptyView));
        listView.setAdapter(beaconAdapter);

        // Authorization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Android M Permission check

            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);

                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });

                builder.show();

            }

        }


        // Beacons
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                // Updating the list
                IBeaconActivity.this.beacons = beacons;
                System.out.println(beacons.size());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        beaconAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
        }

    }

    private class BeaconAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return beacons.size();
        }

        @Override
        public Beacon getItem(int i) {
            return (Beacon) (beacons.toArray())[i];
        }

        @Override
        public long getItemId(int i) {
            return (long) i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // Firstly inflating the view if not already done
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) IBeaconActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.beacon_item_view, viewGroup, false);
            }

            // Finding views
            TextView tv1 = (TextView) view.findViewById(R.id.id1);
            TextView tv2 = (TextView) view.findViewById(R.id.id2);
            TextView rssi = (TextView) view.findViewById(R.id.rssi);

            // Setting values
            Beacon cur = getItem(i);
            tv1.setText(cur.getId2().toString());
            tv2.setText(cur.getId3().toString());
            rssi.setText(cur.getRssi() + "");

            return view;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

}
