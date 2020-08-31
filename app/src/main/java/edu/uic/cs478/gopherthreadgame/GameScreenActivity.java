package edu.uic.cs478.gopherthreadgame;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import java.util.Random;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class GameScreenActivity extends Activity {
    // Data fields to access grid
    private TableLayout gridLayout;
    private TableRow rowPos;
    private Button buttonPos;

    // Buttons for move by move and continous and restart
    Button guessButton;
    Button continuousButton;
    Button restartButton;

    // Used for syncing
    boolean t1Turn = true;
    boolean t2Turn = false;

    // Used for next move
    boolean t1NextMove = false;
    boolean t2NextMove = false;
    boolean continuous = false;

    // Textview Messages
    private TextView messageTextview;

    // Position of gopher, thread / worker one and two
    private int gopherPos;
    private int t1Pos;
    private int t2Pos;

    // Keeps track of positions chosen
    private int[] posTaken = new int[100];

    // Boolean that checks if a thread won
    volatile boolean winner = false;

    // Values to be used by handleMessage()
    public static final int SUCCESS = 0 ;
    public static final int NEAR_MISS = 1 ;
    public static final int CLOSE_GUESS = 2 ;
    public static final int COMPLETE_MISS = 3 ;
    public static final int DISASTER = 4 ;

    // Worker Thread One (Random Strategy)
    private Thread t1;
    private Handler t1Handler;

    // Worker Thread Two (Heuristic Strategy)
    private Thread t2;
    private Handler t2Handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_screen_layout);

        // Initialize all positions taken to 0 ( Available )
        for(int i = 0; i < 100; i++){
            posTaken[i] = 0;
        }

        // Load message text view
        messageTextview = (TextView) findViewById(R.id.MessageTextviewID);

        // Randomly selects spot of gopher
        Random rand = new Random();
        gopherPos = rand.nextInt(100); // Generate random pos from 0-99

        // Initially select a random spot for Worker / thread one
        // strategically choose opposite end for thread / worker two
        t1Pos = rand.nextInt(100); // Generate random pos from 0-99
        int t1Row = (t1Pos - (t1Pos %  10)) / 10;
        int t1Col = t1Pos % 10;
        int t2Row = 9 - t1Row;
        int t2Col = 9 - t1Col;
        t2Pos = (t2Row * 10) + t2Col;

        // Load grid
        gridLayout = (TableLayout) findViewById(R.id.GridLayoutID);

        // Highlight position of gopher with green button
        rowPos = (TableRow) gridLayout.getChildAt((gopherPos - (gopherPos %  10)) / 10);
        buttonPos = (Button) rowPos.getChildAt(gopherPos % 10);
        buttonPos.setBackgroundColor(Color.GREEN);

        // Load guess button
        guessButton = (Button) findViewById(R.id.GuessButtonID);
        guessButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                continuous = false; // Set continuous to false
                t1NextMove = true;
                t2NextMove = true;

                continuousButton.setClickable(true);
                continuousButton.setVisibility(View.VISIBLE);

                if(!t1.isAlive() && !t2.isAlive()){
                    // Start both threads
                    t1.start();
                    t2.start();
                }
            }
        });

        // Load continuous button
        continuousButton = (Button) findViewById(R.id.ContinousButtonID);
        continuousButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                continuous = true; // Set continuous to false
                t1NextMove = false;
                t2NextMove = false;

                continuousButton.setClickable(false);
                continuousButton.setVisibility(View.INVISIBLE);

                if(!t1.isAlive() && !t2.isAlive()){
                    // Start both threads
                    t1.start();
                    t2.start();
                }
            }
        });

        // Load Restart Button
        restartButton = (Button) findViewById(R.id.restartButtonID);
        restartButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), GameScreenActivity.class);
                startActivity(intent);
            }
        });

        // Create two worker threads
        t1 = new Thread(new t1Runnable());
        t2 = new Thread(new t2Runnable());

        // Create both worker's handlers
        t1Handler = new Handler(){
            public void handleMessage(Message msg){
                int what = msg.what;
                switch(what) {
                    case SUCCESS:
                        messageTextview.setText("Worker One: Success Game Over");
                        break;
                    case NEAR_MISS:
                        messageTextview.setText("Worker One: Near Miss");
                        break;
                    case CLOSE_GUESS:
                        messageTextview.setText("Worker One: Close Guess");
                        break;
                    case COMPLETE_MISS:
                        messageTextview.setText("Worker One: Complete Miss");
                        break;
                    case DISASTER:
                        messageTextview.setText("Worker One: Disaster");
                        break;
                }
            }
        } ;

        t2Handler = new Handler(){
            public void handleMessage(Message msg){
                int what = msg.what;
                switch(what) {
                    case SUCCESS:
                        messageTextview.setText("Worker Two: Success Game Over");
                        break;
                    case NEAR_MISS:
                        messageTextview.setText("Worker Two: Near Miss");
                        break;
                    case CLOSE_GUESS:
                        messageTextview.setText("Worker Two: Close Guess");
                        break;
                    case COMPLETE_MISS:
                        messageTextview.setText("Worker Two: Complete Miss");
                        break;
                    case DISASTER:
                        messageTextview.setText("Worker Two: Disaster");
                        break;
                }
            }
        } ;

    }


    // Worker One thread one runs in parallel with UI Thread
    public class t1Runnable implements Runnable {
        @Override
        public void run() {
            while (!winner){
                if(t1NextMove || continuous) {
                    if (t1Turn) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // Checks and sends message according to thread position
                        if (checkDisaster(t1Pos, "blue")) {
                            Message msg = t1Handler.obtainMessage(GameScreenActivity.DISASTER);
                            t1Handler.sendMessage(msg);
                        } else if (checkSuccess(t1Pos, gopherPos)) {
                            Message msg = t1Handler.obtainMessage(GameScreenActivity.SUCCESS);
                            t1Handler.sendMessage(msg);
                            winner = true; // Change winner to true to stop worker threads
                        }
                        else if (checkNearMiss(t1Pos, gopherPos)) {
                            Message msg = t1Handler.obtainMessage(GameScreenActivity.NEAR_MISS);
                            t1Handler.sendMessage(msg);
                        }
                        else if (checkCloseGuess(t1Pos, gopherPos)) {
                            Message msg = t1Handler.obtainMessage(GameScreenActivity.CLOSE_GUESS);
                            t1Handler.sendMessage(msg);
                        }
                        else {
                            Message msg = t1Handler.obtainMessage(GameScreenActivity.COMPLETE_MISS);
                            t1Handler.sendMessage(msg);
                        }

                        // t1 will choose a random pos and highlight button to blue
                        t1Handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Randomly choose pos
                                Random rand = new Random();
                                t1Pos = rand.nextInt(100); // Generate random pos from 0-99
                            }
                        });

                        t1Turn = false;
                        t2Turn = true;
                    }
                    t1NextMove = false; // reset to false for next move
                }
            }

            return; // Return once there is a winner
        }
    }

    // Worker Two thread one runs in parallel with UI Thread
    public class t2Runnable implements Runnable {
        @Override
        public void run() {
            while (!winner){
                if(t2NextMove || continuous) {
                    if (t2Turn) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // Checks and sends message according to thread position
                        if (checkDisaster(t2Pos, "red")) {
                            Message msg = t2Handler.obtainMessage(GameScreenActivity.DISASTER);
                            t2Handler.sendMessage(msg);
                        }
                        else if (checkSuccess(t2Pos, gopherPos)) {
                            Message msg = t2Handler.obtainMessage(GameScreenActivity.SUCCESS);
                            t2Handler.sendMessage(msg);
                            winner = true; // Change winner to true to stop worker threads
                            Log.i("WINNER", "RED WON");
                        }
                        else if (checkNearMiss(t2Pos, gopherPos)) {
                            Message msg = t2Handler.obtainMessage(GameScreenActivity.NEAR_MISS);
                            t2Handler.sendMessage(msg);
                        }
                        else if (checkCloseGuess(t2Pos, gopherPos)) {
                            Message msg = t2Handler.obtainMessage(GameScreenActivity.CLOSE_GUESS);
                            t2Handler.sendMessage(msg);
                        }
                        else {
                            Message msg = t2Handler.obtainMessage(GameScreenActivity.COMPLETE_MISS);
                            t2Handler.sendMessage(msg);
                        }

                        // t1 will choose a random pos and highlight button to blue
                        t2Handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Strategically choose opposite end of worker / thread one
                                int t1Row = (t1Pos - (t1Pos %  10)) / 10;
                                int t1Col = t1Pos % 10;
                                int t2Row = 9 - t1Row;
                                int t2Col = 9 - t1Col;
                                t2Pos = (t2Row * 10) + t2Col;
                            }
                        });

                        t2Turn = false;
                        t1Turn = true;
                    }
                    t2NextMove = false; // reset to false for next move
                }
            }

            return; // Return once there is a winner
        }
    }

    // Checks if there is a winner
    public boolean checkSuccess(int myThreadPos, int myGopherPos){
        if (myThreadPos == myGopherPos){
            return true;
        }
        return false;
    }

    // Checks is thread position is a near miss
    public boolean checkNearMiss(int myThreadPos, int myGopherPos){
        Log.i("POSITION", "POSTION is " + myThreadPos);

        // Initialize all neighbors with values -999
        int[] neighbors = new int[]{-999, -999, -999, -999, -999, -999, -999, -999};
        final int topLeft = 0; // Array pos 0 = topLeft
        final int top = 1; // Array pos 1 = top
        final int topRight = 2; // Array pos 2  = topRight
        final int right = 3; // Array pos 3 = right
        final int bottomRight = 4; // Array pos 4 = bottomRight
        final int bottom = 5; // Array pos 5 = bottom
        final int bottomLeft = 6; // Array pos 6 = bottomLeft
        final int left = 7; // Array pos 7 = left

        int gopherRow = (myGopherPos - (myGopherPos %  10)) / 10;
        int gopherCol = myGopherPos % 10;

        // Check if gopher pos is in first row
        // If true then no top neighbors
        if (gopherRow == 0){
            // if gopher is in column 0 then no left neighbors
            if (gopherCol == 0){
                neighbors[right] = myGopherPos + 1;
                neighbors[bottomRight] = myGopherPos + 11;
                neighbors[bottom] = myGopherPos + 10;
            }
            // if gopher is in column 9 then no right neighbors
            else if(gopherCol == 9){
                neighbors[left] = myGopherPos - 1;
                neighbors[bottomLeft] = myGopherPos + 9;
                neighbors[bottom] = myGopherPos + 10;
            }
            else{
                neighbors[left] = myGopherPos - 1;
                neighbors[bottomLeft] = myGopherPos + 9;
                neighbors[bottom] = myGopherPos + 10;
                neighbors[bottomRight] = myGopherPos + 11;
                neighbors[right] = myGopherPos + 1;
            }
        }
        // Check if gopher pos is in last row
        // If true then no bottom neighbors
        else if(gopherRow == 9){
            // if gopher column is 0 then no left neighbors
            if(gopherCol == 0){
                neighbors[top] = myGopherPos - 10;
                neighbors[topRight] = myGopherPos - 9;
                neighbors[right] = myGopherPos + 1;
            }
            // if gopher column is 9 then no right neighbors
            else if (gopherCol == 9){
                neighbors[left] = myGopherPos - 1;
                neighbors[topLeft] = myGopherPos - 11;
                neighbors[top] = myGopherPos - 10;
            }
            else {
                neighbors[left] = myGopherPos - 1;
                neighbors[topLeft] = myGopherPos - 11;
                neighbors[top] = myGopherPos - 10;
                neighbors[topRight] = myGopherPos - 9;
                neighbors[right] = myGopherPos + 1;
            }
        }
        // if gopher positions is in column 0 and not in corners then no left neighbors
        else if (gopherCol == 0 && gopherRow != 0 && gopherRow != 9){
            neighbors[top] = myGopherPos - 10;
            neighbors[topRight] = myGopherPos - 9;
            neighbors[right] = myGopherPos + 1;
            neighbors[bottomRight] = myGopherPos + 11;
            neighbors[bottom] = myGopherPos + 10;
        }
        // if gopher positions is in column 9 and not in corners then no right neighbors
        else if (gopherCol == 9 && gopherRow != 0 && gopherRow != 9){
            neighbors[top] = myGopherPos - 10;
            neighbors[topLeft] = myGopherPos - 11;
            neighbors[left] = myGopherPos - 1;
            neighbors[bottomLeft] = myGopherPos + 9;
            neighbors[bottom] = myGopherPos + 10;
        }
        // Anywhere in the grid with all 8 neighbors
        else{
            neighbors[topLeft] = myGopherPos - 11;
            neighbors[left] = myGopherPos - 1;
            neighbors[bottomLeft] = myGopherPos + 9;
            neighbors[bottom] = myGopherPos + 10;
            neighbors[bottomRight] = myGopherPos + 11;
            neighbors[right] = myGopherPos + 1;
            neighbors[topRight] = myGopherPos - 9;
            neighbors[top] = myGopherPos - 10;
        }

        // Check if any of the neighbor positions is a thread position
        for(int i  = 0; i < 8; i++){
            if(myThreadPos == neighbors[i]){
                return true; // return true if near miss
            }
        }

        return false; // return false if not near miss
    }

    // Checks is thread position is a near miss
    public boolean checkCloseGuess(int myThreadPos, int myGopherPos){
        Log.i("POSITION", "POSTION is " + myThreadPos);
        // Initialize all neighbors with values -999
        int[] neighbors = new int[]{-999, -999, -999, -999, -999, -999, -999, -999};
        final int topLeft = 0; // Array pos 0 = topLeft
        final int top = 1; // Array pos 1 = top
        final int topRight = 2; // Array pos 2  = topRight
        final int right = 3; // Array pos 3 = right
        final int bottomRight = 4; // Array pos 4 = bottomRight
        final int bottom = 5; // Array pos 5 = bottom
        final int bottomLeft = 6; // Array pos 6 = bottomLeft
        final int left = 7; // Array pos 7 = left

        int gopherRow = (myGopherPos - (myGopherPos %  10)) / 10;
        int gopherCol = myGopherPos % 10;

        // Check if gopher pos is in first row
        // If true then no top neighbors
        if (gopherRow == 0){
            // if gopher is in column 0 then no left neighbors
            if (gopherCol == 0){
                neighbors[right] = myGopherPos + 2;
                neighbors[bottomRight] = myGopherPos + 22;
                neighbors[bottom] = myGopherPos + 20;
            }
            // if gopher is in column 9 then no right neighbors
            else if(gopherCol == 9){
                neighbors[left] = myGopherPos - 2;
                neighbors[bottomLeft] = myGopherPos + 18;
                neighbors[bottom] = myGopherPos + 20;
            }
            else{
                neighbors[left] = myGopherPos - 2;
                neighbors[bottomLeft] = myGopherPos + 18;
                neighbors[bottom] = myGopherPos + 20;
                neighbors[bottomRight] = myGopherPos + 22;
                neighbors[right] = myGopherPos + 2;
            }
        }
        // Check if gopher pos is in last row
        // If true then no bottom neighbors
        else if(gopherRow == 9){
            // if gopher column is 0 then no left neighbors
            if(gopherCol == 0){
                neighbors[top] = myGopherPos - 20;
                neighbors[topRight] = myGopherPos - 18;
                neighbors[right] = myGopherPos + 2;
            }
            // if gopher column is 9 then no right neighbors
            else if (gopherCol == 9){
                neighbors[left] = myGopherPos - 2;
                neighbors[topLeft] = myGopherPos - 22;
                neighbors[top] = myGopherPos - 20;
            }
            else {
                neighbors[left] = myGopherPos - 2;
                neighbors[topLeft] = myGopherPos - 22;
                neighbors[top] = myGopherPos - 20;
                neighbors[topRight] = myGopherPos - 18;
                neighbors[right] = myGopherPos + 2;
            }
        }
        // if gopher positions is in column 0 and not in corners then no left neighbors
        else if (gopherCol == 0 && gopherRow != 0 && gopherRow != 9){
            neighbors[top] = myGopherPos - 20;
            neighbors[topRight] = myGopherPos - 18;
            neighbors[right] = myGopherPos + 2;
            neighbors[bottomRight] = myGopherPos + 22;
            neighbors[bottom] = myGopherPos + 20;
        }
        // if gopher positions is in column 9 and not in corners then no right neighbors
        else if (gopherCol == 9 && gopherRow != 0 && gopherRow != 9){
            neighbors[top] = myGopherPos - 20;
            neighbors[topLeft] = myGopherPos - 22;
            neighbors[left] = myGopherPos - 2;
            neighbors[bottomLeft] = myGopherPos + 18;
            neighbors[bottom] = myGopherPos + 20;
        }
        // Anywhere in the grid with all 8 neighbors
        else{
            neighbors[topLeft] = myGopherPos - 22;
            neighbors[left] = myGopherPos - 2;
            neighbors[bottomLeft] = myGopherPos + 18;
            neighbors[bottom] = myGopherPos + 20;
            neighbors[bottomRight] = myGopherPos + 22;
            neighbors[right] = myGopherPos + 2;
            neighbors[topRight] = myGopherPos - 18;
            neighbors[top] = myGopherPos - 20;
        }

        // Check if any of the neighbor positions is a thread position
        for(int i  = 0; i < 8; i++){
            if(myThreadPos == neighbors[i]){
                return true; // return true if near miss
            }
        }

        return false; // return false if not near miss
    }

    // Checks if position has been taken
    public boolean checkDisaster(int myThreadPos, String thread){
        Log.i("POSITION", "POSTION is " + myThreadPos);
        // if position is not taken mark position
        if(posTaken[myThreadPos] == 0){
            posTaken[myThreadPos] = 1;

            rowPos = (TableRow) gridLayout.getChildAt((myThreadPos - (myThreadPos %  10)) / 10);
            buttonPos = (Button) rowPos.getChildAt(myThreadPos % 10);

            if(thread.equals("blue")){
                buttonPos.setBackgroundColor(Color.BLUE);
            }
            else if(thread.equals("red")){
                buttonPos.setBackgroundColor(Color.RED);
            }

            return false;
        }

        return true; // Return true if that position has been taken already
    }

}
