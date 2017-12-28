package debug;



import peasycam3.PeasyCam;
import peasycam3.org.apache.commons.math.geometry.Rotation;
import peasycam3.org.apache.commons.math.geometry.Vector3D;
import processing.core.PApplet;


public class Peasycam_testHUD extends PApplet {
  

  PeasyCam peasycam;
  
  public void settings() {
    size(1200, 900, P3D);
    smooth(8);
  }
  
  public void setup() {
    // camera
    peasycam = new PeasyCam(this, 400);
 
//    peasycam.setRotationConstraint(true, false, false);
  }
  
  public void draw(){
    
    // in case of surface resizing (happens asynchronous) this has no effect in setup
	  perspective(90 * DEG_TO_RAD, width/(float)height, 0.01f, 5000);
    
    // 3D scene
	  ambientLight(128, 128, 128);
    pointLight(255, 128, 64, -200, -200, 10);
    pointLight(64, 128, 255, +200, +200, 10);
    
    background(32);
    
    rectMode(CENTER);
    noStroke();
    fill(128);
    rect(0, 0, 400, 400);
    
    strokeWeight(2);
    stroke(255, 64,  0); line(0,0,0,100,0,0);
    stroke( 32,255, 32); line(0,0,0,0,100,0);
    stroke(  0, 64,255); line(0,0,0,0,0,100);
    
    noStroke();

    fill(255,0,0);
    box(1);
    fill(255, 100);
    box(10);
    
    translate(80,80,80);
    noStroke();
    fill(128);
    box(50);
    
    
    // screen-aligned 2D HUD
    peasycam.beginHUD();

    int wh = 100;
    rectMode(CORNER);
    noStroke();
    fill(0xFFFF0000); rect(       0,        0, wh, wh);
    fill(0xFF00FF00); rect(width-wh,        0, wh, wh, 30);
    fill(0xFF0000FF); rect(width-wh,height-wh, wh, wh);
    fill(0xFFFFFFFF); rect(       0,height-wh, wh, wh, 30);
    
    peasycam.endHUD();
    
    
    // check if everything got restored
    // Note: Depth testing was disabled during the HUD-drawing
    translate(0,-80,0);
    noStroke();
    fill(128);
    box(60);
    
  }
  
  

  
  public void keyReleased(){
    if(key == '1'){
      peasycam.pushState();
    }
    
    if(key == '2'){
      peasycam.popState(1000);
    }
    
    if(key == 'c'){
      System.out.println(peasycam.getState());
    }
    
    if(key == ' '){
      Rotation rot1 = peasycam.getRotation();
      
      double[] XYZ = PeasyCam.Utils.getAnglesXYZ(rot1);
      Rotation rX = new Rotation(Vector3D.plusI, XYZ[0]);
      Rotation rY = new Rotation(Vector3D.plusJ, XYZ[1]);
      Rotation rZ = new Rotation(Vector3D.plusK, XYZ[2]);
      Rotation rot2 = rX.applyTo(rY.applyTo(rZ));
      
      Rotation rot3 = PeasyCam.Utils.newRotationFromAnglesXYZ(XYZ);
      Rotation rot4 = PeasyCam.Utils.newRotationFromAnglesXYZ(0,0,0);
      String str1 = PeasyCam.Utils.getRotationString(rot1);
      String str2 = PeasyCam.Utils.getRotationString(rot2);
      String str3 = PeasyCam.Utils.getRotationString(rot3);
      String str4 = PeasyCam.Utils.getRotationString(rot4);
      System.out.println(str1);
      System.out.println(str2);
      System.out.println(str3);
      System.out.println(str4);
      System.out.println(peasycam.getState());


    }
    
    
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Peasycam_testHUD.class.getName() });
  }
  
}

