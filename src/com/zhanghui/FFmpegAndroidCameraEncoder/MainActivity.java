/**
 * 基于FFmpeg安卓摄像头编码
 * FFmpeg Android Camera Encoder
 *
 * 张晖 Hui Zhang
 * zhanghuicuc@gmail.com
 * 中国传媒大学/数字电视技术
 * Communication University of China / Digital TV Technology
 *
 *
 */

package com.zhanghui.FFmpegAndroidCameraEncoder;


import java.io.IOException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
	private static final String TAG= "MainActivity";
	private Button mTakeButton;
	private Camera mCamera;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private boolean isRecording = false;
	
	private class StreamTask extends AsyncTask<Void, Void, Void>{
		
        private byte[] mData;

        //构造函数
        StreamTask(byte[] data){
            this.mData = data;
        }
        
        @Override
        protected Void doInBackground(Void... params) {   
            // TODO Auto-generated method stub
        	if(mData!=null){
        	Log.i(TAG, "fps: " + mCamera.getParameters().getPreviewFrameRate());         
            encode(mData);
            }
            
          return null;
        }        
    }   
	private StreamTask mStreamTask;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
 		final Camera.PreviewCallback mPreviewCallbacx=new Camera.PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] arg0, Camera arg1) {
				// TODO Auto-generated method stub
				 if(null != mStreamTask){
			            switch(mStreamTask.getStatus()){
			            case RUNNING:
			                return;
			            case PENDING:		            		         
			            	mStreamTask.cancel(false);		            	
			                break;			            
			            }
			        }
				 mStreamTask = new StreamTask(arg0);
				 mStreamTask.execute((Void)null);
			}
		};

        
        mTakeButton=(Button)findViewById(R.id.take_button);
        
        PackageManager pm=this.getPackageManager();
        boolean hasCamera=pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
        		pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT) ||
        		Build.VERSION.SDK_INT<Build.VERSION_CODES.GINGERBREAD;
        if(!hasCamera)
        	mTakeButton.setEnabled(false);
        
        mTakeButton.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(mCamera!=null)
				{
					if (isRecording) {                      
	                    mTakeButton.setText("Start");                   
	                    mCamera.setPreviewCallback(null);    
	                    Toast.makeText(MainActivity.this, "encode done", Toast.LENGTH_SHORT).show();	                    	                                       
	                    isRecording = false;  	           
	                }else {  
	                    mTakeButton.setText("Stop");
	                    initial(mCamera.getParameters().getPreviewSize().width,mCamera.getParameters().getPreviewSize().height);
	                    mCamera.setPreviewCallback(mPreviewCallbacx);
	                    isRecording = true;  
	                } 
				}
			}
		});
		  
       
        mSurfaceView=(SurfaceView)findViewById(R.id.surfaceView1);
        SurfaceHolder holder=mSurfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        holder.addCallback(new SurfaceHolder.Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder arg0) {
				// TODO Auto-generated method stub
				if(mCamera!=null)
				{
					mCamera.stopPreview();
					mSurfaceView = null;  
					mSurfaceHolder = null;   
				}
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder arg0) {
				// TODO Auto-generated method stub
				try{
					if(mCamera!=null){
						mCamera.setPreviewDisplay(arg0);
						mSurfaceHolder=arg0;
					}
				}catch(IOException exception){
					Log.e(TAG, "Error setting up preview display", exception);
				}
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				if(mCamera==null) return;
				Camera.Parameters parameters=mCamera.getParameters();			
				parameters.setPreviewSize(640,480);
				parameters.setPictureSize(640,480);
				mCamera.setParameters(parameters);
				try{
					mCamera.startPreview();
					mSurfaceHolder=arg0;
				}catch(Exception e){
					Log.e(TAG, "could not start preview", e);
					mCamera.release();
					mCamera=null;
				}
			}
		});
        
    }
    
    @TargetApi(9)
    @Override
    protected void onResume(){
    	super.onResume();
    	if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD){
    		mCamera=Camera.open(0);
    	}else
    	{
    		mCamera=Camera.open();
    	}
    }
    
    @Override
    protected void onPause(){
    	super.onPause();
    	flush();
    	close();
    	if(mCamera!=null){
    		mCamera.release();
    		mCamera=null;
    	}
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
  //JNI
    public native int initial(int width,int height);
    public native int encode(byte[] yuvimage);
    public native int flush();
    public native int close();
    
    
    static{
    	System.loadLibrary("avutil-54");
    	System.loadLibrary("swresample-1");
    	System.loadLibrary("avcodec-56");
    	System.loadLibrary("avformat-56");
    	System.loadLibrary("swscale-3");
    	System.loadLibrary("postproc-53");
    	System.loadLibrary("avfilter-5");
    	System.loadLibrary("avdevice-56");
    	System.loadLibrary("encode");
    }
}
