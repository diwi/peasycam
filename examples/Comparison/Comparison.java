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



package Comparison;

import java.util.Locale;
import peasycam3.PeasyCam;
import peasycam3.org.apache.commons.math.geometry.Rotation;
import peasycam3.org.apache.commons.math.geometry.Vector3D;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;


public class Comparison extends PApplet {
  
  PShape scene;
  PeasyCam cam;
  public void settings() {
    size(1280, 720, P3D); // 3D
    smooth(8);
  }
  
  float seed = 0;
  float rand(){
    return rand(++seed, ++seed);
  }
  float rand(float x, float y){
    double val = Math.sin(x * 12.9898 + y * 78.233) * 43758.545;
    return (float)(val - Math.floor(val));
  }
  
  
  public void setup() {
    surface.setResizable(true);
   
    double distance   = 282.316;
    Vector3D center   = new Vector3D(0, 0, 0);
    Rotation rotation = new Rotation(-0.548, -0.834, 0.066, -0.015, false);
    PeasyCam.State state = new PeasyCam.State(distance, center, rotation);
 
    cam = new PeasyCam(this, state);

    randomSeed(2);
    scene = createShape(GROUP);
    int count = 100;
    float trange = 100;
    for(int i = 0; i < count; i++){
//      float dx = random(10, 40);
//      
//      float tx = random(-trange, trange);
//      float ty = random(-trange, trange);
//      float tz = random(-trange, trange);
      
      float dx = rand() * 30 + 10;
      
      float tx = (rand() * 2 - 1) * trange;
      float ty = (rand() * 2 - 1) * trange;
      float tz = (rand() * 2 - 1) * trange;
      
      PShape shape = createShape(BOX, dx, dx, dx);
      shape.translate(tx,  ty, tz);
      shape.setFill(true);
      shape.setStroke(false);
      shape.setStroke(color(255));
      scene.addChild(shape);
    }
  }
  
 
  public void draw(){
    // just in case the window got resized
    cam.setViewport(0, 0, width, height);
  
    // projection - using camera viewport
    perspective(60 * PI/180, width/(float)height, 1, 5000);

    background(16);
    
    // lights setup
    noLights();
    
    ambientLight(96, 96, 96);
    pointLight(0,128,255, 0,0,0);
//    pointLight(64,16,0, 0,0,300);
    
    PVector dir0 = new PVector(-1, +2, -3); dir0.normalize();
    PVector dir1 = new PVector(-1, +2, -3); dir1.normalize();
    PVector dir2 = new PVector(+1, +2, +2); dir2.normalize();
    directionalLight(50,50,50, dir0.x, dir0.y, dir0.z);
    directionalLight(50,50,50, dir1.x, dir1.y, dir1.z);
    directionalLight(50,50,50, dir2.x, dir2.y, dir2.z);
    
    shape(scene);
    
    cam.beginHUD();
    cam.endHUD();
    
    String txt_fps = String.format(Locale.ENGLISH, "|  fps: %7.2f", frameRate);
    surface.setTitle("MultiView  "+txt_fps);
  }


  
  public void keyReleased(){
    System.out.println(cam.getState());
  }
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Comparison.class.getName() });
  }
  
}

