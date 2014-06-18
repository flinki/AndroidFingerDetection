package it.peretti.kofler.androidfingerdetection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

public  class Audioplayer {

	
	static int sec=4;
	static int anzahl=0;
	Context con;
	int [] count=new int[20];
	int [] arrayfps=new int[10];

	private MediaPlayer player;


	Audioplayer(Context con){


		this.con=con;
	}

	void playAudio(int a){


		AssetFileDescriptor afd;
		try {
			afd = con.getAssets().openFd("audio/"+a+".wav");
			player = new MediaPlayer();
			player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
			player.prepare();
			player.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	void add(int v,int fps){
		anzahl++;
		arrayfps[anzahl%10]=fps;
		count[v]++;
		int sum=0;
		int i = 0;        // Create a separate integer to serve as your array indexer.
		while(i < arrayfps.length) {   // The indexer needs to be less than 10, not A itself.
			sum += arrayfps[i];   // either sum = sum + ... or sum += ..., but not both
			i++;           // You need to increment the index at the end of the loop.
		}
		int durchsch=(int)(sum/arrayfps.length);
		if(durchsch>0){
			if((anzahl/durchsch)>sec){

				int max=0;
				int zahl=0;
				for(int g=0;g<count.length;g++){
					
					if(count[g]>max){
						max=count[g];
						zahl=g;
					}
				}
				playAudio(zahl);
				reset();
				
			}
		}
		
	}

	private void reset() {
		// TODO Auto-generated method stub
		for(int g=0;g<count.length;g++){
			count[g]=0;
			
		}
		anzahl=0;
	}
}
