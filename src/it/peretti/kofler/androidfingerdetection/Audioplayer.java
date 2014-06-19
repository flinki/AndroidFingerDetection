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
		int i = 0;        
		while(i < arrayfps.length) {   
			sum += arrayfps[i];  
			i++;           
		}
		int durchsch=(int)(sum/arrayfps.length);
		if(durchsch>0){
			if((anzahl/durchsch)>sec){

				int gesam=0;
				int max=0;
				int zahl=0;
				for(int g=0;g<count.length;g++){

					gesam=gesam+count[g];
					if(count[g]>max){
						max=count[g];
						zahl=g;
					}
				}
				if(((double)gesam/max>0.7)){
					System.out.println((double)gesam/max);
				playAudio(zahl);
				reset();
				}
				else{
					reset();}

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
