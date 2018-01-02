/**
 * 
 * The PeasyCam3 Processing library.
 *
 *   Copyright 2008 Jonathan Feinberg
 *   Copyright 2018 Thomas Diewald
 *
 *   https://github.com/diwi/peasycam/tree/PeasyCam3
 *
 *   Apache License: http://www.apache.org/licenses/LICENSE-2.0
 * 
 * 
 * explanatory notes:
 * 
 * This library is a derivative of the original PeasyCam Library by Jonathan Feinberg 
 * and combines new useful features with the great look and feel of the original version.
 * 
 * It is designed to be in sync with "p5.EasyCam", the Javascript version for p5js.
 * 
 * 
 */




import java.util.Locale;

import peasycam3.PeasyCam;
import peasycam3.org.apache.commons.math.geometry.Rotation;
import peasycam3.org.apache.commons.math.geometry.Vector3D;



  

  //
  // HeadUpDisplay - HUD
  //
  // This demo shows how to create a scope for XY-screen-aligned and orthographic rendering.
  // 
  // ... 3D ....
  // cam.beginHUD();
  // ... 2D ....
  // cam.endHUD();
  // ... 3D ....
  //
  //
  
  
  PeasyCam cam;
  
  public void settings() {
    size(1280, 720, P3D);
    smooth(8);
  }
	
	public void setup() {
	  surface.setResizable(true);
	  
	  cam = new PeasyCam(this, 300);

	  textFont(createFont("SourceCodePro-Regular", 12));
	}
	

	public void draw() {
	  
	  // keep up with resizing window
	  if(cam.getViewport()[2] != width || cam.getViewport()[3] != height){
	    cam.setViewport(0, 0, width, height);
	    cam.apply();
	  }

	  // projection
    perspective(60 * DEG_TO_RAD, width/(float)height, 1, 5000);
	  
    // BG
	  background(32);
	  
    // gizmo
    strokeWeight(1);
    stroke(255, 32,  0); line(0,0,0,100,0,0);
    stroke( 32,255, 32); line(0,0,0,0,100,0);
    stroke(  0, 32,255); line(0,0,0,0,0,100);

    // objects
    strokeWeight(0.5f);
    stroke(0);
    
    pushMatrix();
    translate(50, 50, 0);
    fill(255);
    box(50, 50, 25);
    popMatrix();
    
    pushMatrix();
    translate(-50, -50, 0);
    fill(255,0,128);
    box(50, 50, 25);
    popMatrix();
    
    pushMatrix();
    translate(+50, -50, 0);
    fill(0,128,255);
    box(50, 50, 25);
    popMatrix();
    
    pushMatrix();
    translate(-50, +50, 0);
    rotateX(PI/2);
    fill(128);
    sphere(30);
    popMatrix();
    
    // HeadsUpDisplay
    displayHUD();
	}
	
	
	
	
	void displayHUD(){
    cam.beginHUD();
   
    fill(0,128);
    strokeWeight(1);
    stroke(0);
    rect(10, 10, 250, 200, 5);
    
    PeasyCam.State state = cam.getState();
    
    float[]  view     = cam.getViewport();
    double   distance = state.distance; //  cam.getDistance()
    Vector3D center   = state.center;   //  cam.getCenter()
    Rotation rotation = state.rotation; //  cam.getRotation()
    double[] angles   = PeasyCam.Utils.getAnglesXYZ(rotation);
    
    
    String txt_view     = String.format(Locale.ENGLISH, "Viewport:   %1.0f %1.0f %1.0f %1.0f", view[0], view[1], view[2], view[3]);
    String txt_fps      = String.format(Locale.ENGLISH, "Framerate:  %7.2f", frameRate);
    String txt_dist     = String.format(Locale.ENGLISH, "Distance:   %7.2f", distance);
    String txt_center   = String.format(Locale.ENGLISH, "Center:     %+7.2f, %+7.2f, %+7.2f", center.getX(), center.getY(), center.getZ());
    String txt_rotation = String.format(Locale.ENGLISH, "Rotation:   %+6.3f, %+6.3f, %+6.3f, %+6.3f", rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3() );
    String txt_angles   = String.format(Locale.ENGLISH, "Euler:      %+6.3f, %+6.3f, %+6.3f", angles[0], angles[1], angles[2]);
    // String txt_rotation = PeasyCam.Utils.getRotationString(rotation);
    String txt_gl_version   = String.format(Locale.ENGLISH, "%s", PGraphicsOpenGL.OPENGL_VERSION);
    String txt_gl_renderer  = String.format(Locale.ENGLISH, "%s", PGraphicsOpenGL.OPENGL_RENDERER);
  
    int tx = 25;
    int ty = 35;
    int tdy = 18;
    
    fill(255);
    text(txt_fps     , tx, ty); ty += tdy;
    text(txt_view    , tx, ty); ty += tdy;
    fill(255,128,0);
    text(txt_dist    , tx, ty); ty += tdy;
    text(txt_center  , tx, ty); ty += tdy;
    text(txt_rotation, tx, ty); ty += tdy;
    fill(255);
    text(txt_angles  , tx, ty); ty += tdy;
    ty += tdy;
    fill(255,128,0);
    text(txt_gl_renderer, tx, ty); ty += tdy;
    text(txt_gl_version , tx, ty); ty += tdy;
   
    cam.endHUD();
	}
	
	